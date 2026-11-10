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
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.lc.type.TypeInfoFactory;

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
	/**
	 * Immutable, statically cacheable description of a js function: its name and its overloads. It never holds a function instance, so
	 * nothing client specific can end up in the static cache. Use {@link JsFunctionCache} to get the (per owner) function instance.
	 */
	public static final class JsMethod
	{
		private final String name;
		private final MemberBox[] methods;

		private JsMethod(String name, MemberBox[] methods)
		{
			this.name = name;
			this.methods = methods;
		}

		public String getName()
		{
			return name;
		}

		/**
		 * Creates a fresh, unparented function without prototype.
		 */
		NativeJavaMethod newFunction()
		{
			return new NativeJavaMethod(methods, name);
		}
	}

	private boolean filled;
	private final Map<String, JsMethod> jsFunctions;
	private volatile JsFunctionCache jsFunctionCache;

	protected DefaultJavaScope(Scriptable scriptParent, Map<String, JsMethod> jsFunctions)
	{
		super(scriptParent);
		this.jsFunctions = jsFunctions;
	}

	protected DefaultJavaScope(Scriptable scriptParent, Supplier<Map<String, Object>> allVarsSupplier, Map<String, JsMethod> jsFunctions)
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
		JsMethod jm = jsFunctions.get(name);
		if (jm != null)
		{
			JsFunctionCache cache = jsFunctionCache;
			if (cache == null)
			{
				cache = new JsFunctionCache();
				jsFunctionCache = cache;
			}
			return cache.get(jm, start);
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

	private static final ConcurrentMap<Class< ? >, Map<String, JsMethod>> functionCache = new ConcurrentHashMap<>();

	public static Map<String, JsMethod> getJsFunctions(Class< ? > clazz)
	{
		Map<String, JsMethod> jsFunctions = functionCache.get(clazz);
		if (jsFunctions != null) return jsFunctions;
		jsFunctions = new HashMap<String, JsMethod>();
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
					JsMethod jsMethod = jsFunctions.get(name);
					MemberBox memberBox = new MemberBox(method, TypeInfoFactory.GLOBAL);
					if (jsMethod == null)
					{
						jsMethod = new JsMethod(name, new MemberBox[] { memberBox });
					}
					else
					{
						jsMethod = new JsMethod(name, Utils.arrayAdd(jsMethod.methods, memberBox, true));
					}
					jsFunctions.put(name, jsMethod);
				}

			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		Map<String, JsMethod> alreadyCreatedValue = functionCache.putIfAbsent(clazz, jsFunctions);
		if (alreadyCreatedValue != null) return alreadyCreatedValue;
		return jsFunctions;
	}

	public static void clearFunctionCache()
	{
		functionCache.values().forEach(map -> map.clear());
	}

}
