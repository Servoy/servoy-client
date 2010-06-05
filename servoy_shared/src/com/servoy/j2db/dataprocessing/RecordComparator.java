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



import java.util.Comparator;
import java.util.List;

import com.servoy.j2db.util.Utils;

/**
 * Can sort records based on there values
 * @author jcompagner
 */
public class RecordComparator implements Comparator
{
	protected IFoundSetInternal foundSet;
	protected List sortColumns;
	
	public RecordComparator(IFoundSetInternal foundSet, List sortColumns)
	{
		super();
		this.foundSet = foundSet;
		this.sortColumns = sortColumns;
	}

	public RecordComparator(List sortColumns)
	{
		super();
		this.sortColumns = sortColumns;
	}

	public int compare(Object o1, Object o2)
	{
		int ret = 0;
		IRecordInternal record1 = null;
		IRecordInternal record2 = null;
		if (foundSet != null)
		{
			record1 = foundSet.getRecord(((Integer)o1).intValue());
			record2 = foundSet.getRecord(((Integer)o2).intValue());
		}
		else
		{
			record1 = (IRecordInternal)o1;
			record2 = (IRecordInternal)o2;
		}
		
		for (int i = 0; i < sortColumns.size(); i++)
		{
			SortColumn sc = (SortColumn) sortColumns.get(i);
			String dataProviderId = sc.getDataProviderID();
			int ascending = (sc.getSortOrder() == SortColumn.ASCENDING ? 1 : -1);
			
			Comparable comp1 = (Comparable) record1.getValue(dataProviderId);
			Comparable comp2 = (Comparable) record2.getValue(dataProviderId);
			if (comp1 == null && comp2 == null) continue;
			if (comp1 == null) return 1*ascending;
			if (comp2 == null) return -1*ascending;
			
			if (comp1 instanceof String)
			{
				ret = ((String)comp1).compareToIgnoreCase((String)comp2);
			}
			else if (comp1 instanceof Number)
			{
				ret = Utils.compare(((Number)comp1).doubleValue(), ((Number)comp2).doubleValue());
			}
			else
			{
				ret = comp1.compareTo(comp2);
			}
			if (ret != 0) return (ret * ascending);
		}
		return ret;
	}
}
