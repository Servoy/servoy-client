/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.server.ngclient.template;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.util.UUID;

/**
 * @author acostescu
 */
public record PersistIdentifier(
	String[] persistUUIDAndFCPropAndComponentPath, // something like ["D9884DBA_C5E7_4395_A934_52030EB8F1F0", "containedForm", "button_1"] if it's a persist from inside a form component container or ["Z9884DBA_C5E7_4395_A934_52030EB8F1F0"] for a simple persist
	String customTypeOrComponentTypePropertyUUIDInsidePersist // in case it wants to identify a "ghost" (form editor) persist that might be inside form components
)
{

	private static final String GHOST_IDENTIFIER_INSIDE_COMPONENT = "g"; //$NON-NLS-1$
	private static final String COMPONENT_LOCATOR_KEY = "p"; //$NON-NLS-1$

	public static PersistIdentifier fromSimpleUUID(UUID uuid)
	{
		return new PersistIdentifier(new String[] { uuid.toString() }, null);
	}

	public static PersistIdentifier fromSimpleID(int id)
	{
		return new PersistIdentifier(new String[] { String.valueOf(id) }, null);
	}

	/**
	 * Writes it as a String in JSON format.<br/>
	 * DO USE {@link #toHTMLEscapedJSONString()} instead if you are going to write it directly to HTML!
	 */
	public String toJSONString()
	{
		String jsonContent;

		if (customTypeOrComponentTypePropertyUUIDInsidePersist == null && persistUUIDAndFCPropAndComponentPath.length == 1)
		{
			jsonContent = persistUUIDAndFCPropAndComponentPath[0]; // simple id/uuid - send it as is to client
		}
		else if (customTypeOrComponentTypePropertyUUIDInsidePersist == null)
		{
			// it has multiple entries in persistUUIDAndFCPropAndComponentPath (so it is nested using formcomponents)
			// or it's a simple id that starts with '[' or '{' - if that is even possible
			jsonContent = new JSONArray(persistUUIDAndFCPropAndComponentPath).toString();
		}
		else
		{
			// it has multiple entries in persistUUIDAndFCPropAndComponentPath (so it is nested using formcomponents)
			// but it also points to a "ghost" (child custom object type of child component type) inside that specific component
			JSONObject jsonS = new JSONObject();
			jsonS.put(COMPONENT_LOCATOR_KEY,
				persistUUIDAndFCPropAndComponentPath.length == 1
					? persistUUIDAndFCPropAndComponentPath[0]
					: new JSONArray(persistUUIDAndFCPropAndComponentPath));
			jsonS.put(GHOST_IDENTIFIER_INSIDE_COMPONENT, customTypeOrComponentTypePropertyUUIDInsidePersist);
			jsonContent = jsonS.toString();
		}

		return jsonContent;
	}

	/**
	 * Writes it as a String in JSON format - that will probably be used client side in svy-id attributes,
	 */
	public String toHTMLEscapedJSONString()
	{
		return StringEscapeUtils.escapeHtml4(toJSONString());
	}

	/**
	 * Writes it as a String in JSON format - that will probably be used client side in svy-id attributes,
	 */
	public static PersistIdentifier fromJSONString(String jsonContent)
	{
		if (jsonContent == null) return null;

		char firstChar = jsonContent.charAt(0);

		if (firstChar == '{')
		{
			JSONObject jsonRepresentation = new JSONObject(jsonContent);
			Object componentLocator = jsonRepresentation.get(COMPONENT_LOCATOR_KEY); // either a String or a JSONArray
			String ghostIdentifierInsideComp = jsonRepresentation.getString(GHOST_IDENTIFIER_INSIDE_COMPONENT);

			if (componentLocator instanceof String)
				return new PersistIdentifier(
					new String[] { (String)componentLocator },
					ghostIdentifierInsideComp);

			// if componentLocator is not a String, it can only be a JSONArray of Strings
			return new PersistIdentifier(
				JSONUtils.getJavaStringArrayFromJSONArray((JSONArray)componentLocator),
				ghostIdentifierInsideComp);
		}
		else if (firstChar == '[')
		{
			return new PersistIdentifier(
				JSONUtils.getJavaStringArrayFromJSONArray(new JSONArray(jsonContent)),
				null);
		}
		else
		{
			return new PersistIdentifier(new String[] { jsonContent }, null);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(persistUUIDAndFCPropAndComponentPath);
		result = prime * result + Objects.hash(customTypeOrComponentTypePropertyUUIDInsidePersist);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		PersistIdentifier other = (PersistIdentifier)obj;
		return Objects.equals(customTypeOrComponentTypePropertyUUIDInsidePersist, other.customTypeOrComponentTypePropertyUUIDInsidePersist) &&
			Arrays.equals(persistUUIDAndFCPropAndComponentPath, other.persistUUIDAndFCPropAndComponentPath);
	}

	/**
	 * USE {@link #toJSONString()} or {@link #toHTMLEscapedJSONString()} instead in code.
	 * This method is for debugging purposes ONLY.
	 */
	@Override
	public String toString()
	{
		return "persistUUIDAndFCPropAndComponentPath = " + Arrays.toString(persistUUIDAndFCPropAndComponentPath) + //$NON-NLS-1$
			"\ncustomTypeOrComponentTypePropertyUUIDInsidePersist = " + customTypeOrComponentTypePropertyUUIDInsidePersist; //$NON-NLS-1$
	}

}
