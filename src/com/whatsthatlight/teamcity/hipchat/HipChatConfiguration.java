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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.*;

@XStreamAlias("hipchat")
public class HipChatConfiguration {

	public static final String API_TOKEN_KEY = "apiToken";
	public static final String API_URL_KEY = "apiUrl";
	public static final String DISABLED_STATUS_KEY = "disabled";
	public static final String NOTIFY_STATUS_KEY = "notify";
	public static final String DEFAULT_ROOM_ID_KEY = "defaultRoomId";
	public static final String DEFAULT_ROOM_ID_KEY_V0DOT1 = "roomId";
	public static final String ROOM_ID_KEY = "roomId";
	public static final String PROJECT_ID_KEY = "projectId";
	public static final String PROJECT_ROOM_KEY = "projectRoom";
	public static final String ROOM_ID_NONE_VALUE = "none";
	public static final String ROOM_ID_DEFAULT_VALUE = "default";
	public static final String ROOM_ID_PARENT_VALUE = "parent";
	public static final String IS_ROOT_PROJECT_KEY = "isRootProject";
	public static final String ROOT_PROJECT_ID_VALUE = "_Root";
	public static final String EVENTS_KEY = "events";
	public static final String BUILD_STARTED_KEY = "buildStarted";
	public static final String BUILD_SUCCESSFUL_KEY = "buildSuccessful";
	public static final String BUILD_FAILED_KEY = "buildFailed";
	public static final String BUILD_INTERRUPTED_KEY = "buildInterrupted";
	public static final String SERVER_STARTUP_KEY = "serverStartup";
	public static final String SERVER_SHUTDOWN_KEY = "serverShutdown";
	public static final String EMOTICON_CACHE_SIZE_KEY = "emoticonCacheSize";
	public static final String ONLY_AFTER_FIRST_BUILD_SUCCESSFUL_KEY = "onlyAfterFirstBuildSuccessful";
	public static final String ONLY_AFTER_FIRST_BUILD_FAILED_KEY = "onlyAfterFirstBuildFailed";

	@XStreamAlias(API_TOKEN_KEY)
	private String apiToken = null;

	@XStreamAlias(API_URL_KEY)
	private String apiUrl = "https://api.hipchat.com/v2/";

	@XStreamAlias(DISABLED_STATUS_KEY)
	private boolean disabled = false;

	@XStreamAlias(NOTIFY_STATUS_KEY)
	private boolean notify = false;

	@XStreamAlias(DEFAULT_ROOM_ID_KEY)
	private String defaultRoomId;
	
	// We use a list for correct serialization. It causes us to perform a linear search when getting or setting, but that's ok. 
	@XStreamImplicit
	private List<HipChatProjectConfiguration> projectRoomMap = new ArrayList<HipChatProjectConfiguration>();
	
	@XStreamAlias(HipChatConfiguration.EVENTS_KEY)
	private HipChatEventConfiguration events = new HipChatEventConfiguration();
	
	public HipChatConfiguration() {
		// Intentionally left empty
	}

	public HipChatEventConfiguration getEvents() {
		return this.events;
	}
	
	public void setEvents(HipChatEventConfiguration events) {
		this.events = events;
	}
	
	public List<HipChatProjectConfiguration> getProjectRoomMap() {
		return this.projectRoomMap;
	}
	
	public void setProjectConfiguration(HipChatProjectConfiguration newProjectConfiguration) {
		boolean found = false;
		for (HipChatProjectConfiguration projectConfiguration : this.projectRoomMap) {
			if (projectConfiguration.getProjectId().contentEquals(newProjectConfiguration.getProjectId())) {
				projectConfiguration.setRoomId(newProjectConfiguration.getRoomId());
				projectConfiguration.setNotifyStatus(newProjectConfiguration.getNotifyStatus());
				found = true;
			}
		}
		if (!found) {
			this.projectRoomMap.add(newProjectConfiguration);		
		}
	}
	
	public HipChatProjectConfiguration getProjectConfiguration(String projectId) {
		for (HipChatProjectConfiguration projectConfiguration : this.projectRoomMap) {
			if (projectConfiguration.getProjectId().contentEquals(projectId)) {
				return projectConfiguration;
			}
		}
		return null;
	}
	
	public String getApiToken() {
		return this.apiToken;
	}

	public String getApiUrl() {
		return this.apiUrl;
	}

	public boolean getDisabledStatus() {
		return this.disabled;
	}

	public boolean getDefaultNotifyStatus() {
		return this.notify;
	}

	public String getDefaultRoomId() {
		return this.defaultRoomId;
	}

	public void setApiToken(String token) {
		this.apiToken = token;
	}

	public void setApiUrl(String url) {
		// TODO: Validate URL
		this.apiUrl = url;
	}

	public void setDisabledStatus(boolean status) {
		this.disabled = status;
	}

	public void setNotifyStatus(boolean status) {
		this.notify = status;
	}

	public void setDefaultRoomId(String roomId) {
		this.defaultRoomId = roomId;
	}
	
}
