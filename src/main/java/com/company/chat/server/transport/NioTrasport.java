package com.company.chat.server.transport;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.company.chat.client.entity.ClientDataType;
import com.company.chat.server.entity.InPackage;
import com.company.chat.server.entity.InPackage.PackageAction;
import com.company.chat.server.entity.OutPackage;
import com.company.chat.server.entity.ServerConfig;

public class NioTrasport implements ITransport {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final ServerConfig config;
	private final ByteBuffer buffer;
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	private static final AtomicLong clientConnectionCounter = new AtomicLong(0l); 
	
	private Selector selector = null;
	private ServerSocketChannel serverChannel = null;
	
	private Consumer<InPackage> clientNewDataConsumer;
	
	private static final ConcurrentMap<Long, SocketChannel> clientIdToChannelMap = new ConcurrentHashMap<>();
	private static final ConcurrentMap<SocketChannel, Long> channelToClientIdMap = new ConcurrentHashMap<>();
	
	
	public NioTrasport(ServerConfig config) {
		this.config = config;
		buffer = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN);
	}

	@Override
	public void init() {
		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(config.getHost(), config.getPort()));
						
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
				
		} catch (IOException e) {
			log.error("Error server initialization", e);
			log.error("Application terminate!!!");
			System.exit(-1);
		}
	}
	
	@Override
	public void run() {
		String threadName = this.getClass().getSimpleName();
		Thread.currentThread().setName(threadName);
		
		try {
			while(selector.select() > -1) {
				if(!selector.isOpen())
					break;
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator	= keys.iterator();
				while(iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if(!key.isValid()) {
						continue;
					}
					try {
						if(key.isAcceptable()) {
							accept(key);						
						} else if(key.isReadable()) {
							read(key);														
						} else {
							log.warn("Unsupported key type");
						}
					}catch (Exception e) {
						log.error("Message processing error", e);
					}					
				}
			}
		}catch (IOException e) {
			log.error("Transport channel error", e);
		}
	}
	
	private void accept(SelectionKey key) {
		try {		
			SocketChannel client = serverChannel.accept();
			if(Objects.nonNull(client)) {
				try {
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
					
					SocketAddress remoteAddr = client.socket().getRemoteSocketAddress();
					log.info("Connected to: {}", remoteAddr);
					
					long clientId = clientConnectionCounter.getAndIncrement();	
					clientIdToChannelMap.put(clientId, client);		
					channelToClientIdMap.put(client, clientId);
					InPackage pack = new InPackage(PackageAction.Connect, clientId);
					clientNewDataConsumer.accept(pack);
				}catch (IOException e) {
					log.error("Error client channel configured", e);
				}
			} else {				
				close(client);
			}
		}catch (IOException e) {
			log.error("Error accept", e);
		}
	}
	
	private void read(SelectionKey key) {
		SocketChannel client = (SocketChannel)key.channel();
		
		byte[] data = null;
		int length = BUFFER_SIZE;
		try { 
			while(length == BUFFER_SIZE) {
				buffer.clear();
				length = client.read(buffer);
				if(length < 1)
					break;
				byte[] newData = new byte[length];
				buffer.flip();
				buffer.get(newData);
				if(Objects.isNull(data)) {
					data = newData;
				} else {
					byte[] b = new byte[data.length + newData.length];
					System.arraycopy(data, 0, b, 0, data.length);
					System.arraycopy(newData, 0, b, data.length, newData.length);
					data = b;
				}
			}
		}catch (IOException e) {
			log.error("Error read clien data, client disconnected", e);
			close(client, key);
			return;
		}
		
		if(length == -1) {
			close(client, key);			
		} else {
			
			ClientDataType type = null;
			try {
				type = ClientDataType.values()[data[0]];
			}catch (ArrayIndexOutOfBoundsException e) {
				log.warn("Unknown package type: {}", data[0]);
				return;
			}
			
			data = Arrays.copyOfRange(data, 1, data.length);
			long clientId = channelToClientIdMap.get(client);			
			InPackage pack = new InPackage(data, clientId, type);
			clientNewDataConsumer.accept(pack);
		}
	}
	
	@Override
	public void setClientNewDataHandler(Consumer<InPackage> consumer) {
		clientNewDataConsumer = consumer;
	}	

	public void sendAll(OutPackage pack) {
		final ByteBuffer bb = prepareOutPackage(pack);
		Runnable sendTask = () -> { 
			for(SocketChannel client : clientIdToChannelMap.values()) {
				bb.rewind();
				send(client, bb);
			}
		};
		executor.execute(sendTask);
	}
	
	@Override
	public void sendByClientId(long clientId, OutPackage pack) {	
		
		SocketChannel channel = clientIdToChannelMap.get(clientId);
		if(Objects.nonNull(channel)) {
			final ByteBuffer bb = prepareOutPackage(pack);
			send(channel, bb);
		}
	}
	
	private ByteBuffer prepareOutPackage(OutPackage pack) {
		ByteBuffer bb = ByteBuffer.allocate(pack.getData().length + 1).order(ByteOrder.BIG_ENDIAN);
		switch (pack.getClientDataType()) {
			case message:
				bb.put((byte)0);
				break;
			case command:
				bb.put((byte)1);
				break;
			default:
				break;			
		}
		bb.put(pack.getData());
		bb.rewind();
		return bb;
	}
	
	private void send(SocketChannel client, ByteBuffer bb) {	
		try {
			if(client.isConnected() && client.isRegistered()) {
				while(bb.hasRemaining()) {
					client.write(bb);				
				}
			}
		} catch (IOException e) {				
			log.error("Error send data to client, client disconnected", e);		
			close(client);
		}
		return;
	}
	
	private void close(SocketChannel client, SelectionKey key) {
		try {
			SocketAddress remoteAddr = client.socket().getRemoteSocketAddress();
			log.info("Connection closed by client: {}", remoteAddr);
			
			Long clientId = channelToClientIdMap.remove(client);
			if(Objects.nonNull(clientId))
				clientIdToChannelMap.remove(clientId);
			
			if(Objects.nonNull(key))
				key.cancel();
			client.close();
			
			InPackage pack = new InPackage(PackageAction.Disconnect, clientId);
			clientNewDataConsumer.accept(pack);
			
		}catch (IOException e) {
			log.error("Error close client channel", e);
		}
	}
	
	private void close(SocketChannel client) {
		SelectionKey key = client.keyFor(selector);
		close(client, key);
	}	
	
	public void shutdown() throws IOException {
		serverChannel.keyFor(selector).cancel();
		serverChannel.close();
		
		executor.shutdown();
		try {
			executor.awaitTermination(1l, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			/*NOP*/
		}
		
		selector.keys().forEach(SelectionKey::cancel);
		
		
		channelToClientIdMap.keySet().forEach(t -> {
			try {
				t.close();
			} catch (Exception e) {		
				log.error("Error close client socket", e);
			}
		});			
			
		selector.close();
		
	}
}
