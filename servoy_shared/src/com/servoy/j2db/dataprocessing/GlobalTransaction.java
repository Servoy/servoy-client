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


import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.servoy.j2db.persistence.ITransactable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * Transaction object, keeping track of db transctions (ids)
 * @author jblok
 */
public class GlobalTransaction
{
	enum Crud
	{
		Created, Updated, Deleted
	}

	private HashMap<IRecordInternal, Crud> records; // all rows to prevent they are gc'ed
	private Map<String, String> serverTransactions; // serverName->transactionID
	private final IDataServer dataServer;
	private final String clientID;
	private final List<ITransactable> transactionEndListeners = new ArrayList<ITransactable>();

	GlobalTransaction(IDataServer ds, String cid)
	{
		serverTransactions = new HashMap<>();
		records = new HashMap<>();
		dataServer = ds;
		clientID = cid;
	}

	Collection<String> rollback(boolean queryForNewData, boolean revertSavedRecords)
	{
		if (serverTransactions != null)
		{
			String[] tids = new String[serverTransactions.size()];
			serverTransactions.values().toArray(tids);
			try
			{
				dataServer.endTransactions(clientID, tids, false);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		Collection<String> collection;
		if (queryForNewData && !records.isEmpty())
		{
			collection = new HashSet<>();
			try
			{
				for (Entry<IRecordInternal, Crud> entry : records.entrySet())
				{
					// TODO this loop can be optimized.
					// Search for all the rows with the same table and do a in query (per 200 rows..) instead of possibly querying them 1-by-1

					IRecordInternal record = entry.getKey();
					Row row = record.getRawData();
					collection.add(record.getDataSource());

					// it is possible that during the transaction the record's foundset did find/search operations or load by query, ....
					// so the record might no longer be in the foundset; we can only put this record back to editing mode with changes if required by "revertSavedRecords" == false argument
					// if the foundset still has a reference to the record; currently Servoy checks all over the place to not have orphaned records in editing mode...
					// if we'd want "revertSavedRecords" == false to work in this case as well we should move the "editing" concept to Rows not just Records which is a bigger change
					// so only use record if not orphaned
					if (!revertSavedRecords)
					{
						// TODO should we also check somehow that the foundset itself is still in use?
						IFoundSetInternal pfs = record.getParentFoundSet();
						if (pfs != null)
						{
							int idx = pfs.getRecordIndex(record);
							if (idx < 0) record = null;
							else record = pfs.getRecord(idx);
						}
						else record = null;
					}
					else record = null; // otherwise following impl. should not need record, just row

					// call startEdit with false so that start edit on a formcontroller (of the current visible one so only for that record)
					// if a deleted record was first updated then just overwrite with the database.

					switch (entry.getValue())
					{
						case Created :
							if (!revertSavedRecords && record != null &&
								row.getRowManager().getFoundsetManager().getEditRecordList().startEditing(record, false))
							{
								row.clearExistInDB();
							}
							break;

						case Updated :
							if (!revertSavedRecords && record != null &&
								row.getRowManager().getFoundsetManager().getEditRecordList().startEditing(record, false))
							{
								// if we shouldn't revert to database values,
								// quickly create the old values (edit values) array
								row.createOldValuesIfNeeded();
								// then revert by keeping the current column data values.
								row.rollbackFromDB(Row.ROLLBACK_MODE.KEEP_CHANGES);
							}
							else
							{
								row.rollbackFromDB(Row.ROLLBACK_MODE.OVERWRITE_CHANGES);
							}
							break;

						case Deleted :
							break;
					}
				}

				records = null;
			}
			catch (Exception e)
			{
				Debug.error(e); //TODO: add to application
			}
		}
		else
		{
			collection = emptyList();
		}

		fireTransactionEnded(false);

		return collection;
	}

	private void fireTransactionEnded(boolean committed)
	{
		ITransactable[] listeners = null;
		synchronized (transactionEndListeners)
		{
			if (transactionEndListeners.size() > 0)
			{
				listeners = transactionEndListeners.toArray(new ITransactable[transactionEndListeners.size()]);
				transactionEndListeners.clear();
			}
		}
		if (listeners != null)
		{
			for (ITransactable listener : listeners)
			{
				if (committed)
				{
					listener.processPostCommit();
				}
				else
				{
					listener.processPostRollBack();
				}
			}
		}
	}

	Collection<String> commit(boolean revertSavedRecords)
	{
		String[] tids = new String[serverTransactions.size()];
		serverTransactions.values().toArray(tids);
		try
		{
			dataServer.endTransactions(clientID, tids, true);
			fireTransactionEnded(true);
			records = null;
		}
		catch (Exception e)
		{
			Debug.error(e);//TODO: add to application
		}
		if (records != null)
		{
			serverTransactions = new HashMap<>(); // clear, so they are not processes on IDataService
			return rollback(true, revertSavedRecords); // all others
		}
		return null;
	}

	String addRecord(String serverName, IRecordInternal r) throws RepositoryException
	{
		if (r.isFlaggedForDeletion())
		{
			records.put(r, Crud.Deleted);
		}
		else if (!records.containsKey(r))
		{
			records.put(r, r.existInDataSource() ? Crud.Updated : Crud.Created);
		}
		return getTransactionID(serverName);
	}

	void addDeletedRecord(IRecordInternal r)
	{
		records.put(r, Crud.Deleted);
	}

	public synchronized String getTransactionID(String serverName) throws RepositoryException
	{
		// check if transaction exist for a server
		String tid = serverTransactions.get(serverName);
		if (tid == null)
		{
			// create
			tid = dataServer.startTransaction(clientID, serverName);
			// store
			serverTransactions.put(serverName, tid);
		}
		return tid;
	}

	public void addTransactionEndListener(ITransactable l)
	{
		synchronized (transactionEndListeners)
		{
			if (!transactionEndListeners.contains(l))
			{
				transactionEndListeners.add(l);
			}
		}
	}

	public void removeTransactionEndListener(ITransactable l)
	{
		synchronized (transactionEndListeners)
		{
			transactionEndListeners.remove(l);
		}
	}
}
