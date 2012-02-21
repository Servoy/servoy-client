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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.scripting.JSMap;

/**
 * Serializer for map to javascript object and back
 * simple version, might need some improves
 * 
 * @author jblok
 */
public class MapSerializer
{
	public static Map<String, Object> convertToMap(Object jsobj)
	{
		Map<String, Object> retval = new HashMap<String, Object>();
		if (jsobj == null || jsobj == Undefined.instance || jsobj instanceof IFoundSet || jsobj instanceof IRecord || !(jsobj instanceof NativeObject))
		{
			return retval;
		}

		IdScriptableObject no = (IdScriptableObject)jsobj;
		Object[] noIDs = no.getIds();
		String propertyKey;
		Object propertyValue;
		for (Object element : noIDs)
		{
			// id can be Integer or String
			if (element instanceof Integer)
			{
				propertyKey = ((Integer)element).toString();
				propertyValue = no.get(((Integer)element).intValue(), no);
			}
			else if (element instanceof String)
			{
				propertyKey = (String)element;
				propertyValue = no.get((String)element, no);
			}
			else
			{
				// should not happen
				continue;
			}
			if (propertyValue instanceof NativeFunction)
			{
				continue;
			}
			if (propertyValue instanceof NativeObject)
			{
				propertyValue = convertToMap(propertyValue);
			}
			if (propertyValue instanceof Wrapper)
			{
				propertyValue = ((Wrapper)propertyValue).unwrap();
			}
			retval.put(propertyKey, propertyValue);
		}
		return retval;
	}

	public static Object convertFromMap(Map<String, Object> objAsMap)
	{
		JSMap<String, Object> retval = new JSMap<String, Object>();
		Iterator<Map.Entry<String, Object>> it = objAsMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, Object> entry = it.next();
			Object propertyValue = entry.getValue();
			if (propertyValue instanceof Map)
			{
				propertyValue = convertFromMap((Map<String, Object>)propertyValue);
			}
			retval.put(entry.getKey(), propertyValue);
		}
		return retval;
	}
}
