package com.whatsthatlight.teamcity.hipchat.test;

import java.net.URISyntaxException;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatRoom;
import com.whatsthatlight.teamcity.hipchat.HipChatRooms;

public class HipChatApiProcessorTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test
	@Ignore
	public void testGetRooms() throws URISyntaxException {
		
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatRooms rooms = processor.getRooms();
		for (HipChatRoom room : rooms.items) {
			System.out.println(String.format("%s - %s", room.id, room.name));
		}
	}

}
