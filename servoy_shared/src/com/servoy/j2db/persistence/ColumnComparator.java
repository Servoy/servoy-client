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


import java.util.Comparator;
import java.util.List;

import com.servoy.base.persistence.IBaseColumn;

/**
 * @author jcompagner
 */
public class ColumnComparator implements Comparator<IColumn>
{
	public static final ColumnComparator INSTANCE = new ColumnComparator();

	private List<String> indexedNamesList = null;

	/**
	 * @see java.util.Comparator#compare(Object, Object)
	 */
	public int compare(IColumn column1, IColumn column2)
	{
		// Names of IColumn can't be null!!
		if (column1 instanceof Column && column2 instanceof Column)
		{
			if (((Column)column1).getRowIdentType() != IBaseColumn.NORMAL_COLUMN)
			{
				if (((Column)column2).getRowIdentType() == IBaseColumn.NORMAL_COLUMN) return -1;
			}
			else if (((Column)column2).getRowIdentType() != IBaseColumn.NORMAL_COLUMN)
			{
				if (((Column)column1).getRowIdentType() == IBaseColumn.NORMAL_COLUMN) return 1;
			}

		}

		if (indexedNamesList != null)
		{
			Integer index1 = new Integer(indexedNamesList.indexOf(column1.getName()));
			Integer index2 = new Integer(indexedNamesList.indexOf(column2.getName()));
			return index1.compareTo(index2);
		}
		String name1 = column1.getName();
		String name2 = column2.getName();
		return name1.compareToIgnoreCase(name2);
	}

	public void setIndexedNamesList(List<String> indexedNamesList)
	{
		this.indexedNamesList = indexedNamesList;
	}
}
