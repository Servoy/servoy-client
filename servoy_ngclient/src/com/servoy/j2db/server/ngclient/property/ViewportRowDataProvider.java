/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.dataprocessing.FireCollector;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.util.Debug;

/**
 * This class is responsible for writing data in a foundset property's viewport.
 *
 * @author acostescu
 */
public abstract class ViewportRowDataProvider
{

	/**
	 * Some data providers need to be initialized before being used. (usually that happens when the property that uses it has first time 'to browser json' happen).
	 * This is needed in order to ignore any previous foundset changes - don't force a toJSON for them before initial send of value, because for such
	 * cases foundset property types convert/write viewport changes to a string and keep it that way - but converting cell values might need a BrowserConverterContext
	 * which is not yet available...
	 */
	protected abstract boolean isReady();

	/**
	 * @param generatedRowId null if {@link #shouldGenerateRowIds()} returns false
	 */
	protected abstract void populateRowData(IRecordInternal record, Set<String> columnName, JSONWriter w, DataConversion clientConversionInfo,
		String generatedRowId)
		throws JSONException;

	protected abstract boolean shouldGenerateRowIds();

	/**
	 *  Returns whether viewport contains dataprovider sent as parameter. Null means all columns, so will return true.
	 */
	protected abstract boolean containsColumn(String columnName);

	protected void writeRowData(int foundsetIndex, Set<String> columnName, IFoundSetInternal foundset, JSONWriter w, DataConversion clientConversionInfo)
		throws JSONException
	{
		// write viewport row contents
		IRecordInternal record = foundset.getRecord(foundsetIndex);
		populateRowData(record, columnName, w, clientConversionInfo, shouldGenerateRowIds() ? record.getPKHashKey() + "_" + foundsetIndex : null);
	}

	protected void writeRowData(int startIndex, int endIndex, IFoundSetInternal foundset, JSONWriter w, DataConversion clientConversionInfo)
		throws JSONException
	{
		writeRowData(startIndex, endIndex, null, foundset, w, clientConversionInfo, null);
	}

	protected void writeRowData(int startIndex, int endIndex, Set<String> columnName, IFoundSetInternal foundset, JSONWriter w,
		DataConversion clientConversionInfo,
		Object sabloValueThatRequestedThisDataToBeWritten) throws JSONException
	{
		w.array();
		if (foundset != null)
		{
			int size = foundset.getSize();
			int end = Math.min(size - 1, endIndex);
			if (startIndex <= end)
			{
				if (end < endIndex)
				{
					Debug.error("Illegal state: view ports end index " + endIndex + " is bigger then the size " + size, new RuntimeException());
				}

				// as our goal here is to write contents of all these rows to JSON without triggering calculations that end up triggering data-change-related solution handlers that might in
				// turn change data/bounds of data that we are trying to write to JSON, we use fire collector; after we are done writing, any such handlers will be called
				// and if they alter anything in the foundset, the foundset/other listeners will pick that up and generate a new change to be written to JSON...
				FireCollector fireCollector = FireCollector.getFireCollector();
				if (sabloValueThatRequestedThisDataToBeWritten != null)
				{
					getDataAdapterList().onlyFireListenersForProperty(sabloValueThatRequestedThisDataToBeWritten);
				}
				try
				{
					for (int i = startIndex; i <= endIndex; i++)
					{
						clientConversionInfo.pushNode(String.valueOf(i - startIndex));
						writeRowData(i, columnName, foundset, w, clientConversionInfo);
						clientConversionInfo.popNode();
					}
				}
				finally
				{
					if (sabloValueThatRequestedThisDataToBeWritten != null)
					{
						getDataAdapterList().resumeNormalListeners();
					}
					fireCollector.done();
				}
			}
		}
		w.endArray();
	}

	protected abstract FoundsetDataAdapterList getDataAdapterList();
}
