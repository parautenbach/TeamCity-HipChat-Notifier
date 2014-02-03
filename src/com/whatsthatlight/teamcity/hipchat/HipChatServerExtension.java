package com.whatsthatlight.teamcity.hipchat;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

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
			// check enabled?
			// https://api.hipchat.com/v1/rooms/message/?auth_token=960dd39a8aedd02ba194ab0ef5a70c&room_id=389590&from=TeamCity&notify=1
			// personal
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

			// logger.info("Received build event");
			HipChatRoomNotification notification = new HipChatRoomNotification("test", "text", "gray", true);
			this.postRoomNotification(notification);

		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}

	private void postRoomNotification(HipChatRoomNotification notification) {
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(clientConfig);
		String roomId = "389590";
		String roomNotificationResource = String.format("room/%s/notification", roomId);
		String resource = String.format("%s%s", this.configuration.getApiUrl(), roomNotificationResource);
		WebResource webResource = client.resource(resource);
		String authorisationHeader = String.format("Bearer %s", this.configuration.getApiToken());
		logger.debug(webResource.getURI());
		// @formatter:off
		ClientResponse webResponse = 
				webResource.header(HttpHeaders.AUTHORIZATION, authorisationHeader)
				.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, notification);
		// @formatter:on
		Status clientResponseStatus = Status.fromStatusCode(webResponse.getStatus());
		logger.debug(String.format("Reponse status: %s %s", clientResponseStatus.getStatusCode(), clientResponseStatus));
		if (clientResponseStatus != Status.NO_CONTENT) {
			logger.error(String.format("Message could not be delivered: %s %s", clientResponseStatus.getStatusCode(), clientResponseStatus));
		}
	}

}
