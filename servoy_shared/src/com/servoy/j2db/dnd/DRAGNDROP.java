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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;

/**
 * The <code>DRAGNDROP</code> class provides constants for handling drag-and-drop operations in Servoy.
 * Key operations include <code>COPY</code>, <code>MOVE</code>, and <code>NONE</code>, which allow developers
 * to define behavior for copying, moving, or disabling drag functionality. These constants help streamline
 * interaction handling in custom applications.
 *
 * Additionally, the class defines MIME types such as <code>MIME_TYPE_SERVOY</code> and <code>MIME_TYPE_SERVOY_RECORD</code>.
 * These constants ensure proper identification of Servoy objects and records during drag-and-drop operations,
 * enhancing integration and compatibility within the Servoy runtime environment.
 *
 * @author gboros
 * @deprecated not used in ngclient
 */
@Deprecated
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
@ServoyClientSupport(ng = true, wc = true, sc = true)
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
	 * @see #NONE
	 */
	public static final int COPY = DnDConstants.ACTION_COPY;

	/**
	 * Constant for move drag operation.
	 *
	 * @sampleas NONE
	 * @see #NONE
	 */
	public static final int MOVE = DnDConstants.ACTION_MOVE;


	/**
	 * Constant used as mime type for servoy objects.
	 *
	 * @sample
	 * if (event.dataMimeType == DRAGNDROP.MIME_TYPE_SERVOY || event.dataMimeType == DRAGNDROP.MIME_TYPE_SERVOY_RECORD) {
	 * 	application.output("Dropping is allowed" );
	 * 	return true;
	 * } else {
	 * 	application.output("Dropping is not allowed" );
	 * 	return false;
	 * }
	 *
	 */
	public static final String MIME_TYPE_SERVOY = "application/x-servoy-object; class=java.lang.Object";

	/**
	 * Constant used as mime type for servoy record objects.
	 *
	 * @sampleas MIME_TYPE_SERVOY
	 * @see #MIME_TYPE_SERVOY
	 *
	 */
	public static final String MIME_TYPE_SERVOY_RECORD = "application/x-servoy-record-object; class=com.servoy.j2db.dataprocessing.Record";


	/*
	 * @see com.servoy.j2db.scripting.IReturnedTypesProvider#getAllReturnedTypes()
	 */
	public Class< ? >[] getAllReturnedTypes()
	{
		return null;
	}
}