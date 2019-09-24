package com.company.chat.server;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.company.chat.common.ClientInfo;
import com.company.chat.common.IChatServer;
import com.company.chat.common.ICommand;
import com.company.chat.common.IMessageStore;
import com.company.chat.server.entity.InPackage;
import com.company.chat.server.entity.ServerConfig;
import com.company.chat.server.transport.ITransport;
import com.company.chat.server.transport.NioTrasport;

public class ChatServer implements IChatServer, Runnable {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final int CLIENT_PACKAGE_HANDLER_COUNT = 3;
	private static final int QUEUE_SIZE = 1_000;
	
	private static final IMessageStore messageStore = new InMemoryMessageStore();
	private static final AtomicReference<Map<String, ICommand>> commandsMapRef = new AtomicReference<Map<String,ICommand>>();	
	private static final Set<ClientInfo> clientInfoSet = ConcurrentHashMap.newKeySet();
	
	private final ServerConfig config;

	public ChatServer(ServerConfig config) {
		super();
		this.config = config;
	}


	@Override
	public void run() {
		
		String threadName = this.getClass().getSimpleName();
		Thread.currentThread().setName(threadName);
		
		buildCommandsMap();
		
		BlockingQueue<InPackage> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
				
		ITransport transport = new NioTrasport(config);
		
		Consumer<InPackage> newClientDataHandler = data -> {
			if(!queue.offer(data)) {
				log.warn("Message processing queue is full! Message rejected");
			}
		};
	
		transport.init();
		transport.setClientNewDataHandler(newClientDataHandler);
		
		for(int i = 0; i < CLIENT_PACKAGE_HANDLER_COUNT; i++) {
			Thread thread = new Thread(new ClientPackageHandler(this, queue, transport, commandsMapRef));
			thread.setDaemon(true);
			thread.start();
		}
		
		Thread transportThread = new Thread(transport);
		transportThread.start();
		
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			log.info("{} interrupt NOW", threadName);
		}
		try {
			transport.shutdown();
		}catch (Exception e) {
			log.error("Transport stop error", e);
		}
	}
	
	public void buildCommandsMap() {
		Map<String, ICommand> commandsMap = new HashMap<>();
		
		
		Reflections  reflections = new Reflections("com.company.chat.server.command");		
		Set<Class<? extends ICommand>> commandsClass = reflections.getSubTypesOf(ICommand.class);		
		for(Class<? extends ICommand> clazz : commandsClass) {
			if(Modifier.isAbstract(clazz.getModifiers())) 
				continue;
			try {
				Constructor<? extends ICommand> ctor = clazz.getDeclaredConstructor();				
				ICommand instance = ctor.newInstance();					 
				instance.init(this);
				commandsMap.put(instance.getCommandName(), instance);
			}catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error init command ", e);
			}
		}
		
		
		if(Files.exists(Paths.get(config.getPathToCommand()))) {		
			Map<String, ICommand> map = loadFromJars(config.getPathToCommand());
			commandsMap.putAll(map);			
		}
		
		commandsMapRef.getAndSet(commandsMap);
		
 	}
	
	private Map<String, ICommand> loadFromJars(String pathToDir) {
		
		Map<String, ICommand> map = new HashMap<>();		
		
		Set<Path> paths;
		try {
			paths = Files.list(Paths.get(pathToDir))
						            .filter(Files::isRegularFile)
						            .collect(Collectors.toSet());
		} catch (IOException e) {
			log.error("Error read jar commands files", e);
			return map;
		}
		
		for(Path path : paths) {
			try {
				
				String pathToJar = path.toString();
				JarFile jarFile = new JarFile(path.toString());
				Enumeration<JarEntry> e  = jarFile.entries();
				URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
				URLClassLoader cl = URLClassLoader.newInstance(urls);
			
				while (e.hasMoreElements()) {
					JarEntry je = e.nextElement();
					if(je.isDirectory() || !je.getName().endsWith(".class")){
						continue;
					}

					String className = je.getName().substring(0, je.getName().length() - 6);
					className = className.replace('/', '.');
					Class<?> clazz = cl.loadClass(className);
			    
					for(Class<?> i : clazz.getInterfaces()) {
						if(i.isAssignableFrom(ICommand.class)) {
							Constructor<?> ctor = clazz.getDeclaredConstructor();				
							ICommand instance = (ICommand) ctor.newInstance();					 
							instance.init(this);
							map.put(instance.getCommandName(), instance);
						}
					}
				}			
				jarFile.close();
			}catch (IOException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Error init jar command", e);
			}
		}
		return map;
	}
	
	@Override
	public Set<ClientInfo> getAllClientInfo() {		
		return clientInfoSet;
	}


	@Override
	public Optional<ClientInfo> getClientInfoById(long id) {
		return clientInfoSet.parallelStream()
				            .filter(p -> p.getId() == id)
				            .findFirst();
	}

	@Override
	public Map<String, ICommand> getCommandsMap() {
		return commandsMapRef.get();
	}


	@Override
	public IMessageStore getMessageStore() {		
		return messageStore;
	}
	
}
