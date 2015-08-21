angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	ROW_ID_COL_KEY: '_svyRowId',
	FOR_FOUNDSET_PROPERTY: 'forFoundset',
	UPDATE_SIZE_CALLBACK:'updateSizeCallback'
})
.run(function ($sabloConverters, $foundsetTypeConstants, $viewportModule, $sabloUtils, $q) {
	var UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them
	var CONVERSIONS = "conversions"; // data conversion info

	var SERVER_SIZE = "serverSize";
	var SELECTED_ROW_INDEXES = "selectedRowIndexes";
	var SEND_SELECTION_RESPONSE = "selectionResponse";
	var SEND_SELECTION_REQUESTID = "selectionRequestID";
	var MULTI_SELECT = "multiSelect";
	var VIEW_PORT = "viewPort";
	var START_INDEX = "startIndex";
	var SIZE = "size";
	var ROWS = "rows";

	var PUSH_TO_SERVER = "w";

	var NO_OP = "n";

	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			var iS = value[$sabloConverters.INTERNAL_IMPL];
			if (iS.unwatchSelection) {
				iS.unwatchSelection();
				delete iS.unwatchSelection;
			}
			if (value[VIEW_PORT][ROWS]) $viewportModule.removeDataWatchesFromRows(value[VIEW_PORT][ROWS].length, iS);
		}
	};

	function addBackWatches(value, componentScope) {
		if (angular.isDefined(value) && value !== null) {
			var internalState = value[$sabloConverters.INTERNAL_IMPL];
			if (value[VIEW_PORT][ROWS]) {
				// prepare pushToServer values as needed for the following call
				var pushToServerValues;
				if (typeof internalState[PUSH_TO_SERVER] === 'undefined') pushToServerValues = {}; // that will not add watches for any column
				else pushToServerValues = internalState[PUSH_TO_SERVER]; // true or false then, just use that for adding watches (deep/shallow) to all columns
				
				$viewportModule.addDataWatchesToRows(value[VIEW_PORT][ROWS], internalState, componentScope, false, pushToServerValues); // shouldn't need component model getter - takes rowids directly from viewport
			}
			if (componentScope) internalState.unwatchSelection = componentScope.$watchCollection(function() { return value[SELECTED_ROW_INDEXES]; }, function (newSel, oldSel) {
				componentScope.$evalAsync(function() {
					if (newSel !== oldSel) {
						internalState.requests.push({newClientSelection: newSel});
						if (internalState.changeNotifier) internalState.changeNotifier();
					}
				});
			});
		}
	};

	$sabloConverters.registerCustomPropertyHandler('foundset', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, componentModelGetter) {
			var newValue = currentClientValue;

			// remove watches so that this update won't trigger them
			removeAllWatches(currentClientValue);

			// see if this is an update or whole value and handle it
			if (!serverJSONValue) {
				newValue = serverJSONValue;
			} else {
				// check for updates
				var updates = false;
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SERVER_SIZE])) {
					currentClientValue[SERVER_SIZE] = serverJSONValue[UPDATE_PREFIX + SERVER_SIZE]; // currentClientValue should always be defined in this case
					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES])) {
					currentClientValue[SELECTED_ROW_INDEXES] = serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES];
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SEND_SELECTION_RESPONSE])) {
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					if (internalState.deferred && internalState.msgid && internalState.msgid === serverJSONValue[UPDATE_PREFIX + SEND_SELECTION_REQUESTID]) {
						
						if (serverJSONValue[UPDATE_PREFIX + SEND_SELECTION_RESPONSE]) internalState.deferred.resolve(currentClientValue[SELECTED_ROW_INDEXES]);
						else internalState.deferred.reject(currentClientValue[SELECTED_ROW_INDEXES]);
						
						delete internalState.deferred;
					}
					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + VIEW_PORT])) {
					updates = true;
					var viewPortUpdate = serverJSONValue[UPDATE_PREFIX + VIEW_PORT];
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];

					if (angular.isDefined(viewPortUpdate[START_INDEX])) {
						currentClientValue[VIEW_PORT][START_INDEX] = viewPortUpdate[START_INDEX];
					}
					if (angular.isDefined(viewPortUpdate[SIZE])) {
						currentClientValue[VIEW_PORT][SIZE] = viewPortUpdate[SIZE];
						if (angular.isDefined(currentClientValue[VIEW_PORT][$foundsetTypeConstants.UPDATE_SIZE_CALLBACK])) {
							currentClientValue[VIEW_PORT][$foundsetTypeConstants.UPDATE_SIZE_CALLBACK](viewPortUpdate[SIZE]);
						}
					}
					if (angular.isDefined(viewPortUpdate[ROWS])) {
						$viewportModule.updateWholeViewport(currentClientValue[VIEW_PORT], ROWS, internalState, viewPortUpdate[ROWS],
								viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][ROWS] ? viewPortUpdate[CONVERSIONS][ROWS] : undefined, componentScope, componentModelGetter);
					} else if (angular.isDefined(viewPortUpdate[UPDATE_PREFIX + ROWS])) {
						$viewportModule.updateViewportGranularly(currentClientValue[VIEW_PORT][ROWS], internalState, viewPortUpdate[UPDATE_PREFIX + ROWS], viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS] ? viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS] : undefined, componentScope, componentModelGetter, false);
					}
				}

				// if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
				if (!updates && !serverJSONValue[NO_OP]) {
					newValue = serverJSONValue; // not updates - so whole thing received
					$sabloConverters.prepareInternalState(newValue);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
					
					if (typeof newValue[PUSH_TO_SERVER] !== 'undefined') {
						internalState[PUSH_TO_SERVER] = newValue[PUSH_TO_SERVER];
						delete newValue[PUSH_TO_SERVER];
					}

					internalState.requests = [];

					// convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
					$viewportModule.updateAllConversionInfo(newValue[VIEW_PORT][ROWS], internalState, newValue[VIEW_PORT][CONVERSIONS] ? newValue[VIEW_PORT][CONVERSIONS][ROWS] : undefined);
					if (newValue[VIEW_PORT][CONVERSIONS]) {
						// relocate conversion info in internal state and convert
						$sabloConverters.convertFromServerToClient(newValue[VIEW_PORT][ROWS], newValue[VIEW_PORT][CONVERSIONS][ROWS], componentScope, componentModelGetter);
						delete newValue[VIEW_PORT][CONVERSIONS];
					}

					// PUBLIC API to components; initialize the property value; make it 'smart'
					newValue.loadRecordsAsync = function(startIndex, size) {
						internalState.requests.push({newViewPort: {startIndex : startIndex, size : size}});
						if (internalState.changeNotifier) internalState.changeNotifier();
					};
					newValue.loadExtraRecordsAsync = function(negativeOrPositiveCount) {
						internalState.requests.push({loadExtraRecords: negativeOrPositiveCount});
						if (internalState.changeNotifier) internalState.changeNotifier();
					};
					newValue.sort = function(columns) {
						internalState.requests.push({sort: columns});
						if (internalState.changeNotifier) internalState.changeNotifier();
					}
					newValue.setPreferredViewportSize = function(size) {
						internalState.requests.push({preferredViewportSize: size});
						if (internalState.changeNotifier) internalState.changeNotifier();
					}
					newValue.requestSelectionUpdate = function(tmpSelectedRowIdxs) {
						if (internalState.deferred) {
							internalState.deferred.reject("canceled");
						}
						delete internalState.deferred;

						internalState.deferred = $q.defer();
						if (internalState.msgid === undefined) {
							internalState.msgid = 1;
						}
						else {
							internalState.msgid++;
						}
						internalState.requests.push({newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: internalState.msgid});
						if (internalState.changeNotifier) internalState.changeNotifier();

						return internalState.deferred.promise;
					}
					// PRIVATE STATE AND IMPL for $sabloConverters (so something components shouldn't use)
					// $sabloConverters setup
					internalState.setChangeNotifier = function(changeNotifier) {
						internalState.changeNotifier = changeNotifier;
					}
					internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }

					// private state/impl
				}

			}

			// restore/add watches
			addBackWatches(newValue, componentScope);

			return newValue;
		},

		updateAngularScope: function(clientValue, componentScope) {
			removeAllWatches(clientValue);
			if (componentScope) addBackWatches(clientValue, componentScope);

			if (clientValue) {
				var internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					$viewportModule.updateAngularScope(clientValue[VIEW_PORT][ROWS], internalState, componentScope, false);
				}
			}
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var newDataInternalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (newDataInternalState.isChanged()) {
					var tmp = newDataInternalState.requests;
					newDataInternalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
	});
})
