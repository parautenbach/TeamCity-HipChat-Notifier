package com.whatsthatlight.teamcity.hipchat;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.project.ProjectTab;

public class HipChatProjectTab extends ProjectTab {

	private static final String PAGE = "projectSettings.jsp";
	private static final String ROOM_ID_LIST = "roomIdList";
	private HipChatConfiguration configuration;
	private HipChatApiProcessor processor;
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	protected HipChatProjectTab(
			@NotNull PagePlaces pagePlaces, 
			@NotNull ProjectManager projectManager,
			@NotNull PluginDescriptor descriptor,
			@NotNull HipChatConfiguration configuration,
			@NotNull HipChatApiProcessor processor) {
		super("hipChat", "HipChat", pagePlaces, projectManager, descriptor.getPluginResourcesPath(PAGE));
		this.configuration = configuration;
		this.processor = processor;
		logger.info("Project configuration page registered");
	}

	@Override
	protected void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request, @NotNull SProject project, @NotNull SUser user) {
		//super.fillModel(model, request);
		// TODO: Complete
		model.put("projectId", project.getProjectId());
		model.put(ROOM_ID_LIST, Utils.getRooms(this.processor));
		model.put(HipChatConfiguration.NOTIFY_STATUS_KEY, true);
		logger.debug("Configuration page variables populated");
	}

}
