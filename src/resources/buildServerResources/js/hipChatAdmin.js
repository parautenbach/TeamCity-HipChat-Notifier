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