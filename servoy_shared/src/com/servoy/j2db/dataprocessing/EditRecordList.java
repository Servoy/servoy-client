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

import static com.servoy.j2db.dataprocessing.SQLGenerator.convertPKValuesForQueryCompare;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.query.AbstractBaseQuery.relinkTable;
import static com.servoy.j2db.query.BooleanCondition.FALSE_CONDITION;
import static com.servoy.j2db.util.Utils.equalObjects;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IPrepareForSave;
import com.servoy.j2db.dataprocessing.FoundSetManager.TableFilterRequest;
import com.servoy.j2db.dataprocessing.ValueFactory.BlobMarkerValue;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.Placeholder;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QueryInsert;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Keeps track of all the edited records and handles the save
 *
 * @author jcompagner
 */
public class EditRecordList
{
	protected static final Logger log = LoggerFactory.getLogger("com.servoy.j2db.dataprocessing.editedRecords"); //$NON-NLS-1$

	private static final String DELETED_RECORDS_FILTER = "_svy_deleted_records_table_filter";

	private final FoundSetManager fsm;

	private final List<IPrepareForSave> prepareForSaveListeners = new ArrayList<>(2);
	private final List<IGlobalEditListener> editListeners = new ArrayList<>();

	private final Map<ITable, Integer> accessMap = Collections.synchronizedMap(new HashMap<>());//per table (could be per column in future)
	private boolean autoSave = true;

	private ConcurrentMap<FoundSet, int[]> fsEventMap;

	// RAGTEST naar EditedRecords verhuizen? (niet als we failedrecords ook een EditedRecords() maken
	private final ReentrantLock editRecordsLock = new ReentrantLock();

	private final EditedRecords editedRecords = new EditedRecords();
	// RAGTEST ?? private final EditedRecords failedRecords = new EditedRecords();
	private final List<IRecordInternal> failedRecords = synchronizedList(new ArrayList<>(2));
	private final Map<IRecordInternal, List<IPrepareForSave>> recordTested = Collections.synchronizedMap(new HashMap<>()); //tested for form.OnRecordEditStop event
	private boolean preparingForSave;

	public EditRecordList(FoundSetManager fsm)
	{
		this.fsm = fsm;
	}

	public IRecordInternal[] getFailedRecords()
	{
		editRecordsLock.lock();
		try
		{
			return failedRecords.toArray(new IRecordInternal[failedRecords.size()]);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param record
	 * @return
	 */
	public boolean isEditing(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			return editedRecords.contains(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @return
	 */
	public boolean isEditing()
	{
		editRecordsLock.lock();
		try
		{
			return !editedRecords.isEmpty() || !failedRecords.isEmpty();
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param set
	 * @return
	 */
	public IRecordInternal[] getEditedRecords(IFoundSet set)
	{
		return getEditedRecords(set, false);
	}

	public IRecordInternal[] getEditedRecords(IFoundSet set, boolean removeUnchanged)
	{
		return getEditedRecords(recordParentFoundsetFilter(set), removeUnchanged);
	}

	public IRecordInternal[] getEditedRecords(String datasource, NativeObject filter, boolean removeUnchanged)
	{
		return getEditedRecords(record -> applyDatasourceAndObjectFilter(record, datasource, filter), removeUnchanged);
	}

	private static Predicate< ? super IRecordInternal> recordParentFoundsetFilter(IFoundSet set)
	{
		return set == null ? record -> true : record -> record.getParentFoundSet() == set;
	}

	private static boolean applyDatasourceAndObjectFilter(IRecordInternal record, String datasource, NativeObject filter)
	{
		if (!record.getDataSource().equals(datasource))
		{
			return false;
		}

		if (filter == null)
		{
			return true;
		}

		return filter.entrySet().stream()
			.filter(entry -> record.has(entry.getKey().toString()))
			.allMatch(entry -> {
				Object recordValue = record.getValue(entry.getKey().toString());
				Object filterValue = entry.getValue();
				Predicate<Object> equalsRecordValue = value -> equalObjects(value, recordValue);

				if (filterValue instanceof Object[])
				{
					return stream((Object[])filterValue).anyMatch(equalsRecordValue);
				}

				if (filterValue instanceof Iterable)
				{
					return StreamSupport.stream(((Iterable< ? >)filterValue).spliterator(), false).anyMatch(equalsRecordValue);
				}

				return equalsRecordValue.test(filterValue);
			});
	}

	private IRecordInternal[] getEditedRecords(Predicate< ? super IRecordInternal> recordFilter, boolean removeUnchanged)
	{
		if (removeUnchanged)
		{
			removeUnChangedRecords(true, false, recordFilter);
		}
		List<IRecordInternal> al = new ArrayList<>();
		editRecordsLock.lock();
		try
		{
			IRecordInternal[] edited = editedRecords.getEdited();
			for (int i = edited.length; --i >= 0;)
			{
				IRecordInternal record = edited[i];
				if (recordFilter.test(record))
				{
					al.add(record);
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}

	public IRecordInternal[] getFailedRecords(IFoundSet set)
	{
		List<IRecordInternal> al = new ArrayList<>();
		editRecordsLock.lock();
		try
		{
			for (int i = failedRecords.size(); --i >= 0;)
			{
				IRecordInternal record = failedRecords.get(i);
				if (record.getParentFoundSet() == set)
				{
					al.add(record);
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}

	public boolean hasEditedRecords(IFoundSet foundset)
	{
		return hasEditedRecords(foundset, true);
	}

	public boolean hasEditedRecords(IFoundSet foundset, boolean testForRemoves)
	{
		// TODO don't we have to check for any related foundset edits here as well?
		if (testForRemoves)
		{
			removeUnChangedRecords(false, false, recordParentFoundsetFilter(foundset));
		}
		editRecordsLock.lock();
		try
		{
			return editedRecords.contains(record -> record.getParentFoundSet() == foundset);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}


	private boolean isSavingAll = false;
	private List<IRecord> savingRecords = new ArrayList<IRecord>();
	private Exception lastStopEditingException;

	private boolean ignoreSave;

	public int stopIfEditing(IFoundSet fs)
	{
		if (hasEditedRecords(fs))
		{
			return stopEditing(false);
		}
		return ISaveConstants.STOPPED;
	}

	public int stopEditing(boolean javascriptStop)
	{
		return stopEditing(javascriptStop, null, (List<IRecord>)null);
	}

	/**
	 * stop/save
	 *
	 * @param javascriptStop
	 * @param recordToSave null means all records
	 * @return IRowChangeListener static final
	 */
	public int stopEditing(boolean javascriptStop, IRecord recordToSave)
	{
		return stopEditing(javascriptStop, null, asList(recordToSave));
	}

	public int stopEditing(boolean javascriptStop, IFoundSet foundset)
	{
		for (IRecordInternal record : getFailedRecords(foundset))
		{
			startEditing(record, false);
		}
		return stopEditing(javascriptStop, foundset, asList(getEditedRecords(foundset)));
	}

	/**
	 * stop/save
	 *
	 * @param javascriptStop
	 * @param recordsToSave null means all records
	 * @return IRowChangeListener static final
	 */
	@SuppressWarnings("nls")
	public int stopEditing(boolean javascriptStop, IFoundSet foundset, List<IRecord> recordsToSave)
	{
		int stopped = stopEditingImpl(javascriptStop, foundset, recordsToSave, 0);
		if ((stopped == ISaveConstants.VALIDATION_FAILED || stopped == ISaveConstants.SAVE_FAILED) && !javascriptStop)
		{
			IApplication application = fsm.getApplication();
			Solution solution = application.getSolution();
			int mid = solution.getOnAutoSaveFailedMethodID();
			if (mid > 0)
			{
				ScriptMethod sm = application.getFlattenedSolution().getScriptMethod(mid);
				if (sm != null)
				{
					// the validation failed in a none javascript stop (so this was an autosave failure)
					List<JSRecordMarkers> failedMarkers = failedRecords.stream().map(record -> record.getRecordMarkers()).collect(toList());
					try
					{
						application.getScriptEngine().getScopesScope()
							.executeGlobalFunction(sm.getScopeName(), sm.getName(),
								Utils.arrayMerge((new Object[] { failedMarkers.toArray() }),
									Utils.parseJSExpressions(
										solution.getFlattenedMethodArguments(IContentSpecConstants.PROPERTY_ONAUTOSAVEDFAILEDMETHODID))),
								false, false);
					}
					catch (Exception e)
					{
						application.reportJSError("Failed to run the solutions auto save failed method", e);
					}
				}
				else
				{
					application
						.reportJSWarning("Solution " + application.getSolutionName() + " onautosavefailed method not found for id " + mid);
				}
			}

		}
		return stopped;
	}

	/**
	 * This method should only be called through stopEditing(boolean,List<Record>) so that that can call onAutoSaveFailed.
	 */
	private int stopEditingImpl(final boolean javascriptStop, IFoundSet foundset, List<IRecord> recordsToSave, int recursionDepth)
	{
		// RAGTEST voer delete uit
		// RAGTEST check voor foundset event PROPERTY_ONDELETEMETHODID PROPERTY_ONAFTERDELETEMETHODID
		// RAGTEST get en verwijder delete queries voor foundsets (foundset = null, dan alle)
		if (recursionDepth > 50)
		{
			fsm.getApplication().reportJSError(
				"stopEditing max recursion exceeded, look if on (or after) record update, inserts or deletes are constantly changing records",
				new RuntimeException());
			return ISaveConstants.SAVE_FAILED;
		}
		if (ignoreSave)
		{
			return ISaveConstants.AUTO_SAVE_BLOCKED;
		}

		if (isSavingAll)
		{
			// we are saving all, no need to save anything more
			return ISaveConstants.STOPPED;
		}

		if (recordsToSave == null && savingRecords.size() > 0)
		{
			// we are saving some records, cannot call save all now, not supported
			return ISaveConstants.STOPPED;
		}

		if (recordsToSave != null && savingRecords.size() > 0)
		{
			// make a copy to be sure that removeAll is supported
			recordsToSave = new ArrayList<>(recordsToSave);
			recordsToSave.removeAll(savingRecords);
		}

		if (recordsToSave != null)
		{
			boolean hasEditedRecords = false;
			editRecordsLock.lock();
			try
			{
				for (IRecord record : recordsToSave)
				{
					if (editedRecords.contains(record))
					{
						hasEditedRecords = true;
						break;
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (!hasEditedRecords) return ISaveConstants.STOPPED;
		}

		// here we can't have a test if editedRecords is empty (and return stop)
		// because for just globals or findstates (or deleted records)
		// we need to pass prepareForSave.
		final List<IRecord> recordsToSaveFinal = recordsToSave;
		if (!fsm.getApplication().isEventDispatchThread())
		{
			// only the event dispatch thread can stop an current edit.
			// this is a fix for innerworkings because background aggregate queries seems to also trigger saves
			Debug.trace("Stop edit postponend because it is not in the event dispatch thread: " + Thread.currentThread().getName()); //$NON-NLS-1$
			// calculations running from lazy table view loading threads may trigger stopEditing
			fsm.getApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					// do not stop if the user is editing something else.
					boolean stop = false;
					editRecordsLock.lock();
					try
					{
						if (recordsToSaveFinal != null && recordsToSaveFinal.size() == 1)
						{
							IRecordInternal[] edited = editedRecords.getEdited();
							stop = edited.length == 1 && edited[0] == recordsToSaveFinal.get(0);
						}
					}
					finally
					{
						editRecordsLock.unlock();
					}
					if (stop)
					{
						stopEditing(javascriptStop, foundset, recordsToSaveFinal);
					}
					else
					{
						Debug.trace("Stop edit skipped because other records are being edited"); //$NON-NLS-1$
					}
				}

			});
			return ISaveConstants.AUTO_SAVE_BLOCKED;
		}

		int editedRecordsSize;
		try
		{
			int p = prepareForSave(true);
			if (p != ISaveConstants.STOPPED)
			{
				return p;
			}
			if (recordsToSave == null)
			{
				isSavingAll = true;
			}
			else
			{
				savingRecords.addAll(recordsToSave);
			}

			//remove any non referenced failed records
			boolean fireChange = false;
			editRecordsLock.lock();
			try
			{
				if (failedRecords.size() != 0)
				{
					Iterator<IRecordInternal> it = failedRecords.iterator();
					while (it.hasNext())
					{
						IRecordInternal rec = it.next();
						if (rec != null)
						{
							if (rec.getParentFoundSet() == null)
							{
								it.remove();
							}
							else if (rec.getParentFoundSet().getRecordIndex(rec) == -1)
							{
								it.remove();
							}
						}
					}
					if (failedRecords.size() == 0)
					{
						fireChange = true;
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (fireChange) fireEditChange();

			// remove the unchanged, really calculate when it is a real stop (autosave = true or it is a javascript stop)
			removeUnChangedRecords(autoSave || javascriptStop, true);

			//check if anything left
			int editRecordListSize;
			editRecordsLock.lock();
			try
			{
				if (editedRecords.isEmpty()) return ISaveConstants.STOPPED;
				editRecordListSize = editedRecords.size();
			}
			finally
			{
				editRecordsLock.unlock();
			}

			//cannot stop, its blocked
			if (!autoSave && !javascriptStop)
			{
				return ISaveConstants.AUTO_SAVE_BLOCKED;
			}

			int failedCount = 0;
			List<Pair<IFoundSetInternal, QueryDelete>> failedDeletes = new ArrayList<>();

			boolean justValidationErrors = false;
			lastStopEditingException = null;
			List<RowUpdateInfo> rowUpdates = new ArrayList<>(editRecordListSize);
			editRecordsLock.lock();
			try
			{
				if (recordsToSave == null)
				{
					// if it is a save all, then first filter out all the duplicate rows.
//	RAGTEST				for (int i = 0; i < editedRecords.size(); i++)
//					{
//						Row toTest = editedRecords.get(i).getRawData();
//						for (int j = editedRecords.size(); --j > i;)
//						{
//							if (editedRecords.get(j).getRawData() == toTest)
//							{
//								removeEditedRecord(editedRecords.get(j));
//							}
//						}
//					}
				}

				Map<IRecordInternal, Integer> processed = new HashMap<>();
				for (IRecordInternal tmp = getFirstElement(editedRecords.getAll(), recordsToSave); //
					tmp != null; //
					tmp = getFirstElement(editedRecords.getAll(), recordsToSave))
				{
					// check if we do not have an infinite recursive loop
					Integer count = processed.get(tmp);
					if (count != null && count.intValue() > 50)
					{
						fsm.getApplication().reportJSError(
							"stopEditing max loop counter exceeded on " + tmp.getDataSource() + "/" + tmp.getPKHashKey(),
							new RuntimeException());
						return ISaveConstants.SAVE_FAILED;
					}
					processed.put(tmp, Integer.valueOf(count == null ? 1 : (count.intValue() + 1)));

					if (tmp instanceof Record)
					{
						Record record = (Record)tmp;

						// prevent multiple update for the same row (from multiple records)
						for (int j = 0; j < rowUpdates.size(); j++)
						{
							if (rowUpdates.get(j).getRow() == record.getRawData())
							{
								// create a new rowUpdate that contains both updates
								RowUpdateInfo removed = rowUpdates.remove(j);
								recordTested.remove(record);
								// do use the first record, that one must always be leading. (for fire of events)
								record = removed.getRecord();
								break;
							}
						}

						try
						{
							// test for table events; this may execute table events if the user attached JS methods to them;
							// the user might add/delete/edit records in the JS - thus invalidating a normal iterator (it)
							// - edited record list changes; this is why an AllowListModificationIterator is used

							// Note that the behaviour is different when trigger returns false or when it throws an exception.
							// when the trigger returns false, record must stay in editedRecords.
							//   this is needed because the trigger may be used as validation to keep the user in the record when autosave=true.
							// when the trigger throws an exception, the record must move from editedRecords to failedRecords so that in
							//    scripting the failed records can be examined (the thrown value is retrieved via record.exception.getValue())
							editRecordsLock.unlock();
							try
							{
								boolean validationErrors = false;
								JSRecordMarkers validateObject = fsm.validateRecord(record, null);
								if (validateObject != null && validateObject.isHasErrors()) // throws ServoyException when trigger method throws exception
								{
									Object[] genericExceptions = validateObject.getGenericExceptions();
									if (genericExceptions.length > 0)
									{
										// compatible with old code, then those exceptions are caught below.
										throw (Exception)genericExceptions[0];
									}
									// we always want to process all records, but mark this as a validation error so below the failed records are updated.
									validationErrors = true;
									// update the just failed boolean to true, if that is true and there is not really an exception then handleException of application is not called.
									justValidationErrors = true;
									failedCount++;
									if (!failedRecords.contains(record))
									{
										failedRecords.add(record);
									}
									recordTested.remove(record);
								}
								if (!validationErrors)
								{
									RowUpdateInfo rowUpdateInfo = getRecordUpdateInfo(record);
									if (rowUpdateInfo != null)
									{
										rowUpdateInfo.setRecord(record);
										rowUpdates.add(rowUpdateInfo);
									}
									else
									{
										recordTested.remove(record);
									}
								}
							}
							finally
							{
								editRecordsLock.lock();
							}
						}
						catch (ServoyException e)
						{
							log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", //$NON-NLS-1$//$NON-NLS-2$
								e);
							// trigger method threw exception
							lastStopEditingException = e;
							failedCount++;
							record.getRawData().setLastException(e); //set latest
							if (!failedRecords.contains(record))
							{
								failedRecords.add(record);
							}
							recordTested.remove(record);
						}
						catch (Exception e)
						{
							Debug.error("Not a normal Servoy/Db Exception generated in saving record: " + record + " removing the record", e); //$NON-NLS-1$ //$NON-NLS-2$
							recordTested.remove(record);
						}
					}
					else
					{
						// find state
						recordTested.remove(tmp);
					}
					editedRecords.remove(tmp);
				}


				// Perform Foundset deletes

				// RAGTEST kunnen deletes andere deletes triggeren? dan recursief of loopen?


				failedCount = executeDeleteQueries(javascriptStop, foundset, failedCount, failedDeletes);
			}
			finally
			{
				editRecordsLock.unlock();
			}


			if (failedCount > 0)
			{
				failedDeletes.forEach(pair -> editedRecords.addDeleteQuery(pair.getLeft(), pair.getRight()));

				placeBackAlreadyProcessedRecords(rowUpdates);

				setDeletedrecordsInternalTableFilter(false);

				if (lastStopEditingException == null && justValidationErrors)
				{
					return ISaveConstants.VALIDATION_FAILED;
				}

				if (!(lastStopEditingException instanceof ServoyException))
				{
					lastStopEditingException = new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException);
				}
				if (!javascriptStop)
				{
					fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
						lastStopEditingException);
				}
				return ISaveConstants.SAVE_FAILED;
			}

			if (rowUpdates.isEmpty())
			{
				fireEditChange();
				if (Debug.tracing())
				{
					Debug.trace("no records to update anymore, failed: " + failedRecords.size()); //$NON-NLS-1$
				}
				return ISaveConstants.STOPPED;
			}

			if (Debug.tracing())
			{
				Debug.trace("Updating/Inserting/Deleting " + rowUpdates.size() + " records: " + rowUpdates.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			List<RowUpdateInfo> infos = orderUpdatesForInsertOrder(rowUpdates, RowUpdateInfo::getRow, false);

			ISQLStatement[] statements;
			if (fsm.config.statementBatching() && infos.size() > 1)
			{
				// Merge insert statements insert statements from all info's: multiple info's can share the same statement of the records are batched together on the statement level
				List<ISQLStatement> mergedStatements = new ArrayList<>(infos.size());

				ISQLStatement prevStatement = null;
				for (RowUpdateInfo rowUpdateInfo : infos)
				{
					ISQLStatement statement = rowUpdateInfo.getISQLStatement();

					if (statement.getAction() == ISQLActionTypes.INSERT_ACTION &&
						prevStatement != null && prevStatement.getAction() == ISQLActionTypes.INSERT_ACTION &&
						insertStatementsCanBeMerged(prevStatement, statement))
					{
						mergeInsertStatements(prevStatement, statement);
					}
					else
					{
						prevStatement = statement;
						mergedStatements.add(statement);
					}
				}

				statements = mergedStatements.toArray(new ISQLStatement[mergedStatements.size()]);
			}
			else
			{
				statements = infos.stream().map(RowUpdateInfo::getISQLStatement).toArray(ISQLStatement[]::new);
			}

			// TODO if one statement fails in a transaction how do we know which one? and should we rollback all rows in these statements?
			Object[] idents = null;
			try
			{
				idents = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), statements);
			}
			catch (Exception e)
			{
				log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", e); //$NON-NLS-1$//$NON-NLS-2$
				lastStopEditingException = e;
				if (!javascriptStop) fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
					new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException));
				return ISaveConstants.SAVE_FAILED;
			}

			if (idents.length != infos.size())
			{
				Debug.error("Should be of same size!!"); //$NON-NLS-1$
			}

			List<RowUpdateInfo> infosToBePostProcessed = new ArrayList<>();

			Map<FoundSet, List<Record>> foundsetToRecords = new HashMap<>();
			Map<FoundSet, List<String>> foundsetToAggregateDeletes = new HashMap<>();
			List<Runnable> fires = new ArrayList<>(infos.size());
			int mustRollbackCount = 0;
			// Walk in reverse over it, so that related rows are update in there row manger before they are required by there parents.
			for (int i = infos.size(); --i >= 0;)
			{
				RowUpdateInfo rowUpdateInfo = infos.get(i);
				FoundSet foundSet = rowUpdateInfo.getFoundSet();
				Row row = rowUpdateInfo.getRow();

				String oldKey = row.getPKHashKey();
				Record record = rowUpdateInfo.getRecord();
				if (idents != null && i < idents.length && idents[i] != null)
				{
					Object retValue = idents[i];
					if (retValue instanceof Exception)
					{
						log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", //$NON-NLS-1$//$NON-NLS-2$
							(Exception)retValue);
						lastStopEditingException = (Exception)retValue;
						failedCount++;
						if (retValue instanceof ServoyException)
						{
							((ServoyException)retValue).fillScriptStack();

							if (((ServoyException)retValue).getErrorCode() == ServoyException.MUST_ROLLBACK)
							{
								mustRollbackCount++;
							}
						}
						row.setLastException((Exception)retValue);
						markRecordAsFailed(record);
						JSRecordMarkers vo = record.getRecordMarkers() != null ? record.getRecordMarkers()
							: new JSRecordMarkers(record, fsm.getApplication());
						vo.addGenericException((Exception)retValue);
						record.setRecordMarkers(vo);
						continue;
					}
					else if (retValue instanceof Object[])
					{
						Object[] rowData = (Object[])retValue;
						Object[] oldRowData = row.getRawColumnData();
						if (oldRowData != null)
						{
							if (oldRowData.length == rowData.length)
							{
								for (int j = 0; j < rowData.length; j++)
								{
									if (rowData[j] instanceof BlobMarkerValue)
									{
										rowData[j] = oldRowData[j];
									}
									if (oldRowData[j] instanceof DbIdentValue)
									{
										row.setDbIdentValue(rowData[j]);
									}
								}
							}
							else
							{
								Debug.error("Requery data has different length from row data.");
							}
						}
						row.setRollbackData(rowData, Row.ROLLBACK_MODE.UPDATE_CHANGES);
					}
					else if (!Boolean.TRUE.equals(retValue))
					{
						// is db ident, can only be one column
						row.setDbIdentValue(retValue);
					}
				}
				editRecordsLock.lock();
				try
				{
					recordTested.remove(record);
				}
				finally
				{
					editRecordsLock.unlock();
				}

				if (!row.existInDB())
				{
					// when row was not saved yet row pkhash will be with new value, pksAndRecordsHolder will have initial value
					foundSet.updatePk(record);
				}
				try
				{
					// RAGTEST bij delete?
					ISQLStatement statement = rowUpdateInfo.getISQLStatement();
					row.getRowManager().rowUpdated(row, oldKey, foundSet, fires,
						statement instanceof ITrackingSQLStatement ? ((ITrackingSQLStatement)statement).getChangedColumns() : null);
				}
				catch (Exception e)
				{
					log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", e); //$NON-NLS-1$//$NON-NLS-2$
					lastStopEditingException = e;
					failedCount++;
					row.setLastException(e);
					JSRecordMarkers vo = record.getRecordMarkers() != null ? record.getRecordMarkers()
						: new JSRecordMarkers(record, fsm.getApplication());
					vo.addGenericException(e);
					record.setRecordMarkers(vo);
					editRecordsLock.lock();
					try
					{
						if (!failedRecords.contains(record))
						{
							failedRecords.add(record);
						}
					}
					finally
					{
						editRecordsLock.unlock();
					}
				}

				infosToBePostProcessed.add(infos.get(i));

				List<Record> lst = foundsetToRecords.get(foundSet);
				if (lst == null)
				{
					lst = new ArrayList<>(3);
					foundsetToRecords.put(foundSet, lst);
				}
				lst.add(record);

				List<String> aggregates = foundsetToAggregateDeletes.get(foundSet);
				if (aggregates == null)
				{
					foundsetToAggregateDeletes.put(foundSet, rowUpdateInfo.getAggregatesToRemove());
				}
				else
				{
					rowUpdateInfo.getAggregatesToRemove().stream()
						.filter(aggregate -> !aggregates.contains(aggregate))
						.forEach(aggregates::add);
				}
			}

			if (mustRollbackCount > 0)
			{
				log.error("Not all edit records were processed: " + mustRollbackCount +
					" records failed because the transaction must be rolled back after a failure");
			}

			// run rowmanager fires in reverse order (original order because info's were processed in reverse order) -> first inserted record is fired first
			for (int i = fires.size(); --i >= 0;)
			{
				fires.get(i).run();
			}

			// get the size of the edited records before the table events, so that we can look if those events did change records again.
			editedRecordsSize = editedRecords.size();
			for (RowUpdateInfo rowUpdateInfo : infosToBePostProcessed)
			{
				Record rowUpdateInfoRecord = null;
				try
				{
					rowUpdateInfoRecord = rowUpdateInfo.getRecord();
					TypedProperty<Integer> property;
					switch (rowUpdateInfo.getISQLStatement().getAction())
					{
						case ISQLActionTypes.INSERT_ACTION :
							property = StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID;
							break;
						case ISQLActionTypes.DELETE_ACTION :
							property = StaticContentSpecLoader.PROPERTY_ONAFTERDELETEMETHODID;
							break;
						default :
							property = StaticContentSpecLoader.PROPERTY_ONAFTERINSERTMETHODID;
					}
					((FoundSet)rowUpdateInfoRecord.getParentFoundSet()).executeFoundsetTrigger(new Object[] { rowUpdateInfoRecord }, property, true);
				}
				catch (ServoyException e)
				{
					if (e instanceof DataException && e.getCause() instanceof JavaScriptException)
					{
						// trigger method threw exception
						log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", e); //$NON-NLS-1$//$NON-NLS-2$
						lastStopEditingException = e;
						failedCount++;
						rowUpdateInfoRecord.getRawData().setLastException(e);
						JSRecordMarkers vo = rowUpdateInfoRecord.getRecordMarkers() != null ? rowUpdateInfoRecord.getRecordMarkers()
							: new JSRecordMarkers(rowUpdateInfoRecord, fsm.getApplication());
						vo.addGenericException(e);
						rowUpdateInfoRecord.setRecordMarkers(vo);
						editRecordsLock.lock();
						try
						{
							if (!failedRecords.contains(rowUpdateInfoRecord))
							{
								failedRecords.add(rowUpdateInfoRecord);
							}
						}
						finally
						{
							editRecordsLock.unlock();
						}
					}
					else
					{
						fsm.getApplication().handleException("Failed to execute after update/insert/delete trigger.", e); //$NON-NLS-1$
					}
				}
			}

			for (Map.Entry<FoundSet, List<Record>> entry : foundsetToRecords.entrySet())
			{
				FoundSet fs = entry.getKey();
				fs.recordsUpdated(entry.getValue(), foundsetToAggregateDeletes.get(fs));
			}
			boolean shouldFireEditChange;
			editRecordsLock.lock();
			try
			{
				shouldFireEditChange = editedRecords.size() == 0;
			}
			finally
			{
				editRecordsLock.unlock();
			}
			if (shouldFireEditChange)
			{
				fireEditChange();
			}

			if (failedCount > 0)
			{
				if (!javascriptStop)
				{
					lastStopEditingException = new ApplicationException(ServoyException.SAVE_FAILED, lastStopEditingException);
					fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), //$NON-NLS-1$
						lastStopEditingException);
				}
				return ISaveConstants.SAVE_FAILED;
			}
		}
		catch (RuntimeException e)
		{
			if (e instanceof IllegalArgumentException)
			{
				fsm.getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, e));
				return ISaveConstants.SAVE_FAILED;
			}
			else if (e instanceof IllegalStateException)
			{
				fsm.getApplication().handleException(fsm.getApplication().getI18NMessage("servoy.formPanel.error.saveFormData"), e); //$NON-NLS-1$
				return ISaveConstants.SAVE_FAILED;
			}
			else
			{
				Debug.error(e);
				throw e;
			}
		}
		finally
		{
			if (recordsToSave == null)
			{
				isSavingAll = false;
			}
			else
			{
				savingRecords.removeAll(recordsToSave);
			}
			fireEvents();
		}

		if (editedRecords.size() != editedRecordsSize && recordsToSave == null && foundset == null)
		{
			// records where changed by the after insert/update/delete table events, call stop edit again if this was not a specific record save.
			return stopEditingImpl(javascriptStop, null, null, recursionDepth + 1);
		}

		return ISaveConstants.STOPPED;
	}

	private int executeDeleteQueries(boolean javascriptStop, IFoundSet foundset, int failedCount, List<Pair<IFoundSetInternal, QueryDelete>> failedDeletes)
	{
		for (Pair<IFoundSetInternal, QueryDelete> tmp = editedRecords.getAndRemoveFirstDeleteQuery(foundset); //
			tmp != null; //
			tmp = editedRecords.getAndRemoveFirstDeleteQuery(foundset))
		{
			try
			{
				setDeletedrecordsInternalTableFilter(false);
				performDeleteForFoundset(tmp.getLeft().getTable(), tmp.getRight());
			}
			catch (ServoyException e)
			{
				log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", //$NON-NLS-1$//$NON-NLS-2$
					e);
				// trigger method threw exception
				lastStopEditingException = e;
				failedDeletes.add(tmp);
				failedCount++;
			}
			catch (Exception e)
			{
				//		Debug.error("Not a normal Servoy/Db Exception generated in saving record: " + record + " removing the record", e); //$NON-NLS-1$ //$NON-NLS-2$
				failedDeletes.add(tmp);
				failedCount++;
			}
		}
		return failedCount;
	}

	private void performDeleteForFoundset(ITable table, QueryDelete deleteQuery) throws ServoyException, RemoteException
	{
		String tid = fsm.getTransactionID(table.getServerName());

		// RAGTEST ignore delete TF of verwijder ze eerst (hoe te handelen met errors)


		SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, table.getServerName(), table.getName(), null,
			tid, deleteQuery, fsm.getTableFilterParams(table.getServerName(), deleteQuery));

		Object[] results = fsm.getDataServer().performUpdates(fsm.getApplication().getClientID(), new ISQLStatement[] { statement });

		Optional<ServoyException> exceptionOpt = stream(results)
			.filter(ServoyException.class::isInstance).map(ServoyException.class::cast)
			.findAny();
		if (exceptionOpt.isPresent())
		{
			throw exceptionOpt.get();
		}
	}

	private <T> List<T> orderUpdatesForInsertOrder(List<T> rowData, Function<T, Row> rowFuction, boolean reverse)
	{
		if (rowData.size() <= 1 || fsm.config.disableInsertsReorder())
		{
			return rowData;
		}

		// search if there are new row pks used that are
		// used in records before this record and sort it based on that.
		List<T> al = new ArrayList<>(rowData);
		int prevI = -1;
		outer : for (int i = al.size(); --i > 0;)
		{
			Row row = rowFuction.apply(al.get(i));
			// only test for new rows and its pks.
			if (row.existInDB()) continue;
			String[] pkColumns = row.getRowManager().getSQLSheet().getPKColumnDataProvidersAsArray();
			Object[] pk = row.getPK();
			for (int j = 0; j < pk.length; j++)
			{
				Object pkObject = pk[j];
				// special case if pk was db ident and that value was copied from another row.
				if (pkObject instanceof DbIdentValue && ((DbIdentValue)pkObject).getRow() != row) continue;
				for (int k = 0; k < i; k++)
				{
					Row otherRow = rowFuction.apply(al.get(k));
					Object[] values = otherRow.getRawColumnData();
					int[] pkIndexes = otherRow.getRowManager().getSQLSheet().getPKIndexes();
					IntHashMap<String> pks = new IntHashMap<String>(pkIndexes.length, 1);
					for (int pkIndex : pkIndexes)
					{
						pks.put(pkIndex, ""); //$NON-NLS-1$
					}
					for (int l = 0; l < values.length; l++)
					{
						// skip all pk column indexes (except from dbidents from other rows, this may need resort). Those shouldn't be resorted
						if (!(values[l] instanceof DbIdentValue && ((DbIdentValue)values[l]).getRow() != otherRow) && pks.containsKey(l))
							continue;

						boolean same = values[l] == pkObject;
						if (!same && values[l] != null)
						{
							Column pkColumn = row.getRowManager().getSQLSheet().getTable().getColumn(pkColumns[j]);
							if (pkColumn.hasFlag(IBaseColumn.UUID_COLUMN))
							{
								// same uuids are the same even if not the same object
								same = equalObjects(pkObject, values[l], 0, true);
							}
						}
						if (same)
						{
							al.add(k, al.remove(i));
							// watch out for endless loops when 2 records both with pk's point to each other...
							if (prevI != i)
							{
								prevI = i;
								i++;
							}
							continue outer;
						}
					}
				}
			}

			if (reverse)
			{
				reverse(al);
			}
		}

		return al;
	}

	/**
	 * Check if these 2 insert statements can be merged into one batched on.
	 */
	private boolean insertStatementsCanBeMerged(ISQLStatement statement1, ISQLStatement statement2)
	{
		// table
		if (!statement1.getServerName().equals(statement2.getServerName()) || !statement1.getTableName().equals(statement2.getTableName()))
		{
			return false;
		}

		// skip oracle server
		try
		{
			IServer server = fsm.getApplication().getSolution().getServer(statement1.getServerName());
			if (SQLSheet.isOracleServer(server))
			{
				return false;
			}
		}
		catch (RepositoryException e)
		{
			return false;
		}

		if (statement1.isOracleFixTrackingData() || statement2.isOracleFixTrackingData())
		{
			return false;
		}

		// insert with PKs, these will be combined
		if (statement1.getPKs() == null || statement2.getPKs() == null || statement1.getPKs().getColumnCount() != statement2.getPKs().getColumnCount())
		{
			return false;
		}

		// filters
		if (!statement1.getClass().equals(statement2.getClass()))
		{
			return false;
		}
		if (statement1 instanceof SQLStatement && !Objects.equals(((SQLStatement)statement1).getFilters(), ((SQLStatement)statement2).getFilters()))
		{
			return false;
		}

		// Don't batch if statement requires requery, only because SQLStatements & serverside logic cannot handle it (yet)
		// Neither do batching if tracking is enabled, because setTrackingData isn't yet implemented properly for batching

		// requerySelect
		if (statement1.getRequerySelect() != null || statement2.getRequerySelect() != null)
		{
			return false;
		}

		// tracking
		if (statement1 instanceof SQLStatement && ((SQLStatement)statement1).isTracking())
		{
			return false;
		}
		if (statement2 instanceof SQLStatement && ((SQLStatement)statement2).isTracking())
		{
			return false;
		}

		// transaction
		if (!Objects.equals(statement1.getTransactionID(), statement2.getTransactionID()))
		{
			return false;
		}

		// identitycolumn, supported when both statements are on the same column
		if (!Objects.equals(statement1.getIdentityColumn(), statement2.getIdentityColumn()))
		{
			return false;
		}

		// datatype
		if (statement1.getDataType() != statement2.getDataType())
		{
			return false;
		}

		return true;
	}

	/**
	 * Merge the src statement into the target statement.
	 */
	private void mergeInsertStatements(ISQLStatement targetStatement, ISQLStatement srcStatement)
	{
		QueryInsert sqlUpdateTarget = (QueryInsert)targetStatement.getUpdate();
		Placeholder placeholderTarget = (Placeholder)sqlUpdateTarget.getValues();
		Object[][] valTarget = (Object[][])placeholderTarget.getValue();

		QueryInsert sqlUpdateSrc = (QueryInsert)srcStatement.getUpdate();
		Placeholder placeholderSrc = (Placeholder)sqlUpdateSrc.getValues();
		Object[][] valSrc = (Object[][])placeholderSrc.getValue();

		// Copy insert values into the target insert placeholder
		for (int i = 0; i < valTarget.length; i++)
		{
			valTarget[i] = Utils.arrayJoin(valSrc[i], valTarget[i]);
		}

		// Copy the pks into the target pks
		IDataSet targetpKs = targetStatement.getPKs();
		IDataSet srcpKs = srcStatement.getPKs();
		for (int row = 0; row < srcpKs.getRowCount(); row++)
		{
			targetpKs.addRow(srcpKs.getRow(row));
		}
	}

	/**
	 * @param rowUpdates
	 */
	private void placeBackAlreadyProcessedRecords(List<RowUpdateInfo> rowUpdates)
	{
		// RAGTEST deletes?
		rowUpdates.stream().map(update -> update.getRecord()).forEachOrdered(editedRecords::addEdited);
	}

	/**
	 * Get the first element of a list, filter on subList when not null;
	 */
	private static IRecordInternal getFirstElement(IRecordInternal[] records, List<IRecord> subList)
	{
		for (IRecordInternal record : records)
		{
			if (subList == null || subList.contains(record))
			{
				return record;
			}
		}
		return null;
	}


	/**
	 * Mark record as failed, move to failed records, remove from editedRecords if it was in there.
	 * @param record
	 */
	void markRecordAsFailed(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			editedRecords.remove(record);
			if (!failedRecords.contains(record))
			{
				failedRecords.add(record);
			}
			recordTested.remove(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	public int prepareForSave(boolean looseFocus)
	{
		if (preparingForSave)
		{
			return ISaveConstants.STOPPED;
		}
		try
		{
			preparingForSave = true;
			//stop UI editing and fire onRecordEditStop Event
			if (!firePrepareForSave(looseFocus))
			{
				Debug.trace("no stop editing because prepare for save stopped it"); //$NON-NLS-1$
				return ISaveConstants.VALIDATION_FAILED;
			}
			return ISaveConstants.STOPPED;
		}
		finally
		{
			preparingForSave = false;
		}
	}

	/*
	 * _____________________________________________________________ Methods for data manipulation
	 */
	private RowUpdateInfo getRecordUpdateInfo(IRecordInternal state) throws ServoyException
	{
		Table table = state.getParentFoundSet().getSQLSheet().getTable();
		RowManager rowManager = fsm.getRowManager(fsm.getDataSource(table));

		Row rowData = state.getRawData();
		if (rowData.isFlaggedForDeletion())
		{
			if (!hasAccess(table, IRepository.DELETE))
			{
				throw new ApplicationException(ServoyException.NO_DELETE_ACCESS, new Object[] { table.getName() });
			}
		}
		else if (rowData.existInDB() && !hasAccess(table, IRepository.UPDATE))
		{
			throw new ApplicationException(ServoyException.NO_MODIFY_ACCESS, new Object[] { table.getName() });
		}

		GlobalTransaction gt = fsm.getGlobalTransaction();
		if (gt != null)
		{
			gt.addRecord(table.getServerName(), state);
		}

		RowUpdateInfo rowUpdateInfo = rowManager.getRowUpdateInfo(rowData, hasAccess(table, IRepository.TRACKING), DELETED_RECORDS_FILTER);
		return rowUpdateInfo;
	}

	private boolean testIfRecordIsChanged(IRecordInternal record, boolean checkCalcValues)
	{
		Row rowData = record.getRawData();
		if (rowData == null) return false;

		if (checkCalcValues && record instanceof Record)
		{
			((Record)record).validateStoredCalculations();
		}

		return rowData.isChanged();
	}

	public void removeUnChangedRecords(boolean checkCalcValues, boolean doActualRemove)
	{
		removeUnChangedRecords(checkCalcValues, doActualRemove, null);
	}

	public void removeUnChangedRecords(boolean checkCalcValues, boolean doActualRemove, Predicate< ? super IRecordInternal> recordFilter)
	{
		if (preparingForSave) return;
		// Test the edited records if they are changed or not.
		IRecordInternal[] editedRecordsArray = null;
		editRecordsLock.lock();
		try
		{
			editedRecordsArray = editedRecords.getEdited();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		for (Object element : editedRecordsArray)
		{
			IRecordInternal record = (IRecordInternal)element;
			if ((recordFilter == null || recordFilter.test(record)) && !testIfRecordIsChanged(record, checkCalcValues))
			{
				if (doActualRemove)
				{
					removeEditedRecord(record);
				}
				else
				{
					stopEditing(true, record);
				}
			}
		}
	}

	public void removeEditedRecord(IRecordInternal r)
	{
		boolean empty;
		editRecordsLock.lock();
		try
		{
			editedRecords.removeEdited(r);
			recordTested.remove(r);
			failedRecords.remove(r);
			empty = editedRecords.isEmpty();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		if (empty)
		{
			fireEditChange();
		}
	}


// RAGTEST niet gebruikt	public void removeDeleteQuery(QueryDelete deleteQuery)
//	{
//		editRecordsLock.lock();
//		try
//		{
//			editedRecords.removeDeleteQuery(deleteQuery);
//		}
//		finally
//		{
//			editRecordsLock.unlock();
//		}
//	}

	void removeEditedRecords(FoundSet set)
	{
		IRecordInternal[] editedRecordsArray = null;
		editRecordsLock.lock();
		try
		{
			editedRecordsArray = editedRecords.getEdited();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		for (Object element : editedRecordsArray)
		{
			IRecordInternal record = (IRecordInternal)element;
			if (record.getParentFoundSet() == set)
			{
				removeEditedRecord(record);
			}
		}
	}

	public boolean removeRecords(String datasource)
	{
		boolean hasRemovedRecords = false;
		boolean editedRecordsChanged = false;
		editRecordsLock.lock();
		try
		{
			editedRecordsChanged = editedRecords.removeEverything(datasource);
			hasRemovedRecords = failedRecords.removeIf(r -> datasource.equals(r.getDataSource()));
			for (Iterator<Entry<IRecordInternal, List<IPrepareForSave>>> it = recordTested.entrySet().iterator(); it.hasNext();)
			{
				if (datasource.equals(it.next().getKey().getDataSource()))
				{
					it.remove();
					hasRemovedRecords = true;
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}

		if (editedRecordsChanged)
		{
			setDeletedrecordsInternalTableFilter(true);
		}

		return editedRecordsChanged | hasRemovedRecords;
	}

	public boolean startEditing(IRecordInternal record, boolean mustFireEditRecordChange)
	{
		if (record == null)
		{
			throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.editNullRecord")); //$NON-NLS-1$
		}
		// only IRowListeners can go into edit.. (SubSummaryFoundSet records can't)
		if (!(record.getParentFoundSet() instanceof IRowListener)) return true;

		if (isEditing(record))
		{
			editRecordsLock.lock();
			try
			{
				recordTested.remove(record);
			}
			finally
			{
				editRecordsLock.unlock();
			}
			return true;
		}

		boolean canStartEditing = record instanceof FindState;
		if (!canStartEditing)
		{
			if (record.existInDataSource())
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.UPDATE))
				{
					canStartEditing = !record.isLocked(); // enable only if not locked by someone else
				}
				else
				{
					// TODO throw exception??
					//	Error handler ???
				}
			}
			else
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.INSERT))
				{
					canStartEditing = true;
				}
				else
				{
					// TODO throw exception??
					// Error handler ???
				}
			}
		}
		if (canStartEditing)
		{
			if (mustFireEditRecordChange) canStartEditing = fireEditRecordStart(record);
			// find states also to the global foundset manager??
			if (canStartEditing)
			{
				editRecordsLock.lock();
				boolean wasEmpty = editedRecords.isEmpty();
				try
				{
					// editRecordStop should be called for this record to match the editRecordStop call
					recordTested.remove(record);

					// extra check if no other thread already added this record
					if (!editedRecords.contains(record))
					{
						editedRecords.addEdited(record);
					}
					failedRecords.remove(record);
					// reset the exception so that it is tried again.
					record.getRawData().setLastException(null);
				}
				finally
				{
					editRecordsLock.unlock();
				}
				if (wasEmpty)
				{
					fireEditChange();
				}
			}
		}
		return canStartEditing;
	}

	public boolean addDeletedRecord(IRecordInternal record)
	{
		if (record == null)
		{
			throw new IllegalArgumentException(fsm.getApplication().getI18NMessage("servoy.foundSet.error.editNullRecord")); //$NON-NLS-1$
		}

		editRecordsLock.lock();
		try
		{
			if (editedRecords.containsDeleted(record))
			{
				return true;
			}
			//RAGTEST		recordTested.remove(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}

		boolean canDeleteRecord = record instanceof FindState; // RAGTEST delete FindState?
		if (!canDeleteRecord)
		{
			if (record.existInDataSource())
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.DELETE))
				{
					canDeleteRecord = !record.isLocked();// enable only if not locked by someone else
				}
				else
				{
					// TODO throw exception??
					//	Error handler ???
				}
			}
//		}
//		if (canDeleteRecord)
//		{
			// RAGTEST firedelete als dit record het enige in editing was
//			if (mustFireEditRecordChange) isEditing = fireEditRecordStart(record); // RAGTEST aparte fire fireEditDeleteStart?
//			// find states also to the global foundset manager??
			if (canDeleteRecord)
			{
				boolean fireChange = false;
				editRecordsLock.lock();
				try
				{
					// RAGTEST check logica
					// RAGTEST in andere methods uit deletedRecords halen
					// editRecordStop should be called for this record to match the editRecordStop call
					recordTested.remove(record);

					// extra check if no other thread already added this record
					if (editedRecords.contains(record))
					{
						editedRecords.remove(record);
						fireChange = editedRecords.isEmpty();
					}
					failedRecords.remove(record);
					// reset the exception so that it is tried again.
					record.getRawData().setLastException(null);

					record.getRawData().flagForDeletion();
					editedRecords.addDeleted(record);
				}
				finally
				{
					editRecordsLock.unlock();
				}

				setDeletedrecordsInternalTableFilter(false);

				if (fireChange)
				{
					// RAGTEST icm filter fire?
					fireEditChange();
				}
			}
		}
		return canDeleteRecord;
	}

	// RAGTEST geladen records al meegeven voor removeedited?
	public void addDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery)
	{
		editRecordsLock.lock();
		try
		{
			editedRecords.addDeleteQuery(foundset, deleteQuery);
		}
		finally
		{
			editRecordsLock.unlock();
		}

		setDeletedrecordsInternalTableFilter(false);
	}

	private void setDeletedrecordsInternalTableFilter(boolean fire)
	{
		// get the servers that have a filter now
		Set<String> serverNames = fsm.getTableFilters(DELETED_RECORDS_FILTER).stream().map(TableFilter::getServerName).collect(toSet());

		// add the servers with deleted records
		Map<String, List<IRecordInternal>> deletedRecordsPerServername = stream(editedRecords.getDeleted()).collect(groupingBy(
			deletedRecord -> deletedRecord.getParentFoundSet().getTable().getServerName()));
		serverNames.addAll(deletedRecordsPerServername.keySet());

		// add the servers with deleted queries
		Map<String, List<Entry<ITable, List<QueryDelete>>>> deleteQueriesPerServername = editedRecords.getDeleteQueries().entrySet().stream()
			.collect(groupingBy(entry -> entry.getKey().getServerName()));
		serverNames.addAll(deleteQueriesPerServername.keySet());

		serverNames.forEach(serverName -> {
			try
			{
				List<TableFilterRequest> tableFilterRequests = new ArrayList<>();
				tableFilterRequests.addAll(getTableFilterRequestsDeletedRecords(deletedRecordsPerServername.get(serverName)));
				tableFilterRequests.addAll(getTableFilterRequestsDeleteQueries(deleteQueriesPerServername.get(serverName)));

				fsm.setTableFilters(DELETED_RECORDS_FILTER, serverName, tableFilterRequests, true, fire);
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		});
	}

	private static List<TableFilterRequest> getTableFilterRequestsDeletedRecords(List<IRecordInternal> deletedRecords)
	{
		if (deletedRecords == null)
		{
			return emptyList();
		}

		Map<ITable, List<IRecordInternal>> deletedPerTable = deletedRecords.stream()
			.collect(groupingBy(deletedRecord -> deletedRecord.getParentFoundSet().getTable()));

		return deletedPerTable.entrySet().stream().map(entry -> {
			ITable table = entry.getKey();
			List<IRecordInternal> records = entry.getValue();
			QuerySelect select = new QuerySelect(table.queryTable());

			QueryColumn[] pkColumns = table.getRowIdentColumns().stream().map(column -> column.queryColumn(select.getTable()))
				.toArray(QueryColumn[]::new);
			Object[][] pkValues = convertPKValuesForQueryCompare(records.stream().map(IRecordInternal::getPK).toArray(Object[][]::new),
				pkColumns.length);

			select.setCondition(SQLGenerator.CONDITION_DELETED, new SetCondition(IBaseSQLCondition.NOT_OPERATOR, pkColumns, pkValues, true));
			return new TableFilterRequest(table, new QueryTableFilterdefinition(select), false);
		}).collect(toList());
	}

	private static List<TableFilterRequest> getTableFilterRequestsDeleteQueries(List<Entry<ITable, List<QueryDelete>>> deleteQueriesPerTable)
	{
		if (deleteQueriesPerTable == null)
		{
			return emptyList();
		}

		return deleteQueriesPerTable.stream().map(entry -> {
			ITable table = entry.getKey();
			List<QueryDelete> deleteQueries = entry.getValue();

			QuerySelect select = new QuerySelect(table.queryTable());
			deleteQueries.stream()
				.map(deleteQuery -> relinkTable(deleteQuery.getTable(), select.getTable(), deepClone(deleteQuery.getCondition())))
				.forEach(condition -> select.addCondition(SQLGenerator.CONDITION_DELETED, condition == null ? FALSE_CONDITION : condition.negate()));

			return new TableFilterRequest(table, new QueryTableFilterdefinition(select), false);
		}).collect(toList());
	}

	public void clearSecuritySettings()
	{
		accessMap.clear();
	}

	public boolean hasAccess(ITable table, int flag)
	{
		if (table == null)
		{
			// no table, cannot insert/update/delete
			return (flag & (IRepository.INSERT | IRepository.UPDATE | IRepository.DELETE)) == 0;
		}
		Integer access = accessMap.get(table);
		if (access == null)
		{
			try
			{
				Iterator<String> it = table.getRowIdentColumnNames();
				if (it.hasNext())
				{
					String cname = it.next();
					//access is based on columns, but we don't yet use that fine grained level, its table only
					access = Integer.valueOf(
						fsm.getApplication().getFlattenedSolution().getSecurityAccess(Utils.getDotQualitfied(table.getServerName(), table.getName(), cname),
							fsm.getApplication().getFlattenedSolution().getSolution().getImplicitSecurityNoRights(table.getDataSource())
								? IRepository.IMPLICIT_TABLE_NO_ACCESS : IRepository.IMPLICIT_TABLE_ACCESS));
				}
				else
				{
					access = Integer.valueOf(-2);
				}
				accessMap.put(table, access);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				return false;
			}
		}
		if (access.intValue() == -1 && (flag == IRepository.TRACKING || flag == IRepository.TRACKING_VIEWS)) //deny tracking if security not is specified
		{
			return false;
		}
		return ((access.intValue() & flag) != 0);
	}

	protected boolean fireEditRecordStart(final IRecordInternal record)
	{
		Object[] array = prepareForSaveListeners.toArray();
		for (Object element : array)
		{
			IPrepareForSave listener = (IPrepareForSave)element;
			if (!listener.recordEditStart(record))
			{
				return false;
			}
		}
		return true;
	}

	protected void fireEditChange()
	{
		boolean editRecordsEmpty;
		boolean failedRecordsEmpty;
		editRecordsLock.lock();
		try
		{
			editRecordsEmpty = editedRecords.isEmpty();
			failedRecordsEmpty = failedRecords.isEmpty();
		}
		finally
		{
			editRecordsLock.unlock();
		}

		GlobalEditEvent e = new GlobalEditEvent(this, !editRecordsEmpty || !failedRecordsEmpty);
		Object[] array = editListeners.toArray();
		for (Object element : array)
		{
			IGlobalEditListener listener = (IGlobalEditListener)element;
			listener.editChange(e);
		}

		if (editRecordsEmpty)
		{
			fsm.performActionIfRequired();
		}
	}

	public synchronized Map<FoundSet, int[]> getFoundsetEventMap()
	{
		if (fsEventMap == null)
		{
			fsEventMap = new ConcurrentHashMap<FoundSet, int[]>();
		}
		return fsEventMap;
	}

	public void fireEvents()
	{
		Map<FoundSet, int[]> map = null;
		synchronized (this)
		{
			if (fsEventMap == null || isSavingAll || savingRecords.size() > 0) return;
			map = fsEventMap;
			fsEventMap = null;
		}
		for (Entry<FoundSet, int[]> entry : map.entrySet())
		{
			int[] indexen = entry.getValue();
			FoundSet fs = entry.getKey();
			if (indexen[0] < 0 && indexen[1] < 0)
			{
				// fire foundset-invalidated
				fs.fireFoundSetEvent(-1, -1, FoundSetEvent.FOUNDSET_INVALIDATED);
			}
			else
			{
				fs.fireFoundSetEvent(indexen[0], indexen[1], FoundSetEvent.CHANGE_UPDATE);
			}
		}
		// call until the map is null..
		fireEvents();
	}

	public void addEditListener(IGlobalEditListener editListener)
	{
		editListeners.add(editListener);
	}

	public void removeEditListener(IGlobalEditListener editListener)
	{
		editListeners.remove(editListener);
	}

	protected boolean firePrepareForSave(boolean looseFocus)
	{
		Object[] array = prepareForSaveListeners.toArray();
		for (Object element : array)
		{
			IPrepareForSave listener = (IPrepareForSave)element;
			if (!listener.prepareForSave(looseFocus))
			{
				return false;
			}
		}
		return true;
	}

	public void addPrepareForSave(IPrepareForSave prepareForSave)
	{
		prepareForSaveListeners.add(prepareForSave);
	}

	public void removePrepareForSave(IPrepareForSave prepareForSave)
	{
		prepareForSaveListeners.remove(prepareForSave);
	}

	/**
	 * @param autoSave
	 */
	public boolean setAutoSave(boolean autoSave)
	{
		if (this.autoSave != autoSave)
		{
			if (autoSave)
			{
				if (stopEditing(true) != ISaveConstants.STOPPED)
				{
					return false;
				}
			}
			this.autoSave = autoSave;
		}
		return true;
	}

	public boolean getAutoSave()
	{
		return autoSave;
	}

	public void rollbackRecords()
	{
		List<IRecordInternal> array = new ArrayList<>();
		editRecordsLock.lock();
		try
		{
			recordTested.clear();
			array.addAll(failedRecords);
			array.addAll(asList(editedRecords.getAll()));
		}
		finally
		{
			editRecordsLock.unlock();
		}
		rollbackRecords(array, true, null);
	}

	/**
	 * Rollback records and remove delete queries
	 *
	 * @param records
	 * @param rolbackFoundsetDeletes
	 * @param foundset when rolbackFoundsetDeletes roll back for all foundset (null) or a specific one (not null)
	 */
	public void rollbackRecords(List<IRecordInternal> records, boolean rolbackFoundsetDeletes, IFoundSetInternal foundset)
	{
		List<IRecordInternal> array = new ArrayList<>();
		Map<ITable, List<QueryDelete>> deleteQueries = Map.of();
		editRecordsLock.lock();
		try
		{
			for (IRecordInternal record : records)
			{
				recordTested.remove(record);
				if (failedRecords.remove(record)) array.add(record);
				if (editedRecords.contains(record)) array.add(record);
			}
			if (rolbackFoundsetDeletes)
			{
				// when foundset is null this will get delete queries for all foundsets
				deleteQueries = editedRecords.getDeleteQueries(foundset);
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}

		if (!array.isEmpty())
		{
			// sort them as for insert, but then reversed, so related is deleted first
			array = orderUpdatesForInsertOrder(array, IRecordInternal::getRawData, true);

			// The "existsInDB" property sometimes changes while iterating over the array
			// below (for example when we have several Records that point to the same Row
			// and the Row is not yet stored in the database). So we memorize the initial
			// values of "existsInDB" and we use them while iterating over the array.

			boolean[] existsInDB = new boolean[array.size()];
			for (int i = 0; i < array.size(); i++)
			{
				existsInDB[i] = array.get(i).existInDataSource();
			}

			for (int i = 0; i < array.size(); i++)
			{
				Row row = array.get(i).getRawData();

				// TODO all fires in rollback should be accumulated and done here at once.

				row.rollbackFromOldValues(); // we also rollback !existsInDB records, since they can be held in variables
				if (!existsInDB[i])
				{
					row.remove();
				}
				row.clearFlagForDeletion();
			}
			editRecordsLock.lock();
			try
			{
				if (!editedRecords.isEmpty())
				{
					editedRecords.removeAll(array);
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
		}

		if (!deleteQueries.isEmpty())
		{
			editRecordsLock.lock();
			try
			{
				// remove all delete queries
				deleteQueries.values().forEach(it -> it.forEach(editedRecords::removeDeleteQuery));
			}
			finally
			{
				editRecordsLock.unlock();
			}
		}

		if (!array.isEmpty() || !deleteQueries.isEmpty())
		{
			setDeletedrecordsInternalTableFilter(true);
			fireEditChange();
		}
	}

	public IRecordInternal[] getEditedRecords()
	{
		removeUnChangedRecords(true, false);
		editRecordsLock.lock();
		try
		{// RAGTEST bevat niet deleted, aparte api?
			return editedRecords.getEdited();
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	/**
	 * @param set
	 * @return
	 */
	public IRecordInternal[] getUnmarkedEditedRecords(IFoundSetInternal set, IPrepareForSave prepareForSave)
	{
		List<IRecordInternal> al = new ArrayList<IRecordInternal>();
		editRecordsLock.lock();
		try
		{
			IRecordInternal[] edited = editedRecords.getEdited();
			for (int i = edited.length; --i >= 0;)
			{
				IRecordInternal record = edited[i];
				if (record.getParentFoundSet() == set)
				{
					List<IPrepareForSave> forms = recordTested.get(record);
					if (forms == null || !forms.contains(prepareForSave))
					{
						al.add(record);
					}
				}
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
		return al.toArray(new IRecordInternal[al.size()]);
	}


	/**
	 * @param record
	 */
	public void markRecordTested(IRecordInternal record, IPrepareForSave prepareForSave)
	{
		editRecordsLock.lock();
		try
		{
			List<IPrepareForSave> forms = recordTested.get(record);
			if (forms == null)
			{
				forms = new ArrayList<>();
				recordTested.put(record, forms);
			}
			if (!forms.contains(prepareForSave))
			{
				forms.add(prepareForSave);
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

	public void init()
	{
		editRecordsLock.lock();
		try
		{
			accessMap.clear();//per table (could be per column in future)
			autoSave = true;
			preparingForSave = false;
			isSavingAll = false;
			savingRecords = new ArrayList<>();

			editedRecords.clear();
			failedRecords.clear();
			recordTested.clear();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		// make sure that flush actions on the foundset manager, that are called by that fireEditChange() are not executed anymore
		// shouldn't be needed for a solution.close() or exit();
		fsm.clearFlushActions();
		fireEditChange();
	}

	/**
	 * If true then the save/stopedit of records will be ignored for that time.
	 * Do make sure that you turn this boolean back to false when you are done ignoring the possible saves.
	 * (try/finally)
	 *
	 * @param ignore
	 */
	public void ignoreSave(boolean ignore)
	{
		this.ignoreSave = ignore;
	}

}
