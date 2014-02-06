package com.whatsthatlight.teamcity.hipchat;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.codehaus.jackson.map.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.HttpHeaders;

import org.springframework.http.MediaType;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;

public class HipChatServerExtension extends BuildServerAdapter {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private SBuildServer server;
	private HipChatConfiguration configuration;

	public HipChatServerExtension(@NotNull SBuildServer server, @NotNull HipChatConfiguration configuration) {
		this.server = server;
		this.configuration = configuration;
		logger.debug("Server extension created");
	}

	public void register() {
		this.server.addListener(this);
		logger.debug("Server extension registered");
	}

	@Override
	public void buildStarted(SRunningBuild build) {
		logger.debug(String.format("Build started: %s", build.getBuildType().getName()));
		super.buildStarted(build);
		this.processBuildEvent(build, BuildEvent.STARTED);
	}

	@Override
	public void buildFinished(SRunningBuild build) {
		super.buildFinished(build);
		this.processBuildEvent(build, BuildEvent.FINISHED);
	}

	@Override
	public void buildInterrupted(SRunningBuild build) {
		super.buildInterrupted(build);
		this.processBuildEvent(build, BuildEvent.INTERRUPTED);
	}

	private void processBuildEvent(SRunningBuild build, BuildEvent buildEvent) {
		try {
			// triggered by
			// owner
			// status
			// type name
			// project name
			// committers
			// build.getFailureReasons()
			// link to build
			// build.getBuildType().getFullName()
			// build.getBuildType().getProjectName()
			logger.info(String.format("Received %s build event", buildEvent));
			if (!this.configuration.getDisabledStatus() && !build.isPersonal()) {
				logger.info("Processing build event");
				String apiUrl = this.configuration.getApiUrl();
				String apiToken = this.configuration.getApiToken();
				String roomId = this.configuration.getRoomId();
				String message = buildEvent.toString();
				String messageFormat = "text";
				String colour = "gray";
				boolean notify = this.configuration.getNotifyStatus();
				HipChatRoomNotification notification = new HipChatRoomNotification(message, messageFormat, colour, notify);
				postRoomNotification(apiUrl, apiToken, roomId, notification);
			}
		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}

	private static synchronized void postRoomNotification(String apiUrl, String apiToken, String roomId, HipChatRoomNotification notification) {
		try {
			String resource = String.format("room/%s/notification", roomId);
			String uri = String.format("%s%s", apiUrl, resource);
			URI u = new URI(uri);
			String authorisationHeader = String.format("Bearer %s", apiToken);

			// Serialise the notification to JSON
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(notification);
			logger.debug(json);

			HttpClient client = HttpClientBuilder.create().build();
			HttpPost postRequest = new HttpPost(u.toString());
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
