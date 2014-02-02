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
import java.util.Map;

public class HipChatConfigurationPageExtension extends AdminPage {

	private static final String TAB_TITLE = "HipChat Notifier";
	private static final String PLUGIN_NAME = "hipChat";
	private static final String PAGE = "settings.jsp";
	private static final String RESOURCE_ROOT = "resourceRoot";
	private static final String BEFORE_PAGE_ID = "clouds";
	private static final String AFTER_PAGE_ID = "jabber";
	
	private HipChatConfiguration configuration;
	private PluginDescriptor descriptor;
	
	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

    public HipChatConfigurationPageExtension(@NotNull PagePlaces pagePlaces, 
    		@NotNull PluginDescriptor descriptor,
    		@NotNull HipChatConfiguration configuration) {
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
        this.descriptor = descriptor;
        register();
        logger.info("Global configuration page registered");
    }

    public boolean isAvailable(@NotNull HttpServletRequest request) {
    	return super.isAvailable(request) && checkHasGlobalPermission(request, Permission.CHANGE_SERVER_SETTINGS);
    }
    
    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        model.put(RESOURCE_ROOT, this.descriptor.getPluginResourcesPath());
        model.put(HipChatConfiguration.API_URL_KEY, configuration.getApiUrl());
        model.put(HipChatConfiguration.API_TOKEN_KEY, configuration.getApiToken());
        model.put(HipChatConfiguration.DISABLED_STATUS_KEY, this.configuration.getDisabledStatus());
        logger.debug("Configuration page variables populated");
    }

	@Override
	public String getGroup() {
		return SERVER_RELATED_GROUP;
	}
	
}
