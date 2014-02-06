package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SRunningBuild;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatServerExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HipChatServerExtensionTest {

	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}

	@Test
	public void test() {

		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setApiUrl("https://api.hipchat.com/v2/");
		// TODO: Remove token/use dummy token
		configuration.setApiToken("token");
		configuration.setRoomId("389590");
		configuration.setNotifyStatus(true);
		//configuration.setDisabledStatus(false);
		
		SBuildServer server = null;
		HipChatServerExtension extension = new HipChatServerExtension(server, configuration);
		
		SBuildType buildType = mock(SBuildType.class);
		when(buildType.getName()).thenReturn("test");
		SRunningBuild build = mock(SRunningBuild.class);
		when(build.getBuildType()).thenReturn(buildType);
		when(build.isPersonal()).thenReturn(false);
		
		extension.buildStarted(build);
	}

}
