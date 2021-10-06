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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.TablePlaceholderKey;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class PksAndRecordsHolder
{
	private transient SafeArrayList<IRecordInternal> cachedRecords = new SafeArrayList<IRecordInternal>(5); //row -> State Note:is based on the 'pks'
	private PKDataSet pks; //the primary keys so far
	private AtomicInteger dbIndexLastPk; // mutable integer wrapper, use wrapper in stead of primitive so that shallow copy can update same data
	private QuerySelect querySelect; // the query the pks were based on

	private final int chunkSize;
	private final boolean optimzeChangeFires;
	private final FoundSet foundSet;
	private boolean hasDynamicPlaceholder;

	private PksAndRecordsHolder(FoundSet foundSet, SafeArrayList<IRecordInternal> cachedRecords, IDataSet pks, AtomicInteger dbIndexLastPk,
		QuerySelect querySelect, int chunkSize, boolean hasDynamicPlaceholder, boolean optimzeChangeFires)
	{
		this(foundSet, chunkSize, optimzeChangeFires);
		this.cachedRecords = cachedRecords;
		this.pks = pks == null || pks instanceof PKDataSet ? (PKDataSet)pks : new PKDataSet(pks);
		if (this.pks != null)
		{
			this.pks.setPksAndRecordsHolder(this);
		}
		this.dbIndexLastPk = dbIndexLastPk;
		this.querySelect = querySelect;
		this.hasDynamicPlaceholder = hasDynamicPlaceholder;
	}

	public PksAndRecordsHolder(FoundSet foundSet, int chunkSize, boolean optimzeChangeFires)
	{
		this.foundSet = foundSet;
		this.chunkSize = chunkSize;
		this.optimzeChangeFires = optimzeChangeFires;
	}

	public synchronized PksAndRecordsHolder shallowCopy()
	{
		return new PksAndRecordsHolder(foundSet, cachedRecords, pks, dbIndexLastPk, querySelect, chunkSize, hasDynamicPlaceholder, optimzeChangeFires);
	}

	public synchronized IFoundSetChanges setPks(IDataSet bufferedDataSet, int dbIndexLastPk)
	{
		return setPksAndQuery(bufferedDataSet, dbIndexLastPk, this.querySelect, false);
	}

	public synchronized IFoundSetChanges setPksAndQuery(IDataSet bufferedDataSet, int dbIndexLastPk, QuerySelect querySelect)
	{
		return setPksAndQuery(bufferedDataSet, dbIndexLastPk, querySelect, false);
	}

	/**
	 * @param bufferedDataSet
	 * @param querySelect
	 */
	public synchronized IFoundSetChanges setPksAndQuery(IDataSet bufferedDataSet, int dbIndexLastPk, QuerySelect querySelect, boolean reuse)
	{
		FoundsetChanges changes = null;
		if (optimzeChangeFires && pks != bufferedDataSet && pks != null && pks.getRowCount() > 0 &&
			pks.getColumnCount() == 1 && bufferedDataSet != null && bufferedDataSet.getRowCount() > 0)
		{
			changes = new FoundsetChanges();
			// if there are currently already pks. and this is a pks set of 1 column try to generate a change object.
			int smallestSize = Math.min(pks.getRowCount(), bufferedDataSet.getRowCount());
			for (int i = 0; i < smallestSize; i++)
			{
				changes.record(i, Utils.equalObjects(pks.getRow(i)[0], bufferedDataSet.getRow(i)[0]));
			}
			if (pks.getRowCount() > bufferedDataSet.getRowCount())
			{
				changes.add(new FoundsetChange(FoundSetEvent.CHANGE_DELETE, bufferedDataSet.getRowCount(), pks.getRowCount() - 1));
			}
			else if (pks.getRowCount() < bufferedDataSet.getRowCount())
			{
				changes.add(new FoundsetChange(FoundSetEvent.CHANGE_INSERT, pks.getRowCount(), bufferedDataSet.getRowCount() - 1));
			}
		}
		pks = bufferedDataSet == null || bufferedDataSet instanceof PKDataSet ? (PKDataSet)bufferedDataSet : new PKDataSet(bufferedDataSet);
		this.dbIndexLastPk = new AtomicInteger(dbIndexLastPk);
		this.querySelect = querySelect;
		this.hasDynamicPlaceholder = checkForDynamicPlaceholder(querySelect);
		if (this.pks != null) this.pks.setPksAndRecordsHolder(this);

		if (reuse)
		{
			cachedRecords = reUseStatesBasedOnNewPrimaryKeys();
		}
		else
		{
			cachedRecords = new SafeArrayList<IRecordInternal>((pks != null ? pks.getRowCount() : 0) + 5);//(re)new
		}
		return changes;
	}

	public synchronized SafeArrayList<IRecordInternal> getCachedRecords()
	{
		return cachedRecords;
	}

	public synchronized PKDataSet getPks()
	{
		return pks;
	}

	public synchronized PKDataSet getPksClone()
	{
		if (pks == null) return null;
		PKDataSet clone = pks.clone();
		clone.setPksAndRecordsHolder(null); // does not belong to this holder anymore
		return clone;
	}


	public FoundSet getFoundSet()
	{
		return foundSet;
	}

	public int getDbIndexLastPk()
	{
		return dbIndexLastPk.get();
	}

	public void setDbIndexLastPk(int dbIndexLastPk)
	{
		this.dbIndexLastPk.set(dbIndexLastPk);
	}

	/**
	 * Get the querySelect for reading only, make no change to the query!
	 */
	public synchronized QuerySelect getQuerySelectForReading()
	{
		return querySelect;
	}

	/**
	 * Get a clone of the querySelect
	 */
	public synchronized QuerySelect getQuerySelectForModification()
	{
		return AbstractBaseQuery.deepClone(querySelect);
	}

	private SafeArrayList<IRecordInternal> reUseStatesBasedOnNewPrimaryKeys()
	{
		SafeArrayList<IRecordInternal> retval = new SafeArrayList<IRecordInternal>((pks != null ? pks.getRowCount() : 0) + 5);//(re)new
		if (cachedRecords.size() > 3 * chunkSize)
		{
			return retval;//sub optimal just re query when needed
		}
		if (pks != null)
		{
			for (int i = 0; i < pks.getRowCount(); i++)
			{
				Object[] rawpk = pks.getRow(i);
				String calcPKHashKey = RowManager.createPKHashKey(rawpk);
				for (int j = cachedRecords.size() - 1; j >= 0; j--)//loop reverse more likely it is found earlier
				{
					IRecordInternal state = cachedRecords.get(j);
					if (state == null) continue;
					if (calcPKHashKey.equals(state.getPKHashKey()))
					{
						retval.set(i, state);
						cachedRecords.set(j, null);
						break;
					}
				}
			}
		}
		return retval;
	}

	private static boolean checkForDynamicPlaceholder(QuerySelect querySelect)
	{
		return querySelect != null &&
			querySelect.getPlaceholder(new TablePlaceholderKey(querySelect.getTable(), SQLGenerator.PLACEHOLDER_FOUNDSET_PKS)) != null;
	}

	public boolean hasDynamicPlaceholder()
	{
		return hasDynamicPlaceholder;
	}

	/**
	 * Sort entries in PKs as in sortedPKs.
	 *
	 * @param sortedPKs
	 */
	public synchronized void reorder(IDataSet sortedPKs)
	{
		if (pks == null || sortedPKs == null)
		{
			return;
		}
		int dsIndex = 0;
		// for each pk in sortedPKs
		for (int sorted = 0; sorted < sortedPKs.getRowCount(); sorted++)
		{
			Object[] sortedPK = sortedPKs.getRow(sorted);
			if (sortedPK != null && sortedPK.length > 0)
			{
				// search for the pk in pks, starting from what is already sorted
				boolean equal = false;
				int i;
				Object[] pk = null;
				for (i = dsIndex; !equal && sortedPK != null && i < pks.getRowCount(); i++)
				{
					pk = pks.getRow(i);
					// compare rows using Utils.equalObjects (possibly values from JS and DB have to be compared)
					equal = pk != null && pk.length == sortedPK.length;
					for (int r = 0; equal && r < pk.length; r++)
					{
						equal = Utils.equalObjects(pk[r], sortedPK[r]);
					}
				}
				if (equal)
				{
					// equal pk found
					if (i - 1 != dsIndex)
					{
						// flip pk(i) and pk(dsindex)
						Object[] tmppk = pks.getRow(dsIndex);
						pks.setRow(dsIndex, pk, false);
						pks.setRow(i - 1, tmppk, false);
						IRecordInternal tmprec = cachedRecords.get(dsIndex);
						cachedRecords.set(dsIndex, cachedRecords.get(i - 1));
						cachedRecords.set(i - 1, tmprec);
					}
					// else pk was on right position already

					// we are sorted up to dsIndex
					dsIndex++;
				}
				// else pk was not found in pks
			}
		}
	}

	public synchronized void rowPkUpdated(String oldPkHash, Row row)
	{
		for (int i = 0; pks != null && i < pks.getRowCount(); i++)
		{
			if (oldPkHash.equals(RowManager.createPKHashKey(pks.getRow(i))))
			{
				pks.setRow(i, row.getPK());
				return;
			}
		}
	}
}

class FoundsetChanges implements IFoundSetChanges
{
	private final List<IFoundSetChange> changes = new ArrayList<>();

	private FoundsetChange current;

	public void record(int row, boolean isEquals)
	{
		if (isEquals && current == null) return;
		if (isEquals && current != null)
		{
			testCurrent();
			return;
		}
		if (!isEquals && current == null)
		{
			current = new FoundsetChange(FoundSetEvent.CHANGE_UPDATE, row);
		}
		else
		{
			current.lastRow = row;
		}
	}

	/**
	 * @param foundsetChange
	 */
	public void add(FoundsetChange change)
	{
		testCurrent();
		changes.add(change);
	}

	/**
	 *
	 */
	private void testCurrent()
	{
		if (current != null)
		{
			changes.add(current);
			current = null;
		}
	}

	@Override
	public List<IFoundSetChange> getChanges()
	{
		testCurrent();
		return changes;
	}

	@Override
	public String toString()
	{
		return "FoundSetChanges[" + changes + ']'; //$NON-NLS-1$
	}
}

class FoundsetChange implements IFoundSetChange
{

	private final int type;
	private final int firstRow;

	int lastRow;

	public FoundsetChange(int type, int firstRow)
	{
		this.type = type;
		this.firstRow = firstRow;
		this.lastRow = firstRow;
	}

	/**
	 * @param type
	 * @param firstRow
	 * @param firstRow
	 */
	public FoundsetChange(int type, int firstRow, int lastRow)
	{
		this.type = type;
		this.firstRow = firstRow;
		this.lastRow = lastRow;
	}

	@Override
	public int getType()
	{
		return type;
	}

	@Override
	public int getFirstRow()
	{
		return firstRow;
	}

	@Override
	public int getLastRow()
	{
		return lastRow;
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "FoundSetChange[type: " + type + ", firstrow: " + firstRow + " ,lastrow: " + lastRow + ']';
	}
}
