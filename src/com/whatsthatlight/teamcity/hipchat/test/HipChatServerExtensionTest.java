package com.whatsthatlight.teamcity.hipchat.test;

import java.net.URISyntaxException;
import java.util.ArrayList;

import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.TriggeredBy;
import jetbrains.buildServer.users.SUser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;
import com.whatsthatlight.teamcity.hipchat.HipChatNotificationProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomNotification;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HipChatServerExtensionTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}

	@Test
	public void testBuildStartedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.START;
		String expectedMessageFormat = HipChatMessageFormat.TEXT;

		// Callback closure
		final ArrayList<HipChatRoomNotification> notifications = new ArrayList<HipChatRoomNotification>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, notifications);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SUser user = mock(SUser.class);
		when(user.getDescriptiveName()).thenReturn(expectedTriggerBy);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getUser()).thenReturn(user);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		SBuildServer server = null;
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, notifications.size());
		HipChatRoomNotification actualNotification = notifications.get(0);
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedBuildName));
		assertTrue(actualNotification.message.contains(expectedStartMessage));
		assertTrue(actualNotification.message.contains(expectedBuildNumber));
		assertTrue(actualNotification.message.contains(expectedTriggerBy));
	}
	
	@Test
	@Ignore
	public void testActualBuildStartedEvent() throws URISyntaxException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("https://api.hipchat.com/v2/");
		configuration.setApiToken("Mi7JkzdiT5wYZ0OAMrjFQzeAP7B5DfcYQu2wXp8e");
		configuration.setRoomId("432380");
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		SBuildServer server = null;
		HipChatNotificationProcessor processor = new HipChatNotificationProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildStarted(build);
	}
	
	@Test
	@Ignore
	public void testActualBuildSucceededEvent() throws URISyntaxException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("https://api.hipchat.com/v2/");
		configuration.setApiToken("Mi7JkzdiT5wYZ0OAMrjFQzeAP7B5DfcYQu2wXp8e");
		configuration.setRoomId("432380");
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		Status status = Status.NORMAL;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		HipChatNotificationProcessor processor = new HipChatNotificationProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
	}

	@Test
	@Ignore
	public void testActualBuildFailedEvent() throws URISyntaxException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("https://api.hipchat.com/v2/");
		configuration.setApiToken("Mi7JkzdiT5wYZ0OAMrjFQzeAP7B5DfcYQu2wXp8e");
		configuration.setRoomId("432380");
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = null;
		HipChatNotificationProcessor processor = new HipChatNotificationProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
	}
	
	private interface Callback {
		
		void invoke(HipChatRoomNotification notification);

	}
	
	private class HipChatRoomNotificationCallback implements Callback {

		ArrayList<HipChatRoomNotification> notifications = new ArrayList<HipChatRoomNotification>();
		Object waitObject = new Object();
		
		public HipChatRoomNotificationCallback(Object waitObject, ArrayList<HipChatRoomNotification> notifications) {
			this.waitObject = waitObject;
			this.notifications = notifications;
		}
		
		public void invoke(HipChatRoomNotification notification) {
			notifications.add(notification);
			synchronized (waitObject) {
				waitObject.notify();
			}
		}
	};
	
	private class MockHipChatNotificationProcessor extends HipChatNotificationProcessor {

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
		public void process(HipChatRoomNotification notification) {
			try {
				this.callback.invoke(notification);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}
}
