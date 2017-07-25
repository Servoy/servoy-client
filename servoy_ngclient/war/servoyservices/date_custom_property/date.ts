angular.module('date_custom_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters, $sabloUtils, $q, $sabloTestability,$sabloApplication) {
	$sabloConverters.registerCustomPropertyHandler('svy_date', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var dateObj = new Date(serverJSONValue);
			return dateObj;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (!newClientData) return null;

			var r = newClientData;
			if (typeof newClientData === 'string' || typeof newClientData === 'number') r = new Date(newClientData);
			if (isNaN(r.getTime())) throw new Error("Invalid date/time value: " + newClientData);
			return moment(r).format();
		},
		
		updateAngularScope: function(clientValue, componentScope) {
			// nothing to do here
		}
		
	});
})
