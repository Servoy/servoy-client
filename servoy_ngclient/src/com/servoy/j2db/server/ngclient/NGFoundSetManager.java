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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.TypedData;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetFactory;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyTypeConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.FoundsetReferencePropertyTypeOld;
import com.servoy.j2db.util.Pair;

/**
 * @author gboros
 *
 * @deprecated now you can use a combination of "to server-side component scripting calls" combined with foundset ref (new one, not old one) and record ref types
 * to do what you need with foundsets in any component; there is no need for this service anymore - and this service uses lots of hard-coded stuff...
 * see SVY-10825 for more information
 */
@Deprecated
public class NGFoundSetManager extends FoundSetManager implements IServerService
{
	public static final String FOUNDSET_SERVICE = "$foundsetManager"; //$NON-NLS-1$

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
			IFoundSetInternal foundset = FoundsetReferencePropertyTypeOld.INSTANCE.fromJSON(args, null, null, null, null);
			String sort = args.optString("sort");
			if (!"".equals(sort))
			{
				foundset.setSort(sort);
			}

			FoundsetTypeSabloValue value = getFoundsetTypeSabloValue(foundset, args.optJSONObject("dataproviders"));

			ChangeAwareList<ChangeAwareMap<String, Object>, Object> foundsets = (ChangeAwareList<ChangeAwareMap<String, Object>, Object>)((NGClient)getApplication()).getWebsocketSession().getClientService(
				"foundset_manager").getProperty("foundsets");
			if (foundsets == null)
			{
				foundsets = new ChangeAwareList<ChangeAwareMap<String, Object>, Object>(new ArrayList<ChangeAwareMap<String, Object>>());
				((NGClient)getApplication()).getWebsocketSession().getClientService("foundset_manager").setProperty("foundsets", foundsets);
			}

			boolean newFoundsetInfo = true;
			for (ChangeAwareMap<String, Object> foundsetInfoMap : foundsets)
			{
				if (foundsetInfoMap.containsValue(value))
				{
					newFoundsetInfo = false;
					break;
				}
			}
			if (newFoundsetInfo)
			{
				HashMap<String, Object> foundsetinfoMap = new HashMap<String, Object>();
				foundsetinfoMap.put("foundset", value);
				foundsetinfoMap.put("foundsethash", args.optString("foundsethash"));

				String childrelation = args.optString("childrelation");
				if (childrelation != null)
				{
					JSONObject childrelationinfo = new JSONObject();
					childrelationinfo.put("name", childrelation);
					for (int i = 0; i < foundset.getSize(); i++)
					{
						IRecordInternal record = foundset.getRecord(i);
						Object o = record.getValue(childrelation);
						if (o instanceof IFoundSetInternal)
						{
							childrelationinfo.put(record.getPKHashKey() + "_" + record.getParentFoundSet().getRecordIndex(record),
								((IFoundSetInternal)o).getSize());
						}
					}
					foundsetinfoMap.put("childrelationinfo", childrelationinfo);
				}

				CustomJSONObjectType dummyCustomObjectTypeForChildRelationInfo = (CustomJSONObjectType)TypesRegistry.createNewType(
					CustomJSONObjectType.TYPE_NAME, "svy__dummyCustomObjectTypeForDeprecatedFMServiceChildRelationInfo");
				PropertyDescription dummyPD = new PropertyDescription("", dummyCustomObjectTypeForChildRelationInfo);
				dummyCustomObjectTypeForChildRelationInfo.setCustomJSONDefinition(dummyPD);
				foundsets.add(new ChangeAwareMap(foundsetinfoMap, null, dummyPD));
			}
		}
		else if ("getRelatedFoundSetHash".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyTypeOld.INSTANCE.fromJSON(args, null, null, null, null);
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
					PropertyDescription foundsetRefProperty = new PropertyDescription("", FoundsetReferencePropertyTypeOld.INSTANCE);
					return new TypedData<IFoundSetInternal>(relatedFoundset, foundsetRefProperty);
				}
			}
		}
		else if ("updateFoundSetRow".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyTypeOld.INSTANCE.fromJSON(args, null, null, null, null);
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
		else if ("removeFoundSetFromCache".equals(methodName))
		{
			IFoundSetInternal foundset = FoundsetReferencePropertyTypeOld.INSTANCE.fromJSON(args, null, null, null, null);
			removeFoundSetTypeSabloValue(foundset);
		}
		else if ("removeFoundSetsFromCache".equals(methodName))
		{
			ChangeAwareList<ChangeAwareMap<String, Object>, Object> foundsets = (ChangeAwareList<ChangeAwareMap<String, Object>, Object>)((NGClient)getApplication()).getWebsocketSession().getClientService(
				"foundset_manager").getProperty("foundsets");
			if (foundsets != null)
			{
				foundsets.clear();
			}
			foundsetTypeSabloValueMap.clear();
		}
		return null;
	}

	public void removeFoundSetTypeSabloValue(IFoundSetInternal foundset)
	{
		if (foundset != null)
		{
			FoundsetTypeSabloValue value = foundsetTypeSabloValueMap.remove(foundset);
			ChangeAwareList<ChangeAwareMap<String, Object>, Object> foundsets = (ChangeAwareList<ChangeAwareMap<String, Object>, Object>)((NGClient)getApplication()).getWebsocketSession().getClientService(
				"foundset_manager").getProperty("foundsets");
			if (foundsets != null)
			{
				int i = 0;
				for (; i < foundsets.size(); i++)
				{
					ChangeAwareMap<String, Object> foundsetInfoMap = foundsets.get(i);
					if (foundsetInfoMap.containsValue(value)) break;
				}
				if (i < foundsets.size())
				{
					foundsets.remove(i);
				}
			}
		}
	}

	@Override
	public void flushCachedItems()
	{
		for (IFoundSetInternal foundset : foundsetTypeSabloValueMap.keySet().toArray(new IFoundSetInternal[foundsetTypeSabloValueMap.size()]))
		{
			foundsetTypeSabloValueMap.remove(foundset);
		}
		super.flushCachedItems();
	}

	private final WeakHashMap<IFoundSetInternal, FoundsetTypeSabloValue> foundsetTypeSabloValueMap = new WeakHashMap<IFoundSetInternal, FoundsetTypeSabloValue>();

	private FoundsetTypeSabloValue getFoundsetTypeSabloValue(IFoundSetInternal foundset, JSONObject dataproviders)
	{
		FoundsetTypeSabloValue foundsetTypeSabloValue = foundsetTypeSabloValueMap.get(foundset);
		if (foundsetTypeSabloValue == null)
		{
			foundsetTypeSabloValue = new FoundsetTypeSabloValue(new JSONObject(), null, null, new FoundsetPropertyTypeConfig(false, false, null, false, 15))
			{
				@Override
				protected void updateFoundset(IRecordInternal record)
				{
					if (record != null)
					{
						super.updateFoundset(record);
					}
				}
			};
			foundsetTypeSabloValue.updateFoundset(foundset);
			foundsetTypeSabloValueMap.put(foundset, foundsetTypeSabloValue);
		}
		foundsetTypeSabloValue.initializeDataproviders(dataproviders);
		foundsetTypeSabloValue.getViewPort().setBounds(0, foundsetTypeSabloValue.getFoundset().getSize());

		return foundsetTypeSabloValue;
	}
}
