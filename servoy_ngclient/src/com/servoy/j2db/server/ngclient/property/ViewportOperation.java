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
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.IToJSONWriter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

@SuppressWarnings("nls")
public class ViewportOperation implements IToJSONWriter<IBrowserConverterContext>
{
	public static final int CHANGE = 0;
	public static final int INSERT = 1;
	public static final int DELETE = 2;
	public static final int CHANGE_IN_LINKED_PROPERTY = 9;

	public final int startIndex;
	public final int endIndex;
	public final int type;
	public final boolean granularUpdate;

	private final IJSONStringWithConversions rowData;

	/**
	 * Null if it's a whole row, and non-null of only one column of the row is in this row data.
	 */
	public final String columnName;

	public ViewportOperation(IJSONStringWithConversions rowData, int startIndex, int endIndex, int type)
	{
		this(rowData, startIndex, endIndex, type, null, false);
	}

	/**
	 * @throws IllegalArgumentException if you specify a column name, start index and end index must be the same (only one row). Partial changes like that are not supported currently
	 * for multiple rows inside the same operation.
	 */
	public ViewportOperation(IJSONStringWithConversions rowData, int startIndex, int endIndex, int type, String columnName, boolean granularUpdate)
	{
		this.rowData = rowData;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.type = type;
		this.columnName = columnName;
		this.granularUpdate = granularUpdate;

		if (columnName != null && startIndex != endIndex) throw new IllegalArgumentException(
			"Partial row updates are not supported for multiple indexes... Column name: " + columnName + ", [" + startIndex + ", " + endIndex + "].");
	}

	@Override
	public boolean writeJSONContent(JSONWriter w, String keyInParent, IToJSONConverter<IBrowserConverterContext> converter,
		DataConversion clientDataConversions) throws JSONException
	{
		JSONUtils.addKeyIfPresent(w, keyInParent);

		w.object();
		if (rowData != null)
		{
			w.key("rows").value(rowData);
			clientDataConversions.pushNode("rows").convert(rowData.getDataConversions()).popNode();
		}

		w.key("startIndex").value(Integer.valueOf(startIndex)).key("endIndex").value(Integer.valueOf(endIndex)).key("type").value(
			Integer.valueOf(type)).endObject();

		return true;
	}

	/**
	 * True if the data of this RowData would be completely replaced by another immediately following RowData.
	 * @param newOperation the following change/update operation.
	 */
	public boolean isMadeIrrelevantBySubsequentRowData(ViewportOperation newOperation)
	{
		// so a change can be made obsolete by a subsequent full change (so not granular update change) or delete of the same row;
		// it we're talking about two change operations, it matters as well if one of them is only for a specific column of the row or for the whole row

		// also a change made just for the sake of client-side listeners firing on foundset prop due to changes in foundset-linked prop. content only can be
		// safely replaced by any similar op., a normal change or a delete on the same row
		boolean canNewTypeOverrideOldType = (type == CHANGE && (newOperation.type == CHANGE || newOperation.type == DELETE) &&
			(newOperation.columnName == null || newOperation.columnName.equals(columnName)) && !newOperation.granularUpdate) ||
			(type == CHANGE_IN_LINKED_PROPERTY &&
				(newOperation.type == CHANGE_IN_LINKED_PROPERTY || newOperation.type == CHANGE || newOperation.type == DELETE));

		return canNewTypeOverrideOldType && startIndex >= newOperation.startIndex && endIndex <= newOperation.endIndex;
	}

	/**
	 * True if the data of this RowData would be completely redundant taking into consideration the previous RowData that was scheduled - so it should not be added.<br/><br/>
	 * This only needs to check situations where the previousOperation was not already removed due to {@link #isMadeIrrelevantBySubsequentRowData(ViewportOperation)} returning true on the previous RowData for current RowData as newOperation.
	 *
	 * @param previousOperation the previous change/update operation.
	 */
	public boolean isMadeIrrelevantByPreviousRowData(ViewportOperation previousOperation)
	{
		// so a change can be redundant if it is an CHANGE_IN_LINKED_PROPERTY that happens right after a real CHANGE on the same rows/column
		boolean canOldTypeOverrideNewType = (type == CHANGE_IN_LINKED_PROPERTY && previousOperation.type == CHANGE);

		return canOldTypeOverrideNewType && startIndex >= previousOperation.startIndex && endIndex <= previousOperation.endIndex;
	}

	@Override
	public String toString()
	{
		return "RowData [startIndex=" + startIndex + ", endIndex=" + endIndex + ", type=" + type + ", rowData=" + rowData + ", columnName=" + columnName + "]";
	}

}