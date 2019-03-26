/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;

@SuppressWarnings("nls")
public class ViewportOperation
{
	public static final int CHANGE = 0;
	public static final int INSERT = 1;
	public static final int DELETE = 2;
	public static final int CHANGE_IN_LINKED_PROPERTY = 9;

	public final int startIndex;
	public final int endIndex;
	public final int type;

	/**
	 * Null if it's a whole row, and non-null of only one column of the row is in this row data.
	 */
	public final String columnName;

	public ViewportOperation(int startIndex, int endIndex, int type)
	{
		this(startIndex, endIndex, type, null);
	}

	/**
	 * @throws IllegalArgumentException if you specify a column name, start index and end index must be the same (only one row). Partial changes like that are not supported currently
	 * for multiple rows inside the same operation.
	 */
	public ViewportOperation(int startIndex, int endIndex, int type, String columnName)
	{
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.type = type;
		this.columnName = columnName;

		if (columnName != null && startIndex != endIndex) throw new IllegalArgumentException(
			"Partial row updates are not supported for multiple indexes... Column name: " + columnName + ", [" + startIndex + ", " + endIndex + "].");
	}

	public boolean writeJSONContent(ViewportRowDataProvider rowDataProvider, IFoundSetInternal foundset, int viewportStartIndex, JSONWriter w,
		String keyInParent, DataConversion clientDataConversions) throws JSONException
	{
		JSONUtils.addKeyIfPresent(w, keyInParent);

		w.object();

		// write actual data if necessary
		if (type != DELETE && type != CHANGE_IN_LINKED_PROPERTY)
		{
			w.key("rows");
			clientDataConversions.pushNode("rows");
			rowDataProvider.writeRowData(viewportStartIndex + startIndex, viewportStartIndex + endIndex, columnName, foundset, w, clientDataConversions, null);
			clientDataConversions.popNode();
		}

		w.key("startIndex").value(Integer.valueOf(startIndex)).key("endIndex").value(Integer.valueOf(endIndex)).key("type").value(
			Integer.valueOf(type)).endObject();

		return true;
	}

	@Override
	public String toString()
	{
		return "ViewportOperation [startIndex=" + startIndex + ", endIndex=" + endIndex + ", type=" + type + ", columnName=" + columnName + "]";
	}

}