/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import com.servoy.j2db.persistence.SortingNullprecedence;

/**
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public class SortOptions
{
	public static SortOptions NONE = new SortOptions(false, SortingNullprecedence.ragtestDefault);

	private final boolean ignoreCase;
	private final SortingNullprecedence nullprecedence;

	public SortOptions(boolean ignoreCase, SortingNullprecedence nullprecedence)
	{
		this.ignoreCase = ignoreCase;
		this.nullprecedence = nullprecedence;
	}

	public boolean ignoreCase()
	{
		return ignoreCase;
	}

	public SortingNullprecedence nullprecedence()
	{
		return nullprecedence;
	}

	public SortOptions withIgnoreCase(boolean ic)
	{
		return this.ignoreCase == ic ? this : new SortOptions(ic, this.nullprecedence);
	}

	public SortOptions withNullprecedence(SortingNullprecedence np)
	{
		return this.nullprecedence == np ? this : new SortOptions(this.ignoreCase, np);
	}

	public String display()
	{
		return ignoreCase ? "#" + nullprecedence.display() : nullprecedence.display();
	}

	@Override
	public String toString()
	{
		return "SortOptions(" + display() + ")";
	}
}
