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

import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.SortedList;

/**
 * Data set optimized for PKs.
 * 
 * @author rgansevles
 *
 */
public class PKDataSet implements IDataSet, IDelegate<IDataSet>
{
	public static final Comparator<Object[]> PK_COMPARATOR = new Comparator<Object[]>()
	{
		public int compare(Object[] o1, Object[] o2)
		{
			if (o1 == o2) return 0;
			if (o2 == null) return 1;
			if (o1 == null) return -1;

			if (o1.length != o2.length)
			{
				return o1.length - o2.length;
			}

			for (int i = 0; i < o1.length; i++)
			{
				Object el1 = o1[i];
				Object el2 = o2[i];
				if (el1 != el2)
				{
					if (el2 == null) return 1;
					if (el1 == null) return -1;

					int cmp;
					if (el1 instanceof Long && el2 instanceof Long)
					{
						long l1 = ((Long)el1).longValue();
						long l2 = ((Long)el2).longValue();
						cmp = l1 < l2 ? 1 : l1 > l2 ? -1 : 0;
					}
					if (el1 instanceof Number && el2 instanceof Number)
					{
						double d1 = ((Number)el1).doubleValue();
						double d2 = ((Number)el2).doubleValue();
						cmp = d1 < d2 ? 1 : d1 > d2 ? -1 : 0;
					}
					else if (el1 instanceof Comparable< ? > && el1.getClass().isAssignableFrom(el2.getClass()))
					{
						cmp = ((Comparable)el1).compareTo(el2);
					}
					else if (el2 instanceof Comparable< ? > && el2.getClass().isAssignableFrom(el1.getClass()))
					{
						cmp = -1 * ((Comparable)el2).compareTo(el1);
					}
					else
					{
						cmp = el1.toString().compareTo(el2.toString());
					}
					if (cmp != 0) return cmp;
				}
			}

			// the same ?
			return RowManager.createPKHashKey(o1).compareTo(RowManager.createPKHashKey(o2));
		}
	};

	private final IDataSet pks;
	private transient SortedList<Object[]> sortedPKs; // cache of pks for fast lookup, used for matching the the next chunk in FoundSet with the current set.

	public PKDataSet(IDataSet pks)
	{
		if (pks == null)
		{
			throw new NullPointerException();
		}
		this.pks = pks;
	}

	public boolean addColumn(int columnIndex, String columnName, int columnType)
	{
		sortedPKs = null;
		return pks.addColumn(columnIndex, columnName, columnType);
	}

	public void addRow(int index, Object[] pk)
	{
		if (sortedPKs != null)
		{
			sortedPKs.add(pk);
		}
		pks.addRow(index, pk);
	}

	public void addRow(Object[] pk)
	{
		if (sortedPKs != null)
		{
			sortedPKs.add(pk);
		}
		pks.addRow(pk);
	}

	public void clearHadMoreRows()
	{
		sortedPKs = null; // no longer needed
		pks.clearHadMoreRows();
	}

	public int getColumnCount()
	{
		return pks.getColumnCount();
	}

	public String[] getColumnNames()
	{
		return pks.getColumnNames();
	}

	public int[] getColumnTypes()
	{
		return pks.getColumnTypes();
	}

	public Object[] getRow(int row)
	{
		return pks.getRow(row);
	}

	public int getRowCount()
	{
		return pks.getRowCount();
	}

	public boolean hadMoreRows()
	{
		return pks.hadMoreRows();
	}

	public boolean removeColumn(int columnIndex)
	{
		sortedPKs = null;
		return pks.removeColumn(columnIndex);
	}

	public void removeRow(int index)
	{
		if (sortedPKs != null)
		{
			Object[] pk = pks.getRow(index);
			if (pk != null)
			{
				sortedPKs.remove(pk);
			}
		}
		pks.removeRow(index);
	}

	public void setRow(int index, Object[] pk)
	{
		if (sortedPKs != null)
		{
			Object[] orgPk = pks.getRow(index);
			if (orgPk != null)
			{
				sortedPKs.remove(orgPk);
			}
			if (pk != null)
			{
				sortedPKs.add(pk);
			}
		}
		pks.setRow(index, pk);
	}

	public void sort(int column, boolean ascending)
	{
		pks.sort(column, ascending);
	}

	public void sort(Comparator<Object[]> rowComparator)
	{
		pks.sort(rowComparator);
	}

	public IDataSet getDelegate()
	{
		return pks;
	}

	public boolean hasPKCache()
	{
		return sortedPKs != null;
	}

	public void createPKCache()
	{
		if (sortedPKs == null)
		{
			sortedPKs = new SortedList<Object[]>(PK_COMPARATOR, pks.getRowCount());
			for (int i = 0; i < pks.getRowCount(); i++)
			{
				sortedPKs.add(pks.getRow(i));
			}
		}
	}

	public boolean containsPk(Object[] pk)
	{
		if (sortedPKs == null)
		{
			createPKCache();
		}

		return sortedPKs.contains(pk);
	}
}
