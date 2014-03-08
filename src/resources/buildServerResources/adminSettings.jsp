<!--
Copyright 2014 Pieter Rautenbach

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<%@ include file="/include.jsp"%>

<c:url value="/configureHipChat.html" var="actionUrl" />

<bs:linkCSS dynamic="${true}">
  ${teamcityPluginResourcesPath}css/hipChatAdmin.css
</bs:linkCSS>

<bs:linkScript>
    ${teamcityPluginResourcesPath}js/hipChatAdmin.js
</bs:linkScript>

<form action="${actionUrl}" id="hipChatForm" method="post"
	onsubmit="return HipChatAdmin.save()">
	<div class="editNotificatorSettingsPage">
		<c:choose>
			<c:when test="${disabled}">
				<div class="pauseNote" style="margin-bottom: 1em;">
					The notifier is <strong>disabled</strong>. All HipChat
					notifications are suspended&nbsp;&nbsp;<a class="btn btn_mini"
						href="#" id="enable-btn">Enable</a>
				</div>
			</c:when>
			<c:otherwise>
				<div style="margin-left: 0.6em;">
					The notifier is <strong>enabled</strong>&nbsp;&nbsp;<a
						class="btn btn_mini" href="#" id="disable-btn">Disable</a>
				</div>
			</c:otherwise>
		</c:choose>
		<bs:messages key="configurationSaved" />
       	<br>		
		<table class="runnerFormTable">
			<tr class="groupingTitle">
          		<td colspan="2">General Configuration</td>
        	</tr>
			<tr>
				<th>
					<label for="apiUrl">API URL: <l:star /></label>
				</th>
				<td>
					<forms:textField name="apiUrl" value="${apiUrl}" />
					<span class="smallNote">This must be the base URL to the <a href="https://www.hipchat.com/docs/apiv2" target="_blank">HipChat version 2 API</a>.</span>
				</td>
			</tr>
			<tr>
				<th>
					<label for="apiToken">API token: <l:star /></label>
				</th>
				<td>
					<forms:textField name="apiToken" value="${apiToken}" />
					<span class="smallNote">A user OAuth token.</span>
				</td>
			</tr>
			<tr>
				<th><label for="defaultRoomId">Default room: </label></th>
				<td>
				  <forms:select name="defaultRoomId">
				  	<forms:option value="">(None)</forms:option>
                    <c:forEach var="roomIdEntry" items="${roomIdList}">
                      <forms:option value="${roomIdEntry.value}" selected="${roomIdEntry.value == defaultRoomId}">
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
			<tr class="groupingTitle">
          		<td colspan="2">Build Events Configuration</td>
        	</tr>
			<tr>
				<th><label for="buildStarted">Build started: </label></th>
				<td>
					<forms:checkbox name="buildStarted" checked="${buildStarted}" value="${buildStarted}"/>
				</td>
			</tr>
			<tr>
				<th><label for="buildSuccessful">Build successful: </label></th>
				<td>
					<forms:checkbox name="buildSuccessful" checked="${buildSuccessful}" value="${buildSuccessful}"/>
				</td>
			</tr>
			<tr>
				<th><label for="buildFailed">Build failed: </label></th>
				<td>
					<forms:checkbox name="buildFailed" checked="${buildFailed}" value="${buildFailed}"/>
				</td>
			</tr>
			<tr>
				<th><label for="buildInterrupted">Build interrupted: </label></th>
				<td>
					<forms:checkbox name="buildInterrupted" checked="${buildInterrupted}" value="${buildInterrupted}"/>
				</td>
			</tr>
			<tr class="groupingTitle">
          		<td colspan="2">Server Events Configuration</td>
        	</tr>
			<tr>
				<th><label for="serverStartup">Server startup: </label></th>
				<td>
					<forms:checkbox name="serverStartup" checked="${serverStartup}" value="${serverStartup}"/>
					<span class="smallNote">When checked, a message will be sent to the default room.</span>
				</td>
			</tr>
			<tr>
				<th><label for="serverShutdown">Server shutdown: </label></th>
				<td>
					<forms:checkbox name="serverShutdown" checked="${serverShutdown}" value="${serverShutdown}"/>
					<span class="smallNote">When checked, a message will be sent to the default room.</span>
				</td>
			</tr>
			</table>
		<div class="saveButtonsBlock">
			<forms:submit label="Save" />
			<forms:submit id="testConnection" type="button"	label="Test connection" onclick="return HipChatAdmin.testConnection()"/>
			<forms:saving />
		</div>
	</div>
</form>

<script type="text/javascript">
	(function($) {
		var sendAction = function(enable) {
			$.post("${actionUrl}?action=" + (enable ? 'enable' : 'disable'),
					function() {
						BS.reload(true);
					});
			$('hipChatComponent').refresh();
			return false;
		};

		$("#enable-btn").click(function() {
			return sendAction(true);
		});

		$("#disable-btn")
				.click(
						function() {
							if (!confirm("HipChat notifications will not be sent until enabled. Disable the notifier?"))
								return false;
							return sendAction(false);
						});
	})(jQuery);
</script>
