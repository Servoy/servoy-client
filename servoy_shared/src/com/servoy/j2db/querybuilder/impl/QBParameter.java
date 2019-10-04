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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.IQueryElement;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.querybuilder.IQueryBuilderParameter;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author rgansevles
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class QBParameter extends QBPart implements IQueryBuilderParameter
{
	static final String PARAMETER_PREFIX = "P:";

	private final TablePlaceholderKey key;
	private boolean isSet = false;
	private Object value;

	public QBParameter(QBSelect parent, String name)
	{
		super(parent, parent);
		this.key = new TablePlaceholderKey(parent.getQueryTable(), PARAMETER_PREFIX + name);
	}

	TablePlaceholderKey getPlaceholderKey()
	{
		return key;
	}

	@Override
	@JSReadonlyProperty
	public QBSelect getParent()
	{
		return (QBSelect)super.getParent();
	}

	public String getName()
	{
		return key.getName().substring(PARAMETER_PREFIX.length());
	}

	public Object getValue() throws RepositoryException
	{
		Placeholder placeholder = getParent().getQuery().getPlaceholder(key);
		if (placeholder != null && placeholder.isSet())
		{
			value = placeholder.getValue();
		}
		return value;
	}

	public void setValue(Object value) throws RepositoryException
	{
		// Do not set placeholder directly, some objects handle setValue special, see SetCondition
		AbstractBaseQuery.setPlaceholderValue(getParent().getQuery(), key, value);
//		Placeholder placeholder = getParent().getQuery().getPlaceholder(key);
//		if (placeholder != null)
//		{
//			placeholder.setValue(value);
//		}
		this.value = value;
		isSet = true;
	}

	public boolean isSet() throws RepositoryException
	{
		Placeholder placeholder = getParent().getQuery().getPlaceholder(key);
		if (placeholder != null)
		{
			isSet = placeholder.isSet();
		}
		return isSet;
	}

	/*
	 * The quey object for parameter is the placeholder
	 *
	 * @see com.servoy.j2db.querybuilder.impl.QBPart#build()
	 */
	@Override
	public IQueryElement build()
	{
		Placeholder placeholder = getParent().getQuery().getPlaceholder(key);
		return placeholder == null ? new Placeholder(key) : placeholder;
	}
}
