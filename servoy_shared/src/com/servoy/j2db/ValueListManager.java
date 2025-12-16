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
import com.servoy.j2db.scripting.DefaultScope;

/**
 * @author Diana
 *
 */
public class ValueListManager extends DefaultScope implements IValueListManager
{

	private final IApplication application;

	public ValueListManager(IApplication application, Scriptable scope)
	{
		super(scope);
		this.application = application;
	}

	@SuppressWarnings({ "nls" })
	@Override
	public Object get(String name, Scriptable start)
	{
		return switch (name)
		{
			case "CUSTOM_VALUES" -> Integer.valueOf(IValueListConstants.CUSTOM_VALUES);
			case "DATABASE_VALUES" -> Integer.valueOf(IValueListConstants.DATABASE_VALUES);
			case "EMPTY_VALUE_ALWAYS" -> Integer.valueOf(IValueListConstants.EMPTY_VALUE_ALWAYS);
			case "EMPTY_VALUE_NEVER" -> Integer.valueOf(IValueListConstants.EMPTY_VALUE_NEVER);
			case "NAMES" -> createNames(start);
			default -> {
				ValueList valueList = application.getFlattenedSolution().getValueList(name);
				yield valueList != null ? valueList.getName() : null;
			}
		};
	}

	/**
	 * @param start
	 * @return
	 */
	private Scriptable createNames(Scriptable start)
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
			@SuppressWarnings("nls")
			@Override
			public String toString()
			{
				StringBuilder sb = new StringBuilder('{');
				for (int i = 0; i < valueListNames.size(); i++)
				{
					String vlName = valueListNames.get(i);
					Object vlNameNativeObject = get(vlName, this);
					if (i > 0) sb.append(", ");
					sb.append(vlName).append(": ").append(vlNameNativeObject == null ? "null" : vlNameNativeObject.toString());
				}
				sb.append('}');
				return sb.toString();
			}

			@Override
			public Object[] getIds()
			{
				return valueListNames.toArray(new Object[0]);
			}
		};

		final Iterator<ValueList> allValueLists = application.getFlattenedSolution().getValueLists(false);
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

	@SuppressWarnings("nls")
	@Override
	public boolean has(String name, Scriptable start)
	{
		return switch (name)
		{
			case "NAMES", "CUSTOM_VALUES", "DATABASE_VALUES", "EMPTY_VALUE_ALWAYS", "EMPTY_VALUE_NEVER" -> true;
			default -> application.getSolution().getValueList(name) != null;
		};
	}

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
}
