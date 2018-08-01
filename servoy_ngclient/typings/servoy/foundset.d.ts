/// <reference path="../angularjs/angular.d.ts" />
/// <reference path="./component.d.ts" />

declare namespace foundsetType {
	
	type ChangeListener = (changeEvent: ChangeEvent) => void;
	
	interface FoundsetPropertyValue {
		addChangeListener(changeListener : ChangeListener) : void;
		removeChangeListener(changeListener : ChangeListener) : void;
	}
	
	interface FoundsetTypeConstants {
		ROW_ID_COL_KEY: string,
		FOR_FOUNDSET_PROPERTY: string,
	
		// listener notification constants follow; prefixed just to separate them a bit from other constants
		NOTIFY_FULL_VALUE_CHANGED: string,
		NOTIFY_SERVER_SIZE_CHANGED: string,
		NOTIFY_HAS_MORE_ROWS_CHANGED: string,
		NOTIFY_MULTI_SELECT_CHANGED: string,
		NOTIFY_COLUMN_FORMATS_CHANGED: string,
		NOTIFY_SORT_COLUMNS_CHANGED: string,
		NOTIFY_SELECTED_ROW_INDEXES_CHANGED: string,
		NOTIFY_SCROLL_TO_SELECTION: string,
		NOTIFY_VIEW_PORT_START_INDEX_CHANGED: string,
		NOTIFY_VIEW_PORT_SIZE_CHANGED: string,
		NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED: string,
		NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED: string,
		NOTIFY_VIEW_PORT_ROW_UPDATES_OLD_VIEWPORTSIZE: string,
		NOTIFY_VIEW_PORT_ROW_UPDATES: string,
	
		// row update types for listener notifications - in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
		ROWS_CHANGED: number,
	    ROWS_INSERTED: number,
	    ROWS_DELETED: number
    }
	
	interface ChangeEvent extends componentType.ChangeEvent {
		// if value was non-null and had a listener attached before, and a full value update was
	    // received from server, this key is set; if newValue is non-null, it will automatically get the old
	    // value's listeners registered to itself
	    // NOTE: it might be easier to rely just on a shallow $watch of the foundset value (to catch even the
	    // null to non-null scenario that you still need) and not use NOTIFY_FULL_VALUE_CHANGED at all
		fullValueChanged: { oldValue: object, newValue: object },
	 
	    // the following keys appear if each of these got updated from server; the names of those
	    // constants suggest what it was that changed; oldValue and newValue are the values for what changed
	    // (e.g. new server size and old server size) so not the whole foundset property new/old value
		serverFoundsetSizeChanged: { oldValue: number, newValue: number },
		hasMoreRowsChanged:  { oldValue: boolean, newValue: boolean },
		multiSelectChanged:  { oldValue: boolean, newValue: boolean },
		columnFormatsChanged:  { oldValue: object, newValue: object },
		sortColumnsChanged:  { oldValue: string, newValue: string },
		selectedRowIndexesChanged:  { oldValue: number[], newValue: number[] },
		viewPortStartIndexChanged:  { oldValue: number, newValue: number },
		viewPortSizeChanged:  { oldValue: number, newValue: number },
	}
	
	type ViewportRowUpdate = RowsChanged | RowsInserted | RowsDeleted;
	type ViewportRowUpdates = ViewportRowUpdate[];
	
    type RowsChanged = { type: 0, startIndex: number, endIndex: number };

	/**
	 * When an INSERT happened but viewport size remained the same, it is
     * possible that some of the rows that were previously at the end of the viewport
     * slided out of it; "removedFromVPEnd" gives the number of such rows that were removed
     * from the end of the viewport due to the insert operation;
     * NOTE: insert signifies an insert into the client viewport, not necessarily
     * an insert in the foundset itself; for example calling "loadExtraRecordsAsync"
     * can result in an insert notification + bigger viewport size notification,
     * with removedFromVPEnd = 0
     */
    type RowsInserted = { type: 1, startIndex: number, endIndex: number, removedFromVPEnd: number };
    
    
    /**
     * When a DELETE happened inside the viewport but there were more rows available in the
     * foundset after current viewport, it is possible that some of those rows
     * slided into the viewport; "appendedToVPEnd " gives the number of such rows
     * that were appended to the end of the viewport due to the DELETE operation
     * NOTE: delete signifies a delete from the client viewport, not necessarily
     * a delete in the foundset itself; for example calling "loadLessRecordsAsync" can
     * result in a delete notification + smaller viewport size notification,
     * with appendedToVPEnd = 0
     */                                
    type RowsDeleted = { type: 2, startIndex : number, endIndex : number, appendedToVPEnd : number }

	
} 