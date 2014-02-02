package com.whatsthatlight.teamcity.hipchat;

import com.thoughtworks.xstream.annotations.*;

@XStreamAlias("hipchat")
public class HipChatConfiguration {

	public static final String API_TOKEN_KEY = "apiToken";
	public static final String API_URL_KEY = "apiUrl";
	public static final String DISABLED_STATUS_KEY = "disabled";
	
	@XStreamAlias(API_TOKEN_KEY)
	private String apiToken = null;
	
	@XStreamAlias(API_URL_KEY)
	private String apiUrl = null; 
	
	@XStreamAlias(DISABLED_STATUS_KEY)
	private Boolean disabled = null; 

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
	
}
