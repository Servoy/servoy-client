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
package com.servoy.j2db.plugins;


import java.awt.Component;
import java.awt.Window;
import java.net.URL;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IBeanManager;
import com.servoy.j2db.IFormManager;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.IDatabaseManager;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.IUIBlocker;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * Default client plugin api
 * 
 * @author Jan Blok
 */
public interface IClientPluginAccess extends IPluginAccess, IUIBlocker, ILogLevel
{
	/**
	 * Constant returned by getApplicationType
	 */
	public static final int CLIENT = IApplication.CLIENT;

	/**
	 * Constant returned by getApplicationType
	 */
	public static final int WEB_CLIENT = IApplication.WEB_CLIENT;

	/**
	 * Constant returned by getApplicationType
	 */
	public static final int HEADLESS_CLIENT = IApplication.HEADLESS_CLIENT;


	/**
	 * Constant returned by getApplicationType
	 * 
	 * @deprecated
	 * @since Servoy 3.5
	 */
	@Deprecated
	public static final int DEVELOPER = IApplication.CLIENT;

	/**
	 * Report a warning in the status (will be shown in red). <br>
	 * <b>Note:</b>Status will be cleared automatically
	 * 
	 * @param s the warning
	 */
	public void reportWarningInStatus(String s);

	/**
	 * Get the application type. Will return one of the client constants like {@link #CLIENT} for the smart client.
	 * 
	 * @return int the type
	 */
	public int getApplicationType();

	/**
	 * Returns whether or not this client is running in the developer.
	 * 
	 * @return true if this client is running in developer
	 */
	public boolean isInDeveloper();


	/**
	 * Get the application name.
	 * 
	 * @return String
	 */
	public String getApplicationName();

	/**
	 * Map containing runtime properties (will never be stored, and live one session)
	 * 
	 * @since Servoy 3.5
	 */
	public Map getRuntimeProperties();

	/**
	 * ResourceBundle containing all the messages of the current loaded solution.
	 * 
	 * @since Servoy 3.5.3
	 * @param locale The locale for which the resource bundle must be created, null if it has to use the default from the client.
	 */
	public ResourceBundle getResourceBundle(Locale locale);

	/**
	 * set a status text in the status area.
	 */
	public void setStatusText(String txt);

	/**
	 * Get the userUID.
	 * 
	 * @return int the user id, null if not logged in
	 */
	public Object getUserUID();

	/**
	 * Get the clientID.
	 * 
	 * @return String the client id, for use in dataserver.
	 */
	public String getClientID();

	/**
	 * @see IDatabaseManager
	 * @deprecated
	 */
	@Deprecated
	public String getTransactionID(String serverName);

	/**
	 * Get the database manager (used to control the rows/transactions etc).
	 * 
	 * @return the database manager
	 */
	public IDatabaseManager getDatabaseManager();

	/**
	 * Get the form manager (used to control the forms, show etc). Note: the form manager can be casted to a ISwingFormManager for more usage
	 * 
	 * @return the form manager
	 */
	public IFormManager getFormManager();

	/**
	 * Get a message for the specified i18n key
	 * 
	 * @param i18nKey The key
	 * @param arguments Arguments to be used that are inserted in the found message (null if no args).
	 * @return the string if the key is found.
	 */
	public String getI18NMessage(String i18nKey, Object[] arguments);

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
	 * Get the import menu, used by plugins to add import menu items (actions).
	 * 
	 * @return JMenu
	 */
	public JMenu getImportMenu();

	/**
	 * Get the export menu, used by plugins to add export menu items (actions).
	 * 
	 * @return JMenu
	 */
	public JMenu getExportMenu();

	/**
	 * Get the current showing window (Dialog or Frame) Use this one as a parent if you want to display a dialog or other frame.
	 * 
	 * @return Window
	 */
	public Window getCurrentWindow();

	/**
	 * Gets the java Window for the JS window or JS dialog with the given name.
	 * @param windowName the name of the window as given when opening a window or dialog from JS. If windowName is null, the main application frame is returned.
	 * @return the dialog or frame instance that corresponds to the given window name.
	 */
	public Window getWindow(String windowName);

	/**
	 * Get the toolbar panel (used to control toolbars).
	 * 
	 * @return IToolbarPanel
	 */
	public IToolbarPanel getToolbarPanel();

	/**
	 * Output something on the out stream and in log. (if running in debugger view console)
	 * 
	 * @param msg
	 */
	public void output(Object msg);

	/**
	 * Output something on the out stream and in log. (if running in debugger view console)
	 * 
	 * @param msg
	 * @param level the severity level
	 * @see ILogLevel
	 * @since 5.0
	 */
	public void output(Object msg, int level);

	/**
	 * Get the http server url
	 */
	public URL getServerURL();

	/**
	 * Get the current solution name
	 * 
	 * @return the name, returns null if no solution is open
	 * @since 2.2rc4
	 */
	public String getSolutionName();

	/**
	 * Get a remote service, will not work in the Servoy Runtime product!
	 */
	public Remote getServerService(String name) throws Exception;

	/**
	 * Register a URLStreamHandler for a protocol
	 * 
	 * @param protocolName
	 * @param handler
	 */
	public void registerURLStreamHandler(String protocolName, URLStreamHandler handler);

	/**
	 * Returns a URLStreamHandler for handling servoy urls ('media' protocol). 
	 * Use this when you construct urls to those by using one of the URL constructors:
	 * {@link URL#URL(URL, String, URLStreamHandler)} or {@link URL#URL(String, String, int, String, URLStreamHandler)} 
	 *
	 * @return The URLStreamHandler for the protocol 'media'
	 */
	public URLStreamHandler getMediaURLStreamHandler();

	/**
	 * Executes a method with method name and arguments in the given context If context is a form name a form method is executed, if null a global method will
	 * be assumed.
	 * 
	 * @see com.servoy.j2db.scripting.FunctionDefinition
	 * 
	 * @param context This is the form name or null if the method is a global method.
	 * @param methodname The method name to be executed
	 * @param arguments The arguments that the method will get.
	 * @param async Execute the method asynchronously or not. If true this method will return immediately with null.
	 * 
	 * @return null if called with "true" for async flag, otherwise the method result is returned
	 */
	public Object executeMethod(String context, String methodname, Object[] arguments, boolean async) throws Exception;

	/**
	 * Invoke the Servoy solution error handler with a msg and exception. Shows a dialog if no solution error handler present (reports in log for
	 * headless/web-client)
	 * 
	 * @param msg
	 * @param detail
	 * @since 3.5
	 */
	public void handleException(String msg, Exception detail);

	/**
	 * Opens the file dialog on the client so that a user can upload a file.
	 * That file will be given as an argument to the given function.
	 * 
	 * Only the {@link IMediaUploadCallback} is mandatory and will be used, the other arguments can be null and can be ignored by the implementation. 
	 * 
	 * @param callback The {@link IMediaUploadCallback} for the call back.
	 * @param fileNameHint A String which file the user should open.
	 * @param multiSelect Multi select files at once.
	 * @param filter A string array of filenames that are allowed.
	 * @param selection A {@link JFileChooser} constant if it should select directories or files.
	 * @param dialogTitle The dialog title.
	 */
	public void showFileOpenDialog(IMediaUploadCallback callback, String fileNameHint, boolean multiSelect, String[] filter, int selection, String dialogTitle);

	/**
	 * exports a remote object on the client that can be transfered to the server (with a remote server call) that can have call backs.
	 *  
	 * @param object The remote object to export
	 * @since 6.0
	 */
	public void exportObject(Remote object) throws Exception;

	/**
	 * Executes a method with methodname and arguments (async).
	 * 
	 * @deprecated
	 * @since 3.5
	 */
	@Deprecated
	public void executeMethod(String formname, String methodname, Object[] arguments);

	/**
	 * @deprecated
	 * @since 3.5 Report an error when in a dialog. (use handleException)
	 * @param parentComponent
	 * @param msg
	 * @param detail
	 */
	@Deprecated
	public void reportError(Component parentComponent, String msg, Object detail);

	/**
	 * Get the main application frame.
	 * 
	 * @deprecated use getCurrentWindow if possible (gives the real current window)
	 * @return JFrame
	 */
	@Deprecated
	public JFrame getMainApplicationFrame();

//	/**
//	 * Get the foundset manager (used to control the forms datasets).
//	 * @return IFoundSetManager
//	 */
//	public IFoundSetManager getFoundSetManager();

	/**
	 * Get a message for the specified i18n key
	 * 
	 * @param i18nKey The key
	 * @return the string if the key is found.
	 * @deprecated
	 */
	@Deprecated
	public String getMessage(String i18nKey);

	/**
	 * Get a message for the specified i18n key
	 * 
	 * @param i18nKey The key
	 * @param arguments Arguments to be used that are inserted in the found message.
	 * @return the string if the key is found.
	 * @deprecated
	 */
	@Deprecated
	public String getMessage(String i18nKey, Object[] arguments);

	/**
	 * @deprecated Get the userID.
	 * @return int the user id
	 */
	@Deprecated
	public Object getUserID();
}
