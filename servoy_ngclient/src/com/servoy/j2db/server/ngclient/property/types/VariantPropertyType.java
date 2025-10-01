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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;

/**
 * @author jcompagner
 * @since 2022.12
 *
 */
public class VariantPropertyType extends DefaultPropertyType<String>
	implements IConvertedPropertyType<String>, IRhinoToSabloComponent<String>, IFormElementToTemplateJSON<String, String>,
	IDesignToFormElement<Object, String, String>
{

	public static final VariantPropertyType INSTANCE = new VariantPropertyType();
	public static final String TYPE_NAME = "variant";

	private VariantPropertyType()
	{
	}

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return (String)newJSONValue;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, String variantName, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		if (variantName == null) return writer;

		if (formElementContext != null && formElementContext.getFlattenedSolution() != null)
		{
			JSONArray existingVariants = formElementContext.getFlattenedSolution().getVariantsHandler().getVariantClasses(variantName);
			if (existingVariants != null)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.value(existingVariants);
			}
		}
		return writer;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, String variantName, PropertyDescription pd,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (variantName == null) return writer;

		IServoyDataConverterContext servoyDataConverterContext = ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext();
		if (servoyDataConverterContext != null && servoyDataConverterContext.getSolution() != null)
		{
			JSONArray existingVariants = servoyDataConverterContext.getSolution().getVariantsHandler().getVariantClasses(variantName);
			if (existingVariants != null)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.value(existingVariants);
			}
		}
		return writer;
	}

	@Override
	public String toSabloComponentValue(Object rhinoValue, String previousComponentValue, PropertyDescription pd, IWebObjectContext componentOrService)
	{
		if (RhinoConversion.isUndefinedOrNotFound(rhinoValue))
		{
			return null;
		}
		if (rhinoValue != null)
		{
			return rhinoValue.toString();
		}
		return null;
	}

	@Override
	public String toFormElementValue(Object designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		if (designValue == null) return null;
		return designValue.toString();
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

}
