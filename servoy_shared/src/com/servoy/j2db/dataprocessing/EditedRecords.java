/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.QueryDelete;

/**
 * Keep track of the new, updated or deleted records and delete queries to be performed when saving to the database.
 *
 * @author rgansevles
 */
public class EditedRecords
{
	protected transient int modCount = 0;

	private final List<EditedRecordOrFoundset> edited = synchronizedList(new ArrayList<>(32));

	public void addEdited(IRecordInternal record)
	{
		if (!contains(record, EditType.edit))
		{
			remove(record);
			edited.add(new EditedRecord(record, EditType.edit));
			modCount++;
		}
	}

	public void addDeleted(IRecordInternal record)
	{
		remove(record);
		edited.add(new EditedRecord(record, EditType.delete));
		modCount++;
	}

	public void addFailed(IRecordInternal record)
	{
		remove(record);
		edited.add(new EditedRecord(record, EditType.failed));
		modCount++;
	}

	public boolean containsEdited(IRecord record)
	{
		return contains(record, EditType.edit);
	}

	public boolean containsDeleted(IRecord record)
	{
		return contains(record, EditType.delete);
	}

	public boolean contains(IRecord record)
	{
		return record != null && contains(record, null);
	}

	public boolean contains(Predicate< ? super IRecord> recordFilter)
	{
		return getRecords(null).map(er -> er.record).anyMatch(recordFilter);
	}

	public void addDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery)
	{
		edited.add(new FoundsetDeletingQuery(foundset, deleteQuery));
		modCount++;
	}

	/**
	 * Get the delete queries grouped per table.
	 */
	public Map<ITable, List<QueryDelete>> getDeleteQueries()
	{
		return getDeleteQueries(null);
	}

	/**
	 * Get the delete queries grouped per table for a foundset (or all if null).
	 */
	public Map<ITable, List<QueryDelete>> getDeleteQueries(IFoundSet foundset)
	{
		return getFoundsetDeletingQueries(foundset)
			.collect(groupingBy(dq -> dq.foundset.getTable(),
				mapping(dq -> dq.queryDelete, toList())));
	}

	/**
	 * Get the delete queries for a foundset (or all if null).
	 */
	public Stream<FoundsetDeletingQuery> getFoundsetDeletingQueries(IFoundSet foundset)
	{
		return edited.stream().filter(FoundsetDeletingQuery.class::isInstance).map(FoundsetDeletingQuery.class::cast)
			.filter(df -> foundset == null || foundset == df.foundset);
	}

	public boolean removeDeleteQuery(FoundsetDeletingQuery foundsetDeletingQuery)
	{
		return modCount(edited.removeIf(df -> df == foundsetDeletingQuery));
	}

	public EditedRecordOrFoundset getAndRemoveFirstEditedRecordOrFoundset(Predicate< ? super EditedRecordOrFoundset> filter)
	{
		Iterator<EditedRecordOrFoundset> it = edited.iterator();
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

	private boolean contains(IRecord record, EditType editType)
	{
		return getRecords(editType).anyMatch(er -> record.equals(er.record));
	}

	public boolean removeForDatasource(String datasource)
	{
		return modCount(edited.removeIf(e -> datasource.equals(e.getDataSource())));
	}

	public boolean removeAll(List<IRecordInternal> array)
	{
		return modCount(edited.removeIf(er -> er instanceof EditedRecord editedRecord && array.contains(editedRecord.record)));
	}

	public boolean remove(IRecordInternal record)
	{
		return modCount(edited.removeIf(isEditingRecord(record, null)));
	}

	public boolean removeEdited(IRecordInternal record)
	{
		return modCount(edited.removeIf(isEditingRecord(record, EditType.edit)));
	}

	public boolean removeDeleted(IRecordInternal record)
	{
		return modCount(edited.removeIf(isEditingRecord(record, EditType.delete)));
	}

	public boolean removeFailed(IRecordInternal record)
	{
		return modCount(edited.removeIf(isEditingRecord(record, EditType.failed)));
	}

	public boolean removeFailedIf(Predicate< ? super IRecordInternal> filter)
	{
		return modCount(edited
			.removeIf(er -> er instanceof EditedRecord editedRecord && (editedRecord.type == EditType.failed) && filter.test(editedRecord.record)));
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
		return editingRecords.map(er -> er.record).toArray(IRecordInternal[]::new);
	}

	public void clear()
	{
		if (!edited.isEmpty())
		{
			edited.clear();
			modCount++;
		}
	}

	private boolean modCount(boolean b)
	{
		if (b)
		{
			modCount++;
		}
		return b;
	}

	private Stream<EditedRecord> getEditingRecords()
	{
		return edited.stream().filter(EditedRecord.class::isInstance).map(EditedRecord.class::cast);
	}

	private static Predicate< ? super EditedRecordOrFoundset> isEditingRecord(IRecordInternal record, EditType editType)
	{
		return er -> er instanceof EditedRecord editedRecord && (editType == null || editedRecord.type == editType) && record.equals(editedRecord.record);
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

	public static final class EditedRecord implements EditedRecordOrFoundset
	{
		private final IRecordInternal record;
		private final EditType type;

		EditedRecord(IRecordInternal record, EditType type)
		{
			this.record = record;
			this.type = type;
		}

		public IRecordInternal getRecord()
		{
			return record;
		}

		@Override
		public String getDataSource()
		{
			return record.getDataSource();
		}

		public boolean isEdit()
		{
			return type == EditType.edit;
		}

		public boolean isDelete()
		{
			return type == EditType.delete;
		}

		public boolean isFailed()
		{
			return type == EditType.failed;
		}

		@Override
		public String toString()
		{
			return type + " " + record;
		}
	}

	public static final class FoundsetDeletingQuery implements EditedRecordOrFoundset
	{
		private final IFoundSetInternal foundset;
		private final QueryDelete queryDelete;

		private FoundsetDeletingQuery(IFoundSetInternal foundset, QueryDelete queryDelete)
		{
			this.foundset = foundset;
			this.queryDelete = queryDelete;
		}

		public QueryDelete getQueryDelete()
		{
			return queryDelete;
		}

		public IFoundSetInternal getFoundset()
		{
			return foundset;
		}

		@Override
		public String getDataSource()
		{
			return foundset.getDataSource();
		}
	}
}