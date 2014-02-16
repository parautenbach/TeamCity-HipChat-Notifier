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

				$('hipChatComponent').refresh();
			}
		});
		return false;
	}
};