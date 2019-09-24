package com.company.chat.server.command;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.common.ClientInfo;

public class GetUsersCommand extends AbstractCommand {

	@Override
	public String getCommandName() {
		return "/getUsers";
	}

	@Override
	public String getDescription() {
		return "Отобразить список подключенных пользователей";
	}

	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clientId) {
		StringBuilder sb = new StringBuilder().append(System.lineSeparator());
		for(ClientInfo clientInfo :server.getAllClientInfo()) {
			sb.append(clientInfo.getName()).append(System.lineSeparator());
		}
		return generateSuccessResponse(clientId, sb);
	}
}
