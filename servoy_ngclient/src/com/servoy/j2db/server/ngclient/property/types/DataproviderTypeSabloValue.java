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

import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.JSONStringWithConversions;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.IServoyAwarePropertyValue;
import com.servoy.j2db.server.ngclient.utils.NGUtils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link DataproviderPropertyType}.
 * Handles any needed listeners and deals with to and from browser communications, find mode, ....
 *
 * @author acostescu
 */
public class DataproviderTypeSabloValue implements IServoyAwarePropertyValue
{
	protected final String dataProviderID;
	protected final DataAdapterList dataAdapterList;
	protected final IServoyDataConverterContext servoyDataConverterContext;
	protected final DataproviderConfig dataproviderConfig;

	protected Object value;
	protected Object jsonValue;
	protected IChangeListener changeMonitor;
	protected PropertyDescription typeOfDP;

	public DataproviderTypeSabloValue(String dataProviderID, DataAdapterList dataAdapterList, WebFormComponent component, DataproviderConfig dataproviderConfig)
	{
		if (dataProviderID.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			this.dataProviderID = ScriptVariable.SCOPES_DOT_PREFIX + dataProviderID;
		}
		else
		{
			this.dataProviderID = dataProviderID;
		}

		this.dataAdapterList = dataAdapterList;
		this.servoyDataConverterContext = component.getDataConverterContext();
		this.dataproviderConfig = dataproviderConfig;
	}

	public String getDataProviderID()
	{
		return dataProviderID;
	}

	/**
	 * Returns the actual value that this dataProvider has.
	 */
	public Object getValue()
	{
		return value;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, BaseWebObject component)
	{
		this.changeMonitor = changeNotifier;
	}

	@Override
	public void detach()
	{
		// nothing to do here... we don't add any listener directly in the property type;
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (typeOfDP == null)
		{
			// see type of dataprovider; this is done only once - first time we get a new record
			typeOfDP = NGUtils.getDataProviderPropertyDescription(dataProviderID, servoyDataConverterContext.getApplication().getFlattenedSolution(),
				servoyDataConverterContext.getForm().getForm(), record.getParentFoundSet().getTable(), dataproviderConfig.hasParseHtml());
		}

		Object v = com.servoy.j2db.dataprocessing.DataAdapterList.getValueObject(record, servoyDataConverterContext.getForm().getFormScope(), dataProviderID);
		if (v == Scriptable.NOT_FOUND) v = null;
		boolean changed = ((v != value) && (v == null || !v.equals(value)));

		value = v;
		if (changed)
		{
			jsonValue = null;
		}
		if (fireChangeEvent && changed) // TODO I don't get here why changeMonitor.valueChanged() shouldn't be done if fireChangeEvent is false; but kept it as it was before refactor...
		{
			changeMonitor.valueChanged();
		}
	}

	public void toJSON(JSONWriter writer, String key, DataConversion clientConversion, IDataConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (jsonValue == null)
		{
			if (typeOfDP != null)
			{
				EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true); // that 'true' is a workaround for allowing directly a value instead of object or array
				DataConversion jsonDataConversion = new DataConversion();
				FullValueToJSONConverter.INSTANCE.toJSONValue(ejw, null, value, typeOfDP, jsonDataConversion, dataConverterContext.getWebObject());
				if (jsonDataConversion.getConversions().size() == 0) jsonDataConversion = null;
				jsonValue = new JSONStringWithConversions(ejw.toJSONString() == null ? null : ejw.toJSONString(), jsonDataConversion);
			}
			else
			{
				jsonValue = value;
			}
		}

		writer.value(jsonValue);
		if (jsonValue instanceof IJSONStringWithConversions) clientConversion.convert(((IJSONStringWithConversions)jsonValue).getDataConversions());
	}

	public void browserUpdateReceived(Object newJSONValue, IDataConverterContext dataConverterContext)
	{
		Object oldValue = value;

		if (typeOfDP != null)
		{
			if (typeOfDP instanceof IConvertedPropertyType< ? >)
			{
				value = ((IConvertedPropertyType)typeOfDP).fromJSON(newJSONValue, value, dataConverterContext);
			}
			else value = newJSONValue;
		}
		else value = newJSONValue; // should never happen I think

		if (oldValue != value && (oldValue == null || !oldValue.equals(value)))
		{
			jsonValue = null;
		}
	}

}