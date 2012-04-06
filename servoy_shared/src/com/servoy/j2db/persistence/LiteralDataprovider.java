/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
 * @since 6.1
 */
public class LiteralDataprovider implements IDataProvider
{
	public static final String LITERAL_PREFIX = "LITERAL:"; //$NON-NLS-1$

	private final String literalWithPrefix;

	public LiteralDataprovider(String literal)
	{
		if (!literal.startsWith(LITERAL_PREFIX))
		{
			this.literalWithPrefix = LITERAL_PREFIX + literal;
		}
		else
		{
			this.literalWithPrefix = literal;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IDataProvider#getDataProviderID()
	 */
	public String getDataProviderID()
	{
		return literalWithPrefix;
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

	public Object getValue()
	{
		String value = literalWithPrefix.substring(LITERAL_PREFIX.length());
		return Utils.parseJSExpression(value);
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
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "LiteralDataprovider[value:" + getValue() + "]"; //$NON-NLS-1$//$NON-NLS-2$
	}

}
