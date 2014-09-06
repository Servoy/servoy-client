angular.module('custom_json_object_property', ['webSocketModule'])
// CustomJSONObject type ------------------------------------------
.run(function ($sabloConverters, $rootScope, $sabloUtils) {
	var UPDATES = "u";
	var KEY = "k";
	var VALUE = "v";
	var CONTENT_VERSION = "ver"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile serverside

	function getChangeNotifier(propertyValue, key) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = true;
			internalState.notifier();
		}
	}
	
	function watchDumbElementForChanges(propertyValue, key) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		return $rootScope.$watch(function() {
			return propertyValue[key];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = { old: oldvalue };
			internalState.notifier();
		}, true);
	}

	$sabloConverters.registerCustomPropertyHandler('JSON_obj', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
			var newValue = currentClientValue;

			// remove old watches and, at the end create new ones to avoid old watches getting triggered by server side change
			if (angular.isDefined(currentClientValue)) {
				var iS = currentClientValue[$sabloConverters.INTERNAL_IMPL];
				if (iS.objStructureUnwatch) iS.objStructureUnwatch();
				for (var key in iS.elUnwatch) {
					iS.elUnwatch[key]();
				}
				iS.objStructureUnwatch = null;
				iS.elUnwatch = null;
			}
			
			try
			{
				if (serverJSONValue && serverJSONValue[VALUE]) {
					// full contents
					newValue = serverJSONValue[VALUE];
					$sabloConverters.prepareInternalState(newValue);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					internalState[CONTENT_VERSION] = serverJSONValue[CONTENT_VERSION]; // being full content updates, we don't care about the version, we just accept it

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
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];

					// if something changed browser-side, increasing the content version thus not matching next expected version,
					// we ignore this update and expect a fresh full copy of the object from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
					if (internalState[CONTENT_VERSION] + 1 == serverJSONValue[CONTENT_VERSION]) {

						internalState[CONTENT_VERSION] = serverJSONValue[CONTENT_VERSION];
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
								currentClientValue[key] = val = $sabloConverters.convertFromServerToClient(val, conversionInfo, currentClientValue[key]);
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
				} else newValue = null; // anything else would not be supported...	// TODO how to handle null values (special watches/complete object set from client)? if null is on server and something is set on client or the other way around?
			} finally {
				// add back watches if needed
				if (newValue) {
					internalState.elUnwatch = {};
					for (var c in newValue) {
						var elem = newValue[c];
						if (!elem || !elem[$sabloConverters.INTERNAL_IMPL] || !elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
							// watch the child's value to see if it changes
							internalState.elUnwatch[c] = watchDumbElementForChanges(newValue, c);
						}
					}

					internalState.objStructureUnwatchIgnoreOnce = true;
					// watch for add/remove and such operations on array
					internalState.objStructureUnwatch = $rootScope.$watchCollection(function() { return newValue; }, function(newVal) {
						if (internalState.objStructureUnwatchIgnoreOnce) internalState.objStructureUnwatchIgnoreOnce = false;
						else {
							internalState.allChanged = true;
							internalState.notifier();
						}
					});
				}
			}

			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			// TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
			
			if (newClientData) {
				var internalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (internalState.isChanged()) {
					++internalState[CONTENT_VERSION]; // we also increase the content version number - server should only be expecting updates for the next version number
					if (internalState.allChanged) {
						// send all
						var changes = {};
						changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
						var toBeSentObj = changes[VALUE] = {};
						for (var key in newClientData) {
							var val = newClientData[key];
							if (internalState.conversionInfo[key]) toBeSentObj[key] = $sabloConverters.convertFromClientToServer(val, internalState.conversionInfo[key], oldClientData ? oldClientData[key] : undefined);
							else toBeSentObj[key] = $sabloUtils.convertClientObject(val);
						}
						return changes;
					} else {
						// send only changed keys
						var changes = {};
						changes[CONTENT_VERSION] = internalState[CONTENT_VERSION];
						var changedElements = changes[UPDATES] = [];
						for (var key in internalState.changedKeys) {
							var newVal = newClientData[key];
							
							var changed = (typeof oldVal == 'undefined');
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
								} else changed = newVal.isChanged(); // must be smart value then
							}

							if (changed) {
								var ch = {};
								ch[KEY] = key;

								if (internalState.conversionInfo[key]) ch[VALUE] = $sabloConverters.convertFromClientToServer(newVal, internalState.conversionInfo[key], oldClientData ? oldClientData[key] : undefined);
								else ch[VALUE] = $sabloUtils.convertClientObject(newVal);

								changedElements.push(ch);
							}
						}
						return changes;
					}
				}
				internalState.allChanged = false;
				internalState.changedKeys = {};
			}
			return {};
		}
	});
});
