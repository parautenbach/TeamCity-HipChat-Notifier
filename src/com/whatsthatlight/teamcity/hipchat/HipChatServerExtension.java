package com.whatsthatlight.teamcity.hipchat;

import java.util.Random;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.stringtemplate.v4.ST;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.users.SUser;

public class HipChatServerExtension extends BuildServerAdapter {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private SBuildServer server;
	private HipChatConfiguration configuration;
	private HipChatNotificationProcessor processor;
	private static Random rng = new Random();

	public HipChatServerExtension(@NotNull SBuildServer server, @NotNull HipChatConfiguration configuration, @NotNull HipChatNotificationProcessor processor) {
		this.server = server;
		this.configuration = configuration;
		this.processor = processor;
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
		this.processBuildEvent(build, TeamCityBuildEvent.STARTED);
	}

	@Override
	public void buildFinished(SRunningBuild build) {
		super.buildFinished(build);
		this.processBuildEvent(build, TeamCityBuildEvent.FINISHED);
	}

	@Override
	public void buildInterrupted(SRunningBuild build) {
		super.buildInterrupted(build);
		this.processBuildEvent(build, TeamCityBuildEvent.INTERRUPTED);
	}

	@Override
	public void serverStartup() {
		boolean notify = this.configuration.getNotifyStatus();
		String messageFormat = HipChatMessageFormat.TEXT;
		String colour = HipChatMessageColour.INFO;
		String message = "Build server started. :-)";
		HipChatRoomNotification notification = new HipChatRoomNotification(message, messageFormat, colour, notify);
		this.processor.process(notification);
	}

	@Override
	public void serverShutdown() {
		boolean notify = this.configuration.getNotifyStatus();
		String messageFormat = HipChatMessageFormat.TEXT;
		String colour = HipChatMessageColour.INFO;
		String message = "Build server shutting down. :-(";
		HipChatRoomNotification notification = new HipChatRoomNotification(message, messageFormat, colour, notify);
		this.processor.process(notification);
	}

	private void processBuildEvent(SRunningBuild build, TeamCityBuildEvent buildEvent) {
		try {
			logger.info(String.format("Received %s build event", buildEvent));
			if (!this.configuration.getDisabledStatus() && !build.isPersonal()) {
				logger.info("Processing build event");
				String message = createPlainTextBuildEventMessage(build, buildEvent);
				String messageFormat = HipChatMessageFormat.TEXT;
				String colour = getMessageColour(build, buildEvent);
				boolean notify = this.configuration.getNotifyStatus();
				HipChatRoomNotification notification = new HipChatRoomNotification(message, messageFormat, colour, notify);
				this.processor.process(notification);
			}
		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}

	private static String getMessageColour(SRunningBuild build, TeamCityBuildEvent buildEvent) {
		if (buildEvent == TeamCityBuildEvent.STARTED) {
			return HipChatMessageColour.START;
		} else if (buildEvent == TeamCityBuildEvent.FINISHED && build.getBuildStatus().isSuccessful()) {
			return HipChatMessageColour.SUCCESS;
		} else if (buildEvent == TeamCityBuildEvent.FINISHED && build.getBuildStatus().isFailed()) {
			return HipChatMessageColour.FAILURE;
		} else if (buildEvent == TeamCityBuildEvent.INTERRUPTED) {
			return HipChatMessageColour.INTERRUPTION;
		}

		return HipChatMessageColour.INFO;
	}

	private String createPlainTextBuildEventMessage(SRunningBuild build, TeamCityBuildEvent buildEvent) {
		try {
			// logger.debug(build.getRevisions().get(0).getRevision());
			// logger.debug(build.getRevisions().get(0).getRevisionDisplayName());
			logger.debug(build.getShortStatistics().getFailedTestCount()); // 0
			logger.debug(build.getShortStatistics().getPassedTestCount()); // 0
			logger.debug(build.isProbablyHanging()); // false
			// build.getBranch().=
			// build.getVcsRootEntries().get(0).
		} catch (Exception e) {
		}

		String message = "(Unknown)";
		if (buildEvent == TeamCityBuildEvent.STARTED) {
			ST buildStartedMessage = new ST("Build \"<fullName>\" has <status>. This is build number <buildNumber>, triggered by <triggeredBy>.");
			buildStartedMessage.add("fullName", build.getBuildType().getFullName());
			buildStartedMessage.add("status", buildEvent.toString().toLowerCase());
			buildStartedMessage.add("buildNumber", build.getBuildNumber());
			buildStartedMessage.add("triggeredBy", tryGetTriggeredByUser(build));
			message = buildStartedMessage.render();
		} else if (buildEvent == TeamCityBuildEvent.FINISHED && build.getBuildStatus().isSuccessful()) {
			message = "SUCCEEDED " + getRandomEmoticon(HipChatEmoticonSet.POSITIVE);
		} else if (buildEvent == TeamCityBuildEvent.FINISHED && build.getBuildStatus().isFailed()) {
			// List<BuildProblemData> reasons = build.getFailureReasons();
			// message = "FAILED: " + reasons.get(0).getDescription() + " (" +
			// reasons.size() + " reasons) " +
			// getRandomEmoticon(HipChatEmoticonSet.NEGATIVE);
			message = "FAILED " + getRandomEmoticon(HipChatEmoticonSet.NEGATIVE);
		} else if (buildEvent == TeamCityBuildEvent.INTERRUPTED) {
			message = "INTERRUPTED " + build.getCanceledInfo().getUserId() + " " + getRandomEmoticon(HipChatEmoticonSet.INDIFFERENT);
		}

		return message;
	}

	private static String getRandomEmoticon(String[] set) {
		int i = rng.nextInt(set.length);
		logger.debug("getRandomEmoticon: " + i);
		return set[i];
	}

	public String tryGetTriggeredByUser(SRunningBuild build) {
		if (build.getTriggeredBy() != null && build.getTriggeredBy().getUser() != null) {
			return build.getTriggeredBy().getUser().getDescriptiveName();
		} else {
			return null;
		}
	}

}
