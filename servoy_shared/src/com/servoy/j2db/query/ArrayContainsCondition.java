/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.j2db.query;

import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Condition for 'array column contains value(s)', used for native array columns (SVY-21478).
 *
 * This is rendered by the database layer with dialect-specific sql (e.g. 'arraycol @> ARRAY[?]' on PostgreSQL,
 * 'elem in (unnest(arraycol))' on HSQLDB), not through Hibernate's own array_contains/array_includes functions:
 * those require the array column to resolve to a Hibernate plural (array) type, which Servoy's generic column type
 * resolver does not provide. Keeping this as a dedicated, self-rendered condition avoids changing that resolver
 * (used by every column in every query) and keeps dialect support local to the database layer, in the same place
 * as the existing array support ({@code DialectExtensionArraySupport}).
 *
 * @author Servoy
 */
public final class ArrayContainsCondition implements ISQLCondition
{
	/**
	 * Whether all elements of {@link #argument} must be present ({@code containsAll}, true) or whether a single
	 * element must be present ({@link #argument} holds that one element, false, {@code contains}).
	 */
	private final boolean all;

	private IQuerySelectValue arrayColumn;
	private Object argument;
	private final boolean negate;

	public ArrayContainsCondition(IQuerySelectValue arrayColumn, Object argument, boolean all)
	{
		this(arrayColumn, argument, all, false);
	}

	private ArrayContainsCondition(IQuerySelectValue arrayColumn, Object argument, boolean all, boolean negate)
	{
		this.arrayColumn = arrayColumn;
		this.argument = argument;
		this.all = all;
		this.negate = negate;
	}

	public IQuerySelectValue getArrayColumn()
	{
		return arrayColumn;
	}

	/**
	 * The element to test for ({@code contains}, {@link #isAll()} false) or the array of elements that must all be
	 * present ({@code containsAll}, {@link #isAll()} true).
	 */
	public Object getArgument()
	{
		return argument;
	}

	public boolean isAll()
	{
		return all;
	}

	public boolean isNegate()
	{
		return negate;
	}

	@Override
	public ISQLCondition negate()
	{
		return new ArrayContainsCondition(arrayColumn, argument, all, !negate);
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	@Override
	public void acceptVisitor(IVisitor visitor)
	{
		arrayColumn = AbstractBaseQuery.acceptVisitor(arrayColumn, visitor);
		argument = AbstractBaseQuery.acceptVisitor(argument, visitor);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (all ? 1231 : 1237);
		result = prime * result + (negate ? 1231 : 1237);
		result = prime * result + ((arrayColumn == null) ? 0 : arrayColumn.hashCode());
		result = prime * result + ((argument == null) ? 0 : argument.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ArrayContainsCondition other = (ArrayContainsCondition)obj;
		if (all != other.all) return false;
		if (negate != other.negate) return false;
		if (arrayColumn == null)
		{
			if (other.arrayColumn != null) return false;
		}
		else if (!arrayColumn.equals(other.arrayColumn)) return false;
		if (argument == null)
		{
			if (other.argument != null) return false;
		}
		else if (!argument.equals(other.argument)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return (negate ? "!" : "") + (all ? "ARRAY_INCLUDES(" : "ARRAY_CONTAINS(") + arrayColumn + ',' + argument + ')'; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { arrayColumn, argument, Boolean.valueOf(all), Boolean.valueOf(negate) });
	}

	public ArrayContainsCondition(ReplacedObject s)
	{
		Object[] array = (Object[])s.getObject();
		arrayColumn = (IQuerySelectValue)array[0];
		argument = array[1];
		all = ((Boolean)array[2]).booleanValue();
		negate = ((Boolean)array[3]).booleanValue();
	}
}
