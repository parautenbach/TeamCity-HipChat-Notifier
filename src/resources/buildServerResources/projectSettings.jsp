<%@ include file="/include.jsp"%>

<c:url value="/configureHipChat.html" var="actionUrl" />

<bs:linkCSS dynamic="${true}">
  ${teamcityPluginResourcesPath}css/hipChatAdmin.css
</bs:linkCSS>

<bs:linkScript>
    ${teamcityPluginResourcesPath}js/hipChatAdmin.js
</bs:linkScript>

<form action="${actionUrl}" id="hipChatProjectForm" method="post"
	onsubmit="return HipChatProject.save()">
	<div class="editNotificatorSettingsPage">
		<bs:messages key="configurationSaved" />
		<table class="runnerFormTable">
			<tr>
				<th><label for="roomId">Room: </label></th>
				<td>
				  <forms:select name="roomId">
				  	<forms:option value="none" selected="${'none' == roomId}">(None)</forms:option>
				  	<forms:option value="default" selected="${'default' == roomId}">(Default)</forms:option>
				  	<c:if test="${!isRootProject}">
					  	<forms:option value="parent" selected="${'parent' == roomId}">(Parent)</forms:option>
					</c:if>                    
                    <c:forEach var="roomIdEntry" items="${roomIdList}">
                      <forms:option value="${roomIdEntry.value}" selected="${roomIdEntry.value == roomId}">
                        <c:out value="${roomIdEntry.key}"/>
                      </forms:option>
                    </c:forEach>
                  </forms:select>
                </td>
			</tr>
			<tr>
				<th><label for="notify">Trigger notifications: </label></th>
				<td>
					<forms:checkbox name="notify" checked="${notify}" value="${notify}"/>
					<span class="smallNote">When checked, a notification for all people in the room will be triggered, taking user preferences into account.</span>
				</td>
			</tr>
		</table>
		<div class="saveButtonsBlock">
			<forms:submit label="Save" />
			<input type="hidden" id="projectId" name="projectId" value="${projectId}"/>
			<forms:saving />
		</div>
	</div>
</form>