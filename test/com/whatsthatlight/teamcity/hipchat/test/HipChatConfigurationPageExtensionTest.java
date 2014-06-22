package com.whatsthatlight.teamcity.hipchat.test;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;
import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationPageExtension;
import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplates;
import com.whatsthatlight.teamcity.hipchat.HipChatRoom;
import com.whatsthatlight.teamcity.hipchat.HipChatRooms;

import jetbrains.buildServer.controllers.WebFixture;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public class HipChatConfigurationPageExtensionTest extends BaseServerTestCase {
	
	private WebFixture webFixture;

	@BeforeMethod
	protected void setUp() throws Exception {
		super.setUp();
		webFixture = new WebFixture(myFixture);
	}

	@Test
	public void test() throws Exception {
		// Test parameters
		int expectedModelSize = 24;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<HipChatRoom> roomItems = new ArrayList<HipChatRoom>();
		roomItems.add(new HipChatRoom(expectedRoomId, null, expectedRoomName));
		HipChatApiResultLinks roomLinks = null;
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms()).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
		
        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		assertEquals(expectedModelSize, model.size());
		
	}
}