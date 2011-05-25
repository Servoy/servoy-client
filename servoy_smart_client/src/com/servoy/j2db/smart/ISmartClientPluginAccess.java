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

package com.servoy.j2db.smart;

import java.net.URLStreamHandler;
import java.rmi.Remote;

import javax.swing.JMenu;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.toolbar.IToolbarPanel;

/**
 * SmartClient specific extension of application access
 * @author jblok
 *
 */
public interface ISmartClientPluginAccess extends IClientPluginAccess
{
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
	 * Get the toolbar panel (used to control toolbars).
	 * 
	 * @return IToolbarPanel
	 */
	public IToolbarPanel getToolbarPanel();

	/**
	 * Register a URLStreamHandler for a protocol
	 * 
	 * @param protocolName
	 * @param handler
	 */
	public void registerURLStreamHandler(String protocolName, URLStreamHandler handler);

	/**
	 * exports a remote object on the client that can be transfered to the server (with a remote server call) that can have call backs.
	 *  
	 * @param object The remote object to export
	 * @since 6.0
	 */
	public void exportObject(Remote object) throws Exception;
}
