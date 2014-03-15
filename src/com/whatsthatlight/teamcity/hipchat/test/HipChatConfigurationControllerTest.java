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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import org.apache.log4j.BasicConfigurator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatApiProcessor;
import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationController;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectConfiguration;

public class HipChatConfigurationControllerTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}

	@Test
	public void testProjectConfiguration() throws URISyntaxException, IOException {
		// Test parameters
		String expectedProjectId1 = "project1";
		String expectedProjectId2 = "project2";
		String expectedRoomId1 = "room1";
		String expectedRoomId2 = "room2";
		boolean expectedNotify1 = true;
		boolean expectedNotify2 = false;
		String expectedConfigDir = ".";
		
		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);
		
		// Prepare
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedProjectId1, expectedRoomId1, expectedNotify1));
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedProjectId2, expectedRoomId2, expectedNotify2));
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.saveConfiguration();
		
		// Execute
		configuration = new HipChatConfiguration();
		assertNull(configuration.getProjectConfiguration(expectedProjectId1));
		assertNull(configuration.getProjectConfiguration(expectedProjectId2));
		controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.loadConfiguration();
		
		// Test
		HipChatProjectConfiguration projectConfiguration1 = configuration.getProjectConfiguration(expectedProjectId1);
		assertEquals(expectedRoomId1, projectConfiguration1.getRoomId());
		assertEquals(expectedNotify1, projectConfiguration1.getNotifyStatus());
		HipChatProjectConfiguration projectConfiguration2 = configuration.getProjectConfiguration(expectedProjectId2);
		assertEquals(expectedRoomId2, projectConfiguration2.getRoomId());
		assertEquals(expectedNotify2, projectConfiguration2.getNotifyStatus());
	}
	
	@Test
	public void testConfigurationFileGetsCreatedWhenNoneExists() throws IOException, JDOMException, ParserConfigurationException, TransformerException, URISyntaxException {
		// Test parameters
		String expectedFileName = "hipchat.xml";
		String expectedApiUrlKey = "apiUrl";
		String expectedApiUrlValue = "http://example.com/";
		String expectedApiUrlDefaultValue = "https://api.hipchat.com/v2/";
		String expectedApiTokenKey = "apiToken";
		String expectedApiTokenValue = "admin_token";
		String expectedRoomIdKey = "defaultRoomId";
		String expectedRoomIdValue = "room_id";
		String expectedNotifyStatusKey = "notify";
		Boolean expectedNotifyStatusValue = true;
		String expectedDisabledStatusKey = "disabled";
		Boolean expectedDisabledStatusValue = false;
		String expectedProjectRoomMapKey = "projectRoom";
		String expectedConfigDir = ".";

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);

		// Pre-conditions
		File initialConfigFile = new File(expectedConfigDir, expectedFileName);
		initialConfigFile.delete();
		assertFalse(initialConfigFile.exists());
		HipChatConfiguration configuration = new HipChatConfiguration();
		assertEquals(expectedApiUrlDefaultValue, configuration.getApiUrl());
		assertNull(configuration.getApiToken());
		assertNull(configuration.getDefaultRoomId());
		assertFalse(configuration.getDefaultNotifyStatus());
		assertFalse(configuration.getDisabledStatus());
		assertEquals(0, configuration.getProjectRoomMap().size());

		// Execute
		// The config file must exist on disk after initialisation
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);		
		controller.initialise();
		File postRegistrationConfigFile = new File(expectedFileName);
		assertTrue(postRegistrationConfigFile.exists());

		// Check XML of the newly created config file
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(postRegistrationConfigFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedApiUrlDefaultValue, rootElement.getChildText(expectedApiUrlKey));
		assertNull(rootElement.getChildText(expectedApiTokenKey));
		assertNull(rootElement.getChildText(expectedRoomIdKey));
		assertFalse(Boolean.parseBoolean(rootElement.getChildText(expectedNotifyStatusKey)));
		assertFalse(Boolean.parseBoolean(rootElement.getChildText(expectedDisabledStatusKey)));
		assertNull(rootElement.getChildText(expectedProjectRoomMapKey));

		// And the instance values must still be the defaults
		assertEquals(expectedApiUrlDefaultValue, configuration.getApiUrl());
		assertNull(configuration.getApiToken());
		assertNull(configuration.getDefaultRoomId());
		assertFalse(configuration.getDefaultNotifyStatus());
		assertFalse(configuration.getDisabledStatus());
		assertEquals(0, configuration.getProjectRoomMap().size());

		// Now change and save the configuration
		configuration.setApiUrl(expectedApiUrlValue);
		configuration.setApiToken(expectedApiTokenValue);
		configuration.setDefaultRoomId(expectedRoomIdValue);
		configuration.setNotifyStatus(expectedNotifyStatusValue);
		configuration.setDisabledStatus(expectedDisabledStatusValue);
		controller.saveConfiguration();

		// Check XML of the saved config file
		builder = new SAXBuilder();
		document = builder.build(postRegistrationConfigFile);
		rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedRoomIdValue, rootElement.getChildText(expectedRoomIdKey));
		assertEquals(expectedNotifyStatusValue.toString(), rootElement.getChildText(expectedNotifyStatusKey));
		assertEquals(expectedDisabledStatusValue.toString(), rootElement.getChildText(expectedDisabledStatusKey));
		assertNull(rootElement.getChildText(expectedProjectRoomMapKey));

		// And also the values in memory
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		configuration.setDefaultRoomId(expectedRoomIdValue);
		configuration.setNotifyStatus(expectedNotifyStatusValue);
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
		assertEquals(0, configuration.getProjectRoomMap().size());
	}

	@Test
	public void testConfigurationGetsReadCorrectlyFromFileUponInitialisation() throws IOException, JDOMException, URISyntaxException {
		// Test parameters
		String expectedFileName = "hipchat.xml";
		String expectedApiUrlKey = "apiUrl";
		String expectedApiUrlValue = "http://example.com/";
		String expectedApiTokenKey = "apiToken";
		String expectedApiTokenValue = "admin_token";
		String expectedRoomIdKey = "defaultRoomId";
		String expectedRoomIdValue = "room_id";
		String expectedNotifyStatusKey = "notify";
		Boolean expectedNotifyStatusValue = true;
		String expectedDisabledStatusKey = "disabled";
		Boolean expectedDisabledStatusValue = false;
		String expectedProjectRoomMapKey = "projectRoom";
		String expectedConfigDir = ".";

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);

		// Pre-conditions
		// @formatter:off
		String configFileContent = 
				"<hipchat>" + 
				"<apiToken>" + expectedApiTokenValue + "</apiToken>" + 
				"<apiUrl>" + expectedApiUrlValue + "</apiUrl>" + 
				"<defaultRoomId>" + expectedRoomIdValue + "</defaultRoomId>" +
				"<notify>" + expectedNotifyStatusValue + "</notify>" +
				"<disabled>" + expectedDisabledStatusValue + "</disabled>" + 
				"</hipchat>";
		// @formatter:on
		File configFile = new File(expectedConfigDir, expectedFileName);
		configFile.delete();
		configFile.createNewFile();
		assertTrue(configFile.exists());
		FileWriter fileWriter = new FileWriter(configFile);
		fileWriter.write(configFileContent);
		fileWriter.flush();
		fileWriter.close();

		// Execute
		// The config file must must not have been overwritten on disk after
		// initialisation
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.initialise();
		File postInitConfigFile = new File(expectedConfigDir, expectedFileName);
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(postInitConfigFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedRoomIdValue, rootElement.getChildText(expectedRoomIdKey));
		assertEquals(expectedNotifyStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedNotifyStatusKey)));
		assertEquals(expectedDisabledStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedDisabledStatusKey)));
		assertNull(rootElement.getChildText(expectedProjectRoomMapKey));

		// Now check the loaded configuration
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		assertEquals(expectedRoomIdValue, configuration.getDefaultRoomId());
		assertEquals(expectedNotifyStatusValue, configuration.getDefaultNotifyStatus());
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
		assertEquals(0, configuration.getProjectRoomMap().size());
	}
	
	@Test
	public void testConfigurationWithProjectRoomMapGetsReadCorrectlyFromFileUponInitialisation() throws IOException, JDOMException, URISyntaxException {
		// Test parameters
		String expectedFileName = "hipchat.xml";
		String expectedApiUrlKey = "apiUrl";
		String expectedApiUrlValue = "http://example.com/";
		String expectedApiTokenKey = "apiToken";
		String expectedApiTokenValue = "admin_token";
		String expectedRoomIdKey = "defaultRoomId";
		String expectedRoomIdValue = "room_id";
		String expectedNotifyStatusKey = "notify";
		Boolean expectedNotifyStatusValue = true;
		String expectedDisabledStatusKey = "disabled";
		Boolean expectedDisabledStatusValue = false;
		String expectedProjectRoomMapKey = "projectRoom";
		String expectedProjectIdKey = "projectId";
		String expectedProjectIdValue = "project1";
		String expectedProjectRoomIdKey = "roomId";
		String expectedConfigDir = ".";

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);

		// Pre-conditions
		// @formatter:off
		String configFileContent = 
				"<hipchat>" + 
				"<apiToken>" + expectedApiTokenValue + "</apiToken>" + 
				"<apiUrl>" + expectedApiUrlValue + "</apiUrl>" + 
				"<defaultRoomId>" + expectedRoomIdValue + "</defaultRoomId>" +
				"<notify>" + expectedNotifyStatusValue + "</notify>" +
				"<disabled>" + expectedDisabledStatusValue + "</disabled>" + 
				"<projectRoom>" +
				  "<projectId>" +  expectedProjectIdValue + "</projectId>" + 
				  "<roomId>" +  expectedRoomIdValue + "</roomId>" + 
				  "<notify>" +  expectedNotifyStatusValue + "</notify>" + 
				"</projectRoom>" +
				"</hipchat>";
		// @formatter:on
		File configFile = new File(expectedConfigDir, expectedFileName);
		assertTrue(configFile.delete());
		assertTrue(configFile.createNewFile());
		assertTrue(configFile.exists());
		FileWriter fileWriter = new FileWriter(configFile);
		fileWriter.write(configFileContent);
		fileWriter.flush();
		fileWriter.close();

		// Execute
		// The config file must must not have been overwritten on disk after
		// initialisation
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.initialise();
		File postInitConfigFile = new File(expectedConfigDir, expectedFileName);
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(postInitConfigFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedRoomIdValue, rootElement.getChildText(expectedRoomIdKey));
		assertEquals(expectedNotifyStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedNotifyStatusKey)));
		assertEquals(expectedDisabledStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedDisabledStatusKey)));
		assertTrue(rootElement.getChildText(expectedProjectRoomMapKey) != null);
		Element projectRoomElement = rootElement.getChild(expectedProjectRoomMapKey);
		assertEquals(expectedProjectIdValue, projectRoomElement.getChildText(expectedProjectIdKey));
		assertEquals(expectedRoomIdValue, projectRoomElement.getChildText(expectedProjectRoomIdKey));
		assertEquals(expectedNotifyStatusValue, Boolean.parseBoolean(projectRoomElement.getChildText(expectedNotifyStatusKey)));

		// Now check the loaded configuration
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		assertEquals(expectedRoomIdValue, configuration.getDefaultRoomId());
		assertEquals(expectedNotifyStatusValue, configuration.getDefaultNotifyStatus());
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
		assertEquals(1, configuration.getProjectRoomMap().size());
		HipChatProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(expectedProjectIdValue);
		assertEquals(expectedRoomIdValue, projectConfiguration.getRoomId());
		assertEquals(expectedNotifyStatusValue, projectConfiguration.getNotifyStatus());
	}

	@Test
	public void testConfigurationWithEventsGetsReadCorrectlyFromFileUponInitialisation() throws IOException, JDOMException, URISyntaxException {
		// Test parameters
		String expectedFileName = "hipchat.xml";
		String expectedApiUrlKey = "apiUrl";
		String expectedApiUrlValue = "http://example.com/";
		String expectedApiTokenKey = "apiToken";
		String expectedApiTokenValue = "admin_token";
		String expectedRoomIdKey = "defaultRoomId";
		String expectedRoomIdValue = "room_id";
		String expectedNotifyStatusKey = "notify";
		Boolean expectedNotifyStatusValue = true;
		String expectedDisabledStatusKey = "disabled";
		Boolean expectedDisabledStatusValue = false;
		String expectedEventsKey = "events";
		String expectedbuildStartedKey = "buildStarted";
		String expectedbuildSuccessfulKey = "buildSuccessful";
		String expectedbuildFailedKey = "buildFailed";
		String expectedBuildInterruptedKey = "buildInterrupted";
		String expectedServerStartupKey = "serverStartup";
		String expectedServerShutdownKey = "serverShutdown";
		String expectedConfigDir = ".";
		HipChatConfiguration configuration = new HipChatConfiguration();
		boolean expectedBuildStartedValue = !configuration.getEvents().getBuildStartedStatus();
		boolean expectedBuildSuccessfulValue = !configuration.getEvents().getBuildSuccessfulStatus();
		boolean expectedBuildFailedValue = !configuration.getEvents().getBuildFailedStatus();
		boolean expectedBuildInterruptedValue = !configuration.getEvents().getBuildInterruptedStatus();
		boolean expectedServerStartupValue = !configuration.getEvents().getServerStartupStatus();
		boolean expectedServerShutdownValue = !configuration.getEvents().getServerShutdownStatus();

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);

		// Pre-conditions
		// @formatter:off
		String configFileContent = 
				"<hipchat>" + 
				"<apiToken>" + expectedApiTokenValue + "</apiToken>" + 
				"<apiUrl>" + expectedApiUrlValue + "</apiUrl>" + 
				"<defaultRoomId>" + expectedRoomIdValue + "</defaultRoomId>" +
				"<notify>" + expectedNotifyStatusValue + "</notify>" +
				"<disabled>" + expectedDisabledStatusValue + "</disabled>" + 
				"<events>" +
				  "<buildStarted>" +  expectedBuildStartedValue + "</buildStarted>" + 
				  "<buildSuccessful>" +  expectedBuildSuccessfulValue + "</buildSuccessful>" + 
				  "<buildFailed>" +  expectedBuildFailedValue + "</buildFailed>" + 
				  "<buildInterrupted>" +  expectedBuildInterruptedValue + "</buildInterrupted>" + 
				  "<serverStartup>" +  expectedServerStartupValue + "</serverStartup>" + 
				  "<serverShutdown>" +  expectedServerShutdownValue + "</serverShutdown>" + 
				"</events>" +
				"</hipchat>";
		// @formatter:on
		File configFile = new File(expectedConfigDir, expectedFileName);
		configFile.delete();
		configFile.createNewFile();
		assertTrue(configFile.exists());
		FileWriter fileWriter = new FileWriter(configFile);
		fileWriter.write(configFileContent);
		fileWriter.flush();
		fileWriter.close();

		// Execute
		// The config file must must not have been overwritten on disk after
		// initialisation
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.initialise();
		File postInitConfigFile = new File(expectedConfigDir, expectedFileName);
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(postInitConfigFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedRoomIdValue, rootElement.getChildText(expectedRoomIdKey));
		assertEquals(expectedNotifyStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedNotifyStatusKey)));
		assertEquals(expectedDisabledStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedDisabledStatusKey)));
		// Events
		Element eventsElement = rootElement.getChild(expectedEventsKey);
		assertEquals(expectedBuildStartedValue, Boolean.parseBoolean(eventsElement.getChildText(expectedbuildStartedKey)));
		assertEquals(expectedBuildSuccessfulValue, Boolean.parseBoolean(eventsElement.getChildText(expectedbuildSuccessfulKey)));
		assertEquals(expectedBuildFailedValue, Boolean.parseBoolean(eventsElement.getChildText(expectedbuildFailedKey)));
		assertEquals(expectedBuildInterruptedValue, Boolean.parseBoolean(eventsElement.getChildText(expectedBuildInterruptedKey)));
		assertEquals(expectedServerStartupValue, Boolean.parseBoolean(eventsElement.getChildText(expectedServerStartupKey)));
		assertEquals(expectedServerShutdownValue, Boolean.parseBoolean(eventsElement.getChildText(expectedServerShutdownKey)));

		// Now check the loaded configuration
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		assertEquals(expectedRoomIdValue, configuration.getDefaultRoomId());
		assertEquals(expectedNotifyStatusValue, configuration.getDefaultNotifyStatus());
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
		// Events
		assertEquals(expectedBuildStartedValue, configuration.getEvents().getBuildStartedStatus());
		assertEquals(expectedBuildSuccessfulValue, configuration.getEvents().getBuildSuccessfulStatus());
		assertEquals(expectedBuildFailedValue, configuration.getEvents().getBuildFailedStatus());
		assertEquals(expectedBuildInterruptedValue, configuration.getEvents().getBuildInterruptedStatus());
		assertEquals(expectedServerStartupValue, configuration.getEvents().getServerStartupStatus());
		assertEquals(expectedServerShutdownValue, configuration.getEvents().getServerShutdownStatus());
	}

	@Test
	public void testConfigurationGetsUpgradedFromV0dot1toV0dot2() throws IOException, JDOMException, URISyntaxException {
		// Test parameters
		String expectedDefaultRoomIdKey = "defaultRoomId";
		String expectedDefaultRoomIdValue = "12345";
		String expectedConfigDir = ".";
		String expectedConfigFileName = "hipchat.xml";
		// @formatter:off
		// roomId is the legacy key
		String v0dot1ConfigurationText = "<hipchat>\n" + 
								   "  <apiToken>token</apiToken>\n" + 
								   "  <apiUrl>https://api.hipchat.com/v2/</apiUrl>\n" + 
								   "  <disabled>false</disabled>\n" + 
								   "  <notify>true</notify>\n" + 
								   "  <roomId>" + expectedDefaultRoomIdValue + "</roomId>\n" +
								   "</hipchat>";
		// @formatter:on

		// Prepare
		File file = new File(expectedConfigDir, expectedConfigFileName);
		if (file.exists()) {
			assertTrue(file.delete());
		}
		file.createNewFile();
		System.out.println(String.format("Canonical path to config file for test: %s", file.getCanonicalPath()));
		FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
		fileWriter.write(v0dot1ConfigurationText);
		fileWriter.flush();
		fileWriter.close();

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);
		
		// After initialisation, the config must've been upgraded
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatApiProcessor processor = new HipChatApiProcessor(configuration);
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration, processor);
		controller.initialise();
				
		// Test XML was upgraded
		File configFile = new File(expectedConfigDir, expectedConfigFileName);
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(configFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedDefaultRoomIdValue, rootElement.getChildText(expectedDefaultRoomIdKey));
		
		// Test config object contains value for room ID
		assertEquals(expectedDefaultRoomIdValue, configuration.getDefaultRoomId());
		// Re-read the config from disk
		controller.initialise();
		assertEquals(expectedDefaultRoomIdValue, configuration.getDefaultRoomId());
	}
		
}
