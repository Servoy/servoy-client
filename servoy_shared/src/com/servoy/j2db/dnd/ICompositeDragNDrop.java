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


/**
 * Interface that container components (composites) should implement for drag and drop support
 * on them and all of their children components.
 * 
 * @author gboros
 */
public interface ICompositeDragNDrop
{
	/**
	 * Called when a drag is started.
	 * 
	 * @param event the drag and drop even
	 * 
	 * @return a DRAGNDROP constant or a combination of 2 constants:
	 * DRAGNDROP.MOVE if only a move can happen,
	 * DRAGNDROP.COPY if only a copy can happen,
	 * DRAGNDROP.MOVE|DRAGNDROP.COPY if a move or copy can happen,
	 * DRAGNDROP.NONE if nothing is supported (drag should not start). 
	 */
	public int onDrag(JSDNDEvent event);

	/**
	 * Called when a drag over starts.
	 * 
	 * @param event the drag and drop event
	 * 
	 * @return whatever a drop can be performed on the component
	 */
	public boolean onDragOver(JSDNDEvent event);

	/**
	 * Called when a drop is performed.
	 * 
	 * @param event the drag and drop event
	 * 
	 * @return whatever the drop was successful
	 */
	public boolean onDrop(JSDNDEvent event);

	/**
	 * Called after the drag operation is finished whatever successfully or not. 
	 * 
	 * @param event the drag and drop event
	 */
	public void onDragEnd(JSDNDEvent event);

	/**
	 * Returns the composite draggable child element at the specified location.
	 * 
	 * @param xy location in the composite
	 * 
	 * @return draggable element in the composite at the specified location
	 */
	public Object getDragSource(Point xy);
}
