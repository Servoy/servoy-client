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

/**
 * @author gganea
 *
 */
public class ReadonlyPropertyType extends DefaultPropertyType<ReadonlySabloValue> implements IConvertedPropertyType<ReadonlySabloValue>,
	IFormElementDefaultValueToSabloComponent<JSONObject, ReadonlySabloValue>, ISabloComponentToRhino<ReadonlySabloValue>,
	IRhinoToSabloComponent<ReadonlySabloValue>, IFormElementToTemplateJSON<String, ReadonlySabloValue>
{

	public static final ReadonlyPropertyType INSTANCE = new ReadonlyPropertyType();
	public static final String TYPE_NAME = "readOnly";

	private static final ReadonlySabloValue defaultValue = new ReadonlySabloValue(null, false);

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

	@Override
	public ReadonlyConfig parseConfig(JSONObject json)
	{
		return ReadonlyConfig.parse(json);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#fromJSON(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public ReadonlySabloValue fromJSON(Object newJSONValue, ReadonlySabloValue previousSabloValue, IDataConverterContext dataConverterContext)
	{
		return previousSabloValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyConverter#toJSON(org.json.JSONWriter, java.lang.String, java.lang.Object,
	 * org.sablo.websocket.utils.DataConversion, org.sablo.specification.property.IDataConverterContext)
	 */
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, ReadonlySabloValue sabloValue, DataConversion clientConversion,
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
	public ReadonlySabloValue toSabloComponentValue(JSONObject formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), !(Boolean)formElement.getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf()));
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
	public ReadonlySabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), !(Boolean)formElement.getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sablo.specification.property.types.DefaultPropertyType#defaultValue(org.sablo.specification.PropertyDescription)
	 */
	@Override
	public ReadonlySabloValue defaultValue(PropertyDescription pd)
	{
		return defaultValue;
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
		Boolean propertyValue = (Boolean)formElementContext.getFormElement().getPropertyValue(((ReadonlyConfig)pd.getConfig()).getOppositeOf());
		writer.value(!propertyValue);
		return writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent#toSabloComponentValue(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@SuppressWarnings("boxing")
	@Override
	public ReadonlySabloValue toSabloComponentValue(Object rhinoValue, ReadonlySabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		return new ReadonlySabloValue((ReadonlyConfig)pd.getConfig(), (Boolean)rhinoValue, previousComponentValue.getOldOppositeOfValue());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#isValueAvailableInRhino(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@Override
	public boolean isValueAvailableInRhino(ReadonlySabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#toRhinoValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject, org.mozilla.javascript.Scriptable)
	 */
	@SuppressWarnings("boxing")
	@Override
	public Object toRhinoValue(ReadonlySabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return webComponentValue.getValue();//
	}

}
