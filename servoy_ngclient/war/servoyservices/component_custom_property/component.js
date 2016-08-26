angular.module('component_custom_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module','$sabloService'])
//Component type ------------------------------------------
.value("$componentTypeConstants", {
	CALL_ON_ONE_SELECTED_RECORD_IF_TEMPLATE : 0,
	CALL_ON_ALL_RECORDS_IF_TEMPLATE : 1
})
.run(function ($sabloConverters, $sabloUtils, $viewportModule, $servoyInternal, $log, $foundsetTypeConstants, $sabloUtils, $propertyWatchesRegistry,$sabloService) {
	var PROPERTY_UPDATES_KEY = "propertyUpdates";

	var MODEL_KEY = "model";
	var MODEL_VIEWPORT_KEY = "model_vp";
	var MODEL_VIEWPORT_CHANGES_KEY = "model_vp_ch";
	var MODEL_VIEWPORT = "modelViewport";

	var PROPERTY_NAME_KEY = "pn";
	var VALUE_KEY = "v";

	var CONVERSIONS = 'conversions';

	var NO_OP = "n";

	function getChildPropertyChanges(componentState, oldPropertyValue, newPropertyValue, propertyName) {
		var internalState = componentState[$sabloConverters.INTERNAL_IMPL];
		var beanConversionInfo = $sabloUtils.getInDepthProperty(internalState, CONVERSIONS);
		
		// just dummy stuff - currently the parent controls layout, but getComponentChanges needs such args...
		var containerSize = {width: 0, height: 0};

		return $servoyInternal.getComponentChanges(newPropertyValue, oldPropertyValue, beanConversionInfo, internalState.beanLayout, containerSize, propertyName, componentState.model);
	};

	function getBeanPropertyChangeNotifierGenerator(propertyValue) {
		return function beanPropertyChangeNotifierGenerator(propertyName) {
			if (!propertyValue) return undefined;

			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			return function beanPropertyChangeNotifier(oldValue, newValue, dumb) { // oldValue, newValue and dumb are only set when called from bean model in-depth/shallow watch; not set for smart properties
				if (dumb !== true) {
					// so smart property - no watch involved (it notifies itself as changed)
					oldValue = newValue = propertyValue[MODEL_KEY][propertyName];
				} 
				internalState.requests.push({ propertyChanges : getChildPropertyChanges(propertyValue, oldValue, newValue, propertyName) });
				if (internalState.changeNotifier) internalState.changeNotifier();
			};
		};
	};

	function watchModel(componentTypeName, beanModel, childChangedNotifierGenerator, componentScope) {
		// $propertyWatchesRegistry knows exactly which properties are to be watched based on component type
		return $propertyWatchesRegistry.watchDumbPropertiesForComponent(componentScope, componentTypeName, beanModel, function(newValue, oldValue, property) {
			childChangedNotifierGenerator(property)(oldValue, newValue, true);
		});
	};

	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			if (iS.modelUnwatch) {
				for (var unW in iS.modelUnwatch)
					iS.modelUnwatch[unW]();
				iS.modelUnwatch = null;
			}
			if (value[MODEL_VIEWPORT]) $viewportModule.removeDataWatchesFromRows(value[MODEL_VIEWPORT].length, iS);
		}
	};

	function addBackWatches(value, componentScope, childChangedNotifierGenerator) {
		if (angular.isDefined(value) && value !== null) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];

			var propertiesToWatch = $propertyWatchesRegistry.getPropertiesToAutoWatchForComponent(value.componentDirectiveName);
			if (typeof propertiesToWatch === 'undefined') propertiesToWatch = {}; // that will not add watches for any column; don't send undefined here as that would mean default (add to all)

			if (value[MODEL_VIEWPORT]) $viewportModule.addDataWatchesToRows(value[MODEL_VIEWPORT], iS, componentScope, false, propertiesToWatch);
			if (componentScope) iS.modelUnwatch = watchModel(value.componentDirectiveName, value.model, childChangedNotifierGenerator, componentScope);
		}
	};

	$sabloConverters.registerCustomPropertyHandler('component', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = currentClientValue;

			// remove watches to avoid an unwanted detection of received changes
			removeAllWatches(currentClientValue);

			var childChangedNotifierGenerator; 
			if (serverJSONValue && serverJSONValue[PROPERTY_UPDATES_KEY]) {
				// granular updates received
				childChangedNotifierGenerator = getBeanPropertyChangeNotifierGenerator(currentClientValue); 

				var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				var beanUpdate = serverJSONValue[PROPERTY_UPDATES_KEY];

				var modelBeanUpdate = beanUpdate[MODEL_KEY];
				var wholeViewportUpdate = beanUpdate[MODEL_VIEWPORT_KEY];
				var viewportUpdate = beanUpdate[MODEL_VIEWPORT_CHANGES_KEY];
				var done = false;

				if (modelBeanUpdate) {
					var beanModel = currentClientValue[MODEL_KEY];

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					var beanLayout = internalState.beanLayout;
					var containerSize = {width: 0, height: 0};

					var modelUpdateConversionInfo = modelBeanUpdate[CONVERSIONS] ? $sabloUtils.getOrCreateInDepthProperty(internalState, CONVERSIONS)
							: $sabloUtils.getInDepthProperty(internalState, CONVERSIONS);

					$servoyInternal.applyBeanData(beanModel, beanLayout, modelBeanUpdate, containerSize, childChangedNotifierGenerator,
							modelUpdateConversionInfo, modelBeanUpdate[CONVERSIONS], componentScope);
					done = true;
				}

				// if component is linked to a foundset, then record - dependent property values are sent over as as viewport representing values for the foundset property's viewport
				if (wholeViewportUpdate) {
					if (!angular.isDefined(currentClientValue[MODEL_VIEWPORT])) currentClientValue[MODEL_VIEWPORT] = [];

					$viewportModule.updateWholeViewport(currentClientValue, MODEL_VIEWPORT,
							internalState, wholeViewportUpdate, beanUpdate[CONVERSIONS] && beanUpdate[CONVERSIONS][MODEL_VIEWPORT_KEY] ?
									beanUpdate[CONVERSIONS][MODEL_VIEWPORT_KEY] : undefined, componentScope, function () {
										return currentClientValue[MODEL_KEY]
									});
					done = true;
				} else if (viewportUpdate) {
					$viewportModule.updateViewportGranularly(currentClientValue[MODEL_VIEWPORT], internalState, viewportUpdate,
							beanUpdate[CONVERSIONS] && beanUpdate[CONVERSIONS][MODEL_VIEWPORT_CHANGES_KEY] ?
									beanUpdate[CONVERSIONS][MODEL_VIEWPORT_CHANGES_KEY] : undefined, componentScope, function () {
										return currentClientValue[MODEL_KEY]
									}, false);
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
					childChangedNotifierGenerator = getBeanPropertyChangeNotifierGenerator(newValue);

					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

					if (angular.isDefined(serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
						// if it's linked to a foundset, keep that info in internal state; viewport.js needs it
						var forFoundsetPropertyName = serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
						internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = function() {
							return componentModelGetter()[forFoundsetPropertyName];
						};
						delete serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
					}
					var executeHandler = function(type,args,row) {
						var promiseAndCmsid = $sabloService.createDeferedEvent();
						var newargs = $sabloUtils.getEventArgs(args,type);
						internalState.requests.push({ handlerExec: {
							eventType: type,
							args: newargs,
							rowId: row,
							defid: promiseAndCmsid.defid
						}});
						if (internalState.changeNotifier) internalState.changeNotifier();
						return promiseAndCmsid.promise;
					};

					// implement what $sabloConverters need to make this work
					internalState.setChangeNotifier = function(changeNotifier) {
						internalState.changeNotifier = changeNotifier; 
					}
					internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }

					// private impl
					internalState.requests = [];
					internalState.beanLayout = null; // not really useful right now; just to be able to reuse existing form code 

					internalState.modelUnwatch = null;

					// calling applyBeanData initially to make sure any needed conversions are done on model's properties
					var beanModel = serverJSONValue[MODEL_KEY];

					// just dummy stuff - currently the parent controls layout, but applyBeanData needs such data...
					internalState.beanLayout = {};
					var containerSize = {width: 0, height: 0};

					var currentConversionInfo = beanModel[CONVERSIONS] ?
							$sabloUtils.getOrCreateInDepthProperty(internalState, CONVERSIONS) : 
								$sabloUtils.getInDepthProperty(internalState, CONVERSIONS);

					$servoyInternal.applyBeanData(beanModel, internalState.beanLayout, beanModel, containerSize, childChangedNotifierGenerator,
							currentConversionInfo, beanModel[CONVERSIONS], componentScope);
					delete beanModel.conversions; // delete the conversion info from component accessible model; it will be kept separately only

					// component property is now be able to send itself entirely at runtime; we need to handle viewport conversions here as well
					var wholeViewport = serverJSONValue[MODEL_VIEWPORT_KEY];
					delete serverJSONValue[MODEL_VIEWPORT_KEY];
					serverJSONValue[MODEL_VIEWPORT] = [];

					if (wholeViewport) {
						$viewportModule.updateWholeViewport(serverJSONValue, MODEL_VIEWPORT,
								internalState, wholeViewport, serverJSONValue[CONVERSIONS] ?
										serverJSONValue[CONVERSIONS][MODEL_VIEWPORT_KEY] : undefined, componentScope, function() {
											return serverJSONValue[MODEL_KEY]
										});
					}
					if (angular.isDefined(serverJSONValue[CONVERSIONS])) delete serverJSONValue[CONVERSIONS];

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
								var wrapper = function() {
									return eventHandler(arguments, null);
								}
								wrapper.selectRecordHandler = function(rowId){
									return function () { return eventHandler(arguments,rowId) }
								};
								serverJSONValue.handlers[key] = wrapper;
							})(key);
						}
					}

					// here we don't specify any of the following as all those can be forwarded by the parent component from it's own servoyApi:
					// formWillShow, hideForm, getFormUrl
					serverJSONValue.servoyApi = {
						/** rowId is only needed if the component is linked to a foundset */
						startEdit: function(property, rowId) {
							var req = { svyStartEdit: {} };

							if (rowId) req.svyStartEdit[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
							req.svyStartEdit[PROPERTY_NAME_KEY] = property;

							internalState.requests.push(req);
							if (internalState.changeNotifier) internalState.changeNotifier();
						},

						apply: function(property, modelOfComponent, rowId) {
							/** rowId is only needed if the component is linked to a foundset */
							var conversionInfo = internalState[CONVERSIONS];
							if (!modelOfComponent) modelOfComponent = serverJSONValue[MODEL_KEY]; // if it's not linked to foundset componentModel will be undefined
							var propertyValue = modelOfComponent[property];

							if (conversionInfo && conversionInfo[property]) {
								propertyValue = $sabloConverters.convertFromClientToServer(propertyValue, conversionInfo[property], undefined);
							} else {
								propertyValue = $sabloUtils.convertClientObject(propertyValue);
							}

							var req = { svyApply: {} };

							if (rowId) req.svyApply[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowId;
							req.svyApply[PROPERTY_NAME_KEY] = property;
							req.svyApply[VALUE_KEY] = propertyValue;

							internalState.requests.push(req);
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					}
				}
			}

			// restore/add model watch
			addBackWatches(newValue, componentScope, childChangedNotifierGenerator);

			return newValue;
		},

		updateAngularScope: function(clientValue, componentScope) {
			removeAllWatches(clientValue);
			if (componentScope) addBackWatches(clientValue, componentScope,	getBeanPropertyChangeNotifierGenerator(clientValue));

			if (clientValue) {
				var internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					var conversionInfo = internalState[CONVERSIONS];
					if (conversionInfo) {
						var beanModel = clientValue[MODEL_KEY];
						for (var key in beanModel) {
							if (conversionInfo[key]) $sabloConverters.updateAngularScope(beanModel[key], conversionInfo[key], componentScope);
						}
					}

					$viewportModule.updateAngularScope(clientValue[MODEL_VIEWPORT], internalState, componentScope, false);
				}
			}
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
