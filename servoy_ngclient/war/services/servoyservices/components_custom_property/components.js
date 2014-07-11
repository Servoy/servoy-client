angular.module('components_custom_property', ['webSocketModule'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters,$utils) {
	$sabloConverters.registerCustomPropertyHandler('component[]', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			if (serverJSONValue) {
				newValue = serverJSONValue;
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL] = {};
				var executeHandler = function(name,type,event,row) {
					if (!internalState.requests) internalState.requests = [];
					var newargs = $utils.getEventArgs(event,type);
					internalState.requests.push({beanName: name, eventType: type,args:newargs, rowId : row});
					if (internalState.notifier) internalState.notifier();
				};
				
				// implement what $sabloConverters need to make this work
				internalState.setChangeNotifier = function(changeNotifier) {
					internalState.notifier = changeNotifier; 
				}
				internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }

				// private impl
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
					serverJSONValue[c].apply =  function(property, componentModel, rowId) {
						// TODO when dataproviders will get sent through components; right now it goes through foundset
        				// $servoyInternal.pushDPChange("product", "datatextfield1c", property, componentModel, rowId);
						// alert("Apply called with: (" + rowId + ", " + property + ", " + componentModel[property] + ")");
					};
				}
			}
			return serverJSONValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var internalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					var tmp = internalState.requests;
					internalState.requests = null;
					return tmp;
				}
			}
			return [];
		}
	});
});
