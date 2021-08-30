/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/servoy/component.d.ts" />
/// <reference path="../../../typings/servoy/foundset.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />

angular.module('foundset_custom_property', ['webSocketModule'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
	// if you change any of these please also update ChangeEvent and other types in foundset.d.ts and or component.d.ts
	ROW_ID_COL_KEY: '_svyRowId',
	FOR_FOUNDSET_PROPERTY: 'forFoundset',
	
	// listener notification constants follow; prefixed just to separate them a bit from other constants
	NOTIFY_REQUEST_INFOS: "requestInfos",
	NOTIFY_FULL_VALUE_CHANGED: "fullValueChanged",
	NOTIFY_SERVER_SIZE_CHANGED: "serverFoundsetSizeChanged",
	NOTIFY_HAS_MORE_ROWS_CHANGED: "hasMoreRowsChanged",
	NOTIFY_MULTI_SELECT_CHANGED: "multiSelectChanged",
	NOTIFY_COLUMN_FORMATS_CHANGED: "columnFormatsChanged",
	NOTIFY_SORT_COLUMNS_CHANGED: "sortColumnsChanged",
	NOTIFY_SELECTED_ROW_INDEXES_CHANGED: "selectedRowIndexesChanged",
	NOTIFY_USER_SET_SELECTION: "userSetSelection",
	NOTIFY_VIEW_PORT_START_INDEX_CHANGED: "viewPortStartIndexChanged",
	NOTIFY_VIEW_PORT_SIZE_CHANGED: "viewPortSizeChanged",
	NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED: "viewportRowsCompletelyChanged",
	NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED: "viewportRowsUpdated",
	NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE: "oldViewportSize", // deprecated since 8.4 where granular updates are pre-processed server side and can be applied directed on client - making this not needed
	NOTIFY_VIEW_PORT_ROW_UPDATES: "updates",
	
	// row update types for listener notifications - in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
	ROWS_CHANGED: 0,
    ROWS_INSERTED: 1,
    ROWS_DELETED: 2
})
.factory("$foundsetTypeUtils", ["$foundsetTypeConstants", function($foundsetTypeConstants: foundsetType.FoundsetTypeConstants) {
	function isChange(update: componentType.ViewportRowUpdate): update is componentType.RowsChanged {
	    return (<componentType.RowsChanged>update).type == $foundsetTypeConstants.ROWS_CHANGED;
	};
	function isInsert(update: componentType.ViewportRowUpdate): update is componentType.RowsInserted {
	    return (<componentType.RowsInserted>update).type == $foundsetTypeConstants.ROWS_INSERTED;
	};
	function isDelete(update: componentType.ViewportRowUpdate): update is componentType.RowsDeleted {
	    return (<componentType.RowsDeleted>update).type == $foundsetTypeConstants.ROWS_DELETED;
	};
	
	return {

		/**
		 * NOTE: Starting with Servoy 8.4 you no longer need to use this method; see @deprecated
		 * comment.
		 * 
		 * The purpose of this method is to aggregate after-the-fact granular updates with indexes
		 * that are relevant only when applying updates 1-by-1 into indexes that are
		 * related to the new/final state of the viewport. It only calculates new indexes
		 * for updates of type $foundsetTypeConstants.ROWS_CHANGED. (taking into account
		 * any insert/delete along the way)
		 * 
		 * @param viewportRowUpdates what a foundset/component property type (viewport) change listener
		 * would receive in changeEvent[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED]
		 * [$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES]
		 * 
		 * @param oldViewportSize what a foundset/component property type (viewport) change listener
		 * would receive in changeEvent[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED]
		 * [$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE]
		 * 
		 * @deprecated starting with 8.4 this is no longer needed as foundset/component/foundsetlinked
		 * property change listeners guarantee that the rows in inserts and updates have their indexes
		 * relative to the already changed viewport (data in the viewport at those indexes at the
		 * moment these listeners trigger does match correctly). So basically calling this method would
		 * not alter any update operations - they would remain the same.
		 * 
		 * @returns an array of $foundsetTypeConstants.ROWS_CHANGED updates with their indexes corrected
		 * to reflect the indexes in the final state of the viewport (after all updates were applied).
		 */
		coalesceGranularRowChanges: function(viewportRowUpdates: componentType.ViewportRowUpdates, oldViewportSize: number): componentType.RowsChanged[] {
			const coalescedUpdates: componentType.RowsChanged[] = [];
			let currentViewportSize = oldViewportSize; 
			for (let i = 0; i < viewportRowUpdates.length; i++) {
				let update = viewportRowUpdates[i];
				if (isChange(update)) {
					coalescedUpdates.push({ type: update.type, startIndex: update.startIndex, endIndex: update.endIndex });
				} else if (isInsert(update)) {
					let added = (update.endIndex - update.startIndex + 1);
					for (let j = 0; j < coalescedUpdates.length; j++) {
						let change = coalescedUpdates[j];
						if (change.startIndex >= update.startIndex) {
							// change is shifted right
							change.startIndex += added;
							change.endIndex += added;
							
							let removedFromEndOfChange = change.endIndex + 1 - (currentViewportSize - update.removedFromVPEnd + added);
							if (removedFromEndOfChange > 0) {
								change.endIndex -= removedFromEndOfChange;
							}
							
							// see if the whole change slided out of viewport after this insert
							if (change.startIndex > change.endIndex) coalescedUpdates.splice(j--, 1);
						} else if (change.endIndex >= update.startIndex) {
							// change is split in two
							coalescedUpdates.splice(j, 0, { type: change.type,
								startIndex: change.startIndex, endIndex: update.startIndex - 1});
							change.startIndex = update.startIndex; // due to splice above that adds one element at current j, next
							// loop exec will handle this same (remaining 2nd part of split) change to shift it to right as needed...
						}
					}
					currentViewportSize += added - update.removedFromVPEnd;
				} else if (isDelete(update)) {
					let deleted = (update.endIndex - update.startIndex + 1);
					for (let j = 0; j < coalescedUpdates.length; j++) {
						let change = coalescedUpdates[j];
						let intersectionStart = Math.max(change.startIndex, update.startIndex);
						let intersectionEnd = Math.min(change.endIndex, update.endIndex);
						if (intersectionStart <= intersectionEnd) {
							// some of the changed rows were deleted
							change.endIndex -= intersectionEnd - update.startIndex + 1;
							if (change.startIndex == intersectionStart) {
								change.startIndex = update.startIndex;
							}
							// see if whole change was deleted
							if (change.startIndex > change.endIndex) coalescedUpdates.splice(j--, 1);
						} else if (change.startIndex > update.startIndex) {
							// none of the changes were deleted, but their indexes must shift left
							let shiftToLeft = update.endIndex - update.startIndex + 1;
							change.startIndex -= shiftToLeft;
							change.endIndex -= shiftToLeft;
						}
					}
					currentViewportSize += update.appendedToVPEnd - deleted;
				}
			}
			return coalescedUpdates;
		}
	}
}])
.run(function ($sabloConverters, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants, $viewportModule, $sabloUtils, $q, $log, $webSocket, $sabloDeferHelper: sablo.ISabloDeferHelper) {
	const UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them

	const SERVER_SIZE = "serverSize";
	const FOUNDSET_ID = "foundsetId";
	const SORT_COLUMNS = "sortColumns";
	const SELECTED_ROW_INDEXES = "selectedRowIndexes";
	const USER_SET_SELECTION = "userSetSelection";
	const MULTI_SELECT = "multiSelect";
	const HAS_MORE_ROWS = "hasMoreRows";
	const VIEW_PORT = "viewPort";
	const START_INDEX = "startIndex";
	const SIZE = "size";
	const ROWS = "rows";
	const COLUMN_FORMATS = "columnFormats";
	const HANDLED_CLIENT_REQUESTS = "handledClientReqIds";
	const ID_KEY = "id";
	const VALUE_KEY = "value";
	const DATAPROVIDER_KEY = "dp";
	const CONVERSIONS = "viewportConversions"; // data conversion info

	const PUSH_TO_SERVER = "w";

	const NO_OP = "n";

	function removeAllWatches(value) {
		if (value != null && angular.isDefined(value)) {
			const iS = value[$sabloConverters.INTERNAL_IMPL];
			if (iS.unwatchSelection) {
				iS.unwatchSelection();
				delete iS.unwatchSelection;
			}
			if (value[VIEW_PORT][ROWS]) $viewportModule.removeDataWatchesFromRows(value[VIEW_PORT][ROWS].length, iS);
		}
	};

	function addBackWatches(value, componentScope) {
		if (angular.isDefined(value) && value !== null) {
			const internalState = value[$sabloConverters.INTERNAL_IMPL];
			if (value[VIEW_PORT][ROWS]) {
				// prepare pushToServer values as needed for the following call
				let pushToServerValues;
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
		fromServerToClient: function (serverJSONValue, currentClientValue, componentScope, propertyContext) {
			let newValue = currentClientValue;

			// see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
			const hasListeners = (currentClientValue && currentClientValue[$sabloConverters.INTERNAL_IMPL].changeListeners.length > 0);
			const notificationParamForListeners = hasListeners ? { } : undefined;
            let requestInfos: any[]; // these will end up in notificationParamForListeners but only if there is another change that is triggered; otherwise they should not trigger the listener just by themselves
			
			// remove watches so that this update won't trigger them
			removeAllWatches(currentClientValue);

			// see if this is an update or whole value and handle it
			if (!serverJSONValue) {
				newValue = serverJSONValue; // set it to nothing
				if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : currentClientValue, newValue : serverJSONValue };
				const oldInternalState = currentClientValue ? currentClientValue[$sabloConverters.INTERNAL_IMPL] : undefined; // internal state / $sabloConverters interface
				if (oldInternalState) $sabloDeferHelper.cancelAll(oldInternalState);

			} else {
				// check for updates
				let updates = false;
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SERVER_SIZE])) {
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_SERVER_SIZE_CHANGED] = { oldValue : currentClientValue[SERVER_SIZE], newValue : serverJSONValue[UPDATE_PREFIX + SERVER_SIZE] };
					currentClientValue[SERVER_SIZE] = serverJSONValue[UPDATE_PREFIX + SERVER_SIZE]; // currentClientValue should always be defined in this case
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + PUSH_TO_SERVER])) {
					const internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					internalState[PUSH_TO_SERVER] = serverJSONValue[UPDATE_PREFIX + PUSH_TO_SERVER];
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + HAS_MORE_ROWS])) {
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_HAS_MORE_ROWS_CHANGED] = { oldValue : currentClientValue[HAS_MORE_ROWS], newValue : serverJSONValue[UPDATE_PREFIX + HAS_MORE_ROWS] };
					currentClientValue[HAS_MORE_ROWS] = serverJSONValue[UPDATE_PREFIX + HAS_MORE_ROWS];
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + MULTI_SELECT])) {
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_MULTI_SELECT_CHANGED] = { oldValue : currentClientValue[MULTI_SELECT], newValue : serverJSONValue[UPDATE_PREFIX + MULTI_SELECT] };
					currentClientValue[MULTI_SELECT] = serverJSONValue[UPDATE_PREFIX + MULTI_SELECT];
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + FOUNDSET_ID])) {
					currentClientValue[FOUNDSET_ID] = serverJSONValue[UPDATE_PREFIX + FOUNDSET_ID] ? serverJSONValue[UPDATE_PREFIX + FOUNDSET_ID] : undefined;
					updates = true;
				}
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + COLUMN_FORMATS])) {
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_COLUMN_FORMATS_CHANGED] = { oldValue : currentClientValue[COLUMN_FORMATS], newValue : serverJSONValue[UPDATE_PREFIX + COLUMN_FORMATS] };
					currentClientValue[COLUMN_FORMATS] = serverJSONValue[UPDATE_PREFIX + COLUMN_FORMATS];
					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SORT_COLUMNS])) {
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_SORT_COLUMNS_CHANGED] = { oldValue : currentClientValue[SORT_COLUMNS], newValue : serverJSONValue[UPDATE_PREFIX + SORT_COLUMNS] };
					currentClientValue[SORT_COLUMNS] = serverJSONValue[UPDATE_PREFIX + SORT_COLUMNS];
					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES])) {
					if (hasListeners) {
						notificationParamForListeners[$foundsetTypeConstants.NOTIFY_SELECTED_ROW_INDEXES_CHANGED] = { oldValue : currentClientValue[SELECTED_ROW_INDEXES], newValue : serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES] };
						if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + USER_SET_SELECTION])) {
							notificationParamForListeners[$foundsetTypeConstants.NOTIFY_USER_SET_SELECTION] = true;
						}
					}
					currentClientValue[SELECTED_ROW_INDEXES] = serverJSONValue[UPDATE_PREFIX + SELECTED_ROW_INDEXES];
					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[HANDLED_CLIENT_REQUESTS])) {
					const handledRequests = serverJSONValue[HANDLED_CLIENT_REQUESTS]; // array of { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
					const internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					
					handledRequests.forEach(function(handledReq) { 
						const defer = $sabloDeferHelper.retrieveDeferForHandling(handledReq[ID_KEY], internalState);
						if (defer) {
                            const promise = defer.promise as foundsetType.RequestInfoPromise<any>;
							if (hasListeners && promise.requestInfo) {
								if (!requestInfos) requestInfos = [];
								requestInfos.push(promise.requestInfo);
							}

							if (defer === internalState.selectionUpdateDefer) {
								if (handledReq[VALUE_KEY]) defer.resolve(currentClientValue[SELECTED_ROW_INDEXES]);
								else defer.reject(currentClientValue[SELECTED_ROW_INDEXES]);

								delete internalState.selectionUpdateDefer;
							} else {
								 if (handledReq[VALUE_KEY]) defer.resolve();
								 else defer.reject();
							 }
						}
					});

					updates = true;
				}
				
				if (angular.isDefined(serverJSONValue[UPDATE_PREFIX + VIEW_PORT])) {
					updates = true;
					const viewPortUpdate = serverJSONValue[UPDATE_PREFIX + VIEW_PORT];
					
					const internalState = currentClientValue[$sabloConverters.INTERNAL_IMPL];
					
					const oldSize = currentClientValue[VIEW_PORT][SIZE];

					if (angular.isDefined(viewPortUpdate[START_INDEX]) && currentClientValue[VIEW_PORT][START_INDEX] != viewPortUpdate[START_INDEX]) {
						if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_START_INDEX_CHANGED] = { oldValue : currentClientValue[VIEW_PORT][START_INDEX], newValue : viewPortUpdate[START_INDEX] };
						currentClientValue[VIEW_PORT][START_INDEX] = viewPortUpdate[START_INDEX];
					}
					if (angular.isDefined(viewPortUpdate[SIZE]) && currentClientValue[VIEW_PORT][SIZE] != viewPortUpdate[SIZE]) {
						if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_SIZE_CHANGED] = { oldValue : currentClientValue[VIEW_PORT][SIZE], newValue : viewPortUpdate[SIZE] };
						currentClientValue[VIEW_PORT][SIZE] = viewPortUpdate[SIZE];
					}
					if (angular.isDefined(viewPortUpdate[ROWS])) {
						const oldRows = currentClientValue[VIEW_PORT][ROWS];
						$viewportModule.updateWholeViewport(currentClientValue[VIEW_PORT], ROWS, internalState, viewPortUpdate[ROWS],
								viewPortUpdate[$sabloConverters.TYPES_KEY] && viewPortUpdate[$sabloConverters.TYPES_KEY][ROWS] ? viewPortUpdate[$sabloConverters.TYPES_KEY][ROWS] : undefined, componentScope, propertyContext);
						
						// new rows; set prototype for each row
						const rows = currentClientValue[VIEW_PORT][ROWS];
						for (let i = rows.length - 1; i >= 0; i--) {
							rows[i] = $sabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
						}
						
						if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue : oldRows, newValue : currentClientValue[VIEW_PORT][ROWS] };
					} else if (angular.isDefined(viewPortUpdate[UPDATE_PREFIX + ROWS])) {
						$viewportModule.updateViewportGranularly(currentClientValue[VIEW_PORT][ROWS], internalState, viewPortUpdate[UPDATE_PREFIX + ROWS], viewPortUpdate[$sabloConverters.TYPES_KEY] && viewPortUpdate[$sabloConverters.TYPES_KEY][UPDATE_PREFIX + ROWS] ? viewPortUpdate[$sabloConverters.TYPES_KEY][UPDATE_PREFIX + ROWS] : undefined, componentScope, propertyContext, false, internalState.rowPrototype);

						if (hasListeners) {
							notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED] = { updates : viewPortUpdate[UPDATE_PREFIX + ROWS] }; // viewPortUpdate[UPDATE_PREFIX + ROWS] was already prepared for listeners by $viewportModule.updateViewportGranularly
							notificationParamForListeners[$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED][$foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE] = oldSize; // deprecated since 8.4 where granular updates are pre-processed server side and can be applied directed on client - making this not needed
						}
					}
				}

				// if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
				if (!updates && !serverJSONValue[NO_OP]) {
					// not updates - so whole thing received
					let oldValueForListeners: any;
					let oldInternalState: any;
					if (currentClientValue) {
                        // reuse the reference so it's easier for components to keep the correct reference (SVY-14764)
                        newValue = currentClientValue; // just use the old value reference, do not create a new ref (easier for the components that use foundset properties)
                        
                        oldValueForListeners = {};
                        oldInternalState = {};
                        
                        // put all from old value (currentClientValue which becomes newValue as well) into oldValueForListeners and remove it from the newValue
                        for (let key of Object.keys(newValue)) {
                            oldValueForListeners[key] = newValue[key];
                            delete newValue[key];
                        }
                        
                        // ok now copy over everything from serverJSONValue to be processed later below
                        for (let key of Object.keys(serverJSONValue)) {
                            newValue[key] = serverJSONValue[key];
                        }
                                                
                        // internal state was not removed yet as it is a non-iterable prop; but we only need to keep from the old internal state the listeners and deferred;
                        // everyting else starts clean; listeners and deferrs will be copied over later the same as it would be done for new values
                        for (let key of Object.keys(newValue[$sabloConverters.INTERNAL_IMPL])) {
                            oldInternalState[key] = newValue[$sabloConverters.INTERNAL_IMPL][key];
                            delete newValue[$sabloConverters.INTERNAL_IMPL][key];
                        }
                    } else {
                        // no old value; create a fresh one
                        
    					let proto = { };
    					// conversion to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
    					proto[$sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC] = function() {
    						return this[FOUNDSET_ID];
    					};
    					
    					newValue = $sabloUtils.cloneWithDifferentPrototype(serverJSONValue, proto);
					    $sabloConverters.prepareInternalState(newValue);
					}
					
					if (hasListeners) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : oldValueForListeners, newValue : newValue };
						
					const internalState = newValue[$sabloConverters.INTERNAL_IMPL]; // internal state / $sabloConverters interface
					
					// conversion of rows to server in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
					internalState.rowPrototype = {};
					internalState.rowPrototype[$sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC] = function() {
						if (this[$foundsetTypeConstants.ROW_ID_COL_KEY])
						{
							const recordRef = {};
							recordRef[$foundsetTypeConstants.ROW_ID_COL_KEY] = this[$foundsetTypeConstants.ROW_ID_COL_KEY];
							recordRef[FOUNDSET_ID] = newValue[FOUNDSET_ID];
							return recordRef;
						}
						return null
					};
					const rows = newValue[VIEW_PORT][ROWS];
					if (typeof newValue[PUSH_TO_SERVER] !== 'undefined') {
						internalState[PUSH_TO_SERVER] = newValue[PUSH_TO_SERVER];
						delete newValue[PUSH_TO_SERVER];
					}

					internalState.requests = [];
					if (oldInternalState)
					{	
						$sabloDeferHelper.initInternalStateForDeferringFromOldInternalState(internalState, oldInternalState);
					}
					else
					{
						$sabloDeferHelper.initInternalStateForDeferring(internalState, "svy foundset * ");
					}	
					// convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
					$viewportModule.updateAllConversionInfo(rows, internalState, newValue[VIEW_PORT][$sabloConverters.TYPES_KEY] ? newValue[VIEW_PORT][$sabloConverters.TYPES_KEY][ROWS] : undefined);
					if (newValue[VIEW_PORT][$sabloConverters.TYPES_KEY]) {
						// relocate conversion info in internal state and convert
						$sabloConverters.convertFromServerToClient(rows, newValue[VIEW_PORT][$sabloConverters.TYPES_KEY][ROWS], componentScope, propertyContext);
						delete newValue[VIEW_PORT][$sabloConverters.TYPES_KEY];
					}
					// do set prototype after rows are converted
					for (let i = rows.length - 1; i >= 0; i--) {
						rows[i] = $sabloUtils.cloneWithDifferentPrototype(rows[i], internalState.rowPrototype);
					}

					// PUBLIC API to components; initialize the property value; make it 'smart'
					newValue.loadRecordsAsync = function(startIndex, size) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * loadRecordsAsync requested with (" + startIndex + ", " + size + ")");
						if (isNaN(startIndex) || isNaN(size)) throw new Error("loadRecordsAsync: start or size are not numbers (" + startIndex + "," + size + ")");

						const req = {newViewPort: {startIndex : startIndex, size : size}};
						const requestID = $sabloDeferHelper.getNewDeferId(internalState);
						req[ID_KEY] = requestID;
						internalState.requests.push(req);
						
						if (internalState.changeNotifier) internalState.changeNotifier();
						
						return internalState.deferred[requestID].defer.promise;
					};
					newValue.loadExtraRecordsAsync = function(negativeOrPositiveCount, dontNotifyYet) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * loadExtraRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")");
						if (isNaN(negativeOrPositiveCount)) throw new Error("loadExtraRecordsAsync: extrarecords is not a number (" + negativeOrPositiveCount + ")");

						const req = { loadExtraRecords: negativeOrPositiveCount };
						const requestID = $sabloDeferHelper.getNewDeferId(internalState);
						req[ID_KEY] = requestID;
						internalState.requests.push(req);
						
						if (internalState.changeNotifier && !dontNotifyYet) internalState.changeNotifier();
						
						return internalState.deferred[requestID].defer.promise;
					};
					newValue.loadLessRecordsAsync = function(negativeOrPositiveCount, dontNotifyYet) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * loadLessRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")");
						if (isNaN(negativeOrPositiveCount)) throw new Error("loadLessRecordsAsync: lessrecords is not a number (" + negativeOrPositiveCount + ")");

						const req = { loadLessRecords: negativeOrPositiveCount };
						const requestID = $sabloDeferHelper.getNewDeferId(internalState);
						req[ID_KEY] = requestID;
						internalState.requests.push(req);

						if (internalState.changeNotifier && !dontNotifyYet) internalState.changeNotifier();
						
						return internalState.deferred[requestID].defer.promise;
					};
					newValue.notifyChanged = function() {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * notifyChanged called");
						if (internalState.changeNotifier && internalState.requests.length > 0) internalState.changeNotifier();
					};
					newValue.sort = function(columns) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * sort requested with " + JSON.stringify(columns));
						const req = {sort: columns};
						const requestID = $sabloDeferHelper.getNewDeferId(internalState);
						req[ID_KEY] = requestID;
						internalState.requests.push(req);
						if (internalState.changeNotifier) internalState.changeNotifier();
						
						return internalState.deferred[requestID].defer.promise;
					}
					newValue.setPreferredViewportSize = function(size, sendSelectionViewportInitially, initialSelectionViewportCentered) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * setPreferredViewportSize called with (" + size + ", " + sendSelectionViewportInitially + ", " + initialSelectionViewportCentered + ")");
						if (isNaN(size)) throw new Error("setPreferredViewportSize(...): illegal argument; size is not a number (" + size + ")");
						const request = { "preferredViewportSize" : size };
						if (angular.isDefined(sendSelectionViewportInitially)) request["sendSelectionViewportInitially"] = !!sendSelectionViewportInitially;
						if (angular.isDefined(initialSelectionViewportCentered)) request["initialSelectionViewportCentered"] = !!initialSelectionViewportCentered;
						internalState.requests.push(request);
						if (internalState.changeNotifier) internalState.changeNotifier();
					}
					newValue.requestSelectionUpdate = function(tmpSelectedRowIdxs) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * requestSelectionUpdate called with " + JSON.stringify(tmpSelectedRowIdxs));
						if (internalState.selectionUpdateDefer) {
							internalState.selectionUpdateDefer.reject("Selection change defer cancelled because we are already sending another selection to server.");
						}
						delete internalState.selectionUpdateDefer;

						const msgId = $sabloDeferHelper.getNewDeferId(internalState);
						internalState.selectionUpdateDefer = internalState.deferred[msgId].defer;
						
						const req = {newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: msgId};
						req[ID_KEY] = msgId;
						internalState.requests.push(req);
						if (internalState.changeNotifier) internalState.changeNotifier();

						return internalState.selectionUpdateDefer.promise;
					}
					newValue.getRecordRefByRowID = function(rowID) {
						if (rowID)
						{
							const recordRef = {};
							recordRef[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
							recordRef[FOUNDSET_ID] = newValue[FOUNDSET_ID];
							return recordRef;
						}
						return null
					};
					newValue.updateViewportRecord = function(rowID, columnID, newValue, oldValue) {
						if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * updateRecord requested with (" + rowID + ", " + columnID + ", " + newValue);
						const r = {};
						r[$foundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
						r[DATAPROVIDER_KEY] = columnID;
						r[VALUE_KEY] = newValue;

						// convert new data if necessary
						let conversionInfo = undefined;
						if(internalState[CONVERSIONS]) {
							for(const idx in this.viewPort.rows) {
								if(this.viewPort.rows[idx][$foundsetTypeConstants.ROW_ID_COL_KEY] == rowID) {
									conversionInfo = internalState[CONVERSIONS][idx];
									break;
								}
							}
						}
						if (conversionInfo && conversionInfo[columnID]) r[VALUE_KEY] = $sabloConverters.convertFromClientToServer(r[VALUE_KEY], conversionInfo[columnID], oldValue);
						else r[VALUE_KEY] = $sabloUtils.convertClientObject(r[VALUE_KEY]);

						internalState.requests.push({viewportDataChanged: r});
						if (internalState.changeNotifier) internalState.changeNotifier();
					}
					// even if it's a completely new value, keep listeners and change notifier from old one if there is an old value
					internalState.changeListeners = (oldInternalState ? oldInternalState.changeListeners : []);
					if (oldInternalState?.changeNotifier) internalState.changeNotifier = oldInternalState.changeNotifier;
					
					/**
					 * Adds a change listener that will get triggered when server sends changes for this foundset.
					 * 
					 * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
					 * @param listener the listener to register.
					 */
					newValue.addChangeListener = function(listener: (change: foundsetType.ChangeEvent) => void) {
						internalState.changeListeners.push(listener);
						return () => newValue.removeChangeListener(listener);
					}
					newValue.removeChangeListener = function(listener) {
						const index = internalState.changeListeners.indexOf(listener);
						if (index > -1) {
							internalState.changeListeners.splice(index, 1);
						}
					}
					internalState.fireChanges = function(foundsetChanges: foundsetType.ChangeEvent) {
						for(let i = 0; i < internalState.changeListeners.length; i++) {
							$webSocket.setIMHDTScopeHintInternal(componentScope);
							internalState.changeListeners[i](foundsetChanges);
							$webSocket.setIMHDTScopeHintInternal(undefined);
						}
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
			
			if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * updates or value received from server; new viewport and server size (" + (newValue ? newValue[VIEW_PORT][START_INDEX] + ", " + newValue[VIEW_PORT][SIZE] + ", " + newValue[SERVER_SIZE] + ", " + JSON.stringify(newValue[SELECTED_ROW_INDEXES]) : newValue) + ")");
			if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
				if ($log.debugEnabled && $log.debugLevel === $log.SPAM) $log.debug("svy foundset * firing founset listener notifications...");

                if ($webSocket.getCurrentRequestInfo()) {
                    if (!requestInfos) requestInfos = [];
                    requestInfos.push($webSocket.getCurrentRequestInfo());
                }

                if (requestInfos) notificationParamForListeners[$foundsetTypeConstants.NOTIFY_REQUEST_INFOS] = requestInfos;

				// use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
				currentClientValue[$sabloConverters.INTERNAL_IMPL].fireChanges(notificationParamForListeners);
			}

			return newValue;
		},

		updateAngularScope: function(clientValue, componentScope) {
			removeAllWatches(clientValue);
			if (componentScope) addBackWatches(clientValue, componentScope);

			if (clientValue) {
				const internalState = clientValue[$sabloConverters.INTERNAL_IMPL];
				if (internalState) {
					$viewportModule.updateAngularScope(clientValue[VIEW_PORT][ROWS], internalState, componentScope, false);
				}
			}
		},

		fromClientToServer: function(newClientData, oldClientData) {
			if (newClientData) {
				const newDataInternalState = newClientData[$sabloConverters.INTERNAL_IMPL];
				if (newDataInternalState.isChanged()) {
					const tmp = newDataInternalState.requests;
					newDataInternalState.requests = [];
					return tmp;
				}
			}
			return [];
		}
	});
})
