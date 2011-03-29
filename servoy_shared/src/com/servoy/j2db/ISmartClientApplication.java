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

import java.awt.Window;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JMenu;


/**
 * Client interface with smart-client specific features.
 * @author rgansevles
 */
public interface ISmartClientApplication extends IApplication
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
	 * Get the main application frame.
	 * 
	 * @return JFrame
	 */
	public JFrame getMainApplicationFrame();

	/**
	 * Add a window to the cache (makes dialogs and windows faster popup if called second time). <br>
	 * <b>Note:</b> the cache will be cleared on solution close and .dispose() will be called on all
	 * 
	 * @param name
	 * @param the dialog or window
	 */
	public void registerWindow(String name, Window d);

	/**
	 * Get a cached window.
	 * 
	 * @param name
	 * @return Window the window requested or null if not found
	 */
	public Window getWindow(String name);

	public String showI18NDialog(String preselect_key, String preselect_language);

	public Date showCalendar(String pattern, Date date);

	public String showColorChooser(String originalColor);

	public String showFontChooser(String font);

	public void beep();

	public void setClipboardContent(String string);

	public String getClipboardString();

	public void setNumpadEnterAsFocusNextEnabled(boolean enabled);

	public int exportObject(Remote object) throws RemoteException;

	public void setPaintTableImmediately(boolean b);

	public int getPaintTableImmediately();
}
