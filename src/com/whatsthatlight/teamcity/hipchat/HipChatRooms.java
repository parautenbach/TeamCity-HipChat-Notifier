package com.whatsthatlight.teamcity.hipchat;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

// See: https://www.hipchat.com/docs/apiv2/method/get_all_rooms
public class HipChatRooms {

	@JsonProperty("items")
	public List<HipChatRoom> items;

	@JsonProperty("startIndex")
	public int startIndex;

	@JsonProperty("maxResults")
	public int maxResults;

	@JsonProperty("links")
	public HipChatRoomsLinks links;

	public HipChatRooms() {
		// Intentionally left empty
	}

	public HipChatRooms(List<HipChatRoom> items, int startIndex, int maxResults, HipChatRoomsLinks links) {
		this.items = items;
		this.startIndex = startIndex;
		this.maxResults = maxResults;
		this.links = links;
	}

}
