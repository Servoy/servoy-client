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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * This class receives a sequence of foundset change events (inserts/deletes/updates) and processes them
 * so that in the end it can provide an equivalent sequence of events (not necessarily the initial ones)
 * that would have the same result as the received ones but who's indexes always reflect the final (after-
 * the-fact) viewport indexes.<br/><br/>
 *
 * That helps in that less changes might be sent to client/browser and that the client component, when
 * receiving them can rely on indexes from the already processed foundset.<br/><br/>
 *
 * <b>For example:<br/>
 * <pre>
 *   Initial viewport: 1 2 3 4 5
 *   Operations: update index 4 (with 5u), insert 2 (a and b) at index 1, delete index 0
 *   => Final viewport: a b 2 3 4 5u
 * </pre>
 *
 * Above you can see that first operation (update index 4) does not reflect the index from the final viewport.<br/>
 * Same goes for the insert (indexes are not the ones from final viewport).<br/>
 * So this class outputs a client-component friendly set of operations that are equivalent to the one above:<br/>
 *
 * <pre>
 *   Equivalent operations: insert 1 at index 0, update index 1, update index 5
 * </pre>
 *
 * which results in the same final viewport, but where the component can rely on updating the data from
 * indexes 1 and 5 after inserting at idx 0 in the viewport. So the component can take these operations one
 * by one and correct the UI using the data from those indexes in the final viewport which are correct.<br/><br/>
 *
 * The algorithm is based on keeping after each operation a list of 'untouched'/'partially touched' intervals (values that have not
 * changed in the viewport or changed only partially, with initial bounds and new bounds) and when the equivalent sequence is asked for,
 * compute it based on this set of unchanged intervals.<br/><br/>
 *
 * <b>In the example above, the unchanged intervals</b> would work like this (in round brackets are the unchanged indexes that correspond to the initial viewport):<br/>
 *
 * <pre>
 *   Initially: [0, 4]
 *   After update index 4    => [0(0), 3(3)]
 *   After insert 2 at idx 1 => [0(0), 0(0)] [3(1), 5(3)]
 *   After delete idx 0      => [3(1), 5(3)]
 * </pre>
 *
 * Then each gap between these SORTED unchanged intervals can be translated into one insert or delete as needed
 * + one update as needed (or one delete as needed followed by one insert as needed). The old and new indexes
 * of each unchanged interval have all the information that is needed to generate the equivalent sequence of
 * operations.
 *
 * @author acostescu
 * @see UnchangedInterval
 */
@SuppressWarnings("nls")
public class ViewportChangeKeeper
{

	private final LinkedList<UnchangedInterval> unchangedIntervals = new LinkedList<>(); // this list will/should remain SORTED all the time (due to code that handles how operations are applied to it)
	private int initialViewportEnd;
	private int currentViewportEnd;
	private int viewportStart;
	private int numberOfUnchangedRows; // number of indexes who's data is not affected by any of the viewport operations; we monitor these because if there are too few of them it is more efficient to send the whole viewport then lots of changes

	public ViewportChangeKeeper()
	{
	}

	public void reset(int newViewportStart, int newViewportEnd)
	{
		unchangedIntervals.clear();
		UnchangedInterval wholeUnchangedInterval = new UnchangedInterval(newViewportStart, newViewportEnd, newViewportStart, newViewportEnd);
		unchangedIntervals.add(wholeUnchangedInterval);

		numberOfUnchangedRows = wholeUnchangedInterval.getUnchangedIndexesCount();

		this.viewportStart = newViewportStart;
		this.initialViewportEnd = this.currentViewportEnd = newViewportEnd;
	}

	public boolean hasChanges()
	{
		// if the number of unchanged rows is not identical to initial viewport size then we have changes
		return initialViewportEnd != currentViewportEnd || (numberOfUnchangedRows != initialViewportEnd - viewportStart + 1);
	}

	public void processOperation(ViewportOperation operation)
	{
		if (operation.startIndex < viewportStart || operation.startIndex > currentViewportEnd + 1 ||
			(operation.endIndex > currentViewportEnd && operation.type != ViewportOperation.INSERT))
			throw new IllegalArgumentException(
				"Cannot process operation outside of viewport bounds (" + viewportStart + ", " + currentViewportEnd + "): " + operation);

		// update unchanged intervals
		ListIterator<UnchangedInterval> listIterator = unchangedIntervals.listIterator();
		IntervalSequenceModifier modifier = new IntervalSequenceModifier(listIterator);
		numberOfUnchangedRows = 0;
		while (listIterator.hasNext())
		{
			numberOfUnchangedRows += listIterator.next().applyOperation(operation, modifier);
		}

		// adjust size
		if (operation.type == ViewportOperation.DELETE) currentViewportEnd -= operation.endIndex - operation.startIndex + 1;
		else if (operation.type == ViewportOperation.INSERT) currentViewportEnd += operation.endIndex - operation.startIndex + 1;
	}

	public ViewportOperation[] getEquivalentSequenceOfOperations()
	{
		List<ViewportOperation> equivalentOps = new ArrayList<>(unchangedIntervals.size() * 2 + 4);

		// fake initial unchanged interval at idx -1 to handle correctly any space before first unchanged interval in the list
		UnchangedInterval previousUnchangedInterval = new UnchangedInterval(viewportStart - 1, viewportStart - 1, viewportStart - 1, viewportStart - 1);

		for (UnchangedInterval ui : unchangedIntervals)
		{
			appendEquivalentViewportOperations(previousUnchangedInterval, ui, equivalentOps);
			previousUnchangedInterval = ui;
		}

		// handle space after last unchanged interval
		appendEquivalentViewportOperations(previousUnchangedInterval,
			new UnchangedInterval(initialViewportEnd + 1, initialViewportEnd + 1, currentViewportEnd + 1, currentViewportEnd + 1), equivalentOps);

		return equivalentOps.toArray(new ViewportOperation[equivalentOps.size()]);
	}

	/**
	 * Generates an equivalent set of viewport operations based on the previous unchanged interval and this interval.
	 *
	 * @param previousUnchangedInterval the unchanged interval that precedes this one (lower indexes).
	 * @param equivalentSequenceOfOperations the space between two unchanged intervals can always be made equivalent to an insert, delete, insert + update, delete + update,
	 * delete + insert (we do not use this approach) or just an update. This call will add to the equivalentSequenceOfOperations list what is needed to treat the space between
	 * previousUnchangedInterval and this interval (+ this interval in case of {@link PartiallyUnchangedInterval}s).
	 */
	protected void appendEquivalentViewportOperations(UnchangedInterval previousUnchangedInterval, UnchangedInterval currentUnchangedInterval,
		List<ViewportOperation> equivalentSequenceOfOperations)
	{
		int changedCountInBetweenDelta = (currentUnchangedInterval.getInitialStart() - previousUnchangedInterval.getInitialEnd()) -
			(currentUnchangedInterval.getNewStart() - previousUnchangedInterval.getNewEnd());
		int numberOfRowsToUpdate; // number of rows for which an update will be generated

		if (changedCountInBetweenDelta > 0)
		{
			// new changed rows are less then previous rows => generate a delete OP
			equivalentSequenceOfOperations.add(new ViewportOperation(previousUnchangedInterval.getNewEnd() + 1,
				previousUnchangedInterval.getNewEnd() + changedCountInBetweenDelta, ViewportOperation.DELETE));
			numberOfRowsToUpdate = currentUnchangedInterval.getNewStart() - previousUnchangedInterval.getNewEnd() - 1;
		}
		else if (changedCountInBetweenDelta < 0)
		{
			// new changed rows are more then previous rows => generate an insert OP
			equivalentSequenceOfOperations.add(new ViewportOperation(previousUnchangedInterval.getNewEnd() + 1,
				previousUnchangedInterval.getNewEnd() - changedCountInBetweenDelta, ViewportOperation.INSERT));
			numberOfRowsToUpdate = currentUnchangedInterval.getInitialStart() - previousUnchangedInterval.getInitialEnd() - 1;
		}
		else numberOfRowsToUpdate = currentUnchangedInterval.getInitialStart() - previousUnchangedInterval.getInitialEnd() - 1; // it would be the same here using 'new' indexes

		if (numberOfRowsToUpdate > 0)
		{
			equivalentSequenceOfOperations.add(new ViewportOperation(currentUnchangedInterval.getNewStart() - numberOfRowsToUpdate,
				currentUnchangedInterval.getNewStart() - 1, ViewportOperation.CHANGE));
		}

		// partially changed intervals might want to add some operations of their own
		currentUnchangedInterval.appendEquivalentViewportOperations(equivalentSequenceOfOperations);
	}

	/**
	 * This is useful to decide if it's worth sending the equivalent ViewportOperations or so much has changed that it's better to send the whole viewport...
	 * @return the number of unchanged rows in the viewport.
	 */
	public int getNumberOfUnchangedRows()
	{
		return numberOfUnchangedRows;
	}

	/**
	 * Just something to give to the UnchangedInterval class to allow it to remove itself or split itself into two when handling an operation.
	 */
	protected static class IntervalSequenceModifier
	{

		private final ListIterator<UnchangedInterval> listIterator;

		private IntervalSequenceModifier(ListIterator<UnchangedInterval> listIterator)
		{
			this.listIterator = listIterator;
		}

		protected void discardCurrentInterval()
		{
			listIterator.remove();
		}

		protected void addOneMoreIntervalAfter(UnchangedInterval unchangedInterval)
		{
			listIterator.add(unchangedInterval);
		}

	}

}
