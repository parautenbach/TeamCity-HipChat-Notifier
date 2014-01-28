package com.whatsthatlight.teamcity.hipchat;

import jetbrains.buildServer.serverSide.ServerPaths;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

//import jetbrains.buildServer.serverSide.MainConfigProcessor;
//import jetbrains.buildServer.serverSide.SBuildServer;

public class HipChatConfiguration {

	private static final String HIPCHAT_ELEMENT = "hipchat-settings";
	private static final String API_URL_ATTRIBUTE = "apiUrl";
	//private SBuildServer buildServer;
	private String configDirectory;
	private boolean status = true;
	private String apiUrl = "http://example.com/";
	private String apiToken;
	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	public HipChatConfiguration(@NotNull ServerPaths serverPaths) { //SBuildServer buildServer) {
		//this.buildServer = buildServer;
		this.configDirectory = serverPaths.getConfigDir();
	}
	
    public void register() {
    	//buildServer.registerExtension(MainConfigProcessor.class, "HipChat", this);
    	logger.info("Registered configuration");
    	logger.info(this.configDirectory);
    }
    
	public void readFrom(Element element) {
		logger.info("readFrom: " + element.getName());
	}

	public void writeTo(Element rootElement) {
		logger.info("writeTo");
		Element hipChatElement = rootElement.getChild(HIPCHAT_ELEMENT);
		if (hipChatElement == null) {
			logger.debug(String.format("The {0} element does not exist", HIPCHAT_ELEMENT));
			hipChatElement = new Element(HIPCHAT_ELEMENT);
			hipChatElement.setAttribute(API_URL_ATTRIBUTE, "test");
			rootElement.addContent(hipChatElement);
		}
	}

	public String getApiUrl() {
		//logger.info("getApiUrl");
		return this.apiUrl; //"http://example.com/";
	}

	public void setApiUrl(String url) {
		//logger.info("setApiUrl " + url);
		this.apiUrl = url;
	}

	public boolean getStatus() {
		return this.status;
	}
	
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public String getApiToken() {
		return this.apiToken;
	}
	
	public void setApiToken(String token) {
		this.apiToken = token;
	}
	
}
