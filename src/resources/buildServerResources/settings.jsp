<%@ include file="/include.jsp"%>

<jsp:useBean id="resourceRoot" type="java.lang.String" scope="request" />

<c:url value="/configureHipChat.html" var="actionUrl" />

<bs:linkCSS dynamic="${true}">
  ${resourceRoot}css/hipChat.css
</bs:linkCSS>

<bs:linkScript>
    ${resourceRoot}js/hipChat.js
</bs:linkScript>

<bs:refreshable containerId="hipChatComponent" pageUrl="${pageUrl}">
<form action="${actionUrl}" id="hipChatForm" method="post"
	onsubmit="return HipChat.save()">
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
		<table class="runnerFormTable">
			<tr>
				<th><label for="apiUrl">API URL: <l:star /></label></th>
				<td><forms:textField name="apiUrl" value="${apiUrl}" /></td>
			</tr>
			<tr>
				<th><label for="apiUrl">API token: <l:star /></label></th>
				<td><forms:textField name="apiToken" value="${apiToken}" /></td>
			</tr>
			<tr>
				<th><label for="roomId">Room ID: <l:star /></label></th>
				<td><forms:textField name="roomId" value="${roomId}" /></td>
			</tr>
			<tr>
				<th><label for="notify">Trigger notifications: </label></th>
				<td><forms:checkbox name="notify" checked="${notify}" /></td>
			</tr>
		</table>
		<div class="saveButtonsBlock">
			<forms:submit label="Save" />
			<!-- TODO: Test API connection -->
			<!-- <forms:submit id="testConnection" type="button"
					label="Test connection" /> -->
			<forms:saving />
		</div>
	</div>
</form>
</bs:refreshable>

<script type="text/javascript">
	(function($) {
		var sendAction = function(enable) {
			$.post("${actionUrl}?action=" + (enable ? 'enable' : 'disable'),
					function() {
						BS.reload(true);
					});
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
