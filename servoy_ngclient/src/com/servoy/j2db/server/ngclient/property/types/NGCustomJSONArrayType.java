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
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;

/**
 * A JSON array type that is Servoy NG client aware as well.
 * So it adds all conversions from {@link NGConversions}.
 *
 * @author acostescu
 */
public class NGCustomJSONArrayType<SabloT, SabloWT> extends CustomJSONArrayType<SabloT, SabloWT> implements IDesignToFormElement<JSONArray, Object[], Object>,
	IFormElementToTemplateJSON<Object[], Object>, IFormElementToSabloComponent<Object[], Object>, ISabloComponentToRhino<Object>,
	IRhinoToSabloComponent<Object>
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
					formElementValues[i] = NGConversions.INSTANCE.convertDesignToFormElementValue(designValue.get(i), getCustomJSONTypeDefinition(),
						flattenedSolution, formElement, propertyPath);
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
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object[] formElementValue, PropertyDescription pd, DataConversion conversionMarkers)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		if (conversionMarkers != null) conversionMarkers.convert(CustomJSONArrayType.TYPE_NAME); // so that the client knows it must use the custom client side JS for what JSON it gets
		writer.object().key(CONTENT_VERSION).value(0).key(VALUE).array();
		DataConversion arrayConversionMarkers = new DataConversion();

		if (formElementValue != null)
		{
			for (int i = 0; i < formElementValue.length; i++)
			{
				arrayConversionMarkers.pushNode(String.valueOf(i));
				NGConversions.INSTANCE.convertFormElementToTemplateJSONValue(writer, null, formElementValue[i], getCustomJSONTypeDefinition(),
					arrayConversionMarkers);
				arrayConversionMarkers.popNode();
			}
		}
		writer.endArray();
		if (arrayConversionMarkers.getConversions().size() > 0)
		{
			writer.key("conversions").object();
			JSONUtils.writeConversions(writer, arrayConversionMarkers.getConversions());
			writer.endObject();
		}
		writer.endObject();
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
				list.add((SabloT)NGConversions.INSTANCE.convertFormElementToSabloComponentValue(element, getCustomJSONTypeDefinition(), formElement, component));
			}
			return list;
		}
		return null;
	}

	@Override
	public Object toSabloComponentValue(Object rhinoValue, Object previousComponentValue, PropertyDescription pd, WebFormComponent component)
	{
		if (rhinoValue == null || rhinoValue == Scriptable.NOT_FOUND) return null;

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
					convertedArray.add(NGConversions.INSTANCE.convertRhinoToSabloComponentValue(rv, previousSpecialArray != null ? previousSpecialArray.get(i)
						: null, getCustomJSONTypeDefinition(), component));
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
		return new RhinoMapOrArrayWrapper(webComponentValue, component, pd.getName(), pd, component.getDataConverterContext());
	}

}
