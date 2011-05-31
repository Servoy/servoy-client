/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.util.serialize;

import org.apache.commons.codec.binary.Base64;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.jabsorb.serializer.impl.StringSerializer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * String serializer with handling of byte[] as base64 encoded string.
 * 
 * @author rgansevles
 *
 */
public class StringByteArraySerializer extends StringSerializer
{
	/**
	  * Classes that this can serialise to.
	  */
	private static Class[] _JSONClasses = new Class[] { String.class, Integer.class, JSONObject.class };

	@Override
	public Class[] getJSONClasses()
	{
		return _JSONClasses;
	}

	@Override
	public Object marshall(SerializerState state, Object p, Object o) throws MarshallException
	{
		if (o instanceof byte[])
		{
			try
			{
				JSONObject val = new JSONObject();
				if (ser.getMarshallClassHints())
				{
					val.put("javaClass", byte[].class.getName());
				}
				val.put("base64String", Base64.encodeBase64String((byte[])o));
				return val;
			}
			catch (JSONException e)
			{
				throw new MarshallException("JSONException: " + e.getMessage(), e);
			}
		}

		return super.marshall(state, p, o);
	}

	@Override
	public Object unmarshall(SerializerState state, Class clazz, Object jso) throws UnmarshallException
	{
		if (clazz == byte[].class && jso instanceof JSONObject)
		{
			return Base64.decodeBase64(((JSONObject)jso).optString("base64String"));
		}

		return super.unmarshall(state, clazz, jso);
	}
}
