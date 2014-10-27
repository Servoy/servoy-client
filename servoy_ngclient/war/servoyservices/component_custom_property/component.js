angular.module('component_custom_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property'])
// Component type ------------------------------------------
.value("$componentTypeConstants", {
    CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
    CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters, $utils, $viewportModule, $servoyInternal, $log, $foundsetTypeConstants) {
	var PROPERTY_UPDATES_KEY = "propertyUpdates";

	var MODEL_KEY = "model";
	var MODEL_VIEWPORT_KEY = "model_vp";
	var MODEL_VIEWPORT_CHANGES_KEY = "model_vp_ch";
	var MODEL_VIEWPORT = "modelViewport";
	
	var PROPERTY_NAME_KEY = "pn";
	var VALUE_KEY = "v";

	var CONVERSIONS = 'conversions';

	var NO_OP = "n";

	function getChildPropertyChanges(propertyValue, oldBeanModel, componentScope) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];

		var newBeanModel = propertyValue.model;
		if (angular.isUndefined(oldBeanModel)) oldBeanModel = newBeanModel; // for child components who's custom prop. changed
		var childChangedNotifier = getBeanPropertyChangeNotifier(propertyValue, componentScope); 
		var beanConversionInfo = $utils.getInDepthProperty(internalState, CONVERSIONS);
		
		// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
		var containerSize = {width: 0, height: 0};
		
		return $servoyInternal.getComponentChanges(newBeanModel, oldBeanModel, beanConversionInfo, internalState.beanLayout, containerSize, childChangedNotifier, componentScope);
	};
	
	function getBeanPropertyChangeNotifier(propertyValue, componentScope) {
		var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
		return function (oldBeanModel) { // oldBeanModel is only set when called from bean model in-depth watch; not set for nested comp. custom properties
			internalState.requests.push({ propertyChanges : getChildPropertyChanges(propertyValue, oldBeanModel, componentScope) });
			if (internalState.notifier) internalState.notifier();
		};
	};
	
	function watchModel(beanModel, childChangedNotifier, componentScope) {
		// TODO refine this watch; it doesn't need to go deep into complex properties as those handle their own changes!
		return componentScope.$watch(function() {
			return beanModel;
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			childChangedNotifier(oldvalue);
		}, true);
	}
	
	$sabloConverters.registerCustomPropertyHandler('component', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope) {
			var newValue = currentClientValue;

			if (serverJSONValue && serverJSONValue[PROPERTY_UPDATES_KEY]) {
				// granular updates received
				var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				var beanUpdate = serverJSONValue[PROPERTY_UPDATES_KEY];

				var modelBeanUpdate = beanUpdate[MODEL_KEY];
				var wholeViewportUpdate = beanUpdate[MODEL_VIEWPORT_KEY];
				var viewportUpdate = beanUpdate[MODEL_VIEWPORT_CHANGES_KEY];
				var done = false;
				
				if (modelBeanUpdate) {
					var childChangedNotifier = getBeanPropertyChangeNotifier(currentClientValue, componentScope); 
					var beanModel = currentClientValue.model;

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					var beanLayout = internalState.beanLayout;
					var containerSize = {width: 0, height: 0};

					var modelUpdateConversionInfo = modelBeanUpdate[CONVERSIONS] ? $utils.getOrCreateInDepthProperty(internalState, CONVERSIONS) : undefined;

					$servoyInternal.applyBeanData(beanModel, beanLayout, modelBeanUpdate, containerSize, childChangedNotifier, currentConversionInfo, modelBeanUpdate[CONVERSIONS], componentScope);
					done = true;
				}
				
				// if component is linked to a foundset, then record - dependent property values are sent over as as viewport representing values for the foundset property's viewport
				if (wholeViewportUpdate) {
					if (!angular.isDefined(currentClientValue[MODEL_VIEWPORT])) currentClientValue[MODEL_VIEWPORT] = [];

					$viewportModule.updateWholeViewport(currentClientValue, MODEL_VIEWPORT,
							internalState, wholeViewportUpdate, beanUpdate[CONVERSIONS] && beanUpdate[CONVERSIONS][MODEL_VIEWPORT_KEY] ?
							beanUpdate[CONVERSIONS][MODEL_VIEWPORT_KEY] : undefined, componentScope);
					done = true;
				} else if (viewportUpdate) {
					$viewportModule.updateViewportGranularly(currentClientValue, MODEL_VIEWPORT, internalState, viewportUpdate,
							beanUpdate[CONVERSIONS] && beanUpdate[CONVERSIONS][MODEL_VIEWPORT_CHANGES_KEY] ?
							beanUpdate[CONVERSIONS][MODEL_VIEWPORT_CHANGES_KEY] : undefined, componentScope);
					done = true;
				}
				
				if (!done) {
					$log.error("Can't interpret component server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2));
				}
			} else if (!angular.isDefined(serverJSONValue) || !serverJSONValue[NO_OP]) {
				// full contents received
				newValue = serverJSONValue;
				
				if (newValue) {
					$sabloConverters.prepareInternalState(newValue);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

					var executeHandler = function(type,event,row) {
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
					internalState.requests = [];
					internalState.beanLayout = null; // not really useful right now; just to be able to reuse existing form code 

					internalState.modelUnwatch = null;
					var childChangedNotifier = getBeanPropertyChangeNotifier(newValue, componentScope); 

					// calling applyBeanData initially to make sure any needed conversions are done on model's properties
					var beanModel = serverJSONValue.model;

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					internalState.beanLayout = {};
					var containerSize = {width: 0, height: 0};

					var currentConversionInfo = beanModel[CONVERSIONS] ? $utils.getOrCreateInDepthProperty(internalState, CONVERSIONS) : undefined;

					$servoyInternal.applyBeanData(beanModel, internalState.beanLayout, beanModel, containerSize, childChangedNotifier, currentConversionInfo, beanModel[CONVERSIONS], componentScope);
					delete beanModel.conversions; // delete the conversion info from component accessible model; it will be kept separately only
					
					// TODO when component property will be able to send itself entirely we need to handle viewport conversions here as well; for
					// now it is sent entirely only as part of the template - when there is no viewport available (no app. even)

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
					
					/** rowId is only needed if the component is linked to a foundset */
					serverJSONValue.apply =  function(property, componentModel, rowId) {
						var conversionInfo = internalState[CONVERSIONS];
						var propertyValue = componentModel[property];

						if (conversionInfo && conversionInfo[property]) {
							propertyValue = $sabloConverters.convertFromClientToServer(propertyValue, conversionInfo[property], undefined);
						} else {
							propertyValue = $sabloUtils.convertClientObject(propertyValue);
						}

						var req = { svyApply: {} };
						
						req.svyApply[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
						req.svyApply[PROPERTY_NAME_KEY] = property;
						req.svyApply[VALUE_KEY] = propertyValue;

						internalState.requests.push(req);
						if (internalState.notifier) internalState.notifier();
					};
					
					// TODO move apply above into servoyApi as well
					// here we don't specify any of the following as all those can be forwarded by the parent component from it's own servoyApi:
					// showForm, hideForm, setFormEnabled, setFormReadOnly,	getFormUrl
					serverJSONValue.servoyApi = {
							/** rowId is only needed if the component is linked to a foundset */
							startEdit: function(property, rowId) {
								var req = { svyStartEdit: {} };
								
								req.svyStartEdit[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
								req.svyStartEdit[PROPERTY_NAME_KEY] = property;

								internalState.requests.push(req);
								if (internalState.notifier) internalState.notifier();
							}
					}

					if (componentScope) internalState.modelUnwatch = watchModel(beanModel, childChangedNotifier, componentScope);
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
					internalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
	});
});
