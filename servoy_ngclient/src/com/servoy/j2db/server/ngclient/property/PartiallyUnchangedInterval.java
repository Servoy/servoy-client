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

package com.servoy.j2db.server.ngclient.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.j2db.server.ngclient.property.ViewportChangeKeeper.IntervalSequenceModifier;

/**
 * A partially unchanged interval; the difference between this and UnchangedInterval is that it will have changes but only to some columns in the row, not to the whole row.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class PartiallyUnchangedInterval extends UnchangedInterval
{

	private final List<String> changedColumnNames = new ArrayList<>();

	/**
	 * See {@link UnchangedInterval} for other params.
	 * @param columnName the name of a column that actually did change
	 */
	public PartiallyUnchangedInterval(int initialStartIndex, int initialEndIndex, int newStartIndex, int newEndIndex, String columnName)
	{
		super(initialStartIndex, initialEndIndex, newStartIndex, newEndIndex);
		if (initialStartIndex != initialEndIndex || newStartIndex != newEndIndex || columnName == null)
			throw new IllegalArgumentException("Partial row changed intervals are not supported for multiple indexes or without column name... Column name: " +
				columnName + ", [" + initialStartIndex + ", " + initialEndIndex + "].");

		changedColumnNames.add(columnName);
	}

	@Override
	protected int applyChange(ViewportOperation changeOperation, IntervalSequenceModifier intervalSequenceModifier)
	{
		// on partially changed intervals, a partial update on the same row will just append to column names;
		// inserts/deletes/full updates that overlap will behave the same as they do for UnchangedInterval
		if (changeOperation.columnName != null && changeOperation.startIndex == getNewStart())
		{
			// we can assume that both this and changeOperation have identical end-indexes as partial changes are only allowed for 1 row
			if (!changedColumnNames.contains(changeOperation.columnName)) changedColumnNames.add(changeOperation.columnName);

			return getUnchangedIndexesCount(); // 0
		}
		else return super.applyChange(changeOperation, intervalSequenceModifier);
	}


	@Override
	public void appendEquivalentViewportOperations(List<ViewportOperation> equivalentSequenceOfOperations)
	{
		if (getInitialStart() != getInitialEnd() || getNewStart() != getNewEnd()) throw new RuntimeException(
			"[appendEquivalentViewportOperations] Partial row changed interval indexes were changed incorrectly to more then one in an interval... Column names: " +
				Arrays.asList(changedColumnNames) + ", [" + getInitialStart() + ", " + getInitialEnd() + "].");

		super.appendEquivalentViewportOperations(equivalentSequenceOfOperations);

		// partially changed intervals still need to add changes for each 'column' on that row
		for (String changedColumn : changedColumnNames)
		{
			equivalentSequenceOfOperations.add(new ViewportOperation(getNewStart(), getNewEnd(), ViewportOperation.CHANGE, changedColumn));
		}
	}

	@Override
	public int getUnchangedIndexesCount()
	{
		if (getInitialStart() != getInitialEnd() || getNewStart() != getNewEnd()) throw new RuntimeException(
			"[getUnchangedIndexes] Partial row changed interval indexes were changed incorrectly to more then one partial change in an interval... Column names: " +
				Arrays.asList(changedColumnNames) + ", [" + getInitialStart() + ", " + getInitialEnd() + "].");

		return 0; // this index is actually partially changed!
	}

	@Override
	public String toString()
	{
		return "PartiallyUnchangedInterval [changedColumns=" + changedColumnNames + ", " + super.toString() + "]";
	}

}
