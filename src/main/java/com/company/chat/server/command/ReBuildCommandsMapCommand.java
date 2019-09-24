package com.company.chat.server.command;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;

public class ReBuildCommandsMapCommand extends AbstractCommand {

	@Override
	public String getCommandName() {
		return "/reBuildCommands";
	}

	@Override
	public String getDescription() {
		return "Переинициализировать команды";
	}

	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clientId) {
		server.buildCommandsMap();
		return generateSuccessResponse(clientId, "OK");
	}

}
