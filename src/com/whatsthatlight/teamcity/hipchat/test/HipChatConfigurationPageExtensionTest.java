package com.whatsthatlight.teamcity.hipchat.test;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationPageExtension;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticonCache;
import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplates;
import com.whatsthatlight.teamcity.hipchat.HipChatRoom;
import com.whatsthatlight.teamcity.hipchat.HipChatRooms;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;
import com.whatsthatlight.teamcity.hipchat.TeamCityEvent;

import jetbrains.buildServer.controllers.WebFixture;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public class HipChatConfigurationPageExtensionTest extends BaseServerTestCase {
	
	private WebFixture webFixture;
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	@BeforeMethod
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		webFixture = new WebFixture(this.myFixture);
		BasicConfigurator.configure();
	}
	
	@AfterMethod
	protected void tearDown() throws Exception {
		super.clearFailure();
		super.tearDown();
	}

	@Test
	public void testIsAvailableFalse() throws IOException {
		// Test parameters
		boolean expectedAvailability = false;
		
		// Mocks and dependencies
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
        
        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);
		
        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		boolean actualAvailability = myPage.isAvailable(request);
	
		// Test
		AssertJUnit.assertEquals(expectedAvailability, actualAvailability);
	}
	
	@Test
	public void testGetGroup() throws IOException {
		// Test parameters
		String expectedGroup = "Server Administration";
		
		// Mocks and dependencies
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		String actualGroup = myPage.getGroup();
	
		// Test
		AssertJUnit.assertEquals(expectedGroup, actualGroup);
	}
	
	@Test
	public void testFillModel() throws Exception {
		// Test parameters
		int expectedModelSize = 31;
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
		HipChatApiResultLinks roomLinks = new HipChatApiResultLinks();
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
	}
	
	@Test
	public void testFillModelUsingServerEventRoomId() throws Exception {
		// Test parameters
		int expectedModelSize = 31;
		String expectedDefaultRoomId = "room1";
		String expectedServerEventRoomId = "room2";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setServerEventRoomId(expectedServerEventRoomId);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<HipChatRoom> roomItems = new ArrayList<HipChatRoom>();
		roomItems.add(new HipChatRoom(expectedDefaultRoomId, null, expectedRoomName));
		HipChatApiResultLinks roomLinks = new HipChatApiResultLinks();
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
		AssertJUnit.assertEquals(expectedDefaultRoomId, model.get("defaultRoomId"));
		AssertJUnit.assertEquals(expectedServerEventRoomId, model.get("serverEventRoomId"));
	}
	
	@Test
	public void testFillModelNoEventsConfiguration() throws Exception {
		// Test parameters
		int expectedModelSize = 23;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		
		ServerPaths serverPaths = org.mockito.Mockito.mock(ServerPaths.class);		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<HipChatRoom> roomItems = new ArrayList<HipChatRoom>();
		roomItems.add(new HipChatRoom(expectedRoomId, null, expectedRoomName));
		HipChatApiResultLinks roomLinks = new HipChatApiResultLinks();
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);		
		
		System.out.println(model.get("apiUrl"));
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
	}
	
	@Test
	public void testFillModelGetTemplateRaisesException() throws IOException {
		// Test parameters
		int expectedModelSize = 19;
		String expectedRoomId = "room1";
		String expectedRoomName = "test room";
		String expectedExceptionText = "This is a test!";
		
		// Logger
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);
		
		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<HipChatRoom> roomItems = new ArrayList<HipChatRoom>();
		roomItems.add(new HipChatRoom(expectedRoomId, null, expectedRoomName));
		HipChatApiResultLinks roomLinks = new HipChatApiResultLinks();
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms(0)).thenReturn(rooms);

		// Other page dependencies
		HipChatConfiguration configuration = new HipChatConfiguration();	
		HipChatNotificationMessageTemplates templates = org.mockito.Mockito.mock(HipChatNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.BUILD_STARTED)).thenThrow(new IOException(expectedExceptionText));
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
        HipChatServerExtension serverExtension = org.mockito.Mockito.mock(HipChatServerExtension.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

        // The test page
        HipChatConfigurationPageExtension myPage = new HipChatConfigurationPageExtension(pagePlaces, descriptor, configuration, processor, templates, serverExtension, emoticonCache);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request);
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		AssertJUnit.assertTrue(exceptionFound);
	}
}