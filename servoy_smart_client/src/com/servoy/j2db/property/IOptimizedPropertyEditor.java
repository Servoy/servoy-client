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
package com.servoy.j2db.property;


import java.awt.event.ActionListener;
import java.beans.PropertyEditor;

import com.servoy.j2db.IApplication;

/**
 * If for the getCustomEditor a IPropertyEditorDialog is returned it will show that dialog.
 * 
 * @author jblok
 */
public interface IOptimizedPropertyEditor extends PropertyEditor
{
	//to be able todo proper init
	public void init(IApplication app);

	//to be able to lazy create the UI, is not called with false is whas cancel
	public void prepareForVisible(IApplication app, boolean b);

	//add an action listener to the editor, to listen for action event being done "OK"
	public void addActionListener(ActionListener a);

	public void removeActionListener(ActionListener a);
}
