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

package com.whatsthatlight.teamcity.hipchat;


import jetbrains.buildServer.controllers.admin.projects.EditProjectTab;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

public class HipChatProjectSettings extends EditProjectTab {

	private static final String ROOM_ID_LIST = "roomIdList";
	private static final String PAGE = "projectSettings.jsp";

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	@NotNull
	private final SecurityContext securityContext;
	@NotNull
	private final HipChatConfiguration configuration;
	@NotNull
	private final HipChatApiProcessor processor;

	public HipChatProjectSettings(@NotNull PagePlaces pagePlaces,
	                              @NotNull PluginDescriptor descriptor,
	                              @NotNull SecurityContext securityContext,
	                              @NotNull HipChatConfiguration configuration,
	                              @NotNull HipChatApiProcessor processor) {
		super(pagePlaces, "HipChat", descriptor.getPluginResourcesPath(PAGE), "HipChat");
		this.securityContext = securityContext;
		this.configuration = configuration;
		this.processor = processor;
		logger.info("Project configuration page registered");
	}

	@Override
	public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
		super.fillModel(model, request);

		SProject project = getProject(request);
		if (project != null) {
			String projectId = project.getProjectId();
			model.put(HipChatConfiguration.PROJECT_ID_KEY, projectId);
			TreeMap<String, String> rooms = Utils.getRooms(this.processor);
			model.put(ROOM_ID_LIST, rooms);
			boolean isRootProject = Utils.isRootProject(project);
			HipChatProjectConfiguration projectConfiguration = this.configuration.getProjectConfiguration(projectId);
			if (projectConfiguration != null) {
				model.put(HipChatConfiguration.ROOM_ID_KEY, projectConfiguration.getRoomId());
				model.put(HipChatConfiguration.NOTIFY_STATUS_KEY, projectConfiguration.getNotifyStatus());
			} else if (isRootProject) {
				model.put(HipChatConfiguration.ROOM_ID_KEY, HipChatConfiguration.ROOM_ID_DEFAULT_VALUE);
				model.put(HipChatConfiguration.NOTIFY_STATUS_KEY, configuration.getDefaultNotifyStatus());
			} else {
				model.put(HipChatConfiguration.ROOM_ID_KEY, HipChatConfiguration.ROOM_ID_PARENT_VALUE);
				model.put(HipChatConfiguration.NOTIFY_STATUS_KEY, configuration.getDefaultNotifyStatus());
			}
			model.put(HipChatConfiguration.IS_ROOT_PROJECT_KEY, isRootProject);
			logger.debug("Configuration page variables populated");
		}
	}


	@Override
	public boolean isAvailable(@NotNull final HttpServletRequest request) {
		final SProject project = getProject(request);
		final SUser user = (SUser) securityContext.getAuthorityHolder().getAssociatedUser();
		return ! configuration.getDisabledStatus() && user != null && project != null &&
				user.isPermissionGrantedForProject(project.getProjectId(), Permission.EDIT_PROJECT);
	}
}
