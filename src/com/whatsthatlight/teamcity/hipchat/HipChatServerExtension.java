package com.whatsthatlight.teamcity.hipchat;

import java.util.HashMap;
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
	private HipChatApiProcessor processor;
	private static Random rng = new Random();
	private String messageFormat;
	private HashMap<TeamCityEvent, HipChatMessageBundle> eventMap;

	public HipChatServerExtension(@NotNull SBuildServer server, @NotNull HipChatConfiguration configuration, @NotNull HipChatApiProcessor processor) {
		this.server = server;
		this.configuration = configuration;
		this.processor = processor;
		this.messageFormat = HipChatMessageFormat.TEXT;
		this.eventMap = new HashMap<TeamCityEvent, HipChatMessageBundle>();
		this.eventMap.put(TeamCityEvent.BUILD_STARTED, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.BUILD_STARTED, 
						HipChatEmoticonSet.POSITIVE, 
						HipChatMessageColour.INFO));
		this.eventMap.put(TeamCityEvent.BUILD_SUCCESSFUL, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.BUILD_SUCCESSFUL, 
						HipChatEmoticonSet.POSITIVE, 
						HipChatMessageColour.SUCCESS));
		this.eventMap.put(TeamCityEvent.BUILD_FAILED, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.BUILD_FAILED, 
						HipChatEmoticonSet.NEGATIVE, 
						HipChatMessageColour.ERROR));
		this.eventMap.put(TeamCityEvent.BUILD_INTERRUPTED, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.BUILD_INTERRUPTED, 
						HipChatEmoticonSet.INDIFFERENT, 
						HipChatMessageColour.WARNING));
		this.eventMap.put(TeamCityEvent.SERVER_STARTUP, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.SERVER_STARTUP, 
						null, 
						HipChatMessageColour.NEUTRAL));
		this.eventMap.put(TeamCityEvent.SERVER_SHUTDOWN, 
				new HipChatMessageBundle(HipChatNotificationMessageTemplate.SERVER_SHUTDOWN, 
						null, 
						HipChatMessageColour.NEUTRAL));
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
		this.processBuildEvent(build, TeamCityEvent.BUILD_STARTED);
	}

	@Override
	public void buildFinished(SRunningBuild build) {
		super.buildFinished(build);
		if (build.getBuildStatus().isSuccessful()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_SUCCESSFUL);
		}
		else if (build.getBuildStatus().isFailed()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_FAILED);
		}
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
		super.buildInterrupted(build);
		this.processBuildEvent(build, TeamCityEvent.BUILD_INTERRUPTED);
	}

	@Override
	public void serverStartup() {
		this.processServerEvent(TeamCityEvent.SERVER_STARTUP);
	}

	@Override
	public void serverShutdown() {
		this.processServerEvent(TeamCityEvent.SERVER_SHUTDOWN);
	}

	private void processServerEvent(TeamCityEvent event) {
		boolean notify = this.configuration.getDefaultNotifyStatus();
		HipChatMessageBundle bundle = this.eventMap.get(event);
		String colour = bundle.getColour();
		String message = bundle.getTemplate();
		HipChatRoomNotification notification = new HipChatRoomNotification(message, this.messageFormat, colour, notify);
		String roomId = this.configuration.getDefaultRoomId();
		if (roomId != null) {
			this.processor.sendNotification(notification, roomId);
		}
	}
	
	private void processBuildEvent(SRunningBuild build, TeamCityEvent event) {
		try {
			logger.info(String.format("Received %s build event", event));
			if (!this.configuration.getDisabledStatus() && !build.isPersonal()) {
				logger.info("Processing build event");
				String message = createPlainTextBuildEventMessage(build, event);
				String colour = getBuildEventMessageColour(event);
				// TODO: Get specific notify status
				// TODO: Inherit room ID from parent
				boolean notify = this.configuration.getDefaultNotifyStatus();
				HipChatRoomNotification notification = new HipChatRoomNotification(message, this.messageFormat, colour, notify);
				// TODO: Skip if no default room ID, else get project's room ID, else default room ID
				String roomId = this.configuration.getDefaultRoomId();
				if (roomId != null) {
					this.processor.sendNotification(notification, roomId);
				}
			}
		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}

	private String getBuildEventMessageColour(TeamCityEvent buildEvent) {
		return this.eventMap.get(buildEvent).getColour();
	}

	private String createPlainTextBuildEventMessage(SRunningBuild build, TeamCityEvent buildEvent) {
		HipChatMessageBundle bundle = this.eventMap.get(buildEvent);
		ST template = new ST(bundle.getTemplate());
		String emoticon = getRandomEmoticon(bundle.getEmoticonSet());

		template.add(HipChatNotificationMessageTemplate.Parameters.EMOTICON, emoticon);		
		template.add(HipChatNotificationMessageTemplate.Parameters.FULL_NAME_PARAM, build.getBuildType().getFullName());
		template.add(HipChatNotificationMessageTemplate.Parameters.BUILD_NUMBER, build.getBuildNumber());
		template.add(HipChatNotificationMessageTemplate.Parameters.TRIGGERED_BY, build.getTriggeredBy().getAsString());
		if (buildEvent == TeamCityEvent.BUILD_INTERRUPTED) {
			long userId = build.getCanceledInfo().getUserId();
			SUser user = this.server.getUserModel().findUserById(userId);
			template.add(HipChatNotificationMessageTemplate.Parameters.CANCELLED_BY, user.getDescriptiveName());
		}
		
		return template.render();
	}

	private static String getRandomEmoticon(String[] set) {
		int i = rng.nextInt(set.length);
		return set[i];
	}

}
