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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildAgent;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.TriggeredBy;
import jetbrains.buildServer.serverSide.userChanges.CanceledInfo;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import com.whatsthatlight.teamcity.hipchat.HipChatApiResultLinks;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticon;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticonCache;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticonSet;
import com.whatsthatlight.teamcity.hipchat.HipChatEmoticons;
import com.whatsthatlight.teamcity.hipchat.HipChatEventConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageColour;
import com.whatsthatlight.teamcity.hipchat.HipChatMessageFormat;
import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatNotificationMessageTemplates;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatRoomNotification;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;
import com.whatsthatlight.teamcity.hipchat.TeamCityEvent;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HipChatServerExtensionTest {

	private static String apiUrl;
	private static String apiToken;
	private static String actualRoomId;

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
		apiUrl = "https://api.hipchat.com/v2/";
		apiToken = "notoken";
		actualRoomId = "00000";
	}

	@Test
	public void testRegisterDoesNotRaiseExceptionWhenEmoticonRetrievalFails() throws URISyntaxException, IOException {
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("http://example.com/");
		configuration.setApiToken("no_such_token");
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
	}

	@Test
	public void testServerStartupEvent() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedStartMessage = "Build server started.";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;

		// Ensure we get the default template.
		File template = new File("hipchat/serverStartupTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		extension.serverStartup();
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testServerStartupEventDisabled() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setServerStartupStatus(false);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		extension.serverStartup();
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testServerShutdownEvent() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedStartMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;

		// Ensure we get the default template.
		File template = new File("hipchat/serverShutdownTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		extension.serverShutdown();
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testServerShutdownEventDisabled() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.getEvents().setServerShutdownStatus(false);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		extension.serverShutdown();
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventAndMessageDetails() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration (fŏŏbārbaß)";
		String expectedStartMessage = "started";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
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
		String expectedBranchName = "feature1";

		// Ensure we get the default template.
		File template = new File("hipchat/buildStartedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
		assertTrue(actualNotification.message.contains(expectedBranchName));
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedBranchFilterMatch() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		boolean branchFilterEnabled = true;
		String branchFilterRegex = "feature1|feature2|feature3";
		String expectedBranchName = "feature2";
		String expectedBuildName = "Test Project :: Test Build Configuration (fŏŏbārbaß)";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";

		// Ensure we get the default template.
		File template = new File("hipchat/buildStartedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setBranchFilterEnabledStatus(branchFilterEnabled);
		configuration.setBranchFilterRegex(branchFilterRegex);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
	}

	@Test
	public void testBuildStartedBranchFilterNoMatch() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		boolean branchFilterEnabled = true;
		String branchFilterRegex = "feature1|feature2|feature3";
		String expectedBranchName = "master";
		String expectedBuildName = "Test Project :: Test Build Configuration (fŏŏbārbaß)";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";

		// Ensure we get the default template.
		File template = new File("hipchat/buildStartedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setBranchFilterEnabledStatus(branchFilterEnabled);
		configuration.setBranchFilterRegex(branchFilterRegex);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
	}

	@Test
	public void testBuildStartedBranchFilterEnabledNoBranch() throws URISyntaxException, InterruptedException, IOException {
		// E.g. SVN
		// Test parameters
		Branch branch = null;
		boolean branchFilterEnabled = true;
		String expectedBuildName = "Test Project :: Test Build Configuration (fŏŏbārbaß)";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";

		// Ensure we get the default template.
		File template = new File("hipchat/buildStartedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setBranchFilterEnabledStatus(branchFilterEnabled);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
	}

	@Test
	public void testBuildStartedEventWithAdditionalParameters() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String additionalParameterKey = "some.variable";
		String expectedAdditionalParameterValue = "additionalParameter";
		String additionalAgentParameterKey = "agent.variable";
		String expectedAdditionalAgentParameterValue = "additionalAgentParameter";
		String expectedMessage = String.format("%s %s", expectedAdditionalParameterValue, expectedAdditionalAgentParameterValue);
		String templateString = String.format("${.data_model[\"%s\"]} ${.data_model[\"%s\"]}", additionalParameterKey, additionalAgentParameterKey);
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";
		String expectedBranchName = "feature1";

		// Ensure we get the default template.
		File templateFile = new File("hipchat/buildStartedTemplate.ftl");
		if (templateFile.exists()) {
			assertTrue(templateFile.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Set additional parameters
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		additionalParameters.put(additionalParameterKey, expectedAdditionalParameterValue);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		HashMap<String, String> additionalAgentParameters = new HashMap<String, String>();
		additionalAgentParameters.put(additionalAgentParameterKey, expectedAdditionalAgentParameterValue);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(additionalAgentParameters);
		when(build.getAgent()).thenReturn(agent);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");

		// Set the template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, templateString);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		Template template = config.getTemplate(templateName);
		HipChatNotificationMessageTemplates templates = mock(HipChatNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.BUILD_STARTED)).thenReturn(template);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertEquals(expectedMessage, actualNotification.message);
	}

	@Test
	public void testBuildFailedEventWithBuildStatistics() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		int expectedPassedTests = 4;
		int expectedFailedTests = 3;
		int expectedNewFailedTotalTests = 2;
		int expectedIgnoredTests = 1;
		int expectedTotalTests = expectedPassedTests + expectedFailedTests + expectedIgnoredTests;
		int expectedTotalTestDuration = 69;
		String expectedStatisticKey = "statKey";
		int expectedStatisticValue = 42;
		String expectedMessage = String.format("%s: %s/%s/%s/%s (%s); %s", 
				expectedTotalTests, 
				expectedPassedTests, 
				expectedFailedTests, 
				expectedNewFailedTotalTests,
				expectedIgnoredTests,
				expectedTotalTestDuration,
				expectedStatisticValue);
		String templateString = String.format("${.data_model[\"noOfTests\"]}: "
				+ "${.data_model[\"noOfPassedTests\"]}/"
				+ "${.data_model[\"noOfFailedTests\"]}/"
				+ "${.data_model[\"noOfNewFailedTests\"]}/"
				+ "${.data_model[\"noOfIgnoredTests\"]} "
				+ "(${.data_model[\"durationOfTests\"]}); "
				+ "${.data_model[\"stats.%s\"]}", expectedStatisticKey);
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";
		String expectedBranchName = "feature1";

		// Ensure we get the default template.
		File templateFile = new File("hipchat/buildStartedTemplate.ftl");
		if (templateFile.exists()) {
			assertTrue(templateFile.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(expectedTotalTests);
		when(fullStatistics.getPassedTestCount()).thenReturn(expectedPassedTests);
		when(fullStatistics.getFailedTestCount()).thenReturn(expectedFailedTests);
		when(fullStatistics.getNewFailedCount()).thenReturn(expectedNewFailedTotalTests);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(expectedIgnoredTests);
		when(fullStatistics.getTotalDuration()).thenReturn((long) expectedTotalTestDuration);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		statisticValues.put(expectedStatisticKey, new BigDecimal(expectedStatisticValue));
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Set additional parameters
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		HashMap<String, String> additionalAgentParameters = new HashMap<String, String>();
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(additionalAgentParameters);
		when(build.getAgent()).thenReturn(agent);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");

		// Set the template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, templateString);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		Template template = config.getTemplate(templateName);
		HipChatNotificationMessageTemplates templates = mock(HipChatNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.BUILD_STARTED)).thenReturn(template);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertEquals(expectedMessage, actualNotification.message);
	}

	@Test
	public void testBuildStartedEventMessageContainsEmoticon() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedEmoticonUrl = "http://example.com/";
		String templateString = String.format("<img src=\"${emoticonUrl}\">", expectedEmoticonUrl);
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedUser2Name = "bar";
		String expectedUser3Name = "baz";
		String expectedBranchName = "feature1";
		String emoticonUrl = "http://example.com/emoticon";

		// Ensure we get the default template.
		File templateFile = new File("hipchat/buildStartedTemplate.ftl");
		if (templateFile.exists()) {
			assertTrue(templateFile.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		List<HipChatEmoticon> items = new ArrayList<HipChatEmoticon>();
		for (String item : HipChatEmoticonSet.POSITIVE) {
			HipChatEmoticon emoticon = new HipChatEmoticon(item, null, item, emoticonUrl);
			items.add(emoticon);
		}

		HipChatApiResultLinks links = new HipChatApiResultLinks(null, null, null);
		HipChatEmoticons expectedEmoticons = new HipChatEmoticons(items, 0, items.size(), links);

		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		HashMap<String, String> additionalParameters = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(additionalParameters);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);

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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback, expectedEmoticons);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatEmoticonCache emoticonCache = new HipChatEmoticonCache(processor);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Set the template
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, templateString);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		Template template = config.getTemplate(templateName);
		HipChatNotificationMessageTemplates templates = mock(HipChatNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.BUILD_STARTED)).thenReturn(template);

		// Execute
		emoticonCache.reload();
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertTrue(actualNotification.message.contains(expectedEmoticonUrl));
	}

	@Test
	public void testBuildProcessingWhenExceptionRaised() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedExceptionText = "Could not process build event";
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedBranchName = "feature1";

		// Logger
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn(expectedUser1Name);
		users.add(user1);
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
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = mock(HipChatNotificationMessageTemplates.class);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		logger.removeAppender(appender);

		// Test
		assertFalse(event.isSet());
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}

	@Test
	public void testBuildStartedEventNullParentProjectConfiguration() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedRoomId = "parent";
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "project2";
		String expectedRootProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedBranchName = "feature1";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn(expectedUser1Name);
		users.add(user1);
		when(userSet.getUsers()).thenReturn(users);
		when(build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSet);

		SProject rootProject = mock(SProject.class);
		when(rootProject.getProjectId()).thenReturn(expectedRootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		when(parentProject.getParentProject()).thenReturn(rootProject);
		when(parentProject.getParentProjectId()).thenReturn(expectedRootProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, expectedRoomId, true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
	}

	@Test
	public void testBuildStartedEventParentProjectConfigurationNoRoom() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedParentRoomId = "none";
		String expectedRoomId = "parent";
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "project2";
		String expectedRootProjectId = "_Root";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;
		String expectedUser1Name = "foo";
		String expectedBranchName = "feature1";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn(expectedBranchName);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getBranch()).thenReturn(branch);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn((long) expectedBuildId);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn(expectedUser1Name);
		users.add(user1);
		when(userSet.getUsers()).thenReturn(users);
		when(build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSet);

		SProject rootProject = mock(SProject.class);
		when(rootProject.getProjectId()).thenReturn(expectedRootProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		when(parentProject.getParentProject()).thenReturn(rootProject);
		when(parentProject.getParentProjectId()).thenReturn(expectedRootProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		ProjectManager projectManager = mock(ProjectManager.class);
		when(projectManager.findProjectById(any(String.class))).thenReturn(project);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn(rootUrl);
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir);
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, expectedRoomId, true);
		HipChatProjectConfiguration parentProjectConfiguration = new HipChatProjectConfiguration(expectedParentProjectId, expectedParentRoomId, true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setProjectConfiguration(projectConfiguration);
		configuration.setProjectConfiguration(parentProjectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
	}

	@Test
	public void testBuildStartedEventConfigurationIsNull() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testBuildStartedEventPersonalBuild() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.isPersonal()).thenReturn(true);
		when(build.getBuildType()).thenReturn(buildType);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setBuildStartedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(events);
		configuration.setDisabledStatus(false);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);

		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn("bar");
		when(build.getTriggeredBy()).thenReturn(triggeredBy);

		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn("foo");
		users.add(user1);
		when(userSet.getUsers()).thenReturn(users);
		when(build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSet);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testBuildStartedEventAllDisabled() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setBuildStartedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(events);
		configuration.setDisabledStatus(true);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);

		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn("bar");
		when(build.getTriggeredBy()).thenReturn(triggeredBy);

		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);

		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		Set<SUser> users = new LinkedHashSet<SUser>();
		SUser user1 = mock(SUser.class);
		when(user1.getDescriptiveName()).thenReturn("foo");
		users.add(user1);
		when(userSet.getUsers()).thenReturn(users);
		when(build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD)).thenReturn(userSet);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testBuildStartedEventDisabled() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitDefaultConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		// We don't set the configuration for the project, hence it must use the
		// defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitDefaultConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		event.clear();
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitNoneConfiguration() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Subproject :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedDefaultRoomId = null;
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);

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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project, hence it must use the
		// defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitNoneConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		when(build.getBuildTypeExternalId()).thenReturn("");
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We set this project to explicitly have no configuration (or, room
		// notifications are disabled for this particular project).
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubprojectWithImplicitParentConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

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
		// The parent project's configuration exists explicitly, but this
		// project's doesn't.
		HipChatProjectConfiguration parentProjectConfiguration = new HipChatProjectConfiguration(parentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentProjectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubprojectWithExplicitParentConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent project's configuration exists explicitly, but this
		// project's doesn't.
		HipChatProjectConfiguration parentProjectConfiguration = new HipChatProjectConfiguration(parentProjectId, expectedRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(parentProjectConfiguration);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(projectId, projectRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitDefaultConfiguration() throws InterruptedException, URISyntaxException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		// We don't set the configuration for the project or parent project,
		// hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitDefaultConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project or parent project,
		// hence it must use the defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedDefaultRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitParentConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent's parent project configuration exists explicitly, but
		// neither this project nor the parent's exist.
		HipChatProjectConfiguration parentParentProjectConfiguration = new HipChatProjectConfiguration(parentParentProjectId, expectedRoomId,
				expectedNotificationStatus);
		configuration.setProjectConfiguration(parentParentProjectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitParentConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		// The parent's parent project configuration exists explicitly, but
		// neither this project nor the parent's exist.
		HipChatProjectConfiguration parentParentProjectConfiguration = new HipChatProjectConfiguration(parentParentProjectId, expectedRoomId,
				expectedNotificationStatus);
		configuration.setProjectConfiguration(parentParentProjectConfiguration);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(projectId, projectRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		String actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedRoomId, actualDefaultRoomId);
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithImplicitNoneConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We don't set the configuration for the project, hence it must use the
		// defaults
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildStartedEventForSubsubprojectWithExplicitNoneConfiguration() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		// We set the configuration for the project explicitly
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(expectedProjectId, configuredRoomId, expectedNotificationStatus);
		configuration.setProjectConfiguration(projectConfiguration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.changesLoaded(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildSuccessfulEvent() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testLargeBuildId() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		Long expectedBuildId = new Long(99999);
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn(expectedBuildId);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
		assertEquals(1, callbacks.size());
		CallbackObject callbackObject = callbacks.get(0);
		HipChatRoomNotification actualNotification = callbackObject.notification;
		System.out.println(actualNotification);
		assertTrue(actualNotification.message.contains(expectedBuildId.toString()));
	}

	@Test
	public void testBuildSuccessfulEventFirstOnlyEnabled() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		history.add(mock(SFinishedBuild.class));
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildSuccessfulStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildSuccessfulEventFirstOnlyEnabledNoPreviousBuilds() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildSuccessfulStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildSuccessfulEventFirstOnlyEnabledNoPreviousBuildsForBranch() throws URISyntaxException, InterruptedException, IOException {
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
		String branchName = "test_branch";

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		Branch branch = mock(Branch.class);
		when(branch.getName()).thenReturn(branchName);
		when(branch.getDisplayName()).thenReturn(branchName);
		when(build.getBranch()).thenReturn(branch);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildSuccessfulStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildSuccessfulEventFirstOnlyDisabled() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.NORMAL);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		history.add(mock(SFinishedBuild.class));
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildSuccessfulStatus(false);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildSuccessfulEventFirstOnlyEnabledNoNotification() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildSuccessfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.NORMAL);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		history.add(mock(SFinishedBuild.class));
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildSuccessfulStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
	}

	@Test
	public void testBuildSuccessfulEventDisabled() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildSuccessfulEventConfigurationIsNull() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		Status status = Status.NORMAL;
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testBuildFailedEvent() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildFailedEventFirstOnlyEnabled() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.NORMAL);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		history.add(mock(SFinishedBuild.class));
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildFailedEventFirstOnlyEnabledNoPreviousBuilds() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildFailedEventFirstOnlyDisabled() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String branchName = "test_branch";
		String otherBranchName = "other_branch";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		Branch previousBranch = mock(Branch.class);
		when(previousBranch.getName()).thenReturn(branchName);
		when(previousBuild.getBranch()).thenReturn(previousBranch);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SFinishedBuild otherBuild = mock(SFinishedBuild.class);
		when(otherBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		Branch otherBranch = mock(Branch.class);
		when(otherBranch.getName()).thenReturn(otherBranchName);
		when(otherBuild.getBranch()).thenReturn(otherBranch);
		history.add(otherBuild);
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		Branch branch = mock(Branch.class);
		when(branch.getName()).thenReturn(branchName);
		when(build.getBranch()).thenReturn(branch);
		when(branch.getDisplayName()).thenReturn(branchName);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(false);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildFailedEventFirstOnlyDisabledOtherBranchInbetween() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedSuccessMessage = "failed";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		String expectedHtmlImageTag = "<img";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.ERROR;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String branchName = "test_branch";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedfulTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		Branch branch = mock(Branch.class);
		when(branch.getName()).thenReturn(branchName);
		when(build.getBranch()).thenReturn(branch);
		when(branch.getDisplayName()).thenReturn(branchName);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(false);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildFailedEventFirstOnlyEnabledNotificationForBranchWithLongHistory() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		Branch branch = mock(Branch.class);
		when(branch.getDisplayName()).thenReturn("test_branch");
		when(branch.getName()).thenReturn("test_branch");
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		Branch otherBranch = mock(Branch.class);
		when(otherBranch.getDisplayName()).thenReturn("other_branch");
		when(otherBranch.getName()).thenReturn("other_branch");
		when(previousBuild.getBranch()).thenReturn(otherBranch);
		when(previousBuild.getBuildId()).thenReturn((long) 1);
		history.add(previousBuild);
		SFinishedBuild previousPreviousBuild = mock(SFinishedBuild.class);
		when(previousPreviousBuild.getBuildStatus()).thenReturn(Status.NORMAL);
		when(previousPreviousBuild.getBranch()).thenReturn(branch);
		when(previousPreviousBuild.getBuildId()).thenReturn((long) 2);
		history.add(previousPreviousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		// When the build is finished, it is included in the history by the
		// build server, so we need to include it here too
		// to ensure we skip over it correctly.
		SFinishedBuild currentBuild = mock(SFinishedBuild.class);
		when(currentBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		when(currentBuild.getBuildId()).thenReturn((long) 0);

		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(build.getBuildStatus()).thenReturn(Status.FAILURE);
		when(build.getBranch()).thenReturn(branch);
		SBuildServer server = mock(SBuildServer.class);
		when(server.getProjectManager()).thenReturn(projectManager);
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
	}

	@Test
	public void testBuildFailedEventFirstOnlyEnabledNoNotification() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Triggered by: Test User";
		boolean expectedNotificationStatus = true;
		String expectedDefaultRoomId = "room_id";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";

		// Ensure we get the default template.
		File template = new File("hipchat/buildFailedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies
		SFinishedBuild previousBuild = mock(SFinishedBuild.class);
		when(previousBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		List<SFinishedBuild> history = new ArrayList<SFinishedBuild>();
		history.add(mock(SFinishedBuild.class));
		history.add(previousBuild);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		when(buildType.getHistory()).thenReturn(history);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);

		// When the build is finished, it is included in the history by the
		// build server, so we need to include it here too
		// to ensure we skip over it correctly.
		SFinishedBuild currentBuild = mock(SFinishedBuild.class);
		when(currentBuild.getBuildStatus()).thenReturn(Status.FAILURE);
		when(currentBuild.getBuildId()).thenReturn((long) 0);

		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		when(server.getRootUrl()).thenReturn("");
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Configuration
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setOnlyAfterFirstBuildFailedStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setEvents(events);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
	}

	@Test
	public void testBuildFailedEventDisabled() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
	}

	@Test
	public void testBuildFailedEventConfigurationIsNull() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		Status status = Status.FAILURE;
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.getBuildStatus()).thenReturn(status);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildFinished(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testBuildInterruptedEvent() throws URISyntaxException, InterruptedException, IOException {
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

		// Ensure we get the default template.
		File template = new File("hipchat/buildInterruptedTemplate.ftl");
		if (template.exists()) {
			assertTrue(template.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		when(build.getProjectExternalId()).thenReturn("");
		when(build.getBuildTypeExternalId()).thenReturn("");
		when(build.getBuildId()).thenReturn((long) 0);
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(new HashMap<String, String>());
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		SBuildAgent agent = mock(SBuildAgent.class);
		when(agent.getAvailableParameters()).thenReturn(new HashMap<String, String>());
		when(build.getAgent()).thenReturn(agent);
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
		BuildStatistics fullStatistics = mock(BuildStatistics.class);
		when(fullStatistics.getAllTestCount()).thenReturn(0);
		when(fullStatistics.getPassedTestCount()).thenReturn(0);
		when(fullStatistics.getFailedTestCount()).thenReturn(0);
		when(fullStatistics.getNewFailedCount()).thenReturn(0);
		when(fullStatistics.getIgnoredTestCount()).thenReturn(0);
		when(fullStatistics.getTotalDuration()).thenReturn((long) 0);
		when(build.getFullStatistics()).thenReturn(fullStatistics);		
		Map<String, BigDecimal> statisticValues = new HashMap<String, BigDecimal>();
		when(build.getStatisticValues()).thenReturn(statisticValues);

		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildInterrupted(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertTrue(event.isSet());
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
	public void testBuildInterruptedEventConfigurationIsNull() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";

		// Mocks and other dependencies
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildInterrupted(build);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testServerStartupAndShutdownEvent() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedServerStartupMessage = "Build server started.";
		String expectedServerShutdownMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";

		// Ensure we get the default template.
		File startuptemplate = new File("hipchat/serverStartupTemplate.ftl");
		if (startuptemplate.exists()) {
			assertTrue(startuptemplate.delete());
		}
		File shutdownTemplate = new File("hipchat/serverShutdownTemplate.ftl");
		if (shutdownTemplate.exists()) {
			assertTrue(shutdownTemplate.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies, and the extension
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		HipChatRoomNotification actualNotification = null;
		String actualDefaultRoomId = null;
		CallbackObject callbackObject = null;

		// Execute start-up
		extension.serverStartup();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());

		// Execute shutdown
		event.clear();
		extension.serverShutdown();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());

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
	public void testServerStartupAndShutdownEventUsingDefaultBuildEventRoomId() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedServerStartupMessage = "Build server started.";
		String expectedServerShutdownMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String expectedDefaultRoomId = "room_id";

		// Ensure we get the default template.
		File startuptemplate = new File("hipchat/serverStartupTemplate.ftl");
		if (startuptemplate.exists()) {
			assertTrue(startuptemplate.delete());
		}
		File shutdownTemplate = new File("hipchat/serverShutdownTemplate.ftl");
		if (shutdownTemplate.exists()) {
			assertTrue(shutdownTemplate.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies, and the extension
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(expectedDefaultRoomId);
		configuration.setServerEventRoomId(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		HipChatRoomNotification actualNotification = null;
		String actualDefaultRoomId = null;
		CallbackObject callbackObject = null;

		// Execute
		extension.serverStartup();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());
		extension.serverShutdown();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());

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
	public void testServerStartupAndShutdownEventUsingServerEventRoomId() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedServerStartupMessage = "Build server started.";
		String expectedServerShutdownMessage = "Build server shutting down.";
		boolean expectedNotificationStatus = true;
		String expectedMessageColour = HipChatMessageColour.NEUTRAL;
		String expectedMessageFormat = HipChatMessageFormat.HTML;
		String defaultRoomId = "room_id1";
		String expectedServerEventRoomId = "room_id2";

		// Ensure we get the default template.
		File startuptemplate = new File("hipchat/serverStartupTemplate.ftl");
		if (startuptemplate.exists()) {
			assertTrue(startuptemplate.delete());
		}
		File shutdownTemplate = new File("hipchat/serverShutdownTemplate.ftl");
		if (shutdownTemplate.exists()) {
			assertTrue(shutdownTemplate.delete());
		}

		// Callback closure
		final ArrayList<CallbackObject> callbacks = new ArrayList<CallbackObject>();
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

		// Mocks and other dependencies, and the extension
		MockHipChatNotificationProcessor processor = new MockHipChatNotificationProcessor(callback);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(expectedNotificationStatus);
		configuration.setDefaultRoomId(defaultRoomId);
		configuration.setServerEventRoomId(expectedServerEventRoomId);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		HipChatRoomNotification actualNotification = null;
		String actualDefaultRoomId = null;
		CallbackObject callbackObject = null;

		// Execute start-up
		extension.serverStartup();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());
		extension.serverShutdown();
		event.doWait(Constants.NO_EVENT_TIMEOUT);
		assertTrue(event.isSet());

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
		assertEquals(expectedServerEventRoomId, actualDefaultRoomId);
		// Shutdown
		callbackObject = callbacks.get(1);
		actualNotification = callbackObject.notification;
		actualDefaultRoomId = callbackObject.roomId;
		System.out.println(actualNotification);
		assertEquals(expectedMessageColour, actualNotification.color);
		assertEquals(expectedMessageFormat, actualNotification.messageFormat);
		assertEquals(expectedNotificationStatus, actualNotification.notify);
		assertTrue(actualNotification.message.contains(expectedServerShutdownMessage));
		assertEquals(expectedServerEventRoomId, actualDefaultRoomId);
	}

	@Test
	public void testStartupAndShutdownEventConfigurationIsNull() throws URISyntaxException, InterruptedException, IOException {
		// Mocks and other dependencies
		SBuildServer server = mock(SBuildServer.class);
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setEvents(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.serverStartup();
		extension.serverShutdown();

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
	}

	@Test
	public void testServerEventException() throws URISyntaxException, InterruptedException, IOException {
		// Test parameters
		String expectedExceptionText = "Error processing server event: SERVER_STARTUP";

		// Logger
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Appender appender = new WriterAppender(new PatternLayout("%m%n"), outputStream);
		logger.addAppender(appender);

		// Mocks and other dependencies, and the extension
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setServerStartupStatus(true);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setNotifyStatus(true);
		configuration.setEvents(events);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = mock(HipChatNotificationMessageTemplates.class);
		when(templates.readTemplate(TeamCityEvent.SERVER_STARTUP)).thenThrow(new IOException("Test exception"));
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);

		// Execute start-up
		extension.serverStartup();
		logger.removeAppender(appender);

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());

		// Test
		boolean exceptionFound = false;
		String logOutput = new String(outputStream.toByteArray());
		for (String line : logOutput.split("\n")) {
			if (line.contains(expectedExceptionText)) {
				exceptionFound = true;
				break;
			}
		}
		assertTrue(exceptionFound);
	}

	@Test
	public void testServerEventNullDefaultRoomId() throws URISyntaxException, InterruptedException, IOException {
		// Mocks and other dependencies, and the extension
		HipChatApiProcessor processor = mock(HipChatApiProcessor.class);
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(null);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);

		// Execute start-up
		extension.serverStartup();

		// Verifications
		verify(processor, times(0)).sendNotification(any(HipChatRoomNotification.class), anyString());
		;
	}

	@Test(enabled = false)
	public void testActualServerStartupAndShutdownEvents() throws URISyntaxException, IOException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);

		// Mocks and other dependencies
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(null, configuration, processor, templates, emoticonCache);
		extension.serverStartup();
		extension.serverShutdown();
	}

	@Test(enabled = false)
	public void testActualBuildStartedEvent() throws URISyntaxException, IOException {
		// Test parameters
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl(apiUrl);
		configuration.setApiToken(apiToken);
		configuration.setDefaultRoomId(actualRoomId);
		configuration.setNotifyStatus(true);
		String expectedTriggerBy = "A Test User (fŏŏbārbaß)";
		String expectedProjectId = "project1";
		String expectedParentProjectId = "_Root";
		String expectedBuildNumber = "0.0.0.0";
		String rootUrl = "http://example.com";
		String expectedBuildTypeId = "42";
		long expectedBuildId = 24;

		// Mocks and other dependencies
		Map<String, String> parametersMap = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(parametersMap);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn("test :: test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		when(build.getProjectExternalId()).thenReturn("bt0000");
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
		extension.changesLoaded(build);
	}

	@Test(enabled = false)
	public void testActualBuildSuccessfulEvent() throws URISyntaxException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
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
		Map<String, String> parametersMap = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(parametersMap);
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
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		when(build.getProjectExternalId()).thenReturn("bt0000");
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
		extension.buildFinished(build);
	}

	@Test(enabled = false)
	public void testActualBuildFailedEvent() throws URISyntaxException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
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
		Map<String, String> parametersMap = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(parametersMap);
		TriggeredBy triggeredBy = mock(TriggeredBy.class);
		when(triggeredBy.getAsString()).thenReturn(expectedTriggerBy);
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getFullName()).thenReturn(expectedBuildName);
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		when(build.getTriggeredBy()).thenReturn(triggeredBy);
		when(build.getBuildNumber()).thenReturn(expectedBuildNumber);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		when(build.getProjectExternalId()).thenReturn("bt0000");
		@SuppressWarnings("unchecked")
		UserSet<SUser> userSet = (UserSet<SUser>) mock(UserSet.class);
		when(build.getCommitters(any(SelectPrevBuildPolicy.class))).thenReturn(userSet);
		Status status = Status.FAILURE;
		when(build.getBuildStatus()).thenReturn(status);
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
		extension.buildFinished(build);
	}

	@Test(enabled = false)
	public void testActualBuildInterruptedEvent() throws URISyntaxException, IOException {
		// Test parameters
		String expectedBuildName = "Test Project :: Test Build Configuration";
		String expectedBuildNumber = "0.0.0.0";
		String expectedTriggerBy = "Test User (fŏŏbārbaß)";
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
		Map<String, String> parametersMap = new HashMap<String, String>();
		ParametersProvider parametersProvider = mock(ParametersProvider.class);
		when(parametersProvider.getAll()).thenReturn(parametersMap);
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
		when(build.getBuildTypeExternalId()).thenReturn(expectedBuildTypeId);
		when(build.getBuildId()).thenReturn(expectedBuildId);
		when(build.getParametersProvider()).thenReturn(parametersProvider);
		when(build.getProjectExternalId()).thenReturn("bt0000");
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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.register();
		extension.buildInterrupted(build);
	}

	@Test
	public void testBuildInterruptedEventDisabled() throws URISyntaxException, InterruptedException, IOException {
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
		final Event event = new Event();
		HipChatRoomNotificationCallback callback = new HipChatRoomNotificationCallback(event, callbacks);

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
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(".");
		HipChatNotificationMessageTemplates templates = new HipChatNotificationMessageTemplates(serverPaths);
		HipChatEmoticonCache emoticonCache = org.mockito.Mockito.mock(HipChatEmoticonCache.class);

		// Execute
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration, processor, templates, emoticonCache);
		extension.buildInterrupted(build);
		event.doWait(Constants.NO_EVENT_TIMEOUT);

		// Test
		assertFalse(event.isSet());
		assertEquals(0, callbacks.size());
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
		Event event;

		public HipChatRoomNotificationCallback(Event event, ArrayList<CallbackObject> callbacks) {
			this.event = event;
			this.callbacks = callbacks;
		}

		public void invoke(HipChatRoomNotification notification, String roomId) {
			callbacks.add(new CallbackObject(notification, roomId));
			this.event.set();
		}
	};

	private class MockHipChatNotificationProcessor extends HipChatApiProcessor {

		private Callback callback;
		private HipChatEmoticons emoticonsResult = null;
		private final Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

		public MockHipChatNotificationProcessor(HipChatConfiguration configuration) throws URISyntaxException {
			super(configuration);
		}

		public MockHipChatNotificationProcessor(Callback callback) throws URISyntaxException {
			this(new HipChatConfiguration());
			this.callback = callback;
		}

		public MockHipChatNotificationProcessor(Callback callback, HipChatEmoticons emoticonsResult) throws URISyntaxException {
			this(callback);
			this.emoticonsResult = emoticonsResult;
		}

		@Override
		public HipChatEmoticons getEmoticons(int startIndex) {
			if (this.emoticonsResult == null) {
				return super.getEmoticons(startIndex);
			} else {
				HipChatEmoticons result = this.emoticonsResult;
				this.emoticonsResult = null;
				return result;
			}
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
