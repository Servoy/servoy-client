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
package com.servoy.j2db.persistence;

import java.io.Serializable;


/** RAGTEST doc
 * Server configuration data.
 *
 * @author rgansevles
 *
 */
public class ServerSettings implements Serializable
{
	public static final ServerSettings DEFAULT = new ServerSettings(false, SortingNullprecedence.ragtestDefault);

	private final boolean sortIgnorecase;
	private final SortingNullprecedence sortingNullprecedence;

	public ServerSettings(boolean sortIgnorecase, SortingNullprecedence sortingNullprecedence)
	{
		this.sortIgnorecase = sortIgnorecase;
		this.sortingNullprecedence = sortingNullprecedence;
	}

	/**
	 * @return the sortIgnorecase
	 */
	public boolean isSortIgnorecase()
	{
		return sortIgnorecase;
	}

	/**
	 * @return the sortingNullprecedence
	 */
	public SortingNullprecedence getSortingNullprecedence()
	{
		return sortingNullprecedence;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (sortIgnorecase ? 1231 : 1237);
		result = prime * result + ((sortingNullprecedence == null) ? 0 : sortingNullprecedence.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ServerSettings other = (ServerSettings)obj;
		if (sortIgnorecase != other.sortIgnorecase) return false;
		if (sortingNullprecedence != other.sortingNullprecedence) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuilder("ServerSettings [")
			.append("sortIgnorecase=").append(sortIgnorecase)
			.append(", sortingNullprecedence=").append(sortingNullprecedence)
			.append("]").toString();
	}
}
