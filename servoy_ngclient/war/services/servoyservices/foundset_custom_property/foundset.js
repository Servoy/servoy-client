angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	ROW_ID_COL_KEY: '_svyRowId'
})
.run(function ($sabloConverters, $rootScope, $foundsetTypeConstants, $sabloUtils) {
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
	
	function rowIgnoreDataChanged(idx, foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		var rowIgnoreChangeValue = internalState.ignoreDataChange[idx] = {};
		var dataprovider;
		for (dataprovider in foundsetValue[VIEW_PORT][ROWS][idx]) {
			if (dataprovider !== $foundsetTypeConstants.ROW_ID_COL_KEY) {
				rowIgnoreChangeValue[dataprovider] = foundsetValue[VIEW_PORT][ROWS][idx][dataprovider];
			}
		}
	}

	function rowsIgnoreDataChanged(foundsetValue) {
		var i;
		for (i = foundsetValue[VIEW_PORT][ROWS].length - 1; i >= 0; i--) {
			rowIgnoreDataChanged(i, foundsetValue);
		}
	};
	
	function addDataWatchToDataprovider(dataprovider, idx, foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		internalState.unwatchData[idx].push(
				$rootScope.$watch(function() { return foundsetValue[VIEW_PORT][ROWS][idx][dataprovider]; }, function (newData, oldData) {
					if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
						var rowIgnoreChangeValue = internalState.ignoreDataChange[idx];
						var changed = false;
						if (rowIgnoreChangeValue && angular.isDefined(rowIgnoreChangeValue[dataprovider])) {
							changed = (rowIgnoreChangeValue[dataprovider] !== newData);
							delete rowIgnoreChangeValue[dataprovider];
						} else changed = true;

						if (changed) {
							var r = {};
							r[$foundsetTypeConstants.ROW_ID_COL_KEY] = foundsetValue[VIEW_PORT][ROWS][idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
							r.dp = dataprovider;
							r.value = newData;

							// convert new data if necessary
							var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][r[$foundsetTypeConstants.ROW_ID_COL_KEY]] : undefined;
							if (conversionInfo && conversionInfo[dataprovider]) r.value = $sabloConverters.convertFromClientToServer(r.value, conversionInfo[dataprovider], oldData);
							else r.value = $sabloUtils.convertClientObject(r.value);

							internalState.requests.push({dataChanged: r});
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					}
				})
		);
	}
	
	function addDataWatchesToRow(idx, foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		internalState.unwatchData[idx] = [];
		var dataprovider;
		for (dataprovider in foundsetValue[VIEW_PORT][ROWS][idx]) {
			if (dataprovider !== $foundsetTypeConstants.ROW_ID_COL_KEY) addDataWatchToDataprovider(dataprovider, idx, foundsetValue);
		}
	}

	function addDataWatchesToRows(foundsetValue) {
		var i;
		for (i = foundsetValue[VIEW_PORT][ROWS].length - 1; i >= 0; i--) {
			addDataWatchesToRow(i, foundsetValue);
		}
	};
	
	function removeDataWatchesFromRow(idx, foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		if (internalState.unwatchData) {
			for (j = internalState.unwatchData[idx].length - 1; j >= 0; j--)
				internalState.unwatchData[idx][j]();
			delete internalState.unwatchData[idx];
		}
	};

	function removeDataWatchesFromRows(foundsetValue) {
		var i;
		for (i = foundsetValue[VIEW_PORT][ROWS].length - 1; i >= 0; i--) {
			removeDataWatchesFromRow(i, foundsetValue);
		}
	};
	
	// TODO we could keep only one row conversion instead of conversion info for all cells... 
	function removeRowConversionInfo(idx, foundsetValue) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		if (angular.isDefined(internalState[CONVERSIONS])) {
			delete internalState[CONVERSIONS][foundsetValue[VIEW_PORT][ROWS][idx][$foundsetTypeConstants.ROW_ID_COL_KEY]];
		}
	}
	
	function updateRowConversionInfo(idx, foundsetValue, serverConversionInfo) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		if (angular.isUndefined(internalState[CONVERSIONS])) {
			internalState[CONVERSIONS] = {};
			internalState[CONVERSIONS][foundsetValue[VIEW_PORT][ROWS][idx][$foundsetTypeConstants.ROW_ID_COL_KEY]] = serverConversionInfo;
		}
	}
	
	function updateAllConversionInfo(foundsetValue, serverConversionInfo) {
		var internalState = foundsetValue[$sabloConverters.INTERNAL_IMPL];
		var i;
		for (i = foundsetValue[VIEW_PORT][ROWS].length - 1; i >= 0; i--)
			updateRowConversionInfo(i, foundsetValue, serverConversionInfo[i]);
	}
	
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
					var viewPortUpdate = serverJSONValue[UPDATE_PREFIX + VIEW_PORT];
					var internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					
					if (angular.isDefined(viewPortUpdate[START_INDEX])) {
						currentClientValue[VIEW_PORT][START_INDEX] = viewPortUpdate[START_INDEX];
					}
					if (angular.isDefined(viewPortUpdate[SIZE])) {
						currentClientValue[VIEW_PORT][SIZE] = viewPortUpdate[SIZE];
					}
					if (angular.isDefined(viewPortUpdate[ROWS])) {
						removeDataWatchesFromRows(currentClientValue);
						currentClientValue[VIEW_PORT][ROWS] = viewPortUpdate[ROWS];
						if (viewPortUpdate[CONVERSIONS]) {
							// do the actual conversion
							$sabloConverters.convertFromServerToClient(currentClientValue[VIEW_PORT][ROWS], viewPortUpdate[CONVERSIONS][ROWS]);
							// update conversion info
							updateAllConversionInfo(currentClientValue, viewPortUpdate[CONVERSIONS][ROWS]);
						}
						addDataWatchesToRows(currentClientValue);
						rowsIgnoreDataChanged(currentClientValue);
					} else if (angular.isDefined(viewPortUpdate[UPDATE_PREFIX + ROWS])) {
						// partial row updates (remove/insert/update)
						var rowUpdates = viewPortUpdate[UPDATE_PREFIX + ROWS]; // array of
						if (viewPortUpdate[CONVERSIONS]) $sabloConverters.convertFromServerToClient(viewPortUpdate[UPDATE_PREFIX + ROWS], viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS]);
						
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
								for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
									// rows[j] = rowUpdate.rows[j - rowUpdate.startIndex];
									// because of a bug in ngGrid that doesn't detect array item changes if array length doesn't change
									// we will reuse the existing row object as a workaround for updating (a case was filed for that bug as it's breaking scenarios with
									// delete and insert as well)
									var dpName;
									for (dpName in rowUpdate.rows[j - rowUpdate.startIndex]) rows[j][dpName] = rowUpdate.rows[j - rowUpdate.startIndex][dpName];
									
									if (viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS][j - rowUpdate.startIndex]) {
										updateRowConversionInfo(j, currentClientValue, viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS][j - rowUpdate.startIndex]);
									}
									rowIgnoreDataChanged(j, currentClientValue);
								}
							} else if (rowUpdate.type == INSERT) {
								var oldLength = rows.length;
								for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) {
									rows.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
									if (viewPortUpdate[CONVERSIONS] && viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS][j]) {
										updateRowConversionInfo(rowUpdate.startIndex, currentClientValue, viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS][j]);
									}
								}
								// insert might have made obsolete some records in cache; remove those
								if (rows.length > currentClientValue[VIEW_PORT].size) {
									// remove conversion info for these rows as well
									if (internalState[CONVERSIONS]) {
										for (j = currentClientValue[VIEW_PORT].size; j < rows.length; j++)
											removeRowConversionInfo(j, currentClientValue);
									}
									
									rows.splice(currentClientValue[VIEW_PORT].size, rows.length - currentClientValue[VIEW_PORT].size);
								}
								for (j = oldLength; j < rows.length; j++)
									addDataWatchesToRow(j, currentClientValue);
								for (j = rowUpdate.startIndex; j < rows.length; j++) {
									rowIgnoreDataChanged(j, currentClientValue);
								}
							} else if (rowUpdate.type == DELETE) {
								var oldLength = rows.length;
								if (internalState[CONVERSIONS]) {
									// delete conversion info for deleted rows
									for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
										removeRowConversionInfo(j, currentClientValue);
								}
								rows.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
								for (j = 0; j < rowUpdate.rows.length; j++) {
									rows.push(rowUpdate.rows[j]);
									if (viewPortUpdate[CONVERSIONS]) {
										var c = viewPortUpdate[CONVERSIONS][UPDATE_PREFIX + ROWS][j];
										if (angular.isDefined(c)) updateRowConversionInfo(rows.length - 1, currentClientValue, c);
									}
								}
								for (j = rows.length; j < oldLength; j++)
									removeDataWatchesFromRow(j, currentClientValue);
								for (j = rowUpdate.startIndex; j < rows.length; j++) {
									rowIgnoreDataChanged(j, currentClientValue);
								}
							}
						}
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
						updateAllConversionInfo(newValue, newValue[VIEW_PORT][CONVERSIONS][ROWS]);
						$sabloConverters.convertFromServerToClient(newValue[VIEW_PORT][ROWS], newValue[VIEW_PORT][CONVERSIONS][ROWS]);
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
							internalState.requests.push({newClientSelection: newSel});
							if (internalState.changeNotifier) internalState.changeNotifier();
						}
					});
					
					// watch for client dataProvider changes and send them to server
					internalState.unwatchData = {}; // { rowPk: [unwatchDataProvider1Func, ...], ... }
					internalState.ignoreDataChange = {};
					rowsIgnoreDataChanged(newValue);
					addDataWatchesToRows(newValue);
				}
				
			}		 
			if (angular.isDefined(currentClientValue) && newValue !== currentClientValue) {
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
					newDataInternalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
	});
})
