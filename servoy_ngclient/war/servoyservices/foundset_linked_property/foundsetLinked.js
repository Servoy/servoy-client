angular.module('foundset_linked_property', ['webSocketModule', 'servoyApp', 'foundset_custom_property', 'foundset_viewport_module'])
// Foundset linked type ------------------------------------------
.value("$foundsetLinkedTypeConstants", {
	ID_FOR_FOUNDSET: "idForFoundset",
	RECORD_LINKED: "recordLinked"
})
.run(function ($sabloConverters, $sabloUtils, $viewportModule, $servoyInternal, $log, $foundsetTypeConstants, $sabloUtils, $foundsetLinkedTypeConstants, $webSocket) {

	var SINGLE_VALUE = "sv";
	var SINGLE_VALUE_UPDATE = "svu";
	var VIEWPORT_VALUE = "vp";
	var VIEWPORT_VALUE_UPDATE = "vpu";
	var CONVERSION_NAME = "fsLinked";
	var PROPERTY_CHANGE = "propertyChange";

	var PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched

	/** Initializes internal state of a new value */
	function initializeNewValue(newValue, oldValue) {
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
		internalState[$foundsetLinkedTypeConstants.RECORD_LINKED] = false;
		internalState.singleValueState = undefined;
		internalState.conversionInfo = [];
		internalState.requests = []; // see viewport.js for how this will get populated
		
		// keep listeners if needed
		internalState.changeListeners = (oldValue && oldValue[$sabloConverters.INTERNAL_IMPL] ? oldValue[$sabloConverters.INTERNAL_IMPL].changeListeners : []);
		
		/**
		 * Adds a change listener that will get triggered when server sends changes for this foundset linked property.
		 * 
		 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
		 * @param listener the listener to register.
		 */
		newValue.addChangeListener = function(listener) {
			internalState.changeListeners.push(listener);
		}
		newValue.removeChangeListener = function(listener) {
			var index = internalState.changeListeners.indexOf(listener);
			if (index > -1) {
				internalState.changeListeners.splice(index, 1);
			}
		}
		internalState.fireChanges = function(values) {
			for (var i = 0; i < internalState.changeListeners.length; i++) {
				$webSocket.setIMHDTScopeHintInternal(componentScope);
				internalState.changeListeners[i](values);
				$webSocket.setIMHDTScopeHintInternal(undefined);
			}
		}
	}
	
	function getUpdateWholeViewportFunc(propertyContext) {
		return function updateWholeViewport(propValue, internalState, wholeViewport, conversionInfos, componentScope) {
			var viewPortHolder = { "tmp" : propValue };
			$viewportModule.updateWholeViewport(viewPortHolder, "tmp", internalState, wholeViewport, conversionInfos, componentScope, propertyContext);
			
			// updateWholeViewport probably changed "tmp" reference to value of "wholeViewport"...
			// update current value reference because that is what is present in the model
			propValue.splice(0, propValue.length);
			var tmp = viewPortHolder["tmp"];
			for (var tz = 0; tz < tmp.length; tz++) propValue.push(tmp[tz]);
			
			if (propValue && internalState && internalState.changeListeners.length > 0) {
				notificationParamForListeners = {};
				notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue: propValue, newValue: propValue }; // should we not set oldValue here? old one has changed into new one so basically we do not have old content anymore...
				
				if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset linked * firing change listener: full viewport changed...");
				// use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
				internalState.fireChanges(notificationParamForListeners);
			}
		}
	}

	function addBackWatches(value, componentScope) {
		if (angular.isDefined(value) && value !== null) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			
			$viewportModule.addDataWatchesToRows(value, iS, componentScope, true, iS[PUSH_TO_SERVER]);
			
			if (componentScope && iS.singleValueState) {
				// watch foundSet viewport size; when it changes generate a new viewport client side as this is a repeated single value; it is not record linked
				function vpSizeGetter() { return $sabloUtils.getInDepthProperty(iS[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY](), "viewPort", "size") };

				iS.singleValueState.viewportSizeUnwatch = componentScope.$watch(vpSizeGetter,
						function (newViewportSize) {
							if (newViewportSize === iS.singleValueState.initialVPSize) return;
							iS.singleValueState.initialVPSize = -1;
							if (!angular.isDefined(newViewportSize)) newViewportSize = 0;
							
							componentScope.$evalAsync(function() {
								$viewportModule.removeDataWatchesFromRows(value.length, iS);
								var wholeViewport = iS.singleValueState.generateWholeViewportFromOneValue(iS, newViewportSize);
								iS.singleValueState.updateWholeViewport(value, iS, wholeViewport, iS.singleValueState.conversionInfos, componentScope);
								$viewportModule.addDataWatchesToRows(value, iS, componentScope, true, iS[PUSH_TO_SERVER]);
							})
						});
			}
		}
	};
	
	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			$viewportModule.removeDataWatchesFromRows(value.length, iS);
			if (iS.singleValueState && iS.singleValueState.viewportSizeUnwatch) {
				iS.singleValueState.viewportSizeUnwatch();
				iS.singleValueState.viewportSizeUnwatch = undefined;
			}
		}
	};

	function handleSingleValue(singleValue, iS, conversionInfo) {
		// this gets called for values that are not actually record linked, and we 'fake' a viewport containing the same value on each row in the array
		iS[$foundsetLinkedTypeConstants.RECORD_LINKED] = false;
		
		// *** BEGIN we need the following in addBackWatches that is also called by updateAngularScope, that is why they are stored in internalState (iS)
		iS.singleValueState.generateWholeViewportFromOneValue = function(internalState, vpSize) {
			if (angular.isUndefined(vpSize)) vpSize = 0;
			var wholeViewport = [];
			internalState.singleValueState.conversionInfos = conversionInfo ? [] : undefined; 
			
			for (var index = vpSize - 1; index >= 0; index--) {
				wholeViewport.push(singleValue);
				if (conversionInfo) internalState.singleValueState.conversionInfos.push(conversionInfo);
			}
			return wholeViewport;
		}
		iS.singleValueState.initialVPSize = $sabloUtils.getInDepthProperty(iS[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY](), "viewPort", "size");
		// *** END
		
		return iS.singleValueState.generateWholeViewportFromOneValue(iS, iS.singleValueState.initialVPSize);
	}

	$sabloConverters.registerCustomPropertyHandler(CONVERSION_NAME, {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, propertyContext) {
			var newValue = (currentClientValue ? currentClientValue : []);

			// remove watches to avoid an unwanted detection of received changes
			removeAllWatches(currentClientValue);

			if (serverJSONValue != null && angular.isDefined(serverJSONValue)) {
				var didSomething = false;
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				if (!angular.isDefined(internalState)) {
					initializeNewValue(newValue, currentClientValue);
					internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				}

				if (angular.isDefined(serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
					// the foundset that this property is linked to; keep that info in internal state; viewport.js needs it
					var forFoundsetPropertyName = serverJSONValue[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY];
					internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] = function() {
						return propertyContext(forFoundsetPropertyName);
					};
					didSomething = true;
				}

				if (typeof serverJSONValue[PUSH_TO_SERVER] !== 'undefined') {
					internalState[PUSH_TO_SERVER] = serverJSONValue[PUSH_TO_SERVER];
				}

				var childChangedNotifier;
				
				if (angular.isDefined(serverJSONValue[VIEWPORT_VALUE_UPDATE])) {
					internalState.singleValueState = undefined;
					internalState[$foundsetLinkedTypeConstants.RECORD_LINKED] = true;
					
					$viewportModule.updateViewportGranularly(newValue, internalState, serverJSONValue[VIEWPORT_VALUE_UPDATE],
							$sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, VIEWPORT_VALUE_UPDATE),
							componentScope, propertyContext, true);
					if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset linked * firing change listener: granular updates...");
					internalState.fireChanges(serverJSONValue[VIEWPORT_VALUE_UPDATE]);
				} else {
					// the rest will always be treated as a full viewport update (single values are actually going to generate a full viewport of 'the one' new value)
					var conversionInfos;
					var updateWholeViewportFunc = getUpdateWholeViewportFunc(propertyContext);
					
					var wholeViewport;
					if (angular.isDefined(serverJSONValue[SINGLE_VALUE]) || angular.isDefined(serverJSONValue[SINGLE_VALUE_UPDATE])) {
						// just update single value from server and make copies of it to duplicate
						var conversionInfo = $sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, angular.isDefined(serverJSONValue[SINGLE_VALUE]) ? SINGLE_VALUE : SINGLE_VALUE_UPDATE);
						var singleValue = angular.isDefined(serverJSONValue[SINGLE_VALUE]) ? serverJSONValue[SINGLE_VALUE] : serverJSONValue[SINGLE_VALUE_UPDATE];
						internalState.singleValueState = {};
						internalState.singleValueState.updateWholeViewport = updateWholeViewportFunc;
						wholeViewport = handleSingleValue(singleValue, internalState, conversionInfo);
						conversionInfos = internalState.singleValueState.conversionInfos;
						// addBackWatches below (end of function) will add a watch for foundset prop. size to regenerate the viewport when that changes - fill it up with single values
					} else if (angular.isDefined(serverJSONValue[VIEWPORT_VALUE])) {
						internalState.singleValueState = undefined;
						internalState[$foundsetLinkedTypeConstants.RECORD_LINKED] = true;
						
						wholeViewport = serverJSONValue[VIEWPORT_VALUE];
						conversionInfos = $sabloUtils.getInDepthProperty(serverJSONValue, $sabloConverters.TYPES_KEY, VIEWPORT_VALUE);
					}
					
					if (angular.isDefined(wholeViewport)) updateWholeViewportFunc(newValue, internalState, wholeViewport, conversionInfos, componentScope);
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
					if (!internalState[$foundsetLinkedTypeConstants.RECORD_LINKED]) {
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
