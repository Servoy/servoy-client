angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	ROW_ID_COL_KEY: '_svyRowId'
})
.run(function ($sabloConverters, $foundsetTypeConstants, $viewportModule, $sabloUtils) {
	var UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them
	var CONVERSIONS = "conversions"; // data conversion info

	var SERVER_SIZE = "serverSize";
	var SELECTED_ROW_INDEXES = "selectedRowIndexes";
	var MULTI_SELECT = "multiSelect";
	var VIEW_PORT = "viewPort";
	var START_INDEX = "startIndex";
	var SIZE = "size";
	var ROWS = "rows";
	
	var NO_OP = "noOP";
	
	$sabloConverters.registerCustomPropertyHandler('foundset', {
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope) {
			var newValue = currentClientValue;
			
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
					currentClientValue[$sabloConverters.INTERNAL_IMPL].ignoreSelectedChangeValue = currentClientValue[SELECTED_ROW_INDEXES]; // don't send back to server selection that came from server
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
					}
					if (angular.isDefined(viewPortUpdate[ROWS])) {
						$viewportModule.updateWholeViewport(currentClientValue[VIEW_PORT], ROWS, internalState, viewPortUpdate[ROWS],
								viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][ROWS] ? viewPortUpdate[CONVERSIONS][ROWS] : undefined, componentScope);
					} else if (angular.isDefined(viewPortUpdate[UPDATE_PREFIX + ROWS])) {
						$viewportModule.updateViewportGranularly(currentClientValue[VIEW_PORT], ROWS, internalState, viewPortUpdate[UPDATE_PREFIX + ROWS], viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS] ? viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS] : undefined, componentScope);
					}
				}
				
				// if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
				if (!updates && serverJSONValue[NO_OP] !== 0) {
					newValue = serverJSONValue; // not updates - so whole thing received
					$sabloConverters.prepareInternalState(newValue);
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
					internalState.requests = [];
					
					// convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
					if (newValue[VIEW_PORT][CONVERSIONS]) {
						// relocate conversion info in internal state and convert
						$viewportModule.updateAllConversionInfo(newValue[VIEW_PORT][ROWS], internalState, newValue[VIEW_PORT][CONVERSIONS][ROWS]);
						$sabloConverters.convertFromServerToClient(newValue[VIEW_PORT][ROWS], newValue[VIEW_PORT][CONVERSIONS][ROWS], componentScope);
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
					
					// PRIVATE STATE AND IMPL for $sabloConverters (so something components shouldn't use)
					// $sabloConverters setup
					internalState.setChangeNotifier = function(changeNotifier) {
						internalState.changeNotifier = changeNotifier; 
					}
					internalState.isChanged = function() { return internalState.requests && (internalState.requests.length > 0); }
					
					// private state/impl

					// watch for client selection changes and send them to server
					internalState.ignoreSelectedChangeValue = newValue[SELECTED_ROW_INDEXES]; // ignore initial watch change
					if (componentScope) internalState.unwatchSelection = componentScope.$watchCollection(function() { return newValue[SELECTED_ROW_INDEXES]; }, function (newSel) {
						var changed = false;
						if (internalState.ignoreSelectedChangeValue) {
							if (internalState.ignoreSelectedChangeValue.length == newSel.length) {
								var i;
								for (i = 0; i < internalState.ignoreSelectedChangeValue.length; i++)
									if (internalState.ignoreSelectedChangeValue[i] !== newSel[i]) { changed = true; break; }
							} else changed = true;
							internalState.ignoreSelectedChangeValue = null;
						} else changed = true;

						if (changed) {
							internalState.requests.push({newClientSelection: newSel});
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					});
					
					// watch for client dataProvider changes and send them to server
					internalState.unwatchData = {}; // { rowPk: [unwatchDataProvider1Func, ...], ... }
					$viewportModule.addDataWatchesToRows(newValue[VIEW_PORT][ROWS], internalState, componentScope);
				}
				
			}		 
			if (angular.isDefined(currentClientValue) && newValue !== currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				
				if (currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection) {
					currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection();
					delete currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection;
				}
				$viewportModule.removeDataWatchesFromRows(currentClientValue[VIEW_PORT][ROWS].length, currentClientValue[$sabloConverters.INTERNAL_IMPL]);
			}

			return newValue;
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
