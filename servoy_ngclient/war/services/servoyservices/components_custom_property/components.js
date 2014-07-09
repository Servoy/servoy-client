angular.module('components_custom_property', ['webSocketModule'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters,$utils) {
	$sabloConverters.registerCustomPropertyHandler('component[]', {
		notifier : null,
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			if (serverJSONValue) {
				newValue = serverJSONValue;
				var executeHandler = function(name,type,event,row) {
					if (!newValue.requests) newValue.requests = [];
					var newargs = $utils.getEventArgs(event,type);
					newValue.requests.push({beanName: name, eventType: type,args:newargs, rowId : row});
					if (notifier) notifier();
				};
				newValue.setChangeNotifier = function(changeNotifier) {
					notifier = changeNotifier; 
				}
				newValue.isChanged = function() { return newValue.requests && (newValue.requests.length > 0); }
				
				for (var c in serverJSONValue) {
					if (!serverJSONValue[c].api) serverJSONValue[c].api = {};
					if (serverJSONValue[c].handlers)
					{
						for (var key in serverJSONValue[c].handlers) 
						{
							var handler = serverJSONValue[c].handlers[key];
							(function(key,beanName) {
								var eventHandler = function (args,rowId)
								{
									return executeHandler(beanName,key,args,rowId);
								}
								eventHandler.selectRecordHandler = function(rowId){
									return function () { return eventHandler(arguments,rowId) }
								};
								serverJSONValue[c].handlers[key] = eventHandler;
							})(key,handler.beanName);
						}
					}
				}
			}
			return serverJSONValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData && newClientData.isChanged()) {
				var tmp = newClientData.requests;
				newClientData.requests = null;
				return tmp;
			}
			return [];
		}
	});
});
