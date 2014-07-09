angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	ROW_ID_COL_KEY: '_svyRowId'
})
.run(function ($sabloConverters, $rootScope, $foundsetTypeConstants) {
	var UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them
	var CONVERSIONS = "conversions"; // data conversion info

	var SERVER_SIZE = "serverSize";
	var SELECTED_ROW_INDEXES = "selectedRowIndexes";
	var MULTI_SELECT = "multiSelect";
	var VIEW_PORT = "viewPort";
	var START_INDEX = "startIndex";
	var SIZE = "size";
	var ROWS = "rows";
	
	var CHANGE = 0;
	var INSERT = 1;
	var DELETE = 2;
	
	var NO_OP = "noOP";
	
	function addDataWatchesToRows(foundsetValue) {
		var i;
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		for (i = foundsetValue.viewPort.rows.length - 1; i >= 0; i--) {
			var unwatchRowFuncs = []; 
			internalState.unwatchData[foundsetValue.viewPort.rows[i][$foundsetTypeConstants.ROW_ID_COL_KEY]] = unwatchRowFuncs;
			var dataprovider;
			for (dataprovider in foundsetValue.viewPort.rows[i]) {
				if (dataprovider !== $foundsetTypeConstants.ROW_ID_COL_KEY) unwatchRowFuncs.push(
						$rootScope.$watch(function() { return foundsetValue.viewPort.rows[i][dataprovider]; }, function (newData, oldData) {
							if (newData !== oldData) {
								var changed = false;
								var cellIgnoreChangeValue = foundsetValue.viewPort
//								if (foundsetValue.__ignoreSelectedChange) {
//									if (foundsetValue.__ignoreSelectedChange.length == newSel.length) {
//										var i;
//										for (i = 0; i < foundsetValue.__ignoreSelectedChange.length; i++)
//											if (foundsetValue.__ignoreSelectedChange[i] !== newSel[i]) { changed = true; break; }
//									} else changed = true;
//									foundsetValue.__ignoreSelectedChange = null;
//								} else changed = true;
//
//								if (changed) {
//									if (!foundsetValue.requests) foundsetValue.requests = [];
//									foundsetValue.requests.push({newClientSelection: newSel});
//									if (foundsetValue.changeNotifier) foundsetValue.changeNotifier();
//								}
							}
						})
				);
			}
		}
	};

	function removeDataWatchesFromRows(foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		if (internalState.unwatchData) {
			var pk;
			for (pk in internalState.unwatchData) {
				var i;
				for (i = internalState.unwatchData[pk].length - 1; i >= 0; i--)
					internalState.unwatchData[pk][i]();
			}
			delete internalState.unwatchData;
		}
	};
	
	$sabloConverters.registerCustomPropertyHandler('foundset', {
		fromServerToClient: function (serverJSONValue, currentClientValue) {
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
					var v = serverJSONValue[UPDATE_PREFIX + VIEW_PORT];
					if (angular.isDefined(v[START_INDEX])) {
						currentClientValue[VIEW_PORT][START_INDEX] = v[START_INDEX];
					}
					if (angular.isDefined(v[SIZE])) {
						currentClientValue[VIEW_PORT][SIZE] = v[SIZE];
					}
					if (angular.isDefined(v[ROWS])) {
						currentClientValue[VIEW_PORT][ROWS] = v[ROWS];
						if (v[CONVERSIONS]) $sabloConverters.convertFromServerToClient(currentClientValue[VIEW_PORT][ROWS], v[CONVERSIONS][ROWS]);
					} else if (angular.isDefined(v[UPDATE_PREFIX + ROWS])) {
						// partial row updates (remove/insert/update)
						var rowUpdates = v[UPDATE_PREFIX + ROWS]; // array of
						if (v[CONVERSIONS]) $sabloConverters.convertFromServerToClient(v[UPDATE_PREFIX + ROWS], v[CONVERSIONS][UPDATE_PREFIX + ROWS]);
						
						// {
						//   "rows": rowData, // array again
						//   "startIndex": ...,
						//   "endIndex": ...,
						//   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
						// }
						
						// apply them one by one
						var i;
						var j;
						var rows = currentClientValue[VIEW_PORT][ROWS];
						for (i = 0; i < rowUpdates.length; i++) {
							var rowUpdate = rowUpdates[i];
							if (rowUpdate.type == CHANGE) {
								for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) rows[j] = rowUpdate.rows[j - rowUpdate.startIndex];
							} else if (rowUpdate.type == INSERT) {
								for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) rows.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
								// insert might have made obsolete some records in cache; remove those
								if (rows.length > currentClientValue[VIEW_PORT].size) rows.splice(currentClientValue[VIEW_PORT].size, rows.length - currentClientValue[VIEW_PORT].size);
							} else if (rowUpdate.type == DELETE) {
								rows.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
								for (j = 0; j < rowUpdate.rows.length; j++) rows.push(rowUpdate.rows[j]);
							}
						}
					}
				}
				// if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
				if (!updates && serverJSONValue[NO_OP] !== 0) {
					newValue = serverJSONValue; // not updates - so whole thing received
					var internalState = newValue[$sabloConverters.INTERNAL_IMPL] = {}; // internal state and $sabloConverters interface
					
					// convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
					if (newValue[VIEW_PORT][CONVERSIONS]) $sabloConverters.convertFromServerToClient(newValue[VIEW_PORT][ROWS], newValue[VIEW_PORT][CONVERSIONS][ROWS]);
					
					// PUBLIC API to components; initialize the property value; make it 'smart'
					newValue.loadRecordsAsync = function(startIndex, size) {
						if (!internalState.requests) internalState.requests = [];
						internalState.requests.push({newViewPort: {startIndex : startIndex, size : size}});
						if (internalState.changeNotifier) internalState.changeNotifier();
					};
					newValue.loadExtraRecordsAsync = function(negativeOrPositiveCount) {
						if (!internalState.requests) internalState.requests = [];
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
					internalState.unwatchSelection = $rootScope.$watchCollection(function() { return newValue[SELECTED_ROW_INDEXES]; }, function (newSel) {
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
							if (!internalState.requests) internalState.requests = [];
							internalState.requests.push({newClientSelection: newSel});
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					});
					
					// watch for client dataProvider changes and send them to server
					internalState.unwatchData = {}; // { rowPk: [unwatchDataProvider1Func, ...], ... }
					addDataWatchesToRows(newValue);
				}
				
			}		 
			if (angular.isDefined(currentClientValue) && newValue != currentClientValue) {
				// the client side object will change completely, and the old one probably has watches defined...
				// unregister those
				
				if (currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection) {
					currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection();
					delete currentClientValue[$sabloConverters.INTERNAL_IMPL].unwatchSelection;
				}
				removeDataWatchesFromRows(currentClientValue);
			}

			return newValue;
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				var newDataInternalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (newDataInternalState.isChanged()) {
					var tmp = newDataInternalState.requests;
					delete newDataInternalState.requests;
					return tmp;
				}
			}
			return [];
		}
	});
})
