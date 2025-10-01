/** listformcomponent_doc.js */

/**
 * Reference to the foundset bound to the component for displaying data.
 */
var foundset;

/**
 * Specifies the form that is embedded within the list form component.
 */
var containedForm;

/**
 * Determines the layout mode of the list form component, such as 'cardview' or 'listview'.
 */
var pageLayout;

/**
 * This property in only used when the component is placed in a responsive form; it is ignored in absolute; when used in paging mode (client property UICONSTANTS.LISTFORMCOMPONENT_PAGING_MODE = true) it sets the number of records displayed in a single page; when used in scrolling mode (which is the default mode) it is only used when pageLayout is set to cardiew, and it sets the maximum records displayed in a single row of the listformcomponent.
 */
var responsivePageSize;

/**
 * This property sets the height of the listformcomponent when using scrolling mode in a responisive form. Adding a listformcomponent in a flex-content layout and setting responsiveHeight property to 0, let the listformcomponent grow up to 100% height of parent element (see more on flex-layout here: https://github.com/Servoy/12grid/wiki/Flexbox-Layout ). Used with other containers than flex-content layout in order to grow the listformcomponent to 100% height, the parent element must have a known height. When responsiveHeight is set to -1, the LFC will auto-size it's height to the number of rows displayed - in this case there is no vertical scrollbar and all rows are rendered
 */
var responsiveHeight;

/**
 * CSS style class for custom styling of the list form component.
 */
var styleClass;

/**
 * CSS style class applied to individual rows in the list form component.
 */
var rowStyleClass;

/**
 * Data provider used to determine the CSS style class for each row dynamically.
 */
var rowStyleClassDataprovider;

/**
 * Data provider indicating whether each row is editable.
 */
var rowEditableDataprovider;

/**
 * Data provider indicating whether each row is enabled.
 */
var rowEnableDataprovider;

/**
 * CSS style class for styling the pagination controls of the component.
 */
var paginationStyleClass;

/**
 * Specifies if the list form component allows editing of entries.
 */
var editable;

/**
 * In case <b>rowStyleClassDataprovider</b> or <b>rowStyleClass</b> are used, make sure that the selection styleclass definition is last in the solution stylesheet, to avoid overwriting it.<br/><i>.listitem-info</i> {<br/>&nbsp;&nbsp;&nbsp;<i>background-color: blue;</i><br/>}<br/><i>.listitem-selected</i> {<br/>&nbsp;&nbsp;&nbsp;<i>background-color: orange;</i><br/>}
 */
var selectionClass;

/**
 * Defines the tab order for navigating through the component.
 */
var tabSeq;

/**
 * Controls the visibility of the list form component.
 */
var visible;


var handlers = {
    /**
     * Triggered when the foundset selection changes.
     * 
     * @param {JSEvent} event Contains details about the selection change.
     */
    onSelectionChanged: function() {},

    /**
     * Triggered when a list item is clicked.
     * 
     * @param {JSRecord} record The record associated with the clicked list item.
     * @param {JSEvent} event Contains details about the click event, such as the source and coordinates.
     */
    onListItemClick: function() {}
};

