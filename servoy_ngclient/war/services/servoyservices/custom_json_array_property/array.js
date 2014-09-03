angular.module('custom_json_array_property', ['webSocketModule'])
// CustomJSONArray type ------------------------------------------
.run(function ($sabloConverters, $utils, $rootScope, $sabloConverters) {
	var UPDATES = "updates";
	var INDEX = "i";
	var VALUE = "v";
	var CONTENT_VERSION = "version"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile serverside

	function getChangeNotifier(propertyValue, idx) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedIndexes.push(idx);
			internalState.notifier();
		}
	}
	
	function watchDumbElementForChanges(propertyValue, idx) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		var notifier = getChangeNotifier(propertyValue, idx);
		return $rootScope.$watch(function() {
			return propertyValue[idx];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			notifier();
		}, true);
	}

	$sabloConverters.registerCustomPropertyHandler('JSON_arr', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;

			if (serverJSONValue && serverJSONValue[VALUE]) {
				newValue = serverJSONValue[VALUE];
				$sabloConverters.prepareInternalState(newValue);
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				internalState[CONTENT_VERSION] = serverJSONValue[CONTENT_VERSION];
				
				// implement what $sabloConverters need to make this work
				internalState.setChangeNotifier = function(changeNotifier) {
					internalState.notifier = changeNotifier; 
				}
				internalState.isChanged = function() { return internalState.allChanged || (internalState.changedIndexes.length > 0); }

				// private impl
				internalState.modelUnwatch = [];
				internalState.arrayStructureUnwatch = null;
				internalState.conversionInfo = [];
				internalState.changedIndexes = [];
				internalState.allChanged = false;
				for (var c in newValue) {
					var elem = newValue[c];
					var conversionInfo = null;
					if (serverJSONValue.conversions) {
						conversionInfo = serverJSONValue.conversions[c];
					}

					if (conversionInfo) {
						internalState.conversionInfo[c] = conversionInfo;
						newValue[c] = elem = $sabloConverters.convertFromServerToClient(elem, conversionInfo);
					}

					if (elem && elem[$sabloConverters.INTERNAL_IMPL] && elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
						// child is able to handle it's own change mechanism
						elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(newValue, c));
					} else {
						// watch the child's value to see if it changes
						internalState.elUnwatch.push(watchDumbElementForChanges(newValue, c));
					}
				}
				
				// watch for add/remove and such operations on array
				internalState.arrayStructureUnwatch = $rootScope.$watchCollection(function() { return newValue; }, function(newVal) {
					internalState.allChanged = true;
					internalState.notifier();
		        });
			} else if (serverJSONValue && serverJSONValue[UPDATES]) {
				// granular updates received
				var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
				internalState[CONTENT_VERSION] = serverJSONValue[CONTENT_VERSION];
				var updates = serverJSONValue[UPDATES];
				var conversionInfos = serverJSONValue.conversions;
				var i;
				for (i in updates) {
					var update = updates[i];
					var idx = update[INDEX];
					var val = update[VALUE];

					var conversionInfo = null;
					if (conversionInfos && conversionInfos[i] && conversionInfos[i][VALUE]) {
						conversionInfo = conversionInfos[i][VALUE];
					}

					if (conversionInfo) {
						internalState.conversionInfo[idx] = conversionInfo;
						currentClientValue[idx] = val = $sabloConverters.convertFromServerToClient(val, conversionInfo);
					}

					if (val && val[$sabloConverters.INTERNAL_IMPL] && val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
						// child is able to handle it's own change mechanism
						val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(currentClientValue, idx));
					} // else a watch for this dumb value at this index is already in place as this does not modify the array size nor the element types
				}
			} else newValue = null; // anything else would not be supported...
			
			if (angular.isDefined(currentClientValue) && newValue !== currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				if (iS.arrayStructureUnwatch) iS.arrayStructureUnwatch();
				for (var key in iS.elUnwatch) {
					iS.elUnwatch[key]();
				}
				iS.arrayStructureUnwatch = null;
				iS.elUnwatch = null;
			}
			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var internalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					if (internalState.allChanged) {
						// send all
						var toBeSentArray = [];
						for (var idx in newClientData) {
							var val = newClientData[idx];
							if (internalState.conversionInfo[idx]) toBeSentArray[idx] = $sabloConverters.convertFromClientToServer(val, internalState.conversionInfo[idx], oldClientData ? oldClientData[idx] : undefined);
							else toBeSentArray[prop] = $sabloUtils.convertClientObject(val);
						}
						return toBeSentArray;
					} else {
						// send only changed indexes
						var changes = {};
						changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
						changedElements = changes[UPDATES] = [];
						for (var idxOfIdx in internalState.changedIndexes) {
							var idx = internalState.changedIndexes[idxOfIdx];
							var ch = {};
							ch[IDX] = idx;
							
							if (internalState.conversionInfo[idx]) ch[VALUE] = $sabloConverters.convertFromClientToServer(newClientData[idx], internalState.conversionInfo[idx], oldClientData ? oldClientData[idx] : undefined);
							else ch[VALUE] = $sabloUtils.convertClientObject(newClientData[idx]);
							
							changedElements.push(ch);
						}
						return changes;
					}
				}
			}
			return {};
		}
	});
});
