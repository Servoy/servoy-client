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


import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;

import com.servoy.base.util.I18NProvider;
import com.servoy.j2db.dataprocessing.IClientHost;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.server.shared.IApplicationServer;

/**
 * Interface for minimal service provider.
 *
 * @author jblok
 */
public interface IServiceProvider extends IEventDelegator, I18NProvider
{
	public static final String RT_OVERRIDESTYLE_CACHE = "overridestylecache";
	public static final String RT_VALUELIST_CACHE = "valuelistcache";

	public static final String RT_PRINTING_FLAG = "printing";
	public static final String RT_LASTFIELDVALIDATIONFAILED_FLAG = "lastFieldValidationFailed";

	public static final String RT_OPEN_METHOD_RESULT = "openMethodResult";

	public static final String RT_JSDATASET_FUNCTIONS = "JSDataSetFunctions";
	public static final String RT_JSFOUNDSET_FUNCTIONS = "JSFoundSetFunctions";
	public static final String RT_JSRECORD_FUNCTIONS = "JSRecordFunctions";

	/**
	 * Get the repository interface.
	 *
	 * @return IRepository
	 */
	public IRepository getRepository();

	/**
	 * Is the repository accessible?
	 */
	public boolean haveRepositoryAccess();

	/**
	 * Get the application server interface.
	 *
	 * @return IApplicationServer
	 */
	public IApplicationServer getApplicationServer();

	/**
	 * Get the data server interface.
	 *
	 * @return IDataServer
	 */

	public IDataServer getDataServer();

	/**
	 * Get the client host interface.
	 *
	 * @return IClientHost
	 */
	public IClientHost getClientHost();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @return Solution
	 * @exclude
	 */
	public Solution getSolution();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @return boolean
	 * @exclude
	 */
	public boolean isSolutionLoaded();

	/**
	 * FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @return Root
	 * @exclude
	 */
	public FlattenedSolution getFlattenedSolution();

	/**
	 * Handle exceptions via (solution) errorhandler or show a dialog if non existent
	 */
	public void handleException(String servoyMsg, Exception e);

	/**
	 * Report an error.
	 *
	 * @param msg
	 * @param detail
	 */
	public void reportError(String msg, Object detail);

	/**
	 * Report an info.
	 *
	 * @param msg
	 */
	public void reportInfo(String msg);

	/**
	 * Report a warning
	 *
	 * @param msg the warning
	 */
	public void reportWarning(String msg);

	/**
	 * Get the script engine
	 *
	 * @return IExecutingEnviroment
	 */
	public IExecutingEnviroment getScriptEngine();

	/**
	 * Get the scheduled executor.
	 *
	 * @return ScheduledExecutorService
	 */
	public ScheduledExecutorService getScheduledExecutor();

	/** TODO: move to IApplication
	 * Report a javascript error.
	 *
	 * @param msg the error
	 * @param detail the detail (can be exception obj)
	 */
	public void reportJSError(String msg, Object detail);

	/**
	 * Get the userID.
	 *
	 * @return String the user uid, null if not logged in
	 */
	public String getUserUID();

	/**
	 * Get the userName.
	 *
	 * @return int the user name, null if not logged in
	 */
	public String getUserName();

	/**
	 * Get the clientID.
	 *
	 * @return String the client id, for use in dataserver.
	 */
	public String getClientID();

	/**
	 * Get the client's locale.
	 *
	 * @return
	 */
	public Locale getLocale();

	/**
	 * Get the client's timezone.
	 *
	 * @return String the client id, for use in dataserver.
	 */
	public TimeZone getTimeZone();

	/**
	 * Get the user properties.
	 *
	 * @return Properties
	 */
	public Properties getSettings();

	/**
	 * Get non stored properties, specific to a client.
	 *
	 * @return Properties
	 */
	public Map getRuntimeProperties();

	/**
	 * Get the foundset manager (used to control the forms datasets).
	 *
	 * @return IFoundSetManager
	 */
	public IFoundSetManagerInternal getFoundSetManager();

	/**
	 * Tells if this process is running on the server or remote.
	 *
	 * @return
	 */
	public boolean isRunningRemote();

	/**
	 * Get the server url, returns localhost URL is not running remote.
	 *
	 * @return
	 */
	public URL getServerURL();

	/**
	 * Set the locale.
	 *
	 * @param locale The locale to set.
	 */
	public void setLocale(Locale locale);

	/**
	 * Set the timezone
	 * @param timezone
	 */
	public void setTimeZone(TimeZone timeZone);

}
