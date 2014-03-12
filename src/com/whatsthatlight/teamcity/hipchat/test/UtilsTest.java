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
import jetbrains.buildServer.serverSide.SProject;

import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.whatsthatlight.teamcity.hipchat.HipChatConfiguration;
import com.whatsthatlight.teamcity.hipchat.HipChatProjectConfiguration;
import com.whatsthatlight.teamcity.hipchat.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {
	
	@BeforeClass
	public static void ClassSetup() {
		// Set up a basic logger for debugging purposes
		BasicConfigurator.configure();
	}
	
	@Test
	public void testParentsParentHasConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "parent_project_id";
		String expectedParentsParentProjectId = "parents_parent_project_id";
		String expectedParentsParentRoomId = "parents_parent_room_id";
		boolean expectedParentsParentNotifyStatus = false;
		HipChatConfiguration configuration = new HipChatConfiguration();
		// The immediate parent has no configuration
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedParentsParentProjectId, expectedParentsParentRoomId, expectedParentsParentNotifyStatus));
		
		// Mocks
		SProject parentsParentProject = mock(SProject.class);
		when(parentsParentProject.getProjectId()).thenReturn(expectedParentsParentProjectId);
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		when(parentProject.getParentProject()).thenReturn(parentsParentProject);
		when(parentProject.getParentProjectId()).thenReturn(expectedParentsParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		HipChatProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentsParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentsParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentsParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}
	
	@Test
	public void testImmediateParentHasConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "parent_project_id";
		String expectedParentRoomId = "parent_room_id";
		boolean expectedParentNotifyStatus = true;
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedParentProjectId, expectedParentRoomId, expectedParentNotifyStatus));
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		HipChatProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}

	@Test
	public void testImmediateParentIsRootProjectWithConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		String expectedParentRoomId = "parent_room_id";
		boolean expectedParentNotifyStatus = true;
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setProjectConfiguration(new HipChatProjectConfiguration(expectedParentProjectId, expectedParentRoomId, expectedParentNotifyStatus));
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		HipChatProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertEquals(expectedParentProjectId, actualParentConfiguration.getProjectId());
		assertEquals(expectedParentRoomId, actualParentConfiguration.getRoomId());
		assertEquals(expectedParentNotifyStatus, actualParentConfiguration.getNotifyStatus());
	}
	
	@Test
	public void testImmediateParentIsRootProjectWithoutConfiguration() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		HipChatConfiguration configuration = new HipChatConfiguration();
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		// Execute
		HipChatProjectConfiguration actualParentConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
		assertNull(actualParentConfiguration);
	}
	
	@Test
	public void testProjectUsesDefaultRoomIdWhenRoomConfigurationAbsent() {
		// Test parameters
		String expectedProjectId = "project_id";
		String expectedParentProjectId = "_Root";
		HipChatConfiguration configuration = new HipChatConfiguration();
		configuration.setDefaultRoomId(expectedProjectId);
		
		// Mocks
		SProject parentProject = mock(SProject.class);
		when(parentProject.getProjectId()).thenReturn(expectedParentProjectId);
		SProject project = mock(SProject.class);
		when(project.getProjectId()).thenReturn(expectedProjectId);
		when(project.getParentProject()).thenReturn(parentProject);
		when(project.getParentProjectId()).thenReturn(expectedParentProjectId);
		
		HipChatProjectConfiguration projectConfiguration = Utils.determineProjectConfiguration(project, configuration);
		assertEquals(expectedProjectId, projectConfiguration.getRoomId());
	}
	
}
