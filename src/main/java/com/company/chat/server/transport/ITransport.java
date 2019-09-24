package com.company.chat.server.transport;

import java.io.IOException;
import java.util.function.Consumer;

import com.company.chat.server.entity.InPackage;
import com.company.chat.server.entity.OutPackage;

public interface ITransport extends Runnable {

	public void init();
	
	public void setClientNewDataHandler(Consumer<InPackage> consumer); 
	
	public void sendByClientId(long clientId, OutPackage pack);
	
	public void sendAll(OutPackage pack);
	
	public void shutdown() throws IOException;
	
}
