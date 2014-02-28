package com.whatsthatlight.teamcity.hipchat;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import jetbrains.buildServer.serverSide.SProject;

public class Utils {
	
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	public static TreeMap<String, String> getRooms(HipChatApiProcessor processor) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (HipChatRoom room : processor.getRooms().items) {
			map.put(room.name, room.id);
		}
		return map;
	}
	
	public static boolean isRootProject(SProject project) {
		return project.getParentProject().getProjectId().equals(HipChatConfiguration.ROOT_PROJECT_ID);
	}

	public static HipChatProjectConfiguration findFirstSpecificParentConfiguration(SProject project, HipChatConfiguration configuration) {
		HipChatProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(project.getParentProjectId());
		if ((!isRootProject(project) && projectConfiguration == null) ||
				(projectConfiguration != null && projectConfiguration.getRoomId().equals(HipChatConfiguration.ROOM_ID_PARENT))) {
			return findFirstSpecificParentConfiguration(project.getParentProject(), configuration);
		} else if (projectConfiguration != null) {
			return projectConfiguration;
		}
		return null;
	}
	
	public static HipChatProjectConfiguration determineProjectConfiguration(SProject project, HipChatConfiguration configuration) {
		return determineProjectConfiguration(project, configuration, true);
	}
	
	public static HipChatProjectConfiguration determineProjectConfiguration(SProject project, HipChatConfiguration configuration, boolean resolveParent) {
		String projectId = project.getProjectId();
		String roomId = configuration.getDefaultRoomId();
		boolean notify = configuration.getDefaultNotifyStatus();
		boolean isRootProject = Utils.isRootProject(project);
		logger.debug(String.format("Default configuration for project ID %s: %s, %s", projectId, roomId, notify));
		logger.debug(String.format("Is root project: %s", isRootProject));
		
		HipChatProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(projectId);
		if (projectConfiguration != null) {
			roomId = projectConfiguration.getRoomId();
			notify = projectConfiguration.getNotifyStatus();
			if (roomId.equals(HipChatConfiguration.ROOM_ID_PARENT) && resolveParent) {
				HipChatProjectConfiguration parentProjectConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
				if (parentProjectConfiguration != null) {
					logger.debug("Using specific configuration in hierarchy determined implicitly");
					roomId = parentProjectConfiguration.getRoomId();
					notify = parentProjectConfiguration.getNotifyStatus();
				}
			}
			logger.debug(String.format("Found specific configuration for project ID %s: %s, %s", projectId, roomId, notify));
		} else if (!isRootProject) {
			roomId = configuration.getDefaultRoomId();
			HipChatProjectConfiguration parentProjectConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
			if (parentProjectConfiguration != null) {
				logger.debug("Found specific configuration in hierarchy");
				roomId = parentProjectConfiguration.getRoomId();
				notify = parentProjectConfiguration.getNotifyStatus();
			}
			logger.debug(String.format("Traversed hierarchy for project ID %s: %s, %s", projectId, roomId, notify));
		}
		
		return new HipChatProjectConfiguration(projectId, roomId, notify);
	}
		
}
