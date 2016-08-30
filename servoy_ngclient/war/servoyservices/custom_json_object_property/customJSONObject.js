angular.module('custom_json_object_property', ['webSocketModule'])
// CustomJSONObject type ------------------------------------------
.run(function ($sabloConverters, $sabloUtils) {
	var UPDATES = "u";
	var KEY = "k";
	var INITIALIZE = "in";
	var VALUE = "v";
	var PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
	var CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
	var NO_OP = "n";
	var angularAutoAddedKeys = ["$$hashKey"];

	function getChangeNotifier(propertyValue, key) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = true;
			internalState.notifier();
		}
	}
	
	function watchDumbElementForChanges(propertyValue, key, componentScope, deep) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		return componentScope.$watch(function() {
			return propertyValue[key];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedKeys[key] = { old: oldvalue };
			internalState.notifier();
		}, deep);
	}

	/** Initializes internal state on a new object value */
	function initializeNewValue(newValue, contentVersion) {
		var newInternalState = false; // TODO although unexpected (internal state to already be defined at this stage it can happen until SVY-8612 is implemented and property types change to use that
		if (!newValue.hasOwnProperty($sabloConverters.INTERNAL_IMPL)) {
			newInternalState = true;
			$sabloConverters.prepareInternalState(newValue);
		} // else: we don't try to redefine internal state if it's already defined

		var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
		internalState[CONTENT_VERSION] = contentVersion; // being full content updates, we don't care about the version, we just accept it

		if (newInternalState) {
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
		} // else don't reinitilize it - it's already initialized
	}
	
	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			if (iS != null && angular.isDefined(iS)) {
				if (iS.objStructureUnwatch) iS.objStructureUnwatch();
				for (var key in iS.elUnwatch) {
					iS.elUnwatch[key]();
				}
				iS.objStructureUnwatch = null;
				iS.elUnwatch = null;
			}
		}
	}

	function addBackWatches(value, componentScope) {
		if (value) {
			var internalState = value[$sabloConverters.INTERNAL_IMPL];
			internalState.elUnwatch = {};

			// add shallow/deep watches as needed
			if (componentScope && typeof internalState[PUSH_TO_SERVER] != 'undefined') {
				
				for (var c in value) {
					var elem = value[c];
					if (!elem || !elem[$sabloConverters.INTERNAL_IMPL] || !elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
						// watch the child's value to see if it changes
						if (angularAutoAddedKeys.indexOf(c) === -1) internalState.elUnwatch[c] = watchDumbElementForChanges(value, c, componentScope, internalState[PUSH_TO_SERVER]);
					}
				}

				// watch for add/remove and such operations on object; this is helpful also when 'smart' child values (that have .setChangeNotifier)
				// get changed completely by reference
				internalState.objStructureUnwatch = componentScope.$watchCollection(function() { return value; }, function(newWVal, oldWVal) {
					if (newWVal === oldWVal) return;
					
					if (newWVal === null || oldWVal === null) {
						// send new value entirely
						internalState.allChanged = true;
						internalState.notifier();
					} else {
						// search for differences between properties of the old and new objects
						var changed = false;

						var tmp = [];
						var idx;
						for (var key in newWVal) if (angularAutoAddedKeys.indexOf(key) === -1) tmp.push(key);
						for (var key in oldWVal) {
							if (angularAutoAddedKeys.indexOf(key) !== -1) continue;

							if ((idx = tmp.indexOf(key)) != -1) {
								tmp.splice(idx, 1); // this will be dealt with here; remove it from tmp
								
								// key in both old and new; check for difference in value
								if (newWVal[key] !== oldWVal[key] && oldWVal[key] && oldWVal[key][$sabloConverters.INTERNAL_IMPL] && oldWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
									// some elements changed by reference; we only need to handle this for old smart element values,
									// as the others will be handled by the separate 'dumb' watches
									changed = true;
									internalState.changedKeys[key] = { old: oldWVal[key] }; // just in case new value is not smart; otherwise we could just put true there for example - that is enough for smart values

									// if new value is smart as well we have to give it the according change notifier
									if (newWVal[key] && newWVal[key][$sabloConverters.INTERNAL_IMPL] && newWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier)
										newWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(newWVal, key));
								}
							} else {
								// old has a key that is no longer present in new one; for 'dumb' properties this will be handled by already added dumb watches;
								// so here we need to see if old value was smart, then we need to send updates to the server
								if (oldWVal[key] && oldWVal[key][$sabloConverters.INTERNAL_IMPL] && oldWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
									changed = true;
									internalState.changedKeys[key] = { old: oldWVal[key] };
								}
							}
						}
						// any keys left in tmp are keys that are in new value but are not in old value; handle those
						for (var idx in tmp) {
							var key = tmp[idx];
							// if a dumb watch is already present for this key let that watch handle the change (could happen for example if a property is initially set with a 'dumb' value then cleared then set again)
							if (!internalState.elUnwatch[key]) {
								// so we were not previously aware of this new key; send it to server
								changed = true;
								internalState.changedKeys[key] = { old: undefined };
								
								// if new value is smart we have to give it the according change notifier; if it's 'dumb' and it was not watched before add a 'dumb' watch on it
								if (newWVal[key] && newWVal[key][$sabloConverters.INTERNAL_IMPL] && newWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier)
									newWVal[key][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(newWVal, key));
								else internalState.elUnwatch[key] = watchDumbElementForChanges(newWVal, key, componentScope, internalState[PUSH_TO_SERVER]);
							} // TODO do we need to handle unlikely situation where the value for a key would switch from dumb in the past to smart?
						}
						if (changed) internalState.notifier();
					}
				});
			}
		}
	}

	$sabloConverters.registerCustomPropertyHandler('JSON_obj', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = currentClientValue;

			// remove old watches and, at the end create new ones to avoid old watches getting triggered by server side change
			removeAllWatches(currentClientValue);
			
			try
			{
				if (serverJSONValue && serverJSONValue[VALUE]) {
					// full contents
					newValue = serverJSONValue[VALUE];
					initializeNewValue(newValue, serverJSONValue[CONTENT_VERSION]);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					if (typeof serverJSONValue[PUSH_TO_SERVER] !== 'undefined') internalState[PUSH_TO_SERVER] = serverJSONValue[PUSH_TO_SERVER];
						
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
				addBackWatches(newValue, componentScope);
			}

			return newValue;
		},

		updateAngularScope: function(clientValue, componentScope) {
			removeAllWatches(clientValue);
			if (componentScope) addBackWatches(clientValue, componentScope);

			if (clientValue) {
				var internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					for (var key in clientValue) {
						if (angularAutoAddedKeys.indexOf(key) !== -1) continue;
						
						var elem = clientValue[key];
						if (internalState.conversionInfo[key]) $sabloConverters.updateAngularScope(elem, internalState.conversionInfo[key], componentScope);
					}
				}
			}
		},

		fromClientToServer: function(newClientData, oldClientData) {
			// TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?

			var internalState;
			if (newClientData && (internalState = newClientData[$sabloConverters.INTERNAL_IMPL])) {
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
						internalState.changedKeys = {};
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
				} else if (angular.equals(newClientData, oldClientData)) { // can't use === because oldClientData is an angular clone not the same ref.
					var x = {}; // no changes
					x[NO_OP] = true;
					return x;
				}
			}
			
			if (internalState) delete newClientData[$sabloConverters.INTERNAL_IMPL]; // some other new value was set; it's internal state is useless and will be re-initialized from server
				
			return newClientData;
		}
	});
});
