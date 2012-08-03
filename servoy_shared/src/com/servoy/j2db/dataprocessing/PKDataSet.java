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

import com.servoy.j2db.persistence.ITransactable;
import com.servoy.j2db.query.AbstractBaseQuery.PlaceHolderSetter;
import com.servoy.j2db.query.TablePlaceholderKey;
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
	private PksAndRecordsHolder pksAndRecordsHolder;

	private ITransactable transactionListener;

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
		pksUpdated();
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

	public void addRow(int index, Object[] pk)
	{
		pksToBeUpdated();
		if (sortedPKs != null)
		{
			sortedPKs.add(pk);
		}
		pks.addRow(index, pk);
		pksUpdated();
	}

	public void addRow(Object[] pk)
	{
		pksToBeUpdated();
		if (sortedPKs != null)
		{
			sortedPKs.add(pk);
		}
		pks.addRow(pk);
		pksUpdated();
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
				final IDataSet pksBeforeTransaction = pks.clone();
				globalTransaction.addTransactionEndListener(transactionListener = new ITransactable()
				{
					public void processPostRollBack()
					{
						// restore old pks from before transaction, so that pks deleted in the transaction are still found
						if (pksAndRecordsHolder.hasDynamicPlaceholder())
						{
							// query still has the pk set condition, set the condition back to the pk set from before the transaction
							pksAndRecordsHolder.getQuerySelectForReading().acceptVisitor(
								new PlaceHolderSetter(new TablePlaceholderKey(pksAndRecordsHolder.getQuerySelectForReading().getTable(),
									SQLGenerator.PLACEHOLDER_FOUNDSET_PKS), SQLGenerator.createPKValuesArray(
									pksAndRecordsHolder.getFoundSet().getSQLSheet().getTable().getRowIdentColumns(), pksBeforeTransaction)));

							// Note: it is also possible to restore the pks, but that may affect selection since the size of the foundset is based on pk.size
//								pksAndRecordsHolder.setPksAndQuery(pksBeforeTransaction, pksAndRecordsHolder.getDbIndexLastPk(),
//									pksAndRecordsHolder.getQuerySelectForReading());
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

	private void pksUpdated()
	{
		// PKs have been updated, foundset pk placeholder may need to be updated
		if (pksAndRecordsHolder != null && pksAndRecordsHolder.hasDynamicPlaceholder() && pksAndRecordsHolder.getFoundSet() != null &&
			!pksAndRecordsHolder.getFoundSet().isInFindMode())
		{
			pksAndRecordsHolder.getQuerySelectForReading().acceptVisitor(
				new PlaceHolderSetter(
					new TablePlaceholderKey(pksAndRecordsHolder.getQuerySelectForReading().getTable(), SQLGenerator.PLACEHOLDER_FOUNDSET_PKS),
					SQLGenerator.createPKValuesArray(pksAndRecordsHolder.getFoundSet().getSQLSheet().getTable().getRowIdentColumns(), pks)));
		}
	}

	public void removeRow(int index)
	{
		pksToBeUpdated();
		if (sortedPKs != null)
		{
			Object[] pk = pks.getRow(index);
			if (pk != null)
			{
				sortedPKs.remove(pk);
			}
		}
		pks.removeRow(index);
		pksUpdated();
	}

	public void setRow(int index, Object[] pk)
	{
		pksToBeUpdated();
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
		pksUpdated();
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
