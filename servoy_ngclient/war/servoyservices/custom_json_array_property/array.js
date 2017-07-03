angular.module('custom_json_array_property', ['webSocketModule'])
//CustomJSONArray type ------------------------------------------
.run(function ($sabloConverters, $sabloUtils) {
	var UPDATES = "u";
	var REMOVES = "r";
	var ADDITIONS = "a";
	var INDEX = "i";
	var INITIALIZE = "in";
	var VALUE = "v";
	var PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
	var CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
	var NO_OP = "n";

	function getChangeNotifier(propertyValue, idx) {
		return function() {
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedIndexes[idx] = true;
			internalState.changeNotifier();
		}
	}

	function watchDumbElementForChanges(propertyValue, idx, componentScope, deep) {
		// if elements are primitives or anyway not something that wants control over changes, just add an in-depth watch
		return componentScope.$watch(function() {
			return propertyValue[idx];
		}, function(newvalue, oldvalue) {
			if (oldvalue === newvalue) return;
			var internalState = propertyValue[$sabloConverters.INTERNAL_IMPL];
			internalState.changedIndexes[idx] = { old: oldvalue };
			internalState.changeNotifier();
		}, deep);
	}

	/** Initializes internal state on a new array value */
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
				internalState.changeNotifier = changeNotifier; 
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
		} // else don't reinitilize it - it's already initialized
	}

	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			if (iS != null && angular.isDefined(iS)) {
				if (iS.arrayStructureUnwatch) iS.arrayStructureUnwatch();
				for (var key in iS.elUnwatch) {
					iS.elUnwatch[key]();
				}
				iS.arrayStructureUnwatch = null;
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
				for (var c = 0; c < value.length; c++) {
					var elem = value[c];
					if (!elem || !elem[$sabloConverters.INTERNAL_IMPL] || !elem[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
						// watch the child's value to see if it changes
						 internalState.elUnwatch[c] = watchDumbElementForChanges(value, c, componentScope, internalState[PUSH_TO_SERVER]);
					} // else if it's a smart value and the pushToServer is shallow or deep we must shallow watch it (it will manage it's own contents but we still must watch for reference changes);
					// but that is done below in a $watchCollection
				}

				// watch for add/remove and such operations on array; this is helpful also when 'smart' child values (that have .setChangeNotifier)
				// get changed completely by reference
				internalState.arrayStructureUnwatch = componentScope.$watchCollection(function() { return value; }, function(newWVal, oldWVal) {
					if (newWVal === oldWVal) return;

					if (newWVal === null || oldWVal === null || newWVal.length !== oldWVal.length) {
						internalState.allChanged = true;
						internalState.changeNotifier();
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

						if (referencesChanged) internalState.changeNotifier();
					}
				});
			}
		}
	}

	$sabloConverters.registerCustomPropertyHandler('JSON_arr', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = currentClientValue;

			// remove old watches (and, at the end create new ones) to avoid old watches getting triggered by server side change
			removeAllWatches(currentClientValue);

			try
			{
				if (serverJSONValue && serverJSONValue[VALUE]) {
					// full contents
					newValue = serverJSONValue[VALUE];
					initializeNewValue(newValue, serverJSONValue[CONTENT_VERSION]);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL];
					if (typeof serverJSONValue[PUSH_TO_SERVER] !== 'undefined') internalState[PUSH_TO_SERVER] = serverJSONValue[PUSH_TO_SERVER];

					if(newValue.length)
					{
						for (var c = 0; c < newValue.length; c++) {
							var elem = newValue[c];
							var conversionInfo = null;
							if (serverJSONValue[$sabloConverters.TYPES_KEY]) {
								conversionInfo = serverJSONValue[$sabloConverters.TYPES_KEY][c];
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
					}
				} else if (serverJSONValue && (serverJSONValue[UPDATES] || serverJSONValue[REMOVES] || serverJSONValue[ADDITIONS])) {
					// granular updates received;

					if (serverJSONValue[INITIALIZE]) initializeNewValue(currentClientValue, serverJSONValue[CONTENT_VERSION]); // this can happen when an array value was set completely in browser and the child elements need to instrument their browser values as well in which case the server sends 'initialize' updates for both this array and 'smart' child elements

					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];

					// if something changed browser-side, increasing the content version thus not matching next expected version,
					// we ignore this update and expect a fresh full copy of the array from the server (currently server value is leading/has priority because not all server side values might support being recreated from client values)
					if (internalState[CONTENT_VERSION] == serverJSONValue[CONTENT_VERSION]) {
						if (serverJSONValue[REMOVES])
						{
							var removes = serverJSONValue[REMOVES];
							for (var idx in removes)
							{
								currentClientValue.splice(removes[idx], 1 );
							}
						}
						if (serverJSONValue[ADDITIONS])
						{
							var additions = serverJSONValue[ADDITIONS];
							var conversionInfos = serverJSONValue[$sabloConverters.TYPES_KEY];
							var i;
							for (i in additions) {
								var element = additions[i];
								var idx = element[INDEX];
								var val = element[VALUE];

								var conversionInfo = null;
								if (conversionInfos && conversionInfos[i] && conversionInfos[i][VALUE]) {
									conversionInfo = conversionInfos[i][VALUE];
								}

								if (conversionInfo) {
									internalState.conversionInfo[idx] = conversionInfo;
									val = $sabloConverters.convertFromServerToClient(val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter);
								}
								currentClientValue.splice(idx, 0, val);

								if (val && val[$sabloConverters.INTERNAL_IMPL] && val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
									val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(currentClientValue, idx));
								}
							}
						}
						if (serverJSONValue[UPDATES])
						{
							var updates = serverJSONValue[UPDATES];
							var conversionInfos = serverJSONValue[$sabloConverters.TYPES_KEY];
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
									currentClientValue[idx] = val = $sabloConverters.convertFromServerToClient(val, conversionInfo, currentClientValue[idx], componentScope, componentModelGetter);
								} else currentClientValue[idx] = val;

								if (val && val[$sabloConverters.INTERNAL_IMPL] && val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
									// child is able to handle it's own change mechanism
									val[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(getChangeNotifier(currentClientValue, idx));
								}
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
				} else if (!serverJSONValue || !serverJSONValue[NO_OP]) newValue = null; // anything else would not be supported...	// TODO how to handle null values (special watches/complete array set from client)? if null is on server and something is set on client or the other way around?
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
					for (var c = 0; c < clientValue.length; c++) {
						var elem = clientValue[c];
						if (internalState.conversionInfo[c]) $sabloConverters.updateAngularScope(elem, internalState.conversionInfo[c], componentScope);
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
				} else if (angular.equals(newClientData, oldClientData)) {
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
