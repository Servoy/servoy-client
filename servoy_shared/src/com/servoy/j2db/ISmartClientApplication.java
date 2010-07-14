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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;


/**
 * Client with smart-client specific features.
 * @author rgansevles
 *
 */
public interface ISmartClientApplication extends IApplication
{

	String showI18NDialog(String preselect_key, String preselect_language);

	Date showCalendar(String pattern, Date date);

	String showColorChooser(String originalColor);

	String showFontChooser(String font);

	void beep();

	void setClipboardContent(String string);

	String getClipboardString();

	void setNumpadEnterAsFocusNextEnabled(boolean enabled);

	int exportObject(Remote object) throws RemoteException;

	void setPaintTableImmediately(boolean b);

	int getPaintTableImmediately();
}
