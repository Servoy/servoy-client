angular.module('custom_properties', ['webSocketModule'])
// Date type -----------------------------------------------
.run(function ($sabloConverters) {
	$sabloConverters.registerCustomPropertyHandler('Date', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			return typeof (serverJSONValue) === "number" ? new Date(serverJSONValue) : serverJSONValue;
		},
		
		fromClientToServer: function(newClientData, oldClientData) {
			return newClientData != null ? newClientData.getTime() : newClientData;
		}
	});
})
// other small ones can be added here