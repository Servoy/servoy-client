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

package com.servoy.j2db.util.xmlxport;

import java.io.Serializable;

public class ServerDef implements Serializable
{
	public final String name;
	public String dbiFileContents = null;

	public ServerDef(String name)
	{
		this.name = name;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj != null && (getClass() == obj.getClass()) && name.equals(((ServerDef)obj).name);
	}
}
