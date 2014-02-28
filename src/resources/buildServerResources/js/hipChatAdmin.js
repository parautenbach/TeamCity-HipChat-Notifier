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
		BS.ajaxRequest($('hipChatProjectForm').action, {
			parameters : 'project=1' + 
			'&roomId=' + $('roomId').value +
			'&notify=' + $('notify').checked + 
			'&projectId=' + $('projectId').value,
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
	save : function() {
		BS.ajaxRequest($('hipChatForm').action, {
			parameters : 'edit=1' + 
			'&apiUrl=' + $('apiUrl').value + 
			'&apiToken=' + $('apiToken').value +
			'&defaultRoomId=' + $('defaultRoomId').value +
			'&notify=' + $('notify').checked,
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
		jQuery.ajax(
				{
					url: $('hipChatForm').action, 
					data: {
							test: 1, 
							apiUrl: $('apiUrl').value,
							apiToken: $('apiToken').value
						  },
					type: 'GET'
				}).done(function() {
					alert('Connection successful!');
				}).fail(function() {
					alert('Connection failed!')
				});
		return false;
	}
};