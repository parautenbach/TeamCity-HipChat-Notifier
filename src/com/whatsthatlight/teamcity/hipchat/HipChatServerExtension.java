package com.whatsthatlight.teamcity.hipchat;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SRunningBuild;

public class HipChatServerExtension extends BuildServerAdapter {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	public HipChatServerExtension(@NotNull HipChatConfiguration configuration) {
		logger.debug("Server extension created");
	}
	
	@Override
	public void buildStarted(SRunningBuild build) {
		super.buildStarted(build);
		logger.debug(String.format("Build started: %s", build.getBuildType().getName()));
	}
	
}
