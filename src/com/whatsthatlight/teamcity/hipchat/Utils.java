package com.whatsthatlight.teamcity.hipchat;

import java.util.TreeMap;

public class Utils {
	public static TreeMap<String, String> getRooms(HipChatApiProcessor processor) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (HipChatRoom room : processor.getRooms().items) {
			map.put(room.name, room.id);
		}
		return map;
	}
}
