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
package com.servoy.j2db.dataprocessing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.util.serialize.IWriteReplace;
import com.servoy.j2db.util.serialize.ReplacedObject;
import com.servoy.j2db.util.visitor.IVisitable;
import com.servoy.j2db.util.visitor.IVisitor;

/**
 * @author jcompagner
 *
 */
public class QueryData implements Serializable, IVisitable, IWriteReplace
{
	private ISQLSelect sqlSelect;
	private ArrayList<TableFilter> filters;
	private final boolean distinctInMemory;
	private final int startRow;
	private final int rowsToRetrieve;
	private final int type;
	private final ITrackingSQLStatement trackingInfo;

	public static String DATAPROCESSING_SERIALIZE_DOMAIN = "D"; //$NON-NLS-1$

	static
	{
		Map<Class< ? extends IWriteReplace>, Short> classMapping = new HashMap<Class< ? extends IWriteReplace>, Short>();

		classMapping.put(QueryData.class, Short.valueOf((short)1));
		classMapping.put(TableFilter.class, Short.valueOf((short)2));
		classMapping.put(DataproviderTableFilterdefinition.class, Short.valueOf((short)3));
		classMapping.put(QueryTableFilterdefinition.class, Short.valueOf((short)4));

		ReplacedObject.installClassMapping(DATAPROCESSING_SERIALIZE_DOMAIN, classMapping);
	}

	public static void initialize()
	{
		// this method triggers the static code above.
	}


	/**
	 *
	 * @param sqlSelect
	 * @param filters
	 * @param distinctInMemory
	 * @param startRow
	 * @param rowsToRetrieve
	 * @param type
	 * @param trackingInfo
	 */
	public QueryData(ISQLSelect sqlSelect, ArrayList<TableFilter> filters, boolean distinctInMemory, int startRow, int rowsToRetrieve, int type,
		ITrackingSQLStatement trackingInfo)
	{
		this.sqlSelect = sqlSelect;
		this.filters = filters;
		this.distinctInMemory = distinctInMemory;
		this.startRow = startRow;
		this.rowsToRetrieve = rowsToRetrieve;
		this.type = type;
		this.trackingInfo = trackingInfo;
	}

	/**
	 * @return the sqlSelect
	 */
	public ISQLSelect getSqlSelect()
	{
		return this.sqlSelect;
	}

	/**
	 * @return the filters
	 */
	public ArrayList<TableFilter> getFilters()
	{
		return this.filters;
	}

	/**
	 * @return the distinctInMemory
	 */
	public boolean isDistinctInMemory()
	{
		return this.distinctInMemory;
	}

	/**
	 * @return the startRow
	 */
	public int getStartRow()
	{
		return this.startRow;
	}

	/**
	 * @return the rowsToRetrieve
	 */
	public int getRowsToRetrieve()
	{
		return this.rowsToRetrieve;
	}

	/**
	 * @return the type
	 */
	public int getType()
	{
		return this.type;
	}

	/**
	 * @return the trackingInfo
	 */
	public ITrackingSQLStatement getTrackingInfo()
	{
		return trackingInfo;
	}

	/**
	 * {@link IVisitable}
	 */
	public void acceptVisitor(IVisitor visitor)
	{
		sqlSelect = AbstractBaseQuery.acceptVisitor(sqlSelect, visitor);
		filters = AbstractBaseQuery.acceptVisitor(filters, visitor);
	}


	///////// serialization ////////////////

	public Object writeReplace()
	{
		// Note: when this serialized structure changes, make sure that old data (maybe saved as serialized xml) can still be deserialized!
		return new ReplacedObject(DATAPROCESSING_SERIALIZE_DOMAIN, getClass(),
			new Object[] { sqlSelect, filters, new int[] { distinctInMemory ? 1 : 0, startRow, rowsToRetrieve, type }, trackingInfo });
	}

	public QueryData(ReplacedObject s)
	{
		Object[] members = (Object[])s.getObject();
		int i = 0;
		sqlSelect = (ISQLSelect)members[i++];
		filters = (ArrayList<TableFilter>)members[i++];

		int[] numbers = (int[])members[i++];
		distinctInMemory = numbers[0] == 1;
		startRow = numbers[1];
		rowsToRetrieve = numbers[2];
		type = numbers[3];
		trackingInfo = (ITrackingSQLStatement)members[i++];
	}
}
