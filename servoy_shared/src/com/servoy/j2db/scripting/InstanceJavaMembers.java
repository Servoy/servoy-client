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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.util.keyword.Ident;

/**
 * @author jcompagner
 */
public class InstanceJavaMembers extends JavaMembers
{
	private List<String> gettersAndSettersToHide;

	/**
	 * Constructor for InstanceJavaMembers.
	 * 
	 * @param scope
	 * @param cl
	 */
	public InstanceJavaMembers(Scriptable scope, Class< ? > cl)
	{
		super(scope, cl);
	}

	/**
	 * @see JavaMembers#reflectField(Scriptable, Field)
	 */
	@Override
	protected void reflectField(Scriptable scope, Field field)
	{
		if (IConstantsObject.class.isAssignableFrom(cl) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) &&
			Modifier.isPublic(field.getModifiers()))
		{
			super.reflectField(scope, field);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.JavaMembers#discoverAccessibleMethods(java.lang.Class, boolean, boolean)
	 */
	@Override
	protected final Method[] discoverAccessibleMethods(Class< ? > clazz, boolean includeProtected, boolean includePrivate)
	{
		Method[] discoverAccessibleMethods = super.discoverAccessibleMethods(clazz, includeProtected, includePrivate);
		List<Method> lst = new ArrayList<Method>(discoverAccessibleMethods.length);
		for (Method discoverAccessibleMethod : discoverAccessibleMethods)
		{
			if (isJsMethod(discoverAccessibleMethod.getName()))
			{
				lst.add(discoverAccessibleMethod);
			}
		}
		return lst.toArray(new Method[lst.size()]);
	}

	protected boolean isJsMethod(String name)
	{
		return name.startsWith("js_") || name.startsWith("jsFunction_");
	}

	/**
	 * @see org.mozilla.javascript.JavaMembers#makeBeanProperties(Scriptable, boolean)
	 */
	@Override
	protected void makeBeanProperties(boolean isStatic)
	{
		Map<String, Object> ht = isStatic ? staticMembers : members;
		Map<String, Object> copy = new HashMap<String, Object>(ht);
		Iterator<String> enumeration = ht.keySet().iterator();
		while (enumeration.hasNext())
		{
			String name = enumeration.next();
			if (name.startsWith("js_")) //$NON-NLS-1$
			{
				String newName = name.substring(3);
				if (!Ident.checkIfKeyword(newName))
				{
					Object value = copy.remove(name);
					copy.put(newName, value);
				}
			}
		}
		ht = copy;
		if (isStatic)
		{
			staticMembers = ht;
		}
		else
		{
			members = ht;
		}
		super.makeBeanProperties(isStatic);
		copy = new HashMap<String, Object>(ht);
		enumeration = ht.keySet().iterator();
		while (enumeration.hasNext())
		{
			String name = enumeration.next();
			if (name.startsWith("jsFunction_")) //$NON-NLS-1$
			{
				String newName = name.substring(11);
				if (!Ident.checkIfKeyword(newName))
				{
					Object value = copy.remove(name);
					copy.put(newName, value);
				}
			}
		}
		if (isStatic)
		{
			staticMembers = copy;
		}
		else
		{
			members = copy;
		}
	}


	/**
	 * @see JavaMembers#shouldDeleteGetAndSetMethods()
	 */
	@Override
	protected boolean shouldDeleteGetAndSetMethods()
	{
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void deleteGetAndSetMethods(boolean isStatic, List toRemove)
	{
		this.gettersAndSettersToHide = Collections.<String> unmodifiableList(toRemove);
	}

	public List<String> getGettersAndSettersToHide()
	{
		return gettersAndSettersToHide;
	}

	static void registerClass(Scriptable scope, Class< ? > cls, InstanceJavaMembers ijm)
	{
		ClassCache cache = ClassCache.get(scope);
		Map<Class< ? >, JavaMembers> ct = cache.getClassCacheMap();

		ct.put(cls, ijm);
	}

	public static void deRegisterClass(Scriptable scope)
	{
		ClassCache cache = ClassCache.get(scope);
		cache.getClassCacheMap().clear();
		cache = null;

	}
}
