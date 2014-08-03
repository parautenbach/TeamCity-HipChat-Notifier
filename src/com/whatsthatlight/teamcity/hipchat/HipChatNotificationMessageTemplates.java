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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.ServerPaths;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class HipChatNotificationMessageTemplates {

	private static Logger logger = Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	public class Parameters {
		
		public static final String FULL_NAME = "fullName";
		public static final String BUILD_NUMBER = "buildNumber";
		public static final String TRIGGERED_BY = "triggeredBy";
		public static final String EMOTICON_URL = "emoticonUrl";
		public static final String CANCELLED_BY = "cancelledBy";
		public static final String CONTRIBUTORS = "contributors";		
		public static final String BRANCH = "branch";
		public static final String SERVER_URL = "serverUrl";
		public static final String PROJECT_ID = "projectId";
		public static final String BUILD_ID = "buildId";
		public static final String BUILD_TYPE_ID = "buildTypeId";		
		public static final String HAS_BRANCH = "hasBranch";
		public static final String HAS_CONTRIBUTORS = "hasContributors";
		
	}

	public static final String BUILD_STARTED_TEMPLATE_KEY = "buildStartedTemplate";
	public static final String BUILD_SUCCESSFUL_TEMPLATE_KEY = "buildSuccessfulTemplate";
	public static final String BUILD_FAILED_TEMPLATE_KEY = "buildFailedTemplate";
	public static final String BUILD_INTERRUPTED_TEMPLATE_KEY = "buildInterruptedTemplate";
	public static final String SERVER_STARTUP_TEMPLATE_KEY = "serverStartupTemplate";
	public static final String SERVER_SHUTDOWN_TEMPLATE_KEY = "serverShutdownTemplate";	

	public static final String BUILD_STARTED_TEMPLATE_DEFAULT_KEY = "buildStartedTemplateDefault";
	public static final String BUILD_SUCCESSFUL_TEMPLATE_DEFAULT_KEY = "buildSuccessfulTemplateDefault";
	public static final String BUILD_FAILED_TEMPLATE_DEFAULT_KEY = "buildFailedTemplateDefault";
	public static final String BUILD_INTERRUPTED_TEMPLATE_DEFAULT_KEY = "buildInterruptedTemplateDefault";
	public static final String SERVER_STARTUP_TEMPLATE_DEFAULT_KEY = "serverStartupTemplateDefault";
	public static final String SERVER_SHUTDOWN_TEMPLATE_DEFAULT_KEY = "serverShutdownTemplateDefault";	

	public static final String BUILD_STARTED_DEFAULT_TEMPLATE = "Build <a href=\"${serverUrl}/viewType.html?buildTypeId=${buildTypeId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> has started. This is build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_SUCCESSFUL_DEFAULT_TEMPLATE = "Build <a href=\"${serverUrl}/viewType.html?buildTypeId=${buildTypeId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> was successful. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_FAILED_DEFAULT_TEMPLATE = "Build <a href=\"${serverUrl}/viewType.html?buildTypeId=${buildTypeId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> failed. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_INTERRUPTED_DEFAULT_TEMPLATE = "Build <a href=\"${serverUrl}/viewType.html?buildTypeId=${buildTypeId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> was cancelled. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}\">#${buildNumber}</a> and was cancelled by ${cancelledBy}. <img src=\"${emoticonUrl}\">";
	public static final String SERVER_STARTUP_DEFAULT_TEMPLATE = "Build server started.";
	public static final String SERVER_SHUTDOWN_DEFAULT_TEMPLATE = "Build server shutting down.";
		
	private static final String TEMPLATE_NAME_EXTENSION = ".ftl";
	private Configuration config;
	private String templateBasePathName;
	private Map<TeamCityEvent, String> defaultTemplateCache;
	private Map<TeamCityEvent, String> eventMap;

	public HipChatNotificationMessageTemplates(@NotNull ServerPaths serverPaths) throws IOException {		
		// Template caching: http://fmpp.sourceforge.net/freemarker/pgui_config_templateloading.html
		this.config = new Configuration();
		File templatePath = new File(serverPaths.getConfigDir(), HipChatConfigurationController.HIPCHAT_CONFIG_DIRECTORY);
		if (!templatePath.exists()) {
			templatePath.mkdir();
		}
		this.config.setDirectoryForTemplateLoading(templatePath);
		this.config.setDefaultEncoding("UTF-8");
		this.templateBasePathName = templatePath.getCanonicalPath();
		logger.debug(String.format("Set \"%s\" as the template path", templateBasePathName));
		
		this.defaultTemplateCache = new HashMap<TeamCityEvent, String>();
		this.defaultTemplateCache.put(TeamCityEvent.BUILD_STARTED, BUILD_STARTED_DEFAULT_TEMPLATE);
		this.defaultTemplateCache.put(TeamCityEvent.BUILD_SUCCESSFUL, BUILD_SUCCESSFUL_DEFAULT_TEMPLATE);
		this.defaultTemplateCache.put(TeamCityEvent.BUILD_FAILED, BUILD_FAILED_DEFAULT_TEMPLATE);
		this.defaultTemplateCache.put(TeamCityEvent.BUILD_INTERRUPTED, BUILD_INTERRUPTED_DEFAULT_TEMPLATE);
		this.defaultTemplateCache.put(TeamCityEvent.SERVER_STARTUP, SERVER_STARTUP_DEFAULT_TEMPLATE);
		this.defaultTemplateCache.put(TeamCityEvent.SERVER_SHUTDOWN, SERVER_SHUTDOWN_DEFAULT_TEMPLATE);
		
		this.eventMap = new HashMap<TeamCityEvent, String>();
		this.eventMap.put(TeamCityEvent.BUILD_STARTED, BUILD_STARTED_TEMPLATE_KEY);
		this.eventMap.put(TeamCityEvent.BUILD_SUCCESSFUL, BUILD_SUCCESSFUL_TEMPLATE_KEY);
		this.eventMap.put(TeamCityEvent.BUILD_FAILED, BUILD_FAILED_TEMPLATE_KEY);
		this.eventMap.put(TeamCityEvent.BUILD_INTERRUPTED, BUILD_INTERRUPTED_TEMPLATE_KEY);
		this.eventMap.put(TeamCityEvent.SERVER_STARTUP, SERVER_STARTUP_TEMPLATE_KEY);
		this.eventMap.put(TeamCityEvent.SERVER_SHUTDOWN, SERVER_SHUTDOWN_TEMPLATE_KEY);
	}
	
	public Template readTemplate(TeamCityEvent event) throws IOException {
		String templateName = this.eventMap.get(event);
		String fullPathName = getFullTemplatePath(this.templateBasePathName, templateName);
		logger.debug(String.format("Reading template %s", templateName));
		File fullPath = new File(fullPathName);
		if (fullPath.exists()) {
			return this.config.getTemplate(templateName + TEMPLATE_NAME_EXTENSION);
		}
		return createDefaultTemplate(this.defaultTemplateCache.get(event));		
	}

	public void writeTemplate(TeamCityEvent event, String template) throws IOException {
		String templateName = this.eventMap.get(event);
		String fullPathName = getFullTemplatePath(this.templateBasePathName, templateName);
		logger.debug(String.format("Writing template to %s", fullPathName));
		File fullPath = new File(fullPathName);
		if (!fullPath.exists()) {
			fullPath.createNewFile();
		}
		FileWriter fileWriter = new FileWriter(fullPath);
		fileWriter.write(template);
		fileWriter.flush();
		fileWriter.close();
	}

	private static String getFullTemplatePath(String path, String templateName) throws IOException {
		File filePath = new File(path, templateName + TEMPLATE_NAME_EXTENSION);
		return filePath.getCanonicalPath();
	}

	private static Template createDefaultTemplate(String templateString) throws IOException {
		String templateName = "template";
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(templateName, templateString);
		Configuration config = new Configuration();
		config.setTemplateLoader(loader);
		return config.getTemplate(templateName);
	}
}
