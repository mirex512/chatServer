package com.company.chat.server.command;

import java.util.Optional;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.common.ClientInfo;

public class ChangeClientNameCommand extends AbstractCommand {
	
	@Override
	public String getCommandName() {
		return "/rename";
	}

	@Override
	public String getDescription() {
		return "Изменить имя клиента";
	}

	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clientId) {
		ClientInfo oldInfo = getClientName(clientId);
		String newName = Optional.ofNullable(clientData.getParam()).map(String::trim).orElse("");
		if(newName.isBlank()) {
			return new ClientCommandResponse(false, oldInfo.getName(), "Ошибка: неподдерживаемое имя: " + newName);
		}
		
		ClientInfo newInfo = new ClientInfo(newName, clientId);
		ClientCommandResponse response ;
		if(server.getAllClientInfo().add(newInfo)) {
			response = new ClientCommandResponse(true, newName, "Успех: Имя изменено на: " + newName);
			if(oldInfo.getId() != -1)
				server.getAllClientInfo().remove(oldInfo);
		} else {
			response = new ClientCommandResponse(false, oldInfo.getName(), "Ошибка: такое имя: " + newName + " уже существует");
		}
		return response;
	}
}
