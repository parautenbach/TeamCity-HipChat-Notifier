/**
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
 */

var HipChatProject = {
	save : function() {
		BS.ajaxRequest($("hipChatProjectForm").action, {
			parameters : 
				"project=1" + 
				"&roomId="    + $("roomId").value +
				"&notify="    + $("notify").checked + 
				"&projectId=" + $("projectId").value,
			onComplete : function(transport) {
				if (transport.responseXML) {
					BS.XMLResponse.processErrors(transport.responseXML, {
						onProfilerProblemError : function(elem) {
							alert(elem.firstChild.nodeValue);
						}
					});
				}
				BS.reload(true);
			}
		});
		return false;
	}
};

var HipChatAdmin = {
	validate : function() {
		var apiUrl = document.forms["hipChatForm"]["apiUrl"].value;
		var apiToken = document.forms["hipChatForm"]["apiToken"].value;                  
		if (apiUrl == null || apiUrl == "" || apiToken == null || apiToken == "") {
			alert("You must specify a value for both the API URL and token.");
			return false;
		}
		return true;
	},
		
	save : function() {
		if (!HipChatAdmin.validate()) {
			return false;
		}
		
		BS.ajaxRequest($("hipChatForm").action, {
			method : "POST",
			parameters : 
				"edit=1" + 
				"&apiUrl="                        + $("apiUrl").value + 
				"&apiToken="                      + $("apiToken").value +
				"&defaultRoomId="                 + $("defaultRoomId").value +
				"&notify="                        + $("notify").checked + 
				"&buildStarted="                  + $("buildStarted").checked +
				"&buildSuccessful="               + $("buildSuccessful").checked +
				"&buildFailed="                   + $("buildFailed").checked +
				"&buildInterrupted="              + $("buildInterrupted").checked +
				"&serverStartup="                 + $("serverStartup").checked +
				"&serverShutdown="                + $("serverShutdown").checked + 
				"&onlyAfterFirstBuildSuccessful=" + $("onlyAfterFirstBuildSuccessful").checked +
				"&onlyAfterFirstBuildFailed="     + $("onlyAfterFirstBuildFailed").checked + 				
				"&buildStartedTemplate="          + encodeURIComponent(document.getElementById('buildStartedTemplate').value) +
				"&buildSuccessfulTemplate="       + encodeURIComponent(document.getElementById('buildSuccessfulTemplate').value) +
				"&buildFailedTemplate="           + encodeURIComponent(document.getElementById('buildFailedTemplate').value) +
				"&buildInterruptedTemplate="      + encodeURIComponent(document.getElementById('buildInterruptedTemplate').value) +
				"&serverStartupTemplate="         + encodeURIComponent(document.getElementById('serverStartupTemplate').value) +
				"&serverShutdownTemplate="        + encodeURIComponent(document.getElementById('serverShutdownTemplate').value),
			onComplete : function(transport) {
				if (transport.responseXML) {
					BS.XMLResponse.processErrors(transport.responseXML, {
						onProfilerProblemError : function(elem) {
							alert(elem.firstChild.nodeValue);
						}
					});
				}
				BS.reload(true);
			}
		});
		return false;
	},
	
	testConnection : function() {
		if (!HipChatAdmin.validate()) {
			return false;
		}
		
		jQuery.ajax(
				{
					url: $("hipChatForm").action, 
					data: {
							test: 1, 
							apiUrl: $("apiUrl").value,
							apiToken: $("apiToken").value
						  },
					type: "GET"
				}).done(function() {
					alert("Connection successful!");
				}).fail(function() {
					alert("Connection failed!")
				});
		return false;
	},
	
	reloadEmoticons : function() {
		if (!HipChatAdmin.validate()) {
			return false;
		}
		
		jQuery.ajax(
				{
					url: $("hipChatForm").action, 
					data: {
							reloadEmoticons: 1
						  },
					type: "GET"
				}).done(function() {
					alert("Reload successful!");
					BS.reload(true);
				}).fail(function() {
					alert("Reload failed!")
				});
		return false;
	}
};