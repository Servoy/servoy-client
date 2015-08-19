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

package com.servoy.j2db.server.ngclient;

import java.util.WeakHashMap;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.TypedData;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetFactory;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.FoundsetReferencePropertyType;
import com.servoy.j2db.util.Pair;

/**
 * @author gboros
 *
 */
public class NGFoundSetManager extends FoundSetManager implements IServerService
{
	public static final String FOUNDSET_SERVICE = "$foundsetService"; //$NON-NLS-1$

	/**
	 * @param application
	 */
	public NGFoundSetManager(IApplication app, IFoundSetFactory factory)
	{
		super(app, factory);
		((NGClient)getApplication()).getWebsocketSession().registerServerService(FOUNDSET_SERVICE, this);
	}

	@SuppressWarnings("nls")
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if ("getFoundSet".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyType.INSTANCE.fromJSON(args, null, null, null);
			String sort = args.optString("sort");
			if (!"".equals(sort))
			{
				foundset.setSort(sort);
			}
			FoundsetTypeSabloValue value = getFoundsetTypeSabloValue(foundset, args.optJSONObject("dataproviders"));
			PropertyDescription foundsetProperty = new PropertyDescription("", FoundsetPropertyType.INSTANCE);
			return new TypedData<FoundsetTypeSabloValue>(value, foundsetProperty);
		}
		else if ("getRelatedFoundSetHash".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyType.INSTANCE.fromJSON(args, null, null, null);
			String rowid = args.optString("rowid");
			String relation = args.optString("relation");

			Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowid);
			int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

			if (recordIndex != -1)
			{
				IRecordInternal record = foundset.getRecord(recordIndex);
				Object o = record.getValue(relation);
				if (o instanceof IFoundSetInternal)
				{
					IFoundSetInternal relatedFoundset = (IFoundSetInternal)o;
					PropertyDescription foundsetRefProperty = new PropertyDescription("", FoundsetReferencePropertyType.INSTANCE);
					return new TypedData<IFoundSetInternal>(relatedFoundset, foundsetRefProperty);
				}
			}
		}
		else if ("updateFoundSetRow".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyType.INSTANCE.fromJSON(args, null, null, null);
			String rowid = args.optString("rowid");
			String dataproviderid = args.optString("dataproviderid");
			Object value = args.get("value");

			Pair<String, Integer> splitHashAndIndex = FoundsetTypeSabloValue.splitPKHashAndIndex(rowid);
			int recordIndex = foundset.getRecordIndex(splitHashAndIndex.getLeft(), splitHashAndIndex.getRight().intValue());

			if (recordIndex != -1)
			{
				IRecordInternal record = foundset.getRecord(recordIndex);
				if (record.startEditing())
				{
					record.setValue(dataproviderid, value);
					return Boolean.TRUE;
				}
			}
		}
		return null;
	}

	private final WeakHashMap<IFoundSetInternal, FoundsetTypeSabloValue> foundsetTypeSabloValueMap = new WeakHashMap<IFoundSetInternal, FoundsetTypeSabloValue>();

	private FoundsetTypeSabloValue getFoundsetTypeSabloValue(IFoundSetInternal foundset, JSONObject dataproviders)
	{
		FoundsetTypeSabloValue foundsetTypeSabloValue = foundsetTypeSabloValueMap.get(foundset);
		if (foundsetTypeSabloValue == null)
		{
			foundsetTypeSabloValue = new FoundsetTypeSabloValue(new JSONObject(), null, null);
			foundsetTypeSabloValue.updateFoundset(foundset);
			foundsetTypeSabloValueMap.put(foundset, foundsetTypeSabloValue);
		}
		foundsetTypeSabloValue.updateDataproviders(dataproviders);
		foundsetTypeSabloValue.getViewPort().setBounds(0, foundsetTypeSabloValue.getFoundset().getSize());

		return foundsetTypeSabloValue;
	}
}
