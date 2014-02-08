package com.whatsthatlight.teamcity.hipchat;

public class HipChatMessageBundle {

	private String template;
	private String[] emoticonSet;
	private String colour;
	
	public HipChatMessageBundle(String template, String[] emoticonSet, String colour) {
		this.template = template;
		this.emoticonSet = emoticonSet;
		this.colour = colour;
	}
	
	public String getTemplate() {
		return this.template;
	}
	
	public String[] getEmoticonSet() {
		return this.emoticonSet;
	}

	public String getColour() {
		return this.colour;
	}

}
