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

import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.PositionConstraint;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class HipChatConfigurationPageExtension extends AdminPage {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	private static final String AFTER_PAGE_ID = "jabber";
	private static final String BEFORE_PAGE_ID = "clouds";
	private static final String PAGE = "adminSettings.jsp";
	private static final String PLUGIN_NAME = "hipChat";

	private static final String TAB_TITLE = "HipChat Notifier";
	private static final String ROOM_ID_LIST = "roomIdList";
	private HipChatConfiguration configuration;
	private HipChatApiProcessor processor;
	private HipChatNotificationMessageTemplates templates;
	private HipChatEmoticonCache emoticonCache;

	public HipChatConfigurationPageExtension(@NotNull PagePlaces pagePlaces, 
			@NotNull PluginDescriptor descriptor, 
			@NotNull HipChatConfiguration configuration, 
			@NotNull HipChatApiProcessor processor,
			@NotNull HipChatNotificationMessageTemplates templates,
			@NotNull HipChatServerExtension serverExtension,
			@NotNull HipChatEmoticonCache emoticonCache) {
		super(pagePlaces);
		setPluginName(PLUGIN_NAME);
		setIncludeUrl(descriptor.getPluginResourcesPath(PAGE));
		setTabTitle(TAB_TITLE);
		ArrayList<String> after = new ArrayList<String>();
		after.add(AFTER_PAGE_ID);
		ArrayList<String> before = new ArrayList<String>();
		before.add(BEFORE_PAGE_ID);
		setPosition(PositionConstraint.between(after, before));
		this.configuration = configuration;
		this.processor = processor;
		this.templates = templates;
		this.emoticonCache = emoticonCache;
		register();
		logger.info("Global configuration page registered");
	}

	@Override
	public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
		super.fillModel(model, request);
		model.put(HipChatConfiguration.API_URL_KEY, this.configuration.getApiUrl());
		model.put(HipChatConfiguration.BYPASS_SSL_CHECK, this.configuration.getBypassSslCheck());
		model.put(HipChatConfiguration.API_TOKEN_KEY, this.configuration.getApiToken());
		model.put(HipChatConfiguration.DEFAULT_ROOM_ID_KEY, this.configuration.getDefaultRoomId());
		model.put(HipChatConfiguration.SERVER_EVENT_ROOM_ID_KEY, this.configuration.getDefaultRoomId());
		if (this.configuration.getServerEventRoomId() != null) {
			model.put(HipChatConfiguration.SERVER_EVENT_ROOM_ID_KEY, this.configuration.getServerEventRoomId());
		}
		model.put(ROOM_ID_LIST, Utils.getRooms(this.processor));
		model.put(HipChatConfiguration.NOTIFY_STATUS_KEY, this.configuration.getDefaultNotifyStatus());
		model.put(HipChatConfiguration.DISABLED_STATUS_KEY, this.configuration.getDisabledStatus());
		model.put(HipChatConfiguration.EMOTICON_CACHE_SIZE_KEY, this.emoticonCache.getSize());
		
	    model.put("branchFilter", Boolean.valueOf(this.configuration.getBranchFilterEnabledStatus()));
	    model.put("branchFilterRegex", this.configuration.getBranchFilterRegex());

		if (this.configuration.getEvents() != null) {
			model.put(HipChatConfiguration.BUILD_STARTED_KEY, this.configuration.getEvents().getBuildStartedStatus());
			model.put(HipChatConfiguration.BUILD_SUCCESSFUL_KEY, this.configuration.getEvents().getBuildSuccessfulStatus());
			model.put(HipChatConfiguration.ONLY_AFTER_FIRST_BUILD_SUCCESSFUL_KEY, this.configuration.getEvents().getOnlyAfterFirstBuildSuccessfulStatus());
			model.put(HipChatConfiguration.BUILD_FAILED_KEY, this.configuration.getEvents().getBuildFailedStatus());
			model.put(HipChatConfiguration.ONLY_AFTER_FIRST_BUILD_FAILED_KEY, this.configuration.getEvents().getOnlyAfterFirstBuildFailedStatus());
			model.put(HipChatConfiguration.BUILD_INTERRUPTED_KEY, this.configuration.getEvents().getBuildInterruptedStatus());
			model.put(HipChatConfiguration.SERVER_STARTUP_KEY, this.configuration.getEvents().getServerStartupStatus());
			model.put(HipChatConfiguration.SERVER_SHUTDOWN_KEY, this.configuration.getEvents().getServerShutdownStatus());
		}
		
		try {
			model.put(HipChatNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_STARTED).toString());
			model.put(HipChatNotificationMessageTemplates.BUILD_STARTED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.BUILD_STARTED_DEFAULT_TEMPLATE));
			model.put(HipChatNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_SUCCESSFUL).toString());
			model.put(HipChatNotificationMessageTemplates.BUILD_SUCCESSFUL_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.BUILD_SUCCESSFUL_DEFAULT_TEMPLATE));
			model.put(HipChatNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_FAILED).toString());
			model.put(HipChatNotificationMessageTemplates.BUILD_FAILED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.BUILD_FAILED_DEFAULT_TEMPLATE));
			model.put(HipChatNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.BUILD_INTERRUPTED).toString());
			model.put(HipChatNotificationMessageTemplates.BUILD_INTERRUPTED_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.BUILD_INTERRUPTED_DEFAULT_TEMPLATE));
			model.put(HipChatNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.SERVER_STARTUP).toString());
			model.put(HipChatNotificationMessageTemplates.SERVER_STARTUP_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.SERVER_STARTUP_DEFAULT_TEMPLATE));
			model.put(HipChatNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_KEY, this.templates.readTemplate(TeamCityEvent.SERVER_SHUTDOWN).toString());
			model.put(HipChatNotificationMessageTemplates.SERVER_SHUTDOWN_TEMPLATE_DEFAULT_KEY, HtmlUtils.htmlEscape(HipChatNotificationMessageTemplates.SERVER_SHUTDOWN_DEFAULT_TEMPLATE));
		} catch (IOException e) {
			logger.error("Exception", e);
		}
				
		logger.debug("Configuration page variables populated");
	}
	
	@Override
	public String getGroup() {
		return SERVER_RELATED_GROUP;
	}

	@Override
	public boolean isAvailable(@NotNull HttpServletRequest request) {
		return super.isAvailable(request) && checkHasGlobalPermission(request, Permission.CHANGE_SERVER_SETTINGS);
	}
	
}
