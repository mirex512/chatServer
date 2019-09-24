package com.company.chat.server.entity;

import com.company.chat.client.entity.ClientDataType;

public class OutPackage {

	private final ClientDataType clientDataType;
	private final byte[] data;
	
	public OutPackage(ClientDataType clientDataType, byte[] data) {
		super();
		this.clientDataType = clientDataType;
		this.data = data;
	}

	public ClientDataType getClientDataType() {
		return clientDataType;
	}

	public byte[] getData() {
		return data;
	}
}
