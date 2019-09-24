package com.company.chat.server.entity;

import java.util.Arrays;

import com.company.chat.client.entity.ClientDataType;

public class InPackage {

	public enum PackageAction {
		Connect, NewData, Disconnect;
	}
		
	private final PackageAction action;	
	private final long clientId;
	private final long timeStamp;
	
	private byte[] rawData;
	
	private ClientDataType clientDataType;	
		
	public InPackage(PackageAction action, long clientId) {		
		this.action = action;
		this.clientId = clientId;
		timeStamp = System.currentTimeMillis();
	}
	
	public InPackage(byte[] data, long clientId, ClientDataType clientDataType) {		
		action = PackageAction.NewData;
		this.rawData = data;
		this.clientId = clientId;
		this.clientDataType = clientDataType;
		timeStamp = System.currentTimeMillis();
	}
	
	public PackageAction getAction() {
		return action;
	}

	public byte[] getRawData() {
		return rawData;
	}

	public long getClientId() {
		return clientId;
	}

	public long getTimeStamp() {
		return timeStamp;
	}
	
	public ClientDataType getClientDataType() {
		return clientDataType;
	}

	@Override
	public String toString() {
		return "ClientPackage [action=" + action + ", clientId=" + clientId + ", timeStamp=" + timeStamp + ", rawData="
				+ Arrays.toString(rawData) + ", clientDataType=" + clientDataType + "]";
	}	
}
