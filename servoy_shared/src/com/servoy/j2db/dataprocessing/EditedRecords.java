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
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public class EditedRecords
{
	private final List<EditingRecordOrDeletingFoundset> edited = synchronizedList(new ArrayList<>(32));

	public void addEdited(IRecordInternal record)
	{
		if (!contains(record, null))
		{
			edited.add(new EditingRecord(record, EditType.edit));
		}
	}

	public void addDeleted(IRecordInternal record)
	{
		remove(record);
		edited.add(new EditingRecord(record, EditType.delete));
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
		edited.add(new DeletingFoundset(foundset, deleteQuery));
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
		return getDeletingFoundsets()
			.filter(df -> foundset == null || foundset == df.foundSet)
			.collect(groupingBy(dq -> dq.foundSet.getTable(),
				mapping(dq -> dq.queryDelete, toList())));
	}


	public boolean removeDeleteQuery(QueryDelete queryDelete)
	{
		return edited.removeIf(df -> df instanceof DeletingFoundset && ((DeletingFoundset)df).queryDelete == queryDelete);
	}

	public RAGTEST getAndRemoveFirstRagtest(Predicate< ? super RAGTEST> filter)
	{
		Iterator<EditingRecordOrDeletingFoundset> it = edited.iterator();
		while (it.hasNext())
		{
			EditingRecordOrDeletingFoundset e = it.next();

			RAGTEST ragtest;
			if (e instanceof EditingRecord)
			{
				ragtest = new RagtestEditedRecord(((EditingRecord)e).record);
			}
			else
			{
				ragtest = new RagtestDeleteQuery(((DeletingFoundset)e).foundSet, ((DeletingFoundset)e).queryDelete);
			}

			if (filter.test(ragtest))
			{
				it.remove();
				return ragtest;
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
		return edited.size(); // RAGTEST empty als er wel delete queries zijn?
	}

	private Stream<EditingRecord> getRecords(EditType editType)
	{
		return getEditingRecords().filter(er -> editType == null || er.type == editType);
	}

	private boolean contains(IRecord record, EditType editType)
	{
		return getRecords(editType).anyMatch(er -> record.equals(er.record));
	}

	public boolean removeForDatasource(String datasource)
	{
		return edited.removeIf(e -> datasource.equals(e.getDataSource()));
	}

	public void removeAll(List<IRecordInternal> array)
	{
		edited.removeIf(er -> er instanceof EditingRecord && array.contains(((EditingRecord)er).record));
	}

	public boolean remove(IRecordInternal record)
	{
		return edited.removeIf(er -> er instanceof EditingRecord && record.equals(((EditingRecord)er).record));
	}

	public boolean removeEdited(IRecordInternal record)
	{
		return edited.removeIf(er -> er instanceof EditingRecord & ((EditingRecord)er).type == EditType.edit && record.equals(((EditingRecord)er).record));
	}

	public IRecordInternal[] getEdited()
	{
		return toArray(getRecords(EditType.edit));
	}

	public IRecordInternal[] getDeleted()
	{
		return toArray(getRecords(EditType.delete));
	}

	public IRecordInternal[] getAll()
	{
		return toArray(getRecords(null));
	}

	private static IRecordInternal[] toArray(Stream<EditingRecord> editingRecords)
	{
		return editingRecords.map(er -> er.record).toArray(IRecordInternal[]::new);
	}

	public void clear()
	{
		edited.clear();
	}

	private Stream<DeletingFoundset> getDeletingFoundsets()
	{
		return edited.stream().filter(DeletingFoundset.class::isInstance).map(DeletingFoundset.class::cast);
	}

	private Stream<EditingRecord> getEditingRecords()
	{
		return edited.stream().filter(EditingRecord.class::isInstance).map(EditingRecord.class::cast);
	}

	public enum EditType
	{
		edit, delete
	}

	private sealed interface EditingRecordOrDeletingFoundset permits EditingRecord, DeletingFoundset
	{
		String getDataSource();
	}

	private static final class EditingRecord implements EditingRecordOrDeletingFoundset
	{
		final IRecordInternal record;
		final EditType type;

		EditingRecord(IRecordInternal record, EditType type)
		{
			this.record = record;
			this.type = type;
		}

		@Override
		public String getDataSource()
		{
			return record.getDataSource();
		}

		@Override
		public String toString()
		{
			return type + " " + record;
		}
	}

	/**
	 * @author rob
	 *
	 */
	public sealed interface RAGTEST permits RagtestEditedRecord, RagtestDeleteQuery
	{

	}

	public final class RagtestEditedRecord implements RAGTEST
	{
		private final IRecordInternal record;

		private RagtestEditedRecord(IRecordInternal record)
		{
			this.record = record;
		}

		public IRecordInternal getRecord()
		{
			return record;
		}
	}

	public final class RagtestDeleteQuery implements RAGTEST
	{
		private final IFoundSetInternal foundset;
		private final QueryDelete queryDelete;

		private RagtestDeleteQuery(IFoundSetInternal foundset, QueryDelete queryDelete)
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
	}

	private static final class DeletingFoundset implements EditingRecordOrDeletingFoundset
	{
		final IFoundSetInternal foundSet;
		final QueryDelete queryDelete;

		DeletingFoundset(IFoundSetInternal foundSet, QueryDelete queryDelete)
		{
			this.foundSet = foundSet;
			this.queryDelete = queryDelete;
		}

		@Override
		public String getDataSource()
		{
			return foundSet.getDataSource();
		}

		@Override
		public String toString()
		{
			return "delete from fs: " + queryDelete;
		}
	}

}
