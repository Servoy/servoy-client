/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sablo.specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyType.DataproviderConfig;
import org.sablo.specification.PropertyType.ValuesConfig;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author rgansevles
 */
@SuppressWarnings("nls")
public class WebComponentSpec extends PropertyDescription
{
	private final Map<String, PropertyDescription> handlers = new HashMap<>(); // second String is always a "function" for now, but in the future it will probably contain more (to specify sent args/types...)
	private final Map<String, WebComponentApiDefinition> apis = new HashMap<>();
	private final Map<String, PropertyDescription> types = new HashMap<>();
	private final String definition;
	private final String[] libraries;
	private final String displayName;
	private final String packageName;

	public WebComponentSpec(String name, String packageName, String displayName, String definition, JSONArray libs)
	{
		super(name, null);
		this.packageName = packageName;
		this.displayName = displayName;
		this.definition = definition;
		if (libs != null)
		{
			this.libraries = new String[libs.length()];
			for (int i = 0; i < libs.length(); i++)
			{
				this.libraries[i] = libs.optString(i);
			}
		}
		else this.libraries = new String[0];
	}

	void addHandler(PropertyDescription propertyDescription)
	{
		handlers.put(propertyDescription.getName(), propertyDescription);
	}

	/**
	 * @param hndlrs
	 */
	void putAllHandlers(Map<String, PropertyDescription> hndlrs)
	{
		handlers.putAll(hndlrs);
	}


	void addType(PropertyDescription componentType)
	{
		types.put(componentType.getName(), componentType);
	}

	/**
	 * You are not allowed to modify this map!
	 */
	public Map<String, PropertyDescription> getHandlers()
	{
		return Collections.unmodifiableMap(handlers);
	}

	void addApi(WebComponentApiDefinition api)
	{
		apis.put(api.getName(), api);
	}

	/**
	 * You are not allowed to modify this map!
	 */
	public Map<String, WebComponentApiDefinition> getApis()
	{
		return Collections.unmodifiableMap(apis);
	}

	public String getDisplayName()
	{
		return displayName == null ? getName() : displayName;
	}

	public String getPackageName()
	{
		return packageName;
	}

	public String getFullName()
	{
		return packageName + ':' + getName();
	}

	public String getDefinition()
	{
		return definition;
	}

	@Override
	public Set<String> getAllPropertiesNames()
	{
		Set<String> names = super.getAllPropertiesNames();
		names.addAll(handlers.keySet());
		return names;
	}

	public String[] getLibraries()
	{
		return libraries == null ? new String[0] : libraries;
	}

	private static ParsedProperty parsePropertyString(final String propertyString, final WebComponentSpec spec, final String specpath)
	{
		String property = propertyString.replaceAll("\\s", "");
		boolean isArray = false;
		PropertyType type = null;
		PropertyDescription wct = null;
		if (property.endsWith("[]"))
		{
			isArray = true;
			property = property.substring(0, property.length() - 2);
		}
		wct = spec.getType(property);
		if (wct != null)
		{
			type = PropertyType.custom;
		}
		else
		{
			type = getPropertyType(property, specpath);
		}
		return new ParsedProperty(type, isArray, wct);
	}

	public static PropertyType getPropertyType(String name, String specpath)
	{
		try
		{
			return PropertyType.get(name);
		}
		catch (IllegalArgumentException e)
		{
			StringBuilder sb = new StringBuilder("Could not parse property '" + name + "' from file '" + specpath + "', avaliable types are:");
			for (PropertyType type : PropertyType.values())
			{
				sb.append(type).append(' ');
			}
			Debug.error(sb.toString());
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static WebComponentSpec parseSpec(String specfileContent, String packageName, String specpath) throws JSONException
	{
		JSONObject json = new JSONObject('{' + specfileContent + '}');

		WebComponentSpec spec = new WebComponentSpec(json.getString("name"), packageName, json.optString("displayName", null), json.getString("definition"),
			json.optJSONArray("libraries"));

		// first types, can be used in properties
		if (json.has("types"))
		{
			JSONObject jsonObject = json.getJSONObject("types");
			// first add all types
			Iterator<String> types = jsonObject.keys();
			while (types.hasNext())
			{
				String name = types.next();
				PropertyDescription wct = new PropertyDescription(name, PropertyType.custom);
				spec.addType(wct);
			}
			// then parse all the types (so that they can find each other)
			types = jsonObject.keys();
			while (types.hasNext())
			{
				String typeName = types.next();
				PropertyDescription type = spec.getType(typeName);
				type.putAll(parseProperties("model", jsonObject.getJSONObject(typeName), specpath, spec));
				// TODO this is currently never true? See 5 lines above this, types are always just PropertyDescription?
				// is this really supported? or should we add it just to the properties? But how are these handlers then added and used?
				if (type instanceof WebComponentSpec)
				{
					((WebComponentSpec)type).putAllHandlers(parseProperties("handlers", jsonObject.getJSONObject(typeName), specpath, spec));
				}
			}
		}

		// properties
		spec.putAll(parseProperties("model", json, specpath, spec));
		spec.putAllHandlers(parseProperties("handlers", json, specpath, spec));

		// api
		if (json.has("api"))
		{
			JSONObject api = json.getJSONObject("api");
			for (String func : Utils.iterate((Iterator<String>)api.keys()))
			{
				JSONObject jsonDef = api.getJSONObject(func);
				WebComponentApiDefinition def = new WebComponentApiDefinition(func);

				if (jsonDef.has("parameters"))
				{
					JSONArray params = jsonDef.getJSONArray("parameters");
					for (int p = 0; p < params.length(); p++)
					{
						JSONObject param = params.getJSONObject(p);
						String paramName = parseParamName(param);
						boolean isOptional = false;
						if (param.has("optional")) isOptional = true;

						ParsedProperty pp = parsePropertyString(param.getString(paramName), spec, specpath);
						PropertyDescription desc = new PropertyDescription(paramName, pp.type, Boolean.valueOf(pp.array));
						desc.setOptional(isOptional);
						def.addParameter(desc);
					}
				}

				if (jsonDef.has("returns"))
				{
					ParsedProperty pp = parsePropertyString(jsonDef.getString("returns"), spec, specpath);
					PropertyDescription desc = new PropertyDescription("return", pp.type, Boolean.valueOf(pp.array));
					def.setReturnType(desc);
				}

				spec.addApi(def);
			}
		}
		return spec;
	}

	private static String parseParamName(JSONObject param)
	{
		String paramName = (String)param.keys().next();
		if (paramName.equals("optional"))
		{
			Iterator it = param.keys();
			while (it.hasNext())
			{
				String name = (String)it.next();
				if (!name.equals("optional"))
				{
					paramName = name;
				}
			}
		}
		return paramName;
	}

	/**
	 * @param next
	 * @return
	 */
	public PropertyDescription getType(String name)
	{
		return types.get(name);
	}

	/**
	 * @param propKey
	 * @param spec
	 * @param json
	 * @param specpath
	 * @throws JSONException
	 */
	private static Map<String, PropertyDescription> parseProperties(String propKey, JSONObject json, String specpath, WebComponentSpec spec)
		throws JSONException
	{
		Map<String, PropertyDescription> pds = new HashMap<>();
		if (json.has(propKey))
		{
			JSONObject jsonProps = json.getJSONObject(propKey);
			for (String key : Utils.iterate((Iterator<String>)jsonProps.keys()))
			{
				Object value = jsonProps.get(key);
				PropertyDescription propWCT = null;
				PropertyType type = null;
				boolean isArray = false;
				JSONObject configObject = null;
				Object defaultValue = null;
				List<Object> values = null;
				if (value instanceof String)
				{
					ParsedProperty pp = parsePropertyString((String)value, spec, specpath);
					isArray = pp.array;
					propWCT = pp.wct;
					type = pp.type;
				}
				else if (value instanceof JSONObject && ((JSONObject)value).has("type"))
				{
					ParsedProperty pp = parsePropertyString(((JSONObject)value).getString("type"), spec, specpath);
					type = pp.type;
					isArray = pp.array;
					propWCT = pp.wct; // todo can this be not null here and then having also a config object?
					configObject = ((JSONObject)value);
					if (((JSONObject)value).has("default"))
					{
						defaultValue = ((JSONObject)value).get("default");
					}
					if (((JSONObject)value).has("values"))
					{
						JSONArray valuesArray = ((JSONObject)value).getJSONArray("values");
						values = new ArrayList<Object>();
						for (int i = 0; i < valuesArray.length(); i++)
						{
							values.add(valuesArray.get(i));
						}
					}

				}
				if (type != null)
				{
					Object config = null;
					switch (type)
					{
						case dataprovider :
							config = parseDataproviderConfig(configObject);
							break;
						case function :
							config = parseFunctionConfig(configObject);
							break;

						case values :
							config = parseValuesConfig(configObject);
							break;

						case tagstring :
							config = parseTagstringConfig(configObject);
							break;

						case font :
							config = parseFontConfig(configObject);
							break;

						case border :
							config = parseBorderConfig(configObject);
							break;
						case format :
							config = parseFormatConfig(configObject);
							break;
						case valuelist :
							config = parseValueListConfig(configObject);
							break;
						case custom :
							config = propWCT;
							break;

						default :
							break;
					}

					pds.put(key, new PropertyDescription(key, type, isArray, config, defaultValue, values));
				}
			}
		}
		return pds;
	}

	private static DataproviderConfig parseDataproviderConfig(JSONObject json)
	{
		String onDataChange = null;
		String onDataChangeCallback = null;
		boolean hasParseHtml = false;
		if (json != null)
		{
			JSONObject onDataChangeObj = json.optJSONObject("ondatachange");
			if (onDataChangeObj != null)
			{
				onDataChange = onDataChangeObj.optString("onchange", null);
				onDataChangeCallback = onDataChangeObj.optString("callback", null);
			}
			hasParseHtml = json.optBoolean("parsehtml");
		}

		return new DataproviderConfig(onDataChange, onDataChangeCallback, hasParseHtml);
	}

	private static Boolean parseFunctionConfig(JSONObject json)
	{
		return Boolean.valueOf(json != null && json.optBoolean("adddefault"));
	}

	private static ValuesConfig parseValuesConfig(JSONObject json) throws JSONException
	{
		ValuesConfig config = new ValuesConfig();
		if (json != null)
		{
			if (json.has("default"))
			{
				Object realdef = null;
				Object displaydef = null;
				Object def = json.get("default");
				if (def instanceof JSONObject)
				{
					realdef = ((JSONObject)def).get("real");
					displaydef = ((JSONObject)def).get("display");
				}
				else
				{
					// some value, both real and display
					realdef = def;
				}
				config.addDefault(realdef, displaydef == null ? null : displaydef.toString());
			}

			Object values = json.get("values");
			if (values instanceof JSONArray)
			{
				int len = ((JSONArray)values).length();
				Object[] real = new Object[len];
				String[] display = new String[len];
				for (int i = 0; i < len; i++)
				{
					Object elem = ((JSONArray)values).get(i);
					Object displayval;
					if (elem instanceof JSONObject)
					{
						// real and display
						real[i] = ((JSONObject)elem).get("real");
						displayval = ((JSONObject)elem).get("display");
					}
					else
					{
						// some value, both real and display
						real[i] = elem;
						displayval = elem;
					}
					display[i] = displayval == null ? "" : displayval.toString();
				}
				config.setValues(real, display);
			}

			config.setEditable(json.optBoolean("editable"));
			config.setMultiple(json.optBoolean("multiple"));
		}

		return config;
	}

	private static Boolean parseTagstringConfig(JSONObject json)
	{
		return Boolean.valueOf(json != null && json.optBoolean("hidetags"));
	}

	private static Boolean parseFontConfig(JSONObject json)
	{
		return Boolean.valueOf(json == null || !json.has("stringformat") || json.optBoolean("stringformat"));
	}

	private static Boolean parseBorderConfig(JSONObject json)
	{
		return Boolean.valueOf(json == null || !json.has("stringformat") || json.optBoolean("stringformat"));
	}

	private static String parseValueListConfig(JSONObject json)
	{
		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
		return "";
	}

	private static String parseFormatConfig(JSONObject json)
	{
		if (json != null && json.has("for"))
		{
			try
			{
				return json.getString("for");
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
		return "";
	}

	@Override
	public String toString()
	{
		return getName();
	}

	private static class ParsedProperty
	{
		private final PropertyType type;
		private final boolean array;
		private final PropertyDescription wct;

		ParsedProperty(PropertyType type, boolean array, PropertyDescription wct)
		{
			this.type = type;
			this.array = array;
			this.wct = wct;
		}
	}
}
