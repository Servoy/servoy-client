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
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

/**
 * Moved out of DebuggerTreeView.
 */
public final class DeclaringClassJavaMembers extends JavaMembers
{
	private final Class clazz;

	public DeclaringClassJavaMembers(Scriptable scope, Class cl, Class clazz)
	{
		super(scope, cl);
		this.clazz = clazz;
	}

	/**
	 * @see org.mozilla.javascript.JavaMembers#shouldDeleteGetAndSetMethods()
	 */
	@Override
	protected boolean shouldDeleteGetAndSetMethods()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.JavaMembers#getFieldIds(boolean)
	 */
	@Override
	public List getFieldIds(boolean isStatic)
	{
		List lst = super.getFieldIds(isStatic);
		for (Iterator iter = lst.iterator(); iter.hasNext();)
		{
			String id = (String)iter.next();
			Object property = getField(id, isStatic);
			if (property instanceof JavaMembers.BeanProperty)
			{
				JavaMembers.BeanProperty beanProperty = (JavaMembers.BeanProperty)property;
				if (beanProperty.getGetter().getDeclaringClass() != clazz && beanProperty.getSetter().getDeclaringClass() != clazz)
				{
					iter.remove();
				}
			}
			else if (property instanceof Field)
			{
				if (((Field)property).getDeclaringClass() != clazz)
				{
					iter.remove();
				}
			}
		}
		return lst;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.JavaMembers#getMethodIds(boolean)
	 */
	@Override
	public List getMethodIds(boolean isStatic)
	{
		List list = super.getMethodIds(isStatic);
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			String id = (String)iter.next();
			NativeJavaMethod method = getMethod(id, isStatic);
			if (method.getMethods()[0].getDeclaringClass() != clazz)
			{
				iter.remove();
			}
		}
		return list;
	}
}