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


import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import com.servoy.j2db.util.ITaskExecuter;
import com.servoy.j2db.util.Utils;

/**
 * Default plugin api
 * 
 * @author jblok
 */
public interface IPluginAccess
{
	/**
	 * Constant returned by getPlatform
	 */
	public static final int PLATFORM_WINDOWS = Utils.PLATFORM_WINDOWS;
	/**
	 * Constant returned by getPlatform
	 */
	public static final int PLATFORM_MAC = Utils.PLATFORM_MAC;
	/**
	 * Constant returned by getPlatform
	 */
	public static final int PLATFORM_LINUX = Utils.PLATFORM_LINUX;
	/**
	 * Constant returned by getPlatform
	 */
	public static final int PLATFORM_OTHER = Utils.PLATFORM_OTHER;

	/**
	 * Get the application version.
	 * 
	 * @return String the version
	 */
	public String getVersion();

	/**
	 * Get the application release number.
	 * 
	 * @return int the release number
	 * @since 3.5.1
	 */
	public int getReleaseNumber();

	/**
	 * Get the user properties (when setting something prefix the key with 'plugin.pluginname.').
	 * 
	 * @return Properties
	 */
	public Properties getSettings();

	/**
	 * Get the task executor.
	 * 
	 * @return Executor
	 */
	public ScheduledExecutorService getExecutor();

	/**
	 * Get the manager which handles the plugins
	 * 
	 * @since 3.5
	 */
	public IPluginManager getPluginManager();

	/**
	 * Get the platform (Operating system).
	 * 
	 * @since 5.0
	 * @return the platform constant
	 */
	public int getPlatform();

	/**
	 * Use Executor
	 * 
	 * @deprecated
	 */
	@Deprecated
	public ITaskExecuter getThreadPool();

	/**
	 * Report an error.
	 * 
	 * @param msg
	 * @param detail
	 * @deprecated
	 * @since 3.5
	 */
	@Deprecated
	public void reportError(String msg, Object detail);
}
