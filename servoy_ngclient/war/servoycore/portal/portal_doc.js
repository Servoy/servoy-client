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