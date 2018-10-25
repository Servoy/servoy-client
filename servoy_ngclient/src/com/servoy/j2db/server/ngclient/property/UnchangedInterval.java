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

import java.util.List;

import com.servoy.j2db.server.ngclient.property.ViewportChangeKeeper.IntervalSequenceModifier;

/**
 * An unchanged or (partially unchanged - see {@link PartiallyUnchangedInterval}) interval. It keeps the new indexes and initial indexes. See {@link ViewportChangeKeeper} for more information.<br/><br/>
 *
 * It can correct it's indexes, split into two/three UnchangedIntervals or remove itself from the ViewportChangeKeeper when an insert, delete or update operation
 * are processed.<br/>
 *
 * @author acostescu
 * @see ViewportChangeKeeper
 */
@SuppressWarnings("nls")
public class UnchangedInterval
{

	private int initialStartIndex;
	private int initialEndIndex;
	private int newStartIndex;
	private int newEndIndex;

	public UnchangedInterval(int initialStartIndex, int initialEndIndex, int newStartIndex, int newEndIndex)
	{
		this.initialStartIndex = initialStartIndex;
		this.initialEndIndex = initialEndIndex;
		this.newStartIndex = newStartIndex;
		this.newEndIndex = newEndIndex;
	}

	/**
	 * Applies the given operation to this interval; this can result in a change of interval indexes, a delete of the interval or a split into two intervals.
	 *
	 * @param operation the viewport operation (insert/delete/update) to apply
	 * @param intervalSequenceModifier can be used to delete the interval or split in two (add another new interval after current)
	 *
	 * @return the number of unchanged indexes remaining from this interval after the operation was applied.
	 */
	public int applyOperation(ViewportOperation operation, IntervalSequenceModifier intervalSequenceModifier)
	{
		int remainingUnchangedIndexes;

		switch (operation.type)
		{
			case ViewportOperation.CHANGE :
			case ViewportOperation.CHANGE_IN_LINKED_PROPERTY :
				remainingUnchangedIndexes = applyChange(operation, intervalSequenceModifier);
				break;
			case ViewportOperation.INSERT :
				remainingUnchangedIndexes = applyInsert(operation, intervalSequenceModifier);
				break;
			case ViewportOperation.DELETE :
				remainingUnchangedIndexes = applyDelete(operation, intervalSequenceModifier);
				break;
			default :
				throw new IllegalArgumentException("ViewportOperation type is not one of the supported values: " + operation.type);
		}

		return remainingUnchangedIndexes;
	}

	protected int applyChange(ViewportOperation changeOperation, IntervalSequenceModifier intervalSequenceModifier)
	{
		// if change intersects current interval then we must restrict the unchanged indexes, maybe even split the interval into multiple ones
		int intersectionStart = Math.max(changeOperation.startIndex, newStartIndex);
		int intersectionEnd = Math.min(changeOperation.endIndex, newEndIndex);
		int intersectionSize = intersectionEnd - intersectionStart + 1;
		int unchangedIndexes = 0;

		if (intersectionSize > 0)
		{
			UnchangedInterval newInterval;
			boolean isPartialChange = (changeOperation.columnName != null);

			if (intersectionStart == newStartIndex)
			{
				intervalSequenceModifier.discardCurrentInterval();
				// first part of this unchanged interval is gone; or whole interval is gone
				if (isPartialChange)
				{
					// if it's a partial change add it before current unchanged interval
					newInterval = new PartiallyUnchangedInterval(initialStartIndex, initialStartIndex + intersectionSize - 1, newStartIndex, intersectionEnd,
						changeOperation.columnName); // intersectionSize should always be 1 here actually as partial change viewport operations are only supported for 1 row
					intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
					unchangedIndexes = newInterval.getUnchangedIndexesCount();
				}

				// update indexes of this interval and add it if it still exists
				newStartIndex += intersectionSize;
				initialStartIndex += intersectionSize;
				if (newEndIndex >= newStartIndex)
				{
					intervalSequenceModifier.addOneMoreIntervalAfter(this);
					unchangedIndexes += getUnchangedIndexesCount();
				}
			}
			else if (intersectionEnd == newEndIndex)
			{
				// last part of this unchanged interval is gone (but first part remains, otherwise it would have entered previous if)
				int oldIinitialEndIndex = initialEndIndex;

				// update indexes of this interval and remove it if it no longer exists
				newEndIndex -= intersectionSize;
				initialEndIndex -= intersectionSize;
				unchangedIndexes = getUnchangedIndexesCount();

				if (isPartialChange)
				{
					// if it's a partial change add it after current unchanged interval
					newInterval = new PartiallyUnchangedInterval(oldIinitialEndIndex - intersectionSize + 1, oldIinitialEndIndex, intersectionStart,
						intersectionEnd, changeOperation.columnName); // intersectionSize should always be 1 here actually as partial change viewport operations are only supported for 1 row
					intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
					unchangedIndexes += newInterval.getUnchangedIndexesCount();
				}
			}
			else
			{
				// update happened in the middle of this interval; we have to split into multiple unchanged intervals
				int oldInitialEndIndex = initialEndIndex;
				int oldNewEndIndex = newEndIndex;

				// update indexes of this interval to match the first resulting interval (the one before intersection)
				initialEndIndex -= newEndIndex - intersectionStart + 1; // for example intersection [5 -> 7], end index = 10, initialEndIndex 8 => initialEndIndex must decrease to 2
				newEndIndex = intersectionStart - 1;
				unchangedIndexes = getUnchangedIndexesCount();

				if (isPartialChange)
				{
					// if it's a partial change add it after current unchanged interval
					newInterval = new PartiallyUnchangedInterval(initialEndIndex + 1, initialEndIndex + intersectionSize, intersectionStart, intersectionEnd,
						changeOperation.columnName); // intersectionSize should always be 1 here actually as partial change viewport operations are only supported for 1 row
					intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
					unchangedIndexes += newInterval.getUnchangedIndexesCount();
				}

				// create the new unchanged interval for after the intersection
				newInterval = new UnchangedInterval(initialEndIndex + intersectionSize + 1, oldInitialEndIndex, intersectionEnd + 1, oldNewEndIndex);
				intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
				unchangedIndexes += newInterval.getUnchangedIndexesCount();
			}
		}
		else
		{
			// else it does not affect at all this unchanged interval
			unchangedIndexes = getUnchangedIndexesCount();
		}

		return unchangedIndexes;
	}

	protected int applyInsert(ViewportOperation insertOperation, IntervalSequenceModifier intervalSequenceModifier)
	{
		// insert can happen before, in the middle of or at the end of this unchanged interval
		int unchangedIndexes;

		if (insertOperation.startIndex <= newStartIndex)
		{
			// insert happened before; this interval just needs shifting
			int insertSize = insertOperation.endIndex - insertOperation.startIndex + 1;
			newStartIndex += insertSize;
			newEndIndex += insertSize;

			unchangedIndexes = getUnchangedIndexesCount();
		}
		else if (insertOperation.startIndex <= newEndIndex)
		{
			// the insert splits current interval
			int oldNewEndIndex = newEndIndex;
			int oldInitialEndIndex = initialEndIndex;

			int insertSize = insertOperation.endIndex - insertOperation.startIndex + 1;
			newEndIndex = insertOperation.startIndex - 1;
			initialEndIndex = initialStartIndex + newEndIndex - newStartIndex;
			unchangedIndexes = getUnchangedIndexesCount();

			UnchangedInterval newInterval = new UnchangedInterval(initialEndIndex + 1, oldInitialEndIndex, insertOperation.endIndex + 1,
				oldNewEndIndex + insertSize);
			intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
			unchangedIndexes += newInterval.getUnchangedIndexesCount();
		}
		else
		{
			// else it does not affect at all this unchanged interval
			unchangedIndexes = getUnchangedIndexesCount();
		}

		return unchangedIndexes;
	}

	protected int applyDelete(ViewportOperation deleteOperation, IntervalSequenceModifier intervalSequenceModifier)
	{
		// delete can happen before, around, in the middle of or at the end of this unchanged interval
		int intersectionStart = Math.max(deleteOperation.startIndex, newStartIndex);
		int intersectionEnd = Math.min(deleteOperation.endIndex, newEndIndex);
		int intersectionSize = intersectionEnd - intersectionStart + 1;
		int unchangedIndexes;

		if (deleteOperation.endIndex < newStartIndex)
		{
			// delete happened before; this interval just needs shifting
			int deleteSize = deleteOperation.endIndex - deleteOperation.startIndex + 1;
			newStartIndex -= deleteSize;
			newEndIndex -= deleteSize;

			unchangedIndexes = getUnchangedIndexesCount();
		}
		else if (intersectionSize > 0)
		{
			// the delete deletes something from this interval; delete and interval overlap
			if (intersectionStart == newStartIndex)
			{
				// first part of interval was deleted (or full interval was deleted); adjust indexes and remove interval if necessary
				newEndIndex -= intersectionEnd - deleteOperation.startIndex + 1;
				newStartIndex = deleteOperation.startIndex;
				initialStartIndex += intersectionSize;
				if (newEndIndex < newStartIndex)
				{
					intervalSequenceModifier.discardCurrentInterval();
					unchangedIndexes = 0;
				}
				else unchangedIndexes = getUnchangedIndexesCount();
			}
			else if (intersectionEnd == newEndIndex)
			{
				// last part of interval was deleted; adjust indexes; the whole interval will not be deleted here because then it would have been treated in the if above
				newEndIndex -= intersectionSize;
				initialEndIndex -= intersectionSize;
				unchangedIndexes = getUnchangedIndexesCount();
			}
			else
			{
				int oldInitialEndIndex = initialEndIndex;
				int oldNewEndIndex = newEndIndex;

				// delete is somewhere inside the interval, so this interval needs to be split in two
				newEndIndex = intersectionStart - 1;
				initialEndIndex = initialStartIndex + newEndIndex - newStartIndex;
				unchangedIndexes = getUnchangedIndexesCount();

				UnchangedInterval newInterval = new UnchangedInterval(initialEndIndex + intersectionSize + 1, oldInitialEndIndex, intersectionStart,
					oldNewEndIndex - intersectionSize);
				intervalSequenceModifier.addOneMoreIntervalAfter(newInterval);
				unchangedIndexes += newInterval.getUnchangedIndexesCount();
			}
		}
		else
		{
			// else it does not affect at all this unchanged interval
			unchangedIndexes = getUnchangedIndexesCount();
		}

		return unchangedIndexes;
	}

	/**
	 * Generates an equivalent set of viewport operations (if any is needed) for this 'unchanged' interval.
	 *
	 * @param equivalentSequenceOfOperations this call will add (if needed) to the equivalentSequenceOfOperations list what is needed to treat the indexes
	 * from this interval (+ this interval in case of {@link PartiallyUnchangedInterval}s).
	 */
	public void appendEquivalentViewportOperations(List<ViewportOperation> equivalentSequenceOfOperations)
	{
		// nothing to add here; this is just for partially changed intervals
	}

	public int getInitialStart()
	{
		return initialStartIndex;
	}

	public int getInitialEnd()
	{
		return initialEndIndex;
	}

	public int getNewStart()
	{
		return newStartIndex;
	}

	public int getNewEnd()
	{
		return newEndIndex;
	}

	public int getUnchangedIndexesCount()
	{
		return initialEndIndex - initialStartIndex + 1;
	}

	@Override
	public String toString()
	{
		return "UnchangedInterval [initialStartIndex=" + initialStartIndex + ", initialEndIndex=" + initialEndIndex + ", newStartIndex=" + newStartIndex +
			", newEndIndex=" + newEndIndex + "]";
	}

}
