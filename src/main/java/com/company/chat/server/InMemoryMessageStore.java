package com.company.chat.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.company.chat.client.entity.ClientMessageResponse;
import com.company.chat.common.IMessageStore;

public class InMemoryMessageStore implements IMessageStore {

	private static final int STORAGE_SIZE = 102;
	private static final int LAST_MESSAGE_COUNT = 100;
	
	private static final BlockingQueue<ClientMessageResponse> queue = new LinkedBlockingQueue<>(STORAGE_SIZE);
	
	@Override
	public void addMessage(ClientMessageResponse data) {
		while(true) {
			if(queue.offer(data))
				break;			
			queue.poll();
		}
	}
	
	@Override
	public List<ClientMessageResponse> getLastMessages() {
		List<ClientMessageResponse> data = new ArrayList<ClientMessageResponse>(queue);
		return data.stream().limit(LAST_MESSAGE_COUNT).collect(Collectors.toList());		
	}
}
