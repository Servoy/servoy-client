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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.sablo.BaseWebObject;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.ObjectPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.WebFormComponent;
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
	IRhinoDesignConverter, IPropertyWithClientSideConversions<JSONObject>, ISupportTemplateValue<JSONObject>, IDesignMapValueConverter
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
			writer.value(fixJSONObjectValueTypesAndI18N(sabloValue, (JSONObject)propertyDescription.getTag(PropertyDescription.VALUE_TYPES_TAG_FOR_PROP),
				dataConverterContext.getWebObject() instanceof IContextProvider prov ? prov.getDataConverterContext().getApplication() : null,
				dataConverterContext));
		}
		return writer;
	}

	@Override
	public boolean valueInTemplate(JSONObject object, PropertyDescription pd, FormElementContext formElementContext)
	{
		return true;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, JSONObject formElementValue, PropertyDescription pd,
		FormElementContext formElementContext) throws JSONException
	{
		if (formElementValue != null)
		{
			writer.key(key);

			writer.value(fixJSONObjectValueTypesAndI18N(formElementValue, (JSONObject)pd.getTag(PropertyDescription.VALUE_TYPES_TAG_FOR_PROP),
				formElementContext.getContext() != null ? formElementContext.getContext().getApplication() : null, null));
		}
		return writer;
	}

	@SuppressWarnings("nls")
	private JSONObject fixJSONObjectValueTypesAndI18N(JSONObject jsonObject, JSONObject types,
		INGApplication application, IBrowserConverterContext dataConverterContext)
	{
		JSONObject fixedJSONObject = new JSONObject();
		for (String jsonKey : jsonObject.keySet())
		{
			Object v = jsonObject.get(jsonKey);
			Object type = types != null ? types.opt(jsonKey) : null;
			if (type != null)
			{
				JSONObject subTypes = null;
				if (type instanceof JSONObject jo)
				{
					subTypes = jo.optJSONObject(PropertyDescription.VALUE_TYPES_TAG_FOR_PROP);
					type = jo.optString("type", null);
				}
				if (type instanceof CharSequence)
				{
					IPropertyType< ? > pt = TypesRegistry.getType(type.toString());
					if (pt instanceof IDesignMapValueConverter mapValueConvertor)
					{
						if (application == null)
						{
							// this is a template thing just skip this value for now
							continue;
						}
						v = mapValueConvertor.createJSONValue(v, application, subTypes, dataConverterContext);
					}
				}
			}
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
			else if (v instanceof Function func && dataConverterContext != null)
			{
				// this is a copy of what is in the class FunctionRefType.toJSON
				JSONObject value = new JSONObject();
				BaseWebObject webObject = dataConverterContext.getWebObject();
				if (webObject instanceof WebFormComponent wfc)
				{
					String name = wfc.getDataAdapterList().getForm().getName();
					value.put("formname", name);
				}
				value.put(FunctionRefType.FUNCTION_HASH, FunctionRefType.INSTANCE.addReference(func, dataConverterContext));
				value.put("svyType", FunctionRefType.TYPE_NAME);


				JSONObject object = new JSONObject();
				object.put(JSONUtils.VALUE_KEY, value);
				object.put(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY, FunctionRefType.TYPE_NAME);
				v = object;

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
				Object value = entry.getValue();
				if (value instanceof Map) value = toSabloComponentValue(value, null, pd, webObjectContext);
				sabloValue.put(entry.getKey().toString(), value);
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
		w.value(ObjectPropertyType.TYPE_NAME);
		return true;
	}

	@SuppressWarnings("nls")
	@Override
	public Object createJSONValue(Object v, INGApplication application, JSONObject subTypes, IBrowserConverterContext dataConverterContext)
	{
		JSONObject jsonObject = null;
		if (v instanceof CharSequence cs)
		{
			String sV = cs.toString();
			if (sV.length() > 1 && sV.startsWith("'") && sV.endsWith("'"))
			{
				sV = sV.substring(1, sV.length() - 1);
			}
			jsonObject = new JSONObject(sV);
		}
		else if (v instanceof JSONObject jo)
		{
			jsonObject = jo;
		}
		if (jsonObject != null)
		{
			JSONObject json = fixJSONObjectValueTypesAndI18N(jsonObject, subTypes, application, dataConverterContext);
			JSONObject object = new JSONObject();
			object.put(JSONUtils.VALUE_KEY, json);
			object.put(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY, ObjectPropertyType.TYPE_NAME);
			return object;
		}
		else if (v instanceof JSONArray array)
		{
			JSONArray fixedArray = new JSONArray();
			for (int i = 0; i < array.length(); i++)
			{
				Object item = array.opt(i);
				if (item instanceof JSONObject || item instanceof JSONArray)
				{
					item = createJSONValue(item, application, subTypes, dataConverterContext);
				}
				fixedArray.put(i, item);
			}
			JSONObject object = new JSONObject();
			object.put(JSONUtils.VALUE_KEY, fixedArray);
			object.put(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY, ObjectPropertyType.TYPE_NAME);
			return object;
		}
		return v;
	}
}