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

import java.awt.dnd.DnDConstants;

import com.servoy.j2db.annotations.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;

@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class DRAGNDROP implements IPrefixedConstantsObject, IReturnedTypesProvider
{
	/**
	 * Constant for no drag operation.
	 *
	 * @sample
	 * function startDrag(event)
	 * {
	 *	if(event.getElementName() == "copy")
	 *		return DRAGNDROP.COPY;
	 *	else if(event.getElementName() == "move")
	 *		return DRAGNDROP.MOVE
	 *
	 *	return DRAGNDROP.NONE;
	 * }
	 */
	public static final int NONE = DnDConstants.ACTION_NONE;

	/**
	 * Constant for copy drag operation.
	 *
	 * @sampleas NONE
	 */
	public static final int COPY = DnDConstants.ACTION_COPY;

	/**
	 * Constant for move drag operation.
	 *
	 * @sampleas NONE
	 */
	public static final int MOVE = DnDConstants.ACTION_MOVE;

	public String getPrefix()
	{
		return "DRAGNDROP"; //$NON-NLS-1$
	}

	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}
}
