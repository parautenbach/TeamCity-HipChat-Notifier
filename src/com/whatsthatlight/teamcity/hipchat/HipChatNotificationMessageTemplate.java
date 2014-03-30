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

public class HipChatNotificationMessageTemplate {

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
		
	}
	
	public class Attributes {
		
		public static final String HAS_BRANCH = "hasBranch";
		public static final String HAS_CONTRIBUTORS = "hasContributors";
		
	}

	// Must we remove contributors from the start event?
	public static final String BUILD_STARTED = "Build <a href=\"${serverUrl}/project.html?projectId=${projectId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> has started. This is build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}&tab=buildResultsDiv&buildTypeId=${buildTypeId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_SUCCESSFUL = "Build <a href=\"${serverUrl}/project.html?projectId=${projectId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> was successful. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}&tab=buildResultsDiv&buildTypeId=${buildTypeId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_FAILED = "Build <a href=\"${serverUrl}/project.html?projectId=${projectId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> failed. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}&tab=buildResultsDiv&buildTypeId=${buildTypeId}\">#${buildNumber}</a> and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> <img src=\"${emoticonUrl}\">";
	public static final String BUILD_INTERRUPTED = "Build <a href=\"${serverUrl}/project.html?projectId=${projectId}\">${fullName}</a> <#if hasBranch>on branch <b>${branch}</b></#if> was cancelled. It was build number <a href=\"${serverUrl}/viewLog.html?buildId=${buildId}&tab=buildResultsDiv&buildTypeId=${buildTypeId}\">#${buildNumber}</a> and was cancelled by ${cancelledBy}. <img src=\"${emoticonUrl}\">";
	public static final String SERVER_STARTUP = "Build server started.";
	public static final String SERVER_SHUTDOWN = "Build server shutting down.";

}
