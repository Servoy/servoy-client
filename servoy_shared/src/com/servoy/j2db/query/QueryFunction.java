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
	// function types
	public static final int SUBSTRING = 0;
	public static final int LOCATE = 1;
	public static final int TRIM = 2;
	public static final int LENGTH = 3;
	public static final int BIT_LENGTH = 4;
	public static final int COALESCE = 5;
	public static final int NULLIF = 6;
	public static final int ABS = 7;
	public static final int MOD = 8;
	public static final int SQRT = 9;
	public static final int UPPER = 10;
	public static final int LOWER = 11;
	public static final int CAST = 12;
	public static final int EXTRACT = 13;

	public static final int CONCAT = 14;
	public static final int DISTINCT = 15;
	public static final int CASTFROM = 16;

	public static final int[] ALL_DEFINED_FUNCTIONS = new int[] { SUBSTRING, LOCATE, TRIM, LENGTH, BIT_LENGTH, COALESCE, NULLIF, ABS, MOD, SQRT, UPPER, LOWER, CAST, EXTRACT, CONCAT, DISTINCT, CASTFROM };


	// functions as defined in Hibernate
	public static final String[] FUNCTION_TYPE_HIBERNATE = new String[] { // standard sql92 functions (defined for each Hibernate dialect)
	"substring", //$NON-NLS-1$
	"locate", //$NON-NLS-1$
	"trim", //$NON-NLS-1$
	"length", //$NON-NLS-1$
	"bit_length", //$NON-NLS-1$
	"coalesce", //$NON-NLS-1$
	"nullif", //$NON-NLS-1$
	"abs", //$NON-NLS-1$
	"mod", //$NON-NLS-1$
	"sqrt", //$NON-NLS-1$
	"upper", //$NON-NLS-1$
	"lower", //$NON-NLS-1$
	"cast", //$NON-NLS-1$
	"extract", //$NON-NLS-1$

	// optional but common functions
	"concat", //$NON-NLS-1$
	"distinct", // slightly abused as function //$NON-NLS-1$

	// added
	"castfrom" //$NON-NLS-1$
	};


	private final int function;
	private IQuerySelectValue[] args;
	private final String name;

	public QueryFunction(int function, IQuerySelectValue[] args, String name)
	{
		this.function = function;
		this.args = args;
		this.name = name;
		getFunctionName(); // throw index error when the function is not defined
	}

	/**
	 * @param function
	 * @param key
	 * @param object
	 */
	public QueryFunction(int function, IQuerySelectValue key, String name)
	{
		this(function, new IQuerySelectValue[] { key }, name);
	}

	public int getFunction()
	{
		return function;
	}

	public String getName()
	{
		return name;
	}

	public String getAlias()
	{
		return name;
	}

	/**
	 * @return function name as defined by Hibernate.
	 */
	public String getFunctionName()
	{
		return FUNCTION_TYPE_HIBERNATE[function];
	}

	public IQuerySelectValue[] getArgs()
	{
		return args;
	}

	public QueryColumn getColumn()
	{
		if (args == null || args.length != 1)
		{
			return null;
		}
		return args[0].getColumn();
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + AbstractBaseQuery.hashCode(this.args);
		result = PRIME * result + this.function;
		result = PRIME * result + ((this.name == null) ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final QueryFunction other = (QueryFunction)obj;
		if (!Arrays.equals(this.args, other.args)) return false;
		if (this.function != other.function) return false;
		if (this.name == null)
		{
			if (other.name != null) return false;
		}
		else if (!this.name.equals(other.name)) return false;
		return true;
	}

	public Object shallowClone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public void acceptVisitor(IVisitor visitor)
	{
		args = (IQuerySelectValue[])AbstractBaseQuery.acceptVisitor(args, visitor);
	}

	@Override
	public String toString()
	{
		return new StringBuffer(getFunctionName().toUpperCase()).append(AbstractBaseQuery.toString(args)).append(' ').append(name).toString();
	}


	///////// serialization ////////////////


	public Object writeReplace()
	{
		return new ReplacedObject(AbstractBaseQuery.QUERY_SERIALIZE_DOMAIN, getClass(), new Object[] { new Integer(function), ReplacedObject.convertArray(args,
			Object.class), name });
	}

	public QueryFunction(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		function = ((Integer)members[i++]).intValue();
		args = (IQuerySelectValue[])ReplacedObject.convertArray((Object[])members[i++], IQuerySelectValue.class);
		name = (String)members[i++];
	}


}
