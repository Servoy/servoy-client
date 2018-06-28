angular.module('foundset_linked_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module'])
// Foundset linked type ------------------------------------------
.value("$foundsetLinkedTypeConstants", {
	ID_FOR_FOUNDSET: "idForFoundset"
})
.run(function ($sabloConverters, $sabloUtils, $viewportModule, $servoyInternal, $log, $foundsetTypeConstants, $sabloUtils, $foundsetLinkedTypeConstants) {

	var SINGLE_VALUE = "sv";
	var SINGLE_VALUE_UPDATE = "svu";
	var VIEWPORT_VALUE = "vp";
	var VIEWPORT_VALUE_UPDATE = "vpu";
	var CONVERSION_NAME = "fsLinked";
	var PROPERTY_CHANGE = "propertyChange";

	var PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched

	/** Initializes internal state of a new value */
	function initializeNewValue(newValue) {
		$sabloConverters.prepareInternalState(newValue);
		var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

		// implement what $sabloConverters need to make this work
		internalState.setChangeNotifier = function(changeNotifier) {
			internalState.changeNotifier = changeNotifier; 
		}
		internalState.isChanged = function() {
			return internalState.requests && (internalState.requests.length > 0);;
		}

		// private impl
		internalState.recordLinked = false;
		internalState.viewportSizeUnwatch = null;
		internalState.conversionInfo = [];
		internalState.requests = [];
	}
	
	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			$viewportModule.removeDataWatchesFromRows(value.length, iS);
			if (iS.viewportSizeUnwatch) iS.viewportSizeUnwatch();
		}
	};

	function addBackWatches(value, componentScope) {
		if (angular.isDefined(value) && value !== null) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			$viewportModule.addDataWatchesToRows(value, iS, componentScope, true, iS[PUSH_TO_SERVER]);
		}
	};

	$sabloConverters.registerCustomPropertyHandler(CONVERSION_NAME, {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = (currentClientValue ? currentClientValue : []);

			// remove watches to avoid an unwanted detection of received changes
			removeAllWatches(currentClientValue);

			if (serverJSONValue != null && angular.isDefined(serverJSONValue)) {
				var didSomething = false;
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				if (!angular.isDefined(internalState)) {
					initializeNewValue(newValue);
					internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					var changeListeners = [];
					newValue.addChangeListener = function(listener) {
						changeListeners.push(listener);
					}
					newValue.removeChangeListener = function(listener) {
						var index = changeListeners.indexOf(listener);
						if (index > -1) {
							changeListeners.splice(index, 1);
						}
					}
					internalState.fireChanges = function(values) {
						for(var i=0;i<changeListeners.length;i++) {
							changeListeners[i](values);
						}
					}
				}

				if (angular.isDefined(serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
					// the foundset that this property is linked to; keep that info in internal state; viewport.js needs it
					var forFoundsetPropertyName = serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
					internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = function() {
						return componentModelGetter()[forFoundsetPropertyName];
					};
					didSomething = true;
				}

				if (typeof serverJSONValue[PUSH_TO_SERVER] !== 'undefined') {
					internalState[PUSH_TO_SERVER] = serverJSONValue[PUSH_TO_SERVER];
				}

				var childChangedNotifier;
				
				if (angular.isDefined(serverJSONValue[VIEWPORT_VALUE_UPDATE])) {
					internalState.recordLinked = true;
					$viewportModule.updateViewportGranularly(newValue, internalState, serverJSONValue[VIEWPORT_VALUE_UPDATE],
							$sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, VIEWPORT_VALUE_UPDATE),
							componentScope, componentModelGetter, true);
					internalState.fireChanges(serverJSONValue[VIEWPORT_VALUE_UPDATE]);
				} else {
					// the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
					var wholeViewport;
					var conversionInfos;
					
					function updateWholeViewport() {
						var viewPortHolder = { "tmp" : newValue };
						$viewportModule.updateWholeViewport(viewPortHolder, "tmp", internalState, wholeViewport, conversionInfos, componentScope, componentModelGetter);
						
						// updateWholeViewport probably changed "tmp" reference to value of "wholeViewport"...
						// update current value reference because that is what is present in the model
						newValue.splice(0, newValue.length);
						var tmp = viewPortHolder["tmp"];
						for (var tz = 0; tz < tmp.length; tz++) newValue.push(tmp[tz]);
					}
					
					if (angular.isDefined(serverJSONValue[SINGLE_VALUE]) || angular.isDefined(serverJSONValue[SINGLE_VALUE_UPDATE])) {
						// just update single value from server and make copies of it to duplicate
						internalState.recordLinked = false;
						var conversionInfo = $sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, SINGLE_VALUE);
						var singleValue = angular.isDefined(serverJSONValue[SINGLE_VALUE]) ? serverJSONValue[SINGLE_VALUE] : serverJSONValue[SINGLE_VALUE_UPDATE];
						
						function generateWholeViewportFromOneValue(vpSize) {
							if (angular.isUndefined(vpSize)) vpSize = 0;
							wholeViewport = [];
							conversionInfos = conversionInfo ? [] : undefined;
							
							for (var index = vpSize - 1; index >= 0; index--) {
								wholeViewport.push(singleValue);
								if (conversionInfo) conversionInfos.push(conversionInfo);
							}
						}
						function vpSizeGetter() { return $sabloUtils.getInDepthProperty(internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY](), "viewPort", "size") };
						var initialVPSize = vpSizeGetter();
						generateWholeViewportFromOneValue(initialVPSize);
						
						// watch foundSet viewport size; when it changes generate a new viewport client side
						internalState.viewportSizeUnwatch = componentScope.$watch(vpSizeGetter,
								function (newV) {
									if (newV === initialVPSize) return;
									initialVPSize = -1;
									if (!angular.isDefined(newV)) newV = 0;
									
									if (componentScope) {
										componentScope.$evalAsync(function(){
											$viewportModule.removeDataWatchesFromRows(newValue.length, internalState);
											generateWholeViewportFromOneValue(newV);
											updateWholeViewport();
											$viewportModule.addDataWatchesToRows(newValue, internalState, componentScope, true, internalState[PUSH_TO_SERVER]);
										})
									}
								});
					} else if (angular.isDefined(serverJSONValue[VIEWPORT_VALUE])) {
						internalState.recordLinked = true;
						wholeViewport = serverJSONValue[VIEWPORT_VALUE];
						conversionInfos = $sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, VIEWPORT_VALUE);
					}
					
					if (angular.isDefined(wholeViewport)) updateWholeViewport();
					else if (!didSomething) $log.error("Can't interpret foundset linked prop. server update correctly: " + JSON.stringify(serverJSONValue, undefined, 2));
				}
			}
			
			if (serverJSONValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET] === null) {
				if (angular.isDefined(newValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET])) delete newValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET];
			} else if (angular.isDefined(serverJSONValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET])) {
				// make it non-iterable as the newValue is an array an ppl. might iterate over it - they wont expect this in the iterations
				if (Object.defineProperty) {
					Object.defineProperty(newValue, $foundsetLinkedTypeConstants.ID_FOR_FOUNDSET, {
						configurable: true,
						enumerable: false,
						writable: true,
						value: serverJSONValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET]
					});
				} else newValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET] = serverJSONValue[$foundsetLinkedTypeConstants.ID_FOR_FOUNDSET];
			}
			
			// restore/add model watch
			addBackWatches(newValue, componentScope);
			return newValue;
		},

		updateAngularScope: function(clientValue, componentScope) {
			removeAllWatches(clientValue);
			if (componentScope) addBackWatches(clientValue, componentScope);

			if (clientValue) {
				var internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					$viewportModule.updateAngularScope(clientValue, internalState, componentScope, true);
				}
			}
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var internalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					if (!internalState.recordLinked) {
						// we don't need to send rowId to server in this case; we just need value
						for (var index in internalState.requests) {
							internalState.requests[index][PROPERTY_CHANGE] = internalState.requests[index].viewportDataChanged.value;
							delete internalState.requests[index].viewportDataChanged;
						}
					}
					var tmp = internalState.requests;
					internalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
	});
	
});
