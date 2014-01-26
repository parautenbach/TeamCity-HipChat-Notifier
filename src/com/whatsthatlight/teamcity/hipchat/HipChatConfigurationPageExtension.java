package com.whatsthatlight.teamcity.hipchat;

import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.PositionConstraint;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;

public class HipChatConfigurationPageExtension extends AdminPage {

	private static final String TAB_TITLE = "HipChat Notifier";
	private static final String PLUGIN_NAME = "hipChat";
	
	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

    //private PiazzaConfiguration piazzaConfiguration;
    //private Piazza piazza;

    public HipChatConfigurationPageExtension(@NotNull PagePlaces pagePlaces, 
    		@NotNull PluginDescriptor descriptor) {
        super(pagePlaces);
        setPluginName(PLUGIN_NAME);
        setIncludeUrl(descriptor.getPluginResourcesPath("settings.jsp"));
        setTabTitle(TAB_TITLE);
        ArrayList<String> after = new ArrayList<String>();
        //after.add("email");
        after.add("jabber");
        ArrayList<String> before = new ArrayList<String>();
        before.add("cloud"); 
        //before.add("diagnostics");
        setPosition(PositionConstraint.between(after, before));
        register();
        logger.info("Global settings page registered");
    }

    public boolean isAvailable(@NotNull HttpServletRequest request) {
    	return super.isAvailable(request) && checkHasGlobalPermission(request, Permission.CHANGE_SERVER_SETTINGS);
    }
    
//    @Override
//    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
//        super.fillModel(model, request);
//        //model.put("showOnFailureOnly", piazzaConfiguration.isShowOnFailureOnly());
//        //model.put("resourceRoot", this.piazza.resourcePath(""));
//    }

	@Override
	public String getGroup() {
		return SERVER_RELATED_GROUP;
	}
	
}
