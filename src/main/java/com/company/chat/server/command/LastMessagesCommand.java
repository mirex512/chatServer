package com.company.chat.server.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.company.chat.client.entity.ClientCommandRequest;
import com.company.chat.client.entity.ClientCommandResponse;
import com.company.chat.client.entity.ClientMessageResponse;

import static com.company.chat.client.util.IDefaults.FORMATTER;

/*
 * Плохая реализация, нет доступа к транспорту(а должен ли быть доступ????), 
 * приходится отдовать последнии команды строкой!!!!
 * И попадаем с временной зоной!!!! 
 * */
public class LastMessagesCommand extends AbstractCommand {

	@Override
	public String getCommandName() {		
		return "/getLastMessages";
	}

	@Override
	public String getDescription() {	
		return "Вернуть последнии сообщения(beta)";
	}

	@Override
	public ClientCommandResponse execute(ClientCommandRequest clientData, long clientId) {
		List<ClientMessageResponse> messageList =  server.getMessageStore().getLastMessages();
		StringBuilder sb = new StringBuilder().append(System.lineSeparator());
		for(ClientMessageResponse response : messageList) {
			ZonedDateTime zdt =  ZonedDateTime.ofInstant(Instant.ofEpochMilli(response.getTimeStamp()), ZoneId.systemDefault());
			String time = zdt.format(FORMATTER);
			sb.append(time)
			  .append(" ")
			  .append(response.getName())
			  .append(": ")
			  .append(response.getMessage())
			  .append(System.lineSeparator());
		}
		
		 
		return generateSuccessResponse(clientId, sb);
	}

}
