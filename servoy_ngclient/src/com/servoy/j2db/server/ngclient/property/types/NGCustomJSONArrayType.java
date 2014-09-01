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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaArray;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion1_FromDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion2_FormElementValueToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion3_FormElementValueToSabloComponentValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion4_1_SabloComponentValueToRhino;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion4_2_RhinoToSabloComponentValue;
import com.servoy.j2db.util.Debug;

/**
 * A JSON array type that is Servoy NG client aware as well.
 * So it adds all conversions from {@link NGConversions}.
 *
 * @author acostescu
 */
public class NGCustomJSONArrayType<SabloT, SabloWT> extends CustomJSONArrayType<SabloT, SabloWT> implements
	ISupportsConversion1_FromDesignToFormElement<JSONArray, Object[], Object>, ISupportsConversion2_FormElementValueToTemplateJSON<Object[], Object>,
	ISupportsConversion3_FormElementValueToSabloComponentValue<Object[], Object>, ISupportsConversion4_1_SabloComponentValueToRhino<Object>,
	ISupportsConversion4_2_RhinoToSabloComponentValue<Object>
{

	public NGCustomJSONArrayType(PropertyDescription definition)
	{
		super(definition);
	}

	@Override
	public Object[] toFormElementValue(JSONArray designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, FormElement formElement,
		PropertyPath propertyPath)
	{
		if (designValue != null)
		{
			Object[] formElementValues = new Object[designValue.length()];
			for (int i = designValue.length() - 1; i >= 0; i--)
			{
				try
				{
					propertyPath.add(i);
					formElementValues[i] = NGConversions.INSTANCE.applyConversion1(designValue.get(i), getCustomJSONTypeDefinition(), flattenedSolution,
						formElement, propertyPath);
					propertyPath.backOneLevel();
				}
				catch (JSONException e)
				{
					Debug.error(e);
					formElementValues[i] = null;
				}
			}
			return formElementValues;
		}
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object[] formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers) throws JSONException
	{
		if (formElementValue != null)
		{
			writer.array();
			for (int i = 0; i < formElementValue.length; i++)
			{
				if (browserConversionMarkers != null) browserConversionMarkers.pushNode(String.valueOf(i));
				NGConversions.INSTANCE.applyConversion2(writer, key, formElementValue[i], getCustomJSONTypeDefinition(), browserConversionMarkers);
				if (browserConversionMarkers != null) browserConversionMarkers.popNode();
			}
			writer.endArray();
		}
		return writer;
	}

	@Override
	public Object toSabloComponentValue(Object[] formElementValue, PropertyDescription pd, FormElement formElement, WebFormComponent component)
	{
		if (formElementValue != null)
		{
			List<SabloT> list = new ArrayList<>(formElementValue.length);
			for (Object element : formElementValue)
			{
				list.add((SabloT)NGConversions.INSTANCE.applyConversion3(element, getCustomJSONTypeDefinition(), formElement, component));
			}
			return list;
		}
		return null;
	}

	@Override
	public Object toSabloComponentValue(Object rhinoValue, Object previousComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		if (rhinoValue instanceof RhinoMapOrArrayWrapper)
		{
			return ((RhinoMapOrArrayWrapper)rhinoValue).getWrappedValue();
		}
		else
		{
			// if it's some kind of array, convert it (in depth, iterate over children)

			ChangeAwareList<SabloT, SabloWT> previousSpecialArray = (ChangeAwareList<SabloT, SabloWT>)previousComponentValue;
			List<Object> rhinoArray = null;

			if (rhinoValue instanceof NativeArray)
			{
				// rhinoValue which is (NativeArray) implements List
				rhinoArray = (List)rhinoValue;
			}
			else if (rhinoValue instanceof NativeJavaArray)
			{
				// rhinoValue.unwrap() will be a java static array []
				rhinoArray = Arrays.asList(((NativeJavaArray)rhinoValue).unwrap());
			}

			if (rhinoArray != null)
			{
				List<Object> convertedArray = new ArrayList<Object>(rhinoArray.size());

				int i = 0;
				for (Object rv : rhinoArray)
				{
					convertedArray.add(NGConversions.INSTANCE.applyConversion4_2(rv, previousSpecialArray != null ? previousSpecialArray.get(i) : null,
						getCustomJSONTypeDefinition(), component));
					i++;
				}

				return convertedArray;
			}
		}
		return previousComponentValue; // or should we return null or throw exception here? incompatible thing was assigned
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		return new RhinoMapOrArrayWrapper(webComponentValue, component, pd.getName(), getElementType() instanceof DataproviderPropertyType, pd,
			component.getDataConverterContext());
	}

}
