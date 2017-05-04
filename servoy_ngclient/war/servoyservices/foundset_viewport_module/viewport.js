angular.module('foundset_viewport_module', ['webSocketModule'])
//Viewport reuse code module -------------------------------------------
.factory("$viewportModule", function ($sabloConverters, $foundsetTypeConstants, $sabloUtils) {

	var CONVERSIONS = "viewportConversions"; // data conversion info

	var CHANGE = $foundsetTypeConstants.ROWS_CHANGED;
	var INSERT = $foundsetTypeConstants.ROWS_INSERTED;
	var DELETE = $foundsetTypeConstants.ROWS_DELETED;
	
	var DATAPROVIDER_KEY = "dp";
	var VALUE_KEY = "value";

	function addDataWatchToCell(columnName /*can be null*/, idx, viewPort, internalState, componentScope, dumbWatchType) {
		if (componentScope) {
			function queueChange(newData, oldData) {
				var r = {};

				if (angular.isDefined(internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
					r[$foundsetTypeConstants.ROW_ID_COL_KEY] = internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]().viewPort.rows[idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
				} else r[$foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]; // if it doesn't have internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] then it's probably the foundset property's viewport directly which has those in the viewport
				r[DATAPROVIDER_KEY] = columnName;
				r[VALUE_KEY] = newData;

				// convert new data if necessary
				var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][idx] : undefined;
				if (conversionInfo && conversionInfo[columnName]) r[VALUE_KEY] = $sabloConverters.convertFromClientToServer(r[VALUE_KEY], conversionInfo[columnName], oldData);
				else r[VALUE_KEY] = $sabloUtils.convertClientObject(r[VALUE_KEY]);

				internalState.requests.push({viewportDataChanged: r});
				if (internalState.changeNotifier) internalState.changeNotifier();
			}

			function getCellValue() { 
				return columnName == null ? viewPort[idx] : viewPort[idx][columnName]
			}; // viewport row can be just a value or an object of key/value pairs

			if (getCellValue() && getCellValue()[$sabloConverters.INTERNAL_IMPL] && getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
				// smart property value

				// watch for change-by reference if needed
				if (typeof (dumbWatchType) !== 'undefined') internalState.unwatchData[idx].push(
						componentScope.$watch(getCellValue, function (newData, oldData) {
							if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
								queueChange(newData, oldData);
							}
						})
				);

				// we don't care to check below for dumbWatchType because some types (see foundset) need to send internal protocol messages even if they are not watched/changeable on server
				getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(function () {
					if (getCellValue()[$sabloConverters.INTERNAL_IMPL].isChanged()) queueChange(getCellValue(), getCellValue());
				});
			} else if (typeof (dumbWatchType) !== 'undefined') {
				// deep watch for change-by content / dumb value
				internalState.unwatchData[idx].push(
						componentScope.$watch(getCellValue, function (newData, oldData) {
							if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
								var changed = false;
								if (typeof newVal == "object") {
									var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][idx] : undefined;
									if ($sabloUtils.isChanged(newData, oldData, conversionInfo)) {
										changed = true;
									}
								} else {
									changed = true;
								}
								if (changed) queueChange(newData, oldData);
							}
						}, dumbWatchType)
				);
			}
		}
	};

	// 1. dumbWatchMarkers when simpleRowValue === true means (undefined - no watch needed, true/false - deep/shallow watch)
	// 2. dumbWatchMarkers when simpleRowValue === false means (undefined - watch all cause there is no marker information,
	//                                                          { col1: true; col2: false } means watch type needed for each column and no watches for the ones not mentioned,
	//                                                          true/false directly means deep/shallow watch for all columns)
	function addDataWatchesToRow(idx, viewPort, internalState, componentScope, simpleRowValue/*not key/value pairs in each row*/, dumbWatchMarkers) {
		if (!angular.isDefined(internalState.unwatchData)) internalState.unwatchData = {};
		internalState.unwatchData[idx] = [];
		if (simpleRowValue) {
			addDataWatchToCell(null, idx, viewPort, internalState, componentScope, dumbWatchMarkers);
		} else {
			var columnName;
			for (columnName in viewPort[idx]) {
				if (columnName !== $foundsetTypeConstants.ROW_ID_COL_KEY) 
					addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope,
							(typeof dumbWatchMarkers === 'boolean' ? dumbWatchMarkers : (dumbWatchMarkers ? dumbWatchMarkers[columnName] : true)));
			}
		}
	};

	function addDataWatchesToRows(viewPort, internalState, componentScope, simpleRowValue/*not key/value pairs in each row*/, dumbWatchMarkers) {
		var i;
		for (i = viewPort.length - 1; i >= 0; i--) {
			addDataWatchesToRow(i, viewPort, internalState, componentScope, simpleRowValue, dumbWatchMarkers);
		}
	};

	function removeDataWatchesFromRow(idx, internalState) {
		if (internalState.unwatchData && internalState.unwatchData[idx]) {
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
	function removeRowConversionInfo(i, internalState) {
		if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(i)) {
			delete internalState[CONVERSIONS][i];
		}
	};

	function updateRowConversionInfo(idx, internalState, serverConversionInfo) {
		if (angular.isUndefined(internalState[CONVERSIONS])) {
			internalState[CONVERSIONS] = {};
		}
		internalState[CONVERSIONS][idx] = serverConversionInfo;
	};

	function updateAllConversionInfo(viewPort, internalState, serverConversionInfo) {
		internalState[CONVERSIONS] = {};
		var i;
		for (i = viewPort.length - 1; i >= 0; i--)
			updateRowConversionInfo(i, internalState, serverConversionInfo ? serverConversionInfo[i] : undefined);
	};

	function updateWholeViewport(viewPortHolder, viewPortPropertyName, internalState, viewPortUpdate, viewPortUpdateConversions, componentScope, componentModelGetter) {
		if (viewPortUpdateConversions) {
			// do the actual conversion
			viewPortUpdate = $sabloConverters.convertFromServerToClient(viewPortUpdate, viewPortUpdateConversions, viewPortHolder[viewPortPropertyName], componentScope, componentModelGetter);
		}
		viewPortHolder[viewPortPropertyName] = viewPortUpdate;
		// update conversion info
		updateAllConversionInfo(viewPortHolder[viewPortPropertyName], internalState, viewPortUpdateConversions);
	};

	function updateViewportGranularly(viewPort, internalState, rowUpdates, rowUpdateConversions, componentScope,
			componentModelGetter, simpleRowValue/*not key/value pairs in each row*/) {
		// partial row updates (remove/insert/update)

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
					var relIdx = j - rowUpdate.startIndex;

					// apply the conversions
					var rowConversionUpdate = (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[relIdx] : undefined;
					if (rowConversionUpdate) $sabloConverters.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], componentScope, componentModelGetter);
					// if the rowUpdate contains '_svyRowId' then we know it's the entire/complete row object
					if (simpleRowValue || rowUpdate.rows[relIdx][$foundsetTypeConstants.ROW_ID_COL_KEY]) {
						viewPort[j] = rowUpdate.rows[relIdx];

						if (rowConversionUpdate) {
							// update conversion info
							if (angular.isUndefined(internalState[CONVERSIONS])) {
								internalState[CONVERSIONS] = {};
							}
							internalState[CONVERSIONS][j] = rowConversionUpdate;
						} else if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(internalState[CONVERSIONS][j]))
							delete internalState[CONVERSIONS][j];
					} else {
						// key/value pairs in each row
						// this might be a partial update (so only a column changed for example) - don't drop all other columns, just update the ones we received
						for (dpName in rowUpdate.rows[relIdx]) {
							// update value
							viewPort[j][dpName] = rowUpdate.rows[relIdx][dpName];

							if (rowConversionUpdate) {
								// update conversion info
								if (angular.isUndefined(internalState[CONVERSIONS])) {
									internalState[CONVERSIONS] = {};
								}
								if (angular.isUndefined(internalState[CONVERSIONS][j]))
								{
									internalState[CONVERSIONS][j] = {};
								}
								internalState[CONVERSIONS][j][dpName] = rowConversionUpdate[dpName];
							} else if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(internalState[CONVERSIONS][j])
									&& angular.isDefined(internalState[CONVERSIONS][j][dpName])) delete internalState[CONVERSIONS][j][dpName];
						}
					}
				}
			} else if (rowUpdate.type == INSERT) {
				if (rowUpdateConversions && rowUpdateConversions[i]) $sabloConverters.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, componentScope, componentModelGetter);

				for (j = rowUpdate.rows.length - 1; j >= 0 ; j--) {
					viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
					updateRowConversionInfo(rowUpdate.startIndex, internalState, (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[j] : undefined);
				}
				// insert might have made obsolete some records in cache; remove those; for inserts
				// !!! rowUpdate.endIndex by convention means the new length of the viewport
				rowUpdate.removedFromVPEnd = viewPort.length - rowUpdate.endIndex; // prepare rowUpdate for listener notifications
				if (rowUpdate.removedFromVPEnd > 0) {
					// remove conversion info for these rows as well
					if (internalState[CONVERSIONS]) {
						for (j = rowUpdate.endIndex; j < viewPort.length; j++)
							removeRowConversionInfo(j, internalState);
					}

					viewPort.splice(rowUpdate.endIndex, rowUpdate.removedFromVPEnd);
				}
				rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate for listener notifications
			} else if (rowUpdate.type == DELETE) {
				if (rowUpdateConversions && rowUpdateConversions[i]) $sabloConverters.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, componentScope, componentModelGetter);

				var oldLength = viewPort.length;
				if (internalState[CONVERSIONS]) {
					// delete conversion info for deleted rows
					for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
						removeRowConversionInfo(j, internalState);
				}
				viewPort.splice(rowUpdate.startIndex, rowUpdate.endIndex - rowUpdate.startIndex + 1);
				for (j = 0; j < rowUpdate.rows.length; j++) {
					viewPort.push(rowUpdate.rows[j]);
					updateRowConversionInfo(viewPort.length - 1, internalState, (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[j] : undefined);
				}
				
				rowUpdate.appendedToVPEnd = rowUpdate.rows.length;
			}
			delete rowUpdate.rows; // prepare rowUpdate for listener notifications
		}
	};

	function updateAngularScope(viewPort, internalState, componentScope, simpleRowValue/*not key/value pairs in each row*/) {
		var i;
		for (i = viewPort.length - 1; i >= 0; i--) {
			var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][i] : undefined;
			if (conversionInfo) {
				if (simpleRowValue) {
					if (conversionInfo) $sabloConverters.updateAngularScope(viewPort[i], conversionInfo, componentScope);
				} else {
					var columnName;
					for (columnName in viewPort[i]) {
						if (columnName !== $foundsetTypeConstants.ROW_ID_COL_KEY) $sabloConverters.updateAngularScope(viewPort[i][columnName], conversionInfo[columnName], componentScope);
					}
				}
			}
		}
	};

	return {
		updateWholeViewport: updateWholeViewport,
		updateViewportGranularly: updateViewportGranularly,

		addDataWatchesToRows: addDataWatchesToRows,
		removeDataWatchesFromRows: removeDataWatchesFromRows,
		updateAllConversionInfo: updateAllConversionInfo,
		// this will not remove/add viewport watches (use removeDataWatchesFromRows/addDataWatchesToRows for that) - it will just forward 'updateAngularScope' to viewport values that need it
		updateAngularScope: updateAngularScope
	};

});
