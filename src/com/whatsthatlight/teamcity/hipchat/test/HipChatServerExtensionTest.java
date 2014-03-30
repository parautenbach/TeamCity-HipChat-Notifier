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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

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
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;
import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomNotification;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HipChatServerExtensionTest {

	private static String apiUrl;
	private static String apiToken;
	private static String actualRoomId;

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
		apiUrl = "https://api.hipchat.com/v2/";
		apiToken = "notatoken";
		actualRoomId = "000000";
	}

	@Test
	public void testRegisterDoesNotRaiseExceptionWhenEmoticonRetrievalFails() throws URISyntaxException {		
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("http://example.com");
		configuration.setApiToken("no_such_token");
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.register();
	}
	
	@Test
	public void testServerStartupEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedStartMessage = "Build server started.";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		
		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		extension.serverStartup();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, callbackObject.roomId);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedStartMessage));
	}

	@Test
	public void testServerStartupEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		
		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setServerStartupStatus(false);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		extension.serverStartup();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}
	
	@Test
	public void testServerShutdownEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedStartMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		
		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		extension.serverShutdown();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, callbackObject.roomId);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedStartMessage));
	}
	
	@Test
	public void testServerShutdownEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		
		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setServerShutdownStatus(false);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor);
		extension.serverShutdown();
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}
	
	@Test
	public void testBuildStartedEventAndMessageDetails() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.INFO;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";
		String expectedContributors = String.format("%s, %s, %s", expectedUser2Name, expectedUser3Name, expectedUser1Name);

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
		when(build.getBuildTypeId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
				
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn(expectedUser1Name);
		users.add(user1);
		SUser user2 = mock(SUser.class);
		when(user2.getDescriptiveName()).thenReturn(expectedUser2Name);
		users.add(user2);
		SUser user3 = mock(SUser.class);
		when(user3.getDescriptiveName()).thenReturn(expectedUser3Name);
		users.add(user3);
		when(userSet.getUsers()).thenReturn(users);
		when(build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSet);
				
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
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
		assertTrue(actualNotification.message.contains(String.format("buildId=%s", expectedBuildId)));
		assertTrue(actualNotification.message.contains(String.format("buildTypeId=%s", expectedBuildTypeId)));
		System.out.println(String.format("Expected: %s", expectedContributors));
		assertTrue(actualNotification.message.contains(expectedContributors));
		assertTrue(actualNotification.message.contains("<img"));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
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
		configuration.getEvents().setBuildStartedStatus(false);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}
	
	@Test
	public void testBuildStartedEventForSubprojectWithImplicitDefaultConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project, hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubprojectWithExplicitDefaultConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String configuredRoomId = "default";
		String rootUrl = "http://example.com";
		// If they match we can't tell that the correct one was used.
		assertNotEquals(configuredRoomId, expectedDefaultRoomId);

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
		when(build.getProjectExternalId()).thenReturn(expectedProjectId);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We set this project to explicitly use the defaults.
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubprojectWithImplicitNoneConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedDefaultRoomId = null;
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

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);		
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		
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
		// We don't set the configuration for the project, hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
				
		// Test
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitNoneConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String configuredRoomId = "none";
		// If they match we can't tell that the correct one was used.
		assertNotEquals(configuredRoomId, expectedDefaultRoomId);

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>)
		mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
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
		// We set this project to explicitly have no configuration (or, room notifications are disabled for this particular project).
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitParentConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String defaultRoomId = "default_room_id";
		String expectedRoomId = "parent_room_id";
		String parentProjectId = "parent_project";
		String projectId = "project";
		String rootUrl = "http://example.com";
		
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(projectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(parentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent project's configuration exists explicitly, but this project's doesn't.
		HipChatProjectConfiguration parentProjectConfiguration = new HipChatProjectConfiguration(parentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentProjectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitParentConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String defaultRoomId = "default_room_id";
		String expectedRoomId = "parent_room_id";
		String parentProjectId = "parent_project";
		String projectId = "project";
		// Explicitly inherit from parent
		String projectRoomId = "parent";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(projectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(parentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent project's configuration exists explicitly, but this project's doesn't.
		HipChatProjectConfiguration parentProjectConfiguration = new HipChatProjectConfiguration(parentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentProjectConfiguration);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(projectId, projectRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitDefaultConfiguration() throws InterruptedException, URISyntaxException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: :: Test Subsubproject Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String rootProjectId = "_Root";
		String parentProjectId = "parent";
		String rootUrl = "http://example.com";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(rootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project or parent project, hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitDefaultConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: :: Test Subsubproject Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String rootProjectId = "_Root";
		String parentProjectId = "parent";
		String configuredRoomId = "default";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(rootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project or parent project, hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);		
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitParentConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String defaultRoomId = "default_room_id";
		String expectedRoomId = "parent_parent_room_id";
		String parentProjectId = "parent_project";
		String projectId = "project";
		String parentParentProjectId = "parent_parent_project";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(parentParentProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		when(parentProject.getParentProjectId()).thenReturn(parentParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(projectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(parentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent's parent project configuration exists explicitly, but neither this project nor the parent's exist.
		HipChatProjectConfiguration parentParentProjectConfiguration = new HipChatProjectConfiguration(parentParentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentParentProjectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitParentConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String defaultRoomId = "default_room_id";
		String expectedRoomId = "parent_parent_room_id";
		String parentProjectId = "parent_project";
		String projectId = "project";
		String parentParentProjectId = "parent_parent_project";
		String projectRoomId = "parent";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(parentParentProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		when(parentProject.getParentProjectId()).thenReturn(parentParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(projectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(parentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent's parent project configuration exists explicitly, but neither this project nor the parent's exist.
		HipChatProjectConfiguration parentParentProjectConfiguration = new HipChatProjectConfiguration(parentParentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentParentProjectConfiguration);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(projectId, projectRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitNoneConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedDefaultRoomId = null;
		String expectedProjectId = "project1";
		String rootProjectId = "_Root";
		String parentProjectId = "parent_project";

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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>)
		mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(rootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProjectId()).thenReturn(rootProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project, hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
				
		// Test
		assertEquals(0, callbacks.size());
	}
	
	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitNoneConfiguration() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedDefaultRoomId = null;
		String expectedProjectId = "project1";
		String rootProjectId = "_Root";
		String parentProjectId = "parent_project";
		String configuredRoomId = "none";
		boolean expectedNotificationStatus = true;
		String expectedBuildTypeId = "42";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Object waitObject = new Object();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(waitObject, callbacks);
		
		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getBuildTypeId()).thenReturn(expectedBuildTypeId);

		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>)
		mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentParentProject = mock(SProject.class);
		when(parentParentProject.getProjectId()).thenReturn(rootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(parentProjectId);
		when(parentProject.getParentProjectId()).thenReturn(rootProjectId);
		when(parentProject.getParentProject()).thenReturn(parentParentProject);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We set the configuration for the project explicitly
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		
		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.changesLoaded(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
				
		// Test
		assertEquals(0, callbacks.size());
	}
	
	@Test
	public void testBuildSuccessfulEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "successful";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.SUCCESS;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		Status status = Status.NORMAL;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
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
		assertTrue(actualNotification.message.contains(expectedHtmlImageTag));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildSuccessfulEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
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
		Status status = Status.NORMAL;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setBuildSuccessfulStatus(false);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildFailedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedFailedMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
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
		assertTrue(actualNotification.message.contains(expectedHtmlImageTag));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildFailedEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
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
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setBuildFailedStatus(false);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildFinished(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildInterruptedEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedInterruptedMessage = "cancelled";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggeredBy = "Test User";
		String expectedCanceledBy = "Cancelled by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.WARNING;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SUser user = mock(SUser.class);
		when(user.getDescriptiveName()).thenReturn(expectedCanceledBy);
		UserModel userModel = mock(UserModel.class);
		when(userModel.findUserById(0)).thenReturn(user);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getUserModel()).thenReturn(userModel);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);

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
		assertTrue(actualNotification.message.contains(expectedHtmlImageTag));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testServerStartupAndShutdownEvent() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedServerStartupMessage = "Build server started.";
		String expectedServerShutdownMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
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
		configuration.setDefaultRoomId(actualRoomId);
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
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);
		String expectedTriggerBy = "A Test User";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String expectedBuildNumber = "0.0.0.0";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		
		// Mocks and other dependencies
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn("test :: test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getBuildTypeId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.register();
		extension.changesLoaded(build);
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
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		
		// Mocks and other dependencies
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		Status status = Status.NORMAL;
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getBuildStatus()).thenReturn(status);
		when(build.getBuildTypeId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.register();
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
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		
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
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		when(build.getBuildTypeId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.register();
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
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		
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
		when(build.getBuildTypeId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		SUser user = mock(SUser.class);
		when(user.getDescriptiveName()).thenReturn(expectedCanceledBy);
		UserModel userModel = mock(UserModel.class);
		when(userModel.findUserById(0)).thenReturn(user);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getUserModel()).thenReturn(userModel);
		when(server.getRootUrl()).thenReturn(rootUrl);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.register();
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

		// TODO: Record and test notify status
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
	

	@Test
	public void testBuildInterruptedEventDisabled() throws URISyntaxException, InterruptedException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggeredBy = "Test User";
		String expectedCanceledBy = "Cancelled by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		
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
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getUserModel()).thenReturn(userModel);
		when(server.getProjectManager()).thenReturn(projectManager);
		
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setBuildInterruptedStatus(false);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor);
		extension.buildInterrupted(build);
		synchronized (waitObject) {
			waitObject.wait(1000);
		}
		
		// Test
		assertEquals(0, callbacks.size());
	}

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
