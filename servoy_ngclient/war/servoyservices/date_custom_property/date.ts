angular.module('date_custom_property', ['webSocketModule'])
// Valuelist type -------------------------------------------
.run(function ($sabloConverters, $sabloUtils, $q, $sabloTestability,$sabloApplication) {
	var dateConverter =  {
			fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
				var dateObj = new Date(serverJSONValue);
				return dateObj;
			},

			fromClientToServer: function(newClientData, oldClientData) {
				if (!newClientData) return null;

				var r = newClientData;
				if (typeof newClientData === 'string' || typeof newClientData === 'number') r = new Date(newClientData);
				if (isNaN(r.getTime())) return null;//throw new Error("Invalid date/time value: " + newClientData);
				return moment(r).format();
			},
			
			updateAngularScope: function(clientValue, componentScope) {
				// nothing to do here
			}
			
		}
	$sabloConverters.registerCustomPropertyHandler('svy_date',dateConverter);
	// also set the default conversion (overwrite it)
	$sabloConverters.registerCustomPropertyHandler('Date',dateConverter);
})
