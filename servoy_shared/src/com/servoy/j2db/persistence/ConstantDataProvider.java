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

package com.servoy.j2db.persistence;

/**
 * @author Diana
 *
 */
public class ConstantDataProvider implements IDataProvider
{
	private final String name;
	private final int type;

	public ConstantDataProvider(String name, int type)
	{
		this.name = name;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderID()
	 */
	@Override
	public String getDataProviderID()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getColumnWrapper()
	 */
	@Override
	public ColumnWrapper getColumnWrapper()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getLength()
	 */
	@Override
	public int getLength()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#isEditable()
	 */
	@Override
	public boolean isEditable()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getFlags()
	 */
	@Override
	public int getFlags()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderType()
	 */
	@Override
	public int getDataProviderType()
	{
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public String toString()
	{
		return "ConstantsDataProvider: " + getDataProviderID() + " type: " + getDataProviderType();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ConstantDataProvider)
		{
			return ((ConstantDataProvider)obj).getDataProviderID().equals(getDataProviderID()) &&
				((ConstantDataProvider)obj).getDataProviderType() == getDataProviderType();
		}
		return false;
	}

}
