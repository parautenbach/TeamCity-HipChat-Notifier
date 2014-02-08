package com.whatsthatlight.teamcity.hipchat;

import org.codehaus.jackson.annotate.JsonProperty;

// See: https://www.hipchat.com/docs/apiv2/method/send_room_notification
public class HipChatRoomNotification {

	@JsonProperty("color")
	public String color;

	@JsonProperty("message")
	public String message;

	@JsonProperty("message_format")
	public String messageFormat;

	@JsonProperty("notify")
	public boolean notify;

	public HipChatRoomNotification() {
		// Intentionally left empty
	}

	public HipChatRoomNotification(String message, String messageFormat, String color, boolean notify) {
		this.message = message;
		this.messageFormat = messageFormat;
		this.color = color;
		this.notify = notify;
	}

	@Override
	public String toString() {
		return String.format("Message: '%s'\nFormat: %s\nColor: %s\nNotify: %s", message, messageFormat, color, notify);
	}

}
