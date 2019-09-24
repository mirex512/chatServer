package com.company.chat.server.command;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;

public class ShutdownServerCommand extends AbstractCommand {

	@Override
	public String getCommandName() {
		return "/shutdownServer";
	}

	@Override
	public String getDescription() {
		return "Остановка сервера";
	}

	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clienId) {
		Thread.currentThread().interrupt();
		return generateSuccessResponse(clienId, "OK");
	}

}
