package com.whatsthatlight.teamcity.hipchat;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

public class HipChatConfigurationController extends BaseController {

    private HipChatConfiguration configuration;

	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	public HipChatConfigurationController(@NotNull SBuildServer server, 
			@NotNull WebControllerManager manager, 
			@NotNull HipChatConfiguration configuration) {
        super(server);
        manager.registerController("/configureHipChat.html", this);
        this.configuration = configuration;
        logger.info("Controller created");
    }
    
	@Override
	protected ModelAndView doHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.debug("doHandle");
		logger.debug("edit=" + request.getParameter("edit"));
		logger.debug("apiUrl=" + request.getParameter("apiUrl"));
		logger.debug("apiToken=" + request.getParameter("apiToken"));
		logger.debug("action=" + request.getParameter("action"));
		if (request.getParameter("edit") != null) {
			configuration.setApiUrl(request.getParameter("apiUrl"));
			getOrCreateMessages(request).addMessage("configurationSaved", "Saved");
			//configuration.save();
		}
		
		if (request.getParameter("action") != null) {
			this.configuration.setStatus(request.getParameter("action").equals("enable"));
			//configuration.save();
		}

		return null;
	}

}
