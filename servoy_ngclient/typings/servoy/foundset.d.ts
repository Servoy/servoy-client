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

} 