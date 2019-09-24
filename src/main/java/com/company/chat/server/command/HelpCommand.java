package com.company.chat.server.command;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.common.ICommand;

public class HelpCommand extends AbstractCommand {

	@Override
	public String getCommandName() {		
		return "/help";
	}

	@Override
	public String getDescription() {
		return "Описание команд";
	}


	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clientId) {
		
		StringBuilder sb = new StringBuilder().append(System.lineSeparator());
		for(ICommand command : server.getCommandsMap().values()) {
			sb.append(command.getCommandName()).append(": ").append(command.getDescription()).append(System.lineSeparator());
		}		
		return generateSuccessResponse(clientId, sb);		
	}
}
