package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHipChatServerExtension {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}

	@Test
	public void test() {

		HipChatConfiguration configuration = new HipChatConfiguration();
		
		configuration.setApiUrl("https://api.hipchat.com/v2/");
		configuration.setApiToken("Mi7JkzdiT5wYZ0OAMrjFQzeAP7B5DfcYQu2wXp8e");
		
		SBuildServer server = null;
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration);
		
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		
		extension.buildStarted(build);
	}

}
