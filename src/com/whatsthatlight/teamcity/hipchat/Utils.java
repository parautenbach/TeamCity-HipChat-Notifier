package com.whatsthatlight.teamcity.hipchat;

import java.util.TreeMap;

import jetbrains.buildServer.serverSide.SProject;

public class Utils {
	
	private static final String ROOT_PROJECT_ID = "_Root";
	
	public static TreeMap<String, String> getRooms(HipChatApiProcessor processor) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (HipChatRoom room : processor.getRooms().items) {
			map.put(room.name, room.id);
		}
		return map;
	}
	
	public static boolean isRootProject(SProject project) {
		return project.getParentProject().getProjectId().equals(ROOT_PROJECT_ID);
	}

	public static HipChatProjectConfiguration findFirstSpecificParentConfiguration(SProject project, HipChatConfiguration configuration) {
		HipChatProjectConfiguration projectConfiguration = configuration.getProjectConfiguration(project.getParentProjectId());
		if (!isRootProject(project) && projectConfiguration == null) {
			return findFirstSpecificParentConfiguration(project.getParentProject(), configuration);
		} else if (projectConfiguration != null) {
			return projectConfiguration;
		}
		return null;
	}
		
}
