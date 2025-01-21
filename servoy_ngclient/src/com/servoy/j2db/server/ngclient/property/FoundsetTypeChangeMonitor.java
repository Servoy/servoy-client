/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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
import java.util.List;
import java.util.Set;

import org.sablo.IChangeListener;
import org.sablo.specification.property.ArrayOperation;

import com.servoy.j2db.dataprocessing.FireCollector;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.util.Pair;

/**
 * This class is responsible for keeping track of what changes need to be sent to the client (whole thing, selection changes, viewport idx/size change, row data changes...)
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetTypeChangeMonitor
{

	/**
	 * The whole foundset property needs to get sent to the client.
	 */
	protected static final int SEND_ALL = 0b000000001;
	/**
	 * Only the bounds of the viewPort changed, data is the same; for example records were added/removed before startIndex of viewPort,
	 * or even inside the viewPort but will be combined by incremental updates (adds/deletes).
	 */
	protected static final int SEND_VIEWPORT_BOUNDS = 0b000000010;

	protected static final int SEND_FOUNDSET_SORT = 0b000000100;
	/**
	 * Foundset size changed (add/remove of records).
	 */
	protected static final int SEND_FOUNDSET_SIZE = 0b000001000;

	protected static final int SEND_SELECTED_INDEXES = 0b000010000;

	// 0b000100000;
	protected static final int SEND_MULTISELECT = 0b001000000;

	protected static final int SEND_COLUMN_FORMATS = 0b010000000;

	protected static final int SEND_HAD_MORE_ROWS = 0b100000000;

	// 0b1000000000 used to be PUSH_TO_SERVER that is now no longer needed/used;

	protected static final int SEND_USER_SET_SELECTION = 0b10000000000;

	protected static final int SEND_FOUNDSET_ID = 0b100000000000;

	protected static final int SEND_FOUNDSET_DEFINITION_CHANGE = 0b1000000000000;

	protected boolean lastHadMoreRecords = false;

	protected IChangeListener changeNotifier;

	protected int changeFlags = 0;
	protected List<Pair<Integer, Boolean>> handledRequestIds = new ArrayList<>();
	protected final FoundsetTypeViewportDataChangeMonitor viewPortDataChangeMonitor;
	private final List<ViewportDataChangeMonitor< ? >> viewPortDataChangeMonitors = new ArrayList<>();

	protected final FoundsetTypeSabloValue propertyValue; // TODO when we implement merging foundset events based on indexes, data will no longer be needed and this member can be removed

	public FoundsetTypeChangeMonitor(FoundsetTypeSabloValue propertyValue, FoundsetTypeRowDataProvider rowDataProvider)
	{
		this.propertyValue = propertyValue;
		viewPortDataChangeMonitor = new FoundsetTypeViewportDataChangeMonitor(new IChangeListener()
		{
			@Override
			public void valueChanged()
			{
				notifyChange();
			}
		}, rowDataProvider, propertyValue);
		addViewportDataChangeMonitor(viewPortDataChangeMonitor);
	}

	/**
	 * A client request (change selection, load extra records...) was handled on server either successfully or not. But the client
	 * still needs to know about this in case a client side promise is still waiting to be resolved/cancelled for that action.
	 *
	 * We keep these all the time, even if the whole viewport is resent because this info needs to go to client anyway.
	 *
	 * @param reqId the requestId that we got from the client for executing an action
	 * @param success wether that action was handled successfully or with failures.
	 */
	public void requestIdHandled(int reqId, boolean success)
	{
		handledRequestIds.add(new Pair<>(Integer.valueOf(reqId), Boolean.valueOf(success)));
		if (handledRequestIds.size() == 1) notifyChange();
	}

	/**
	 * Called when the foundSet selection needs to be re-sent to client.
	 */
	public void selectionChanged(boolean userSetSelection)
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_SELECTED_INDEXES;
			if (userSetSelection)
			{
				changeFlags = changeFlags | SEND_USER_SET_SELECTION;
			}
			if (oldChangeFlags != changeFlags) notifyChange();
		}
		propertyValue.setDataAdapterListToSelectedRecord();
	}

	public void multiSelectChanged()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_MULTISELECT;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}

	/**
	 * The foundset's size changed.
	 * This doesn't notify changes as this is probably part of a larger check which could result in more changes. Notification must be handled by caller.
	 */
	protected void foundSetSizeChanged()
	{
		if (!shouldSendAll())
		{
			changeFlags = changeFlags | SEND_FOUNDSET_SIZE;
		}
		checkHadMoreRows(false);
	}

	protected void foundsetSortChanged()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_FOUNDSET_SORT;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}

	/**
	 * Called when the viewPort bounds need to be re-sent to client.<br/>
	 * Only the bounds of the viewPort changed, data is the same; for example records were added/removed before startIndex of viewPort.<br/><br/>
	 *
	 * This doesn't notify changes as this is probably part of a larger check which could result in more changes. Notification must be handled by caller.
	 */
	protected void viewPortBoundsOnlyChanged()
	{
		if (!shouldSendWholeViewPort() && !shouldSendAll())
		{
			changeFlags = changeFlags | SEND_VIEWPORT_BOUNDS;
		}
	}

	/**
	 * Called when viewPort bounds and data changed; for example client requested completely new viewport bounds.
	 */
	public void viewPortCompletelyChanged()
	{
		boolean changed = !viewPortDataChangeMonitor.shouldSendWholeViewport();
		for (ViewportDataChangeMonitor< ? > vdcm : viewPortDataChangeMonitors)
		{
			// we don't need to use callAllViewportDataChangeMonitorsWrappedInFireCollector here as viewPortCompletelyChanged will not get data from the record/possibly triggering calculations
			vdcm.viewPortCompletelyChanged();
		}
		if (!shouldSendAll() && changed)
		{
			// clear all more granular changes as whole viewport will be sent
			changeFlags = changeFlags & (~SEND_VIEWPORT_BOUNDS); // clear flag
			notifyChange();
		}
	}

	/**
	 * Called when all foundset info needs to be resent to client.
	 */
	public void allChanged()
	{
		int oldChangeFlags = changeFlags;
		changeFlags = SEND_ALL; // clears all others as well
		for (ViewportDataChangeMonitor< ? > vdcm : viewPortDataChangeMonitors)
		{
			// we don't need to use callAllViewportDataChangeMonitorsWrappedInFireCollector here as viewPortCompletelyChanged will not get data from the record/possibly triggering calculations
			vdcm.viewPortCompletelyChanged();
		}
		if (oldChangeFlags != changeFlags)
		{
			notifyChange();
		}
	}

	/**
	 * Called for example when the used foundset instance changes (for example due to use of related foundset).
	 * In that case viewPort is set to 0, 0 (so that might have already triggered a notification), but also the server size can change then.
	 */
	public void newFoundsetSize()
	{
		int oldChangeFlags = changeFlags;
		foundSetSizeChanged();
		if (oldChangeFlags != changeFlags) notifyChange();
	}

	/**
	 * Called when the find mode changes on this foundset.
	 */
	public void findModeChanged(boolean newFindMode)
	{
		allChanged();
		if (propertyValue.getDataAdapterList() != null) propertyValue.getDataAdapterList().setFindMode(newFindMode);
	}

	public void shrinkClientViewport(final int relativeFirstRowToOldViewport, final int relativeLastRowToOldViewport, int oldSize)
	{
		if (!shouldSendAll() && !shouldSendWholeViewPort() && relativeFirstRowToOldViewport <= relativeLastRowToOldViewport)
		{
			callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
			{
				@Override
				public void run(ViewportDataChangeMonitor< ? > vpdcm)
				{
					vpdcm.queueOperation(relativeFirstRowToOldViewport, relativeLastRowToOldViewport, oldSize, ArrayOperation.DELETE);
				}
			});
		}
	}

	private interface IVPDCMRunnable
	{
		void run(ViewportDataChangeMonitor< ? > vpdcm);
	}

	private void callAllViewportDataChangeMonitorsWrappedInFireCollector(IVPDCMRunnable r)
	{
		// as our goal here is to write contents of all these rows to JSON without triggering calculations that end up triggering data-change-related solution handlers that might in
		// turn change data/bounds of data that we are trying to write to JSON, we use fire collector; after we are done writing, any such handlers will be called
		// and if they alter anything in the foundset, the foundset/other listeners will pick that up and generate a new change to be written to JSON...
		FireCollector fireCollector = FireCollector.getFireCollector();
		try
		{
			for (ViewportDataChangeMonitor< ? > vpdcm : viewPortDataChangeMonitors)
			{
				r.run(vpdcm);
			}
		}
		finally
		{
			fireCollector.done();
		}
	}

	public void recordsDeleted(int firstDeletedRowInFoundset, int lastDeletedRowInFoundset, final FoundsetTypeViewport viewPort)
	{
		int oldChangeFlags = changeFlags;
		if (lastDeletedRowInFoundset - firstDeletedRowInFoundset >= 0) foundSetSizeChanged();

		int pkChunkSize = ((FoundSetManager)propertyValue.getApplication().getFoundSetManager()).config.pkChunkSize();
		if (firstDeletedRowInFoundset == pkChunkSize &&
			propertyValue.getFoundset().getSize() == pkChunkSize &&
			(viewPort.getStartIndex() + viewPort.getSize() - 1 <= lastDeletedRowInFoundset))
		{
			// try to determine when foundset is changed by loadbyquery/loadallrecords, in this case we should not load the whole foundset based on old viewport data, we should reset the viewport to default values
			viewPort.setPreferredViewportBounds();
		}
		else
		{
			if (!shouldSendAll() && !shouldSendWholeViewPort())
			{
				int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;

				int slideBy;
				if (firstDeletedRowInFoundset < viewPort.getStartIndex())
				{
					// this will adjust the viewPort startIndex (and size if needed)
					slideBy = firstDeletedRowInFoundset - Math.min(viewPort.getStartIndex(), lastDeletedRowInFoundset + 1);
				}
				else
				{
					// this will adjust the viewPort size if needed (not enough records to insert in the viewPort to replace deleted ones)
					slideBy = 0;
				}

				if (belongsToInterval(firstDeletedRowInFoundset, viewPort.getStartIndex(), viewPortEndIdx) ||
					belongsToInterval(lastDeletedRowInFoundset, viewPort.getStartIndex(), viewPortEndIdx) ||
					belongsToInterval(viewPort.getStartIndex(), firstDeletedRowInFoundset, lastDeletedRowInFoundset))
				{
					// so either part of the viewport was deleted (firstRow or lastRow inside viewport bounds)
					// or whole viewport was deleted ((firstRow or lastRow NOT inside viewport bounds) but viewport startIndex inside deleted bounds)
					// so not only indexes need to be adjusted/checked but also the contents of the viewport...

					int firstRowDeletedInViewport = Math.max(viewPort.getStartIndex(), firstDeletedRowInFoundset);
					int lastRowDeletedInViewport = Math.min(viewPortEndIdx, lastDeletedRowInFoundset);
					final int relativeFirstRow = firstRowDeletedInViewport - viewPort.getStartIndex();
					// number of deletes from current viewPort
					final int relativeLastRow = lastRowDeletedInViewport - viewPort.getStartIndex();
					final int numberOfDeletes = lastRowDeletedInViewport - firstRowDeletedInViewport + 1;

					// adjust viewPort bounds if necessary
					final int oldViewPortSize = viewPort.getSize();

					viewPort.slideAndCorrect(slideBy);
					viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1; // update
					final int insertedStartDueToSlidingInRelative = oldViewPortSize - numberOfDeletes;
					final int insertedEndDueToSlidingInRelative = viewPortEndIdx - viewPort.getStartIndex();

					// add new records if available
					// we need to replace same amount of records in current viewPort; append rows if available
					callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
					{
						public void run(ViewportDataChangeMonitor< ? > vpdcm)
						{
							vpdcm.queueOperation(relativeFirstRow, relativeLastRow, oldViewPortSize, ArrayOperation.DELETE);

							// see if records slided automatically into the viewport due to delete; generate an INSERT for those
							if (insertedStartDueToSlidingInRelative <= insertedEndDueToSlidingInRelative)
								vpdcm.queueOperation(insertedStartDueToSlidingInRelative, insertedEndDueToSlidingInRelative, oldViewPortSize,
									ArrayOperation.INSERT);
						}
					});
				}
				else if (slideBy != 0)
				{
					viewPort.slideAndCorrect(slideBy);
				}
			} // else maybe client requested a loadRecords with specific bounds before this insert notification arrived and we should honor those viewport bounds, not alter them automatically
			else if (viewPort.getStartIndex() + viewPort.getSize() > propertyValue.getFoundset().getSize())
			{
				// if it will already send the whole viewport, the size still needs to be in sync with the foundset
				viewPort.correctAndSetViewportBoundsInternal(viewPort.getStartIndex(), viewPort.getSize());
			}
		}
		if (oldChangeFlags != changeFlags) notifyChange();
	}

	public void extendClientViewport(final int firstRow, int lastRow, int oldSize, final FoundsetTypeViewport viewPort)
	{
		if (!shouldSendAll() && !shouldSendWholeViewPort())
		{
			int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;
			final int lastViewPortInsert = Math.min(lastRow, viewPortEndIdx); // actually this should always be lastRow as we assume given [firstRow, lastRow] are first part or last part of viewPort (viewport bounds were already adjusted by caller)

			// add records that were inserted in viewPort
			callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
			{
				public void run(ViewportDataChangeMonitor< ? > vpdcm)
				{
					vpdcm.queueOperation(firstRow - viewPort.getStartIndex(), lastViewPortInsert - viewPort.getStartIndex(), oldSize, ArrayOperation.INSERT);
				}
			});
		}
	}

	/**
	 * Deals with new records being inserted into the foundset.
	 * @param firstRow the first row of the insertion.
	 * @param lastRow the last row of the insertion.
	 * @param viewPort the current viewPort.
	 */
	public void recordsInserted(final int firstInsertedRowInFoundset, int lastInsertedRowInFoundset, int oldViewportSize, final FoundsetTypeViewport viewPort)
	{
		int oldChangeFlags = changeFlags;
		if (lastInsertedRowInFoundset - firstInsertedRowInFoundset >= 0) foundSetSizeChanged();

		if (oldViewportSize == 0)
		{
			// reset to the preferred viewport size if that is set
			viewPort.setPreferredViewportBounds();
		}
		else
		{
			int nrNewRecords = lastInsertedRowInFoundset - firstInsertedRowInFoundset + 1;
			int foundsetSize = viewPort.foundset.getSize();

			// if the size of the viewport is still smaller then the preferredViewPortSize
			// and the foundset size allows for bigger viewport then size then update the bounds so that it
			// adds the extra wanted records at the end
			if (viewPort.size < viewPort.preferredViewPortSize && (foundsetSize - viewPort.startIndex) > viewPort.size &&
				(foundsetSize - nrNewRecords) == (viewPort.startIndex + viewPort.size))
			{
				int oldStartIndex = viewPort.startIndex;
				int newSize = Math.min(viewPort.preferredViewPortSize, (foundsetSize - viewPort.startIndex));

				int insertStart = Math.max(viewPort.startIndex, firstInsertedRowInFoundset);
				int insertEnd = Math.min(viewPort.startIndex + newSize - 1, lastInsertedRowInFoundset);

				if (insertEnd >= insertStart && ((insertEnd - insertStart + 1) == (newSize - oldViewportSize)))
				{
					viewPort.correctAndSetViewportBoundsInternal(viewPort.startIndex, newSize);
					if (oldStartIndex != viewPort.startIndex || oldViewportSize != viewPort.size) viewPortBoundsOnlyChanged();
					// ONLY viewPortBoundsOnlyChanged() here, not full viewport changed, because, even though viewport data was added, a
					// granular insert op. for, the new data in the viewport is generated in code below - vpdcm.queueOperation..., or
					// if it is already marked to send all or send whole viewport, the data and bounds will be sent due to that anyway
				}
			}

			if (!shouldSendAll() && !shouldSendWholeViewPort())
			{
				int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;
				if (viewPort.getStartIndex() <= firstInsertedRowInFoundset && firstInsertedRowInFoundset <= viewPortEndIdx)
				{
					final int lastViewPortInsert = Math.min(lastInsertedRowInFoundset, viewPortEndIdx);
					// add records that were inserted in viewPort
					callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
					{
						public void run(ViewportDataChangeMonitor< ? > vpdcm)
						{
							vpdcm.queueOperation(firstInsertedRowInFoundset - viewPort.getStartIndex(), lastViewPortInsert - viewPort.getStartIndex(),
								oldViewportSize,
								ArrayOperation.INSERT);

							// for insert operations that will push some rows out of the viewport we need to generate a delete operation as well (so that viewport data is correct afterwards)
							int rowsInsertedInViewport = lastViewPortInsert - firstInsertedRowInFoundset + 1;
							int rowsThatSlidedOutOfViewport = oldViewportSize + rowsInsertedInViewport - viewPort.getSize();
							if (rowsThatSlidedOutOfViewport > 0)
								vpdcm.queueOperation(viewPort.getSize(), viewPort.getSize() + rowsThatSlidedOutOfViewport - 1, oldViewportSize,
									ArrayOperation.DELETE);
						}
					});
				}
				else if (viewPort.getStartIndex() > firstInsertedRowInFoundset)
				{
					viewPort.slideAndCorrect(lastInsertedRowInFoundset - firstInsertedRowInFoundset + 1);
				}
			} // else maybe client requested a loadRecords with specific bounds before this insert notification arrived and we should honor those viewport bounds, not alter them automatically
				// an insert should not be able to make viewport have invalid bounds, so no further corrections needed to viewport bounds
		}

		if (oldChangeFlags != changeFlags) notifyChange();
	}

	public void recordsUpdated(int firstRow, int lastRow, int foundSetSize, final FoundsetTypeViewport viewPort, final Set<String> dataproviders)
	{
		if (propertyValue.getDataAdapterList() != null && propertyValue.getDataAdapterList().isQuietRecordChangeInProgress()) return;

		if (firstRow == 0 && lastRow == foundSetSize - 1)
		{
			// calculation field change
			if (dataproviders != null && dataproviders.size() > 0)
			{
				int oldChangeFlags = changeFlags;
				if (!shouldSendAll() && !shouldSendWholeViewPort())
				{
					// get the rows that are changed.
					final int firstViewPortIndex = Math.max(viewPort.getStartIndex(), firstRow);
					final int lastViewPortIndex = Math.min(viewPort.getStartIndex() + viewPort.getSize() - 1, lastRow);
					if (firstViewPortIndex <= lastViewPortIndex) callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
					{
						public void run(ViewportDataChangeMonitor< ? > vpdcm)
						{
							for (String dataprovider : dataproviders)
							{
								vpdcm.queueCellChangeDueToColumn(firstViewPortIndex - viewPort.getStartIndex(), viewPort.getSize(), dataprovider);
							}
						}
					});
				}
				if (oldChangeFlags != changeFlags) notifyChange();
			}
			else if (viewPort.getSize() > 0) viewPortCompletelyChanged();
		}
		else
		{
			int oldChangeFlags = changeFlags;
			if (!shouldSendAll() && !shouldSendWholeViewPort())
			{
				// get the rows that are changed.
				final int firstViewPortIndex = Math.max(viewPort.getStartIndex(), firstRow);
				final int lastViewPortIndex = Math.min(viewPort.getStartIndex() + viewPort.getSize() - 1, lastRow);
				if (firstViewPortIndex <= lastViewPortIndex)
				{
					callAllViewportDataChangeMonitorsWrappedInFireCollector(new IVPDCMRunnable()
					{
						public void run(ViewportDataChangeMonitor< ? > vpdcm)
						{
							if (firstViewPortIndex == lastViewPortIndex && dataproviders != null && dataproviders.size() > 0)
							{
								for (String dataprovider : dataproviders)
								{
									vpdcm.queueCellChangeDueToColumn(firstViewPortIndex - viewPort.getStartIndex(), viewPort.getSize(), dataprovider);
								}
							}
							else
							{
								vpdcm.queueOperation(firstViewPortIndex - viewPort.getStartIndex(), lastViewPortIndex - viewPort.getStartIndex(),
									viewPort.getSize(), ArrayOperation.CHANGE);
							}
						}
					});
				}
			}
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}

	protected boolean belongsToInterval(int x, int intervalStartInclusive, int intervalEndInclusive)
	{
		return intervalStartInclusive <= x && x <= intervalEndInclusive;
	}

	public boolean shouldSendAll()
	{
		return (changeFlags & SEND_ALL) != 0;
	}

	public boolean shouldSendSelectedIndexes()
	{
		return (changeFlags & SEND_SELECTED_INDEXES) != 0;
	}

	public boolean shouldSendUserSetSelection()
	{
		return (changeFlags & SEND_USER_SET_SELECTION) != 0;
	}

	public List<Pair<Integer, Boolean>> getHandledRequestIds()
	{
		return handledRequestIds;
	}

	public boolean shouldSendFoundsetSize()
	{
		return (changeFlags & SEND_FOUNDSET_SIZE) != 0;
	}

	public boolean shouldSendFoundsetSort()
	{
		return (changeFlags & SEND_FOUNDSET_SORT) != 0;
	}

	public boolean shouldSendHadMoreRows()
	{
		return (changeFlags & SEND_HAD_MORE_ROWS) != 0;
	}

	public boolean shouldSendMultiSelect()
	{
		return (changeFlags & SEND_MULTISELECT) != 0;
	}

	public boolean shouldSendFoundsetID()
	{
		return (changeFlags & SEND_FOUNDSET_ID) != 0;
	}

	public boolean shouldSendFoundsetDefinitionChange()
	{
		return (changeFlags & SEND_FOUNDSET_DEFINITION_CHANGE) != 0;
	}

	public boolean shouldSendColumnFormats()
	{
		return (changeFlags & SEND_COLUMN_FORMATS) != 0;
	}

	public boolean shouldSendViewPortBounds()
	{
		return (changeFlags & SEND_VIEWPORT_BOUNDS) != 0;
	}

	public boolean shouldSendWholeViewPort()
	{
		return viewPortDataChangeMonitor.shouldSendWholeViewport();
	}

	public ArrayOperation[] getViewPortChanges()
	{
		return viewPortDataChangeMonitor.getViewPortChanges();
	}

	public boolean hasViewportChanges()
	{
		return viewPortDataChangeMonitor.hasViewportChanges();
	}

	/**
	 * Registers the change notifier; this notifier is to be used to fire property change notifications.
	 * @param changeNotifier the object that should be notified when this property needs to send updates to client.
	 */
	public void setChangeNotifier(IChangeListener changeNotifier)
	{
		this.changeNotifier = changeNotifier;
		if (hasChanges()) changeNotifier.valueChanged();
	}

	public boolean hasChanges()
	{
		return shouldSendAll() || shouldSendFoundsetSize() || shouldSendFoundsetSort() || shouldSendSelectedIndexes() || shouldSendViewPortBounds() ||
			shouldSendWholeViewPort() || shouldSendColumnFormats() || getHandledRequestIds().size() > 0 || hasViewportChanges();
	}

	public void clearChanges()
	{
		changeFlags = 0;
		viewPortDataChangeMonitor.clearChanges();
		propertyValue.getViewPort().clearSendingInitialPreferredViewport();
		handledRequestIds.clear();
	}

	protected void notifyChange()
	{
		if (changeNotifier != null) changeNotifier.valueChanged();
	}

	public void addViewportDataChangeMonitor(ViewportDataChangeMonitor< ? > viewPortChangeMonitor)
	{
		if (!viewPortDataChangeMonitors.contains(viewPortChangeMonitor))
		{
			viewPortDataChangeMonitors.add(viewPortChangeMonitor);
			if (viewPortChangeMonitor != viewPortDataChangeMonitor) viewPortChangeMonitor.setFoundsetTypeViewportDataChangeMonitor(viewPortDataChangeMonitor);
		}
	}

	public void removeViewportDataChangeMonitor(ViewportDataChangeMonitor< ? > viewPortChangeMonitor)
	{
		viewPortDataChangeMonitors.remove(viewPortChangeMonitor);
	}

	public void columnFormatsUpdated()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_COLUMN_FORMATS;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}

	public void checkHadMoreRows()
	{
		checkHadMoreRows(true);
	}

	private void checkHadMoreRows(boolean doNotifyChange)
	{
		if (propertyValue.getFoundset() != null)
		{
			boolean newHadMoreRows = propertyValue.getFoundset().hadMoreRows();
			boolean changed = (newHadMoreRows != lastHadMoreRecords);
			lastHadMoreRecords = newHadMoreRows;

			if (changed && !shouldSendAll())
			{
				int oldChangeFlags = changeFlags;
				changeFlags = changeFlags | SEND_HAD_MORE_ROWS;
				if (doNotifyChange && (oldChangeFlags != changeFlags)) notifyChange();
			}
		}
	}

	public void foundsetIDChanged()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_FOUNDSET_ID;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}

	public void foundsetDefinitionChanged()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_FOUNDSET_DEFINITION_CHANGE;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
	}


	public boolean isVisible()
	{
		if (propertyValue != null && propertyValue.getDataAdapterList() != null)
		{
			return propertyValue.getDataAdapterList().getForm().isFormVisible();
		}
		return true;
	}

	@Override
	public String toString()
	{
		return "FTCM: " + propertyValue;
	}

//	protected static class RecordChangeDescriptor implements JSONWritable
//	{
//
//		public static enum Types
//		{
//			CHANGE(0), ADD_TO_VIEWPORT(1), REMOVE_FROM_VIEWPORT(2);
//
//			public final int v;
//
//			private Types(int v)
//			{
//				this.v = v;
//			}
//		};
//
//		private final int relativeIndex;
//		private final Types type;
//
//		/**
//		 * @param type one of {@link #CHANGE}, {@link #ADD_TO_VIEWPORT} or {@link #REMOVE_FROM_VIEWPORT}
//		 * @param relativeIndex viewPort relative index of the change.
//		 */
//		public RecordChangeDescriptor(Types type, int relativeIndex)
//		{
//			this.type = type;
//			this.relativeIndex = relativeIndex;
//		}
//
//		public Map<String, Object> toMap()
//		{
//			Map<String, Object> retValue = new HashMap<>();
//			retValue.put("relativeIndex", Integer.valueOf(relativeIndex));
//			retValue.put("type", Integer.valueOf(type.v));
//			return retValue;
//		}
//	}

}
