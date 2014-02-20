package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationController;

public class HipChatConfigurationControllerTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}

	@Test
	public void testConfigurationFileGetsCreatedWhenNoneExists() throws IOException, JDOMException, ParserConfigurationException, TransformerException {
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
		assertFalse(configuration.getNotifyStatus());
		assertFalse(configuration.getDisabledStatus());

		// Execute
		// The config file must exist on disk after initialisation
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration);
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

		// And the instance values must still be the defaults
		assertEquals(expectedApiUrlDefaultValue, configuration.getApiUrl());
		assertNull(configuration.getApiToken());
		assertNull(configuration.getDefaultRoomId());
		assertFalse(configuration.getNotifyStatus());
		assertFalse(configuration.getDisabledStatus());

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

		// And also the values in memory
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		configuration.setDefaultRoomId(expectedRoomIdValue);
		configuration.setNotifyStatus(expectedNotifyStatusValue);
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
	}

	@Test
	public void testConfigurationGetsReadCorrectlyFromFileUponInitialisation() throws IOException, JDOMException {
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
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		bufferedWriter.write(configFileContent);
		bufferedWriter.flush();
		bufferedWriter.close();

		// Execute
		// The config file must must not have been overwritten on disk after
		// initialisation
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration);
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

		// Now check the loaded configuration
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		assertEquals(expectedRoomIdValue, configuration.getDefaultRoomId());
		assertEquals(expectedNotifyStatusValue, configuration.getNotifyStatus());
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
	}

	@Test
	public void testConfigurationGetsUpgradedFromV0dot1toV0dot2() throws IOException, JDOMException {
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
		BufferedWriter bw = new BufferedWriter(fileWriter);
		bw.write(v0dot1ConfigurationText);
		bw.close();
		fileWriter.close();

		// Mocks
		ServerPaths serverPaths = mock(ServerPaths.class);
		when(serverPaths.getConfigDir()).thenReturn(expectedConfigDir);
		SBuildServer server = mock(SBuildServer.class);
		WebControllerManager manager = mock(WebControllerManager.class);
		
		// After initialisation, the config must've been upgraded
		HipChatConfiguration configuration = new HipChatConfiguration();
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration);
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
