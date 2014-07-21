angular.module('components_custom_property', ['webSocketModule', 'servoyApp'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters, $utils, $servoyInternal, $rootScope) {
	var PROPERTY_UPDATES = "propertyUpdates";
	
	function getChildPropertyChanges(propertyValue, beanIndex, oldBeanModel) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];

		var newBeanModel = propertyValue[beanIndex].model;
		if (angular.isUndefined(oldBeanModel)) oldBeanModel = newBeanModel; // for child components who's custom prop. changed
		var childChangedNotifier = getBeanPropertyChangeNotifier(propertyValue, beanIndex); 
		var beanConversionInfo = $utils.getInDepthProperty(internalState, 'conversions', beanIndex);
		
		// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
		var containerSize = {width: 0, height: 0};
		
		return $servoyInternal.getComponentChanges(newBeanModel, oldBeanModel, beanConversionInfo, internalState.beanLayout[beanIndex], containerSize, childChangedNotifier);
	};
	
	function getBeanPropertyChangeNotifier(propertyValue, beanIndex) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
		return function (oldBeanModel) { // oldBeanModel is only set when called from bean model in-depth watch; not set for nested comp. custom properties
			if (!internalState.requests) internalState.requests = [];
			internalState.requests.push({ propertyChanges : {
				beanIndex: beanIndex,
				changes: getChildPropertyChanges(propertyValue, beanIndex, oldBeanModel)
			}});
			if (internalState.notifier) internalState.notifier();
		};
	};
	
	function watchModel(beanModel, childChangedNotifier) {
		// TODO refine this watch; it doesn't need to go deep into complex properties as those handle their own changes!
		return $rootScope.$watch(function() {
			return beanModel;
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			childChangedNotifier(oldvalue);
		}, true);
	}
	
	$sabloConverters.registerCustomPropertyHandler('component[]', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;

			if ($.isArray(serverJSONValue)) {
				newValue = serverJSONValue;
				$sabloConverters.prepareInternalState(newValue);
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				
				var executeHandler = function(name,type,event,row) {
					if (!internalState.requests) internalState.requests = [];
					var newargs = $utils.getEventArgs(event,type);
					internalState.requests.push({ handlerExec: {
						beanName: name,
						eventType: type,
						args:newargs,
						rowId : row
					}});
					if (internalState.notifier) internalState.notifier();
				};
				
				// implement what $sabloConverters need to make this work
				internalState.setChangeNotifier = function(changeNotifier) {
					internalState.notifier = changeNotifier; 
				}
				internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }

				// private impl
				internalState.beanLayout = []; // not really useful right now; just to be able to reuse existing form code 
				
				internalState.modelUnwatch = [];
				for (var c in serverJSONValue) {
					var childChangedNotifier = getBeanPropertyChangeNotifier(newValue, c); 
					
					// calling applyBeanData initially to make sure any needed conversions are done on model's properties
					var beanModel = serverJSONValue[c].model;
					
					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					internalState.beanLayout[c] = {};
					var containerSize = {width: 0, height: 0};
					
					var currentConversionInfo = beanModel.conversions ? $utils.getOrCreateInDepthProperty(internalState, 'conversions', i) : undefined;
					
					$servoyInternal.applyBeanData(beanModel, internalState.beanLayout[c], beanModel, containerSize, childChangedNotifier, currentConversionInfo, beanModel.conversions);
					delete beanModel.conversions; // delete the conversion info from component accessible model; it will be kept separately only
					
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
					
					internalState.modelUnwatch[c] = watchModel(beanModel, childChangedNotifier);
				}
			} else {
				// granular updates received
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				if (serverJSONValue[PROPERTY_UPDATES]) {
					var updates = serverJSONValue[PROPERTY_UPDATES];
					var i;
					for (i in updates) {
						var childChangedNotifier = getBeanPropertyChangeNotifier(newValue, i); 
						var beanUpdate = updates[i];
						var beanModel = newValue[i].model;
						
						// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
						var beanLayout = internalState.beanLayout[i];
						var containerSize = {width: 0, height: 0};
						
						var currentConversionInfo = beanUpdate.conversions ? $utils.getOrCreateInDepthProperty(internalState, 'conversions', i) : undefined;

						$servoyInternal.applyBeanData(beanModel, beanLayout, beanUpdate, containerSize, childChangedNotifier, currentConversionInfo, beanUpdate.conversions);
					}
				}
			}
			
			if (angular.isDefined(currentClientValue) && newValue !== currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL]; // not using internalState to not override closure var of current/new value that will be used by nested functions
				var c;
				for (c in iS.modelUnwatch) {
					iS.modelUnwatch[c]();
				}
			}
			return newValue;
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
