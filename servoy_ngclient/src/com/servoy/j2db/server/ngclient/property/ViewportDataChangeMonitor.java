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
import org.sablo.websocket.utils.JSONUtils.ChangesToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;
import org.sablo.websocket.utils.JSONUtils.IJSONStringWithConversions;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeChangeMonitor.RowData;
import com.servoy.j2db.util.Debug;

/**
 * This class is responsible for monitoring changes to a foundset property's viewport data and
 * being able to update the client with those changes.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ViewportDataChangeMonitor<DPT extends ViewportRowDataProvider>
{
	public static final String VIEWPORT_CHANGED = "viewportDataChanged";

	/**
	 * ViewPort bounds and data changed; for example client requested completely new viewPort bounds.
	 */
	protected boolean viewPortCompletelyChanged = false;

	protected List<RowData> viewPortChanges = new ArrayList<>();

	protected final DPT rowDataProvider;

	protected final IChangeListener monitor;

	private FoundsetTypeViewportDataChangeMonitor foundsetTypeViewportDataChangeMonitor;

//	protected String ignoreUpdateOnPkHash;

	public ViewportDataChangeMonitor(IChangeListener monitor, DPT rowDataProvider)
	{
		this.rowDataProvider = rowDataProvider;
		this.monitor = monitor;
	}

	protected void setFoundsetTypeViewportDataChangeMonitor(FoundsetTypeViewportDataChangeMonitor foundsetTypeViewportDataChangeMonitor)
	{
		// ViewportDataChangeMonitor that are used for foundset linked properties are aware of the change monitor of the actual foundset property so that they can
		// trigger sending special updates to client - meant to trigger the client-side foundset listener even if changes happened only in the linked properties, not just on actual foundset property contents
		this.foundsetTypeViewportDataChangeMonitor = foundsetTypeViewportDataChangeMonitor;
	}

	public DPT getRowDataProvider()
	{
		return rowDataProvider;
	}

	public void viewPortCompletelyChanged()
	{
		boolean changed = !viewPortCompletelyChanged;
		viewPortCompletelyChanged = true;
		viewPortChanges.clear();
		if (changed && monitor != null) monitor.valueChanged();
	}

	public boolean shouldSendWholeViewport()
	{
		return viewPortCompletelyChanged;
	}

	public List<RowData> getViewPortChanges()
	{
		return viewPortChanges;
	}

	public void clearChanges()
	{
		viewPortCompletelyChanged = false;

		viewPortChanges.clear();
		FoundsetDataAdapterList fsDAL = rowDataProvider.getDataAdapterList();
		if (fsDAL != null) fsDAL.resetDALToSelectedIndexQuietly();
	}

	/**
	 * This gets called when rows in the viewport were deleted/inserted/changed.
	 * @param relativeFirstRow viewPort relative start index for given operation.
	 * @param relativeLastRow viewPort relative end index for given operation (inclusive).
	 * @param newDataStartIndex foundset relative first row of new data (that is automatically added to the end of viewPort in case of delete, or just added in case of insert, or just changed for change) index.
	 * @param newDataEndIndex foundset relative end row of new data (that is automatically added to the end of viewPort in case of delete, or just added in case of insert, or just changed for change) index.
	 * @param operationType can be one of {@link RowData#DELETE}, {@link RowData#INSERT} or {@link RowData#CHANGE}.
	 *
	 * @return true if the operation was queued, false otherwise.
	 */
	public boolean queueOperation(int relativeFirstRow, int relativeLastRow, final int newDataStartIndex, final int newDataEndIndex,
		final IFoundSetInternal foundset, int operationType)
	{
		if (!rowDataProvider.isReady()) return false;

		if (!shouldSendWholeViewport())
		{
			// changed values that were sent from browser should not be sent back as they are already up-to-date

//			if (operationType != RowData.CHANGE || ignoreUpdateOnPkHash == null || newDataStartIndex != newDataEndIndex ||
//				!foundset.getRecord(newDataStartIndex).getPKHashKey().equals(ignoreUpdateOnPkHash))
//			{

			boolean changed = (viewPortChanges.size() == 0);
			try
			{
				IJSONStringWithConversions writtenAsJSON;
				writtenAsJSON = JSONUtils.writeToJSONString(new IToJSONWriter<IBrowserConverterContext>()
				{
					@Override
					public boolean writeJSONContent(JSONWriter w, String keyInParent, IToJSONConverter<IBrowserConverterContext> converter,
						DataConversion clientDataConversions) throws JSONException
					{
						rowDataProvider.writeRowData(newDataStartIndex, newDataEndIndex, foundset, w, clientDataConversions);
						return true;
					}
				}, FullValueToJSONConverter.INSTANCE);

				removeIrrelevantOperationsAndAdd(new RowData(writtenAsJSON, relativeFirstRow, relativeLastRow, operationType));

				if (changed && monitor != null) monitor.valueChanged();
				return true;
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
//			}
		}
		return false;
	}

	protected boolean removeIrrelevantOperationsAndAdd(RowData newOperation)
	{
		boolean changed = false;
		// it happens often that we get multiple change events for the same row one after another; don't send each one to browser as it's not needed
		while (viewPortChanges.size() > 0 && viewPortChanges.get(viewPortChanges.size() - 1).isMadeIrrelevantBySubsequentRowData(newOperation))
		{
			viewPortChanges.remove(viewPortChanges.size() - 1);
			changed = true;
		}

		if (viewPortChanges.size() == 0 || !newOperation.isMadeIrrelevantByPreviousRowData(viewPortChanges.get(viewPortChanges.size() - 1)))
		{
			viewPortChanges.add(newOperation);
			changed = true;
		}

		return changed;
	}

	/**
	 * This gets called when the value of a cell in the viewport was changed.
	 *
	 * @param relativeFirstRow viewPort relative start index for given operation.
	 * @param relativeLastRow viewPort relative end index for given operation (inclusive).
	 * @param newDataStartIndex foundset relative first row of new data (that is automatically added to the end of viewPort in case of delete, or just added in case of insert, or just changed for change) index.
	 * @param newDataEndIndex foundset relative end row of new data (that is automatically added to the end of viewPort in case of delete, or just added in case of insert, or just changed for change) index.
	 * @param granularUpdate if true then the cell (child comp. property or foundset linked prop. like a valuelist which called filter from client) called valueChanged on it's monitor, so we need to send a
	 * granular update if available on that type; if it is false then a full toJSON for that cell should be done.
	 *
	 * @return true if the operation was queued, false otherwise.
	 */
	public boolean queueCellChange(int relativeRowIndex, final int absoluteRowIndex, final String columnName, final IFoundSetInternal foundset,
		boolean granularUpdate)
	{
		if (!rowDataProvider.isReady() || !rowDataProvider.containsColumn(columnName)) return false;

		if (!shouldSendWholeViewport())
		{
			boolean changed = (viewPortChanges.size() == 0);
			try
			{
				IJSONStringWithConversions writtenAsJSON;
				writtenAsJSON = JSONUtils.writeToJSONString(new IToJSONWriter<IBrowserConverterContext>()
				{
					@Override
					public boolean writeJSONContent(JSONWriter w, String keyInParent, IToJSONConverter<IBrowserConverterContext> converter,
						DataConversion clientDataConversions) throws JSONException
					{
						rowDataProvider.writeRowData(absoluteRowIndex, absoluteRowIndex, columnName, foundset, w, clientDataConversions);
						return true;
					}
				}, granularUpdate ? ChangesToJSONConverter.INSTANCE : FullValueToJSONConverter.INSTANCE);

				boolean added = removeIrrelevantOperationsAndAdd(
					new RowData(writtenAsJSON, relativeRowIndex, relativeRowIndex, RowData.CHANGE, columnName, granularUpdate));

				// If at least one ViewportDataChangeMonitor sent changes (so the change affected the data in that ViewportDataChangeMonitor)
				// but the foundset property itself was not affected in any way; still, we want the client side (browser) listeners attached to the foundset property
				// notify that an update occurred in this case, even though it didn't actually affect the foundset property itself... but the other foundset linked properties.
				// in the future if we impl. change listeners for foundset linked or foundset linked component types, we could avoid this "queueLinkedPropertyUpdate" completely
				if (added && foundsetTypeViewportDataChangeMonitor != null)
				{
					foundsetTypeViewportDataChangeMonitor.queueLinkedPropertyUpdate(relativeRowIndex, relativeRowIndex, columnName);
				}

				if (changed && monitor != null) monitor.valueChanged();
				return true;
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
		return false;
	}

	/**
	 * Ignores update record events for the record with given pkHash.
	 *
	 * @deprecated disabled for now. Should we really do this? when an update comes from client who's to say a data change handler or something won't\
	 * change other properties of the component/values of the record that should get sent to client?
	 */
	@Deprecated
	protected void pauseRowUpdateListener(String pkHash)
	{
//		this.ignoreUpdateOnPkHash = pkHash;
	}

	/**
	 * Resumes listening normally to row updates.
	 *
	 * @deprecated disabled for now. Should we really do this? when an update comes from client who's to say a data change handler or something won't\
	 * change other properties of the componen/values of the record that should get sent to client?
	 */
	@Deprecated
	protected void resumeRowUpdateListener()
	{
//		this.ignoreUpdateOnPkHash = null;
	}

	@Override
	public String toString()
	{
		return "ViewportDataChangeMonitor [viewPortCompletelyChanged=" + viewPortCompletelyChanged + ", viewPortChanges=" + viewPortChanges + "]";
	}


}
