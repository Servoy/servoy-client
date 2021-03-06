angular.module('foundset_viewport_module', ['webSocketModule'])
    //Viewport reuse code module -------------------------------------------
    .factory("$viewportModule", function ($sabloConverters, $foundsetTypeConstants, $sabloUtils) {
    var CONVERSIONS = "viewportConversions"; // data conversion info
    var ROW_ID_COL_KEY_PARTIAL_UPDATE = "_svyRowId_p"; // same as $foundsetTypeConstants.ROW_ID_COL_KEY but sent when the foundset property is sending just a partial update, but some of the columns that did changed are also pks so they do affect the pk hash
    var CHANGE = $foundsetTypeConstants.ROWS_CHANGED;
    var INSERT = $foundsetTypeConstants.ROWS_INSERTED;
    var DELETE = $foundsetTypeConstants.ROWS_DELETED;
    var DATAPROVIDER_KEY = "dp";
    var VALUE_KEY = "value";
    function addDataWatchToCell(columnName /*can be null*/, idx, viewPort, internalState, componentScope, dumbWatchType) {
        function queueChange(newData, oldData) {
            var r = {};
            if (angular.isDefined(internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY])) {
                r[$foundsetTypeConstants.ROW_ID_COL_KEY] = internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY]().viewPort.rows[idx][$foundsetTypeConstants.ROW_ID_COL_KEY];
            }
            else
                r[$foundsetTypeConstants.ROW_ID_COL_KEY] = viewPort[idx][$foundsetTypeConstants.ROW_ID_COL_KEY]; // if it doesn't have internalState[$foundsetTypeConstants.FOR_FOUNDSET_PROPERTY] then it's probably the foundset property's viewport directly which has those in the viewport
            r[DATAPROVIDER_KEY] = columnName;
            r[VALUE_KEY] = newData;
            // convert new data if necessary
            var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][idx] : undefined;
            if (conversionInfo && (!columnName || conversionInfo[columnName]))
                r[VALUE_KEY] = $sabloConverters.convertFromClientToServer(r[VALUE_KEY], columnName ? conversionInfo[columnName] : conversionInfo, oldData);
            else
                r[VALUE_KEY] = $sabloUtils.convertClientObject(r[VALUE_KEY]);
            internalState.requests.push({ viewportDataChanged: r });
            if (internalState.changeNotifier)
                internalState.changeNotifier();
        }
        function getCellValue() {
            return columnName == null ? viewPort[idx] : viewPort[idx][columnName];
        }
        ; // viewport row can be just a value or an object of key/value pairs
        if (componentScope) {
            if (getCellValue() && getCellValue()[$sabloConverters.INTERNAL_IMPL] && getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
                // smart property value
                // watch for change-by reference if needed
                if (typeof (dumbWatchType) !== 'undefined')
                    internalState.unwatchData[idx].push(componentScope.$watch(getCellValue, function (newData, oldData) {
                        if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
                            queueChange(newData, oldData);
                        }
                    }));
                // we don't care to check below for dumbWatchType because some types (see foundset) need to send internal protocol messages even if they are not watched/changeable on server
                getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(function () {
                    if (getCellValue()[$sabloConverters.INTERNAL_IMPL].isChanged())
                        queueChange(getCellValue(), getCellValue());
                });
            }
            else if (typeof (dumbWatchType) !== 'undefined') {
                // deep watch for change-by content / dumb value
                internalState.unwatchData[idx].push(componentScope.$watch(getCellValue, function (newData, oldData) {
                    if (newData !== oldData) { /* this doesn't seem to work correctly for 2 identical Date objects in Chrome when debugging; but it should */
                        var changed = false;
                        if (typeof newData == "object") {
                            var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][idx] : undefined;
                            if ($sabloUtils.isChanged(newData, oldData, conversionInfo ? (columnName ? conversionInfo[columnName] : conversionInfo) : undefined)) {
                                changed = true;
                            }
                        }
                        else {
                            changed = true;
                        }
                        if (changed)
                            queueChange(newData, oldData);
                    }
                }, dumbWatchType));
            }
        }
        else if (getCellValue() && getCellValue()[$sabloConverters.INTERNAL_IMPL] && getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier) {
            getCellValue()[$sabloConverters.INTERNAL_IMPL].setChangeNotifier(function () {
                if (getCellValue()[$sabloConverters.INTERNAL_IMPL].isChanged())
                    queueChange(getCellValue(), getCellValue());
            });
        }
    }
    ;
    // 1. dumbWatchMarkers when simpleRowValue === true means (undefined - no watch needed, true/false - deep/shallow watch)
    // 2. dumbWatchMarkers when simpleRowValue === false means (undefined - watch all cause there is no marker information,
    //                                                          { col1: true; col2: false } means watch type needed for each column and no watches for the ones not mentioned,
    //                                                          true/false directly means deep/shallow watch for all columns)
    function addDataWatchesToRow(idx, viewPort, internalState, componentScope, simpleRowValue /*not key/value pairs in each row*/, dumbWatchMarkers) {
        if (!angular.isDefined(internalState.unwatchData))
            internalState.unwatchData = {};
        internalState.unwatchData[idx] = [];
        if (simpleRowValue) {
            addDataWatchToCell(null, idx, viewPort, internalState, componentScope, dumbWatchMarkers);
        }
        else {
            var columnName;
            for (columnName in viewPort[idx]) {
                if (columnName !== $foundsetTypeConstants.ROW_ID_COL_KEY)
                    addDataWatchToCell(columnName, idx, viewPort, internalState, componentScope, (typeof dumbWatchMarkers === 'boolean' ? dumbWatchMarkers : (dumbWatchMarkers ? dumbWatchMarkers[columnName] : true)));
            }
        }
    }
    ;
    function addDataWatchesToRows(viewPort, internalState, componentScope, simpleRowValue /*not key/value pairs in each row*/, dumbWatchMarkers) {
        var i;
        for (i = viewPort.length - 1; i >= 0; i--) {
            addDataWatchesToRow(i, viewPort, internalState, componentScope, simpleRowValue, dumbWatchMarkers);
        }
    }
    ;
    function removeDataWatchesFromRow(idx, internalState) {
        if (internalState.unwatchData && internalState.unwatchData[idx]) {
            for (var j = internalState.unwatchData[idx].length - 1; j >= 0; j--)
                internalState.unwatchData[idx][j]();
            delete internalState.unwatchData[idx];
        }
    }
    ;
    function removeDataWatchesFromRows(rowCount, internalState) {
        var i;
        for (i = rowCount - 1; i >= 0; i--) {
            removeDataWatchesFromRow(i, internalState);
        }
    }
    ;
    function removeRowConversionInfo(i, internalState) {
        if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(i)) {
            delete internalState[CONVERSIONS][i];
        }
    }
    ;
    function updateRowConversionInfo(idx, internalState, serverConversionInfo) {
        if (angular.isUndefined(internalState[CONVERSIONS])) {
            internalState[CONVERSIONS] = {};
        }
        internalState[CONVERSIONS][idx] = serverConversionInfo;
    }
    ;
    function updateAllConversionInfo(viewPort, internalState, serverConversionInfo) {
        internalState[CONVERSIONS] = {};
        var i;
        for (i = viewPort.length - 1; i >= 0; i--)
            updateRowConversionInfo(i, internalState, serverConversionInfo ? serverConversionInfo[i] : undefined);
    }
    ;
    function updateWholeViewport(viewPortHolder, viewPortPropertyName, internalState, viewPortUpdate, viewPortUpdateConversions, componentScope, propertyContext) {
        if (viewPortUpdateConversions) {
            // do the actual conversion
            viewPortUpdate = $sabloConverters.convertFromServerToClient(viewPortUpdate, viewPortUpdateConversions, viewPortHolder[viewPortPropertyName], componentScope, propertyContext);
        }
        viewPortHolder[viewPortPropertyName] = viewPortUpdate;
        // update conversion info
        updateAllConversionInfo(viewPortHolder[viewPortPropertyName], internalState, viewPortUpdateConversions);
    }
    ;
    function updateViewportGranularly(viewPort, internalState, rowUpdates, rowUpdateConversions, componentScope, propertyContext, simpleRowValue /*not key/value pairs in each row*/, rowPrototype) {
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
                    if (rowConversionUpdate)
                        rowUpdate.rows[relIdx] = $sabloConverters.convertFromServerToClient(rowUpdate.rows[relIdx], rowConversionUpdate, viewPort[j], componentScope, propertyContext);
                    // if the rowUpdate contains '_svyRowId' then we know it's the entire/complete row object
                    if (simpleRowValue || rowUpdate.rows[relIdx][$foundsetTypeConstants.ROW_ID_COL_KEY]) {
                        viewPort[j] = rowUpdate.rows[relIdx];
                        if (rowPrototype)
                            viewPort[j] = $sabloUtils.cloneWithDifferentPrototype(viewPort[j], rowPrototype);
                        if (rowConversionUpdate) {
                            // update conversion info
                            if (angular.isUndefined(internalState[CONVERSIONS])) {
                                internalState[CONVERSIONS] = {};
                            }
                            internalState[CONVERSIONS][j] = rowConversionUpdate;
                        }
                        else if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(internalState[CONVERSIONS][j]))
                            delete internalState[CONVERSIONS][j];
                    }
                    else {
                        // key/value pairs in each row
                        // this might be a partial update (so only a column changed for example) - don't drop all other columns, just update the ones we received
                        for (dpName in rowUpdate.rows[relIdx]) {
                            // update value
                            viewPort[j][dpName === ROW_ID_COL_KEY_PARTIAL_UPDATE ? $foundsetTypeConstants.ROW_ID_COL_KEY : dpName] = rowUpdate.rows[relIdx][dpName];
                            if (rowConversionUpdate) {
                                // update conversion info
                                if (angular.isUndefined(internalState[CONVERSIONS])) {
                                    internalState[CONVERSIONS] = {};
                                }
                                if (angular.isUndefined(internalState[CONVERSIONS][j])) {
                                    internalState[CONVERSIONS][j] = {};
                                }
                                internalState[CONVERSIONS][j][dpName] = rowConversionUpdate[dpName];
                            }
                            else if (angular.isDefined(internalState[CONVERSIONS]) && angular.isDefined(internalState[CONVERSIONS][j])
                                && angular.isDefined(internalState[CONVERSIONS][j][dpName]))
                                delete internalState[CONVERSIONS][j][dpName];
                        }
                    }
                }
            }
            else if (rowUpdate.type == INSERT) {
                var numberOfInsertedRows = rowUpdate.rows.length;
                var oldLength = viewPort.length;
                // apply conversions
                if (rowUpdateConversions && rowUpdateConversions[i])
                    rowUpdate = $sabloConverters.convertFromServerToClient(rowUpdate, rowUpdateConversions[i], undefined, componentScope, propertyContext);
                // shift conversion info after insert to the right
                if (internalState[CONVERSIONS]) {
                    for (j = oldLength - 1; j >= rowUpdate.startIndex; j--) {
                        internalState[CONVERSIONS][j + numberOfInsertedRows] = internalState[CONVERSIONS][j];
                        delete internalState[CONVERSIONS][j];
                    }
                }
                for (j = numberOfInsertedRows - 1; j >= 0; j--) {
                    if (rowPrototype)
                        rowUpdate.rows[j] = $sabloUtils.cloneWithDifferentPrototype(rowUpdate.rows[j], rowPrototype);
                    updateRowConversionInfo(rowUpdate.startIndex + j, internalState, (rowUpdateConversions && rowUpdateConversions[i] && rowUpdateConversions[i].rows) ? rowUpdateConversions[i].rows[j] : undefined);
                    viewPort.splice(rowUpdate.startIndex, 0, rowUpdate.rows[j]);
                }
                rowUpdate.removedFromVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'removedFromVPEnd' is deprecated and always 0 as server-side code will add a separate delete operation as necessary
                rowUpdate.endIndex = rowUpdate.startIndex + rowUpdate.rows.length - 1; // prepare rowUpdate.endIndex for listener notifications
            }
            else if (rowUpdate.type == DELETE) {
                var oldLength = viewPort.length;
                var numberOfDeletedRows = rowUpdate.endIndex - rowUpdate.startIndex + 1;
                if (internalState[CONVERSIONS]) {
                    // delete conversion info for deleted rows and shift left what is after deletion
                    for (j = rowUpdate.startIndex; j <= rowUpdate.endIndex; j++)
                        removeRowConversionInfo(j, internalState);
                    for (j = rowUpdate.endIndex + 1; j < oldLength; j++) {
                        internalState[CONVERSIONS][j - numberOfDeletedRows] = internalState[CONVERSIONS][j];
                        delete internalState[CONVERSIONS][j];
                    }
                }
                viewPort.splice(rowUpdate.startIndex, numberOfDeletedRows);
                rowUpdate.appendedToVPEnd = 0; // prepare rowUpdate for listener notifications; starting with Servoy 8.4 'appendedToVPEnd' is deprecated and always 0 as server-side code will add a separate insert operation as necessary
            }
            delete rowUpdate.rows; // prepare rowUpdate for listener notifications
        }
    }
    ;
    function updateAngularScope(viewPort, internalState, componentScope, simpleRowValue /*not key/value pairs in each row*/) {
        var i;
        for (i = viewPort.length - 1; i >= 0; i--) {
            var conversionInfo = internalState[CONVERSIONS] ? internalState[CONVERSIONS][i] : undefined;
            if (conversionInfo) {
                if (simpleRowValue) {
                    if (conversionInfo)
                        $sabloConverters.updateAngularScope(viewPort[i], conversionInfo, componentScope);
                }
                else {
                    var columnName;
                    for (columnName in viewPort[i]) {
                        if (columnName !== $foundsetTypeConstants.ROW_ID_COL_KEY)
                            $sabloConverters.updateAngularScope(viewPort[i][columnName], conversionInfo[columnName], componentScope);
                    }
                }
            }
        }
    }
    ;
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
