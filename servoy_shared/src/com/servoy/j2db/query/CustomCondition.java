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

import com.servoy.j2db.util.serialize.ReplacedObject;


/**
 * Query condition class that uses a user-defined string and arguments.
 * 
 * @author rgansevles
 * 
 */
public final class CustomCondition extends QueryCustomElement implements ISQLCondition
{
	public CustomCondition(String condition, Object[] args)
	{
		super(condition, args);
	}

	public ISQLCondition negate()
	{
		if (sql.startsWith("not (") && matchFirstBrace(sql) == sql.length() - 1) //$NON-NLS-1$
		{
			return new CustomCondition(sql.substring(5, sql.length() - 1), args);
		}
		else
		{
			return new CustomCondition("not (" + sql + ')', args); //$NON-NLS-1$
		}
	}

	/**
	 * Check whether the sql is already surrounded by round brackets.
	 */
	public boolean isBracketed()
	{
		return sql.startsWith("(") && matchFirstBrace(sql) == sql.length() - 1; //$NON-NLS-1$
	}

	/**
	 * Return the position of the close brace that matches the first open brace or -1 if not found.
	 * 
	 * @param s
	 * @return
	 */
	private static int matchFirstBrace(String s)
	{
		char[] chars = s.toCharArray();
		int depth = 0;
		for (int c = 0; c < chars.length; c++)
		{
			switch (chars[c])
			{
				case '(' :
					depth++;
					break;

				case ')' :
					if (--depth == 0)
					{
						return c;
					}
					break;
			}
		}

		// not found
		return -1;
	}

	///////// serialization ////////////////

	public CustomCondition(ReplacedObject s)
	{
		super(s);
	}
}
