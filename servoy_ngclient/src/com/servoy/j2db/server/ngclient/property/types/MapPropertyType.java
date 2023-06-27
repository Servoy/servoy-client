/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.property.BrowserFunction;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.serialize.JSONConverter;

/**
 * @author gboros
 *
 */
public class MapPropertyType extends DefaultPropertyType<JSONObject>
	implements IConvertedPropertyType<JSONObject>, IFormElementToTemplateJSON<JSONObject, JSONObject>, IRhinoToSabloComponent<JSONObject>,
	IRhinoDesignConverter, IPropertyWithClientSideConversions<JSONObject>
{

	public static final MapPropertyType INSTANCE = new MapPropertyType();
	public static final String TYPE_NAME = "map"; //$NON-NLS-1$

	private static final JSONConverter converter = new JSONConverter();

	protected MapPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public JSONObject fromJSON(Object newJSONValue, JSONObject previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return newJSONValue instanceof JSONObject ? (JSONObject)newJSONValue : null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, JSONObject sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (sabloValue != null)
		{
			writer.key(key);
			writer.value(fixJSONObjectValueTypesAndI18N(sabloValue));
		}
		return writer;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, JSONObject formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		if (formElementValue != null)
		{
			writer.key(key);
			writer.value(fixJSONObjectValueTypesAndI18N(formElementValue));
		}
		return writer;
	}

	private JSONObject fixJSONObjectValueTypesAndI18N(JSONObject jsonObject)
	{
		JSONObject fixedJSONObject = new JSONObject();
		for (String jsonKey : jsonObject.keySet())
		{
			Object v = jsonObject.get(jsonKey);
			if (v instanceof String)
			{
				String sV = (String)v;
				if (sV.length() > 1 && sV.startsWith("'") && sV.endsWith("'"))
				{
					v = sV.substring(1, sV.length() - 1);
				}
				else if (sV.toLowerCase().equals("true") || sV.toLowerCase().equals("false"))
				{
					fixedJSONObject.put(jsonKey, Boolean.parseBoolean(sV));
					continue;
				}
				else
				{
					try
					{
						long lV = Long.parseLong(sV);
						fixedJSONObject.put(jsonKey, lV);
						continue;
					}
					catch (NumberFormatException ex)
					{
						try
						{
							double dV = Double.parseDouble(sV);
							fixedJSONObject.put(jsonKey, dV);
							continue;
						}
						catch (NumberFormatException ex1)
						{
						}
					}
				}

				v = Text.processTags((String)v, null);
			}
			else if (v instanceof BrowserFunction bf)
			{
				// this is a copy of what is in the class DynamicClientFunctionPropertyType.toJSON
				INGApplication application = bf.getApplication();
				if (application.getRuntimeProperties().containsKey("NG2")) //$NON-NLS-1$
				{
					JSONObject object = new JSONObject();
					object.put(JSONUtils.VALUE_KEY, application.registerClientFunction(bf.getFunctionString()));
					object.put(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY, DynamicClientFunctionPropertyType.CLIENT_SIDE_TYPE_NAME);
					v = object;
				}
				else
				{
					v = bf.getFunctionString();
				}
			}
			fixedJSONObject.put(jsonKey, v);
		}
		return fixedJSONObject;
	}

	@Override
	public JSONObject toSabloComponentValue(Object rhinoValue, JSONObject previousComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		JSONObject sabloValue = null;

		if (rhinoValue instanceof Map)
		{
			sabloValue = new JSONObject();
			for (Map.Entry< ? , ? > entry : ((Map< ? , ? >)rhinoValue).entrySet())
			{
				sabloValue.put(entry.getKey().toString(), entry.getValue());
			}
		}

		return sabloValue;
	}

	/**
	 * To design should be to json (from native object)
	 *
	 * @see com.servoy.j2db.util.IRhinoDesignConverter#fromRhinoToDesignValue(java.lang.Object, org.sablo.specification.PropertyDescription, com.servoy.j2db.IApplication, com.servoy.j2db.scripting.solutionmodel.JSWebComponent)
	 */
	@SuppressWarnings("nls")
	@Override
	public Object fromRhinoToDesignValue(Object solutionModelScriptingValue, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (solutionModelScriptingValue == null) return null;
		try
		{
			return converter.convertToJSONValue(solutionModelScriptingValue);
		}
		catch (Exception e)
		{
			Debug.log("Converting json value " + solutionModelScriptingValue + "  to native js failed for " + pd, e);
		}
		return solutionModelScriptingValue;
	}

	/**
	 * from design should be json to native js.
	 * @see com.servoy.j2db.util.IRhinoDesignConverter#fromDesignToRhinoValue(java.lang.Object, org.sablo.specification.PropertyDescription, com.servoy.j2db.IApplication, com.servoy.j2db.scripting.solutionmodel.JSWebComponent)
	 */
	@SuppressWarnings("nls")
	@Override
	public Object fromDesignToRhinoValue(Object frmJSONValue, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (frmJSONValue == null) return null;
		Context cx = Context.enter();
		try
		{
			Object fromJSON = converter.convertFromJSON(frmJSONValue);
			return cx.getWrapFactory().wrap(cx, application.getScriptEngine().getSolutionScope(), fromJSON, frmJSONValue.getClass());
		}
		catch (Exception e)
		{
			Debug.log("Converting js object " + frmJSONValue + "  to json failed for " + pd, e);
		}
		finally
		{
			Context.exit();
		}
		return frmJSONValue;
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);
		return true;
	}
}