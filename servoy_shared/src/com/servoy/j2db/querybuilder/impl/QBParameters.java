/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.querybuilder.impl;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBParameters extends DefaultJavaScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(QBParameters.class);

	private final QBSelect query;
	private final Map<String, QBParameter> parameters = new HashMap<String, QBParameter>();

	QBParameters(Scriptable scriptParent, QBSelect query)
	{
		super(scriptParent, jsFunctions);
		this.query = query;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object obj = super.get(name, start);
		if (obj != null && obj != Scriptable.NOT_FOUND)
		{
			return obj;
		}

		QBParameter param = parameters.get(name);
		try
		{
			if (param != null)
			{
				Object o = param.getValue();
				if (o != null && o != Scriptable.NOT_FOUND && !(o instanceof Scriptable))
				{
					Context context = Context.getCurrentContext();
					if (context != null) o = context.getWrapFactory().wrap(context, start, o, o.getClass());
				}
				return o;
			}
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		return null;
	}

	@Override
	public void put(String name, Scriptable start, Object val)
	{
		Object value = val;
		if (value instanceof Wrapper)
		{
			value = ((Wrapper)value).unwrap();
		}

		try
		{
			if (value instanceof IQueryBuilder)
			{
				value = ((IQueryBuilder)value).build();
			}
			getParameter(name).setValue(value);
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		super.put(name, start, null);
	}

	@Override
	public void delete(String name)
	{
		parameters.remove(name);
		super.delete(name);
	}

	@Override
	public Object[] getIds()
	{
		return parameters.keySet().toArray();
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return parameters.containsKey(name) || super.has(name, start);
	}

	public QBParameter getParameter(String name) throws RepositoryException
	{
		QBParameter param = parameters.get(name);
		if (param == null)
		{
			parameters.put(name, param = new QBParameter(query, name));
		}
		return param;
	}
}
