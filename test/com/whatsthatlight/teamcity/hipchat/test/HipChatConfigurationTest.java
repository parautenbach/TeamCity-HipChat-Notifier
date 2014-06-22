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

package com.whatsthatlight.teamcity.hipchat.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.annotations.Test;

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
