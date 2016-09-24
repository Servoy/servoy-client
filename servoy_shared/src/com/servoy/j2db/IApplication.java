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
package com.servoy.j2db;


import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.rmi.Remote;
import java.util.Locale;
import java.util.ResourceBundle;

import com.servoy.base.persistence.constants.IBaseApplicationTypes;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.ClientInfo;
import com.servoy.j2db.dataprocessing.DataServerProxy;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.plugins.IPluginAccess;
import com.servoy.j2db.plugins.IPluginManager;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.ui.ItemFactory;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.RendererParentWrapper;

/**
 * Main interface for the client application.
 *
 * @author jblok
 */
public interface IApplication extends IBasicApplication, IServiceProvider, ILogLevel
{
	public static final int SERVER = 1;
	public static final int CLIENT = IBaseApplicationTypes.SMART_CLIENT; //smart, rich
	public static final int HEADLESS_CLIENT = IBaseApplicationTypes.HEADLESS_CLIENT;
	public static final int WEB_CLIENT = IBaseApplicationTypes.WEB_CLIENT;
	public static final int NG_CLIENT = IBaseApplicationTypes.NG_CLIENT;
	public static final int RUNTIME = IBaseApplicationTypes.RUNTIME_CLIENT;
	public static final int OFFLINE = 7;
	public static final int MOBILE = IBaseApplicationTypes.MOBILE_CLIENT;
	public static final int TYPES_COUNT = 9;

	// UI properties defined/used by Servoy
	/**
	 * If Boolean.TRUE type-ahead fields will show the popup even when the value is empty.
	 */
	public static final String TYPE_AHEAD_SHOW_POPUP_WHEN_EMPTY = "TypeAhead.showPopupWhenEmpty"; //$NON-NLS-1$
	/**
	 * If Boolean.TRUE type-ahead fields will show the popup when they gain focus.
	 */
	public static final String TYPE_AHEAD_SHOW_POPUP_ON_FOCUS_GAIN = "TypeAhead.showPopupOnFocusGain"; //$NON-NLS-1$
	/**
	 * If Boolean.TRUE combobox fields will show the popup when they gain focus.
	 */
	public static final String COMBOBOX_SHOW_POPUP_ON_FOCUS_GAIN = "Combobox.showPopupOnFocusGain"; //$NON-NLS-1$
	/**
	 * If Boolean.FALSE, the combobox search box is hidden. NGClient only.
	 */
	public static final String COMBOBOX_ENABLE_FILTER = "Combobox.enableFilter"; //$NON-NLS-1$
	/**
	 * When Boolean.TRUE, only selected part of the date formatted field will be affected when using up/down keys to cycle through values. (for example, pressing up when cursor is on minutes and minutes shows 59 will not result in hour change)
	 */
	public static final String DATE_FORMATTERS_ROLL_INSTEAD_OF_ADD = "DateLNF.rollInsteadOfAdd"; //$NON-NLS-1$
	/**
	 * When Boolean.FALSE, date formatted fields will not allow input of out-of-bounds values (like 62 minutes means 2 minutes and +1 hour).
	 */
	public static final String DATE_FORMATTERS_LENIENT = "DateLNF.lenient"; //$NON-NLS-1$
	/**
	 * Value that indicates that a dialog/window should completely fill the screen.
	 */
	public static final int FULL_SCREEN = -42;
	/**
	 * When Boolean.TRUE, the system standard Print dialog is used when printing; if Boolean.FALSE, the Servoy Print dialog will be used.
	 */
	public static final String USE_SYSTEM_PRINT_DIALOG = "useSystemPrintDialog"; //$NON-NLS-1$
	/**
	 * Value that indicates the delay in milliseconds before the tooltip is shown.
	 */
	public static final String TOOLTIP_INITIAL_DELAY = "tooltipInitialDelay"; //$NON-NLS-1$
	/**
	 * Value that indicates the delay in milliseconds after the tooltip is dismissed.
	 */
	public static final String TOOLTIP_DISMISS_DELAY = "tooltipDismissDelay"; //$NON-NLS-1$
	/**
	 * When Boolean.TRUE, fields that are read-only won't be editable in find mode
	 */
	public static final String LEAVE_FIELDS_READONLY_IN_FIND_MODE = "leaveFieldsReadOnlyInFindMode"; //$NON-NLS-1$
	/**
	 * When Boolean.TRUE, table views in web client are by default scrollables
	 */
	public static final String TABLEVIEW_WC_DEFAULT_SCROLLABLE = "webClientTableViewScrollable"; //$NON-NLS-1$
	/**
	 * When Boolean.TRUE, scrollable table views in web client will keep not visible but loaded rows in the view
	 */
	public static final String TABLEVIEW_WC_SCROLLABLE_KEEP_LOADED_ROWS = "webClientTableViewScrollableKeepLoadedRows"; //$NON-NLS-1$
	/**
	 * When Boolean.TRUE, you can use arrow up and down keys to change selection in tableview
	 */
	public static final String TABLEVIEW_WC_USE_KEY_NAVIGATION = "webClientTableViewUseKeyNavigation"; //$NON-NLS-1$

	/**
	 * When Boolean.TRUE, the tableview will be seen as fully readonly and will generate for the ngclient an optimized version.
	 */
	public static final String TABLEVIEW_NG_OPTIMIZED_READONLY_MODE = "ngClientOptimizedReadonlyMode"; //$NON-NLS-1$

	/**
	 * When Boolean.TRUE, any subsequent events on same element and having same type will be blocked (cancelled).
	 */
	public static final String BLOCK_DUPLICATE_EVENTS = "ngBlockDuplicateEvents"; //$NON-NLS-1$

	/**
	 * A number that defines the factor of what the next page size should be is in the tableview/listview/portal,
	 * this value is used to get the initial size (numerOfVisibleRows * thisPageSize).
	 * So a value of 2 (default) will load in 20 records if the number of visible rows is 10.
	 * Then if you scroll down the new set of records will be: (numberOfVisibleRows * thisPageSize) - numerOfVisibleRows
	 * so that will load for the default value 2, 1 page which is the number of visible rows (10 in this example).
	 */
	public static final String TABLEVIEW_NG_PAGE_SIZE_FACTOR = "ngClientPageSizeFactor"; //$NON-NLS-1$

	/**
	 * When Boolean.TRUE, component will accept javascript links in the input
	 */
	public static final String ALLOW_JAVASCRIPT_LINK_INPUT = "allowJavascriptLinkInput"; //$NON-NLS-1$

	/**
	 * When Boolean.TRUE, the element may show unsanitized data.
	 */
	public static final String TRUST_DATA_AS_HTML = "trustDataAsHtml"; //$NON-NLS-1$

	/**
	 * The WebClient JSON configuration for the HTML Editor.
	 */
	public static final String HTML_EDITOR_CONFIGURATION = "config"; //$NON-NLS-1$

	/**
	 * Value that indicates maximum number of rows that will be queried in database or related valuelist. Can be between 1 and 1000. Default value is 500.
	 */
	public static final String VALUELIST_MAX_ROWS = "maxValuelistRows";//$NON-NLS-1$

	/**
	 * Non-null name for the first (main) window.
	 */
	public static final String APP_WINDOW_NAME = "Application_frame";

	/**
	 * Get the type of the application, will return one of the client constants like {@link #CLIENT} for the smart client.
	 *
	 * @return the type of app
	 */
	public int getApplicationType();

	/**
	 * Get the Operation System name where the client runs in like "Windows 7"
	 *
	 * @return the Operation System name where the client runs in.
	 */
	public String getClientOSName();

	/**
	 * Get the platform of the client, local platform for smart client, browser platform for web client.
	 */
	public int getClientPlatform();

	/**
	 * Show progress in status progress bar. <br>
	 * <b>Note:</b>if blockGUI(...) is called this info is lost
	 *
	 * @param progress the progress between (0-100)
	 */
	public void setStatusProgress(int progress);

	/**
	 * Show a status, better to use blockGUI(...). <br>
	 * <b>Note:</b>if blockGUI(...) is called this info is lost
	 *
	 * @param text the text to show
	 */
	public void setStatusText(String text, String tooltip);

	/**
	 * Update solution loading status/UI.
	 */
	public void showSolutionLoading(boolean loading);

	/**
	 * Get the form manager (used to control the forms, show etc).
	 *
	 * @return IFormManager
	 */
	public IBasicFormManager getFormManager();

	/**
	 * Get the cmd manager (used to execute undoable cmd).
	 *
	 * @return ICmdManager
	 */
	public ICmdManager getCmdManager();

	/**
	 * Get the bean manager (used to control beans).
	 *
	 * @return IBeanManager
	 */
	public IBeanManager getBeanManager();

	/**
	 * Get the plugin manager (used to control plugins).
	 *
	 * @return IPluginManager
	 */
	public IPluginManager getPluginManager();

	/**
	 * Get the mode manager (used to control the form modes).
	 *
	 * @return IModeManager
	 */
	public IModeManager getModeManager();

	/**
	 * Get the user manager (used to do authentication and security)
	 *
	 * @return
	 */
	public IUserManager getUserManager();

	/**
	 * Get the application name.
	 *
	 * @return String
	 */
	public String getApplicationName();

	/**
	 * Set a UI property.
	 *
	 * @param name of property
	 * @see javax.swing.UIDefaults (for swing side)
	 */
	public boolean putClientProperty(Object name, Object val);

	public Object getClientProperty(Object key);

	/**
	 * Set the window title.
	 *
	 * @param title to show
	 */
	public void setTitle(String title);

	/**
	 * Get the Look and Feel manager (used to set the laf on the application).
	 *
	 * @return ILAFManager
	 */
	public ILAFManager getLAFManager();

	/**
	 * Output something on the out stream. (if running in debugger view output tab)
	 *
	 * @param msg
	 */

	public void output(Object msg, int level);


	/**
	 * Call authenticator module
	 *
	 * @param authenticator_solution authenticator solution name, null for built-in Servoy authentication
	 * @param method
	 * @param credentials
	 *
	 * @return result from authenticator solution
	 */
	public Object authenticate(String authenticator_solution, String method, Object[] credentials) throws RepositoryException;

	/**
	 * Perform user logout
	 *
	 * @param solution_to_open_args
	 */
	public void logout(Object[] solution_to_open_args);

	/**
	 * Close the solution, you may pass info to open a new solution
	 *
	 * @param force, close a solution with force
	 * @param args, pass solutionName,solutionStartupMethod,methodArgument as array
	 * @return true if successful
	 */
	public boolean closeSolution(boolean force, Object[] args);

	/**
	 * get the clientinfo object
	 *
	 * @return the client info
	 */
	public ClientInfo getClientInfo();

	/**
	 * Get the factory to create new ui components
	 */
	public ItemFactory getItemFactory();

	/**
	 * Get the factory to create new data renderers
	 */
	public IDataRendererFactory getDataRenderFactory();

	/**
	 * Get the plugin access for an application type
	 */
	public IPluginAccess getPluginAccess();

	/**
	 * A parent to render with
	 */
	public RendererParentWrapper getPrintingRendererParent();

	/**
	 * Get the current page format.
	 *
	 * @return PageFormat the page format
	 */
	public PageFormat getPageFormat();

	public void setPageFormat(PageFormat currentPageFormat);

	/**
	 * Get clear of the login form, since this call indicated a succesfull login was done on security scripting object.
	 */
	public void clearLoginForm();

	/**
	 * gets an user property for the current session/user. Implementations should store this for the current session.
	 *
	 * @param name The name of the property to get.
	 * @return The property value if found.
	 */
	public String getUserProperty(String name);

	/**
	 * sets an user property for the current session/user. Implementations should store this for the current session.
	 *
	 * @param name The name of the property to be set.
	 * @param value The value to set (null is remove).
	 */
	public void setUserProperty(String name, String value);

	/**
	 * get all the user property names for the current session/user.
	 */
	public String[] getUserPropertyNames();

	/**
	 * User uid changed (usually user logged in or out)
	 * @param userUidBefore
	 * @param userUidAfter
	 */
	public void handleClientUserUidChanged(String userUidBefore, String userUidAfter);

	/**
	 * Get a remote service
	 * @return the remote server object
	 */
	public Remote getServerService(String name);

	/**
	 * Set a message filter
	 */
	public void setI18NMessagesFilter(String columnname, String[] value);

	/**
	 * Get a locale resouce bundle
	 * @return the resouce bundle
	 */
	public ResourceBundle getResourceBundle(Locale locale);

	/**
	 * Get the screensize
	 * @return the dimension
	 */
	public Dimension getScreenSize();

	/**
	 * Show an url in a browser
	 * @param url the url to show
	 * @param target the target
	 * @param target_options the options
	 * @param timeout
	 * @return true is successful
	 */
	public boolean showURL(String url, String target, String target_options, int timeout, boolean onRootFrame);

	/**
	 * Test if this client is running from developer
	 * @return the dev status
	 */
	public boolean isInDeveloper();

	/**
	 * Test if this client in in shutdown
	 * @return the shutdown status
	 */
	public boolean isShutDown();

	/**
	 * Delivers a proxied version of IDataServer interface for use with switchServer.
	 * Which is also be returned from getDataServer() then as well, after the call.
	 * @return the proxy
	 */
	public DataServerProxy getDataServerProxy();

	/**
	 * Get the window manager
	 * @return the manager
	 */
	public RuntimeWindowManager getRuntimeWindowManager();

	/**
	 * Get the solution name
	 * @return the solution name
	 */
	public String getSolutionName();

	/**
	 * Loose the focus, helpful to get an cursor out of the fields.
	 */
	public void looseFocus();

	/**
	 * @param time
	 */
	public void updateUI(int time);

	/**
	 * @param name
	 * @param displayValues
	 * @param realValues
	 * @param autoconvert
	 */
	public void setValueListItems(String name, Object[] displayValues, Object[] realValues, boolean autoconvert);

	/**
	 * Report a javascript error.
	 *
	 * @param msg the error
	 * @param detail the detail (can be exception obj)
	 */
	public void reportJSError(String msg, Object detail);

	/**
	 * Report a javascript warning.
	 *
	 * @param msg the warning
	 */
	public void reportJSWarning(String msg);

	/**
	 * Report a javascript warning with stack trace.
	 *
	 * @param msg the warning
	 * @param t the stack to report
	 */
	public void reportJSWarning(String msg, Throwable t);

	/**
	 * Report a javascript info.
	 *
	 * @param msg the info
	 */
	public void reportJSInfo(String msg);


}
