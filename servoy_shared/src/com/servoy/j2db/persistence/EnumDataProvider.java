/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class EnumDataProvider implements IDataProvider
{
	private final String name;
	private final int type;

	public EnumDataProvider(String name, int type)
	{
		this.name = name;
		this.type = type;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getColumnWrapper()
	 */
	public ColumnWrapper getColumnWrapper()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getLength()
	 */
	public int getLength()
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#isEditable()
	 */
	public boolean isEditable()
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getFlags()
	 */
	public int getFlags()
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderType()
	 */
	public int getDataProviderType()
	{
		return type;
	}

	@Override
	public String toString()
	{
		return "EnumDataprovider:" + getDataProviderID();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof EnumDataProvider))
		{
			return false;
		}
		if (obj == this)
		{
			return true;
		}
		//in FS we create this untyped, i think name is enough to identify dp ?
		return Utils.equalObjects(name, ((EnumDataProvider)obj).name);
	}
}
