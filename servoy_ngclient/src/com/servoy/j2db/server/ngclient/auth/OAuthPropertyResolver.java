/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.j2db.server.ngclient.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * Resolves {@code %%key%%} placeholders in OAuth config JSON string values
 * using server properties (from {@code servoy.properties} / admin panel).
 *
 * <p>This allows storing property references instead of literal secrets in
 * {@code solution_settings.obj}, keeping secrets out of version control.</p>
 *
 * @author emera
 */
public class OAuthPropertyResolver
{
	/** Pattern matching %%propertyName%% tokens. */
	static final Pattern PLACEHOLDER = Pattern.compile("%%([^%]+)%%");

	/**
	 * Returns a copy of {@code json} with all {@code %%key%%} placeholders in
	 * top-level string values replaced by the corresponding server property value.
	 * Placeholders whose key has no matching property are left unchanged.
	 * The original {@code json} object is not mutated.
	 */
	public static JSONObject resolve(JSONObject json, Properties settings)
	{
		JSONObject result = new JSONObject(json.toString());
		for (String key : result.keySet())
		{
			Object val = result.get(key);
			if (val instanceof String)
			{
				result.put(key, resolvePlaceholders((String)val, settings));
			}
		}
		return result;
	}

	/**
	 * Replaces all {@code %%key%%} tokens in {@code value} with the matching
	 * property from {@code settings}. Tokens with no matching property are left
	 * as-is.
	 */
	public static String resolvePlaceholders(String value, Properties settings)
	{
		Matcher m = PLACEHOLDER.matcher(value);
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			String propKey = m.group(1);
			String propVal = settings.getProperty(propKey);
			m.appendReplacement(sb, propVal != null ? Matcher.quoteReplacement(propVal) : Matcher.quoteReplacement(m.group(0)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Returns {@code true} if any top-level string value in {@code json}
	 * contains a {@code %%key%%} placeholder.
	 */
	public static boolean containsPlaceholder(JSONObject json)
	{
		for (String key : json.keySet())
		{
			Object val = json.get(key);
			if (val instanceof String && PLACEHOLDER.matcher((String)val).find()) return true;
		}
		return false;
	}

	/**
	 * Returns the property key names referenced by {@code %%key%%} tokens in
	 * {@code json} that have no matching entry in {@code settings}.
	 * Duplicate missing keys are reported only once.
	 */
	public static List<String> findUnresolved(JSONObject json, Properties settings)
	{
		List<String> missing = new ArrayList<>();
		for (String key : json.keySet())
		{
			Object val = json.get(key);
			if (val instanceof String)
			{
				Matcher m = PLACEHOLDER.matcher((String)val);
				while (m.find())
				{
					String propKey = m.group(1);
					if (settings.getProperty(propKey) == null && !missing.contains(propKey))
					{
						missing.add(propKey);
					}
				}
			}
		}
		return missing;
	}
}
