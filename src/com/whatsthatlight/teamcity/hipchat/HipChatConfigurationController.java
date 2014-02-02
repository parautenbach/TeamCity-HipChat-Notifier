package com.whatsthatlight.teamcity.hipchat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

import com.thoughtworks.xstream.XStream;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;

public class HipChatConfigurationController extends BaseController {

	private static final String HIPCHAT_CONFIG_FILE = "hipchat.xml";
	private static final String ACTION_PARAMETER = "action";
	private static final String EDIT_PARAMETER = "edit";
	private static final Object ACTION_ENABLE = "enable";
	private String configFilePath;
	private HipChatConfiguration configuration;

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	public HipChatConfigurationController(@NotNull SBuildServer server, @NotNull ServerPaths serverPaths, @NotNull WebControllerManager manager,
			@NotNull HipChatConfiguration configuration) throws IOException {
		// super(server);
		manager.registerController("/configureHipChat.html", this);
		this.configuration = configuration;
		this.configFilePath = (new File(serverPaths.getConfigDir(), HIPCHAT_CONFIG_FILE)).getCanonicalPath();
		logger.debug(String.format("Config file path: %s", this.configFilePath));
		logger.info("Controller created");
	}

	public void initialise() {
		try {
			File file = new File(this.configFilePath);
			if (file.exists()) {
				logger.debug("Loading existing configuration");
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

	@Override
	protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) {
		try {
			logger.debug("Handling configuration change");
			if (request.getParameter(EDIT_PARAMETER) != null) {
				String apiUrl = request.getParameter(HipChatConfiguration.API_URL_KEY);
				String apiToken = request.getParameter(HipChatConfiguration.API_TOKEN_KEY);
				logger.debug(String.format("API URL: %s", apiUrl));
				logger.debug(String.format("API token: %s", apiToken));
				configuration.setApiUrl(apiUrl);
				configuration.setApiToken(apiToken);
				getOrCreateMessages(request).addMessage("configurationSaved", "Saved");
			}

			if (request.getParameter(ACTION_PARAMETER) != null) {
				Boolean disabled = !request.getParameter(ACTION_PARAMETER).equals(ACTION_ENABLE);
				logger.debug(String.format("Disabled status: %s", disabled));
				this.configuration.setDisabledStatus(disabled);
			}

			this.saveConfiguration();
		} catch (Exception e) {
			logger.error("Could not handle request", e);
		} 
		
		return null;
	}

	public void loadConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.processAnnotations(HipChatConfiguration.class);
		File file = new File(this.configFilePath);
		HipChatConfiguration configuration = (HipChatConfiguration) xstream.fromXML(file);
		// Copy the values, because we need it on the original shared (bean), which is a singleton
		this.configuration.setApiUrl(configuration.getApiUrl());
		this.configuration.setApiToken(configuration.getApiToken());
		this.configuration.setDisabledStatus(configuration.getDisabledStatus());
	}

	public void saveConfiguration() throws IOException {
		XStream xstream = new XStream();
		xstream.processAnnotations(HipChatConfiguration.class);
		File file = new File(this.configFilePath);
		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		xstream.toXML(this.configuration, bufferedWriter);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

}
