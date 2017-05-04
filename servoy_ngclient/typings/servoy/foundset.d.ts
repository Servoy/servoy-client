/// <reference path="../angularjs/angular.d.ts" />

declare namespace foundsetType {
	
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
		NOTIFY_VIEW_PORT_START_INDEX_CHANGED: string,
		NOTIFY_VIEW_PORT_SIZE_CHANGED: string,
		NOTIFY_VIEW_PORT_ROWS_COMPLETELY_CHANGED: string,
		NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED: string,
	
		// row update types for listener notifications - in case NOTIFY_VIEW_PORT_ROW_UPDATES_RECEIVED is triggered
		ROWS_CHANGED: number,
	    ROWS_INSERTED: number,
	    ROWS_DELETED: number
    }
	
} 