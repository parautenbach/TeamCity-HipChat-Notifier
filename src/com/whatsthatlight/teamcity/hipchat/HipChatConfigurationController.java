package com.whatsthatlight.teamcity.hipchat;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;

public class HipChatConfigurationController extends BaseController {

    private HipChatConfiguration configuration;

	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

	public HipChatConfigurationController(SBuildServer server, WebControllerManager manager, HipChatConfiguration configuration) {
        super(server);
        manager.registerController("/configureHipChat.html", this);
        this.configuration = configuration;
        logger.info("Controller created");
    }
    
	@Override
	protected ModelAndView doHandle(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		configuration.setApiUrl(request.getParameter("apiUrl"));
		getOrCreateMessages(request).addMessage("configurationSaved", "Saved");
		
		return null;
	}

}
