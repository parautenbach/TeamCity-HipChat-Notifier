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
