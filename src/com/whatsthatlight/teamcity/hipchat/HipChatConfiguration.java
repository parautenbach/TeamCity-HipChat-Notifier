package com.whatsthatlight.teamcity.hipchat;

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

	public HipChatConfiguration() {
		// Intentionally left empty
	}

	public String getApiToken() {
		return this.apiToken;
	}

	public String getApiUrl() {
		return this.apiUrl;
	}

	public Boolean getDisabledStatus() {
		return this.disabled;
	}

	public Boolean getNotifyStatus() {
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

	public void setDisabledStatus(Boolean status) {
		this.disabled = status;
	}

	public void setNotifyStatus(Boolean status) {
		this.notify = status;
	}

	public void setDefaultRoomId(String roomId) {
		this.defaultRoomId = roomId;
	}
}
