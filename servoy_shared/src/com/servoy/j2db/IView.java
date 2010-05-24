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


import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.ui.ISupportRowBGColorScript;
import com.servoy.j2db.util.IDestroyable;

/**
 * Helper interface which defines a view
 * 
 * @author jblok
 */
public interface IView extends ISupportRowBGColorScript, IDestroyable
{
	public void setModel(IFoundSetInternal fs);

	public void start(IApplication app);

	public void stop();

	public boolean editCellAt(int row);//used for focusing on first field

	public boolean stopUIEditing(boolean looseFocus);

	public boolean isEditing();

	public void requestFocus();

	public void ensureIndexIsVisible(int index);

	public void setEditable(boolean findMode);

	public boolean isDisplayingMoreThanOneRecord();
}
