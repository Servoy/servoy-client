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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.servoy.j2db.query.QueryDelete;

/**
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public class EditedRecords
{
// RAGTEST failedRecords?
	// RAGTET failed delete?
	private final List<EditingRecord> records = synchronizedList(new ArrayList<>(32));
	private final List<DeletingFoundset> deleteQueries = new ArrayList<>();

	public void addEdited(IRecordInternal record)
	{
		if (!contains(record, null))
		{
			records.add(new EditingRecord(record, EditType.edit));
		}
	}

	public void addDeleted(IRecordInternal record)
	{
		remove(record);
		records.add(new EditingRecord(record, EditType.delete));
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
		return contains(record, null);
	}

	public boolean contains(Predicate< ? super IRecord> recordFilter)
	{
		return getRecords(null).map(er -> er.record).anyMatch(recordFilter);
	}

	public void addDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery)
	{
		deleteQueries.add(new DeletingFoundset(foundset, deleteQuery));
	}

//	public void rmeoveDeleteQuery(IFoundSetInternal foundset, QueryDelete deleteQuery)
//	{
//		List<QueryDelete> list = deleteQueries.get(foundset);
//		if (list != null)
//		{
//			list.remove(deleteQuery);
//			if (list.isEmpty())
//			{
//				deleteQueries.remove(foundset);
//			}
//		}
//	}

	public int size()
	{
		return records.size(); // RAGTEST empty als er wel delete queries zijn?
	}

	private Stream<EditingRecord> getRecords(EditType editType)
	{
		return records.stream().filter(er -> editType == null || er.type == editType);
	}

	private boolean contains(IRecord record, EditType editType)
	{
		return getRecords(editType).anyMatch(er -> record.equals(er.record));
	}

	public void removeAll(List<IRecordInternal> array)
	{
		records.removeIf(er -> array.contains(er.record));
	}

	public boolean remove(IRecordInternal record)
	{
		return records.removeIf(er -> record.equals(er.record));
	}

	public boolean removeEdited(IRecordInternal record)
	{
		return records.removeIf(er -> er.type == EditType.edit && record.equals(er.record));
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
		records.clear(); // RAGTEST queries ook ?
	}

	private enum EditType
	{
		edit, delete
	}

	private static class EditingRecord
	{
		final IRecordInternal record;
		final EditType type;

		EditingRecord(IRecordInternal record, EditType type)
		{
			this.record = record;
			this.type = type;
		}

		@Override
		public String toString()
		{
			return type + " " + record;
		}
	}

	private static class DeletingFoundset
	{
		final IFoundSet foundSet;
		final QueryDelete queryDelete;

		DeletingFoundset(IFoundSet foundSet, QueryDelete queryDelete)
		{
			this.foundSet = foundSet;
			this.queryDelete = queryDelete;
		}

		@Override
		public String toString()
		{
			return "delete from fs: " + queryDelete;
		}
	}

}
