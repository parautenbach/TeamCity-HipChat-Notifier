package com.whatsthatlight.teamcity.hipchat;

import org.apache.log4j.Logger;
import org.jdom.Element;

import jetbrains.buildServer.serverSide.MainConfigProcessor;
import jetbrains.buildServer.serverSide.SBuildServer;

public class HipChatNotifierGlobalSettings implements MainConfigProcessor {

	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private SBuildServer buildServer;
	
	public HipChatNotifierGlobalSettings(SBuildServer buildServer) {
		this.buildServer = buildServer;
	}
	
    public void register() {
    	buildServer.registerExtension(MainConfigProcessor.class, "HipChat", this);
    	logger.info("Registered global settings");
    }
    
	@Override
	public void readFrom(Element element) {
		logger.info("readFrom");
	}

	@Override
	public void writeTo(Element element) {
		logger.info("writeTo");
	}

	public String getApiUrl() {
		logger.info("getApiUrl");
		return "http://example.com/";
	}

}
