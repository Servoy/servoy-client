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
package com.servoy.j2db.dnd;

import java.awt.Point;

import com.servoy.j2db.FormController;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.ui.IComponent;

/**
 * Interface to which all form data renderer that supports drag and drop need to conform
 * 
 * @author gboros
 */

public interface ISupportDragNDrop
{
	public void initDragNDrop(FormController formController, int clientDesignYOffset);

	public int onDrag(JSEvent event);

	public boolean onDragOver(JSEvent event);

	public boolean onDrop(JSEvent event);

	public String getDragFormName();

	public IComponent getDragSource(Point xy);

	public IRecordInternal getDragRecord();
}
