/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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
import java.util.Map;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class ServoyAttributesPropertyType extends NGObjectPropertyType implements IFormElementToSabloComponent<Map<String, String>, Object>
{
	public final static ServoyAttributesPropertyType NG_INSTANCE = new ServoyAttributesPropertyType();
	public static final String TYPE_NAME = "servoyattributes";

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (IContentSpecConstants.PROPERTY_ATTRIBUTES.equals(key) && dataConverterContext.getWebObject() instanceof IContextProvider &&
			((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication().getRuntimeProperties().containsKey("NG2"))
		{
			key = "servoyAttributes"; //$NON-NLS-1$
		}

		// this if is for when e2e tests are running only (note that the e2e test might also be testing attributes set and resend to client, so it is not enough just to send the data-cy in the template)
		// note that this if is in ChildrenJSONGenerator as well for the template value...
		if (dataConverterContext != null /* this actually can't be null I think */
			&& dataConverterContext.getWebObject() instanceof WebFormComponent component &&
			Utils.isInTestingMode(component.getDataConverterContext().getApplication()) && component.getFormElement() != null &&
			component.getFormElement().getPersistIfAvailable() != null && sabloValue instanceof Map attrs)
		{
			String elementName = /* designer ? fe.getDesignId() : it's e2e tests, it is not designer */
				component.getName();

			IPersist persist = component.getFormElement().getPersistIfAvailable();
			if (elementName.startsWith("svy_") && persist.getUUID() != null)
			{
				elementName = "svy_" + persist.getUUID().toString();
			}
			attrs.put("data-cy", persist.getAncestor(Form.class).getName() + "." + elementName); //$NON-NLS-1$//$NON-NLS-2$
		}

		return super.toJSON(writer, key, sabloValue, propertyDescription, dataConverterContext);
	}

	@Override
	public Object toSabloComponentValue(Map<String, String> formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		if (formElementValue == null) return null; // will probably never happen

		// else - "attributes" is a map - and, as it can be modified at runtime and it's not a primitive, we want to make a copy
		// of that map from FormElement for runtime, so that changing it in one client will not change it in another...

		return new HashMap<>(formElementValue);
	}

}
