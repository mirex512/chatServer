package com.company.chat.server.main;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.company.chat.server.ChatServer;
import com.company.chat.server.entity.ServerConfig;

public class ServerMain {
	
	private final static Logger log = LoggerFactory.getLogger(ServerMain.class);
	
	public static void main(String[] args) throws IOException {
		log.info("Init....");
		ServerConfig config = ServerInit.getConfig(); 
			
		log.info("Init....OK");
		
		ChatServer server = new ChatServer(config);
		
		new Thread(server).start();
		
	}
}
