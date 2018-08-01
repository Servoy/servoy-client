/// <reference path="../angularjs/angular.d.ts" />

declare namespace componentType {
	
	type ViewportChangeListener = (changeEvent: ChangeEvent) => void;
	
	interface ComponentPropertyValue {
		addViewportChangeListener(viewportChangeListener : ViewportChangeListener) : void;
		removeViewportChangeListener(viewportChangeListener : ViewportChangeListener) : void;
	}
	
	interface ChangeEvent {
	    // the following keys appear if each of these got updated from server; the names of those
	    // constants suggest what it was that changed; oldValue and newValue are the values for what changed
	    // (e.g. new server size and old server size) so not the whole foundset property new/old value
		viewportRowsCompletelyChanged:  { oldValue: number, newValue: number },
	 
	    // if we received add/remove/change operations on a set of rows from the viewport, this key
	    // will be set; as seen below, it contains "updates" which is an array that holds a sequence of
	    // granular update operations to the viewport; the array will hold one or more granular add or remove
	    // or update operations;
	    // all the "startIndex" and "endIndex" values below are relative to the viewport's state after all
	    // previous updates in the array were already processed (so they are NOT relative to the initial state);
	    // indexes are 0 based
		viewportRowsUpdated: { updates: ViewportRowUpdates, oldViewportSize: number }
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