package com.whatsthatlight.teamcity.hipchat;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(HipChatConfiguration.PROJECT_ROOM)
public class HipChatProjectConfiguration {
	
	@XStreamAlias(HipChatConfiguration.PROJECT_ID_KEY)
	private String projectId;
	
	@XStreamAlias(HipChatConfiguration.ROOM_ID_KEY)
	private String roomId;
	
	@XStreamAlias(HipChatConfiguration.NOTIFY_STATUS_KEY)
	private boolean notify;
	
	public HipChatProjectConfiguration(String projectId, String roomId, boolean notifyStatus) {
		this.projectId = projectId;
		this.roomId = roomId;
		this.notify = notifyStatus;
	}
	
	public String getProjectId() {
		return this.projectId;
	}

	public String getRoomId() {
		return this.roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
	public boolean getNotifyStatus() {
		return this.notify;
	}
	
	public void setNotifyStatus(boolean status) {
		this.notify = status;
	}
}
