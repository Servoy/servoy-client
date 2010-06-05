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
package com.servoy.j2db.persistence;

import java.io.Serializable;

/**
 * @author jcompagner
 *
 */
public class ValueDataProvider implements IDataProvider, Serializable
{

	private final String value;
	private final IDataProvider mapped;

	public ValueDataProvider(String value, IDataProvider mapped)
	{
		this.value = value;
		this.mapped = mapped;
	}

	/**
	 * @return the value
	 */
	public Object getValue()
	{
		return Column.getAsRightType(mapped.getDataProviderType(), mapped.getFlags(), value, mapped.getLength(), true);
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#getColumnWrapper()
	 */
	public ColumnWrapper getColumnWrapper()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return "\"" + value + "\""; //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderType()
	 */
	public int getDataProviderType()
	{
		return mapped.getDataProviderType();
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#getFlags()
	 */
	public int getFlags()
	{
		return mapped.getFlags();
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#getLength()
	 */
	public int getLength()
	{
		return mapped.getLength();
	}

	/**
	 * @see com.servoy.j2db.persistence.IDataProvider#isEditable()
	 */
	public boolean isEditable()
	{
		return false;
	}

}
