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
package com.servoy.j2db.query;

import java.util.Arrays;

import com.servoy.base.query.BaseAbstractBaseQuery;
import com.servoy.base.query.BaseColumnType;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * Functions in queries.
 *
 * @author rgansevles
 *
 */
public final class QueryFunction implements IQuerySelectValue
{
	// function types, names must match Hibernate function names
	public enum QueryFunctionType
	{
		// standard sql92 functions (defined for each Hibernate dialect)
		substring,
		locate,
		trim,
		length,
		bit_length,
		coalesce,
		nullif,
		abs,
		mod,
		sqrt,
		upper,
		lower,
		cast,
		extract,

		// extract from date (also standard)
		second,
		minute,
		hour,
		day,
		month,
		year,

		// optional but common functions
		concat,
		floor,
		round,
		ceil,

		// slightly abused as function
		distinct,
		plus,
		minus,
		multiply,
		divide,

		// added
		castfrom,

		// custom
		custom
	}

	private final QueryFunctionType function;
	private IQuerySelectValue[] args;
	private final String name;
	private final String alias;

	public QueryFunction(QueryFunctionType function, IQuerySelectValue[] args, String name, String alias)
	{
		this.function = function;
		this.args = args;
		this.name = name;
		this.alias = alias;
	}

	public QueryFunction(QueryFunctionType function, IQuerySelectValue[] args, String name)
	{
		this(function, args, name, null);
	}

	/**
	 * @param function
	 * @param key
	 * @param object
	 */
	public QueryFunction(QueryFunctionType function, IQuerySelectValue key, String name)
	{
		this(function, new IQuerySelectValue[] { key }, name, null);
	}

	@Override
	public IQuerySelectValue asAlias(String newAlias)
	{
		return new QueryFunction(function, args, name, newAlias);
	}

	public QueryFunctionType getFunction()
	{
		return function;
	}

	public String getName()
	{
		return name;
	}

	public String getAlias()
	{
		return alias == null ? name : alias;
	}

	public IQuerySelectValue[] getArgs()
	{
		return args;
	}

	public QueryColumn getColumn()
	{
		if (args != null && args.length >= 1)
		{
			// for functions that do not change the return type return the first column
			switch (function)
			{
				case upper :
				case lower :
				case trim :
				case abs :
				case mod :
				case substring :
				case floor :
				case round :
				case ceil :
				case distinct :
				case nullif :
				case plus :
				case minus :
				case multiply :
				case divide :
					return args[0].getColumn();

				default :
					break;
			}
		}

		return null;
	}


	@Override
	public BaseColumnType getColumnType()
	{
		// for functions that do not change the return type return the column
		switch (function)
		{
			case second :
			case minute :
			case hour :
			case day :
			case month :
			case year :
			case locate :
			case length :
			case bit_length :
				return ColumnType.getColumnType(IColumnTypes.INTEGER);

			case concat :
				return ColumnType.getColumnType(IColumnTypes.TEXT);

			default :
				break;
		}

		// resolve via getColumn()
		return IQuerySelectValue.super.getColumnType();
	}

	@Override
	public int getFlags()
	{
		// for functions that do not change the return type return the column
		switch (function)
		{
			case second :
			case minute :
			case hour :
			case day :
			case month :
			case year :
			case locate :
			case length :
			case bit_length :
			case concat :
				return 0;

			default :
				break;
		}

		// resolve via getColumn()
		return IQuerySelectValue.super.getFlags();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + ((function == null) ? 0 : function.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		QueryFunction other = (QueryFunction)obj;
		if (!Arrays.equals(args, other.args)) return false;
		if (function != other.function) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		return true;
	}

	@Override
	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		args = AbstractBaseQuery.acceptVisitor(args, visitor);
	}

	@Override
	public String toString()
	{
		return new StringBuilder(function.name().toUpperCase()).append(BaseAbstractBaseQuery.toString(args)).append(' ').append(name).append(
			alias == null ? "" : (" AS " + alias)).toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(),
			new Object[] { function.name(), ReplacedObject.convertArray(args, Object.class), name, alias });
	}

	public QueryFunction(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		function = QueryFunctionType.valueOf((String)members[i++]);
		args = (IQuerySelectValue[])ReplacedObject.convertArray((Object[])members[i++], IQuerySelectValue.class);
		name = (String)members[i++];
		alias = (i < members.length) ? (String)members[i++] : null;
	}

}
