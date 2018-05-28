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

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.impl.DateSerializer;

import com.servoy.j2db.util.Utils;

/**
 * Extends DateSerializer with Calendar support.
 *
 * @author rgansevles
 *
 */
public class ServoyDateSerializer extends DateSerializer
{
	private final static long serialVersionUID = 2;

	private final static Class< ? >[] _extraSerializableClasses = new Class[] { GregorianCalendar.class };

	@Override
	public Class< ? >[] getSerializableClasses()
	{
		return Utils.arrayJoin(super.getSerializableClasses(), _extraSerializableClasses);
	}

	@Override
	public Object marshall(SerializerState state, Object p, Object o) throws MarshallException
	{
		Object toSerialize = o;
		if (o instanceof Calendar)
		{
			toSerialize = ((Calendar)o).getTime();
		}

		return super.marshall(state, p, toSerialize);
	}
}
