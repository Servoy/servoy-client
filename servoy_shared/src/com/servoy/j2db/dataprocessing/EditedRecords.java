/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.util.ObjectKey;
import com.servoy.j2db.util.WeakHashSet;

/**
 * Keep track of the new, updated or deleted records and delete queries to be performed when saving to the database.
 *
 * @author rgansevles
 */
public class EditedRecords
{
	protected transient int modCount = 0;

	private final LinkedHashMap<Object, EditedRecordOrFoundset> edited = new LinkedHashMap<>(32);

	public void addEdited(IRecordInternal record)
	{
		if (record != null && !containsRecord(record, EditType.edit))
		{
			remove(record);
			edited.put(record.getKey(), EditedRecord.of(record, EditType.edit, emptySet()));
			modCount++;
		}
	}

	public void addDeleted(IRecordInternal record, Collection<IFoundSetInternal> affectedFoundsets)
	{
		if (record != null)
		{
			remove(record);
			edited.put(record.getKey(), EditedRecord.of(record, EditType.delete, affectedFoundsets));
			modCount++;
		}
	}

	public void addFailed(IRecordInternal record)
	{
		if (record != null)
		{
			remove(record);
			edited.put(record.getKey(), EditedRecord.of(record, EditType.failed, emptySet()));
			modCount++;
		}
	}

	public boolean containsEdited(IRecordInternal record)
	{
		return containsRecord(record, EditType.edit);
	}

	public boolean containsDeleted(IRecordInternal record)
	{
		return containsRecord(record, EditType.delete);
	}

	public Collection<IFoundSetInternal> getAffectedFoundsets(IRecord record)
	{
		return getEditingRecords().filter(ed -> ed.getRecord().equals(record))
			.findAny()
			.map(EditedRecord::getAffectedFoundsets)
			.orElse(emptySet());
	}

	public boolean contains(IRecordInternal record)
	{
		return record != null && containsRecord(record, null);
	}

	public boolean contains(Predicate< ? super IRecord> recordFilter)
	{
		return getRecords(null).map(er -> er.getRecord()).anyMatch(recordFilter);
	}

	public void addDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery, ArrayList<TableFilter> filters,
		Collection<IFoundSetInternal> affectedFoundsets)
	{
		var foundsetDeletingQuery = FoundsetDeletingQuery.of(foundset, deleteQuery, filters, affectedFoundsets);
		edited.put(foundsetDeletingQuery.getKey(), foundsetDeletingQuery);
		modCount++;
	}

	public Collection<IFoundSetInternal> getAffectedFoundsets(IFoundSetInternal foundset, QueryDelete deleteQuery)
	{
		return getFoundsetDeletingQueries(foundset).filter(dq -> dq.getQueryDelete() == deleteQuery)
			.findAny()
			.map(FoundsetDeletingQuery::getAffectedFoundsets)
			.orElse(emptySet());
	}

	/**
	 * Get the delete queries grouped per table.
	 */
	public Map<ITable, List<FoundsetDeletingQuery>> getDeleteQueries()
	{
		return getDeleteQueries(null);
	}

	/**
	 * Get the delete queries grouped per table for a foundset (or all if null).
	 */
	public Map<ITable, List<FoundsetDeletingQuery>> getDeleteQueries(IFoundSet foundset)
	{
		return getFoundsetDeletingQueries(foundset)
			.collect(groupingBy(dq -> dq.getFoundset().getTable(),
				mapping(identity(), toList())));
	}

	/**
	 * Get the delete queries for a foundset (or all if null).
	 */
	public Stream<FoundsetDeletingQuery> getFoundsetDeletingQueries(IFoundSet foundset)
	{
		return edited.values().stream().filter(FoundsetDeletingQuery.class::isInstance).map(FoundsetDeletingQuery.class::cast)
			.filter(df -> foundset == null || foundset == df.getFoundset());
	}

	public boolean removeDeleteQuery(FoundsetDeletingQuery foundsetDeletingQuery)
	{
		return increaseModCountIf(edited.remove(foundsetDeletingQuery.getKey()) != null);
	}

	public EditedRecordOrFoundset getAndRemoveFirstEditedRecordOrFoundset(Predicate< ? super EditedRecordOrFoundset> filter)
	{
		Iterator<EditedRecordOrFoundset> it = edited.values().iterator();
		while (it.hasNext())
		{
			EditedRecordOrFoundset first = it.next();
			if (filter.test(first))
			{
				it.remove();
				modCount++;
				return first;
			}
		}
		return null;
	}

	public boolean isEmpty()
	{
		return edited.isEmpty();
	}

	public int size()
	{
		return edited.size();
	}

	public int getModCount()
	{
		return modCount;
	}

	private Stream<EditedRecord> getRecords(EditType editType)
	{
		return getEditingRecords().filter(er -> editType == null || er.type == editType);
	}

	private boolean containsRecord(IRecordInternal record, EditType editType)
	{
		if (record == null)
		{
			return false;
		}
		EditedRecord editedRecord = (EditedRecord)edited.get(record.getKey());
		return editedRecord != null && (editType == null || editedRecord.type == editType);
	}

	public boolean removeForDatasource(String datasource)
	{
		return increaseModCountIf(edited.values().removeIf(e -> datasource.equals(e.getDataSource())));
	}

	public boolean removeAll(List<IRecordInternal> array)
	{
		return increaseModCountIf(array.stream().map(IRecordInternal::getKey).map(edited::remove).filter(Objects::nonNull).count() > 0);
	}

	public boolean remove(IRecordInternal record)
	{
		return record != null && edited.remove(record.getKey()) != null;
	}

	public boolean removeEdited(IRecordInternal record)
	{
		return removeRecord(record, EditType.edit);
	}

	public boolean removeDeleted(IRecordInternal record)
	{
		return removeRecord(record, EditType.delete);
	}

	public boolean removeFailed(IRecordInternal record)
	{
		return removeRecord(record, EditType.failed);
	}

	public boolean removeFailedIf(Predicate< ? super IRecordInternal> filter)
	{
		return increaseModCountIf(edited.values()
			.removeIf(er -> er instanceof EditedRecord editedRecord && (editedRecord.type == EditType.failed) && filter.test(editedRecord.getRecord())));
	}

	public IRecordInternal[] getEdited()
	{
		return toArray(getRecords(EditType.edit));
	}

	public IRecordInternal[] getDeleted()
	{
		return toArray(getRecords(EditType.delete));
	}

	public IRecordInternal[] getFailed()
	{
		return toArray(getRecords(EditType.failed));
	}

	public IRecordInternal[] getAll()
	{
		return toArray(getRecords(null));
	}

	private static IRecordInternal[] toArray(Stream<EditedRecord> editingRecords)
	{
		return editingRecords.map(er -> er.getRecord()).toArray(IRecordInternal[]::new);
	}

	public void clear()
	{
		if (!edited.isEmpty())
		{
			edited.clear();
			modCount++;
		}
	}

	private boolean increaseModCountIf(boolean b)
	{
		if (b)
		{
			modCount++;
		}
		return b;
	}

	private Stream<EditedRecord> getEditingRecords()
	{
		return edited.values().stream().filter(EditedRecord.class::isInstance).map(EditedRecord.class::cast);
	}

	private boolean removeRecord(IRecordInternal record, EditType editType)
	{
		if (record != null)
		{
			Object key = record.getKey();
			EditedRecord editedRecord = (EditedRecord)edited.get(key);
			if (editedRecord != null && editedRecord.type == editType)
			{
				edited.remove(key);
				modCount++;
				return true;
			}
		}
		return false;
	}

	private enum EditType
	{
		edit, delete, failed
	}

	/**
	 * Common interface for EditedRecord and FoundsetDeletingQuery
	 */
	public sealed interface EditedRecordOrFoundset permits EditedRecord, FoundsetDeletingQuery
	{
		String getDataSource();
	}

	record EditedRecord(IRecordInternal getRecord, EditType type, Collection<IFoundSetInternal> getAffectedFoundsets) implements EditedRecordOrFoundset
	{
		EditedRecord
		{
			if (!(getAffectedFoundsets instanceof WeakHashSet))
			{
				throw new IllegalArgumentException("affectedFoundsets must be WeakHashSet, use EditedRecord.of() factory method");
			}
		}

		static EditedRecord of(IRecordInternal record, EditType type, Collection<IFoundSetInternal> affectedFoundsets)
		{
			return new EditedRecord(record, type, new WeakHashSet<>(affectedFoundsets));
		}

		@Override
		public String getDataSource()
		{
			return getRecord().getDataSource();
		}

		boolean isEdit()
		{
			return type == EditType.edit;
		}

		boolean isDelete()
		{
			return type == EditType.delete;
		}

		boolean isFailed()
		{
			return type == EditType.failed;
		}

		@Override
		public String toString()
		{
			return type + " " + getRecord();
		}
	}

	public record FoundsetDeletingQuery(IFoundSetInternal getFoundset, QueryDelete getQueryDelete, ArrayList<TableFilter> getFilters,
		Collection<IFoundSetInternal> getAffectedFoundsets, ObjectKey getKey) implements EditedRecordOrFoundset
	{
		static FoundsetDeletingQuery of(IFoundSetInternal foundset, QueryDelete queryDelete, ArrayList<TableFilter> filters,
			Collection<IFoundSetInternal> affectedFoundsets)
		{
			return new FoundsetDeletingQuery(foundset, queryDelete, filters, affectedFoundsets, new ObjectKey(queryDelete, foundset));
		}

		@Override
		public String getDataSource()
		{
			return getFoundset().getDataSource();
		}
	}

}
