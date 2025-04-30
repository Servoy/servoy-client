/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.scripting.info;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * <p>The <code>UICONSTANTS</code> object provides predefined script constants to configure various aspects
 * of the user interface. These constants enable developers to control application behavior, including
 * component-specific properties, tooltip timings, and table view settings.</p>
 *
 * <p>For instance:</p>
 * <ul>
 *   <li><code>CALENDAR_NG_SHOW_ISO_WEEK_NUMBER</code> allows toggling the display of ISO week numbers in
 *   calendar components.</li>
 *   <li><code>COMBOBOX_ENABLE_FILTER</code> controls the visibility of search boxes in combobox fields.</li>
 *   <li><code>HTML_EDITOR_CONFIGURATION</code> supports custom configurations for TinyMCE editors in
 *   editable HTML areas.</li>
 *   <li><code>TABLEVIEW_NG_PAGE_SIZE_FACTOR</code> adjusts data-loading size for table views in NGClient,
 *   enhancing performance for large datasets.</li>
 *   <li><code>TRUST_DATA_AS_HTML</code> determines whether displayed data is sanitized, a feature that
 *   should be used cautiously to avoid potential XSS vulnerabilities.</li>
 * </ul>
 *
 * <p>These constants are typically configured using methods like <code>application.putClientProperty</code>
 * or <code>element.putClientProperty</code>, allowing for application-wide or component-specific behavior
 * modifications. Many of these configurations, such as <code>TOOLTIP_INITIAL_DELAY</code> or
 * <code>TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN</code>, contribute to a more user-friendly interface.</p>
 *
 * @author gerzse
 */
@SuppressWarnings("nls")
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class UICONSTANTS implements IPrefixedConstantsObject
{

	/**
	 * Property that can be set using application.putClientProperty() or element.putClientProperty().
	 *
	 * If set on application it will affect all TYPE-AHEAD fields. If set on an element it will affect only that TYPE-AHEAD element/field (with priority over the application property).
	 * Value can be true/false/null.
	 *
	 * If set to true, the affected TYPE_AHEAD(s) will show the pop-up when the field content is empty.
	 * DEFAULT: true.
	 *
	 * @sample
	 * // make all TYPE-AHEAD fields not show the pop-up when there is empty content in the field
	 * application.putClientProperty(UICONSTANTS.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY, false);
	 * // make one TYPE-AHEAD field show the pop-up when there is empty content in the field - overrides the application property set
	 * forms.someForm.elements.typeAheadElement.putClientProperty(UICONSTANTS.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY, true);
	 *
	 * @deprecated not used in TiNG
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public static final String TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY = IApplication.TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY;

	/**
	 * Property that can be set using application.putClientProperty() or element.putClientProperty().
	 *
	 * If set on application it will affect all default/legacy TYPE-AHEAD fields. If set on an element it will affect only that default/legacy TYPE-AHEAD element/field
	 * (with priority over the application property).
	 * Value can be true/false/null.
	 *
	 * If set to true, the affected TYPE_AHEAD(s) will show the pop-up when gaining focus.
	 * DEFAULT: true.
	 *
	 * @sample
	 * // make all TYPE-AHEAD fields not show the pop-up when gaining focus
	 * application.putClientProperty(UICONSTANTS.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN, false);
	 * // make one TYPE-AHEAD field show the pop-up when gaining focus - overrides the application property set
	 * forms.someForm.elements.typeAheadElement.putClientProperty(UICONSTANTS.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN, true);
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN = IApplication.TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN;

	/**
	 * Property that can be set using application.putClientProperty() or element.putClientProperty(). It is used only in Titanium Client.
	 *
	 * If set on application it will affect all COMBOBOX fields. If set on an element it will affect only that COMBOBOX element/field (with priority over the application property).
	 * Value can be true/false/null.
	 *
	 * If set to true, the affected COMBOBOX will show the pop-up when gaining focus (either from user input or API).
	 * DEFAULT:  false for Titanium Client (to be more compatible with NGClient).
	 *
	 * @sample
	 * // make all COMBOBOX fields not show the pop-up when gaining focus
	 * application.putClientProperty(UICONSTANTS.COMBOBOX_SHOW_POPUP_ON_FOCUS_GAIN, false);
	 * // make one COMBOBOX field show the pop-up when gaining focus - overrides the application property set
	 * forms.someForm.elements.comboboxElement.putClientProperty(UICONSTANTS.COMBOBOX_SHOW_POPUP_ON_FOCUS_GAIN, true);
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = true)
	public static final String COMBOBOX_SHOW_POPUP_ON_FOCUS_GAIN = IApplication.COMBOBOX_SHOW_POPUP_ON_FOCUS_GAIN;

	/**
	 * Property that can be set using application.putClientProperty(). It is used only in NGClient (not in Titanium Client).
	 *
	 * This is a global setting, it will affect all COMBOBOX fields. It must be set as soon as possible, ie. on solution open.
	 * Value can be true/false/null.
	 *
	 * If set to false, ALL COMBOBOXes will hide the search box when gaining focus.
	 * DEFAULT: null.
	 *
	 * @sample
	 * // make all COMBOBOX fields hide the search box when gaining focus
	 * application.putClientProperty(UICONSTANTS.COMBOBOX_ENABLE_FILTER, false);
	 *
	 * @deprecated not used in TiNG
	 */
	@Deprecated
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String COMBOBOX_ENABLE_FILTER = IApplication.COMBOBOX_ENABLE_FILTER;

	/**
	 * Legacy Smart Client property, do not use anymore.
	 * Property that can be set using application.putClientProperty() or element.putClientProperty().
	 *
	 * If set on application it will affect all date formatted fields. If set on an element it will affect only that date formatted element/field (with priority over the application property).
	 * Value can be true/false/null.
	 *
	 * If set to true, only selected part of the date will be affected when using up/down keys to cycle through values. (for example, pressing up when cursor is on minutes and minutes shows 59 will not result in hour change)
	 * DEFAULT: false.
	 *
	 * @sample
	 * // make all date formatted fields use roll instead of add
	 * application.putClientProperty(UICONSTANTS.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD, true);
	 * // make one date formatted field use add instead of roll - overrides the application property set
	 * forms.someForm.elements.typeAheadElement.putClientProperty(UICONSTANTS.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD, false);
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public static final String DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD = IApplication.DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD;

	/**
	 * Property that can be set using application.putClientProperty() or element.putClientProperty().
	 *
	 * If set on application it will affect all date formatted fields. CAUTION: this property must be set on application before the fields are created (for example in solution onOpen handler). Changing it after fields were created will not affect these existing fields.
	 * If set on an element it will affect only that date formatted element/field (with priority over the application property).
	 * Value can be true/false/null.
	 *
	 * If set to false, date formatted fields will not allow input of out-of-bounds values (like 62 minutes means 2 minutes and +1 hour).
	 * DEFAULT: true.
	 *
	 * @sample
	 * // make all date formatted fields (created after this line is executed) not use lenient mode
	 * application.putClientProperty(UICONSTANTS.DATE_FORMATTERS_LENIENT, false);
	 * // make one date formatted field use lenient mode - overrides the application property set
	 * forms.someForm.elements.typeAheadElement.putClientProperty(UICONSTANTS.DATE_FORMATTERS_LENIENT, true);
	 *
	 * @deprecated not used in ngclient
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public static final String DATE_FORMATTERS_LENIENT = IApplication.DATE_FORMATTERS_LENIENT;

	/**
	 * Value that can be used to specify that a dialog should completely fill the screen.
	 *
	 * @deprecated because of a rewrite of dialogs/windows (based in JSWindow objects)
	 * @sample
	 * application.showFormInDialog('customers',UICONSTANTS.DIALOG_FULL_SCREEN,UICONSTANTS.DIALOG_FULL_SCREEN,
	 *      UICONSTANTS.DIALOG_FULL_SCREEN,UICONSTANTS.DIALOG_FULL_SCREEN,'My Title',true,true,'customers_dialog',true)
	 */
	@Deprecated
	public static final int DIALOG_FULL_SCREEN = IApplication.FULL_SCREEN;

	/**
	 * Value that can be used to specify that a dialog/window should completely fill the screen.
	 *
	 * @deprecated because of a rewrite of dialogs/windows (based in JSWindow objects)
	 * @sample
	 * application.showFormInDialog('customers',UICONSTANTS.FULL_SCREEN,UICONSTANTS.FULL_SCREEN,
	 *      UICONSTANTS.FULL_SCREEN,UICONSTANTS.FULL_SCREEN,'My Title',true,true,'customers_dialog',true)
	 */
	@Deprecated
	public static final int FULL_SCREEN = IApplication.FULL_SCREEN;

	/**
	 * Property that can be set using application.putClientProperty().
	 *
	 * If set to true, the system standard Printing dialog will be used when printing is needed.
	 * If set to false, the Servoy Printing dialog will be used.
	 *
	 * The value can be true/false/null.
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.USE_SYSTEM_PRINT_DIALOG, true)
	 *
	 * @deprecated not used in ngclient
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = false, sc = true)
	public static final String USE_SYSTEM_PRINT_DIALOG = IApplication.USE_SYSTEM_PRINT_DIALOG;

	/**
	 * Property that can be set using application.putClientProperty() and
	 * indicates the delay in milliseconds before the tooltip is shown.
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TOOLTIP_INITIAL_DELAY, 2000)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String TOOLTIP_INITIAL_DELAY = IApplication.TOOLTIP_INITIAL_DELAY;

	/**
	 * Property that can be set using application.putClientProperty() and
	 * indicates the delay in milliseconds after the tooltip is dismissed.
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TOOLTIP_DISMISS_DELAY, 4000)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String TOOLTIP_DISMISS_DELAY = IApplication.TOOLTIP_DISMISS_DELAY;

	/**
	 * Property that can be set using application.putClientProperty().
	 *
	 * If set to true, fields that are read-only won't be editable in find mode
	 * If set to false, fields that are read-only will be editable in find mode
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.LEAVE_FIELDS_READONLY_IN_FIND_MODE, true)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String LEAVE_FIELDS_READONLY_IN_FIND_MODE = IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE;

	/**
	 * Property that can be set using application.putClientProperty().
	 *
	 * If set to true, table views in web client are scrollable by default
	 * If set to false, table views in web client are not scrollable, but pageable by default
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * When this property is set to true, you can control the size of the page by setting the servoy property "servoy.webclient.scrolling.tableview.multiplier".
	 * The default value is 2. Setting the property to a higher value, will result in more data to be queried at once. You can also set it to a lower value, like 1 or 1.5 for example.
	 * We strongly recommend that the default or lower size be used in order to avoid blocking situations due to the big request being made to the server.
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TABLEVIEW_WC_DEFAULT_SCROLLABLE, true)
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = false)
	public static final String TABLEVIEW_WC_DEFAULT_SCROLLABLE = IApplication.TABLEVIEW_WC_DEFAULT_SCROLLABLE;

	/**
	 * Property that can be set using application.putClientProperty().
	 *
	 * If set to true, scrollable table views in web client will keep the already loaded rows in the view
	 * If set to false, scrollable table views in web client will unload not visible rows in the view
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TABLEVIEW_WC_SCROLLABLE_KEEP_LOADED_ROWS, true)
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = false)
	public static final String TABLEVIEW_WC_SCROLLABLE_KEEP_LOADED_ROWS = IApplication.TABLEVIEW_WC_SCROLLABLE_KEEP_LOADED_ROWS;

	/**
	 * Property that can be set using application.putClientProperty(). This property only works in NGClient (not in Titanium).
	 *
	 * If set to true, the tableview will be seens as fully readonly and NGClient will generate an optimized version (textfields are replaced)
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TABLEVIEW_NG_OPTIMIZED_READONLY_MODE, true)
	 *
	 * @deprecated not used in TiNG
	 */
	@Deprecated
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String TABLEVIEW_NG_OPTIMIZED_READONLY_MODE = IApplication.TABLEVIEW_NG_OPTIMIZED_READONLY_MODE;

	/**
	 * Property that can be set using application.putClientProperty(). This property only works in NGClient (not in Titanium).
	 *
	 * A number that defines the factor of what the next page size should be is in the tableview/listview/portal,
	 * this value is used to get the initial size (numerOfVisibleRows * thisPageSize).
	 * So a value of 2 (default) will load in 20 records if the number of visible rows is 10.
	 * Then if you scroll down the new set of records will be: (numberOfVisibleRows * thisPageSize) - numerOfVisibleRows
	 * so that will load for the default value 2, 1 page which is the number of visible rows (10 in this example).
	 *
	 * The value can be any number but it should be bigger then 1.
	 *
	 * WARNING the bigger the number, the more data is pushed initially to the client (and more is pushed in every new page)
	 *
	 * DEFAULT: 2
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TABLEVIEW_NG_PAGE_SIZE_FACTOR, 3)
	 *
	 *  @deprecated not used in TiNG
	 */
	@Deprecated
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String TABLEVIEW_NG_PAGE_SIZE_FACTOR = IApplication.TABLEVIEW_NG_PAGE_SIZE_FACTOR;

	/**
	 * Property that can be set using application.putClientProperty() or element.putClientProperty()..
	 *
	 * If set to true, any events of same type and on same component will be blocked (cancelled) until first event is finished.
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.NG_BLOCK_DUPLICATE_EVENTS, true)
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String NG_BLOCK_DUPLICATE_EVENTS = IApplication.BLOCK_DUPLICATE_EVENTS;

	/**
	 * Legacy Web Client property, do not use anymore.
	 *
	 * Property that can be set using application.putClientProperty().
	 *
	 * If set to true, you can change selection in webclient tableview using up/down keys
	 * If set to false, you cannot change selection via keyboard arrows
	 *
	 * The value can be true/false
	 * DEFAULT: true
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.TABLEVIEW_WC_USE_KEY_NAVIGATION, false)
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = false)
	public static final String TABLEVIEW_WC_USE_KEY_NAVIGATION = IApplication.TABLEVIEW_WC_USE_KEY_NAVIGATION;

	/**
	 * Property that can be set using element.putClientProperty()
	 *
	 * If set to true, the element will accept javascript links in the input
	 * If set to false, all 'javascript:' texts will be removed from the input
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * element.putClientProperty(UICONSTANTS.ALLOW_JAVASCRIPT_LINK_INPUT, true)
	 *
	 * @deprecated not used in ngclient
	 */
	@Deprecated
	@ServoyClientSupport(ng = false, wc = true, sc = true)
	public static final String ALLOW_JAVASCRIPT_LINK_INPUT = IApplication.ALLOW_JAVASCRIPT_LINK_INPUT;

	/**
	 * Property that can be set using element.putClientProperty() or application.putClientProperty()
	 *
	 * If set to true, data showed on elements like buttons or labels will not be sanitized.
	 *
	 * Showing unsanitized data can make the system vulnerable to XSS attacks, for example, an
	 * user registers with name 'John Doe<script>someEvilJavascript</script>', when this data is shown in a label (by another user)
	 * the javascript in the script tags will be executed.
	 *
	 * Only enable this setting if the data shown can always be trusted and is never composed of data from an external system or user.
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * element.putClientProperty(UICONSTANTS.TRUST_DATA_AS_HTML, true)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String TRUST_DATA_AS_HTML = IApplication.TRUST_DATA_AS_HTML;

	/**
	 * Property that can be set on editable html area using element.putClientProperty()
	 *
	 * The value must be a valid json string according to TinyMCE editor configuration (http://www.tinymce.com/wiki.php/configuration).
	 * It will be used to override/set configuration properties in order to customize the editor.
	 *
	 *
	 * @sample
	 * // adding a new TinyMCE plugin (same for skin or theme)
	 * // this code assumes plugin.min.js was copied in web server at specified path
	 * // NOTE: we use external plugin, not plugin, in order for file to be accessible from web server; for this example, pluging.min.js file must be copied under ServoyInstall/application_server/server/webapps/ROOT/tinymce/plugins/link
	 * element.putClientProperty(UICONSTANTS.HTML_EDITOR_CONFIGURATION, '{"external_plugins": { "link": "../../../tinymce/plugins/link/plugin.min.js"}}')
	 *
	 * // change the editor configuration (add menubar, status bar and change toolbar)
	 * element.putClientProperty(UICONSTANTS.HTML_EDITOR_CONFIGURATION, '{"menubar": "tools table format view insert edit", "statusbar" : true, "toolbar": "undo redo | styleselect | bold italic"}')
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = false)
	public static final String HTML_EDITOR_CONFIGURATION = IApplication.HTML_EDITOR_CONFIGURATION;

	/**
	 * Property that can be set using application.putClientProperty(), this has no meaning on an component
	 *
	 * If set to true, related find/search will only return records that have a related match, also in case of left outer joins.
	 * Otherwise a related search on a field may return records where the related search does not match.
	 *
	 * For example,
	 * * <pre>
	 *     if (foundset.find()) {
	 *      foundset.myleftouterjoinrelation.myfield = 'someval';
	 *      foundset.search();
	 *     }
	 * </pre>
	 * With this setting to false records of the foundset table that have no related records via the relation will also be returned.
	 *
	 * The value can be true/false
	 * DEFAULT: servoy property servoy.client.relatedNullSearchAddPkCondition/true
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.RELATED_NULL_SEARCH_ADD_PK_CONDITION, false)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String RELATED_NULL_SEARCH_ADD_PK_CONDITION = IApplication.RELATED_NULL_SEARCH_ADD_PK_CONDITION;

	/**
	 * Property that can be set using application.putClientProperty()
	 *
	 * The value can be a positive integer representing the maximum number of rows that will be retrieved by query in database or related valuelist.
	 * Can have a maximum value of 1000.
	 *
	 * DEFAULT: 500
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.VALUELIST_MAX_ROWS, 1000)
	 */
	@ServoyClientSupport(ng = true, wc = true, sc = true)
	public static final String VALUELIST_MAX_ROWS = IApplication.VALUELIST_MAX_ROWS;

	/**
	 * Property that can be set using application.putClientProperty(), preferably in solution onOpen handler (or anyway before forms containing calendars are shown).
	 *
	 * If set to true, the default calendar, bootstrap calendar, bootstrap inline calendar and nggrid calendar will show week number according to ISO 8601. Other 3rd party (calendar) components are free to take this value into consideration as they please.
	 * By default (false) those will show week number according to locale.
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 * NOTE: In Titanium Client this value is by default true and currently cannot be changed.
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.CALENDAR_NG_SHOW_ISO_WEEK_NUMBER, true)
	 *
	 * @deprecated this is replaced for TiNG with i18n.setFirstDayOfTheWeek(1);
	 */
	@Deprecated
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String CALENDAR_NG_SHOW_ISO_WEEK_NUMBER = IApplication.CALENDAR_NG_SHOW_ISO_WEEK_NUMBER;


	/**
	 * Property that can be set using application.putClientProperty(), preferably in solution onOpen handler (or before showing the ui components that formats numbers).
	 *
	 * If set this will be used as the grouping symbol for all number formatted fields that uses the default servoy formatting services (so 3rd party components that they there own formatting could/will ignore this)
	 *
	 * The value can be any kind of character (string)
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.FORMAT_GROUPING_SYMBOL, ' '); // make the grouping symbol a space
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String FORMAT_GROUPING_SYMBOL = IApplication.FORMAT_GROUPING_SYMBOL;

	/**
	 * Property that can be set using application.putClientProperty(), preferably in solution onOpen handler (or before showing the ui components that formats numbers).
	 *
	 * If set this will be used as the grouping symbol for all number formatted fields that uses the default servoy formatting services (so 3rd party components that they there own formatting could/will ignore this)
	 *
	 * The value can be any kind of character (string)
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.FORMAT_DECIMAL_SYMBOL, ';'); // make the grouping symbol a semicolon
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String FORMAT_DECIMAL_SYMBOL = IApplication.FORMAT_DECIMAL_SYMBOL;


	/**
	 * Property that can be set using application.putClientProperty(). This property only works in Titanium.
	 * Property should be set onSolutionOpen or onLoad of the form (before form is shown in client).
	 * If set to true, the listformcomponent will use the old paging mode for display.
	 *
	 * The value can be true/false
	 * DEFAULT: false
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.LISTFORMCOMPONENT_PAGING_MODE, true)
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String LISTFORMCOMPONENT_PAGING_MODE = IApplication.LISTFORMCOMPONENT_PAGING_MODE;


	/**
	 * Sets the default value (true/false) that should be used when showing a form in a container.
	 * Default this is true when the forms useMinHeight property is unset,
	 * that means the css minHeight is set for this form at runtime.
	 *
	 * You can set the default value to false for all forms that don't have that property set,
	 * so that no minHeigth is generated for those forms.
	 *
	 * The value can be true/false
	 * DEFAULT: true
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.DEFAULT_FORM_USE_MIN_HEIGHT, false)
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String DEFAULT_FORM_USE_MIN_HEIGHT = "DEFAULT_FORM_USE_MIN_HEIGHT";


	/**
	 * Sets the default value (true/false) that should be used when showing a form in a container.
	 * Default this is true when the forms useMinWidth property is unset,
	 * that means the css minWidth is set for this form at runtime.
	 *
	 * You can set the default value to false for all forms that don't have that property set,
	 * so that no minWidth is generated for those forms.
	 *
	 * The value can be true/false
	 * DEFAULT: true
	 *
	 * @sample
	 * application.putClientProperty(UICONSTANTS.DEFAULT_FORM_USE_MIN_WIDTH, false)
	 */
	@ServoyClientSupport(ng = true, wc = false, sc = false)
	public static final String DEFAULT_FORM_USE_MIN_WIDTH = "DEFAULT_FORM_USE_MIN_WIDTH";

	public String getPrefix()
	{
		return "UICONSTANTS";
	}

	@Override
	public String toString()
	{
		return "Properties that alter L&F of UI components";
	}

}
