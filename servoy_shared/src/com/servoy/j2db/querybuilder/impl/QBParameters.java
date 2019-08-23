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

import static com.servoy.j2db.querybuilder.impl.QBParameter.PARAMETER_PREFIX;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.TypePredicate;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBParameters extends DefaultJavaScope
{
	private static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(QBParameters.class);

	private final QBSelect query;

	QBParameters(Scriptable scriptParent, QBSelect query)
	{
		super(scriptParent, jsFunctions);
		this.query = query;

		QuerySelect querySelect = query.getQuery(false);
		if (querySelect != null)
		{
			// get all existing parameters from the query
			AbstractBaseQuery.<TablePlaceholderKey> search(querySelect,
				new TypePredicate<>(TablePlaceholderKey.class, key -> key.getName().startsWith(PARAMETER_PREFIX))) //
				.forEach(key -> getParameter(key.getName().substring(PARAMETER_PREFIX.length())));
		}
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		QBParameter parameter = (QBParameter)allVars.get(name);
		if (parameter != null)
		{
			Object value = null;
			try
			{
				value = parameter.getValue();
			}
			catch (RepositoryException e)
			{
				Debug.log(e);
			}
			if (value != null && value != Scriptable.NOT_FOUND && !(value instanceof Scriptable))
			{
				Context context = Context.getCurrentContext();
				if (context != null) value = context.getWrapFactory().wrap(context, start, value, value.getClass());
			}
			return value;
		}

		return super.get(name, start);
	}

	@Override
	public void put(String name, Scriptable start, Object val)
	{
		// the parameter has to be created first using QBSelect.getParameter(), otherwise the parameter value is not RAGTEST
		QBParameter parameter = (QBParameter)allVars.get(name);
		if (parameter != null)
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

				parameter.setValue(value);
			}
			catch (RepositoryException e)
			{
				Debug.log(e);
			}
			super.put(name, start, parameter);
		}
	}

	@Override
	public Object remove(String key)
	{
		// ignore
		return null;
	}


	@Override
	public void delete(String name)
	{
		// ignore
	}

	QBParameter getParameter(String name)
	{
		QBParameter param = (QBParameter)allVars.get(name);
		if (param == null)
		{
			allVars.put(name, param = new QBParameter(query, name));
		}
		return param;
	}
}
