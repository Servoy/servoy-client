/// <reference path="../../../typings/angularjs/angular.d.ts" />
/// <reference path="../../../typings/servoy/foundset.d.ts" />
/// <reference path="../../../typings/sablo/sablo.d.ts" />
/// <reference path="../foundset_viewport_module/viewport.ts" />

angular.module('foundset_custom_property', ['webSocketModule', 'foundset_viewport_module'])
// Foundset type -------------------------------------------
.value("$foundsetTypeConstants", {
    // if you change any of these please also update ChangeEvent and other types in foundset.d.ts and or component.d.ts
    ROW_ID_COL_KEY: '_svyRowId', // if you change this you have to also change some members declared in typescript classes with this name e.g. RowValue
    FOR_FOUNDSET_PROPERTY: 'forFoundset',  // if you change this value you MUST change it also in some typescript classes that used it directly as a member in their internal state declaration
    
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
    NOTIFY_FOUNDSET_DEFINITION_CHANGE: "foundsetDefinitionChanged",

    // row update types for listener notifications - in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
    ROWS_CHANGED: 0,
    ROWS_INSERTED: 1,
    ROWS_DELETED: 2
} as foundsetType.FoundsetTypeConstants)
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
.run(function($sabloConverters: sablo.ISabloConverters, $foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
        $viewportModule: ngclient.propertyTypes.ViewportService, $log: sablo.ILogService,
        $webSocket: sablo.IWebSocket, $sabloDeferHelper: sablo.ISabloDeferHelper, $typesRegistry: sablo.ITypesRegistry) {

    $typesRegistry.registerGlobalType('foundset', new ngclient.propertyTypes.FoundsetType($sabloConverters, $foundsetTypeConstants,
            $viewportModule, $log, $webSocket, $sabloDeferHelper), false);
});

namespace ngclient.propertyTypes {

    export class FoundsetType implements sablo.IType<FoundsetValue> {
        static readonly UPDATE_PREFIX = "upd_"; // prefixes keys when only partial updates are send for them

        static readonly SERVER_SIZE = "serverSize";
        static readonly FOUNDSET_ID = "foundsetId";
        static readonly FOUNDSET_DEFINITION_CHANGE = "foundsetDefinition";
        static readonly SORT_COLUMNS = "sortColumns";
        static readonly SELECTED_ROW_INDEXES = "selectedRowIndexes";
        static readonly USER_SET_SELECTION = "userSetSelection";
        static readonly MULTI_SELECT = "multiSelect";
        static readonly HAS_MORE_ROWS = "hasMoreRows";
        static readonly VIEW_PORT = "viewPort";
        static readonly START_INDEX = "startIndex";
        static readonly SIZE = "size";
        static readonly ROWS = "rows";
        static readonly COLUMN_FORMATS = "columnFormats";
        static readonly HANDLED_CLIENT_REQUESTS = "handledClientReqIds";
        static readonly ID_KEY = "id";
        static readonly VALUE_KEY = "value";
        static readonly DATAPROVIDER_KEY = "dp";

        static readonly NO_OP = "n";
        
        constructor(private readonly sabloConverters: sablo.ISabloConverters, private readonly foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
                private readonly viewportModule: ngclient.propertyTypes.ViewportService,
                private readonly log: sablo.ILogService, private readonly webSocket: sablo.IWebSocket, private readonly sabloDeferHelper: sablo.ISabloDeferHelper) {}

        private removeAllWatches(value: FoundsetValue) {
            if (value != null && angular.isDefined(value)) {
                const iS: FoundsetTypeInternalState = value[this.sabloConverters.INTERNAL_IMPL];
                if (iS.unwatchSelection) {
                    iS.unwatchSelection();
                    delete iS.unwatchSelection;
                }
                if (value[FoundsetType.VIEW_PORT][FoundsetType.ROWS]) this.viewportModule.removeDataWatchesFromRows(iS, value[FoundsetType.VIEW_PORT][FoundsetType.ROWS], false);
            }
        }

        private addBackWatches(value: FoundsetValue, componentScope: angular.IScope) {
            if (angular.isDefined(value) && value !== null) {
                const internalState: FoundsetTypeInternalState = value[this.sabloConverters.INTERNAL_IMPL];
                if (value[FoundsetType.VIEW_PORT][FoundsetType.ROWS]) {
                    this.viewportModule.addDataWatchesToRows(value[FoundsetType.VIEW_PORT][FoundsetType.ROWS], internalState, componentScope, internalState.propertyContextCreator, false); // shouldn't need component model getter - takes rowids directly from viewport
                }
                if (componentScope) internalState.unwatchSelection = componentScope.$watchCollection(function() { return value[FoundsetType.SELECTED_ROW_INDEXES]; }, function(newSel, oldSel) {
                    componentScope.$evalAsync(function() {
                        if (newSel !== oldSel) {
                            internalState.requests.push({newClientSelection: newSel});
                            if (internalState.changeNotifier) internalState.changeNotifier();
                        }
                    });
                });
            }
        }

        public fromServerToClient(serverJSONValue: any, currentClientValue: FoundsetValue, componentScope: angular.IScope, propertyContext: sablo.IPropertyContext): FoundsetValue {
            let newValue:FoundsetValue = currentClientValue;

            // see if someone is listening for changes on current value; if so, prepare to fire changes at the end of this method
            const hasListeners = (currentClientValue && (currentClientValue[this.sabloConverters.INTERNAL_IMPL] as FoundsetTypeInternalState).changeListeners.length > 0);
            const notificationParamForListeners: foundsetType.ChangeEvent = hasListeners ? { } : undefined;
            let requestInfos: any[]; // these will end up in notificationParamForListeners but only if there is another change that is triggered; otherwise they should not trigger the listener just by themselves
            
            // remove watches so that this update won't trigger them
            this.removeAllWatches(currentClientValue);

            // see if this is an update or whole value and handle it
            if (!serverJSONValue) {
                newValue = serverJSONValue; // set it to nothing
                if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : currentClientValue, newValue : serverJSONValue };
                const oldInternalState: FoundsetTypeInternalState = currentClientValue ? currentClientValue[this.sabloConverters.INTERNAL_IMPL] : undefined; // internal state / this.sabloConverters interface
                if (oldInternalState) this.sabloDeferHelper.cancelAll(oldInternalState);
            } else {
                // check for updates
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SERVER_SIZE])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_SERVER_SIZE_CHANGED] = { oldValue : currentClientValue[FoundsetType.SERVER_SIZE], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SERVER_SIZE] };
                    currentClientValue[FoundsetType.SERVER_SIZE] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SERVER_SIZE]; // currentClientValue should always be defined in this case
                }
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.FOUNDSET_DEFINITION_CHANGE])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_FOUNDSET_DEFINITION_CHANGE] = true;
                }
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.HAS_MORE_ROWS])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_HAS_MORE_ROWS_CHANGED] = { oldValue : currentClientValue[FoundsetType.HAS_MORE_ROWS], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.HAS_MORE_ROWS] };
                    currentClientValue[FoundsetType.HAS_MORE_ROWS] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.HAS_MORE_ROWS];
                }
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.MULTI_SELECT])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_MULTI_SELECT_CHANGED] = { oldValue : currentClientValue[FoundsetType.MULTI_SELECT], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.MULTI_SELECT] };
                    currentClientValue[FoundsetType.MULTI_SELECT] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.MULTI_SELECT];
                }
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.FOUNDSET_ID])) {
                    currentClientValue[FoundsetType.FOUNDSET_ID] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.FOUNDSET_ID] ? serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.FOUNDSET_ID] : undefined;
                }
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.COLUMN_FORMATS])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_COLUMN_FORMATS_CHANGED] = { oldValue : currentClientValue[FoundsetType.COLUMN_FORMATS], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.COLUMN_FORMATS] };
                    currentClientValue[FoundsetType.COLUMN_FORMATS] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.COLUMN_FORMATS];
                }
                
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SORT_COLUMNS])) {
                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_SORT_COLUMNS_CHANGED] = { oldValue : currentClientValue[FoundsetType.SORT_COLUMNS], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SORT_COLUMNS] };
                    currentClientValue[FoundsetType.SORT_COLUMNS] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SORT_COLUMNS];
                }
                
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SELECTED_ROW_INDEXES])) {
                    if (hasListeners) {
                        notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_SELECTED_ROW_INDEXES_CHANGED] = { oldValue : currentClientValue[FoundsetType.SELECTED_ROW_INDEXES], newValue : serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SELECTED_ROW_INDEXES] };
                        if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.USER_SET_SELECTION])) {
                            notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_USER_SET_SELECTION] = true;
                        }
                    }
                    currentClientValue[FoundsetType.SELECTED_ROW_INDEXES] = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.SELECTED_ROW_INDEXES];
                }
                
                if (angular.isDefined(serverJSONValue[FoundsetType.HANDLED_CLIENT_REQUESTS])) {
                    const handledRequests = serverJSONValue[FoundsetType.HANDLED_CLIENT_REQUESTS]; // array of { id: ...int..., value: ...boolean... } which says if a req. was handled successfully by server or not
                    delete serverJSONValue[FoundsetType.HANDLED_CLIENT_REQUESTS]; // make sure it does not end up in the actual value if this is a full value update
                    const internalState: FoundsetTypeInternalState = currentClientValue[this.sabloConverters.INTERNAL_IMPL];
                    
                    handledRequests.forEach((handledReq: { id: number, value: any }) => { 
                        const defer = this.sabloDeferHelper.retrieveDeferForHandling(handledReq[FoundsetType.ID_KEY], internalState);
                        if (defer) {
                            const promise = defer.promise as foundsetType.RequestInfoPromise<any>;
                            if (hasListeners && promise.requestInfo) {
                                if (!requestInfos) requestInfos = [];
                                requestInfos.push(promise.requestInfo);
                            }

                            if (defer === internalState.selectionUpdateDefer) {
                                if (handledReq[FoundsetType.VALUE_KEY]) defer.resolve(currentClientValue[FoundsetType.SELECTED_ROW_INDEXES]);
                                else defer.reject(currentClientValue[FoundsetType.SELECTED_ROW_INDEXES]);
                                 
                                delete internalState.selectionUpdateDefer;
                            } else {
                                if (handledReq[FoundsetType.VALUE_KEY]/* boolean */) defer.resolve();
                                else defer.reject();
                            }
                        }
                    });
                }
                
                if (angular.isDefined(serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.VIEW_PORT])) {
                    const viewPortUpdate = serverJSONValue[FoundsetType.UPDATE_PREFIX + FoundsetType.VIEW_PORT];

                    const internalState: FoundsetTypeInternalState = currentClientValue[this.sabloConverters.INTERNAL_IMPL];
                    const oldSize = currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.SIZE];

                    if (angular.isDefined(viewPortUpdate[FoundsetType.START_INDEX]) && currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.START_INDEX] != viewPortUpdate[FoundsetType.START_INDEX]) {
                        if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_START_INDEX_CHANGED] = { oldValue : currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.START_INDEX], newValue : viewPortUpdate[FoundsetType.START_INDEX] };
                        currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.START_INDEX] = viewPortUpdate[FoundsetType.START_INDEX];
                    }
                    if (angular.isDefined(viewPortUpdate[FoundsetType.SIZE]) && currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.SIZE] != viewPortUpdate[FoundsetType.SIZE]) {
                        if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_SIZE_CHANGED] = { oldValue : currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.SIZE], newValue : viewPortUpdate[FoundsetType.SIZE] };
                        currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.SIZE] = viewPortUpdate[FoundsetType.SIZE];
                    }
                    if (angular.isDefined(viewPortUpdate[FoundsetType.ROWS])) {
                        const oldRows = currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS].slice(); // create shallow copy of old rows as ref. will be the same otherwise
                        currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS] = this.viewportModule.updateWholeViewport(currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS], internalState, viewPortUpdate[FoundsetType.ROWS],
                                viewPortUpdate[this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY], undefined, componentScope, internalState.propertyContextCreator, false);

                        // new rows; make them RowValue instances - so that record ref type client-to-server conversion can work
                        const rows = currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS];
                        for (let i = rows.length - 1; i >= 0; i--) {
                            rows[i] = new RowValue(rows[i], newValue);
                        }

                        if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED] = { oldValue : oldRows, newValue : currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS] };
                    } else if (angular.isDefined(viewPortUpdate[FoundsetType.UPDATE_PREFIX + FoundsetType.ROWS])) {
                        this.viewportModule.updateViewportGranularly(currentClientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS], internalState, viewPortUpdate[FoundsetType.UPDATE_PREFIX + FoundsetType.ROWS], undefined, componentScope, internalState.propertyContextCreator, false,
                                rawRowData => new RowValue(rawRowData, newValue));

                        if (hasListeners) {
                            notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED] = { updates : viewPortUpdate[FoundsetType.UPDATE_PREFIX + FoundsetType.ROWS] }; // viewPortUpdate[UPDATE_PREFIX + ROWS] was already prepared for listeners by this.viewportModule.updateViewportGranularly
                            notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED][this.foundsetTypeConstants.NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE] = oldSize; // deprecated since 8.4 where granular updates are pre-processed server side and can be applied directed on client - making this not needed
                        }
                    }
                }

                // if it's a no-op, ignore it (sometimes server asks a prop. to send changes even though it has none to send)
                // if it has serverJSONValue[FoundsetType.SERVER_SIZE] !== undefined that means a full value has been sent from server; so no granular updates above
                if (!serverJSONValue[FoundsetType.NO_OP] && serverJSONValue[FoundsetType.SERVER_SIZE] !== undefined) {
                    // not updates - so the whole thing was received

                    let internalState: FoundsetTypeInternalState;
                    let oldValueShallowCopy: FoundsetFieldsOnly;
                    if (!newValue /* newValue is now already currentValue, see code above, so we are checking current value here */) {
                        newValue = new FoundsetValue();
                        this.sabloConverters.prepareInternalState(newValue, new FoundsetTypeInternalState(propertyContext, this.webSocket, componentScope,
                             this.sabloConverters, this.viewportModule, this.sabloDeferHelper, this.foundsetTypeConstants, this.log));
                        internalState = newValue[this.sabloConverters.INTERNAL_IMPL];
                        this.sabloDeferHelper.initInternalStateForDeferring(internalState, "svy foundset * ");
                    } else {
                        // reuse old value; but make a shallow copy of the old value to give as oldValue to the listener
                        internalState = newValue[this.sabloConverters.INTERNAL_IMPL];
                        
                        oldValueShallowCopy = new FoundsetFieldsOnly(newValue);
                    }
                    
                    for (const propName of Object.keys(serverJSONValue)) {
                        newValue[propName] = serverJSONValue[propName];
                    }
        
                    const rows = newValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS];
        
                    // convert data if needed - specially done for Date send/receive as the rest are primitives anyway in case of foundset
                    // relocate conversion info in internal state and convert
                    newValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS] = this.viewportModule.updateWholeViewport([] /* this is a full viewport replace; no need to give old/currentClientValue rows here I think */,
                            internalState, newValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS], newValue[FoundsetType.VIEW_PORT][this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY],
                            undefined, componentScope, internalState.propertyContextCreator, false);
                        delete newValue[FoundsetType.VIEW_PORT][this.sabloConverters.CONVERSION_CL_SIDE_TYPE_KEY];
                    
                    for (let i = rows.length - 1; i >= 0; i--) {
                        rows[i] = new RowValue(rows[i], newValue);
                    }

                    if (hasListeners) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_FULL_VALUE_CHANGED] = { oldValue : oldValueShallowCopy, newValue : newValue };
                }
            }

            // restore/add watches
            this.addBackWatches(newValue, componentScope);
            
            if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy foundset * updates or value received from server; new viewport and server size (" + (newValue ? newValue[FoundsetType.VIEW_PORT][FoundsetType.START_INDEX] + ", " + newValue[FoundsetType.VIEW_PORT][FoundsetType.SIZE] + ", " + newValue[FoundsetType.SERVER_SIZE] + ", " + JSON.stringify(newValue[FoundsetType.SELECTED_ROW_INDEXES]) : newValue) + ")");
            if (notificationParamForListeners && Object.keys(notificationParamForListeners).length > 0) {
                if (this.log.debugEnabled && this.log.debugLevel === this.log.SPAM) this.log.debug("svy foundset * firing founset listener notifications...");

                var currentRequestInfo = this.webSocket.getCurrentRequestInfo();
                if(currentRequestInfo) {
                    if (!requestInfos) requestInfos = [];
                    requestInfos.push(currentRequestInfo);
                }

                if (requestInfos) notificationParamForListeners[this.foundsetTypeConstants.NOTIFY_REQUEST_INFOS] = requestInfos;
                
                // use previous (current) value as newValue might be undefined/null and the listeners would be the same anyway
                (currentClientValue[this.sabloConverters.INTERNAL_IMPL] as FoundsetTypeInternalState).fireChanges(notificationParamForListeners);
            }

            return newValue;
        }

        public updateAngularScope(clientValue: FoundsetValue, componentScope: angular.IScope): void {
            if (clientValue) {
                this.removeAllWatches(clientValue);

                const internalState: FoundsetTypeInternalState = clientValue[this.sabloConverters.INTERNAL_IMPL];
                if (internalState) this.viewportModule.updateAngularScope(clientValue[FoundsetType.VIEW_PORT][FoundsetType.ROWS], internalState, componentScope, false);

                if (componentScope) this.addBackWatches(clientValue, componentScope);
            }
        }

        public fromClientToServer(newClientData: any, _oldClientData: FoundsetValue, _scope: angular.IScope, _propertyContext: sablo.IPropertyContext): any {
            if (newClientData) {
                const newDataInternalState: FoundsetTypeInternalState = newClientData[this.sabloConverters.INTERNAL_IMPL];
                if (newDataInternalState.isChanged()) {
                    const tmp = newDataInternalState.requests;
                    newDataInternalState.requests = [];
                    return tmp;
                }
            }
            return [];
        }
            
    }
    
    // this class if currently copied over to all places where it needs to be extended to avoid load order problems with js file that could happen in ng1 (like foundset or component type files being loaded in browser before viewport => runtime error because this class could not be found)
    // make sure you keep all the copies in sync    
    // ng2 will be smarter about load order and doesn't have to worry about this
    abstract class InternalStateForViewport_copyInFoundsetType implements sablo.ISmartPropInternalState {
        forFoundset?: () => FoundsetValue;
        
        viewportTypes: any; // TODO type this
        unwatchData: { [idx: number]: Array<() => void> };
        
        requests: Array<any> = [];
        
        // inherited from sablo.ISmartPropInternalState
        changeNotifier: () => void;
        
        
        constructor(public readonly webSocket: sablo.IWebSocket,
                        public componentScope: angular.IScope, public readonly changeListeners: Array<(values: any) => void> = []) {}
                        
        getComponentScope() {
            return this.componentScope;
        }        

        updateAngularScope(newComponentScope: angular.IScope) {
            this.componentScope = newComponentScope;
        }
        
        setChangeNotifier(changeNotifier: () => void): void {
            this.changeNotifier = changeNotifier;
        }
         
        isChanged(): boolean {
            return this.requests && (this.requests.length > 0);
        }
        
        fireChanges(changes: any) {
            for (let i = 0; i < this.changeListeners.length; i++) {
                this.webSocket.setIMHDTScopeHintInternal(this.componentScope);
                this.changeListeners[i](changes);
                this.webSocket.setIMHDTScopeHintInternal(undefined);
            }
        }

    }
    
    class FoundsetTypeInternalState extends InternalStateForViewport_copyInFoundsetType implements sablo.IInternalStateWithDeferred {
        
        propertyContextCreator: sablo.IPropertyContextCreator;
        deferred: { [msgId: number]: { defer: angular.IDeferred<unknown>, timeoutPromise: angular.IPromise<void> } }; // key is msgId (which always increases), values is { defer: ...q defer..., timeoutPromise: ...timeout promise for cancel... }
        currentMsgId: number;
        timeoutRejectLogPrefix: string;
        
        selectionUpdateDefer: angular.IDeferred<unknown>;
        unwatchSelection: () => void;
        
        constructor(propertyContext: sablo.IPropertyContext,
                    webSocket: sablo.IWebSocket,
                    componentScope: angular.IScope,
                    public readonly sabloConverters: sablo.ISabloConverters,
                    public readonly viewportModule: ngclient.propertyTypes.ViewportService,
                    public readonly sabloDeferHelper: sablo.ISabloDeferHelper,
                    public foundsetTypeConstants: foundsetType.FoundsetTypeConstants,
                    public readonly log: sablo.ILogService) {
                        
            super(webSocket, componentScope);
            
            this.propertyContextCreator = {
                withPushToServerFor(_propertyName: string): sablo.IPropertyContext {
                    return propertyContext; // currently foundset prop columns always have foundset prop's pushToServer so only one property context needed
                }
            } as sablo.IPropertyContextCreator;
        }
        
        fireChanges(changes: foundsetType.ChangeEvent): void {
            super.fireChanges(changes);
        }

    }

    export class FoundsetValue implements foundsetType.FoundsetPropertyValue {
        
        public foundsetId: any;
        public serverSize: number;
        
        public viewPort: Viewport;
        
        public selectedRowIndexes: number[];
        public sortColumns: string;
        public multiSelect: boolean;
        public findMode: boolean;
        public hasMoreRows: boolean;
        public columnFormats?: Record<string, any>;
        
        private __internalState: FoundsetTypeInternalState;

        constructor () {
        }
        
        // PUBLIC API to components follows; make it 'smart'
        
        public getId(): number {
            // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "foundsetRef"
            return this[FoundsetType.FOUNDSET_ID];
        }
        
        public loadRecordsAsync(startIndex: number, size: number): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * loadRecordsAsync requested with (" + startIndex + ", " + size + ")");
            if (isNaN(startIndex) || isNaN(size)) throw new Error("loadRecordsAsync: start or size are not numbers (" + startIndex + "," + size + ")");

            const req = {newViewPort: {startIndex : startIndex, size : size}};
            const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            req[FoundsetType.ID_KEY] = requestID;
            this.__internalState.requests.push(req);
            
            if (this.__internalState.changeNotifier) this.__internalState.changeNotifier();
            return this.__internalState.deferred[requestID].defer.promise;
        }
        
        public loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * loadExtraRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")");
            if (isNaN(negativeOrPositiveCount)) throw new Error("loadExtraRecordsAsync: extrarecords is not a number (" + negativeOrPositiveCount + ")");

            const req = { loadExtraRecords: negativeOrPositiveCount };
            const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            req[FoundsetType.ID_KEY] = requestID;
            this.__internalState.requests.push(req);
            
            if (this.__internalState.changeNotifier && !dontNotifyYet) this.__internalState.changeNotifier();
            return this.__internalState.deferred[requestID].defer.promise;
        }
        
        public loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet?: boolean): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * loadLessRecordsAsync requested with (" + negativeOrPositiveCount + ", " + dontNotifyYet + ")");
            if (isNaN(negativeOrPositiveCount)) throw new Error("loadLessRecordsAsync: lessrecords is not a number (" + negativeOrPositiveCount + ")");

            const req = { loadLessRecords: negativeOrPositiveCount };
            const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            req[FoundsetType.ID_KEY] = requestID;
            this.__internalState.requests.push(req);

            if (this.__internalState.changeNotifier && !dontNotifyYet) this.__internalState.changeNotifier();
            return this.__internalState.deferred[requestID].defer.promise;
        }
        
        public notifyChanged() {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * notifyChanged called");
            if (this.__internalState.changeNotifier && this.__internalState.requests.length > 0) this.__internalState.changeNotifier();
        }
        
        public sort(columns: any): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * sort requested with " + JSON.stringify(columns));
            const req = { sort: columns };
            const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            req[FoundsetType.ID_KEY] = requestID;
            this.__internalState.requests.push(req);
            if (this.__internalState.changeNotifier) this.__internalState.changeNotifier();
            return this.__internalState.deferred[requestID].defer.promise;
        }
        
        public setPreferredViewportSize(size: number, sendSelectionViewportInitially?: boolean, initialSelectionViewportCentered?: boolean): void {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * setPreferredViewportSize called with (" + size + ", " + sendSelectionViewportInitially + ", " + initialSelectionViewportCentered + ")");
            if (isNaN(size)) throw new Error("setPreferredViewportSize(...): illegal argument; size is not a number (" + size + ")");
            const request = { "preferredViewportSize" : size };
            if (angular.isDefined(sendSelectionViewportInitially)) request["sendSelectionViewportInitially"] = !!sendSelectionViewportInitially;
            if (angular.isDefined(initialSelectionViewportCentered)) request["initialSelectionViewportCentered"] = !!initialSelectionViewportCentered;
            this.__internalState.requests.push(request);
            if (this.__internalState.changeNotifier) this.__internalState.changeNotifier();
        }
        
        public requestSelectionUpdate(tmpSelectedRowIdxs: number[]): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * requestSelectionUpdate called with " + JSON.stringify(tmpSelectedRowIdxs));
            if (this.__internalState.selectionUpdateDefer) {
                this.__internalState.selectionUpdateDefer.reject("Selection change defer cancelled because we are already sending another selection to server.");
            }
            delete this.__internalState.selectionUpdateDefer;

            const msgId = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            this.__internalState.selectionUpdateDefer = this.__internalState.deferred[msgId].defer;
            
            const req = {newClientSelectionRequest: tmpSelectedRowIdxs, selectionRequestID: msgId};
            req[FoundsetType.ID_KEY] = msgId;
            this.__internalState.requests.push(req);
            if (this.__internalState.changeNotifier) this.__internalState.changeNotifier();

            return this.__internalState.selectionUpdateDefer.promise;
        }
        
        public getRecordRefByRowID(rowID: string) {
            if (rowID)
            {
                const recordRef = {};
                recordRef[this.__internalState.foundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
                recordRef[FoundsetType.FOUNDSET_ID] = this.getId();
                return recordRef;
            }
            return null;
        }
        
        public updateViewportRecord(rowID: string, columnID: string, newValue: any, oldValue: any): angular.IPromise<any> {
            if (this.__internalState.log.debugEnabled && this.__internalState.log.debugLevel === this.__internalState.log.SPAM) this.__internalState.log.debug("svy foundset * updateRecord requested with (" + rowID + ", " + columnID + ", " + newValue);
            
            const req = { };
            const requestID = this.__internalState.sabloDeferHelper.getNewDeferId(this.__internalState);
            req[FoundsetType.ID_KEY] = requestID;
            
            const r = {};
            r[this.__internalState.foundsetTypeConstants.ROW_ID_COL_KEY] = rowID;
            r[FoundsetType.DATAPROVIDER_KEY] = columnID;
            r[FoundsetType.VALUE_KEY] = newValue;

            // convert new data if necessary
            r[FoundsetType.VALUE_KEY] = this.__internalState.sabloConverters.convertFromClientToServer(
                r[FoundsetType.VALUE_KEY],
                this.__internalState.viewportModule.getClientSideTypeFor(rowID, columnID, this.__internalState, this.viewPort.rows),
                oldValue, this.__internalState.getComponentScope(), this.__internalState.propertyContextCreator.withPushToServerFor(undefined)); // if clientSideType would be null we still want to call it for default conversions

            req['viewportDataChanged'] = r;

            this.__internalState.requests.push(req);
            if (this.__internalState.changeNotifier) this.__internalState.changeNotifier();
            
            return this.__internalState.deferred[requestID].defer.promise;
        }
        
        /**
         * Adds a change listener that will get triggered when server sends changes for this foundset.
         * 
         * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
         * @param listener the listener to register.
         * @return a listener unregister function
         */
        public addChangeListener(listener: (change: foundsetType.ChangeEvent) => void): () => void {
            this.__internalState.changeListeners.push(listener);
            return () => this.removeChangeListener(listener);
        }
        
        public removeChangeListener(listener: (change: foundsetType.ChangeEvent) => void) {
            const index = this.__internalState.changeListeners.indexOf(listener);
            if (index > -1) {
                this.__internalState.changeListeners.splice(index, 1);
            }
        }
        
    }
    
    export class RowValue {
        
       private readonly _foundset: FoundsetValue;
       _svyRowId: string;
        
        constructor (serverRow: object, foundset: FoundsetValue) {
            for (const propName of Object.getOwnPropertyNames(serverRow)) {
                this[propName] = serverRow[propName];
            }
            
            // make foundset private member non-iterable in JS world
            if (Object.defineProperty) {
                // try to avoid unwanted iteration/non-intended interference over the private property state
                Object.defineProperty(this, "_foundset", {
                    configurable: false,
                    enumerable: false,
                    writable: false,
                    value: foundset
                });
            } else this._foundset = foundset;
        }
        
        public getId(): string {
            // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "recordRef"
            return this._svyRowId;
        }
        
        public getFoundset(): FoundsetValue {
            // conversion to server needs this in case it is sent to handler or server side internalAPI calls as argument of type "recordRef"
            return this._foundset;
        }
        
    }
    
    type Viewport = {
            startIndex: number,
            size: number,
            rows: { _svyRowId: string, [columnName: string]: any }[];
    }
    
    class FoundsetFieldsOnly {

        foundsetId: number;
        serverSize: number;
        viewPort: Viewport;
        selectedRowIndexes: number[];
        sortColumns: string;
        multiSelect: boolean;
        hasMoreRows: boolean;
        columnFormats?: Record<string, any>;

        constructor(foundsetToShallowCopy: FoundsetValue) {
            this.foundsetId = foundsetToShallowCopy.foundsetId;
            this.serverSize = foundsetToShallowCopy.serverSize;
            this.viewPort = foundsetToShallowCopy.viewPort;
            this.selectedRowIndexes = foundsetToShallowCopy.selectedRowIndexes;
            this.sortColumns = foundsetToShallowCopy.sortColumns;
            this.multiSelect = foundsetToShallowCopy.multiSelect;
            this.hasMoreRows = foundsetToShallowCopy.hasMoreRows;
            this.columnFormats = foundsetToShallowCopy.columnFormats;
        }

    }

}