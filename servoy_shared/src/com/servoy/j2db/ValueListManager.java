/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.DefaultJavaScope;

/**
 * @author Diana
 *
 */
public class ValueListManager implements IValueListManager, Scriptable
{

	private final ClientState application;

	public ValueListManager(ClientState clientState)
	{
		this.application = clientState;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getClassName()
	 */
	@Override
	public String getClassName()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#get(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(String name, Scriptable start)
	{
		if (name.equals("CUSTOM_VALUES")) return IValueListConstants.CUSTOM_VALUES; //$NON-NLS-1$
		if (name.equals("DATABASE_VALUES")) return IValueListConstants.DATABASE_VALUES; //$NON-NLS-1$
		if (name.equals("EMPTY_VALUE_ALWAYS")) return IValueListConstants.EMPTY_VALUE_ALWAYS; //$NON-NLS-1$
		if (name.equals("EMPTY_VALUE_NEVER")) return IValueListConstants.EMPTY_VALUE_NEVER; //$NON-NLS-1$

		if (name.equals("NAMES")) //$NON-NLS-1$
		{
			Scriptable topLevel = ScriptableObject.getTopLevelScope(start);

			final List<String> valueListNames = new ArrayList<>();

			DefaultJavaScope namesScope = new DefaultJavaScope(topLevel, Map.of())
			{
				@Override
				public Object getDefaultValue(Class< ? > hint)
				{
					return toString();
				}

				// toString is needed when JSValueList.NAMES is used without valuelist name after it
				@Override
				public String toString()
				{
					StringBuilder sb = new StringBuilder("{");
					for (int i = 0; i < valueListNames.size(); i++)
					{
						String vlName = valueListNames.get(i);
						Object vlNameNativeObject = get(vlName, this);
						if (i > 0) sb.append(", ");
						sb.append(vlName).append(": ").append(vlNameNativeObject == null ? "null" : vlNameNativeObject.toString());
					}
					sb.append("}");
					return sb.toString();
				}

				@Override
				public Object[] getIds()
				{
					return valueListNames.toArray(new Object[0]);
				}
			};

			final Iterator<ValueList> allValueLists = application.getSolution().getValueLists(false);
			while (allValueLists.hasNext())
			{
				ValueList vl = allValueLists.next();
				if (vl != null && vl.getName() != null)
				{
					namesScope.put(vl.getName(), namesScope, vl.getName());
					valueListNames.add(vl.getName());
				}
			}

			return namesScope;

		}

		ValueList valueList = application.getSolution().getValueList(name);
		return valueList != null ? valueList.getName() : null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#get(int, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public Object get(int index, Scriptable start)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public boolean has(String name, Scriptable start)
	{
		return get(name, start) != null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#has(int, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public boolean has(int index, Scriptable start)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#put(java.lang.String, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	@Override
	public void put(String name, Scriptable start, Object value)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#put(int, org.mozilla.javascript.Scriptable, java.lang.Object)
	 */
	@Override
	public void put(int index, Scriptable start, Object value)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#delete(java.lang.String)
	 */
	@Override
	public void delete(String name)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#delete(int)
	 */
	@Override
	public void delete(int index)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
	@Override
	public Scriptable getPrototype()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#setPrototype(org.mozilla.javascript.Scriptable)
	 */
	@Override
	public void setPrototype(Scriptable prototype)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getParentScope()
	 */
	@Override
	public Scriptable getParentScope()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#setParentScope(org.mozilla.javascript.Scriptable)
	 */
	@Override
	public void setParentScope(Scriptable parent)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getIds()
	 */
	@Override
	public Object[] getIds()
	{
		List<String> names = new ArrayList<String>();

		Iterator<ValueList> it = application.getFlattenedSolution().getValueLists(false);
		names = StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
			.map(ValueList::getName)
			.toList();

		return names.toArray();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getDefaultValue(java.lang.Class)
	 */
	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#hasInstance(org.mozilla.javascript.Scriptable)
	 */
	@Override
	public boolean hasInstance(Scriptable instance)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
