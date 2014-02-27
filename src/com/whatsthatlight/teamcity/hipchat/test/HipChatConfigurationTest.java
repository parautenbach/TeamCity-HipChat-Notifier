package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectConfiguration;

public class HipChatConfigurationTest {

	@Test
	public void testProjectConfigurationContainsNoDuplicateProjectIds() throws URISyntaxException, IOException {
		// Test parameters
		String expectedProjectId = "project1";
		String expectedRoomIdFormer = "room1";
		boolean expectedNotifyStatusFormer = true;
		String expectedRoomIdLatter = "room2";
		boolean expectedNotifyStatusLatter = false;
				
		// Prepare
		HipChatConfiguration configuration = new HipChatConfiguration();
		assertEquals(0, configuration.getProjectRoomMap().size());
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedProjectId, expectedRoomIdFormer, expectedNotifyStatusFormer));
		assertEquals(1, configuration.getProjectRoomMap().size());
		HipChatProjectConfiguration projectConfigurationFormer = configuration.getProjectConfiguration(expectedProjectId);
		assertEquals(expectedRoomIdFormer, projectConfigurationFormer.getRoomId());
		assertEquals(expectedNotifyStatusFormer, projectConfigurationFormer.getNotifyStatus());
		
		// Execute
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedProjectId, expectedRoomIdLatter, expectedNotifyStatusLatter));
		
		// Test
		assertEquals(1, configuration.getProjectRoomMap().size());
		HipChatProjectConfiguration projectConfigurationLatter = configuration.getProjectConfiguration(expectedProjectId);
		assertEquals(expectedRoomIdLatter, projectConfigurationLatter.getRoomId());
		assertEquals(expectedNotifyStatusLatter, projectConfigurationLatter.getNotifyStatus());
	}

}
