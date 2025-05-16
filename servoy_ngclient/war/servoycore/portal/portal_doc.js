/**
 * A Servoy Extra Component that displays a portal for viewing and editing related foundset records.
 */

/**
 * A Servoy Extra Component that displays a portal for viewing and editing related foundset records.
 */

/**
 * The background color for the portal component.
 */
var background;

/**
 * The type of border applied to the portal.
 */
var borderType;

/**
 * Flag indicating whether the portal is enabled for user interaction.
 */
var enabled;

/**
 * Flag indicating whether the portal is editable.
 */
var editable;

/**
 * The foreground color for the portal component.
 */
var foreground;

/**
 * The height of the portal header in pixels.
 */
var headerHeight;

/**
 * The initial sort order for the portal, specified as a string.
 */
var initialSort;

/**
 * The spacing between cells in the portal.
 */
var intercellSpacing;

/**
 * The location of the portal on the form.
 */
var location;

/**
 * Flag indicating whether the portal supports multiple lines.
 */
var multiLine;

/**
 * The read-only mode setting for the portal.
 */
var readOnlyMode;

/**
 * The foundset related to the portal.
 */
var relatedFoundset;

/**
 * Flag indicating whether the portal supports reordering of records.
 */
var reorderable;

/**
 * Flag indicating whether the portal is resizable.
 */
var resizable;

/**
 * (Deprecated) Duplicate property for resizable.
 */
var resizeble;

/**
 * Calculation used to determine the row background color.
 */
var rowBGColorCalculation;

/**
 * The height of each row in the portal.
 */
var rowHeight;

/**
 * Configuration for scrollbars in the portal.
 */
var scrollbars;

/**
 * Flag indicating whether horizontal grid lines are shown.
 */
var showHorizontalLines;

/**
 * Flag indicating whether vertical grid lines are shown.
 */
var showVerticalLines;

/**
 * The dimensions (width and height) of the portal.
 */
var size;

/**
 * Flag indicating whether the portal is sortable.
 */
var sortable;

/**
 * CSS style classes applied to the portal.
 */
var styleClass;

/**
 * The tab sequence order for keyboard navigation.
 */
var tabSeq;

/**
 * Flag indicating whether the portal background is transparent.
 */
var transparent;

/**
 * Flag indicating whether the portal is visible.
 */
var visible;


var handlers = {
    /**
     * Called when a drag operation ends on the portal.
     *
     * @param {JSDNDEvent} event The event object associated with the drag end.
     */
    onDragEndMethodID: function() {},

    /**
     * Called during a drag operation over the portal.
     *
     * @param {JSDNDEvent} event The event object associated with the drag.
     *
     * @return {Number} The numeric value indicating the drag effect.
     */
    onDragMethodID: function() {},

    /**
     * Called when an element is dragged over the portal.
     *
     * @param {JSDNDEvent} event The event object associated with the drag over.
     *
     * @return {Boolean} True if the drag over action is accepted, false otherwise.
     */
    onDragOverMethodID: function() {},

    /**
     * Called when an element is dropped onto the portal.
     *
     * @param {JSDNDEvent} event The event object associated with the drop.
     *
     * @return {Boolean} True if the drop action is accepted, false otherwise.
     */
    onDropMethodID: function() {}
};

/**
 * Retrieves the sorting columns applied to the portal's related foundset.
 *
 * @return {String} A string representing the current sort order of the foundset, 
 * formatted as a comma-separated list of column names followed by sort directions (ASC or DESC).
 */
function getSortColumns() {
}

/**
 * Retrieves the width of the portal component.
 *
 * @return {Number} The width of the portal in pixels.
 */
function getWidth() {
}

/**
 * Retrieves the current horizontal scroll position of the portal.
 *
 * @return {Number} The horizontal scroll position in pixels. Always returns 0, as scrolling is not implemented for web.
 */
function getScrollX() {
}

/**
 * Retrieves the height of the portal component.
 *
 * @return {Number} The height of the portal in pixels.
 */
function getHeight() {
}

/**
 * Retrieves the current vertical scroll position of the portal.
 *
 * @return {Number} The vertical scroll position in pixels. Always returns 0, as scrolling is not implemented for web.
 */
function getScrollY() {
}

/**
 * Retrieves the name of the form that the portal is associated with.
 *
 * @return {String} The name of the form containing the portal.
 */
function getFormName() {
}

/**
 * Retrieves the index of the currently selected record in the portal's foundset.
 *
 * @return {Number} The 1-based index of the selected record, or -1 if no record is selected.
 */
function getSelectedIndex() {
}

/**
 * Retrieves the maximum record index available in the portal's foundset.
 *
 * @return {Number} The total number of records in the related foundset.
 */
function getMaxRecordIndex() {
}

/**
 * Sets the selected record index in the portal's foundset.
 *
 * @param {Number} index The 1-based index of the record to be selected.
 */
function setSelectedIndex(index) {
}

/**
 * Retrieves the X-coordinate (horizontal position) of the portal component.
 *
 * @return {Number} The X-coordinate of the portal in pixels.
 */
function getLocationX() {
}

/**
 * Sets the scroll position of the portal.
 *
 * @param {Number} x The horizontal scroll position in pixels.
 * @param {Number} y The vertical scroll position in pixels.
 */
function setScroll(x, y) {
}

/**
 * Duplicates the currently selected record in the portal's foundset.
 *
 * @param {Boolean} [addOnTop] Whether to add the duplicated record at the top of the foundset.
 */
function duplicateRecord(addOnTop) {
}

/**
 * Creates a new record in the portal's foundset.
 *
 * @param {Boolean} [addOnTop] Whether to add the new record at the top of the foundset.
 */
function newRecord(addOnTop) {
}

/**
 * Deletes the currently selected record from the portal's foundset.
 */
function deleteRecord() {
}

/**
 * Retrieves the Y-coordinate (vertical position) of the portal component.
 *
 * @return {Number} The Y-coordinate of the portal in pixels.
 */
function getLocationY() {
}



