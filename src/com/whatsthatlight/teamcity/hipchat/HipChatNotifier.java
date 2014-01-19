package com.whatsthatlight.teamcity.hipchat;

import java.util.Collection;
import java.util.Set;

//import com.whatsthatlight.teamcity.Utils;

import jetbrains.buildServer.Build;
import jetbrains.buildServer.notification.Notificator;
import jetbrains.buildServer.notification.NotificatorRegistry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.mute.MuteInfo;
import jetbrains.buildServer.serverSide.problems.BuildProblemInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsRoot;

//import org.apache.log4j.Logger;

public class HipChatNotifier implements Notificator {

//	private static Logger logger = Logger.getLogger(HipChatNotifier.class);
	private static String name = "HipChat Notifier";
	private static String type = "HipChat Build Notifiers";
	
	public HipChatNotifier(NotificatorRegistry registry) {
		// Register the notifier in the TeamCity registry
		registry.register(this);
		//String version = this.getClass().getPackage().getImplementationVersion();
		//String logMessage = String.format("%1$s (%2$s) version %3$s registered", getDisplayName(), getNotificatorType(), version);
		//logger.info(logMessage);
	}
	
	@Override
	public String getDisplayName() {
		return name;
	}

	@Override
	public String getNotificatorType() {
		return type;
	}

	@Override
	public void notifyBuildFailed(SRunningBuild arg0, Set<SUser> arg1) {
		//logger.info("Build failed");
	}

	@Override
	public void notifyBuildFailedToStart(SRunningBuild arg0, Set<SUser> arg1) {
		//logger.info("Build failed to start");
	}

	@Override
	public void notifyBuildFailing(SRunningBuild arg0, Set<SUser> arg1) {
		//logger.info("Build failing");
	}

	@Override
	public void notifyBuildProbablyHanging(SRunningBuild arg0, Set<SUser> arg1) {
//		logger.info("Build hanging");
	}

	@Override
	public void notifyBuildProblemResponsibleAssigned(
			Collection<BuildProblemInfo> arg0, ResponsibilityEntry arg1,
			SProject arg2, Set<SUser> arg3) {
//		logger.info("Build problem responsibility assigned");		
	}

	@Override
	public void notifyBuildProblemResponsibleChanged(
			Collection<BuildProblemInfo> arg0, ResponsibilityEntry arg1,
			SProject arg2, Set<SUser> arg3) {
//		logger.info("Build problem responsible changed");
	}

	@Override
	public void notifyBuildProblemsMuted(Collection<BuildProblemInfo> arg0,
			MuteInfo arg1, Set<SUser> arg2) {
//		logger.info("Build problems muted");
	}

	@Override
	public void notifyBuildProblemsUnmuted(Collection<BuildProblemInfo> arg0,
			MuteInfo arg1, SUser arg2, Set<SUser> arg3) {
//		logger.info("Build problems unmuted");
	}

	@Override
	public void notifyBuildStarted(SRunningBuild arg0, Set<SUser> arg1) {
//		logger.info("Build started");
	}

	@Override
	public void notifyBuildSuccessful(SRunningBuild arg0, Set<SUser> arg1) {
//		logger.info("Build successful");
	}

	@Override
	public void notifyLabelingFailed(Build arg0, VcsRoot arg1, Throwable arg2,
			Set<SUser> arg3) {
//		logger.info("Build labeling failed");
	}

	@Override
	public void notifyResponsibleAssigned(SBuildType arg0, Set<SUser> arg1) {
//		logger.info("Build responsible assigned");
	}

	@Override
	public void notifyResponsibleAssigned(TestNameResponsibilityEntry arg0,
			TestNameResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
//		logger.info("Build responsible assigned for test");
	}

	@Override
	public void notifyResponsibleAssigned(Collection<TestName> arg0,
			ResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
//		logger.info("Build responsible assigned for test");
	}

	@Override
	public void notifyResponsibleChanged(SBuildType arg0, Set<SUser> arg1) {
//		logger.info("Build responsible changed");
	}

	@Override
	public void notifyResponsibleChanged(TestNameResponsibilityEntry arg0,
			TestNameResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
//		logger.info("Build responsible changed for test");
	}

	@Override
	public void notifyResponsibleChanged(Collection<TestName> arg0,
			ResponsibilityEntry arg1, SProject arg2, Set<SUser> arg3) {
//		logger.info("Build responsible changed");
	}

	@Override
	public void notifyTestsMuted(Collection<STest> arg0, MuteInfo arg1,
			Set<SUser> arg2) {
//		logger.info("Tests muted");
	}

	@Override
	public void notifyTestsUnmuted(Collection<STest> arg0, MuteInfo arg1,
			SUser arg2, Set<SUser> arg3) {
//		logger.info("Tests unmuted");
	}

}
