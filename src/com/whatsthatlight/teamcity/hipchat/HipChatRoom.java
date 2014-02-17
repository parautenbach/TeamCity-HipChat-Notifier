package com.whatsthatlight.teamcity.hipchat;

import org.codehaus.jackson.annotate.JsonProperty;

public class HipChatRoom {
	
	@JsonProperty("id")
	public String id;

	@JsonProperty("links")
	public HipChatRoomLinks links;
	
	@JsonProperty("name")
	public String name;
	
	public HipChatRoom() {
		// Intentionally left empty
	}
	
	public HipChatRoom(String id, HipChatRoomLinks links, String name) {
		this.id = id;
		this.links = links;
		this.name = name;
	}
	
}
