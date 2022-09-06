/// <reference path="../angularjs/angular.d.ts" />
/// <reference path="./component.d.ts" />

declare namespace foundsetType {

    type ChangeListener = (changeEvent: ChangeEvent) => void;

    /**
     * Besides working like a normal IPromise that you can use to get notified when some action is done
     * (success/error/finally), chain etc., this promise also contains field "requestInfo" which can be set
     * by the user and could later be reported in some listener events back to the user (in case this same
     * action is going to trigger those listeners as well).
     *
     * @since 2021.09
     */
    interface RequestInfoPromise<T> extends angular.IPromise<T> {

        /**
         * You can assign any value to it. The value that you assign - if any - will be given back in the
         * event object of any listener that will be triggered as a result of the promise's action. So in
         * case the same action, when done, will trigger both the "then" of the Promise and a separate
         * listener, that separate listener will contain this "requestInfo" value.
         *
         * This is useful for some components that want to know if some change (reported by the listener)
         * happened due to an action that the component requested or due to changes in the outside world.
         * (eg: FoundsetPropertyValue.loadRecordsAsync(...) returns RequestInfoPromise and 
         * ChangeEvent.requestInfos array can return that RequestInfoPromise.requestInfo on the event that
         * was triggered by that loadRecordsAsync)
         */
        requestInfo?: any;

    }

    interface FoundsetPropertyValue {

        /**
         * An identifier that allows you to use this foundset via the 'foundsetRef' type;
         * when a 'foundsetRef' type sends a foundset from server to client (for example
         * as a return value of callServerSideApi) it will translate to this identifier
         * on client (so you can use it to find the actual foundset property in the model if
         * server side script put it in the model as well); internally when sending a
         * 'foundset' typed property to server through a 'foundsetRef' typed argument or prop,
         * it will use this foundsetId as well to find it on server and give a real Foundset
         */
        foundsetId: number, 

        /**
         * the size of the foundset on server (so not necessarily the total record count
         * in case of large DB tables)
         */
        serverSize: number, 

        /**
         * this is the data you need to have loaded on client (just request what you need via provided
         * loadRecordsAsync or loadExtraRecordsAsync)
         */
        viewPort: {
            startIndex: number,
            size: number,
            rows: object[]
        },

        /**
         * array of selected records in foundset; indexes can be out of current
         * viewPort as well
         */
        selectedRowIndexes: number[],

        /**
         * sort string of the foundset, the same as the one used in scripting for
         * foundset.sort and foundset.getCurrentSort. Example: 'orderid asc'.
         */
        sortColumns: string,

        /**
         * the multiselect mode of the server's foundset; if this is false,
         * selectedRowIndexes can only have one item in it
         */
        multiSelect: boolean,

        /**
         * if the foundset is large and on server-side only part of it is loaded (so
         * there are records in the foundset beyond 'serverSize') this is set to true;
         * in this way you know you can load records even after 'serverSize' (requesting
         * viewport to load records at index serverSize-1 or greater will load more
         * records in the foundset)
         */
        hasMoreRows: boolean,

        /** 
         * columnFormats is only present if you specify
         * "provideColumnFormats": true inside the .spec file for this foundset property;
         * it gives the default column formatting that Servoy would normally use for
         * each column of the viewport - which you can then also use in the
         * browser yourself; keys are the dataprovider names and values are objects that contain
         * the format contents
         */
        columnFormats?: object, 

        /**
         * Request a change of viewport bounds from the server; the requested data will be loaded
         * asynchronously in 'viewPort'
         *
         * @param startIndex the index that you request the first record in "viewPort.rows" to have in
         *                   the real foundset (so the beginning of the viewPort).
         * @param size the number of records to load in viewPort.
         *
         * @return a $q promise that will get resolved when the requested records arrived browser-
         *                   side. As with any promise you can register success, error callbacks, finally, ...
         *                   See JSDoc of RequestInfoPromise.requestInfo and ChangeEvent.requestInfos
         *                   for more information about determining if a listener event was caused by this call.
         */
        loadRecordsAsync(startIndex: number, size: number): RequestInfoPromise<any>;

        /**
         * Request more records for your viewPort; if the argument is positive more records will be
         * loaded at the end of the 'viewPort', when negative more records will be loaded at the beginning
         * of the 'viewPort' - asynchronously.
         *
         * @param negativeOrPositiveCount the number of records to extend the viewPort.rows with before or
         *                                after the currently loaded records.
         * @param dontNotifyYet if you set this to true, then the load request will not be sent to server
         *                      right away. So you can queue multiple loadLess/loadExtra before sending them
         *                      to server. If false/undefined it will send this (and any previously queued
         *                      request) to server. See also notifyChanged(). See also notifyChanged().
         *
         * @return a $q promise that will get resolved when the requested records arrived browser-
         *                   side. As with any promise you can register success, error callbacks, finally, ...
         *                   That allows custom component to make sure that loadExtra/loadLess calls from
         *                   client do not stack on not yet updated viewports to result in wrong bounds.
         *                   See JSDoc of RequestInfoPromise.requestInfo and ChangeEvent.requestInfos
         *                   for more information about determining if a listener event was caused by this call.
         */
        loadExtraRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet: boolean): RequestInfoPromise<any>;

        /**
         * Request a shrink of the viewport; if the argument is positive the beginning of the viewport will
         * shrink, when it is negative then the end of the viewport will shrink - asynchronously.
         *
         * @param negativeOrPositiveCount the number of records to shrink the viewPort.rows by before or
         *                                after the currently loaded records.
         * @param dontNotifyYet if you set this to true, then the load request will not be sent to server
         *                      right away. So you can queue multiple loadLess/loadExtra before sending them
         *                      to server. If false/undefined it will send this (and any previously queued
         *                      request) to server. See also notifyChanged(). See also notifyChanged().
         *
         * @return a $q promise that will get resolved when the requested records arrived browser
         *                   -side. As with any promise you can register success, error callbacks, finally, ...
         *                   That allows custom component to make sure that loadExtra/loadLess calls from
         *                   client do not stack on not yet updated viewports to result in wrong bounds.
         *                   See JSDoc of RequestInfoPromise.requestInfo and ChangeEvent.requestInfos
         *                   for more information about determining if a listener event was caused by this call.
         */
        loadLessRecordsAsync(negativeOrPositiveCount: number, dontNotifyYet: boolean): RequestInfoPromise<any>;

        /**
         * If you queue multiple loadExtraRecordsAsync and loadLessRecordsAsync by using dontNotifyYet = true
         * then you can - in the end - send all these requests to server (if any are queued) by calling
         * this method. If no requests are queued, it calling this method will have no effect.
         */
        notifyChanged(): void;

        /**
         * Sort the foundset by the dataproviders/columns identified by sortColumns.
         *
         * The name property of each sortColumn can be filled with the dataprovider name the foundset provides
         * or specifies. If the foundset is used with a component type (like in table-view) then the name is
         * the name of the component on who's first dataprovider property the sort should happen. If the
         * foundset is used with another foundset-linked property type (dataprovider/tagstring linked to
         * foundsets) then the name you should give in the sortColumn is that property's 'idForFoundset' value
         * (for example a record 'dataprovider' property linked to the foundset will be an array of values
         * representing the viewport, but it will also have a 'idForFoundset' prop. that can be used for
         * sorting in this call; this 'idForFoundset' was added in version 8.0.3).
         *
         * @param sortColumns an array of JSONObjects { name : dataprovider_id,
         *                    direction : sortDirection }, where the sortDirection can be "asc" or "desc".
         * @return (added in Servoy 8.2.1) a $q promise that will get resolved when the new sort
         *                   will arrive browser-side. As with any promise you can register success, error
         *                   and finally callbacks.
         *                   See JSDoc of RequestInfoPromise.requestInfo and ChangeEvent.requestInfos
         *                   for more information about determining if a listener event was caused by this call.
         */
        sort(sortColumns: Array<{ name: string, direction: ("asc" | "desc") }>): RequestInfoPromise<any>;

        /**
         * Request a selection change of the selected row indexes. Returns a promise that is resolved
         * when the client receives the updated selection from the server. If successful, the array
         * selectedRowIndexes will also be updated. If the server does not allow the selection change,
         * the reject function will get called with the 'old' selection as parameter.
         *
         * If requestSelectionUpdate is called a second time, before the first call is resolved, the
         * first call will be rejected and the caller will receive the string 'canceled' as the value
         * for the parameter serverRows.
         * E.g.: foundset.requestSelectionUpdate([2,3,4]).then(function(serverRows){},function(serverRows){});
         *
         * @return a $q promise that will get resolved when the requested selection was updated server-
         *                   side. As with any promise you can register success, error callbacks, finally, ...
         *                   See JSDoc of RequestInfoPromise.requestInfo and ChangeEvent.requestInfos
         *                   for more information about determining if a listener event was caused by this call.
         */
        requestSelectionUpdate(selectedRowIdxs: number[]): RequestInfoPromise<any>;

        /**
         * Sets the preferred viewPort options hint on the server for this foundset, so that the next
         * (initial or new) load will automatically return that many rows, even without any of the loadXYZ
         * methods above being called.
         *
         * You can use this when the component size is not known initially and the number of records the
         * component wants to load depends on that. As soon as the component knows how many it wants
         * initially it can call this method.
         *
         * These can also be specified initially using the .spec options "initialPreferredViewPortSize" and
         * "sendSelectionViewportInitially". But these can be altered at runtime via this method as well
         * because they are used/useful in other scenarios as well, not just initially: for example when a
         * related foundset changes parent record, when a search/find is performed and so on.
         *
         * @param preferredSize the preferred number or rows that the viewport should get automatically
         *                      from the server.
         * @param sendViewportWithSelection if this is true, the auto-sent viewport will contain
         *                                            the selected row (if any).
         * @param centerViewportOnSelected if this is true, the selected row will be in the middle
         *                                           of auto-sent viewport if possible. If it is false, then
         *                                           the foundset property type will assume a 'paging'
         *                                           strategy and will send the page that contains the
         *                                           selected row (here the page size is assumed to be
         *                                           preferredSize).
         */
        setPreferredViewportSize(preferredSize: number, sendViewportWithSelection: boolean,
                                    centerViewportOnSelected: boolean): void;

        /**
         * It will send a data update for a cell (a column in a row) in the foundset to the server.
         * Please make sure to adjust the viewport value as well not just call this method.
         *
         * This method is useful if you do not want to add angular watches on data (so calculated pushToServer for the foundset property is set to just 'allow').
         * Then server will accept data changes from this property, but there are no automatic watches to detect the changes so the component must call
         * this method instead - when it wants to change the data in a cell.
         * 
         * @param rowID the _svyRowId (so $foundsetTypeConstants.ROW_ID_COL_KEY) column of the client side row
         * @param columnID the name of the column to be updated on server (in that row).
         * @param newValue the new data in that cell
         * @param oldValue the old data that used to be in that cell
         * @return (first versions of this method didn't return anything; more recent ones return this) a $q promise that will get resolved when the new cell value
         *                   update is done server-side (resolved if ok, rejected if it failed). As with any promise you can register success, error
         *                   and finally callbacks.
         */
        updateViewportRecord(rowID: string, columnID: string, newValue: any, oldValue: any): RequestInfoPromise<any>;

        /**
         * Receives a client side rowID (taken from myFoundsetProp.viewPort.rows[idx]
         * [$foundsetTypeConstants.ROW_ID_COL_KEY]) and gives a Record reference, an object
         * which can be resolved server side to the exact Record via the 'record' property type;
         * for example if you call a handler or a $scope.svyServoyapi.callServerSideApi(...) and want
         * to give it a Record as parameter and you have the rowID and foundset in your code,
         * you can use this method. E.g: $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord",
         *                     [$scope.model.myFoundsetProp.getRecordRefByRowID(clickedRowId)]);
         *
         * NOTE: if in your component you know the whole row (so myFoundsetProp.viewPort.rows[idx])
         * already - not just the rowID - that you want to send you can just give that directly to the
         * handler/serverSideApi; you do not need to use this method in that case. E.g:
         * // if you have the index inside the viewport
         * $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord",
         *           [$scope.model.myFoundsetProp.viewPort.rows[clickedRowIdx]]);
         * // or if you have the row directly
         * $scope.svyServoyapi.callServerSideApi("doSomethingWithRecord", [clickedRow]);
         *
         * This method has been added in Servoy 8.3.
         */
        getRecordRefByRowID(rowId: string): void;

        /**
         * Adds a change listener that will get triggered when server sends changes for this foundset.
         * 
         * @see $webSocket.addIncomingMessageHandlingDoneTask if you need your code to execute after all properties that were linked to this foundset get their changes applied you can use $webSocket.addIncomingMessageHandlingDoneTask.
         * @param changeListener the listener to register.
         * @return a listener unregister function
         */
        addChangeListener(changeListener : ChangeListener) : () => void;
        removeChangeListener(changeListener : ChangeListener) : void;

    }

    interface FoundsetTypeConstants {
        ROW_ID_COL_KEY: string,
        FOR_FOUNDSET_PROPERTY: string,

        // listener notification constants follow; prefixed just to separate them a bit from other constants
        NOTIFY_REQUEST_INFOS: string,
        NOTIFY_FULL_VALUE_CHANGED: string,
        NOTIFY_SERVER_SIZE_CHANGED: string,
        NOTIFY_HAS_MORE_ROWS_CHANGED: string,
        NOTIFY_MULTI_SELECT_CHANGED: string,
        NOTIFY_COLUMN_FORMATS_CHANGED: string,
        NOTIFY_SORT_COLUMNS_CHANGED: string,
        NOTIFY_SELECTED_ROW_INDEXES_CHANGED: string,
        NOTIFY_USER_SET_SELECTION: string, // true when selection is changed by user, in scripting
        NOTIFY_VIEW_PORT_START_INDEX_CHANGED: string,
        NOTIFY_VIEW_PORT_SIZE_CHANGED: string,
        NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED: string,
        NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED: string,
        NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE: string,
        NOTIFY_VIEW_PORT_ROW_UPDATES: string,
        NOTIFY_FOUNDSET_DEFINITION_CHANGE: string,

        // row update types for listener notifications, in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
        ROWS_CHANGED: number,
        ROWS_INSERTED: number,
        ROWS_DELETED: number
    }

    interface ChangeEvent extends componentType.ChangeEvent {

        /**
         * If this change event is caused by one or more calls (by the component) on the IFoundset obj
         * (like loadRecordsAsync requestSelectionUpdate and so on), and the caller then assigned a value to
         * the returned RequestInfoPromise's "requestInfo" field, then that value will be present in this array.
         *
         * This is useful for some components that want to know if some change (reported in this ChangeEvent)
         * happened due to an action that the component requested or due to changes in the outside world. (eg:
         * IFoundset.loadRecordsAsync(...) returns RequestInfoPromise and ChangeEvent.requestInfos array can
         * contain that RequestInfoPromise.requestInfo on the event that was triggered by that loadRecordsAsync)
         *
         * @since 2021.09
         */
        requestInfos?: any[],

        /**
         * If and a full value update was received from server, this key is set; if newValue is non-null:
         *   - prior to Servoy version 2021.06: newValue is a new reference, but it will automatically get
         *     the old value's listeners registered to itself
         *   - starting with Servoy 2021.06: the old value's reference will be reused (so the reference of
         *     the foundset property doesn't change, just it's contents are updated) and oldValue given
         *     below is actually a shallow-copy of the old value's properties/keys; this can help
         *     in some component implementations
         */
        fullValueChanged?: { oldValue: object, newValue: object },
     
        // the following keys appear if each of these got updated from server; the names of those
        // constants suggest what it was that changed; oldValue and newValue are the values for what changed
        // (e.g. new server size and old server size) so not the whole foundset property new/old value
        serverFoundsetSizeChanged?: { oldValue: number, newValue: number },
        hasMoreRowsChanged?:  { oldValue: boolean, newValue: boolean },
        multiSelectChanged?:  { oldValue: boolean, newValue: boolean },
        columnFormatsChanged?:  { oldValue: object, newValue: object },
        sortColumnsChanged?:  { oldValue: string, newValue: string },
        selectedRowIndexesChanged?:  { oldValue: number[], newValue: number[] },
        viewPortStartIndexChanged?:  { oldValue: number, newValue: number },
        viewPortSizeChanged?:  { oldValue: number, newValue: number }

    }

} 