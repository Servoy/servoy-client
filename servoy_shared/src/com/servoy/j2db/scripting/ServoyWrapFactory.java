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
package com.servoy.j2db.scripting;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

import com.servoy.j2db.FormController;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public final class ServoyWrapFactory extends WrapFactory
{
	public ServoyWrapFactory()
	{
		setJavaPrimitiveWrap(false);
	}

	/**
	 * @see org.mozilla.javascript.WrapFactory#wrap(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object wrap(Context cx, Scriptable scope, Object obj, Class staticType)
	{
		if (obj == null || obj == Undefined.instance || obj instanceof Scriptable || obj instanceof Date || obj instanceof String || obj instanceof Number ||
			obj instanceof Boolean)
		{
			return obj;
		}
		if (obj instanceof JSConvertedMap< ? , ? > && cx != null)
		{
			Scriptable newObject = null;
			JSConvertedMap< ? , ? > map = (JSConvertedMap< ? , ? >)obj;
			if (map.getConstructorName() != null)
			{
				newObject = cx.newObject(scope, map.getConstructorName());
			}
			else
			{
				newObject = cx.newObject(scope);
			}
			Iterator< ? > iterator = map.entrySet().iterator();
			while (iterator.hasNext())
			{
				Map.Entry< ? , ? > next = (Entry< ? , ? >)iterator.next();
				Object key = next.getKey();
				Object value = next.getValue();
				if (value != null)
				{
					value = wrap(cx, newObject, value, value.getClass());
				}

				if (key instanceof Integer)
				{
					newObject.put(((Integer)key).intValue(), newObject, value);
				}
				else if (key instanceof String)
				{
					newObject.put((String)key, newObject, value);
				}
				else
				{
					Debug.error("Try to create a JSConvertedMap->NativeObject with a key that isnt a string or integer:" + key + " for value: " + value); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return newObject;

		}
		if (obj instanceof FormController)
		{
			throw new IllegalArgumentException(
				"Bad practice: FormController cant be wrapped to javascript (for example not usable as argument in Scheduler plugin)"); //$NON-NLS-1$
		}
		return super.wrap(cx, scope, obj, staticType);
	}
}
