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
	
	private HipChatConfiguration configuration;
	private PluginDescriptor descriptor;
	private static Logger logger = (Logger) Logger.getLogger("com.whatsthatlight.teamcity.hipchat");

    public HipChatConfigurationPageExtension(@NotNull PagePlaces pagePlaces, 
    		@NotNull PluginDescriptor descriptor,
    		@NotNull HipChatConfiguration configuration) {
        super(pagePlaces);
        setPluginName(PLUGIN_NAME);
        setIncludeUrl(descriptor.getPluginResourcesPath("settings.jsp"));
        setTabTitle(TAB_TITLE);
        ArrayList<String> after = new ArrayList<String>();
        after.add("jabber");
        ArrayList<String> before = new ArrayList<String>();
        before.add("clouds"); 
        setPosition(PositionConstraint.between(after, before));
        this.configuration = configuration;
        this.descriptor = descriptor;
        register();
        logger.info("Global settings page registered");
    }

    public boolean isAvailable(@NotNull HttpServletRequest request) {
    	return super.isAvailable(request) && checkHasGlobalPermission(request, Permission.CHANGE_SERVER_SETTINGS);
    }
    
    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        super.fillModel(model, request);
        model.put("resourceRoot", this.descriptor.getPluginResourcesPath());
        model.put("apiUrl", configuration.getApiUrl());
        model.put("apiToken", configuration.getApiToken());
        logger.debug(this.configuration.getStatus());
        model.put("disabled", !this.configuration.getStatus());
        logger.info("fillModel");
    }

	@Override
	public String getGroup() {
		return SERVER_RELATED_GROUP;
	}
	
}
