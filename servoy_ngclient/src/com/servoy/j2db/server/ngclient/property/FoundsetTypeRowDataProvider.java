/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.dataprocessing.IRecordInternal;

/**
 * @author acostescu
 */
public final class FoundsetTypeRowDataProvider extends ViewportRowDataProvider
{

	protected final FoundsetTypeSabloValue foundsetPropertyValue;
	protected IBrowserConverterContext browserConverterContext;

	public FoundsetTypeRowDataProvider(FoundsetTypeSabloValue foundsetPropertyValue)
	{
		this.foundsetPropertyValue = foundsetPropertyValue;
	}

	@Override
	protected void populateRowData(IRecordInternal record, String columnName, JSONWriter w, DataConversion clientConversionInfo, String generatedRowId)
		throws JSONException
	{
		w.object();
		if (columnName == null)
		{
			w.key(FoundsetTypeSabloValue.ROW_ID_COL_KEY).value(generatedRowId); // foundsetIndex is just a hint for where to start searching for the pk when needed
		}

		foundsetPropertyValue.populateRowData(record, columnName, w, clientConversionInfo, browserConverterContext);

		w.endObject();
	}

	@Override
	protected boolean containsColumn(String columnName)
	{
		if (columnName == null) return true;

		return foundsetPropertyValue.getComponentName(columnName) != null;
	}

	@Override
	protected boolean shouldGenerateRowIds()
	{
		return true;
	}

	/**
	 * We need this as a separate reference and not only passed when doing toJSON on main property because on foundset changes, the foundset property
	 * will write at once the changes and keep them as strings... So then we need access to the context at that time.
	 */
	public void initializeIfNeeded(IBrowserConverterContext context)
	{
		if (browserConverterContext == null) browserConverterContext = context;
	}

	@Override
	protected boolean isReady()
	{
		return browserConverterContext != null;
	}

}
