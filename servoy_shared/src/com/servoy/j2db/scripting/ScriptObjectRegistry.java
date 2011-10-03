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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * 
 */
public class ScriptObjectRegistry
{
	private static final Object NULL_SCOPE = new Object();

	private static final ConcurrentHashMap<Object, Map<Class< ? >, JavaMembers>> javaMembersCache = new ConcurrentHashMap<Object, Map<Class< ? >, JavaMembers>>();

	private static Map<Class< ? >, IScriptable> scriptObjectRegistry = new ConcurrentHashMap<Class< ? >, IScriptable>();

	public static void registerScriptObjectForClass(Class< ? > clz, IScriptable scriptobject)
	{
		// If there is already an XMLScriptObjectAdapter registered for this class, then
		// don't allow it to be overwritten
		if (scriptObjectRegistry.containsKey(clz))
		{
			IScriptable existing = scriptObjectRegistry.get(clz);
			if (existing instanceof XMLScriptObjectAdapter) return;
		}
		scriptObjectRegistry.put(clz, scriptobject);
	}

	public static void registerReturnedTypesProviderForClass(Class< ? > clz, IReturnedTypesProvider rtProvider)
	{
		if (!scriptObjectRegistry.containsKey(clz)) scriptObjectRegistry.put(clz, new ReturnedTypesProviderToScriptObjectAdapter(rtProvider));
	}

	public static IScriptObject getScriptObjectForClass(Class< ? > clz)
	{
		IScriptable so = scriptObjectRegistry.get(clz);
		if (so == null)
		{
			try
			{
				Class.forName(clz.getName(), true, clz.getClassLoader());
			}
			catch (ClassNotFoundException e)
			{
				// ignore
			}
			so = scriptObjectRegistry.get(clz);
			if (so == null)
			{
				for (Class< ? > key : scriptObjectRegistry.keySet())
				{
					if (key.isAssignableFrom(clz))
					{
						so = scriptObjectRegistry.get(key);
						break;
					}
				}
				if (so == null)
				{
					if (IScriptable.class.isAssignableFrom(clz))
					{
						try
						{
							// just try to make it.
							so = (IScriptable)clz.newInstance();
							ScriptObjectRegistry.registerScriptObjectForClass(clz, so);
						}
						catch (Exception e)
						{
							// ignore
						}
					}
				}
			}
		}
		if (so instanceof IScriptObject) return (IScriptObject)so;
		return null;
	}

	/** 
	 * If there is an XMLScriptObject adapter in the registry, use that.
	 * This means that the docs were loaded from an XML from the plugin jar. 
	 * The real scriptobject will be hidden behind the XMLScriptObject adapter.
	 */
	public static IScriptObject getAdapterIfAny(IScriptObject scriptObject)
	{
		if (scriptObject == null) return null;
		Class< ? > key = scriptObject.getClass();
		if (scriptObjectRegistry.containsKey(key))
		{
			IScriptable existing = scriptObjectRegistry.get(key);
			if (existing instanceof XMLScriptObjectAdapter)
			{
				return (XMLScriptObjectAdapter)existing;
			}
		}
		return scriptObject;
	}

	public static Set<Class< ? >> getRegisteredClasses()
	{
		return scriptObjectRegistry.keySet();
	}

	public static void removeEntryFromCache(Scriptable scope)
	{

		Object key = ScriptableObject.getTopLevelScope(scope);
		javaMembersCache.remove(key);

		InstanceJavaMembers.deRegisterClass(scope);

	}

	public static JavaMembers getJavaMembers(final Class< ? > clss, Scriptable scope)
	{
		if (clss.isArray() || clss.isPrimitive() || clss == String.class || clss == Object.class || Number.class.isAssignableFrom(clss) ||
			Date.class.isAssignableFrom(clss))
		{
			return null;
		}
		Object key = scope;
		if (scope == null)
		{
			key = NULL_SCOPE;
		}
		else
		{
			key = ScriptableObject.getTopLevelScope(scope);
		}
		Map<Class< ? >, JavaMembers> map = javaMembersCache.get(key);
		if (map == null)
		{
			map = new ConcurrentHashMap<Class< ? >, JavaMembers>();
			javaMembersCache.put(key, map);
		}
		JavaMembers jm = map.get(clss);
		if (jm == null)
		{
			try
			{
				Context.enter();
				InstanceJavaMembers ijm;
				if (clss != DataException.class)
				{
					ijm = new InstanceJavaMembers(scope, clss);
				}
				else
				{
					// ugly workaround to hide the existence of an inherited deprecated method isServoyException (it was tested for existence in JS) and constants
					ijm = new InstanceJavaMembers(scope, clss)
					{
						@Override
						protected void reflectField(Scriptable scope, Field field)
						{
							if (field.getDeclaringClass() == cl && IConstantsObject.class.isAssignableFrom(cl) && Modifier.isStatic(field.getModifiers()) &&
								Modifier.isFinal(field.getModifiers()) && Modifier.isPublic(field.getModifiers()))
							{
								super.reflectField(scope, field);
							}
						}

						/*
						 * (non-Javadoc)
						 * 
						 * @see com.servoy.j2db.scripting.InstanceJavaMembers#isJsMethod(java.lang.String)
						 */
						@Override
						protected boolean isJsMethod(Method method)
						{
							return super.isJsMethod(method) && !"js_isServoyException".equals(method.getName());
						}

					};
				}
				jm = ijm;
				if (ijm.getFieldIds(false).size() == 0 && ijm.getMethodIds(false).size() == 0 && ijm.getFieldIds(true).size() == 0 &&
					!ReferenceOnlyInJS.class.isAssignableFrom(clss))
				{
					jm = new JavaMembers(scope, clss);
				}
				else
				{
					if (scope != null)
					{
						InstanceJavaMembers.registerClass(scope, clss, ijm);
					}
				}
				map.put(clss, jm);
			}
			catch (Exception e)
			{
				Debug.error("Error creating java members returning null", e); //$NON-NLS-1$
			}
			finally
			{
				Context.exit();
			}
		}
		return jm;
	}

	private static class ReturnedTypesProviderToScriptObjectAdapter implements IScriptObject
	{
		private final IReturnedTypesProvider rtProvider;

		public ReturnedTypesProviderToScriptObjectAdapter(IReturnedTypesProvider rtProvider)
		{
			this.rtProvider = rtProvider;
		}

		public String[] getParameterNames(String methodName)
		{
			return null;
		}

		public String getSample(String methodName)
		{
			return null;
		}

		public String getToolTip(String methodName)
		{
			return null;
		}

		public boolean isDeprecated(String methodName)
		{
			return false;
		}

		public Class< ? >[] getAllReturnedTypes()
		{
			return rtProvider.getAllReturnedTypes();
		}
	}
}
