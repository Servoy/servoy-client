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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.mozilla.javascript.ClassCache;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.util.Ident;

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

	/**
	 * @see JavaMembers#reflectMethod(Scriptable, Method)
	 */
	@Override
	protected void reflectMethod(Hashtable ht, String name, Scriptable scope, MemberBox[] methodBoxes)
	{
		if (name.startsWith("js_") || name.startsWith("jsFunction_")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			super.reflectMethod(ht, name, scope, methodBoxes);
		}
		else
		{
			ht.remove(name);
		}
		//ignore the rest
	}

	/**
	 * @see org.mozilla.javascript.JavaMembers#makeBeanProperties(Scriptable, boolean)
	 */
	@Override
	protected void makeBeanProperties(boolean isStatic)
	{
		Hashtable ht = isStatic ? staticMembers : members;
		Enumeration enumeration = ht.keys();
		while (enumeration.hasMoreElements())
		{
			String name = (String)enumeration.nextElement();
			if (name.startsWith("js_")) //$NON-NLS-1$
			{
				String newName = name.substring(3);
				if (!Ident.checkIfKeyword(newName))
				{
					Object value = ht.remove(name);
					ht.put(newName, value);
				}
			}
		}
		super.makeBeanProperties(isStatic);
		enumeration = ht.keys();
		while (enumeration.hasMoreElements())
		{
			String name = (String)enumeration.nextElement();
			if (name.startsWith("jsFunction_")) //$NON-NLS-1$
			{
				String newName = name.substring(11);
				if (!Ident.checkIfKeyword(newName))
				{
					Object value = ht.remove(name);
					ht.put(newName, value);
				}
			}
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
		Hashtable ct = cache.getClassTable();

		ct.put(cls, ijm);
	}

	public static void deRegisterClass(Scriptable scope)
	{
		ClassCache cache = ClassCache.get(scope);
		cache.getClassTable().clear();
		cache = null;

	}
}
