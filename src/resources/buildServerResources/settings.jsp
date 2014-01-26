<%@ include file="/include.jsp" %>

<c:url value="/configureHipChat.html" var="actionUrl"/>

Hello <%= request.getServerName() %>! The time is now <%= new java.util.Date() %>.
<%-- <jsp:useBean id="resourceRoot" type="java.lang.String" scope="request"/> --%>
<br>
The API URL is ${apiUrl}.

<bs:messages key="configurationSaved"/>

<form action="${actionUrl}" id="hipChatForm" method="post" onsubmit="return">
	<table>
		<tr>
			<td>
				<forms:textField name="apiUrl" value="${apiUrl}test"/>
			</td>
		</tr>
		<tr>
			<td>
				<div>
					<!-- <input type="button" id="hipChatSaveButton" onclick="$('hipChatSaveButton').disabled='true';" value="Save" /> <forms:saving id="hipChatSaveProgress" style="float:none" /> -->
					<forms:submit label="Save" />
					<!-- <forms:submit id="testConnection" type="button" label="Test connection" /> -->
					<forms:saving />
				</div>
			</td>
		</tr>
</form>