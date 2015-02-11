angular.module('custom_json_object_property', ['webSocketModule'])
// CustomJSONObject type ------------------------------------------
.run(function ($sabloConverters, $sabloUtils) {
	var UPDATES = "u";
	var KEY = "k";
	var INITIALIZE = "in";
	var VALUE = "v";
	var CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile serverside
	var NO_OP = "n";
	var angularAutoAddedKeys = ["$$hashKey"];

	function getChangeNotifier(propertyValue, key) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = true;
			internalState.notifier();
		}
	}
	
	function watchDumbElementForChanges(propertyValue, key, componentScope) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		return componentScope.$watch(function() {
			return propertyValue[key];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = { old: oldvalue };
			internalState.notifier();
		}, true);
	}

	/** Initializes internal state on a new object value */
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
			if (!hasChanges) for (var x in internalState.changedKeys) { hasChanges = true; break; }
			return hasChanges;
		}

		// private impl
		internalState.modelUnwatch = [];
		internalState.objStructureUnwatch = null;
		internalState.conversionInfo = {};
		internalState.changedKeys = {};
		internalState.allChanged = false;
	}
	
	$sabloConverters.registerCustomPropertyHandler('JSON_obj', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = currentClientValue;

			// remove old watches and, at the end create new ones to avoid old watches getting triggered by server side change
			if (currentClientValue != null && angular.isDefined(currentClientValue)) {
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				if (iS != null && angular.isDefined(iS)) {
					if (iS.objStructureUnwatch) iS.objStructureUnwatch();
					for (var key in iS.elUnwatch) {
						iS.elUnwatch[key]();
					}
					iS.objStructureUnwatch = null;
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
							newValue[c] = elem = $sabloConverters.convertFromServerToClient(elem, conversionInfo, currentClientValue ? currentClientValue[c] : undefined, componentScope, componentModelGetter);
						}

						if (elem && elem[$sabloConverters.INTERNAL_IMPL] && elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
							// child is able to handle it's own change mechanism
							elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(newValue, c));
						}
					}
				} else if (serverJSONValue && serverJSONValue[UPDATES]) {
					// granular updates received;
					
					if (serverJSONValue[INITIALIZE]) initializeNewValue(currentClientValue, serverJSONValue[CONTENT_VERSION]); // this can happen when an object value was set completely in browser and the child values need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child values
					
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];

					// if something changed browser-side, increasing the content version thus not matching next expected version,
					// we ignore this update and expect a fresh full copy of the object from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
					if (internalState[CONTENT_VERSION] == serverJSONValue[CONTENT_VERSION]) {
						var updates = serverJSONValue[UPDATES];
						var conversionInfos = serverJSONValue.conversions;
						var i;
						for (i in updates) {
							var update = updates[i];
							var key = update[KEY];
							var val = update[VALUE];

							var conversionInfo = null;
							if (conversionInfos && conversionInfos[i] && conversionInfos[i][VALUE]) {
								conversionInfo = conversionInfos[i][VALUE];
							}

							if (conversionInfo) {
								internalState.conversionInfo[key] = conversionInfo;
								currentClientValue[key] = val = $sabloConverters.convertFromServerToClient(val, conversionInfo, currentClientValue[key], componentScope, componentModelGetter);
							} else currentClientValue[key] = val;

							if (val && val[$sabloConverters.INTERNAL_IMPL] && val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
								// child is able to handle it's own change mechanism
								val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(currentClientValue, key));
							}
						}
					}
					//else {
					  // else we got an update from server for a version that was already bumped by changes in browser; ignore that, as browser changes were sent to server
					  // and server will detect the problem and send back a full update
					//}
				} else if (serverJSONValue && serverJSONValue[INITIALIZE]) {
					// only content version update - this happens when a full object value is set on this property client side; it goes to server
					// and then server sends back the version and we initialize / prepare the existing newValue for being watched/handle child conversions
					initializeNewValue(currentClientValue, serverJSONValue[CONTENT_VERSION]); // here we can count on not having any 'smart' values cause if we had
					// updates would have been received with this initialize as well (to initialize child elements as well to have the setChangeNotifier and internal things)
				} else if (!serverJSONValue || !serverJSONValue[NO_OP]) newValue = null; // anything else would not be supported...	// TODO how to handle null values (special watches/complete object set from client)? if null is on server and something is set on client or the other way around?
			} finally {
				// add back watches if needed
				if (newValue) {
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					internalState.elUnwatch = {};
					for (var c in newValue) {
						var elem = newValue[c];
						if (!elem || !elem[$sabloConverters.INTERNAL_IMPL] || !elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
							// watch the child's value to see if it changes
							if (angularAutoAddedKeys.indexOf(key) === -1 && componentScope) internalState.elUnwatch[c] = watchDumbElementForChanges(newValue, c, componentScope);
						}
					}

					// watch for add/remove and such operations on object; this is helpful also when 'smart' child values (that have .setChangeNotifier)
					// get changed completely by reference
					if (componentScope) internalState.objStructureUnwatch = componentScope.$watchCollection(function() { return newValue; }, function(newWVal, oldWVal) {
						if (newWVal === oldWVal) return;
						var sendAllNeeded = (newWVal === null || oldWVal === null);
						if (!sendAllNeeded) {
							// see if the two objects have the same keys
							var tmp = [];
							var tzi;
							for (var tz in newWVal) if (angularAutoAddedKeys.indexOf(tz) === -1) tmp.push(tz);
							for (var tz in oldWVal) {
								if (angularAutoAddedKeys.indexOf(key) !== -1) continue;
								
								if ((tzi = tmp.indexOf(tz)) != -1) tmp.splice(tzi, 1);
								else {
									sendAllNeeded = true;
									break;
								}
							}
							sendAllNeeded = (tmp.length != 0);
						}
						
						if (sendAllNeeded) {
							internalState.allChanged = true;
							internalState.notifier();
						} else {
							// some elements changed by reference; we only need to handle this for smart element values,
							// as the others will be handled by the separate 'dumb' watches
							
							// we already checked that length and keys are the same above
							var referencesChanged = false;
							for (var j in newWVal) {
								if (angularAutoAddedKeys.indexOf(j) !== -1) continue;
								
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
				var internalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					var changes = {};
					changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
					if (internalState.allChanged) {
						// structure might have changed; increase version number
						++internalState[CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
						// send all
						var toBeSentObj = changes[VALUE] = {};
						for (var key in newClientData) {
							if (angularAutoAddedKeys.indexOf(key) !== -1) continue;

							var val = newClientData[key];
							if (internalState.conversionInfo[key]) toBeSentObj[key] = $sabloConverters.convertFromClientToServer(val, internalState.conversionInfo[key], oldClientData ? oldClientData[key] : undefined);
							else toBeSentObj[key] = $sabloUtils.convertClientObject(val);
						}
						internalState.allChanged = false;
						internalState.changedIndexes = {};
						return changes;
					} else {
						// send only changed keys
						var changedElements = changes[UPDATES] = [];
						for (var key in internalState.changedKeys) {
							var newVal = newClientData[key];
							var oldVal = oldClientData ? oldClientData[key] : undefined;

							var changed = (newVal !== oldVal);
							if (!changed) {
								if (internalState.elUnwatch[key]) {
									var oldDumbVal = internalState.changedKeys[key].old;
									// it's a dumb value - watched; see if it really changed acording to sablo rules
									if (oldDumbVal !== newVal) {
										if (typeof newVal == "object") {
											if ($sabloUtils.isChanged(newVal, oldDumbVal, internalState.conversionInfo[key])) {
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
								ch[KEY] = key;

								if (internalState.conversionInfo[key]) ch[VALUE] = $sabloConverters.convertFromClientToServer(newVal, internalState.conversionInfo[key], oldVal);
								else ch[VALUE] = $sabloUtils.convertClientObject(newVal);

								changedElements.push(ch);
							}
						}
						internalState.allChanged = false;
						internalState.changedKeys = {};
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
