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

import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.property.ArrayOperation;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;

@SuppressWarnings("nls")
public class ViewportOperation extends ArrayOperation
{

	public ViewportOperation(int startIndex, int endIndex, int type)
	{
		super(startIndex, endIndex, type, null);
	}

	public ViewportOperation(int startIndex, int endIndex, int type, Set<String> columnNames)
	{
		super(startIndex, endIndex, type, columnNames);
	}

	public boolean writeJSONContent(ViewportRowDataProvider rowDataProvider, IFoundSetInternal foundset, int viewportStartIndex, JSONWriter w,
		String keyInParent, DataConversion clientDataConversions, Object sabloValueThatRequestedThisDataToBeWritten) throws JSONException
	{
		JSONUtils.addKeyIfPresent(w, keyInParent);

		w.object();

		// write actual data if necessary
		if (type != DELETE)
		{
			w.key("rows");
			clientDataConversions.pushNode("rows");
			rowDataProvider.writeRowData(viewportStartIndex + startIndex, viewportStartIndex + endIndex, columnNames, foundset, w, clientDataConversions,
				sabloValueThatRequestedThisDataToBeWritten);
			clientDataConversions.popNode();
		}

		w.key("startIndex").value(Integer.valueOf(startIndex)).key("endIndex").value(Integer.valueOf(endIndex)).key("type").value(
			Integer.valueOf(type)).endObject();

		return true;
	}

	@Override
	public String toString()
	{
		return "ViewportOperation [startIndex=" + startIndex + ", endIndex=" + endIndex + ", type=" + type + ", columnName=" + columnNames + "]";
	}

}