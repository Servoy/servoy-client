/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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


import org.json.JSONObject;
import org.mozilla.javascript.Undefined;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;

public class JSONConverter
{

	private JSONSerializerWrapper serializerWrapper;

	public JSONConverter()
	{
		super();
	}

	private JSONSerializerWrapper getJSONSerializer()
	{
		if (serializerWrapper == null)
		{
			serializerWrapper = new JSONSerializerWrapper(new NativeObjectSerializer(true, true));
		}
		return serializerWrapper;
	}

	/**
	 * @param value
	 * @return
	 * @throws Exception 
	 */
	public String convertToJSON(Object value) throws Exception
	{
		if (value == null || value == Undefined.instance) return null;
		if (value instanceof IFoundSet || value instanceof IRecord) throw new RuntimeException("value cant be a record or foundset"); //$NON-NLS-1$

		return getJSONSerializer().toJSON(value).toString();
	}

	/**
	 * @param retval
	 * @return
	 * @throws Exception 
	 */
	public Object convertFromJSON(Object retval) throws Exception
	{
		if (retval instanceof String)
		{
			return getJSONSerializer().fromJSON((String)retval);
		}
		if (retval instanceof JSONObject)
		{
			return getJSONSerializer().fromJSON((JSONObject)retval);
		}
		return retval;
	}

}