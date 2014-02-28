/**
Copyright 2014 Pieter Rautenbach

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

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
