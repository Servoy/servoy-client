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

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.IToJSONWriter;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.util.Pair;

/**
 * This class is responsible for keeping track of what changes need to be sent to the client (whole thing, selection changes, viewport idx/size change, row data changes...)
 *
 * @author acostescu
 */
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

	protected boolean lastHadMoreRecords = false;

	protected IChangeListener changeNotifier;

	protected int changeFlags = 0;
	protected List<Pair<Integer, Boolean>> handledRequestIds = new ArrayList<>();
	protected final ViewportDataChangeMonitor<FoundsetTypeRowDataProvider> viewPortDataChangeMonitor;
	protected final List<ViewportDataChangeMonitor< ? >> viewPortDataChangeMonitors = new ArrayList<>();

	protected final FoundsetTypeSabloValue propertyValue; // TODO when we implement merging foundset events based on indexes, data will no longer be needed and this member can be removed

	public FoundsetTypeChangeMonitor(FoundsetTypeSabloValue propertyValue, FoundsetTypeRowDataProvider rowDataProvider)
	{
		this.propertyValue = propertyValue;
		viewPortDataChangeMonitor = new ViewportDataChangeMonitor<>(null, rowDataProvider);
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
	public void selectionChanged()
	{
		if (!shouldSendAll())
		{
			int oldChangeFlags = changeFlags;
			changeFlags = changeFlags | SEND_SELECTED_INDEXES;
			if (oldChangeFlags != changeFlags) notifyChange();
		}
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
		for (ViewportDataChangeMonitor vdcm : viewPortDataChangeMonitors)
		{
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
		for (ViewportDataChangeMonitor vdcm : viewPortDataChangeMonitors)
		{
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

	/**
	 * Called when the dataProviders that this foundset type provides changed.
	 */
	public void dataProvidersChanged()
	{
		// this normally happens only before initial send of initial form data so it isn't very useful; will we allow dataProviders to change later on?
		if (propertyValue.viewPort.size > 0) allChanged();
	}

	public void shrinkClientViewport(int relativeFirstRowToOldViewport, int relativeLastRowToOldViewport)
	{
		if (!shouldSendAll() && !shouldSendWholeViewPort() && relativeFirstRowToOldViewport <= relativeLastRowToOldViewport)
		{
			for (ViewportDataChangeMonitor vpdcm : viewPortDataChangeMonitors)
			{
				vpdcm.queueOperation(relativeFirstRowToOldViewport, relativeLastRowToOldViewport, 0, -1, propertyValue.getFoundset(), RowData.DELETE);
			}
			notifyChange();
		}
	}

	public void recordsDeleted(int firstRow, int lastRow, FoundsetTypeViewport viewPort)
	{
		int oldChangeFlags = changeFlags;
		boolean viewPortRecordChangesUpdated = false;

		if (lastRow - firstRow >= 0) foundSetSizeChanged();
		if (!shouldSendAll() && !shouldSendWholeViewPort())
		{
			int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;

			int slideBy;
			if (firstRow < viewPort.getStartIndex())
			{
				// this will adjust the viewPort startIndex (and size if needed)
				slideBy = firstRow - Math.min(viewPort.getStartIndex(), lastRow + 1);
			}
			else
			{
				// this will adjust the viewPort size if needed (not enough records to insert in the viewPort to replace deleted ones)
				slideBy = 0;
			}

			if (belongsToInterval(firstRow, viewPort.getStartIndex(), viewPortEndIdx) || belongsToInterval(lastRow, viewPort.getStartIndex(), viewPortEndIdx))
			{
				// first row to be deleted inside current viewPort
				int firstRowDeletedInViewport = Math.max(viewPort.getStartIndex(), firstRow);
				int lastRowDeletedInViewport = Math.min(viewPortEndIdx, lastRow);
				int relativeFirstRow = firstRowDeletedInViewport - viewPort.getStartIndex();
				// number of deletes from current viewPort
				int relativeLastRow = lastRowDeletedInViewport - viewPort.getStartIndex();
				int numberOfDeletes = lastRowDeletedInViewport - firstRowDeletedInViewport + 1;

				// adjust viewPort bounds if necessary
//				int oldViewPortStart = viewPort.getStartIndex();
				int oldViewPortSize = viewPort.getSize();

				// TODO merge changes with previous ones without keeping any actual data (indexes kept in a way should be enough) - implementation started below
//				// ok, viewPort bounds are updated; update existing recordChange data if needed; we are working here a lot with viewPort relative indexes (both client side and server side ones)
//				ListIterator<RecordChangeDescriptor> iterator = viewPortRecordChanges.listIterator();
//				int browserViewPortIdxDelta = 0; // delta between the current client side viewPort data relative "i" index and the old server viewPort relative "i" index
//				int toBeDeleted = relativeFirstRow;
//				while (iterator.hasNext())
//				{
//					RecordChangeDescriptor recordChange = iterator.next();
//					while (toBeDeleted <= relativeLastRow && toBeDeleted + browserViewPortIdxDelta < recordChange.relativeIndex)
//					{
//						// record deleted before previous Add/Remove/Update operation; add before
//						iterator.add(new RecordChangeDescriptor(RecordChangeDescriptor.Types.REMOVE_FROM_VIEWPORT, browserViewPortIdxDelta + toBeDeleted));
//						if (toBeDeleted + browserViewPortIdxDelta >= viewPort.getSize())
//						{
//
//						}
//						toBeDeleted++;
//					}
//
//					switch (recordChange.type)
//					{
//						case REMOVE_FROM_VIEWPORT :
//							browserViewPortIdxDelta++;
//							break;
//						case ADD_TO_VIEWPORT :
//							// TODO
//							break;
//						case CHANGE :
//							// TODO
//							break;
//					}
//				}
//				while (toBeDeleted <= relativeLastRow && )
//				{
//					// record deleted before previous Add/Remove/Update operation; add before
//					iterator.add(new RecordChangeDescriptor(RecordChangeDescriptor.Types.REMOVE_FROM_VIEWPORT, browserViewPortIdxDelta + toBeDeleted));
//					toBeDeleted++;
//				}

				viewPort.slideAndCorrect(slideBy);
				viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1; // update

				// add new records if available
				// we need to replace same amount of records in current viewPort; append rows if available
				for (ViewportDataChangeMonitor vpdcm : viewPortDataChangeMonitors)
				{
					vpdcm.queueOperation(relativeFirstRow, relativeLastRow, viewPort.getStartIndex() + oldViewPortSize - numberOfDeletes, viewPortEndIdx,
						propertyValue.getFoundset(), RowData.DELETE);
				}
				viewPortRecordChangesUpdated = true;
			}
			else if (slideBy != 0)
			{
				viewPort.slideAndCorrect(slideBy);
			}
		}
		else if (viewPort.getSize() > propertyValue.getFoundset().getSize())
		{
			// if it will already send the whole viewport then the size needs to be in sync with the foundset.
			viewPort.correctAndSetViewportBoundsInternal(viewPort.getStartIndex(), viewPort.getSize());
		}
		if (oldChangeFlags != changeFlags || viewPortRecordChangesUpdated) notifyChange();
	}

	public void extendClientViewport(int firstRow, int lastRow, FoundsetTypeViewport viewPort)
	{
		if (!shouldSendAll() && !shouldSendWholeViewPort())
		{
			int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;
			int lastViewPortInsert = Math.min(lastRow, viewPortEndIdx);
			// add records that were inserted in viewPort
			for (ViewportDataChangeMonitor vpdcm : viewPortDataChangeMonitors)
			{
				vpdcm.queueOperation(firstRow - viewPort.getStartIndex(), viewPort.getSize(), firstRow, lastViewPortInsert, propertyValue.getFoundset(),
					RowData.INSERT); // for insert operations client needs to know the new viewport size so that it knows if it should delete records at the end or not; that is done by putting the 'size' in relativeLastRow
			}
			notifyChange();
		}
	}

	/**
	 * Deals with new records being inserted into the foundset.
	 * @param firstRow the first row of the insertion.
	 * @param lastRow the last row of the insertion.
	 * @param viewPort the current viewPort.
	 */
	public void recordsInserted(int firstRow, int lastRow, FoundsetTypeViewport viewPort)
	{
		int oldChangeFlags = changeFlags;
		boolean viewPortRecordChangesUpdated = false;
		if (lastRow - firstRow >= 0) foundSetSizeChanged();
		if (!shouldSendAll() && !shouldSendWholeViewPort())
		{
			int viewPortEndIdx = viewPort.getStartIndex() + viewPort.getSize() - 1;
			if (viewPort.getStartIndex() <= firstRow && firstRow <= viewPortEndIdx)
			{
				int lastViewPortInsert = Math.min(lastRow, viewPortEndIdx);
				// add records that were inserted in viewPort
				for (ViewportDataChangeMonitor vpdcm : viewPortDataChangeMonitors)
				{
					vpdcm.queueOperation(firstRow - viewPort.getStartIndex(), viewPort.getSize(), firstRow, lastViewPortInsert, propertyValue.getFoundset(),
						RowData.INSERT); // for insert operations client needs to know the new viewport size so that it knows if it should delete records at the end or not; that is done by putting the 'size' in relativeLastRow
				}
				viewPortRecordChangesUpdated = true;
			}
			else if (viewPort.getStartIndex() > firstRow)
			{
				viewPort.slideAndCorrect(lastRow - firstRow + 1);
			}
		}

		if (oldChangeFlags != changeFlags || viewPortRecordChangesUpdated) notifyChange();
	}

	public void recordsUpdated(int firstRow, int lastRow, int foundSetSize, FoundsetTypeViewport viewPort, List<String> dataproviders)
	{
		if (firstRow == 0 && lastRow == foundSetSize - 1)
		{
			if (viewPort.getSize() > 0) viewPortCompletelyChanged();
		}
		else
		{
			int oldChangeFlags = changeFlags;
			boolean viewPortRecordChangesUpdated = false;
			if ((propertyValue.getDataAdapterList() == null || !propertyValue.getDataAdapterList().isQuietRecordChangeInProgress()) && !shouldSendAll() &&
				!shouldSendWholeViewPort())
			{
				// get the rows that are changed.
				int firstViewPortIndex = Math.max(viewPort.getStartIndex(), firstRow);
				int lastViewPortIndex = Math.min(viewPort.getStartIndex() + viewPort.getSize() - 1, lastRow);
				if (firstViewPortIndex <= lastViewPortIndex)
				{
					for (ViewportDataChangeMonitor vpdcm : viewPortDataChangeMonitors)
					{
						if (firstViewPortIndex == lastViewPortIndex && dataproviders != null && dataproviders.size() > 0)
						{
							for (String dataprovider : dataproviders)
							{
								vpdcm.queueCellChange(firstViewPortIndex - viewPort.getStartIndex(), firstViewPortIndex, dataprovider,
									propertyValue.getFoundset());
							}
						}
						else
						{
							vpdcm.queueOperation(firstViewPortIndex - viewPort.getStartIndex(), lastViewPortIndex - viewPort.getStartIndex(),
								firstViewPortIndex, lastViewPortIndex, propertyValue.getFoundset(), RowData.CHANGE);
						}
					}
					viewPortRecordChangesUpdated = true;
				}
			}
			if (oldChangeFlags != changeFlags || viewPortRecordChangesUpdated) notifyChange();
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

	public List<RowData> getViewPortChanges()
	{
		return viewPortDataChangeMonitor.getViewPortChanges();
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
			shouldSendWholeViewPort() || shouldSendColumnFormats() || getHandledRequestIds().size() > 0 || getViewPortChanges().size() > 0;
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

	public static class RowData implements IToJSONWriter<IBrowserConverterContext>
	{
		public static final int CHANGE = 0;
		public static final int INSERT = 1;
		public static final int DELETE = 2;

		public final int startIndex;
		public final int endIndex;
		public final int type;

		private final IJSONStringWithConversions rowData;

		/**
		 * Null if it's a whole row, and non-null of only one column of the row is in this row data.
		 */
		public final String columnName;

		public RowData(IJSONStringWithConversions rowData, int startIndex, int endIndex, int type)
		{
			this(rowData, startIndex, endIndex, type, null);
		}

		public RowData(IJSONStringWithConversions rowData, int startIndex, int endIndex, int type, String columnName)
		{
			this.rowData = rowData;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.type = type;
			this.columnName = columnName;
		}

		@Override
		public boolean writeJSONContent(JSONWriter w, String keyInParent, IToJSONConverter<IBrowserConverterContext> converter,
			DataConversion clientDataConversions) throws JSONException
		{
			JSONUtils.addKeyIfPresent(w, keyInParent);

			w.object().key("rows").value(rowData);
			clientDataConversions.pushNode("rows").convert(rowData.getDataConversions()).popNode();

			w.key("startIndex").value(Integer.valueOf(startIndex)).key("endIndex").value(Integer.valueOf(endIndex)).key("type").value(
				Integer.valueOf(type)).endObject();

			return true;
		}

		/**
		 * True if the data of this RowData would be completely replaced by another immediately following RowData.
		 * @param newOperation the following update operation.
		 */
		public boolean isMadeIrrelevantBySubsequentRowData(RowData newOperation)
		{
			// so a change can be made obsolet by a subsequent (imediately after) change or delete of the same row;
			// it we're talking about two change operations, it matters as well if one of them is only for a specific column of the row or for the whole row
			return (type == CHANGE && (newOperation.type == CHANGE || newOperation.type == DELETE) && startIndex >= newOperation.startIndex &&
				endIndex <= newOperation.endIndex && (newOperation.columnName == null || newOperation.columnName.equals(columnName)));
		}

	}

	public void addViewportDataChangeMonitor(ViewportDataChangeMonitor viewPortChangeMonitor)
	{
		if (!viewPortDataChangeMonitors.contains(viewPortChangeMonitor)) viewPortDataChangeMonitors.add(viewPortChangeMonitor);
	}

	public void removeViewportDataChangeMonitor(ViewportDataChangeMonitor viewPortChangeMonitor)
	{
		viewPortDataChangeMonitors.remove(viewPortChangeMonitor);
	}

	/**
	 * Ignores update record events for the record with given pkHash.
	 */
	protected void pauseRowUpdateListener(String pkHash)
	{
		viewPortDataChangeMonitor.pauseRowUpdateListener(pkHash);
	}

	/**
	 * Resumes listening normally to row updates.
	 */
	protected void resumeRowUpdateListener()
	{
		viewPortDataChangeMonitor.resumeRowUpdateListener();
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
		if (propertyValue.getFoundset() != null)
		{
			boolean newHadMoreRows = propertyValue.getFoundset().hadMoreRows();
			boolean changed = (newHadMoreRows != lastHadMoreRecords);
			lastHadMoreRecords = newHadMoreRows;

			if (changed && !shouldSendAll())
			{
				int oldChangeFlags = changeFlags;
				changeFlags = changeFlags | SEND_HAD_MORE_ROWS;
				if (oldChangeFlags != changeFlags) notifyChange();
			}
		}
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
