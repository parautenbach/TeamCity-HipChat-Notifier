package com.whatsthatlight.teamcity.hipchat;

public class HipChatNotificationMessageTemplate {

	public class Parameters {
		
		public static final String FULL_NAME_PARAM = "fullName";
		public static final String BUILD_NUMBER = "buildNumber";
		public static final String TRIGGERED_BY = "triggeredBy";
		public static final String EMOTICON = "emoticon";
		public static final String CANCELLED_BY = "cancelledBy";
		
	}
	
	public static final String BUILD_STARTED = "Build \"<fullName>\" has started. This is build number <buildNumber>. Triggered by: <triggeredBy>. <emoticon>";
	public static final String BUILD_SUCCESSFUL = "Build \"<fullName>\" was successful. It was build number <buildNumber>. Triggered by: <triggeredBy>. <emoticon>";
	public static final String BUILD_FAILED = "Build \"<fullName>\" failed. It was build number <buildNumber>. Triggered by: <triggeredBy>. <emoticon>";
	public static final String BUILD_INTERRUPTED = "Build \"<fullName>\" was cancelled. It was build number <buildNumber>. Cancelled by: <cancelledBy>. <emoticon>";
	public static final String SERVER_STARTUP = "Build server started.";
	public static final String SERVER_SHUTDOWN = "Build server shutting down.";

}
