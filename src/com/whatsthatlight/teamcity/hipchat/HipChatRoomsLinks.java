package com.whatsthatlight.teamcity.hipchat;

import org.codehaus.jackson.annotate.JsonProperty;

public class HipChatRoomsLinks {

	@JsonProperty("self")
	public String self;
	
	@JsonProperty("prev")
	public String prev;
	
	@JsonProperty("next")
	public String next;
	
	public HipChatRoomsLinks() {
		// Intentionally left empty
	}
	
	public HipChatRoomsLinks(String self, String prev, String next) {
		this.self = self;
		this.prev = prev;
		this.next = next;
	}
	
}
