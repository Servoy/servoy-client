angular.module('custom_properties', ['webSocketModule'])
// Date type -----------------------------------------------
.run(function ($sabloConverters) {
	$sabloConverters.registerCustomPropertyHandler('Date', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			return typeof (serverJSONValue) === "number" ? new Date(serverJSONValue) : serverJSONValue;
		},
		
		fromClientToServer: function(newClientData, oldClientData) {
			  if(!newClientData) return null;
			  if(typeof newClientData == 'string') newClientData = new Date(newClientData);
			return newClientData.getTime();
		}
	});
})
// other small ones can be added here