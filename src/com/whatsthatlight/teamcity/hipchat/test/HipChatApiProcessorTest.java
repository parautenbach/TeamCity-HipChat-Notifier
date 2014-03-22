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

package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import java.net.URISyntaxException;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticon;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticons;
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
	public void testGetEmoticons() throws URISyntaxException {
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatEmoticons emoticons = processor.getEmoticons(0);
		for (HipChatEmoticon emoticon : emoticons.items) {
			System.out.println(String.format("%s: %s - %s", emoticon.id, emoticon.shortcut, emoticon.url));
		}
	}
	
	@Test
	public void testGetEmoticonsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "invalid_token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatEmoticons emoticons = processor.getEmoticons(0);
		assertNull(emoticons);
	}
	
	@Test
	public void testGetRoomsReturnsEmptyInCaseOfFailure() throws URISyntaxException {
		
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "invalid_token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		HipChatRooms rooms = processor.getRooms();
		assertNotNull(rooms);
		assertNotNull(rooms.items);
		assertEquals(0, rooms.items.size());
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

	@Test
	@Ignore
	public void testTestAuthentication() throws URISyntaxException {
		
		String apiUrl = "https://api.hipchat.com/v2/";
		String apiToken = "token";
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		
		assertTrue(processor.testAuthentication());
	}
	
}
