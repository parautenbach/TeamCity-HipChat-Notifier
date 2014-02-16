package com.whatsthatlight.teamcity.hipchat;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;

public class HipChatNotificationProcessor {
	
	private HipChatConfiguration configuration;
	
	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	
	public HipChatNotificationProcessor(@NotNull HipChatConfiguration configuration) throws URISyntaxException {
		this.configuration = configuration;
	}
	
	public void process(HipChatRoomNotification notification) {
		try {
			String resource = String.format("room/%s/notification", configuration.getDefaultRoomId());
			URI uri = new URI(String.format("%s%s", configuration.getApiUrl(), resource));
			String authorisationHeader = String.format("Bearer %s", configuration.getApiToken());

			// Serialise the notification to JSON
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(notification);
			logger.debug(json);

			// Make request
			HttpClient client = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(uri.toString());
			postRequest.addHeader(HttpHeaders.AUTHORIZATION, authorisationHeader);
			postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
			postRequest.setEntity(new StringEntity(json));
			HttpResponse postResponse = client.execute(postRequest);
			StatusLine status = postResponse.getStatusLine();
			if (status.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
				logger.error(String.format("Message could not be delivered: %s %s", status.getStatusCode(), status.getReasonPhrase()));
			}
		} catch (Exception e) {
			logger.error("Could not post room notification", e);
		}
	}
	
}
