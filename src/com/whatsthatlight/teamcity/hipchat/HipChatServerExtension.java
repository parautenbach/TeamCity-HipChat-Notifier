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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildStatistics;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;

public class HipChatServerExtension extends BuildServerAdapter {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");
	private SBuildServer server;
	private HipChatConfiguration configuration;
	private HipChatApiProcessor processor;
	private static Random rng = new Random();
	private String messageFormat;
	private HashMap<TeamCityEvent, HipChatMessageBundle> eventMap;
	private HipChatNotificationMessageTemplates templates;
	private HipChatEmoticonCache emoticonCache;

	public HipChatServerExtension(@NotNull SBuildServer server, 
			@NotNull HipChatConfiguration configuration, 
			@NotNull HipChatApiProcessor processor, 
			@NotNull HipChatNotificationMessageTemplates templates, 
			@NotNull HipChatEmoticonCache emoticonCache) {
		this.server = server;
		//this.configDirectory = serverPaths.getConfigDir();
		this.configuration = configuration;
		this.processor = processor;
		this.templates = templates;
		this.messageFormat = HipChatMessageFormat.HTML;
		this.eventMap = new HashMap<TeamCityEvent, HipChatMessageBundle>();
		this.eventMap.put(TeamCityEvent.BUILD_STARTED, new HipChatMessageBundle(HipChatEmoticonSet.POSITIVE, HipChatMessageColour.INFO));
		this.eventMap.put(TeamCityEvent.BUILD_SUCCESSFUL, new HipChatMessageBundle(HipChatEmoticonSet.POSITIVE, HipChatMessageColour.SUCCESS));
		this.eventMap.put(TeamCityEvent.BUILD_FAILED, new HipChatMessageBundle(HipChatEmoticonSet.NEGATIVE, HipChatMessageColour.ERROR));
		this.eventMap.put(TeamCityEvent.BUILD_INTERRUPTED, new HipChatMessageBundle(HipChatEmoticonSet.INDIFFERENT, HipChatMessageColour.WARNING));
		this.eventMap.put(TeamCityEvent.SERVER_STARTUP, new HipChatMessageBundle(null, HipChatMessageColour.NEUTRAL));
		this.eventMap.put(TeamCityEvent.SERVER_SHUTDOWN,new HipChatMessageBundle(null, HipChatMessageColour.NEUTRAL));
		this.emoticonCache = emoticonCache;
		logger.debug("Server extension created");
	}

	public void register() {
		this.server.addListener(this);
		logger.debug("Server extension registered");
		//this.controller.IsInitialised();
	}
	
	@Override
	public void changesLoaded(SRunningBuild build) {
		logger.debug(String.format("Build started: %s", build.getBuildType().getName()));
		super.changesLoaded(build);
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getBuildStartedStatus()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_STARTED);
		}
	}
	
	@Override
	public void buildFinished(SRunningBuild build) {
		super.buildFinished(build);
		Branch branch = build.getBranch();
		List<SFinishedBuild> buildHistory = build.getBuildType().getHistory();
		SFinishedBuild previousBuild = null;
				
		if (branch != null) {			
			for (SFinishedBuild tmpBuild : buildHistory) {
				Branch tmpBranch = tmpBuild.getBranch();
				if ((build.getBuildId() != tmpBuild.getBuildId()) && tmpBranch.getName().equals(branch.getName())) {
					previousBuild = tmpBuild;
					break;
				}
			}
		} else {
			if (buildHistory.size() > 1) {
				previousBuild = buildHistory.get(1);
			}
		}
		
		if (build.getBuildStatus().isSuccessful() && this.configuration.getEvents() != null && this.configuration.getEvents().getBuildSuccessfulStatus()) {
			if (!this.configuration.getEvents().getOnlyAfterFirstBuildSuccessfulStatus() || previousBuild == null || previousBuild.getBuildStatus().isFailed()) {
				this.processBuildEvent(build, TeamCityEvent.BUILD_SUCCESSFUL);
			}
		} else if (build.getBuildStatus().isFailed() && this.configuration.getEvents() != null && this.configuration.getEvents().getBuildFailedStatus()) {
			if (!this.configuration.getEvents().getOnlyAfterFirstBuildFailedStatus() || previousBuild == null || previousBuild.getBuildStatus().isSuccessful()) {
				this.processBuildEvent(build, TeamCityEvent.BUILD_FAILED);
			}
		}
	}
	
	@Override
	public void buildInterrupted(SRunningBuild build) {
		super.buildInterrupted(build);
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getBuildInterruptedStatus()) {
			this.processBuildEvent(build, TeamCityEvent.BUILD_INTERRUPTED);
		}
	}
	
	@Override
	public void serverStartup() {
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getServerStartupStatus()) {
			this.processServerEvent(TeamCityEvent.SERVER_STARTUP);
		}
	}

	@Override
	public void serverShutdown() {
		if (this.configuration.getEvents() != null && this.configuration.getEvents().getServerShutdownStatus()) {
			this.processServerEvent(TeamCityEvent.SERVER_SHUTDOWN);
		}
	}
	
	private void processServerEvent(TeamCityEvent event) {
		try {
			boolean notify = this.configuration.getDefaultNotifyStatus();
			HipChatMessageBundle bundle = this.eventMap.get(event);
			String colour = bundle.getColour();
			String message = renderTemplate(this.templates.readTemplate(event), new HashMap<String, Object>());
			HipChatRoomNotification notification = new HipChatRoomNotification(message, this.messageFormat, colour, notify);
			String roomId = this.configuration.getDefaultRoomId();
			if ((event == TeamCityEvent.SERVER_STARTUP || event == TeamCityEvent.SERVER_SHUTDOWN) && 
					this.configuration.getServerEventRoomId() != null) {
				roomId = this.configuration.getServerEventRoomId();
			}
			if (roomId != null) {
				this.processor.sendNotification(notification, roomId);
			}
		} catch (Exception e) {
			logger.error(String.format("Error processing server event: %s", event), e);
		}
	}
	
	private void processBuildEvent(SRunningBuild build, TeamCityEvent event) {
		try {
			logger.info(String.format("Received %s build event", event));
			if (!this.configuration.getDisabledStatus() && !build.isPersonal()) {
				
		        Branch branch = build.getBranch();
		        if ((this.configuration.getBranchFilterEnabledStatus()) && (branch != null)) {
		          String branchDisplayName = branch.getDisplayName();
		          if (branchDisplayName.matches(this.configuration.getBranchFilterRegex())) {
		            logger.debug(String.format("Branch %s skipped", new Object[] { branchDisplayName }));
		            return;
		          }
		        }
				
				logger.info("Processing build event");
				String message = createHtmlBuildEventMessage(build, event);
				String colour = getBuildEventMessageColour(event);
				ProjectManager projectManager = this.server.getProjectManager();
				SProject project = projectManager.findProjectById(build.getProjectId());
				HipChatProjectConfiguration projectConfiguration = Utils.determineProjectConfiguration(project, configuration);
				HipChatRoomNotification notification = new HipChatRoomNotification(message, this.messageFormat, colour, projectConfiguration.getNotifyStatus());
				String roomId = projectConfiguration.getRoomId();
				logger.debug(String.format("Room to be notified: %s", roomId));
				if (!Utils.IsRoomIdNullOrNone(roomId)) {
					if (roomId.equals(HipChatConfiguration.ROOM_ID_DEFAULT_VALUE)) {
						roomId = configuration.getDefaultRoomId();
					} else if (roomId.equals(HipChatConfiguration.ROOM_ID_PARENT_VALUE)) {
						HipChatProjectConfiguration parentProjectConfiguration = Utils.findFirstSpecificParentConfiguration(project, configuration);
						if (parentProjectConfiguration != null) {
							logger.debug("Using specific configuration in hierarchy determined implicitly");
							roomId = parentProjectConfiguration.getRoomId();
							notification.notify = parentProjectConfiguration.getNotifyStatus();
						}
					}
					
					if (!Utils.IsRoomIdNullOrNone(roomId)) {
						logger.debug(String.format("Room notified: %s", roomId));
						this.processor.sendNotification(notification, roomId);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Could not process build event", e);
		}
	}

	private String getBuildEventMessageColour(TeamCityEvent buildEvent) {
		return this.eventMap.get(buildEvent).getColour();
	}
		
	private String createHtmlBuildEventMessage(SRunningBuild build, TeamCityEvent buildEvent) throws TemplateException, IOException {	
		HipChatMessageBundle bundle = this.eventMap.get(buildEvent);
		Template template = this.templates.readTemplate(buildEvent);
		
		// Emoticon
		String emoticon = getRandomEmoticon(bundle.getEmoticonSet());
		logger.debug(String.format("Emoticon: %s", emoticon));
		String emoticonUrl = this.emoticonCache.get(emoticon);

		// Branch
		Branch branch = build.getBranch();
		boolean hasBranch = branch != null;
		logger.debug(String.format("Has branch: %s", hasBranch));
		String branchDisplayName = "";
		if (hasBranch) {
			branchDisplayName = branch.getDisplayName();
			logger.debug(String.format("Branch: %s", branchDisplayName));
		}
		
		// Contributors (committers)
		String contributors = getContributors(build);
		boolean hasContributors = !contributors.isEmpty();
		logger.debug(String.format("Has contributors: %s", hasContributors));
		
		// Fill the template.
		Map<String, Object> templateMap = new HashMap<String, Object>();		
		
		// Build statistics
		logger.debug("Adding build statistics");
		BuildStatistics statistics = build.getFullStatistics();
		logger.debug(String.format("Number of tests: %s", statistics.getAllTestCount()));
		logger.debug(String.format("Number of passed tests: %s", statistics.getPassedTestCount()));
		logger.debug(String.format("Number of failed tests: %s", statistics.getFailedTestCount()));
		logger.debug(String.format("Number of new failed tests: %s", statistics.getNewFailedCount()));
		logger.debug(String.format("Number of ignored tests: %s", statistics.getIgnoredTestCount()));
		logger.debug(String.format("Tests duration: %s", statistics.getTotalDuration()));
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.NO_OF_TESTS, statistics.getAllTestCount());
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.NO_OF_PASSED_TESTS, statistics.getPassedTestCount());
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.NO_OF_FAILED_TESTS, statistics.getFailedTestCount());
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.NO_OF_NEW_FAILED_TESTS, statistics.getNewFailedCount());
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.NO_OF_IGNORED_TESTS, statistics.getIgnoredTestCount());
		templateMap.put(HipChatNotificationMessageTemplates.Parameters.DURATION_OF_TESTS, statistics.getTotalDuration());
		Map<String, BigDecimal> allStatistics = build.getStatisticValues();
		for (Map.Entry<String, BigDecimal> statistic : allStatistics.entrySet()) {
			logger.debug(String.format("\t%s: %s", statistic.getKey(), statistic.getValue()));
			templateMap.put(String.format("%s.%s", HipChatNotificationMessageTemplates.STATS_PARAMETERS_PREFIX, statistic.getKey()), statistic.getValue());
		}
		
//		// TODO: Add artifact dependencies as a template variable
//		try {
//			SBuildType buildType = build.getBuildType();
//			Collection<SBuildType> childDependencies = buildType.getChildDependencies();
//			for (SBuildType sBuildType : childDependencies) {
//				SFinishedBuild changes = sBuildType.getLastChangesFinished();
//				changes.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
//			} 
//			logger.debug(String.format("Children: %s", childDependencies.isEmpty()));
//			List<Dependency> dependencies = buildType.getDependencies();
//			//dependencies.get(0).
//			logger.debug(String.format("Children: %s", dependencies.isEmpty()));
//			List<SBuildType> dependencyReferences = buildType.getDependencyReferences();
//			logger.debug(String.format("Children: %s", dependencyReferences.isEmpty()));
//		} catch (Exception e) {
//		}
				
		// Add all available project, build configuration, agent, server, etc. parameters to the data model
		// These are accessed as ${.data_model["some.variable"]}
		// See: http://freemarker.org/docs/ref_specvar.html
		logger.debug("Adding build parameters");
		for (Map.Entry<String, String> entry : build.getParametersProvider().getAll().entrySet()) {
			logger.debug(String.format("\t%s: %s", entry.getKey(), entry.getValue()));
			templateMap.put(entry.getKey(), entry.getValue());
		}
		logger.debug("Adding agent parameters");
		for (Map.Entry<String, String> entry : build.getAgent().getAvailableParameters().entrySet()) {
			logger.debug(String.format("\t%s: %s", entry.getKey(), entry.getValue()));
			templateMap.put(entry.getKey(), entry.getValue());
		}
		// Standard plugin parameters
		logger.debug("Adding standard parameters");
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.EMOTICON_URL, emoticonUrl == null ? "" : emoticonUrl);		
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.FULL_NAME, build.getBuildType().getFullName());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.TRIGGERED_BY, build.getTriggeredBy().getAsString());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.HAS_CONTRIBUTORS, hasContributors);
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.CONTRIBUTORS, contributors);
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.HAS_BRANCH, hasBranch);
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.BRANCH, branchDisplayName);
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.SERVER_URL, this.server.getRootUrl());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.PROJECT_ID, build.getProjectExternalId());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.BUILD_ID, new Long(build.getBuildId()).toString());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.BUILD_TYPE_ID, build.getBuildTypeExternalId());
	    templateMap.put(HipChatNotificationMessageTemplates.Parameters.BUILD_NUMBER, build.getBuildNumber());
		if (buildEvent == TeamCityEvent.BUILD_INTERRUPTED) {
			long userId = build.getCanceledInfo().getUserId();
			SUser user = this.server.getUserModel().findUserById(userId);
			templateMap.put(HipChatNotificationMessageTemplates.Parameters.CANCELLED_BY, user.getDescriptiveName());
		}
		
		return renderTemplate(template, templateMap);
	}

	private static String getContributors(SBuild build) {
		UserSet<SUser> committers = build.getCommitters(SelectPrevBuildPolicy.SINCE_LAST_BUILD);	
		Collection<String> userSet = new HashSet<String>();
		for (SUser committer : committers.getUsers()) {
			userSet.add(committer.getDescriptiveName());
		}
		List<String> userList = new ArrayList<String>(userSet);
		Collections.sort(userList, String.CASE_INSENSITIVE_ORDER);
		String contributors = Utils.join(userList);
		return contributors;
	}
	
	private static String renderTemplate(Template template, Map<String, Object> templateMap) throws TemplateException, IOException {
		Writer writer = new StringWriter();
	    template.process(templateMap, writer);
	    writer.flush();
	    String renderedTemplate = writer.toString();
	    writer.close();
	    return renderedTemplate;		
	}
	
	private static String getRandomEmoticon(String[] set) {
		int i = rng.nextInt(set.length);
		return set[i];
	}

}
