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

import static com.google.common.collect.Streams.concat;
import static com.servoy.j2db.dataprocessing.SQLGenerator.convertPKValuesForQueryCompare;
import static com.servoy.j2db.query.AbstractBaseQuery.deepClone;
import static com.servoy.j2db.query.AbstractBaseQuery.relinkTable;
import static com.servoy.j2db.query.BooleanCondition.FALSE_CONDITION;
import static com.servoy.j2db.util.Utils.equalObjects;
import static com.servoy.j2db.util.Utils.stream;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
import com.servoy.j2db.dataprocessing.EditedRecords.EditedRecord;
import com.servoy.j2db.dataprocessing.EditedRecords.EditedRecordOrFoundset;
import com.servoy.j2db.dataprocessing.EditedRecords.FoundsetDeletingQuery;
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

	private final Map<ITable, Integer> accessMap = synchronizedMap(new HashMap<>());//per table (could be per column in future)
	private boolean autoSave = true;

	private ConcurrentMap<FoundSet, int[]> fsEventMap;

	private final ReentrantLock editRecordsLock = new ReentrantLock();

	private final EditedRecords editedRecords = new EditedRecords();
	private final Map<IRecordInternal, List<IPrepareForSave>> recordTested = synchronizedMap(new HashMap<>()); // tested for form.OnRecordEditStop event
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
			return editedRecords.getFailed();
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

	public boolean isEditing()
	{
		editRecordsLock.lock();
		try
		{
			return !editedRecords.isEmpty();
		}
		finally
		{
			editRecordsLock.unlock();
		}
	}

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
			IRecordInternal[] failedRecords = editedRecords.getFailed();
			for (int i = failedRecords.length; --i >= 0;)
			{
				IRecordInternal record = failedRecords[i];
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
					Object[] failedMarkers = Stream.of(editedRecords.getFailed()).map(record -> record.getRecordMarkers()).toArray();
					try
					{
						application.getScriptEngine().getScopesScope()
							.executeGlobalFunction(sm.getScopeName(), sm.getName(),
								Utils.arrayMerge((new Object[] { failedMarkers }),
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

		boolean hasFoundsetDeleteQueries = false;
		if (recordsToSave != null || foundset != null)
		{
			editRecordsLock.lock();
			try
			{
				hasFoundsetDeleteQueries = foundset != null && !editedRecords.getFoundsetDeletingQueries(foundset).findAny().isEmpty();
				if (!hasFoundsetDeleteQueries && stream(recordsToSave).noneMatch(editedRecords::contains))
				{
					return ISaveConstants.STOPPED;
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
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

		int editedRecordsModCount;
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

			// remove any non referenced failed records
			editRecordsLock.lock();
			try
			{
				editedRecords.removeFailedIf(rec -> rec.getParentFoundSet() == null || rec.getParentFoundSet().getRecordIndex(rec) == -1);
			}
			finally
			{
				editRecordsLock.unlock();
			}
			performActionIfRequired();

			// remove the unchanged, really calculate when it is a real stop (autosave = true or it is a javascript stop)
			removeUnChangedRecords(autoSave || javascriptStop, true);

			// check if anything left
			int editRecordListSize;
			editRecordsLock.lock();
			try
			{
				if (!hasFoundsetDeleteQueries && editedRecords.isEmpty()) return ISaveConstants.STOPPED;
				editRecordListSize = editedRecords.size();
			}
			finally
			{
				editRecordsLock.unlock();
			}

			// cannot stop, its blocked
			if (!autoSave && !javascriptStop)
			{
				return ISaveConstants.AUTO_SAVE_BLOCKED;
			}

			int failedCount = 0;

			boolean justValidationErrors = false;
			lastStopEditingException = null;
			List<DatabaseUpdateInfo> dbUpdates = new ArrayList<>(editRecordListSize);
			editRecordsLock.lock();
			try
			{
				if (recordsToSave == null)
				{
					// if it is a save all, then first filter out all the duplicate rows.
					IRecordInternal[] allEdited = editedRecords.getAll();
					for (int i = 0; i < allEdited.length; i++)
					{
						Row toTest = allEdited[i].getRawData();
						for (int j = allEdited.length; --j > i;)
						{
							if (allEdited[j].getRawData() == toTest)
							{
								removeEditedRecord(allEdited[j]);
							}
						}
					}
				}

				Map<IRecordInternal, Integer> processedRecords = new HashMap<>();

				Predicate<EditedRecordOrFoundset> shouldProcessRecordOrFoundset = shouldProcessRecordOrFoundset(recordsToSave, foundset);
				for (EditedRecordOrFoundset tmp = editedRecords.getAndRemoveFirstEditedRecordOrFoundset(shouldProcessRecordOrFoundset); //
					tmp != null; //
					tmp = editedRecords.getAndRemoveFirstEditedRecordOrFoundset(shouldProcessRecordOrFoundset))
				{
					if (tmp instanceof EditedRecord editedRecord)
					{
						IRecordInternal rec = editedRecord.getRecord();
						// check if we do not have an infinite recursive loop
						Integer count = processedRecords.get(rec);
						if (count != null && count.intValue() > 50)
						{
							fsm.getApplication().reportJSError(
								"stopEditing max loop counter exceeded on " + rec.getDataSource() + "/" + rec.getPKHashKey(),
								new RuntimeException());
							return ISaveConstants.SAVE_FAILED;
						}
						processedRecords.put(rec, Integer.valueOf(count == null ? 1 : (count.intValue() + 1)));
					}

					setDeletedrecordsInternalTableFilter(false);

					// tmp is a record to save/delete or a delete-query to perform
					if (tmp instanceof EditedRecord editedRecord)
					{
						Record record = (Record)editedRecord.getRecord();

						// prevent multiple update for the same row (from multiple records)
						for (int j = 0; j < dbUpdates.size(); j++)
						{
							if (dbUpdates.get(j) instanceof RowUpdateInfo rowUpdateInfo && rowUpdateInfo.getRow() == record.getRawData())
							{
								// create a new rowUpdate that contains both updates
								RowUpdateInfo removed = (RowUpdateInfo)dbUpdates.remove(j);
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

							// Note that the behavior is different when trigger returns false or when it throws an exception.
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
									editedRecords.addFailed(record);
									recordTested.remove(record);
								}
								if (!validationErrors)
								{
									RowUpdateInfo rowUpdateInfo = getRecordUpdateInfo(record);
									if (rowUpdateInfo != null)
									{
										rowUpdateInfo.setRecord(record);
										dbUpdates.add(rowUpdateInfo);
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
							record.getRawData().setLastException(e); // set latest
							editedRecords.addFailed(record);
							recordTested.remove(record);
						}
						catch (Exception e)
						{
							Debug.error("Not a normal Servoy/Db Exception generated in saving record: " + record + " removing the record", e); //$NON-NLS-1$ //$NON-NLS-2$
							recordTested.remove(record);
						}
					}

					else if (tmp instanceof FoundsetDeletingQuery foundsetDeletingQuery)
					{
						try
						{
							dbUpdates
								.add(getTableUpdateInfoForDeleteQuery(foundsetDeletingQuery.getFoundset().getTable(), foundsetDeletingQuery.getQueryDelete()));
						}
						catch (ServoyException e)
						{
							log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", //$NON-NLS-1$//$NON-NLS-2$
								e);
							// trigger method threw exception
							lastStopEditingException = e;
							failedCount++;
							editedRecords.addDeleteQuery(foundsetDeletingQuery.getFoundset(), foundsetDeletingQuery.getQueryDelete(),
								foundsetDeletingQuery.getAffectedFoundsets());
						}
						catch (Exception e)
						{
							Debug.error("Not a normal Servoy/Db Exception generated in deleting recors", e);
						}
					}
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}

			if (failedCount > 0)
			{
				placeBackAlreadyProcessedRecords(dbUpdates);
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

			if (dbUpdates.isEmpty())
			{
				performActionIfRequired();
				if (Debug.tracing())
				{
					Debug.trace("no records to update anymore, failed: " + editedRecords.getFailed().length); //$NON-NLS-1$
				}
				return ISaveConstants.STOPPED;
			}

			if (Debug.tracing())
			{
				Debug.trace("Updating/Inserting/Deleting " + dbUpdates.size() + " records: " + dbUpdates.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			List<DatabaseUpdateInfo> infos = orderUpdatesForInsertOrder(dbUpdates, DatabaseUpdateInfo::getRow, false);
			ISQLStatement[] statements;
			if (fsm.config.statementBatching() && infos.size() > 1)
			{
				// Merge insert statements insert statements from all info's: multiple info's can share the same statement of the records are batched together on the statement level
				List<ISQLStatement> mergedStatements = new ArrayList<>(infos.size());

				ISQLStatement prevStatement = null;
				for (DatabaseUpdateInfo rowUpdateInfo : infos)
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
				statements = infos.stream().map(DatabaseUpdateInfo::getISQLStatement).toArray(ISQLStatement[]::new);
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

			List<DatabaseUpdateInfo> infosToBePostProcessed = new ArrayList<>();

			Map<FoundSet, List<Record>> foundsetToRecords = new HashMap<>();
			Map<FoundSet, List<String>> foundsetToAggregateDeletes = new HashMap<>();
			List<Runnable> fires = new ArrayList<>(infos.size());
			int mustRollbackCount = 0;
			// Walk in reverse over it, so that related rows are update in there row manger before they are required by there parents.
			for (int i = infos.size(); --i >= 0;)
			{
				DatabaseUpdateInfo dbUpdateInfo = infos.get(i);
				if (!(dbUpdateInfo instanceof RowUpdateInfo rowUpdateInfo)) continue;
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
					else if (retValue instanceof Object[] rowData)
					{
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
					ISQLStatement statement = dbUpdateInfo.getISQLStatement();
					row.getRowManager().rowUpdated(row, oldKey, foundSet, fires,
						statement instanceof ITrackingSQLStatement trackingSQLStatement ? trackingSQLStatement.getChangedColumns() : null);
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
						editedRecords.addFailed(record);
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
			editedRecordsModCount = editedRecords.getModCount();
			for (var dbUpdateInfo : infosToBePostProcessed)
			{
				if (dbUpdateInfo instanceof RowUpdateInfo rowUpdateInfo)
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
								property = StaticContentSpecLoader.PROPERTY_ONAFTERUPDATEMETHODID;
						}
						((FoundSet)rowUpdateInfoRecord.getParentFoundSet()).executeFoundsetTrigger(new Object[] { rowUpdateInfoRecord }, property, true);
					}
					catch (ServoyException e)
					{
						if (e instanceof DataException && e.getCause() instanceof JavaScriptException)
						{
							// trigger method threw exception
							log.debug("stopEditing(" + javascriptStop + ") encountered an exception - could be expected and treated by solution code or not", //$NON-NLS-1$//$NON-NLS-2$
								e);
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
								editedRecords.addFailed(rowUpdateInfoRecord);
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
			}

			for (Map.Entry<FoundSet, List<Record>> entry : foundsetToRecords.entrySet())
			{
				FoundSet fs = entry.getKey();
				fs.recordsUpdated(entry.getValue(), foundsetToAggregateDeletes.get(fs));
			}

			performActionIfRequired();

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

		if (editedRecords.getModCount() != editedRecordsModCount && recordsToSave == null && foundset == null)
		{
			// records where changed by the after insert/update/delete table events, call stop edit again if this was not a specific record save.
			return stopEditingImpl(javascriptStop, null, null, recursionDepth + 1);
		}

		return ISaveConstants.STOPPED;
	}

	/*
	 * Return a function for the loop to check if the EditedRecordOrFoundset should be processed in the save-loop.
	 */
	private Predicate<EditedRecordOrFoundset> shouldProcessRecordOrFoundset(List<IRecord> subList, IFoundSet foundset)
	{
		return editedRecordOrFoundset -> {
			if (editedRecordOrFoundset instanceof EditedRecord editedRecord)
			{
				if (editedRecord.isFailed())
				{
					return false;
				}
				var record = editedRecord.getRecord();
				if (subList == null || subList.contains(record))
				{
					return true;
				}
				if (record.getParentFoundSet() == foundset)
				{
					return true;
				}
			}
			if (editedRecordOrFoundset instanceof FoundsetDeletingQuery foundsetDeletingQuery)
			{
				return foundset == null || foundset == foundsetDeletingQuery.getFoundset();
			}
			return false;
		};
	}

	private TableUpdateInfo getTableUpdateInfoForDeleteQuery(ITable table, QueryDelete deleteQuery) throws ServoyException
	{
		String tid = fsm.getTransactionID(table.getServerName());
		SQLStatement statement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, table.getServerName(), table.getName(), null,
			tid, deleteQuery, fsm.getTableFilterParams(table.getServerName(), deleteQuery));

		return new TableUpdateInfo(table, statement);
	}

	private <T> List<T> orderUpdatesForInsertOrder(List<T> rowData, Function<T, Row> rowFuction, boolean reverse)
	{
		if (rowData.size() <= 1 || fsm.config.disableInsertsReorder())
		{
			return rowData;
		}

		// split up in blocks of updates on records, delete-by-query (row == null) should be kept in original order;
		List<List<T>> blocks = new ArrayList<>();
		boolean prevHasNoRow = false;
		for (T rd : rowData)
		{
			Row row = rowFuction.apply(rd);
			if ((row == null != prevHasNoRow) || blocks.isEmpty())
			{
				blocks.add(new ArrayList<>());
			}

			blocks.get(blocks.size() - 1).add(rd);
			prevHasNoRow = row == null;
		}

		blocks = blocks.stream().map(block -> orderUpdatesForInsertOrderBlock(block, rowFuction, reverse)).collect(toList());
		if (reverse)
		{
			reverse(blocks);
		}

		return blocks.stream().flatMap(List::stream).collect(toList());
	}

	private <T> List<T> orderUpdatesForInsertOrderBlock(List<T> rowData, Function<T, Row> rowFuction, boolean reverse)
	{
		if (rowData.size() <= 1 || fsm.config.disableInsertsReorder())
		{
			return rowData;
		}

		// search if there are new row pks used that are
		// used in records before this record and sort it based on that.
		List<T> al = new ArrayList<>(rowData);
		int prevI = -1;
		for (int i = al.size(); --i > 0;)
		{
			Row row = rowFuction.apply(al.get(i));
			// dbupdates contains rowupdates and delete-queries
			if (row == null) continue;

			var switched = false;
			if (row.isFlaggedForDeletion())
			{
				switched = switchRowForDelete(row, rowFuction, al, i);
			}
			else if (!row.existInDB())
			{
				switched = switchRowForInsert(row, rowFuction, al, i);
			}
			// watch out for endless loops when 2 records both with pk's point to each other...
			if (switched && prevI != i)
			{
				prevI = i;
				i++;
			}
		}

		if (reverse)
		{
			reverse(al);
		}

		return al;
	}

	/*
	 * Determine if the rows need to be switched around when sorting while doing inserts.
	 *
	 * Main records must be saved before the detail records.
	 */
	private static <T> boolean switchRowForInsert(Row row, Function<T, Row> rowFuction, List<T> al, int i)
	{
		Table table = row.getRowManager().getSQLSheet().getTable();
		String[] pkColumns = row.getRowManager().getSQLSheet().getPKColumnDataProvidersAsArray();
		Object[] pk = row.getPK();
		for (int j = 0; j < pk.length; j++)
		{
			Object pkObject = pk[j];
			// special case if pk was db ident and that value was copied from another row.
			if (pkObject instanceof DbIdentValue dbIdentValue && dbIdentValue.getRow() != row) continue;
			for (int k = 0; k < i; k++)
			{
				Row otherRow = rowFuction.apply(al.get(k));
				Object[] values = otherRow.getRawColumnData();
				int[] pkIndexes = otherRow.getRowManager().getSQLSheet().getPKIndexes();
				IntHashMap<String> pks = new IntHashMap<>(pkIndexes.length, 1);
				for (int pkIndex : pkIndexes)
				{
					pks.put(pkIndex, ""); //$NON-NLS-1$
				}
				for (int l = 0; l < values.length; l++)
				{
					// skip all pk column indexes (except from dbidents from other rows, this may need resort). Those shouldn't be resorted
					if (!(values[l] instanceof DbIdentValue dbIdentValue && dbIdentValue.getRow() != otherRow) && pks.containsKey(l))
						continue;

					if (isSame(table.getColumn(pkColumns[j]), pkObject, values[l]))
					{
						al.add(k, al.remove(i));
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * Determine if the rows need to be switched around when sorting while doing deletes.
	 *
	 * Detail records must be deleted before the main records.
	 */
	private static <T> boolean switchRowForDelete(Row row, Function<T, Row> rowFuction, List<T> al, int i)
	{
		Object[] values = row.getRawColumnData();
		int[] pkIndexes = row.getRowManager().getSQLSheet().getPKIndexes();
		IntHashMap<String> pks = new IntHashMap<>(pkIndexes.length, 1);
		for (int pkIndex : pkIndexes)
		{
			pks.put(pkIndex, ""); //$NON-NLS-1$
		}

		for (Object value : values)
		{
			// special case if pk was db ident and that value was copied from another row.
			if (value instanceof DbIdentValue dbIdentValue && dbIdentValue.getRow() != row) continue;
			for (int k = 0; k < i; k++)
			{
				Row otherRow = rowFuction.apply(al.get(k));
				Table table = otherRow.getRowManager().getSQLSheet().getTable();
				String[] pkColumns = otherRow.getRowManager().getSQLSheet().getPKColumnDataProvidersAsArray();
				Object[] pk = otherRow.getPK();

				for (int l = 0; l < pk.length; l++)
				{
					// skip all pk column indexes (except from dbidents from other rows, this may need resort). Those shouldn't be resorted
					if (!(pk[l] instanceof DbIdentValue dbIdentValue && dbIdentValue.getRow() != otherRow) && pks.containsKey(l))
						continue;

					if (isSame(table.getColumn(pkColumns[l]), value, pk[l]))
					{
						al.add(k, al.remove(i));
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isSame(Column column, Object pkObject, Object value)
	{
		if (value == pkObject)
		{
			return true;
		}
		if (value != null)
		{
			if (column.hasFlag(IBaseColumn.UUID_COLUMN))
			{
				// same uuids are the same even if not the same object
				return equalObjects(pkObject, value, 0, true);
			}
		}
		return false;
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
			valTarget[i] = Utils.arrayJoin(valTarget[i], valSrc[i]);
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
	 * @param dbUpdates
	 */
	private void placeBackAlreadyProcessedRecords(List<DatabaseUpdateInfo> dbUpdates)
	{
		dbUpdates.stream()
			.filter(RowUpdateInfo.class::isInstance)
			.map(RowUpdateInfo.class::cast)
			.map(update -> update.getRecord())
			// RAGTEST addDeleted?
			.forEachOrdered(editedRecords::addEdited);
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
			editedRecords.addFailed(record);
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

		return rowManager.getRowUpdateInfo(rowData, hasAccess(table, IRepository.TRACKING), DELETED_RECORDS_FILTER);
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
		for (IRecordInternal record : editedRecordsArray)
		{
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

	public void removeEditedRecord(IRecordInternal record)
	{
		editRecordsLock.lock();
		try
		{
			editedRecords.remove(record);
			recordTested.remove(record);
		}
		finally
		{
			editRecordsLock.unlock();
		}

		performActionIfRequired();
	}

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
			editedRecordsChanged = editedRecords.removeForDatasource(datasource);
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
				if (editedRecords.removeFailed(record))
				{
					editedRecords.addEdited(record);
				}
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
				try
				{
					// editRecordStop should be called for this record to match the editRecordStop call
					recordTested.remove(record);

					// extra check if no other thread already added this record
					editedRecords.removeFailed(record);
					if (!editedRecords.contains(record))
					{
						editedRecords.addEdited(record);
					}
					// reset the exception so that it is tried again.
					record.getRawData().setLastException(null);
				}
				finally
				{
					editRecordsLock.unlock();
				}

				performActionIfRequired();
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
			recordTested.remove(record);
			if (editedRecords.containsDeleted(record))
			{
				return true;
			}
		}
		finally
		{
			editRecordsLock.unlock();
		}

		boolean canDeleteRecord = record instanceof FindState;
		if (!canDeleteRecord)
		{
			if (record.existInDataSource())
			{
				if (hasAccess(record.getParentFoundSet().getSQLSheet().getTable(), IRepository.DELETE))
				{
					canDeleteRecord = !record.isLocked(); // enable only if not locked by someone else
				}
				else
				{
					// TODO throw exception??
					//	Error handler ???
				}
			}

			if (canDeleteRecord)
			{
				editRecordsLock.lock();
				try
				{
					// editRecordStop should be called for this record to match the editRecordStop call
					recordTested.remove(record);

					// reset the exception so that it is tried again.
					record.getRawData().setLastException(null);
					record.getRawData().flagForDeletion();

					List<IFoundSetInternal> affectedFoundsets = record.getRawData()
						.getRegisterdRecords()
						.map(IRecordInternal::getParentFoundSet)
						.toList();
					editedRecords.addDeleted(record, affectedFoundsets);
				}
				finally
				{
					editRecordsLock.unlock();
				}

				setDeletedrecordsInternalTableFilter(false);

				performActionIfRequired();
			}
		}
		return canDeleteRecord;
	}

	public void addDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery, Collection<IFoundSetInternal> affectedFoundsets)
	{
		editRecordsLock.lock();
		try
		{
			editedRecords.addDeleteQuery(foundset, deleteQuery, affectedFoundsets);
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

	protected void performActionIfRequired()
	{
		if (!isEditing())
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
		Map<FoundSet, int[]> map;
		synchronized (this)
		{
			if (fsEventMap == null || isSavingAll || !savingRecords.isEmpty()) return;
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
	 * @param rollbackFoundsetDeletes
	 * @param foundset when rolbackFoundsetDeletes roll back for all foundset (null) or a specific one (not null)
	 */
	public void rollbackRecords(List<IRecordInternal> records, boolean rollbackFoundsetDeletes, IFoundSetInternal foundset)
	{
		List<IRecordInternal> revertedRecords = new ArrayList<>();
		List<IFoundSetInternal> foundsetsWithRevertingDeletes;
		editRecordsLock.lock();
		try
		{
			for (IRecordInternal record : records)
			{
				recordTested.remove(record);
				if (editedRecords.contains(record)) revertedRecords.add(record);
			}

			foundsetsWithRevertingDeletes = removeDeletesForRevert(records, rollbackFoundsetDeletes, foundset);
		}
		finally
		{
			editRecordsLock.unlock();
		}

		if (!foundsetsWithRevertingDeletes.isEmpty())
		{
			setDeletedrecordsInternalTableFilter(false);
		}

		if (!revertedRecords.isEmpty())
		{
			// sort them as for insert, but then reversed, so related is deleted first
			revertedRecords = orderUpdatesForInsertOrder(revertedRecords, IRecordInternal::getRawData, true);

			// The "existsInDB" property sometimes changes while iterating over the array
			// below (for example when we have several Records that point to the same Row
			// and the Row is not yet stored in the database). So we memorize the initial
			// values of "existsInDB" and we use them while iterating over the array.

			boolean[] existsInDB = new boolean[revertedRecords.size()];
			for (int i = 0; i < revertedRecords.size(); i++)
			{
				existsInDB[i] = revertedRecords.get(i).existInDataSource();
			}

			for (int i = 0; i < revertedRecords.size(); i++)
			{
				Row row = revertedRecords.get(i).getRawData();

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
					editedRecords.removeAll(revertedRecords);
				}
			}
			finally
			{
				editRecordsLock.unlock();
			}
		}

		foundsetsWithRevertingDeletes.forEach(fs -> {
			if (fs instanceof FoundSet)
			{
				try
				{
					((FoundSet)fs).refreshFromDB(false, true);
				}
				catch (ServoyException e)
				{
					Debug.error("Error refreshing foundset", e);
				}
			}
		});

		if (!revertedRecords.isEmpty() || !foundsetsWithRevertingDeletes.isEmpty())
		{
			performActionIfRequired();
		}
	}

	/**
	 * Remove the records and queries to be deleted, remove them from editedRecords and return the affected foundsets.
	 */
	private List<IFoundSetInternal> removeDeletesForRevert(List<IRecordInternal> records, boolean rollbackFoundsetDeletes, IFoundSetInternal foundset)
	{
		List<FoundsetDeletingQuery> deleteQueries = emptyList();
		if (rollbackFoundsetDeletes)
		{
			// when foundset is null this will get delete queries for all foundsets
			deleteQueries = editedRecords.getFoundsetDeletingQueries(foundset).collect(toList());
		}

		List<IFoundSetInternal> foundsetsWithRevertingDeletes = concat(
			stream(records)
				.filter(editedRecords::containsDeleted)
				.map(editedRecords::getAffectedFoundsets),
			deleteQueries.stream()
				.map(FoundsetDeletingQuery::getAffectedFoundsets))
					.flatMap(Collection::stream)
					.distinct().toList();

		// remove all deletes
		stream(records).forEach(editedRecords::removeDeleted);
		deleteQueries.forEach(editedRecords::removeDeleteQuery);

		return foundsetsWithRevertingDeletes;
	}

	public IRecordInternal[] getEditedRecords()
	{
		removeUnChangedRecords(true, false);
		editRecordsLock.lock();
		try
		{
			return editedRecords.getAll();
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
		List<IRecordInternal> al = new ArrayList<>();
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
	public void markRecordTested(IRecordInternal record, IPrepareForSave form)
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
			if (!forms.contains(form))
			{
				forms.add(form);
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
			recordTested.clear();
		}
		finally
		{
			editRecordsLock.unlock();
		}
		// make sure that flush actions on the foundset manager, that are called by that fireEditChange() are not executed anymore
		// shouldn't be needed for a solution.close() or exit();
		fsm.clearFlushActions();
		performActionIfRequired();
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
