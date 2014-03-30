/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.server.ngclient.property.PropertyType.DataproviderConfig;
import com.servoy.j2db.server.ngclient.property.PropertyType.ValuesConfig;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class WebComponentSpec extends PropertyDescription
{
	private final Map<String, PropertyDescription> events = new HashMap<>(); // second String is always a "function" for now, but in the future it will probably contain more (to specify sent args/types...)
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

	void addEvent(PropertyDescription propertyDescription)
	{
		events.put(propertyDescription.getName(), propertyDescription);
	}

	void addType(PropertyDescription componentType)
	{
		types.put(componentType.getName(), componentType);
	}

	/**
	 * You are not allowed to modify this map!
	 */
	public Map<String, PropertyDescription> getEvents()
	{
		return Collections.unmodifiableMap(events);
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
		names.addAll(events.keySet());
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
				parseProperties("model", spec.getType(typeName), jsonObject.getJSONObject(typeName), specpath, spec);
				parseProperties("handlers", spec.getType(typeName), jsonObject.getJSONObject(typeName), specpath, spec);
			}
		}

		// properties
		parseProperties("model", spec, json, specpath, spec);
		parseProperties("handlers", spec, json, specpath, spec);

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
	private static void parseProperties(String propKey, PropertyDescription wct, JSONObject json, String specpath, WebComponentSpec spec) throws JSONException
	{
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

					PropertyDescription desc = new PropertyDescription(key, type, isArray, config, defaultValue);
					if (type == PropertyType.function)
					{
						if (wct instanceof WebComponentSpec)
						{
							((WebComponentSpec)wct).addEvent(desc);
						}
						else
						{
							Debug.error("Type '" + wct.getName() + "' in spec file '" + specpath + "', can't have a function '" + key + "' as param");
						}
					}
					if (propWCT != null && propWCT.getType() == PropertyType.custom)
					{
						//copy properties from type
						//this will change whe tree structure comes in 
						desc.putAll(spec.getType(propWCT.getName()).getProperties());
						wct.putProperty(key, desc);
					}
					else wct.putProperty(key, desc);
				}
			}
		}
	}

	private static DataproviderConfig parseDataproviderConfig(JSONObject json)
	{
		String onDataChange = null;
		String onDataChangeCallback = null;
		if (json != null)
		{
			JSONObject onDataChangeObj = json.optJSONObject("ondatachange");
			if (onDataChangeObj != null)
			{
				onDataChange = onDataChangeObj.optString("onchange", null);
				onDataChangeCallback = onDataChangeObj.optString("callback", null);
			}
		}

		return new DataproviderConfig(onDataChange, onDataChangeCallback);
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
