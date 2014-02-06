package com.whatsthatlight.teamcity.hipchat;

import org.codehaus.jackson.annotate.JsonProperty;

public class HipChatRoomNotification {

	public HipChatRoomNotification() {
		// Intentionally left empty
	}
	
	public HipChatRoomNotification(String message, String messageFormat, String color, boolean notify) {
		this.message = message;
		this.messageFormat = messageFormat;
		this.color = color;
		this.notify = notify;
	}
	
	@JsonProperty("message")
	public String message;

	@JsonProperty("message_format")
	public String messageFormat;

	@JsonProperty("color")
	public String color;

	@JsonProperty("notify")
	public boolean notify;
	
	@Override
	public String toString() {
		return String.format("Message: '%s'\nFormat: %s\nColor: %s\nNotify: %s", message, messageFormat, color, notify);
	}

}
