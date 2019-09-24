package com.company.chat.server;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.client.entity.ClientDataType;
import com.company.chat.client.entity.ClientMessageRequest;
import com.company.chat.client.entity.ClientMessageResponse;
import com.company.chat.common.ClientInfo;
import com.company.chat.common.IChatServer;
import com.company.chat.common.ICommand;
import com.company.chat.server.entity.InPackage;
import com.company.chat.server.entity.OutPackage;
import com.company.chat.server.exception.UnknownClientNameException;
import com.company.chat.server.transport.ITransport;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import static com.company.chat.client.util.IDefaults.defaultCharset;

public class ClientPackageHandler implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final AtomicInteger theadsCounter = new AtomicInteger(0);
	private static final Gson gson = new Gson();
	private static final String unsupportedCommandText;
	
	
	private final BlockingQueue<InPackage> queue;
	private final ITransport transport;
	private final AtomicReference<Map<String, ICommand>> commandsMapRef;
	private final IChatServer server;
	
	static {
		StringBuilder sb = new StringBuilder();
		sb.append("Неподдерживаемая команда!!!")
		  .append(System.lineSeparator())
		  .append("Для справки по командам введи: /help");
		unsupportedCommandText = sb.toString();		
	}
	
	public ClientPackageHandler(IChatServer server, BlockingQueue<InPackage> queue, ITransport transport, 
				                AtomicReference<Map<String, ICommand>> commandsMapRef) {
		this.server = server;
		this.queue = queue;
		this.transport = transport;		
		this.commandsMapRef = commandsMapRef;
	}

	@Override
	public void run() {
		String threadName = this.getClass().getSimpleName() + theadsCounter.getAndIncrement();
		Thread.currentThread().setName(threadName);
		
		try {
			while(true) {
				InPackage pack = queue.take();
				switch (pack.getAction()) {
					case Connect:	
						connect(pack);
					break;
					
					case Disconnect:
						disconnect(pack);
						break;
						
					case NewData:	
						newData(pack);
						break;

					default:
						log.warn("Unsupported action: ", pack.toString());
						break;
				}
			}
		} catch (InterruptedException e) {
			log.info("{} interrupt NOW", threadName);
		}
		
		synchronized (server) {
			server.notify();
		}
	}
	
	private void newData(InPackage pack) {
		byte[] rawData = pack.getRawData();
		if(Objects.isNull(rawData))
			return;
		
		try {					
			switch (pack.getClientDataType()) {
				case message:
					handleMessage(pack);
					break;
				
				case command:
					handleCommand(pack);
					break;
					
				default:
					log.warn("Unsupported client data type: {}", pack.toString());
			}
		}catch (UnknownClientNameException e) {
			log.warn("Unknown client name: {}", pack.toString());
		
		}catch (JsonSyntaxException e) {
			log.warn("Error create json: {}", pack.toString());
		
		}catch (UnsupportedEncodingException e) {
			log.warn("Error parsing client data: {}", pack.toString());
		}		
	}
		
	private void handleMessage(InPackage pack) throws UnsupportedEncodingException {
		long id = pack.getClientId();
		String str = new String(pack.getRawData(), defaultCharset.name());
		ClientMessageRequest clientData = gson.fromJson(str, ClientMessageRequest.class);
				
		String name = getClientNameById(id);
		ClientMessageResponse reponse = new ClientMessageResponse(name, clientData.getText(), pack.getTimeStamp());

		String json = gson.toJson(reponse);	
		
		OutPackage out = new OutPackage(ClientDataType.message, json.getBytes());
		transport.sendAll(out);
		server.getMessageStore().addMessage(reponse);		
	}
	
	private void handleCommand(InPackage pack) throws UnsupportedEncodingException {
		long id = pack.getClientId();
		String name = getClientNameById(id);
		String str = new String(pack.getRawData(), defaultCharset.name());
		ClientCommandRequest clientData = gson.fromJson(str, ClientCommandRequest.class);
		String commandName = clientData.getCommand();				
		
		ICommand command = commandsMapRef.get().get(commandName);
		ClientCommandResponse response;
		if(Objects.isNull(command)) {
			response = new ClientCommandResponse(false, name, commandName + ": " + unsupportedCommandText);			
		} else {
			response = command.execute(clientData, id);
		}
		
		String json = gson.toJson(response);
		OutPackage out = new OutPackage(ClientDataType.command, json.getBytes());
		transport.sendByClientId(id, out);
	}

	private String getClientNameById(long id) {
		return server.getClientInfoById(id)
				     .map(ClientInfo::getName)
				     .orElseThrow(UnknownClientNameException::new);
	}
	
	private void disconnect(InPackage pack) {
		server.getClientInfoById(pack.getClientId()).ifPresent(item -> {
			server.getAllClientInfo().remove(item);
		});
		
	}
	
	private void connect(InPackage pack) {
		boolean flag = false;
		
		while(!flag) {
			ClientInfo clientInfo = new ClientInfo(UUID.randomUUID().toString(), pack.getClientId());
			flag = server.getAllClientInfo().add(clientInfo);
		}		
	}	
}
