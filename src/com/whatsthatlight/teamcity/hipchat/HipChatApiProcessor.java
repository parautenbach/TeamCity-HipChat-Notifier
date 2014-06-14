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

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;

public class HipChatApiProcessor {
	
	private HipChatConfiguration configuration;
	
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	public HipChatApiProcessor(@NotNull HipChatConfiguration configuration) throws URISyntaxException {
		this.configuration = configuration;
	}
	
	public HipChatEmoticons getEmoticons(int startIndex) {
		try {
			URI uri = new URI(String.format("%s%s?start-index=%s", this.configuration.getApiUrl(), "emoticon", startIndex));
			String authorisationHeader = String.format("Bearer %s", this.configuration.getApiToken());

			// Make request
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet(uri.toString());
			getRequest.addHeader(HttpHeaders.AUTHORIZATION, authorisationHeader);
			getRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			HttpResponse getResponse = client.execute(getRequest);
			StatusLine status = getResponse.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_OK) {
				logger.error(String.format("Could not retrieve emoticons: %s %s", status.getStatusCode(), status.getReasonPhrase()));
				return null;
			}
			
			Reader reader = new InputStreamReader(getResponse.getEntity().getContent());
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(reader, HipChatEmoticons.class);
		} catch (Exception e) {
			logger.error("Could not get emoticons", e);
		}
		
		return null;
	}
	
	public HipChatRooms getRooms() {
		try {
			URI uri = new URI(String.format("%s%s", this.configuration.getApiUrl(), "room"));
			String authorisationHeader = String.format("Bearer %s", this.configuration.getApiToken());

			// Make request
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet(uri.toString());
			getRequest.addHeader(HttpHeaders.AUTHORIZATION, authorisationHeader);
			getRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			HttpResponse getResponse = client.execute(getRequest);
			StatusLine status = getResponse.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_OK) {
				logger.error(String.format("Could not retrieve rooms: %s %s", status.getStatusCode(), status.getReasonPhrase()));
				return new HipChatRooms(new ArrayList<HipChatRoom>(), 0, 0, null);
			}
			
			Reader reader = new InputStreamReader(getResponse.getEntity().getContent());
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(reader, HipChatRooms.class);
		} catch (Exception e) {
			logger.error("Could not get rooms", e);
		}
		
		return new HipChatRooms(new ArrayList<HipChatRoom>(), 0, 0, null);
	}
	
	public void sendNotification(HipChatRoomNotification notification, String roomId) {
		try {
			String resource = String.format("room/%s/notification", roomId);
			URI uri = new URI(String.format("%s%s", this.configuration.getApiUrl(), resource));
			String authorisationHeader = String.format("Bearer %s", this.configuration.getApiToken());

			// Serialise the notification to JSON
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(notification);
			logger.debug(json);

			// Make request
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(uri.toString());
			postRequest.addHeader(HttpHeaders.AUTHORIZATION, authorisationHeader);
			postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			postRequest.setEntity(new StringEntity(json, Charset.forName("UTF-8")));
			HttpResponse postResponse = client.execute(postRequest);
			StatusLine status = postResponse.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
				logger.error(String.format("Message could not be delivered: %s %s", status.getStatusCode(), status.getReasonPhrase()));
			}
		} catch (Exception e) {
			logger.error("Could not post room notification", e);
		}
	}
	
	public boolean testAuthentication() {
		try {
			String resource = String.format("room?auth_token=%s&auth_test=true", this.configuration.getApiToken());
			URI uri = new URI(String.format("%s%s", this.configuration.getApiUrl(), resource));

			// Make request
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet getRequest = new HttpGet(uri.toString());
			HttpResponse postResponse = client.execute(getRequest);
			StatusLine status = postResponse.getStatusLine();
			if (status.getStatusCode() == HttpStatus.SC_ACCEPTED) {
				return true;
			} else {
				logger.error(String.format("Authentication failed: %s %s", status.getStatusCode(), status.getReasonPhrase()));
			}
		} catch (Exception e) {
			logger.error("Request failed", e);
		}
		
		return false;		
	}
	
}
