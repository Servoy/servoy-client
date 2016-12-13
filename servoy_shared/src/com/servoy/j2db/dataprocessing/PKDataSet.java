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

import com.servoy.j2db.persistence.ITransactable;
import com.servoy.j2db.query.AbstractBaseQuery.PlaceHolderSetter;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

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
	private transient PksAndRecordsHolder pksAndRecordsHolder;

	private transient ITransactable transactionListener;

	public PKDataSet(IDataSet pks)
	{
		if (pks == null)
		{
			throw new NullPointerException();
		}
		this.pks = pks;
	}

	/**
	 * @param pksAndRecordsHolder the pksAndRecordsHolder to set
	 */
	public void setPksAndRecordsHolder(PksAndRecordsHolder pksAndRecordsHolder)
	{
		this.pksAndRecordsHolder = pksAndRecordsHolder;
	}

	@Override
	public PKDataSet clone()
	{
		return new PKDataSet(pks.clone());
	}

	public boolean addColumn(int columnIndex, String columnName, int columnType)
	{
		sortedPKs = null;
		return pks.addColumn(columnIndex, columnName, columnType);
	}

	public void setColumnName(int columnIndex, String columnName)
	{
		pks.setColumnName(columnIndex, columnName);
	}

	public void addRow(int index, Object[] pk)
	{
		pksToBeUpdated();
		if (sortedPKs != null)
		{
			sortedPKs.add(pk);
		}
		pks.addRow(index, pk);

		// pk is added, update the dynamic pk values holder.
		if (pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder() && pksAndRecordsHolder.getFoundSet() != null &&
			!pksAndRecordsHolder.getFoundSet().isInFindMode())
		{
			DynamicPkValuesArray dynArray = getDynamicPkValuesArray();
			if (dynArray != null)
			{
				dynArray.getPKs().addRow(pk); // index does not matter
			}
		}
	}

	public void addRow(Object[] pk)
	{
		addRow(pks.getRowCount(), pk);
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

	private void pksToBeUpdated()
	{
		// When a dynamic pk condition is used (placeholder SQLGenerator.PLACEHOLDER_FOUNDSET_PKS), and we are in a transaction, the
		// pk set from before the transaction is saved and restored on rollback,  so that after rollback, deleted records are still found.

		if (transactionListener == null && pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder())
		{
			// check if I am within a transaction
			GlobalTransaction globalTransaction = pksAndRecordsHolder.getFoundSet().getFoundSetManager().getGlobalTransaction();
			if (globalTransaction != null)
			{
				// add a listener for rollback to restore pks on rollback so that query returns deleted records
				DynamicPkValuesArray dynArray = getDynamicPkValuesArray();
				if (dynArray != null)
				{
					final DynamicPkValuesArray pksBeforeTransaction = dynArray.clone();

					globalTransaction.addTransactionEndListener(transactionListener = new ITransactable()
					{
						public void processPostRollBack()
						{
							// restore old pks from before transaction, so that pks deleted in the transaction are still found
							if (pksAndRecordsHolder.hasDynamicPlaceholder())
							{
								// query still has the pk set condition, set the condition back to the pk set from before the transaction
								pksAndRecordsHolder.getQuerySelectForReading().acceptVisitor(new PlaceHolderSetter(
									new TablePlaceholderKey(pksAndRecordsHolder.getQuerySelectForReading().getTable(), SQLGenerator.PLACEHOLDER_FOUNDSET_PKS),
									pksBeforeTransaction));
							}
							transactionListener = null;
						}

						public void processPostCommit()
						{
							transactionListener = null;
						}
					});
				}
			}
		}
	}

	private DynamicPkValuesArray getDynamicPkValuesArray()
	{
		if (pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder() && pksAndRecordsHolder.getFoundSet() != null &&
			!pksAndRecordsHolder.getFoundSet().isInFindMode())
		{
			Placeholder placeholder = pksAndRecordsHolder.getQuerySelectForReading().getPlaceholder(
				new TablePlaceholderKey(pksAndRecordsHolder.getQuerySelectForReading().getTable(), SQLGenerator.PLACEHOLDER_FOUNDSET_PKS));
			Object value = placeholder.getValue();
			if (value instanceof DynamicPkValuesArray)
			{
				return (DynamicPkValuesArray)value;
			}
		}
		return null;
	}

	public void removeRow(int index)
	{
		pksToBeUpdated();
		Object[] pk = pks.getRow(index);
		if (pk != null && sortedPKs != null)
		{
			sortedPKs.remove(pk);
		}
		pks.removeRow(index);

		// pk is removed, update the dynamic pk values holder.
		if (pk != null && pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder() && pksAndRecordsHolder.getFoundSet() != null &&
			!pksAndRecordsHolder.getFoundSet().isInFindMode())
		{
			DynamicPkValuesArray dynArray = getDynamicPkValuesArray();
			if (dynArray != null)
			{
				IDataSet ds = dynArray.getPKs();
				for (int i = ds.getRowCount() - 1; i >= 0; i--)
				{
					if (Utils.equalObjects(pk, ds.getRow(i)))
					{
						ds.removeRow(i);
					}
				}
			}
		}
	}

	public void setRow(int index, Object[] pk)
	{
		setRow(index, pk, true);
	}

	public void setRow(int index, Object[] pk, boolean updateDynamicPKCondition)
	{
		if (updateDynamicPKCondition) pksToBeUpdated();
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
		Object[] oldpk = pks.getRow(index);
		pks.setRow(index, pk);

		// // pk is updated, update the dynamic pk values holder if needed.
		if (updateDynamicPKCondition && pk != null && pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder() &&
			pksAndRecordsHolder.getFoundSet() != null && !pksAndRecordsHolder.getFoundSet().isInFindMode())
		{
			DynamicPkValuesArray dynArray = getDynamicPkValuesArray();
			if (dynArray != null)
			{
				if (oldpk != null)
				{
					// updated pk, remove old one
					IDataSet ds = dynArray.getPKs();
					for (int i = ds.getRowCount() - 1; i >= 0; i--)
					{
						if (Utils.equalObjects(oldpk, ds.getRow(i)))
						{
							ds.removeRow(i);
						}
					}
				}
				dynArray.getPKs().addRow(pk); // index does not matter
			}
		}
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

	@Override
	public List<Object[]> getRows()
	{
		return pks.getRows();
	}
}
