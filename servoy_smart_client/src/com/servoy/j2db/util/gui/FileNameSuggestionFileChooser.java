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
package com.servoy.j2db.util.gui;


import java.io.File;
import java.lang.reflect.Method;

import javax.swing.JFileChooser;
import javax.swing.plaf.FileChooserUI;

import com.servoy.j2db.util.Debug;

/**
 * @author jblok
 * 
 * To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class FileNameSuggestionFileChooser extends JFileChooser
{
	public FileNameSuggestionFileChooser()
	{
		super();
	}

	public FileNameSuggestionFileChooser(File dir)
	{
		super(dir);
	}

	public void suggestFileName(String name)
	{
		FileChooserUI ui = getUI();
		try
		{
			Method method = ui.getClass().getMethod("setFileName", String.class);
			if (method != null) method.invoke(ui, name);
		}
		catch (Exception ex)
		{
			Debug.trace(ex);
		}
	}
}