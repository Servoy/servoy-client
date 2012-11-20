/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONObject;

/**
 * Serialize (replace actually) javascript Undefined with null.
 * 
 * @author rgansevles
 * 
 * @since 7.0
 *
 */
public class NullForUndefinedSerializer extends AbstractSerializer
{
	/**
	 * Unique serialisation id.
	 */
	private final static long serialVersionUID = 2;

	/**
	 * The classes that this can serialise
	 */
	private final static Class[] _serializableClasses = new Class[] { org.mozilla.javascript.Undefined.class };

	/**
	 * The class that this serialises to
	 */
	private final static Class[] _JSONClasses = new Class[] { JSONObject.class };

	public Class[] getSerializableClasses()
	{
		return _serializableClasses;
	}

	public Class[] getJSONClasses()
	{
		return _JSONClasses;
	}

	public Object marshall(SerializerState state, Object p, Object o) throws MarshallException
	{
		if (o instanceof org.mozilla.javascript.Undefined)
		{
			return null;
		}

		throw new MarshallException("NullForUndefinedSerializer cannot marshall class " + o.getClass());
	}

	public ObjectMatch tryUnmarshall(SerializerState state, Class clazz, Object o) throws UnmarshallException
	{
		return ObjectMatch.NULL;
	}

	public Object unmarshall(SerializerState state, Class clazz, Object o) throws UnmarshallException
	{
		return null;
	}
}
