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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IDataConverterContext;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class FindModePropertyType extends DefaultPropertyType<FindModeSabloValue> implements IConvertedPropertyType<FindModeSabloValue>,
	IFormElementDefaultValueToSabloComponent<JSONObject, FindModeSabloValue>, ISabloComponentToRhino<FindModeSabloValue>,
	IRhinoToSabloComponent<FindModeSabloValue>, IFormElementToTemplateJSON<String, FindModeSabloValue>
{

	public static final FindModePropertyType INSTANCE = new FindModePropertyType();
	public static final String TYPE_NAME = "findmode";
	private static final FindModeSabloValue defaultValue = new FindModeSabloValue(null, null);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyType#getName()
	 */
	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.types.DefaultPropertyType#parseConfig(org.json.JSONObject)
	 */
	@Override
	public Object parseConfig(JSONObject config)
	{
		HashMap<String, Object> forEntities = new HashMap<String, Object>();
		try
		{
			JSONObject forProperty = (JSONObject)config.get("for"); //$NON-NLS-1$
			String[] names = JSONObject.getNames(forProperty);
			for (String propertyName : names)
			{
				forEntities.put(propertyName, forProperty.get(propertyName));
			}
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return new FindModeConfig(forEntities);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.types.DefaultPropertyType#defaultValue(org.sablo.specification.PropertyDescription)
	 */
	@Override
	public FindModeSabloValue defaultValue(PropertyDescription pd)
	{
		return defaultValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#fromJSON(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public FindModeSabloValue fromJSON(Object newJSONValue, FindModeSabloValue previousSabloValue, IDataConverterContext dataConverterContext)
	{
		//we do not allow changes coming in from the client
		return previousSabloValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#toJSON(org.json.JSONWriter, java.lang.String, java.lang.Object,
	 * org.sablo.websocket.utils.DataConversion, org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FindModeSabloValue sabloValue, DataConversion clientConversion,
		IDataConverterContext dataConverterContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		return sabloValue.toJSON(writer);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent#toSabloComponentValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, com.servoy.j2db.server.ngclient.FormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@Override
	public FindModeSabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return new FindModeSabloValue((FindModeConfig)pd.getConfig(), dataAdapterList);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent#toSabloComponentDefaultValue(org.sablo.specification
	 * .PropertyDescription, com.servoy.j2db.server.ngclient.FormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@Override
	public FindModeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new FindModeSabloValue((FindModeConfig)pd.getConfig(), dataAdapterList);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#isValueAvailableInRhino(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@Override
	public boolean isValueAvailableInRhino(FindModeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#toRhinoValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object toRhinoValue(FindModeSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return Scriptable.NOT_FOUND;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent#toSabloComponentValue(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@Override
	public FindModeSabloValue toSabloComponentValue(Object rhinoValue, FindModeSabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		return previousComponentValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON#toTemplateJSONValue(org.json.JSONWriter, java.lang.String,
	 * java.lang.Object, org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion, com.servoy.j2db.FlattenedSolution,
	 * com.servoy.j2db.server.ngclient.FormElement)
	 */
	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FlattenedSolution fs, FormElementContext formElementContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(Boolean.FALSE);
		return writer;
	}
}
