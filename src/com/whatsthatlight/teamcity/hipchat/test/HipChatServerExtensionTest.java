package com.whatsthatlight.teamcity.hipchat.test;

import java.net.URISyntaxException;
import java.util.ArrayList;

import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.TriggeredBy;
import jetbrains.buildServer.serverSide.userChanges.CanceledInfo;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;
import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomNotification;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HipChatServerExtensionTest {

	private static String apiUrl;
	private static String apiToken;
	private static String roomId;

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
		apiUrl = "https://api.hipchat.com/v2/";
		apiToken = "notatoken";
		roomId = "000000";
	}

	@Test
	public void testBuildStartedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.INFO;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedStartMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitDefaultConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.INFO;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		SBuildServer server = null;
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedStartMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
		
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubprojectWithExplicitDefaultConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubprojectWithImplicitNoneConfiguration() {
		fail("Incomplete");
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitNoneConfiguration() {
		fail("Incomplete");
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitParentConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.INFO;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		SBuildServer server = null;
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedStartMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitParentConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitDefaultConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitDefaultConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitParentConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitParentConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitNoneConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitNoneConfiguration() {
		fail("Incomplete");
	}
	
	@Test
	public void testBuildSuccessfulEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "successful";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedEmoticonEndCharacter = ")";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.SUCCESS;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		Status status = Status.NORMAL;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedSuccessMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
		assertTrue(actualNotification.message.endsWith(expectedEmoticonEndCharacter));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildFailedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedFailedMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedEmoticonEndCharacter = ")";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedFailedMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
		assertTrue(actualNotification.message.endsWith(expectedEmoticonEndCharacter));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildInterruptedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedInterruptedMessage = "cancelled";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggeredBy = "Test User";
		String expectedCanceledBy = "Cancelled by: Test User";
		String expectedEmoticonEndCharacter = ")";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.WARNING;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";
		
		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		CanceledInfo canceledInfo = mock(CanceledInfo.class);
		when(canceledInfo.getUserId()).thenReturn((long) 0);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggeredBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getCanceledInfo()).thenReturn(canceledInfo);
		SUser user = mock(SUser.class);
		when(user.getDescriptiveName()).thenReturn(expectedCanceledBy);
		UserModel userModel = mock(UserModel.class);
		when(userModel.findUserById(0)).thenReturn(user);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getUserModel()).thenReturn(userModel);
		
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildInterrupted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedInterruptedMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggeredBy));
		assertTrue(actualNotification.message.endsWith(expectedEmoticonEndCharacter));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testServerStartupAndShutdownEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedServerStartupMessage = "Build server started.";
		String expectedServerShutdownMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies, and the extension
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		HipChatRoomNotification actualNotification = null;
		String actualDefaultRoomId = null;
		CallbackObject callbackObject = null;
		
		// Execute start-up
		extension.serverStartup();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
				
		// Execute shutdown
		extension.serverShutdown();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test start-up
		assertEquals(2, callbacks.size());
		// Start-up
		callbackObject = callbacks.get(0);
		actualNotification = callbackObject.notification;
		actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedServerStartupMessage));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);

		// Shutdown
		callbackObject = callbacks.get(1);
		actualNotification = callbackObject.notification;
		actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedServerShutdownMessage));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	@Ignore
	public void testActualServerStartupAndShutdownEvents() throws URISyntaxException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(roomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		extension.serverStartup();
		extension.serverShutdown();
	}
	
	@Test
	@Ignore
	public void testActualBuildStartedEvent() throws URISyntaxException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(roomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		SBuildServer server = null;
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
	}

	@Test
	@Ignore
	public void testActualBuildSuccessfulEvent() throws URISyntaxException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(roomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		Status status = Status.NORMAL;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
	}

	@Test
	@Ignore
	public void testActualBuildFailedEvent() throws URISyntaxException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(roomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
	}
	
	@Test
	@Ignore
	public void testActualBuildInterruptedEvent() throws URISyntaxException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		String expectedCanceledBy = "Cancel User";
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(roomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		CanceledInfo canceledInfo = mock(CanceledInfo.class);
		when(canceledInfo.getUserId()).thenReturn((long) 0);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getCanceledInfo()).thenReturn(canceledInfo);
		SUser user = mock(SUser.class);
		when(user.getDescriptiveName()).thenReturn(expectedCanceledBy);
		UserModel userModel = mock(UserModel.class);
		when(userModel.findUserById(0)).thenReturn(user);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getUserModel()).thenReturn(userModel);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildInterrupted(build);
	}
	
	private interface Callback {
		
		void invoke(HipChatRoomNotification notification, String roomId);

	}
	
	private class CallbackObject {
		
		public HipChatRoomNotification notification;
		public String roomId;

		public CallbackObject(HipChatRoomNotification notification, String roomId) {
			this.notification = notification;
			this.roomId = roomId;
		}
		
	}
	
	private class HipChatRoomNotificationCallback implements Callback {

		// TODO: Record notify status
		ArrayList<CallbackObject> callbacks;
		Object waitObject;
		
		public HipChatRoomNotificationCallback(Object waitObject, ArrayList<CallbackObject> callbacks) {
			this.waitObject = waitObject;
			this.callbacks = callbacks;
		}
		
		public void invoke(HipChatRoomNotification notification, String roomId) {
			callbacks.add(new CallbackObject(notification, roomId));
			synchronized (waitObject) {
				waitObject.notify();
			}
		}
	};
	
	private class MockHipChatNotificationProcessor extends HipChatApiProcessor {

		private Callback callback;
		private final Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
		
		public MockHipChatNotificationProcessor(HipChatConfiguration configuration) throws URISyntaxException {
			super(configuration);
		}
		
		public MockHipChatNotificationProcessor(Callback callback) throws URISyntaxException {
			this(new HipChatConfiguration());
			this.callback = callback;
		}
		
		@Override
		public void sendNotification(HipChatRoomNotification notification, String roomId) {
			try {
				this.callback.invoke(notification, roomId);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}
}
