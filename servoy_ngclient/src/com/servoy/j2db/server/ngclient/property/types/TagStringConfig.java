/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

/**
 * @author jcompagner
 *
 */
public class TagStringConfig
{

	private final boolean hideTags;
	private final String forFoundSet;

	/**
	 * @param valueOf
	 * @param optString
	 */
	public TagStringConfig(boolean hideTags, String forFoundSet)
	{
		this.hideTags = hideTags;
		this.forFoundSet = forFoundSet;
	}

	/**
	 * @return the hideTags
	 */
	public boolean isHideTags()
	{
		return hideTags;
	}

	/**
	 * @return the forFoundSet
	 */
	public String getForFoundSet()
	{
		return forFoundSet;
	}
}
