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

package com.whatsthatlight.teamcity.hipchat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

public class HipChatConfigurationController extends BaseController {

	private static final Object ACTION_ENABLE = "enable";
	private static final String ACTION_PARAMETER = "action";
	private static final String CONTROLLER_PATH = "/configureHipChat.html";
	public static final String EDIT_PARAMETER = "edit";
	private static final String TEST_PARAMETER = "test";
	private static final String PROJECT_PARAMETER = "project";
	private static final String HIPCHAT_CONFIG_FILE = "hipchat.xml";
	public static final String HIPCHAT_CONFIG_DIRECTORY = "hipchat";
	private static final String SAVED_ID = "configurationSaved";
	private static final String SAVED_MESSAGE = "Saved";
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private String configFilePath;

	private HipChatConfiguration configuration;
	private HipChatApiProcessor processor;
	private HipChatNotificationMessageTemplates templates;

	public HipChatConfigurationController(@NotNull SBuildServer server, @NotNull ServerPaths serverPaths, @NotNull WebControllerManager manager,
			@NotNull HipChatConfiguration configuration, @NotNull HipChatApiProcessor processor, @NotNull HipChatNotificationMessageTemplates templates) throws IOException {
		manager.registerController(CONTROLLER_PATH, this);
		this.configuration = configuration;
		this.configFilePath = (new File(serverPaths.getConfigDir(), HIPCHAT_CONFIG_FILE)).getCanonicalPath();
		this.processor = processor;
		this.templates = templates;
		logger.debug(String.format("Config file path: %s", this.configFilePath));
		logger.info("Controller created");
	}

	private void handleProjectConfigurationChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing project configuration");
		String roomId = request.getParameter(HipChatConfiguration.ROOM_ID_KEY);
		boolean notify = Boolean.parseBoolean(request.getParameter(HipChatConfiguration.NOTIFY_STATUS_KEY));
		String projectId = request.getParameter("projectId");
		logger.debug(String.format("Room ID: %s", roomId));
		logger.debug(String.format("Trigger notification: %s", notify));
		logger.debug(String.format("Project ID: %s", projectId));
		HipChatProjectConfiguration projectConfiguration = new HipChatProjectConfiguration(projectId, roomId, notify);
		this.configuration.setProjectConfiguration(projectConfiguration);
		this.getOrCreateMessages(request).addMessage(SAVED_ID, SAVED_MESSAGE);
		this.saveConfiguration();
	}
	
	private void handleConfigurationChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing configuration");
		logger.debug(String.format("Query string: '%s'", request.getQueryString()));
		
		// Get parameters
		String apiUrl = request.getParameter(HipChatConfiguration.API_URL_KEY);
		String apiToken = request.getParameter(HipChatConfiguration.API_TOKEN_KEY);
		String defaultRoomId = request.getParameter(HipChatConfiguration.DEFAULT_ROOM_ID_KEY);
		String notify = request.getParameter(HipChatConfiguration.NOTIFY_STATUS_KEY);
		String buildStarted = request.getParameter(HipChatConfiguration.BUILD_STARTED_KEY);
		String buildSuccessful = request.getParameter(HipChatConfiguration.BUILD_SUCCESSFUL_KEY);
		String buildFailed = request.getParameter(HipChatConfiguration.BUILD_FAILED_KEY);
		String buildInterrupted = request.getParameter(HipChatConfiguration.BUILD_INTERRUPTED_KEY);
		String serverStartup = request.getParameter(HipChatConfiguration.SERVER_STARTUP_KEY);
		String serverShutdown = request.getParameter(HipChatConfiguration.SERVER_SHUTDOWN_KEY);
		String buildStartedTemplate = request.getParameter(HipChatNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_KEY);
		String buildSuccessfulTemplate = request.getParameter(HipChatNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_KEY);
		String buildFailedTemplate = request.getParameter(HipChatNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_KEY);
		String buildInterruptedTemplate = request.getParameter(HipChatNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_KEY);
		String serverStartupTemplate = request.getParameter(HipChatNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_KEY);
		String serverShutdownTemplate = request.getParameter(HipChatNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_KEY);
		
		// Logging
		logger.debug(String.format("API URL: %s", apiUrl));
		logger.debug(String.format("API token: %s", apiToken));
		logger.debug(String.format("Default room ID: %s", defaultRoomId));
		logger.debug(String.format("Trigger notification: %s", notify));
		logger.debug("Events:");
		logger.debug(String.format("\tBuild started: %s", buildStarted));		
		logger.debug(String.format("\tBuild successful: %s", buildSuccessful));
		logger.debug(String.format("\tBuild failed: %s", buildFailed));
		logger.debug(String.format("\tBuild interrupted: %s", buildInterrupted));
		logger.debug(String.format("\tServer startup: %s", serverStartup));
		logger.debug(String.format("\tServer shutdown: %s", serverShutdown));
		logger.debug("Templates:");
		logger.debug(String.format("\tBuild started: %s", buildStartedTemplate));
		logger.debug(String.format("\tBuild successful: %s", buildSuccessfulTemplate));
		logger.debug(String.format("\tBuild failed: %s", buildFailedTemplate));
		logger.debug(String.format("\tBuild interrupted: %s", buildInterruptedTemplate));
		logger.debug(String.format("\tServer startup: %s", serverStartupTemplate));
		logger.debug(String.format("\tServer shutdown: %s", serverShutdownTemplate));
		
		// Save the configuration
		this.configuration.setApiUrl(apiUrl);
		this.configuration.setApiToken(apiToken);
		this.configuration.setDefaultRoomId(defaultRoomId == "" ? null : defaultRoomId);
		this.configuration.setNotifyStatus(Boolean.parseBoolean(notify));
		HipChatEventConfiguration events = new HipChatEventConfiguration();
		events.setBuildStartedStatus(Boolean.parseBoolean(buildStarted));
		events.setBuildSuccessfulStatus(Boolean.parseBoolean(buildSuccessful));
		events.setBuildFailedStatus(Boolean.parseBoolean(buildFailed));
		events.setBuildInterruptedStatus(Boolean.parseBoolean(buildInterrupted));
		events.setServerStartupStatus(Boolean.parseBoolean(serverStartup));
		events.setServerShutdownStatus(Boolean.parseBoolean(serverShutdown));
		this.configuration.setEvents(events);
		this.saveConfiguration();
		
		// Save the templates
		this.templates.writeTemplate(TeamCityEvent.BUILD_STARTED, buildStartedTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_SUCCESSFUL, buildSuccessfulTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_FAILED, buildFailedTemplate);
		this.templates.writeTemplate(TeamCityEvent.BUILD_INTERRUPTED, buildInterruptedTemplate);
		this.templates.writeTemplate(TeamCityEvent.SERVER_STARTUP, serverStartupTemplate);
		this.templates.writeTemplate(TeamCityEvent.SERVER_SHUTDOWN, serverShutdownTemplate);
		
		// Update the page
		this.getOrCreateMessages(request).addMessage(SAVED_ID, SAVED_MESSAGE);
	}
	
	private void handleTestConnection(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("Testing authentication");
		String apiUrl = request.getParameter(HipChatConfiguration.API_URL_KEY);
		String apiToken = request.getParameter(HipChatConfiguration.API_TOKEN_KEY);
		logger.debug(String.format("API URL: %s", apiUrl));
		logger.debug(String.format("API token: %s", apiToken));
		this.configuration.setApiUrl(apiUrl);
		this.configuration.setApiToken(apiToken);
		boolean result = this.processor.testAuthentication();
		logger.debug(String.format("Authentication status: %s", result));
		if (result) {
			response.setStatus(HttpStatus.SC_OK);
		} else {
			response.setStatus(HttpStatus.SC_BAD_REQUEST);
		}
	}
	
	private void handlePluginStatusChange(HttpServletRequest request) throws IOException {
		logger.debug("Changing status");
		Boolean disabled = !request.getParameter(ACTION_PARAMETER).equals(ACTION_ENABLE);
		logger.debug(String.format("Disabled status: %s", disabled));
		this.configuration.setDisabledStatus(disabled);
		this.saveConfiguration();
	}
	
	@Override
	public ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) {
		try {
			logger.debug("Handling request");
			if (request.getParameter(PROJECT_PARAMETER) != null) {
				this.handleProjectConfigurationChange(request);
			} else if (request.getParameter(EDIT_PARAMETER) != null) {
				this.handleConfigurationChange(request);
			} else if (request.getParameter(TEST_PARAMETER) != null) {
				this.handleTestConnection(request, response);
			} else if (request.getParameter(ACTION_PARAMETER) != null) {
				this.handlePluginStatusChange(request);
			}
		} catch (Exception e) {
			logger.error("Could not handle request", e);
		}

		return null;
	}

	public void initialise() {
		try {
			File file = new File(this.configFilePath);
			if (file.exists()) {
				logger.debug("Loading existing configuration");
				this.upgradeConfigurationFromV0dot1ToV0dot2();
				this.loadConfiguration();
			} else {
				logger.debug("No configuration file exists; creating new one");
				this.saveConfiguration();
			}
		} catch (Exception e) {
			logger.error("Could not load configuration", e);
		}
		logger.info("Controller initialised");
	}

	private void upgradeConfigurationFromV0dot1ToV0dot2() throws IOException, SAXException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		File configFile = new File(this.configFilePath);
		if (configFile.exists()) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(configFile);
			Element rootElement = document.getDocumentElement();
			NodeList nodes = rootElement.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().equals(HipChatConfiguration.DEFAULT_ROOM_ID_KEY_V0DOT1) && nodes.item(i) instanceof Element) {
					Element roomElement = (Element)nodes.item(i);
					document.renameNode(roomElement, roomElement.getNamespaceURI(), HipChatConfiguration.DEFAULT_ROOM_ID_KEY);
				}
			}
			
			// Save
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Result output = new StreamResult(configFile);
			Source input = new DOMSource(document);
			transformer.transform(input, output);
		}
	}
	
	public void loadConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.setClassLoader(this.configuration.getClass().getClassLoader());
		xstream.setClassLoader(HipChatProjectConfiguration.class.getClassLoader());
		xstream.processAnnotations(HipChatConfiguration.class);
		FileReader fileReader = new FileReader(this.configFilePath);
		HipChatConfiguration configuration = (HipChatConfiguration) xstream.fromXML(fileReader);
		fileReader.close();
		
		// Copy the values, because we need it on the original shared (bean),
		// which is a singleton
		this.configuration.setApiUrl(configuration.getApiUrl());
		this.configuration.setApiToken(configuration.getApiToken());
		this.configuration.setDefaultRoomId(configuration.getDefaultRoomId());
		this.configuration.setNotifyStatus(configuration.getDefaultNotifyStatus());
		this.configuration.setDisabledStatus(configuration.getDisabledStatus());
		if (configuration.getEvents() != null) {
			this.configuration.getEvents().setBuildStartedStatus(configuration.getEvents().getBuildStartedStatus());
			this.configuration.getEvents().setBuildSuccessfulStatus(configuration.getEvents().getBuildSuccessfulStatus());
			this.configuration.getEvents().setBuildFailedStatus(configuration.getEvents().getBuildFailedStatus());
			this.configuration.getEvents().setBuildInterruptedStatus(configuration.getEvents().getBuildInterruptedStatus());
			this.configuration.getEvents().setServerStartupStatus(configuration.getEvents().getServerStartupStatus());
			this.configuration.getEvents().setServerShutdownStatus(configuration.getEvents().getServerShutdownStatus());
		}
		if (configuration.getProjectRoomMap() != null) {
			for (HipChatProjectConfiguration projectConfiguration : configuration.getProjectRoomMap()) {
				this.configuration.setProjectConfiguration(projectConfiguration);
			}
		}
	}

	public void saveConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.processAnnotations(this.configuration.getClass());
		File file = new File(this.configFilePath);
		file.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		xstream.toXML(this.configuration, fileOutputStream);
		fileOutputStream.flush();
		fileOutputStream.close();
	}

}
