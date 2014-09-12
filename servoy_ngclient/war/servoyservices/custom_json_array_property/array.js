angular.module('custom_json_array_property', ['webSocketModule'])
// CustomJSONArray type ------------------------------------------
.run(function ($sabloConverters, $rootScope, $sabloUtils) {
	var UPDATES = "u";
	var INDEX = "i";
	var INITIALIZE = "in";
	var VALUE = "v";
	var CONTENT_VERSION = "ver"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile serverside
	var NO_OP = "n";

	function getChangeNotifier(propertyValue, idx) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedIndexes[idx] = true;
			internalState.notifier();
		}
	}
	
	function watchDumbElementForChanges(propertyValue, idx) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		return $rootScope.$watch(function() {
			return propertyValue[idx];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedIndexes[idx] = { old: oldvalue };
			internalState.notifier();
		}, true);
	}
	
	/** Initializes internal state on a new array value */
	function initializeNewValue(newValue, contentVersion) {
		$sabloConverters.prepareInternalState(newValue);
		var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
		internalState[CONTENT_VERSION] = contentVersion; // being full content updates, we don't care about the version, we just accept it

		// implement what $sabloConverters need to make this work
		internalState.setChangeNotifier = function(changeNotifier) {
			internalState.notifier = changeNotifier; 
		}
		internalState.isChanged = function() {
			var hasChanges = internalState.allChanged;
			if (!hasChanges) for (var x in internalState.changedIndexes) { hasChanges = true; break; }
			return hasChanges;
		}

		// private impl
		internalState.modelUnwatch = [];
		internalState.arrayStructureUnwatch = null;
		internalState.conversionInfo = [];
		internalState.changedIndexes = {};
		internalState.allChanged = false;
	}

	$sabloConverters.registerCustomPropertyHandler('JSON_arr', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;

			// remove old watches and, at the end create new ones to avoid old watches getting triggered by server side change
			if (angular.isDefined(currentClientValue)) {
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				if (angular.isDefined(iS)) {
					if (iS.arrayStructureUnwatch) iS.arrayStructureUnwatch();
					for (var key in iS.elUnwatch) {
						iS.elUnwatch[key]();
					}
					iS.arrayStructureUnwatch = null;
					iS.elUnwatch = null;
				}
			}
			
			try
			{
				if (serverJSONValue && serverJSONValue[VALUE]) {
					// full contents
					newValue = serverJSONValue[VALUE];
					initializeNewValue(newValue, serverJSONValue[CONTENT_VERSION]);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];

					for (var c in newValue) {
						var elem = newValue[c];
						var conversionInfo = null;
						if (serverJSONValue.conversions) {
							conversionInfo = serverJSONValue.conversions[c];
						}

						if (conversionInfo) {
							internalState.conversionInfo[c] = conversionInfo;
							newValue[c] = elem = $sabloConverters.convertFromServerToClient(elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined);
						}

						if (elem && elem[$sabloConverters.INTERNAL_IMPL] && elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
							// child is able to handle it's own change mechanism
							elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(newValue, c));
						}
					}
				} else if (serverJSONValue && serverJSONValue[UPDATES]) {
					// granular updates received;
					
					if (serverJSONValue[INITIALIZE]) initializeNewValue(currentClientValue, serverJSONValue[CONTENT_VERSION] - 1); // this can happen when an array value was set completely in browser and the child elements need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child elements
					
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];

					// if something changed browser-side, increasing the content version thus not matching next expected version,
					// we ignore this update and expect a fresh full copy of the array from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
					if (internalState[CONTENT_VERSION] + 1 == serverJSONValue[CONTENT_VERSION]) {

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
								currentClientValue[idx] = val = $sabloConverters.convertFromServerToClient(val, conversionInfo, currentClientValue[idx]);
							} else currentClientValue[idx] = val;

							if (val && val[$sabloConverters.INTERNAL_IMPL] && val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
								// child is able to handle it's own change mechanism
								val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(currentClientValue, idx));
							}
						}
					}
					//else {
					  // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
					  // and server will detect the problem and send back a full update
					//}
				} else if (serverJSONValue && serverJSONValue[INITIALIZE]) {
					// only content version update - this happens when a full array value is set on this property client side; it goes to server
					// and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
					initializeNewValue(currentClientValue, serverJSONValue[CONTENT_VERSION]); // here we can count on not having any 'smart' values cause if we had
					// updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
				} else newValue = null; // anything else would not be supported...	// TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
			} finally {
				// add back watches if needed
				if (newValue) {
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					internalState.elUnwatch = {};
					for (var c = 0; c < newValue.length; c++) {
						var elem = newValue[c];
						if (!elem || !elem[$sabloConverters.INTERNAL_IMPL] || !elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
							// watch the child's value to see if it changes
							internalState.elUnwatch[c] = watchDumbElementForChanges(newValue, c);
						}
					}

					// watch for add/remove and such operations on array; this is helpful also when 'smart' child values (that have .setChangeNotifier)
					// get changed completely by reference
					internalState.arrayStructureUnwatch = $rootScope.$watchCollection(function() { return newValue; }, function(newWVal, oldWVal) {
						if (newWVal == oldWVal) return;

						if (newWVal === null || oldWVal === null || newWVal.length !== oldWVal.length) {
							internalState.allChanged = true;
							internalState.notifier();
						} else {
							// some elements changed by reference; we only need to handle this for smart element values,
							// as the others will be handled by the separate 'dumb' watches
							var referencesChanged = false;
							for (var j in newWVal) {
								if (newWVal[j] !== oldWVal[j] && oldWVal[j] && oldWVal[j][$sabloConverters.INTERNAL_IMPL] && oldWVal[j][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
									changed = true;
									internalState.changedIndexes[j] = { old: true };
								}
							}
							
							if (referencesChanged) internalState.notifier();
						}
					});
				}
			}

			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			// TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
			
			var internalState;
			if (newClientData && (internalState = newClientData[$sabloConverters.INTERNAL_IMPL])) {
				if (internalState.isChanged()) {
					++internalState[CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
					if (internalState.allChanged) {
						// send all
						var changes = {};
						changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
						var toBeSentArray = changes[VALUE] = [];
						for (var idx = 0; idx < newClientData.length; idx++) {
							var val = newClientData[idx];
							if (internalState.conversionInfo[idx]) toBeSentArray[idx] = $sabloConverters.convertFromClientToServer(val, internalState.conversionInfo[idx], oldClientData ? oldClientData[idx] : undefined);
							else toBeSentArray[idx] = $sabloUtils.convertClientObject(val);
						}
						internalState.allChanged = false;
						internalState.changedIndexes = {};
						return changes;
					} else {
						// send only changed indexes
						var changes = {};
						changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
						var changedElements = changes[UPDATES] = [];
						for (var idx in internalState.changedIndexes) {
							var newVal = newClientData[idx];
							var oldVal = oldClientData ? oldClientData[idx] : undefined;
							
							var changed = (newVal !== oldVal);
							if (!changed) {
								if (internalState.elUnwatch[idx]) {
									var oldDumbVal = internalState.changedIndexes[idx].old;
									// it's a dumb value - watched; see if it really changed acording to sablo rules
									if (oldDumbVal !== newVal) {
										if (typeof newVal == "object") {
											if ($sabloUtils.isChanged(newVal, oldDumbVal, internalState.conversionInfo[idx])) {
												changed = true;
											}
										} else {
											changed = true;
										}
									}
								} else changed = newVal && newVal[$sabloConverters.INTERNAL_IMPL].isChanged(); // must be smart value then; same reference as checked above; so ask it if it changed
							}

							if (changed) {
								var ch = {};
								ch[INDEX] = idx;

								if (internalState.conversionInfo[idx]) ch[VALUE] = $sabloConverters.convertFromClientToServer(newVal, internalState.conversionInfo[idx], oldVal);
								else ch[VALUE] = $sabloUtils.convertClientObject(newVal);

								changedElements.push(ch);
							}
						}
						internalState.allChanged = false;
						internalState.changedIndexes = {};
						return changes;
					}
				} else if (newClientData === oldClientData) {
					var x = {}; // no changes
					x[NO_OP] = true;
					return x;
				}
			}
			return newClientData;
		}
	});
});
