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


import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * This class is a calculated script variable, which can use multiple other dataproviders in it's calculation<br>
 * It also can store the result of the calculation (a so called stored calculation)
 *
 * @author jblok
 */
public class ScriptCalculation extends AbstractScriptProvider implements IDataProvider, IColumn, ISupportHTMLToolTipText, ICloneable

{
	/**
	 * Constructor I
	 */
	ScriptCalculation(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.SCRIPTCALCULATIONS, parent, element_id, uuid);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public String toHTML()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("<html>"); //$NON-NLS-1$
		sb.append("<b>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append("</b> "); //$NON-NLS-1$
		sb.append(Column.getDisplayTypeString(Column.mapToDefaultType(getType())));
		String code = getMethodCode();
		if (code != null)
		{
			sb.append(" calculation: <br>"); //$NON-NLS-1$
			sb.append(getMethodCode().substring(0, Math.min(code.length(), 30)));
		}
		sb.append("</html>"); //$NON-NLS-1$
		return sb.toString();
	}

	/*
	 * _____________________________________________________________ Methods from IDataProvider
	 */

	public ColumnWrapper getColumnWrapper()
	{
		return null;
	}

	public boolean isAggregate()
	{
		return false;
	}

	public int getLength()
	{
		return -1;
	}

	public boolean isEditable()
	{
		return false;
	}

	@SuppressWarnings("nls")
	public int getFlags()
	{
		String declaration = getDeclaration();
		if (declaration != null)
		{
			int index = declaration.indexOf("*/");
			if (index != -1)
			{
				int typeIndex = declaration.lastIndexOf("@type", index);
				if (typeIndex != -1)
				{
					if (declaration.substring(typeIndex, declaration.indexOf('\n', typeIndex)).indexOf("{UUID}") != -1)
					{
						return Column.NORMAL_COLUMN | Column.UUID_COLUMN;
					}
				}
			}
		}
		return Column.NORMAL_COLUMN;
	}

	public int getDataProviderType()
	{
		try
		//if stored calc type is enforced to be the same
		{
			Table table = getTable();
			Column c = table.getColumn(getName());
			if (c != null)
			{
				return c.getDataProviderType();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return Column.mapToDefaultType(getType()); //not stored, use type
	}

	public String getTypeAsString()
	{
		return Column.getDisplayTypeString(getDataProviderType());
	}

	/**
	 * @see com.servoy.j2db.persistence.IColumn#getTable()
	 */
	public Table getTable() throws RepositoryException
	{
		TableNode node = (TableNode)getParent();
		return node.getTable();
	}

	/**
	 * Returns the type.
	 *
	 * @return int
	 */
	public int getType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPE).intValue();
	}

	public void setType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPE, arg);
	}

	/**
	 * Sets the type.
	 *
	 * @param type The type to set
	 */
	public void setTypeAndCheck(int arg, IServiceProvider application)
	{
		int dpType = arg;
		try
		//if stored calc type is enforced to be the same
		{
			Table table = getTable();
			if (table != null)
			{
				Column c = table.getColumn(getName());
				if (c != null)
				{
					// use dataprovider type as defined by converter
					ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, c, application);
					dpType = componentFormat.dpType;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		setType(dpType);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#getColumnInfo()
	 */
	@Override
	public ColumnInfo getColumnInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#getExistInDB()
	 */
	@Override
	public boolean getExistInDB()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#getAllowNull()
	 */
	@Override
	public boolean getAllowNull()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#getConfiguredColumnType()
	 */
	@Override
	public ColumnType getConfiguredColumnType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#removeColumnInfo()
	 */
	@Override
	public void removeColumnInfo()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#getSequenceType()
	 */
	@Override
	public int getSequenceType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IColumn#setAllowNull(boolean)
	 */
	@Override
	public void setAllowNull(boolean allowNull)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.persistence.IColumn#setDatabasePK(boolean)
	 */
	@Override
	public void setDatabasePK(boolean pkColumn)
	{
		// TODO Auto-generated method stub

	}
}
