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

package com.servoy.j2db.scripting;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Scope for classes with dynamic members and js functions.
 *
 * @author rgansevles
 *
 * @since 7.4
 *
 */
public abstract class DefaultJavaScope extends DefaultScope implements IJavaScriptType
{
	private boolean filled;
	private final Map<String, NativeJavaMethod> jsFunctions;

	protected DefaultJavaScope(Scriptable scriptParent, Map<String, NativeJavaMethod> jsFunctions)
	{
		super(scriptParent);
		this.jsFunctions = jsFunctions;
	}

	protected DefaultJavaScope(Scriptable scriptParent, Supplier<Map<String, Object>> allVarsSupplier, Map<String, NativeJavaMethod> jsFunctions)
	{
		super(scriptParent, allVarsSupplier);
		this.jsFunctions = jsFunctions;
	}

	/*
	 * To be overridden when subclass wants to fill itself dynamically.
	 */
	protected boolean fill()
	{
		return true;
	}

	protected void setFilled(boolean filled)
	{
		this.filled = filled;
	}

	private void checkFill()
	{
		if (!filled)
		{
			filled = fill();
		}
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		NativeJavaMethod jm = jsFunctions.get(name);
		if (jm != null)
		{
			ScriptRuntime.setFunctionProtoAndParent(jm, start);
			return jm;
		}
		checkFill();
		return super.get(name, start);
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		checkFill();
		return jsFunctions.containsKey(name) || super.has(name, start);
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		checkFill();
		return super.get(index, start);
	}

	@Override
	public Object[] getIds()
	{
		checkFill();
		return super.getIds();
	}

	/**
	 * Use this method if you want to by pass the fill check (so you only get what really is in this scope at this time)
	 * This will skip also the 2 "allnames" and "length" properties if they are the only 2 in this.
	 *
	 * @return
	 */
	protected final Object[] getRealIds()
	{
		Object[] ids = super.getIds();
		// this assumes that the super call will always be "allnames" and "length" plus the rest (so if there is no rest this call will return an empty array)
		if (ids.length == 2) return new Object[0];
		return ids;
	}

	@Override
	public Object[] getValues()
	{
		checkFill();
		return super.getValues();
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		checkFill();
		return super.has(index, start);
	}

	private static final ConcurrentMap<Class< ? >, Map<String, NativeJavaMethod>> functionCache = new ConcurrentHashMap<>();

	public static Map<String, NativeJavaMethod> getJsFunctions(Class< ? > clazz)
	{
		Map<String, NativeJavaMethod> jsFunctions = functionCache.get(clazz);
		if (jsFunctions != null) return jsFunctions;
		jsFunctions = new HashMap<String, NativeJavaMethod>();
		try
		{
			for (Method method : clazz.getMethods())
			{
				String name = null;
				if (method.getName().startsWith("js_")) //$NON-NLS-1$
				{
					name = method.getName().substring(3);
				}
				else if (method.getName().startsWith("jsFunction_")) //$NON-NLS-1$
				{
					name = method.getName().substring(11);
				}
				else
				{
					AnnotationManagerReflection annotationManager = AnnotationManagerReflection.getInstance();
					JSReadonlyProperty jsReadonlyProperty = annotationManager.getAnnotation(method, clazz, JSReadonlyProperty.class);
					if (jsReadonlyProperty != null)
					{
						name = jsReadonlyProperty.property();
						if (name == null || name.length() == 0)
						{
							name = method.getName();
						}
					}
					else if (annotationManager.isAnnotationPresent(method, clazz, JSFunction.class))
					{
						name = method.getName();
					}
				}
				if (name != null)
				{
					NativeJavaMethod nativeJavaMethod = jsFunctions.get(name);
					if (nativeJavaMethod == null)
					{
						nativeJavaMethod = new NativeJavaMethod(method, name);
					}
					else
					{
						nativeJavaMethod = new NativeJavaMethod(Utils.arrayAdd(nativeJavaMethod.getMethods(), new MemberBox(method), true), name);
					}
					jsFunctions.put(name, nativeJavaMethod);
				}

			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		Map<String, NativeJavaMethod> alreadyCreatedValue = functionCache.putIfAbsent(clazz, jsFunctions);
		if (alreadyCreatedValue != null) return alreadyCreatedValue;
		return jsFunctions;
	}

	public static void clearFunctionCache()
	{
		functionCache.values().forEach(map -> map.clear());
	}

}
