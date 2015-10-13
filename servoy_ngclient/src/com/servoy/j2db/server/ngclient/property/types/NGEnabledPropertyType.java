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
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.EnabledPropertyType;
import org.sablo.specification.property.types.EnabledSabloValue;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;

/**
 * @author emera
 */
public class NGEnabledPropertyType extends EnabledPropertyType implements IFormElementDefaultValueToSabloComponent<Boolean, EnabledSabloValue>,
	ISabloComponentToRhino<EnabledSabloValue>, IRhinoToSabloComponent<EnabledSabloValue>, IFormElementToTemplateJSON<Boolean, EnabledSabloValue>
{
	public static final NGEnabledPropertyType NG_INSTANCE = new NGEnabledPropertyType();

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent#toSabloComponentValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, com.servoy.j2db.server.ngclient.INGFormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@SuppressWarnings("boxing")
	@Override
	public EnabledSabloValue toSabloComponentValue(Boolean formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new EnabledSabloValue(formElementValue);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON#toTemplateJSONValue(org.json.JSONWriter, java.lang.String,
	 * java.lang.Object, org.sablo.specification.PropertyDescription, org.sablo.websocket.utils.DataConversion,
	 * com.servoy.j2db.server.ngclient.FormElementContext)
	 */
	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Boolean formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		writer.value(formElementValue);
		return writer;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent#toSabloComponentValue(java.lang.Object, java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@Override
	public EnabledSabloValue toSabloComponentValue(Object rhinoValue, EnabledSabloValue previousComponentValue, PropertyDescription pd,
		BaseWebObject componentOrService)
	{
		if (rhinoValue instanceof Boolean)
		{
			return new EnabledSabloValue((boolean)rhinoValue);
		}
		return null;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#isValueAvailableInRhino(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject)
	 */
	@Override
	public boolean isValueAvailableInRhino(EnabledSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino#toRhinoValue(java.lang.Object,
	 * org.sablo.specification.PropertyDescription, org.sablo.BaseWebObject, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object toRhinoValue(EnabledSabloValue webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		return Boolean.valueOf(webComponentValue.getValue());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent#toSabloComponentDefaultValue(org.sablo.
	 * specification.PropertyDescription, com.servoy.j2db.server.ngclient.INGFormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@Override
	public EnabledSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		return new EnabledSabloValue(Boolean.TRUE);
	}
}
