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


import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * A so called aggregate
 *
 * @author jblok
 */
public class AggregateVariable extends AbstractBase implements IColumn, ISupportUpdateableName, ISupportHTMLToolTipText, ISupportContentEquals
{

	private static final long serialVersionUID = 1L;

	public static final String[] AGGREGATE_TYPE_STRINGS = new String[] { "count", //$NON-NLS-1$
		"maximum", //$NON-NLS-1$
		"minimum", //$NON-NLS-1$
		"average", //$NON-NLS-1$
		"sum" //$NON-NLS-1$
	};


	/**
	 * Constructor I
	 */
	AggregateVariable(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.AGGREGATEVARIABLES, parent, element_id, uuid);
	}

	/*
	 * _____________________________________________________________ Methods from this class
	 */

	public String toHTML()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("<b>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append("</b> "); //$NON-NLS-1$
		sb.append(AGGREGATE_TYPE_STRINGS[getType()]);
		sb.append("( "); //$NON-NLS-1$
		sb.append(getDataProviderIDToAggregate());
		sb.append(" )"); //$NON-NLS-1$
		return sb.toString();
	}


	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/**
	 * Set the name
	 *
	 * @param arg the name
	 */
	public void updateName(IValidateName validator, String arg) throws RepositoryException
	{
		ITable table = getTable();
		if (table == null)
		{
			TableNode node = (TableNode)getParent();
			throw new RepositoryException(ServoyException.InternalCodes.TABLE_NOT_FOUND, new Object[] { node.getTableName() });
		}
		validator.checkName(arg, getID(), new ValidatorSearchContext(table, IRepository.AGGREGATEVARIABLES), true);
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
		getRootObject().getChangeHandler().fireIPersistChanged(this);
	}

	/**
	 * Get the name
	 *
	 * @return the name
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * Set the type
	 *
	 * @param arg the type
	 */
	public void setType(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPE, arg);
	}

	/**
	 * Get the type
	 *
	 * @return the type
	 */
	public int getType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPE).intValue();
	}

	/**
	 * Set the dataProviderIDToAggregate
	 *
	 * @param arg the dataProviderIDToAggregate
	 */
	public void setDataProviderIDToAggregate(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERIDTOAGGREGATE, arg);
	}

	/**
	 * Get the dataProviderIDToAggregate
	 *
	 * @return the dataProviderIDToAggregate
	 */
	public String getDataProviderIDToAggregate()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DATAPROVIDERIDTOAGGREGATE);
	}

	public String getColumnNameToAggregate()
	{
		try
		{
			Column c = getTable().getColumn(getDataProviderIDToAggregate());
			if (c != null)
			{
				return c.getSQLName();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return getDataProviderIDToAggregate();
	}

	public static String getTypeAsString(int type)
	{
		return AGGREGATE_TYPE_STRINGS[type];
	}

	public String getTypeAsString()
	{
		return getTypeAsString(getType());
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/*
	 * _____________________________________________________________ Methods from IDataProvider
	 */
	public String getDataProviderID()
	{
		return getName();
	}

	public int getDataProviderType()
	{
		try
		{
			switch (getType())
			{
				case QueryAggregate.COUNT :
					return IColumnTypes.INTEGER;
				case QueryAggregate.MAX :
				case QueryAggregate.MIN :
//					if (lookup != null)
//					{
//						IDataProvider dp = lookup.getDataProvider(dataProviderIDToAggregate);
//						if (dp != null) return dp.getDataProviderType(lookup);
//					}
					Column c = getTable().getColumn(getDataProviderIDToAggregate());
					if (c != null)
					{
						return Column.mapToDefaultType(c.getType());
					}
					return IColumnTypes.NUMBER;
				case QueryAggregate.AVG :
					return IColumnTypes.NUMBER;
				case QueryAggregate.SUM :
					return IColumnTypes.NUMBER;
				default :
					return -1;
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			return -1;
		}
	}

//	public String[] getDependentDataProviderIDs()
//	{
//		return new String[] { dataProviderIDToAggregate };
//	}

	public ColumnWrapper getColumnWrapper()
	{
		return null;
	}

	public boolean isAggregate()
	{
		return true;
	}

	public int getLength()
	{
		return -1;
	}

	public boolean isEditable()
	{
		return false;
	}

	public int getFlags()
	{
		return Column.NORMAL_COLUMN;
	}

	/**
	 * @see com.servoy.j2db.persistence.IColumn#getTable()
	 */
	public ITable getTable() throws RepositoryException
	{
		// TODO this uses state in the persist..
		TableNode node = (TableNode)getParent();
		return node.getTable();
	}

	//the repository element id can differ!
	public boolean contentEquals(Object obj)
	{
		if (obj instanceof AggregateVariable)
		{
			AggregateVariable other = (AggregateVariable)obj;
			return (getDataProviderID().equals(other.getDataProviderID()) && getType() == other.getType() &&
				getDataProviderIDToAggregate().equals(other.getDataProviderIDToAggregate()));
		}
		return false;
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IColumn#setSequenceType(int)
	 */
	@Override
	public void setSequenceType(int sequenceType)
	{
		// TODO Auto-generated method stub

	}
}
