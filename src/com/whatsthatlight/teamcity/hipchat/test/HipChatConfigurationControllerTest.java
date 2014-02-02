package com.whatsthatlight.teamcity.hipchat.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatConfigurationController;

;

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
		String expectedApiTokenKey = "apiToken";
		String expectedApiTokenValue = "admin_token";
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
		assertNull(configuration.getApiUrl());
		assertNull(configuration.getApiToken());
		assertNull(configuration.getDisabledStatus());

		// Execute
		// The config file must exist on disk after initialisation
		HipChatConfigurationController controller = new HipChatConfigurationController(server, serverPaths, manager, configuration);
		controller.initialise();
		File postRegistrationConfigFile = new File(expectedFileName);
		assertTrue(postRegistrationConfigFile.exists());

		// Check XML of the newly created config file
		SAXBuilder builder = new SAXBuilder();
		Document document = (Document) builder.build(postRegistrationConfigFile);
		Element rootElement = document.getRootElement();
		assertNull(rootElement.getChildText(expectedApiUrlKey));
		assertNull(rootElement.getChildText(expectedApiTokenKey));
		assertNull(rootElement.getChildText(expectedDisabledStatusKey));

		// And the instance values must still be null
		assertNull(configuration.getApiUrl());
		assertNull(configuration.getApiToken());
		assertNull(configuration.getDisabledStatus());

		// Now change and save the configuration
		configuration.setApiUrl(expectedApiUrlValue);
		configuration.setApiToken(expectedApiTokenValue);
		configuration.setDisabledStatus(expectedDisabledStatusValue);
		controller.saveConfiguration();

		// Check XML of the saved config file
		builder = new SAXBuilder();
		document = (Document) builder.build(postRegistrationConfigFile);
		rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedDisabledStatusValue.toString(), rootElement.getChildText(expectedDisabledStatusKey));

		// And also the values in memory
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
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
		Document document = (Document) builder.build(postInitConfigFile);
		Element rootElement = document.getRootElement();
		assertEquals(expectedApiUrlValue, rootElement.getChildText(expectedApiUrlKey));
		assertEquals(expectedApiTokenValue, rootElement.getChildText(expectedApiTokenKey));
		assertEquals(expectedDisabledStatusValue, Boolean.parseBoolean(rootElement.getChildText(expectedDisabledStatusKey)));

		// Now check the loaded configuration
		assertEquals(expectedApiUrlValue, configuration.getApiUrl());
		assertEquals(expectedApiTokenValue, configuration.getApiToken());
		assertEquals(expectedDisabledStatusValue, configuration.getDisabledStatus());
	}

}
