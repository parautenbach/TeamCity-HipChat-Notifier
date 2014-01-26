package com.whatsthatlight.teamcity.hipchat;

import org.apache.log4j.Logger;
import org.jdom.Element;

import jetbrains.buildServer.serverSide.MainConfigProcessor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.ServerExtension;

//import javax.servlet.http.HttpServletRequest;

public class HipChatNotifierGlobalSettings implements MainConfigProcessor { //, ServerExtension {

	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private SBuildServer buildServer;
	
	public HipChatNotifierGlobalSettings(SBuildServer buildServer) {
		this.buildServer = buildServer;
	}
	
    public void register() {
    	logger.info("Registering global settings");
    	buildServer.registerExtension(MainConfigProcessor.class, "HipChat", this);
    }
    
	@Override
	public void readFrom(Element element) {
		logger.info("readFrom");
	}

	@Override
	public void writeTo(Element element) {
		logger.info("writeTo");
	}

}
