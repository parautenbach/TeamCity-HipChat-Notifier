package com.whatsthatlight.teamcity.hipchat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
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
	private static final String EDIT_PARAMETER = "edit";
	private static final String TEST_PARAMETER = "test";
	private static final String PROJECT_PARAMETER = "project";
	private static final String HIPCHAT_CONFIG_FILE = "hipchat.xml";
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private String configFilePath;

	private HipChatConfiguration configuration;
	private HipChatApiProcessor processor;

	public HipChatConfigurationController(@NotNull SBuildServer server, @NotNull ServerPaths serverPaths, @NotNull WebControllerManager manager,
			@NotNull HipChatConfiguration configuration, @NotNull HipChatApiProcessor processor) throws IOException {
		manager.registerController(CONTROLLER_PATH, this);
		this.configuration = configuration;
		this.configFilePath = (new File(serverPaths.getConfigDir(), HIPCHAT_CONFIG_FILE)).getCanonicalPath();
		this.processor = processor;
		logger.debug(String.format("Config file path: %s", this.configFilePath));
		logger.info("Controller created");
	}

	@Override
	protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) {
		try {
			logger.debug("Handling request");
			
			if (request.getParameter(PROJECT_PARAMETER) != null) {
				logger.debug("Changing project configuration");
				String roomId = request.getParameter(HipChatConfiguration.ROOM_ID_KEY);
				String notify = request.getParameter(HipChatConfiguration.NOTIFY_STATUS_KEY);
				String projectId = request.getParameter("projectId");
				logger.debug(String.format("Room ID: %s", roomId));
				logger.debug(String.format("Trigger notification: %s", notify));
				logger.debug(String.format("Project ID: %s", projectId));
				// TODO: Complete
			}
			
			if (request.getParameter(EDIT_PARAMETER) != null) {
				logger.debug("Changing configuration");
				String apiUrl = request.getParameter(HipChatConfiguration.API_URL_KEY);
				String apiToken = request.getParameter(HipChatConfiguration.API_TOKEN_KEY);
				String defaultRoomId = request.getParameter(HipChatConfiguration.DEFAULT_ROOM_ID_KEY);
				String notify = request.getParameter(HipChatConfiguration.NOTIFY_STATUS_KEY);
				logger.debug(String.format("API URL: %s", apiUrl));
				logger.debug(String.format("API token: %s", apiToken));
				logger.debug(String.format("Default room ID: %s", defaultRoomId));
				logger.debug(String.format("Trigger notification: %s", notify));
				this.configuration.setApiUrl(apiUrl);
				this.configuration.setApiToken(apiToken);
				this.configuration.setDefaultRoomId(defaultRoomId == "" ? null : defaultRoomId);
				this.configuration.setNotifyStatus(Boolean.parseBoolean(notify));
				this.getOrCreateMessages(request).addMessage("configurationSaved", "Saved");
				this.saveConfiguration();
			}

			if (request.getParameter(TEST_PARAMETER) != null) {
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
			
			if (request.getParameter(ACTION_PARAMETER) != null) {
				logger.debug("Changing status");
				Boolean disabled = !request.getParameter(ACTION_PARAMETER).equals(ACTION_ENABLE);
				logger.debug(String.format("Disabled status: %s", disabled));
				this.configuration.setDisabledStatus(disabled);
				this.saveConfiguration();
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(configFile);
		Element rootElement = document.getDocumentElement();
		NodeList nodes = rootElement.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i).getNodeName() == HipChatConfiguration.DEFAULT_ROOM_ID_KEY_V0DOT1 && nodes.item(i) instanceof Element) {
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

	public void loadConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.setClassLoader(this.configuration.getClass().getClassLoader());
		xstream.setClassLoader(HipChatProjectConfiguration.class.getClassLoader());
		xstream.processAnnotations(HipChatConfiguration.class);
		File file = new File(this.configFilePath);
		HipChatConfiguration configuration = (HipChatConfiguration) xstream.fromXML(file);
		
		// Copy the values, because we need it on the original shared (bean),
		// which is a singleton
		this.configuration.setApiUrl(configuration.getApiUrl());
		this.configuration.setApiToken(configuration.getApiToken());
		this.configuration.setDefaultRoomId(configuration.getDefaultRoomId());
		this.configuration.setNotifyStatus(configuration.getNotifyStatus());
		this.configuration.setDisabledStatus(configuration.getDisabledStatus());
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
		FileWriter fileWriter = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		xstream.toXML(this.configuration, bufferedWriter);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

}
