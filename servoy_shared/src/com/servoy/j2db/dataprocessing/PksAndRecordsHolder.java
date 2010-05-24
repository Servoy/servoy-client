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

import java.util.concurrent.atomic.AtomicInteger;

import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
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

	private PksAndRecordsHolder(SafeArrayList<IRecordInternal> cachedRecords, IDataSet pks, AtomicInteger dbIndexLastPk, QuerySelect querySelect, int chunkSize)
	{
		this(chunkSize);
		this.cachedRecords = cachedRecords;
		this.pks = pks == null || pks instanceof PKDataSet ? (PKDataSet)pks : new PKDataSet(pks);
		this.dbIndexLastPk = dbIndexLastPk;
		this.querySelect = querySelect;
	}

	public PksAndRecordsHolder(int chunkSize)
	{
		this.chunkSize = chunkSize;
	}

	public synchronized PksAndRecordsHolder shallowCopy()
	{
		return new PksAndRecordsHolder(cachedRecords, pks, dbIndexLastPk, querySelect, chunkSize);
	}

	public synchronized SafeArrayList<IRecordInternal> setPks(IDataSet bufferedDataSet, int dbIndexLastPk)
	{
		return setPksAndQuery(bufferedDataSet, dbIndexLastPk, this.querySelect, false);
	}

	public synchronized SafeArrayList<IRecordInternal> setPksAndQuery(IDataSet bufferedDataSet, int dbIndexLastPk, QuerySelect querySelect)
	{
		return setPksAndQuery(bufferedDataSet, dbIndexLastPk, querySelect, false);
	}

	/**
	 * @param bufferedDataSet
	 * @param querySelect
	 */
	public synchronized SafeArrayList<IRecordInternal> setPksAndQuery(IDataSet bufferedDataSet, int dbIndexLastPk, QuerySelect querySelect, boolean reuse)
	{
		pks = bufferedDataSet == null || bufferedDataSet instanceof PKDataSet ? (PKDataSet)bufferedDataSet : new PKDataSet(bufferedDataSet);
		this.dbIndexLastPk = new AtomicInteger(dbIndexLastPk);
		this.querySelect = querySelect;
		if (reuse)
		{
			cachedRecords = reUseStatesBasedOnNewPrimaryKeys();
		}
		else
		{
			cachedRecords = new SafeArrayList<IRecordInternal>((pks != null ? pks.getRowCount() : 0) + 5);//(re)new
		}
		return cachedRecords;
	}

	public synchronized SafeArrayList<IRecordInternal> getCachedRecords()
	{
		return cachedRecords;
	}

	public synchronized PKDataSet getPks()
	{
		return pks;
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
						pks.setRow(dsIndex, pk);
						pks.setRow(i - 1, tmppk);
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
}
