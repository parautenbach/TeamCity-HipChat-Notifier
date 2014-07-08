package com.whatsthatlight.teamcity.hipchat.test;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationPageExtension;
import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplates;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectTab;
import com.whatsthatlight.teamcity.hipchat.HipChatRoom;
import com.whatsthatlight.teamcity.hipchat.HipChatRooms;
import com.whatsthatlight.teamcity.hipchat.TeamCityEvent;

import jetbrains.buildServer.controllers.WebFixture;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public class HipChatProjectTabTest extends BaseServerTestCase {
	
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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFillModelNoConfigurationAndIsRootProject() throws Exception {
		// Test parameters
		int expectedModelSize = 5;
		String expectedRoomId = "default";
		String expectedRoomName = "test room";
		boolean expectedNotifyStatus = false;
		boolean expectedIsRootProject = true;
		String expectedProjectId = "project1";
		String availableRoomId = "some_room";
		String availableRoomName = "Some Room";
		TreeMap<String, String> expectedRoomIdList = new TreeMap<String, String>();
		expectedRoomIdList.put(availableRoomName, availableRoomId);
		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotifyStatus);

		// Expected rooms
		int startIndex = 0;
		int maxResults = 1;
		List<HipChatRoom> roomItems = new ArrayList<HipChatRoom>();
		roomItems.add(new HipChatRoom(availableRoomId, null, expectedRoomName));
		HipChatApiResultLinks roomLinks = null;
		HipChatRooms rooms = new HipChatRooms(roomItems, startIndex, maxResults, roomLinks);

		// Project mocks
		SProject parentProject = org.mockito.Mockito.mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn("_Root");
		SProject project = org.mockito.Mockito.mock(SProject.class);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		
		// Processor mock
		HipChatApiProcessor processor = org.mockito.Mockito.mock(HipChatApiProcessor.class);
		when(processor.getRooms()).thenReturn(rooms);

		// Other page dependencies
		SUser user = org.mockito.Mockito.mock(SUser.class);
		PagePlaces pagePlaces = webFixture.getPagePlaces();
		PluginDescriptor descriptor = org.mockito.Mockito.mock(PluginDescriptor.class);
        when(descriptor.getPluginResourcesPath(anyString())).thenReturn("");
		
        // The test page
        HipChatProjectTab myPage = new HipChatProjectTab(pagePlaces, this.myProjectManager, descriptor, configuration, processor);

        // Execute
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);
		Map<String, Object> model = new HashMap<String, Object>();
		myPage.fillModel(model, request, project, user);
		
		// Test
		AssertJUnit.assertEquals(expectedModelSize, model.size());
		AssertJUnit.assertEquals(expectedRoomId, model.get("roomId"));
		AssertJUnit.assertEquals(expectedNotifyStatus, model.get("notify"));
		AssertJUnit.assertEquals(expectedIsRootProject, model.get("isRootProject"));
		AssertJUnit.assertEquals(expectedProjectId, model.get("projectId"));
		TreeMap<String, String> actualRoomIdList = (TreeMap<String, String>)model.get("roomIdList");
		AssertJUnit.assertEquals(expectedRoomIdList.size(), actualRoomIdList.size());
		AssertJUnit.assertEquals(expectedRoomIdList.get(availableRoomId), actualRoomIdList.get(availableRoomId));
	}	
	
}