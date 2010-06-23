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

/**
 * Interface for all plugin interfaces.
 * 
 * @author jblok
 */
public interface IPlugin
{
	/**
	 * Key to use to provide a display name from the plugin properties.
	 */
	public static final String DISPLAY_NAME = "display_name"; //$NON-NLS-1$

	/**
	 * Constants used in propertyChange (PropertyChangeEvent.getPropertyName())
	 */
	public static final String PROPERTY_SOLUTION = "solution"; //$NON-NLS-1$
	public static final String PROPERTY_LOCALE = "locale"; //$NON-NLS-1$
	public static final String PROPERTY_CURRENT_WINDOW = "currentWindow"; //$NON-NLS-1$

	/**
	 * Called on application startup before application started up.
	 */
	public void load() throws PluginException;

	/**
	 * Called on application shutdown.
	 */
	public void unload() throws PluginException;

	/**
	 * Called for info about the plugin.
	 */
	public Properties getProperties();
}
