var HipChat = {
	save : function() {
		BS.ajaxRequest($('hipChatForm').action, {
			parameters : 'edit=1' + 
			'&apiUrl=' + $('apiUrl').value + 
			'&apiToken=' + $('apiToken').value +
			'&roomId=' + $('roomId').value +
			'&notify=' + $('notify').value,
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