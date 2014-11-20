angular.module('foundset_viewport_module', ['webSocketModule'])
//Viewport reuse code module -------------------------------------------
.factory("$viewportModule", function ($sabloConverters, $foundsetTypeConstants, $sabloUtils) {

	var CONVERSIONS = "viewportConversions"; // data conversion info

	var CHANGE = 0;
	var INSERT = 1;
	var DELETE = 2;
	
	function addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope) {
		if (componentScope) {
			function queueChange(newData, oldData) {
				var r = {};
				r[$foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
				r.dp = columnName;
				r.value = newData;

				// convert new data if necessary
				var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][r[$foundsetTypeConstants.ROW_ID_COL_KEY]] : undefined;
				if (conversionInfo && conversionInfo[columnName]) r.value = $sabloConverters.convertFromClientToServer(r.value, conversionInfo[columnName], oldData);
				else r.value = $sabloUtils.convertClientObject(r.value);

				internalState.requests.push({viewportDataChanged: r});
				if (internalState.changeNotifier) internalState.changeNotifier();
			}
			
			
			if (viewPort[idx][columnName] && viewPort[idx][columnName][$sabloConverters.INTERNAL_IMPL] && viewPort[idx][columnName][$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
				// smart propery value
				
				// watch for change-by reference
				internalState.unwatchData[idx].push(
						componentScope.$watch(function() { return viewPort[idx][columnName]; }, function (newData, oldData) {
							if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
								queueChange(newData, oldData);
							}
						})
				);

				viewPort[idx][columnName][$sabloConverters.INTERNAL_IMPL].setChangeNotifier(function () {
					if (viewPort[idx][columnName][$sabloConverters.INTERNAL_IMPL].isChanged()) queueChange(viewPort[idx][columnName], viewPort[idx][columnName]);
				});
			} else {
				// deep watch for change-by content / dumb value
				internalState.unwatchData[idx].push(
						componentScope.$watch(function() { return viewPort[idx][columnName]; }, function (newData, oldData) {
							if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
								var changed = false;
								if (typeof newVal == "object") {
									var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]] : undefined;
									if ($sabloUtils.isChanged(newData, oldData, conversionInfo)) {
										changed = true;
									}
								} else {
									changed = true;
								}
								if (changed) queueChange(newData, oldData);
							}
						}, true)
				);
			}
		}
	};

	function addDataWatchesToRow(idx, viewPort, internalState, componentScope) {
		if (!angular.isDefined(internalState.unwatchData)) internalState.unwatchData = {};
		internalState.unwatchData[idx] = [];
		var columnName;
		for (columnName in viewPort[idx]) {
			if (columnName !== $foundsetTypeConstants.ROW_ID_COL_KEY) addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope);
		}
	};

	function addDataWatchesToRows(viewPort, internalState, componentScope) {
		var i;
		for (i = viewPort.length - 1; i >= 0; i--) {
			addDataWatchesToRow(i, viewPort, internalState, componentScope);
		}
	};

	function removeDataWatchesFromRow(idx, internalState) {
		if (internalState.unwatchData) {
			for (j = internalState.unwatchData[idx].length - 1; j >= 0; j--)
				internalState.unwatchData[idx][j]();
			delete internalState.unwatchData[idx];
		}
	};

	function removeDataWatchesFromRows(rowCount, internalState) {
		var i;
		for (i = rowCount - 1; i >= 0; i--) {
			removeDataWatchesFromRow(i, internalState);
		}
	};

	// TODO we could keep only one row conversion instead of conversion info for all cells... 
	function removeRowConversionInfo(rowId, internalState) {
		if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(rowId)) {
			delete internalState[CONVERSIONS][rowId];
		}
	};

	function updateRowConversionInfo(idx, viewPort, internalState, serverConversionInfo) {
		if (angular.isUndefined(internalState[CONVERSIONS])) {
			internalState[CONVERSIONS] = {};
		}
		var rowId = viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
		if (angular.isDefined(rowId)) internalState[CONVERSIONS][rowId] = serverConversionInfo;
	};

	function updateAllConversionInfo(viewPort, internalState, serverConversionInfo) {
		internalState[CONVERSIONS] = {};
		var i;
		for (i = viewPort.length - 1; i >= 0; i--)
			updateRowConversionInfo(i, viewPort, internalState, serverConversionInfo[i]);
	};

	function updateWholeViewport(viewPortHolder, viewPortPropertyName, internalState, viewPortUpdate, viewPortUpdateConversions, componentScope) {
		viewPortHolder[viewPortPropertyName] = viewPortUpdate;
		if (viewPortUpdateConversions) {
			// do the actual conversion
			$sabloConverters.convertFromServerToClient(viewPortHolder[viewPortPropertyName], viewPortUpdateConversions, componentScope);
			// update conversion info
			updateAllConversionInfo(viewPortHolder[viewPortPropertyName], internalState, viewPortUpdateConversions);
		}
	};

	function updateViewportGranularly(viewPortHolder, viewPortPropertyName, internalState, rowUpdates, rowUpdateConversions, componentScope) {
		// partial row updates (remove/insert/update)
		var viewPort = viewPortHolder[viewPortPropertyName];

		// {
		//   "rows": rowData, // array again
		//   "startIndex": ...,
		//   "endIndex": ...,
		//   "type": ... // ONE OF CHANGE = 0; INSERT = 1; DELETE = 2;
		// }

		// apply them one by one
		var i;
		var j;
		for (i = 0; i < rowUpdates.length; i++) {
			var rowUpdate = rowUpdates[i];
			if (rowUpdate.type == CHANGE) {
				for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++) {
					// rows[j] = rowUpdate.rows[j - rowUpdate.startIndex];
					// because of a bug in ngGrid that doesn't detect array item changes if array length doesn't change
					// we will reuse the existing row object as a workaround for updating (a case was filed for that bug as it's breaking scenarios with
					// delete and insert as well)
					
					var dpName;
					var rowId = viewPort[j][$foundsetTypeConstants.ROW_ID_COL_KEY];
					var relIdx = j - rowUpdate.startIndex;
					
					// apply the conversions
					var rowConversionUpdate = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[relIdx] : undefined;
					if (rowConversionUpdate) $sabloConverters.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], componentScope);
					
					// this might be a partial update (so only a column changed for example) - don't drop all other columns, just update the ones we received
					for (dpName in rowUpdate.rows[relIdx]) {
						// update value
						viewPort[j][dpName] = rowUpdate.rows[relIdx][dpName];
						
						if (rowConversionUpdate) {
							// update conversion info
							if (angular.isUndefined(internalState[CONVERSIONS])) {
								internalState[CONVERSIONS] = {};
							}
							if (angular.isDefined(rowId)) {
								   if (angular.isUndefined(internalState[CONVERSIONS][rowId]))
								   {
									   internalState[CONVERSIONS][rowId] = {};
								   }
								    internalState[CONVERSIONS][rowId][dpName] = rowConversionUpdate[dpName];
							}
						}
					}
				}
			} else if (rowUpdate.type == INSERT) {
				if (rowUpdateConversions && rowUpdateConversions[i]) $sabloConverters.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, componentScope);

				for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) {
					viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
					if (rowUpdateConversions && rowUpdateConversions[j]) {
						updateRowConversionInfo(rowUpdate.startIndex, viewPort, internalState, rowUpdateConversions[j]);
					}
				}
				// insert might have made obsolete some records in cache; remove those; for inserts
				// !!! rowUpdate.endIndex means the new length of the viewport
				if (viewPort.length > rowUpdate.endIndex) {
					// remove conversion info for these rows as well
					if (internalState[CONVERSIONS]) {
						for (j = rowUpdate.endIndex; j < viewPort.length; j++)
							removeRowConversionInfo(viewPort[j][$foundsetTypeConstants.ROW_ID_COL_KEY], internalState);
					}

					viewPort.splice(rowUpdate.endIndex, viewPort.length - rowUpdate.endIndex);

//					// workaround follows for a bug in ng-grid (changing the row references while the array has the same length doesn't trigger a UI update)
//					// see https://github.com/angular-ui/ng-grid/issues/1279
//					viewPortHolder[viewPortPropertyName] = viewPort.splice(0); // changes array reference completely while keeping contents
//					viewPort = viewPortHolder[viewPortPropertyName];
				}
			} else if (rowUpdate.type == DELETE) {
				if (rowUpdateConversions && rowUpdateConversions[i]) $sabloConverters.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, componentScope);

				var oldLength = viewPort.length;
				if (internalState[CONVERSIONS]) {
					// delete conversion info for deleted rows
					for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
						removeRowConversionInfo(viewPort[j][$foundsetTypeConstants.ROW_ID_COL_KEY], internalState);
				}
				viewPort.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
				for (j = 0; j < rowUpdate.rows.length; j++) {
					viewPort.push(rowUpdate.rows[j]);
					if (rowUpdateConversions) {
						var c = rowUpdateConversions[j];
						if (angular.isDefined(c)) updateRowConversionInfo(viewPort.length - 1, viewPort, internalState, c);
					}
				}
//				if (oldLength == viewPort.length) {
//					// workaround follows for a bug in ng-grid (changing the row references while the array has the same length doesn't trigger a UI update)
//					// see https://github.com/angular-ui/ng-grid/issues/1279
//					viewPortHolder[viewPortPropertyName] = viewPort.splice(0); // changes array reference completely while keeping contents
//					viewPort = viewPortHolder[viewPortPropertyName];
//				}
			}
		}
	};

	return {
		updateWholeViewport: updateWholeViewport,
		updateViewportGranularly: updateViewportGranularly,
		
		addDataWatchesToRows: addDataWatchesToRows,
		removeDataWatchesFromRows: removeDataWatchesFromRows,
		updateAllConversionInfo: updateAllConversionInfo
	};
	
});
