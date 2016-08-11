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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.CharSequenceBuffer;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 */
public final class ServoyWrapFactory extends WrapFactory
{
	private final IApplication application;

	/**
	 * @param application
	 */
	public ServoyWrapFactory(IApplication application)
	{
		this.application = application;
		setJavaPrimitiveWrap(false);
	}

	/**
	 * @see org.mozilla.javascript.WrapFactory#wrap(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object wrap(Context cx, Scriptable scope, Object obj, Class staticType)
	{
		if (obj == null || obj == Undefined.instance || obj instanceof Scriptable || obj instanceof String || obj instanceof CharSequenceBuffer ||
			obj instanceof Number || obj instanceof Boolean)
		{
			return obj;
		}
		if (obj instanceof Date)
		{
			return cx.newObject(scope, "Date", new Object[] { new Double(NativeDate.convertToUTCMillisFromJava(((Date)obj).getTime())) });
		}
		if (obj instanceof DbIdentValue || obj instanceof UUID)
		{
			return new NativeJavaObject(scope, obj, ScriptObjectRegistry.getJavaMembers(obj.getClass(), null));
		}

		if (cx != null)
		{
			if (obj instanceof JSConvertedMap< ? , ? >)
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

			if (obj.getClass() == JSONObject.class)
			{
				JSONObject json = (JSONObject)obj;
				Scriptable newObject = cx.newObject(scope);
				Iterator<String> iterator = json.keys();
				while (iterator.hasNext())
				{
					String key = iterator.next();
					Object value = null;
					try
					{
						value = json.get(key);
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
					if (value != null)
					{
						value = wrap(cx, newObject, value, value.getClass());
					}

					newObject.put(key, newObject, value);
				}
				return newObject;
			}

			if (obj.getClass() == JSONObject.class)
			{
				JSONArray array = (JSONArray)obj;
				Scriptable newObject = cx.newObject(scope, "Array");

				for (int i = 0; i < array.length(); i++)
				{
					Object value = null;
					try
					{
						value = array.get(i);
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
					if (value != null)
					{
						value = wrap(cx, newObject, value, value.getClass());
					}
					newObject.put(i, newObject, value);
				}
				return newObject;
			}
		}

		if (obj instanceof IDataSet)
		{
			return new JSDataSet(application, (IDataSet)obj);
		}
		if (obj instanceof FormController)
		{
			throw new IllegalArgumentException(
				"Bad practice: FormController cant be wrapped to javascript (for example not usable as argument in Scheduler plugin)"); //$NON-NLS-1$
		}
		// if it is a none primitive array
		if (obj.getClass().isArray() && !obj.getClass().getComponentType().isPrimitive())
		{
			// then convert the array to a NativeArray, first convert all the elements of the array itself.
			Object[] source = (Object[])obj;
			Object[] array = new Object[source.length];
			for (int i = 0; i < source.length; i++)
			{
				Object src = source[i];
				array[i] = wrap(cx, scope, src, src != null ? src.getClass() : null);
			}
			return cx.newArray(scope, array);
		}
		return super.wrap(cx, scope, obj, staticType);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.WrapFactory#wrapAsJavaObject(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, java.lang.Object,
	 * java.lang.Class)
	 */
	@Override
	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class< ? > staticType)
	{
		JavaMembers members = javaObject != null ? ScriptObjectRegistry.getJavaMembers(javaObject.getClass(), scope)
			: ScriptObjectRegistry.getJavaMembers(staticType, scope);
		if (members != null)
		{
			return new NativeJavaObject(scope, javaObject, members);
		}
		return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
	}
}
