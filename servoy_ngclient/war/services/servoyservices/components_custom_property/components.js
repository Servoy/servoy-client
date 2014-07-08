angular.module('components_custom_property', ['webSocketModule'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters) {
	$sabloConverters.registerCustomPropertyHandler('component[]', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			if (serverJSONValue) {
				for (var c in serverJSONValue) {
					if (!serverJSONValue[c].api) serverJSONValue[c].api = {};
				}
			}
			return serverJSONValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			return {};
		}
	});
});
