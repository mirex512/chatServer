package com.company.chat.server.entity;

public class ServerConfig {

	private String host;
	private int port;	
	private String pathToCommand;
	
	public ServerConfig() {}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPathToCommand() {
		return pathToCommand;
	}

	public void setPathToCommand(String pathToCommand) {
		this.pathToCommand = pathToCommand;
	}
}
