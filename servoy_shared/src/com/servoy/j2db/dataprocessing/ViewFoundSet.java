/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.AbstractBaseQuery;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.querybuilder.impl.QBSelect;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewFoundSet", scriptingName = "ViewFoundSet")
public class ViewFoundSet implements IFoundSetInternal
{
	private final String datasource;
	private final IFoundSetManagerInternal manager;

	private final List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<IFoundSetEventListener>();

	private List<ViewRecord> records = new ArrayList<>();

	private final List<WeakReference<IRecordInternal>> allParents = new ArrayList<WeakReference<IRecordInternal>>(6);

	private QuerySelect select;

	private int foundsetID = 0;

	private boolean hasMore = false;
	private boolean refesh = true;
	private int currentChunkSize;
	private final int chunkSize;
	private boolean multiSelect;
	private int[] selection;

	public ViewFoundSet(String datasource, QuerySelect select, IFoundSetManagerInternal manager, int chunkSize)
	{
		this.datasource = datasource;
		this.select = select;
		this.manager = manager;
		this.chunkSize = chunkSize;
		this.currentChunkSize = chunkSize;
	}

	@Override
	public String[] getDataProviderNames(int type)
	{
		return null;
	}

	@Override
	public String getDataSource()
	{
		return datasource;
	}

	@Override
	public String getRelationName()
	{
		return null;
	}

	@Override
	public int getSize()
	{
		if (refesh)
		{
			try
			{
				loadAllRecords();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
		return records.size();
	}

	@Override
	public void addFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			if (!foundSetEventListeners.contains(l))
			{
				foundSetEventListeners.add(l);
			}
		}
	}

	@Override
	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			foundSetEventListeners.remove(l);
		}
	}

	@Override
	public Object forEach(IRecordCallback callback)
	{
		FoundSetIterator foundsetIterator = new FoundSetIterator();
		while (foundsetIterator.hasNext())
		{
			IRecord currentRecord = foundsetIterator.next();
			Object returnValue = callback.handleRecord(currentRecord, foundsetIterator.currentIndex, this);
			if (returnValue != null && returnValue != Undefined.instance)
			{
				return returnValue;
			}
		}
		return null;
	}

	/**
	 * Iterates over the records of a foundset taking into account inserts and deletes that may happen at the same time.
	 * It will dynamically load all records in the foundset (using Servoy lazy loading mechanism). If callback function returns a non null value the traversal will be stopped and that value is returned.
	 * If no value is returned all records of the foundset will be traversed. Foundset modifications( like sort, omit...) cannot be performed in the callback function.
	 * If foundset is modified an exception will be thrown. This exception will also happen if a refresh happens because of a rollback call for records on this datasource when iterating.
	 * When an exception is thrown from the callback function, the iteraion over the foundset will be stopped.
	 *
	 * @sample
	 *  foundset.forEach(function(record,recordIndex,foundset) {
	 *  	//handle the record here
	 *  });
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 *
	 * @return Object the return value of the callback
	 *
	 */
	public Object js_forEach(Function callback)
	{
		return forEach(new CallJavaScriptCallBack(callback, manager.getApplication().getScriptEngine(), null));
	}

	/**
	 * @clonedesc js_forEach(Function)
	 *
	 * @sampleas js_forEach(Function)
	 *
	 * @param callback The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.
	 * @param thisObject What the this object should be in the callback function (default it is the foundset)
	 *
	 * @return Object the return value of the callback
	 *
	 */
	public Object js_forEach(Function callback, Scriptable thisObject)
	{
		return forEach(new CallJavaScriptCallBack(callback, manager.getApplication().getScriptEngine(), thisObject));
	}


	@Override
	public int getRecordIndex(IRecord record)
	{
		return records.indexOf(record);
	}

	@Override
	public boolean isRecordEditable(int row)
	{
		return false;
	}

	@Override
	public boolean isInFindMode()
	{
		return false;
	}

	@Override
	public boolean find()
	{
		return false;
	}

	@Override
	public int search() throws Exception
	{
		return 0;
	}

	@Override
	@JSFunction
	public void loadAllRecords() throws ServoyException
	{
		String serverName = DataSourceUtils.getDataSourceServerName(select.getTable().getDataSource());
		String transaction_id = manager.getTransactionID(serverName);
		try
		{
			IDataSet ds = manager.getApplication().getDataServer().performQuery(manager.getApplication().getClientID(), serverName, transaction_id, select,
				manager.getTableFilterParams(serverName, select), select.isUnique(), 0, currentChunkSize, IDataServer.FOUNDSET_LOAD_QUERY);
			refesh = false;
			String[] columnNames = select.getColumnNames();
			int firstChange = -1;
			int currentSize = records.size();
			List<ViewRecord> old = records;
			records = new ArrayList<>(ds.getRowCount());

			for (int i = 0; i < ds.getRowCount(); i++)
			{
				Object[] rowData = ds.getRow(i);
				if (i < currentSize)
				{
					if (firstChange == -1)
					{
						ViewRecord current = old.get(i);
						if (Utils.equalObjects(rowData, current.getData()))
						{
							records.add(current);
							continue;
						}
						firstChange = i;
					}
				}
				records.add(new ViewRecord(columnNames, rowData, i, this));
			}
			hasMore = ds.hadMoreRows();
			if (firstChange != -1 && currentSize <= records.size())
			{
				fireFoundSetEvent(firstChange, currentSize - 1, FoundSetEvent.CHANGE_UPDATE);
			}
			fireDifference(currentSize, records.size());
		}
		catch (RemoteException e)
		{
			throw new RepositoryException(e);
		}
	}

	@Override
	public void clear()
	{
		int prevIndex = records.size();
		records.clear();
		refesh = false;
		hasMore = false;
		currentChunkSize = chunkSize;
		fireFoundSetEvent(0, prevIndex - 1, FoundSetEvent.CHANGE_DELETE);
	}

	@Override
	public void deleteRecord(int row) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public void deleteAllRecords() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int recordIndex, int indexToAdd) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int getSelectedIndex()
	{
		if (selection != null && selection.length > 0) return selection[0];
		return -1;
	}

	@Override
	public void setSelectedIndex(int selectedRow)
	{
		this.selection = new int[] { selectedRow };
	}

	@Override
	@JSGetter
	public void setMultiSelect(boolean multiSelect)
	{
		this.multiSelect = multiSelect;
	}

	@Override
	@JSGetter
	public boolean isMultiSelect()
	{
		return multiSelect;
	}

	@Override
	public void setSelectedIndexes(int[] indexes)
	{
		if (!multiSelect && indexes != null && indexes.length > 1) setSelectedIndex(indexes[0]);
		else this.selection = indexes;

	}

	@Override
	public int[] getSelectedIndexes()
	{
		return selection;
	}

	@JSFunction
	public ViewRecord getSelectedRecord()
	{
		int selectedIndex = getSelectedIndex();
		if (selectedIndex >= 0 && selectedIndex < records.size()) return records.get(selectedIndex);
		return null;
	}

	@Override
	public String getSort()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSort(String sortString) throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Get the query that the foundset is currently using (as a clone; modifying this QBSelect will not automatically change the foundset).
	 * When the founset is in find mode, the find conditions are included in the resulting query.
	 * So the query that would be used when just calling search() (or search(true,true)) is returned.
	 * Note that foundset filters are included and table filters are not included in the query.
	 *
	 * @sample
	 * var q = foundset.getQuery()
	 * q.where.add(q.columns.x.eq(100))
	 * foundset.loadRecords(q);
	 *
	 * @return query.
	 */
	@JSFunction
	public QBSelect getQuery()
	{
		QuerySelect query = AbstractBaseQuery.deepClone(this.select, true);
		IApplication application = manager.getApplication();
		return new QBSelect(manager, manager.getScopesScopeProvider(), application.getFlattenedSolution(), application.getScriptEngine().getSolutionScope(),
			select.getTable().getDataSource(), null, query);
	}

	@Override
	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		this.select = ((QBSelect)query).build(); // makes a clone
		loadAllRecords();
		return true;
	}

	@Override
	public void setRecordToStringDataProviderID(String dataProviderID)
	{
	}

	@Override
	public String getRecordToStringDataProviderID()
	{
		return null;
	}

	@Override
	public void sort(List<SortColumn> sortColumns) throws ServoyException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void makeEmpty()
	{
		clear();
	}

	@Override
	public void browseAll() throws ServoyException
	{
		loadAllRecords();
	}

	@Override
	public void deleteAll() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public boolean containsDataProvider(String dataProviderID)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getDataProviderValue(String dataProviderID)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<IRecord> iterator()
	{
		return new FoundSetIterator();
	}

	@Override
	public void completeFire(Map<IRecord, List<String>> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		List<String> dataproviders = null;
		for (IRecord record : entries.keySet())
		{
			int index = getRecordIndex(record);
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
			dataproviders = entries.get(record);
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, dataproviders);
		}
	}

	@Override
	public SQLSheet getSQLSheet()
	{
		return null;
	}

	@Override
	public PrototypeState getPrototypeState()
	{
		return new PrototypeState(this);
	}

	@Override
	public void addParent(IRecordInternal record)
	{
		if (record != null && record.getParentFoundSet() != this)
		{
			synchronized (allParents)
			{
				for (int i = allParents.size(); --i >= 0;)
				{
					WeakReference<IRecordInternal> wr = allParents.get(i);
					IRecordInternal rcd = wr.get();
					if (rcd == null)
					{
						allParents.remove(i);
					}
					else
					{
						if (rcd.equals(record))
						{
							return;
						}
					}
				}
				allParents.add(new WeakReference<IRecordInternal>(record));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#fireAggregateChangeWithEvents(com.servoy.j2db.dataprocessing.IRecordInternal)
	 */
	@Override
	public void fireAggregateChangeWithEvents(IRecordInternal record)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#isValidRelation(java.lang.String)
	 */
	@Override
	public boolean isValidRelation(String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getRelatedFoundSet(com.servoy.j2db.dataprocessing.IRecordInternal, java.lang.String,
	 * java.util.List)
	 */
	@Override
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal record, String relationName, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IFoundSetInternal#getCalculationValue(com.servoy.j2db.dataprocessing.IRecordInternal, java.lang.String,
	 * java.lang.Object[], com.servoy.j2db.scripting.UsedDataProviderTracker)
	 */
	@Override
	public Object getCalculationValue(IRecordInternal record, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void sort(Comparator<Object[]> recordPKComparator)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public List<SortColumn> getSortColumns()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Sorts the foundset based on the given record comparator function.
	 * Tries to preserve selection based on primary key. If first record is selected or cannot select old record it will select first record after sort.
	 * The comparator function is called to compare
	 * two records, that are passed as arguments, and
	 * it will return -1/0/1 if the first record is less/equal/greater
	 * then the second record.
	 *
	 * The function based sorting does not work with printing.
	 * It is just a temporary in-memory sort.
	 *
	 * NOTE: starting with 7.2 release this function doesn't save the data anymore
	 *
	 * @sample
	 * %%prefix%%foundset.sort(mySortFunction);
	 *
	 * function mySortFunction(r1, r2)
	 * {
	 *	var o = 0;
	 *	if(r1.id < r2.id)
	 *	{
	 *		o = -1;
	 *	}
	 *	else if(r1.id > r2.id)
	 *	{
	 *		o = 1;
	 *	}
	 *	return o;
	 * }
	 *
	 * @param recordComparisonFunction record comparator function
	 */
	@JSFunction
	@JSSignature(arguments = { Function.class })
	public void sort(Object recordComparisonFunction)
	{
		if (recordComparisonFunction instanceof Function)
		{
			final Function func = (Function)recordComparisonFunction;
			final IExecutingEnviroment scriptEngine = manager.getApplication().getScriptEngine();
			final Scriptable recordComparatorScope = func.getParentScope();
			sort(new Comparator<Object[]>()
			{
				public int compare(Object[] o1, Object[] o2)
				{
					try
					{
						Object compareResult = scriptEngine.executeFunction(func, recordComparatorScope, recordComparatorScope,
							new Object[] { getRecord(o1), getRecord(o2) }, false, true);
						double cmp = Utils.getAsDouble(compareResult, true);
						return cmp < 0 ? -1 : cmp > 0 ? 1 : 0;
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					return 0;
				}
			});
		}
	}

	@Override
	public IFoundSetManagerInternal getFoundSetManager()
	{
		return manager;
	}

	@Override
	public ITable getTable()
	{
		try
		{
			return manager.getTable(select.getTable().getDataSource());
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create a copy from the  current record of a View Foundset of datasource " + this.datasource);
	}

	@Override
	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		return new ViewFoundSet(datasource, AbstractBaseQuery.deepClone(this.select, true), manager, chunkSize);
	}

	public ViewRecord js_getRecord(int row)
	{
		return getRecord(row - 1);
	}

	@Override
	public ViewRecord getRecord(int row)
	{
		testForLoadMore(row);
		if (row < 0 || row >= records.size()) return null;
		return records.get(row);
	}

	private void testForLoadMore(int maxIndex)
	{
		boolean queryForMore = hasMore && (maxIndex == records.size() - 1);
		if (refesh || queryForMore)
		{
			try
			{
				if (queryForMore) currentChunkSize += chunkSize;
				loadAllRecords();
			}
			catch (ServoyException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public IRecordInternal[] getRecords(int startrow, int count)
	{
		int toIndex = startrow + count;
		testForLoadMore(toIndex);
		toIndex = records.size() < toIndex ? records.size() : toIndex;
		List<ViewRecord> subList = records.subList(startrow, toIndex);
		return subList.toArray(new IRecordInternal[subList.size()]);
	}

	@Override
	public void deleteAllInternal() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public void addAggregateModificationListener(IModificationListener listener)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAggregateModificationListener(IModificationListener listener)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hadMoreRows()
	{
		return hasMore;
	}

	@Override
	public void deleteRecord(IRecordInternal record) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records  from a View Foundset of datasource " + this.datasource);
	}

	@Override
	public IRecordInternal getRecord(Object[] pk)
	{
		for (IRecordInternal record : records)
		{
			if (Utils.equalObjects(record.getPK(), pk)) return record;
		}
		return null;
	}

	@Override
	public int getRecordIndex(String pkHash, int startHint)
	{
		int hintStart = Math.min(startHint + 5, getSize());

		int start = (hintStart < 0 || hintStart > records.size()) ? 0 : hintStart;

		for (int i = start; --i >= 0;)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		for (int i = start; i < records.size(); i++)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Gets the unique id of the foundset. The foundset does not have an id until this method is called the first time on it.
	 */
	@Override
	public int getID()
	{
		// we do not automatically assign an id to each foundset so that ng client client side code cannot send in random ints to target any foundset;
		// in this way, only foundsets that were sent to client already (getID() was called on them in various property types to send it to client) can be targeted
		if (foundsetID == 0) foundsetID = this.manager.getNextFoundSetID();
		return foundsetID;
	}


	/**
	 * Same as {@link #getID()} but it will not assign an id if it wasn't set before.
	 */
	int getIDInternal()
	{
		return foundsetID;
	}

	/**
	 * Fire difference based on real size (not corrected for fires!)
	 *
	 * @param oldSize
	 * @param newSize
	 */
	protected void fireDifference(int oldSize, int newSize)
	{
		if (newSize == 0 && oldSize == 0 || oldSize == newSize) return;

		//let the List know the model changed,the new states
		if (newSize < oldSize)
		{
			fireFoundSetEvent(newSize, oldSize - 1, FoundSetEvent.CHANGE_DELETE);
		}
		else
		{
			if (newSize > oldSize)
			{
				fireFoundSetEvent(oldSize, newSize - 1, FoundSetEvent.CHANGE_INSERT);
			}
		}
	}

	public void fireSelectionModeChange()
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.SELECTION_MODE_CHANGE, FoundSetEvent.CHANGE_UPDATE));
	}

	protected final void fireFoundSetEvent(int firstRow, int lastRow, int changeType)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow));
	}

	protected final void fireFoundSetEvent(int firstRow, int lastRow, int changeType, List<String> dataproviders)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow, dataproviders));
	}

	protected void fireFoundSetEvent(final FoundSetEvent e)
	{
		if (foundSetEventListeners.size() > 0)
		{
			Runnable run = new Runnable()
			{
				@Override
				public void run()
				{
					if (foundSetEventListeners.size() > 0)
					{
						final IFoundSetEventListener[] array;
						synchronized (foundSetEventListeners)
						{
							array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
						}

						for (IFoundSetEventListener element : array)
						{
							element.foundSetChanged(e);
						}
					}
				}
			};
			if (this.manager.getApplication().isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				this.manager.getApplication().invokeLater(run);
			}
		}
	}

	private class FoundSetIterator implements Iterator<IRecord>
	{
		private int currentIndex = -1;
		private Object[] currentPK = null;
		private final List<Object[]> processedPKS = new ArrayList<Object[]>();
		private IRecord currentRecord = null;

		public FoundSetIterator()
		{
		}

		@Override
		public boolean hasNext()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			return currentRecord != null;
		}

		@Override
		public IRecord next()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			IRecord returnRecord = currentRecord;
			currentRecord = null;
			return returnRecord;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private IRecord getNextRecord()
		{

			int nextIndex = currentIndex + 1;
			IRecord nextRecord = getRecord(nextIndex);
			while (nextRecord instanceof PrototypeState)
			{
				nextIndex = currentIndex + 1;
				nextRecord = getRecord(nextIndex);
			}
			if (currentIndex >= 0)
			{
				IRecord tmpCurrentRecord = getRecord(currentIndex);
				if (tmpCurrentRecord == null || !Utils.equalObjects(tmpCurrentRecord.getPK(), currentPK))
				{
					// something is changed in the foundset, recalculate
					if (tmpCurrentRecord == null)
					{
						int size = records.size();
						if (size == 0)
						{
							return null;
						}
						currentIndex = size - 1;
						tmpCurrentRecord = getRecord(currentIndex);
					}
					if (!listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
					{
						// substract current index
						while (tmpCurrentRecord != null && !listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex - 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex + 1;
						nextRecord = getRecord(nextIndex);
					}
					else
					{
						// increment current index
						while (tmpCurrentRecord != null && listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex + 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex;
						nextRecord = tmpCurrentRecord;
					}
					if (nextRecord == null)
					{
						return null;
					}
				}
			}
			if (nextRecord != null)
			{
				currentPK = nextRecord.getPK();
			}
			currentIndex = nextIndex;
			processedPKS.add(currentPK);
			return nextRecord;
		}

		private boolean listContainsArray(List<Object[]> list, Object[] value)
		{
			if (list != null)
			{
				for (Object[] array : list)
				{
					if (Utils.equalObjects(array, value))
					{
						return true;
					}
				}
			}
			return false;
		}

	}
}
