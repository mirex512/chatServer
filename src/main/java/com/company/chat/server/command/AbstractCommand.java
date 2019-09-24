package com.company.chat.server.command;

import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.common.ClientInfo;
import com.company.chat.common.IChatServer;
import com.company.chat.common.ICommand;

public abstract class AbstractCommand implements ICommand {

	protected IChatServer server;

	@Override
	public void init(IChatServer server) {
		this.server = server;
	}
	
	
	protected ClientInfo getClientName(long clientId) {
		return server.getClientInfoById(clientId).orElse(ClientInfo.getEmpty());
	}	
	
	protected ClientCommandResponse generateSuccessResponse(long clientId, StringBuilder sb) {
		return generateSuccessResponse(clientId, sb.toString());
	}
	
	protected ClientCommandResponse generateSuccessResponse(long clientId, String sb) {
		ClientInfo clientInfo = getClientName(clientId);
		ClientCommandResponse response = new ClientCommandResponse(true, clientInfo.getName(),sb);
		return response;
	}
}
