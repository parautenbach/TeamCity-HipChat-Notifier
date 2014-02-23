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
	public static final String PROJECT_ROOM = "projectRoom";

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
	
	@XStreamImplicit
	private List<HipChatProjectConfiguration> projectRoomMap = new ArrayList<HipChatProjectConfiguration>();
	
	public HipChatConfiguration() {
		// Intentionally left empty
	}

	public List<HipChatProjectConfiguration> getProjectRoomMap() {
		return this.projectRoomMap;
	}
	
	public void setProjectConfiguration(HipChatProjectConfiguration newProjectConfiguration) {
		boolean found = false;
		for (HipChatProjectConfiguration projectConfiguration : this.projectRoomMap) {
			if (projectConfiguration.getProjectId().contentEquals(newProjectConfiguration.getProjectId())) {
				projectConfiguration.setRoomId(newProjectConfiguration.getRoomId());
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

	public boolean getNotifyStatus() {
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
