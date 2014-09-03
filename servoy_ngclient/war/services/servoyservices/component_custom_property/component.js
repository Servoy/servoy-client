angular.module('component_custom_property', ['webSocketModule', 'servoyApp'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters, $utils, $servoyInternal, $rootScope) {
	var PROPERTY_UPDATES = "propertyUpdates";
	
	function getChildPropertyChanges(propertyValue, oldBeanModel) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];

		var newBeanModel = propertyValue.model;
		if (angular.isUndefined(oldBeanModel)) oldBeanModel = newBeanModel; // for child components who's custom prop. changed
		var childChangedNotifier = getBeanPropertyChangeNotifier(propertyValue); 
		var beanConversionInfo = $utils.getInDepthProperty(internalState, 'conversions');
		
		// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
		var containerSize = {width: 0, height: 0};
		
		return $servoyInternal.getComponentChanges(newBeanModel, oldBeanModel, beanConversionInfo, internalState.beanLayout, containerSize, childChangedNotifier);
	};
	
	function getBeanPropertyChangeNotifier(propertyValue) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
		return function (oldBeanModel) { // oldBeanModel is only set when called from bean model in-depth watch; not set for nested comp. custom properties
			if (!internalState.requests) internalState.requests = [];
			internalState.requests.push({ propertyChanges : getChildPropertyChanges(propertyValue, oldBeanModel) });
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
	
	$sabloConverters.registerCustomPropertyHandler('component', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;

			if (serverJSONValue && serverJSONValue[PROPERTY_UPDATES]) {
				// granular updates received
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				var beanUpdate = serverJSONValue[PROPERTY_UPDATES];
				var childChangedNotifier = getBeanPropertyChangeNotifier(newValue); 
				var beanModel = newValue.model;

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
				var beanLayout = internalState.beanLayout;
				var containerSize = {width: 0, height: 0};

				var currentConversionInfo = beanUpdate.conversions ? $utils.getOrCreateInDepthProperty(internalState, 'conversions') : undefined;

				$servoyInternal.applyBeanData(beanModel, beanLayout, beanUpdate, containerSize, childChangedNotifier, currentConversionInfo, beanUpdate.conversions);
			} else {
				// full contents received
				newValue = serverJSONValue;
				
				if (newValue) {
					$sabloConverters.prepareInternalState(newValue);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

					var executeHandler = function(type,event,row) {
						if (!internalState.requests) internalState.requests = [];
						var newargs = $utils.getEventArgs(event,type);
						internalState.requests.push({ handlerExec: {
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
					internalState.beanLayout = null; // not really useful right now; just to be able to reuse existing form code 

					internalState.modelUnwatch = null;
					var childChangedNotifier = getBeanPropertyChangeNotifier(newValue); 

					// calling applyBeanData initially to make sure any needed conversions are done on model's properties
					var beanModel = serverJSONValue.model;

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					internalState.beanLayout = {};
					var containerSize = {width: 0, height: 0};

					var currentConversionInfo = beanModel.conversions ? $utils.getOrCreateInDepthProperty(internalState, 'conversions') : undefined;

					$servoyInternal.applyBeanData(beanModel, internalState.beanLayout, beanModel, containerSize, childChangedNotifier, currentConversionInfo, beanModel.conversions);
					delete beanModel.conversions; // delete the conversion info from component accessible model; it will be kept separately only

					if (!serverJSONValue.api) serverJSONValue.api = {};
					if (serverJSONValue.handlers)
					{
						for (var key in serverJSONValue.handlers) 
						{
							var handler = serverJSONValue.handlers[key];
							(function(key) {
								var eventHandler = function (args,rowId)
								{
									return executeHandler(key,args,rowId);
								}
								eventHandler.selectRecordHandler = function(rowId){
									return function () { return eventHandler(arguments,rowId) }
								};
								serverJSONValue.handlers[key] = eventHandler;
							})(key);
						}
					}
					serverJSONValue.apply =  function(property, componentModel, rowId) {
						// TODO when dataproviders will get sent through components; right now it goes through foundset
						// $servoyInternal.pushDPChange("product", "datatextfield1c", property, componentModel, rowId);
						// alert("Apply called with: (" + rowId + ", " + property + ", " + componentModel[property] + ")");
					};

					internalState.modelUnwatch = watchModel(beanModel, childChangedNotifier);
				}
			}
			
			if (angular.isDefined(currentClientValue) && newValue !== currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL]; // not using internalState to not override closure var of current/new value that will be used by nested functions
				if (iS.modelUnwatch) {
					iS.modelUnwatch();
					iS.modelUnwatch = null;
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
			return {};
		}
	});
});
