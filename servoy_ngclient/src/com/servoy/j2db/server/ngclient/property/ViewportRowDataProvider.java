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

import com.servoy.j2db.dataprocessing.FireCollector;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.util.Debug;

/**
 * This class is responsible for writing data in a foundset property's viewport.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public abstract class ViewportRowDataProvider
{

	/**
	 * Some data providers need to be initialized before being used. (usually that happens when the property that uses it has first time 'to browser json' happen).
	 * This is needed in order to ignore any previous foundset changes - don't force a toJSON for them before initial send of value.
	 */
	protected abstract boolean isReady();

	/**
	 * @param generatedRowId null if {@link #shouldGenerateRowIds()} returns false
	 * @param types this can be used to register client-side conversion types for each cell - if needed. It is the responsibility of the caller to call
	 * {@link ViewportClientSideTypes#nextRecordWillBe(int)} before calling this method - so that "populateRowData" can directly use {@link ViewportClientSideTypes#registerClientSideType(com.servoy.j2db.util.Pair...)}
	 */
	protected abstract void populateRowData(IRecordInternal record, Set<String> columnNames, JSONWriter w, String generatedRowId, ViewportClientSideTypes types)
		throws JSONException;

	protected abstract boolean shouldGenerateRowIds();

	protected void writeRowData(int foundsetIndex, Set<String> columnNames, IFoundSetInternal foundset, JSONWriter w, ViewportClientSideTypes types)
		throws JSONException
	{
		// write viewport row contents
		IRecordInternal record = foundset.getRecord(foundsetIndex);
		types.nextRecordWillBe(foundsetIndex);
		populateRowData(record, columnNames, w,
			shouldGenerateRowIds() ? (foundset.isInFindMode() ? String.valueOf(foundsetIndex) : record.getPKHashKey()) : null, types);
	}

	protected ViewportClientSideTypes writeRowData(int startIndex, int endIndex, IFoundSetInternal foundset, JSONWriter w) throws JSONException
	{
		return writeRowData(startIndex, endIndex, null, foundset, w, null);
	}

	protected ViewportClientSideTypes writeRowData(int startIndex, int endIndex, Set<String> columnNames, IFoundSetInternal foundset, JSONWriter w,
		Object sabloValueThatRequestedThisDataToBeWritten) throws JSONException
	{
		ViewportClientSideTypes types = null;
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
					// the DAL here is the FoundsetDAL which will always change record and fire listeners at each row write from each property that is being written
					// so in case multiple FoundsetLinkedTypeSabloValue s would be registered to this foundset (so its DAL) it's no use updating all of them for every row - as only one of them is being actually written at that time
					getDataAdapterList().onlyFireListenersForProperty(sabloValueThatRequestedThisDataToBeWritten);
				}
				try
				{
					types = new ViewportClientSideTypes(startIndex, endIndex);
					for (int i = startIndex; i <= endIndex; i++)
					{
						writeRowData(i, columnNames, foundset, w, types);
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
		return types;
	}

	protected abstract FoundsetDataAdapterList getDataAdapterList();

}
