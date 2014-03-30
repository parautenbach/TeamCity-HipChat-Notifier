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
		public static final String EMOTICON = "emoticon";
		public static final String CANCELLED_BY = "cancelledBy";
		public static final String CONTRIBUTORS = "contributors";		
		public static final String BRANCH = "branch";
		
	}
	
	public class Attributes {
		
		public static final String HAS_BRANCH = "hasBranch";
		public static final String HAS_CONTRIBUTORS = "hasContributors";
		
	}

	// Must we remove contributors from the start event?
	public static final String BUILD_STARTED = "Build ${fullName} <#if hasBranch>on branch <b>${branch}</b></#if> has started. This is build number #${buildNumber} and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> ${emoticon}";
	public static final String BUILD_SUCCESSFUL = "Build ${fullName} <#if hasBranch>on branch <b>${branch}</b></#if> was successful. It was build number #${buildNumber} and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> ${emoticon}";
	public static final String BUILD_FAILED = "Build ${fullName} <#if hasBranch>on branch <b>${branch}</b></#if> failed. It was build number #${buildNumber} and was triggered by ${triggeredBy}. <#if hasContributors>Contributors: ${contributors}.</#if> ${emoticon}";
	public static final String BUILD_INTERRUPTED = "Build ${fullName} <#if hasBranch>on branch <b>${branch}</b></#if> was cancelled. It was build number #${buildNumber} and was cancelled by ${cancelledBy}. ${emoticon}";
	public static final String SERVER_STARTUP = "Build server started.";
	public static final String SERVER_SHUTDOWN = "Build server shutting down.";

}
