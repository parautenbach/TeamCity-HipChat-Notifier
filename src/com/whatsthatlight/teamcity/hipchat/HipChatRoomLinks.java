package com.whatsthatlight.teamcity.hipchat;

import org.codehaus.jackson.annotate.JsonProperty;

public class HipChatRoomLinks {

	@JsonProperty("self")
	public String self;
	
	@JsonProperty("webhooks")
	public String webhooks;
	
	@JsonProperty("members")
	public String members;
	
	public HipChatRoomLinks() {
		// Intentionally left empty
	}
	
	public HipChatRoomLinks(String self, String webhooks, String members) {
		this.self = self;
		this.webhooks = webhooks;
		this.members = members;
	}
}
