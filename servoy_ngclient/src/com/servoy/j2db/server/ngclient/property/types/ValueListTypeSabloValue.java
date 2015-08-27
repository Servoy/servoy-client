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

package com.servoy.j2db.server.ngclient.property.types;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.LookupListModel;
import com.servoy.j2db.dataprocessing.LookupValueList;
import com.servoy.j2db.server.ngclient.ColumnBasedValueList;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link ValueListPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, filtering, ....
 *
 * @author acostescu
 */
public class ValueListTypeSabloValue implements IDataLinkedPropertyValue, ListDataListener
{

	protected IValueList valueList;
	protected LookupListModel filteredValuelist;
	protected IChangeListener changeMonitor;
	protected final DataAdapterList dataAdapterList;
	protected final String dataproviderID;
	protected final ValueListConfig config;
	private IRecordInternal previousRecord;
	private final PropertyDescription vlPD;

	public ValueListTypeSabloValue(IValueList valueList, DataAdapterList dataAdapterList, ValueListConfig config, String dataproviderID,
		PropertyDescription vlPD)
	{
		this.valueList = valueList;
		this.dataAdapterList = dataAdapterList;
		this.config = config;
		this.dataproviderID = dataproviderID;
		this.vlPD = vlPD;
	}

	public IValueList getValueList()
	{
		return valueList;
	}

	public void setValueList(IValueList valueList)
	{
		this.valueList = valueList;
		filteredValuelist = null;
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	protected List<Map<String, Object>> getJavaValueForJSON() // TODO this should return TypedData<List<Map<String, Object>>> instead
	{
		List<Map<String, Object>> jsonValue = null;
		if (filteredValuelist != null)
		{
			List<Map<String, Object>> array = new ArrayList<>(filteredValuelist.getSize());
			for (int i = 0; i < filteredValuelist.getSize(); i++)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("realValue", filteredValuelist.getRealElementAt(i));
				Object displayValue = filteredValuelist.getElementAt(i);
				if (!(displayValue instanceof Timestamp))
				{
					map.put("displayValue", displayValue != null ? dataAdapterList.getApplication().getI18NMessageIfPrefixed(displayValue.toString()) : "");
				}
				else map.put("displayValue", displayValue);

				array.add(map);
			}
			jsonValue = array;
		}
		else
		{
			List<Map<String, Object>> array = new ArrayList<>(valueList.getSize());
			for (int i = 0; i < valueList.getSize(); i++)
			{
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("realValue", valueList.getRealElementAt(i));
				Object displayValue = valueList.getElementAt(i);
				if (displayValue instanceof Timestamp)
				{
					map.put("displayValue", displayValue);
				}
				else
				{
					map.put("displayValue", displayValue != null ? dataAdapterList.getApplication().getI18NMessageIfPrefixed(displayValue.toString()) : "");
				}
				array.add(map);
			}
			jsonValue = array;
		}

		return jsonValue;
	}

	@Override
	public void attachToBaseObject(IChangeListener monitor, BaseWebObject component)
	{
		this.changeMonitor = monitor;
		valueList.addListDataListener(this);

		FormElement formElement = ((WebFormComponent)component).getFormElement();
		// register data link and find mode listeners as needed
		TargetDataLinks dataLinks = (TargetDataLinks)formElement.getPreprocessedPropertyInfo(IDataLinkedType.class, vlPD);
		if (dataLinks == null)
		{
			// they weren't cached in form element; get them again
			dataLinks = ((ValueListPropertyType)vlPD.getType()).getDataLinks(valueList, vlPD,
				((WebFormComponent)component).getDataConverterContext().getSolution(), formElement);
		}
		dataAdapterList.addDataLinkedProperty(this, dataLinks);
	}

	@Override
	public void detach()
	{
		dataAdapterList.removeDataLinkedProperty(this);

		this.changeMonitor = null;
		if (filteredValuelist != null)
		{
			filteredValuelist.removeListDataListener(this);
		}
		else
		{
			valueList.removeListDataListener(this);
		}
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (previousRecord != null && !previousRecord.equals(record) || Utils.equalObjects(dataProvider, dataproviderID))
		{
			revertFilter();
		}
		if (dataProvider == null || !isFormDP && !isGlobalDP)
		{
			valueList.fill(record);
			if (fireChangeEvent && changeMonitor != null) changeMonitor.valueChanged(); // TODO please optimize this, maybe the valuelist didn't actually change!
		}
		previousRecord = record;
	}

	private List<Map<String, Object>> javaValueForJSON;

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion, boolean checkChanged) throws IllegalArgumentException, JSONException
	{
		List<Map<String, Object>> newJavaValueForJSON = getJavaValueForJSON();
		if (!checkChanged || javaValueForJSON == null || !javaValueForJSON.equals(newJavaValueForJSON))
		{
			if (clientConversion != null) clientConversion.convert(ValueListPropertyType.TYPE_NAME);
			DataConversion clientConversionsInsideValuelist = new DataConversion();
			JSONUtils.toBrowserJSONFullValue(writer, key, newJavaValueForJSON, null, clientConversionsInsideValuelist, null);
			// TODO send these to browser and use them in browser!
		}
		javaValueForJSON = newJavaValueForJSON;
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion) throws IllegalArgumentException, JSONException
	{
		toJSON(writer, key, clientConversion, false);
	}

	private void revertFilter()
	{
		if (filteredValuelist != null)
		{
			filteredValuelist.removeListDataListener(this);
			valueList.addListDataListener(this);
			filteredValuelist = null;
			if (changeMonitor != null) changeMonitor.valueChanged();
		}
	}

	/**
	 * Filters the values of the valuelist for type-ahead-like usage.
	 */
	public void filterValuelist(String filterString)
	{
		if (filteredValuelist == null)
		{
			if (valueList instanceof CustomValueList)
			{
				filteredValuelist = new LookupListModel(dataAdapterList.getApplication(), (CustomValueList)valueList);
			}
			else if (valueList instanceof LookupValueList)
			{
				filteredValuelist = new LookupListModel(dataAdapterList.getApplication(), (LookupValueList)valueList);
			}
			else if (valueList instanceof ColumnBasedValueList)
			{
				filteredValuelist = ((ColumnBasedValueList)valueList).getListModel();
			}
			else if (valueList instanceof LookupListModel)
			{
				filteredValuelist = (LookupListModel)valueList;
			}

			if (filteredValuelist != null)
			{
				valueList.removeListDataListener(this);
				filteredValuelist.addListDataListener(this);
			}
		}

		if (filteredValuelist != null)
		{
			try
			{
				filteredValuelist.fill(dataAdapterList.getRecord(), dataproviderID, filterString, false);
				if (changeMonitor != null) changeMonitor.valueChanged();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}

	}

	@Override
	public void intervalAdded(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	@Override
	public void intervalRemoved(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

	@Override
	public void contentsChanged(ListDataEvent e)
	{
		if (changeMonitor != null) changeMonitor.valueChanged();
	}

}