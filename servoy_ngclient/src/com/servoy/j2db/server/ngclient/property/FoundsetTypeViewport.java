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

import org.slf4j.Logger;

import com.servoy.j2db.dataprocessing.FoundSetEvent;
import com.servoy.j2db.dataprocessing.IFoundSetEventListener;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;

/**
 * Holds the client used viewport info for this foundset.
 *
 * @author acostescu
 */
public class FoundsetTypeViewport
{

	private final Logger log;

	protected int startIndex = 0;
	protected int size = 0;
	protected FoundsetTypeChangeMonitor changeMonitor;
	protected IFoundSetInternal foundset;
	protected IFoundSetEventListener foundsetEventListener;

	protected int preferredViewPortSize; // 50 by default; see constructor
	private boolean preferredViewportContainsSelection; // default is false; see constructor
	private boolean preferredViewportCentersOnSelected = true; // default is false; see constructor

	private boolean sendingInitialPreferredViewport = false;
	private boolean callPreferredViewportBoundsOnSelectionChange = false;
	private final boolean foundsetDefinitionListener;

	/**
	 * Creates a new viewport object.
	 * @param changeMonitor change monitor can be used to announce changes in viewport (bounds).
	 */
	public FoundsetTypeViewport(FoundsetTypeChangeMonitor changeMonitor, FoundsetPropertyTypeConfig specConfig, Logger log)
	{
		this.changeMonitor = changeMonitor;
		this.preferredViewPortSize = specConfig.initialPreferredViewPortSize;
		this.preferredViewportContainsSelection = specConfig.sendSelectionViewportInitially;
		this.preferredViewportCentersOnSelected = specConfig.centerInitialViewportOnSelected;
		this.foundsetDefinitionListener = specConfig.foundsetDefinitionListener;
		this.log = log;
		// we don't define initialSelectionViewportCentered in .spec config yet because the component usually doesn't know on first show what it's desired page size is (that is normally based on UI space) - or some component might even not know if it's using paging or scrolling UI viewport
		// but initialSelectionViewportCentered can still be altered by the component after it is shown in browser via a foundset type API call from client
	}

	protected void setFoundset(IFoundSetInternal newFoundset)
	{
		if (foundset != null) foundset.removeFoundSetEventListener(getFoundsetEventListener());
		if (newFoundset != null) newFoundset.addFoundSetEventListener(getFoundsetEventListener());
		this.foundset = newFoundset;

		setPreferredViewportBounds();
		changeMonitor.viewPortCompletelyChanged();
	}

	public void setPreferredViewportBounds()
	{
		sendingInitialPreferredViewport = true;

		if (foundset != null)
		{
			if (foundset.getSelectedIndex() >= foundset.getSize())
			{
				this.callPreferredViewportBoundsOnSelectionChange = true;
				return;
			}
			// reset to the preferred viewport
			int firstSelectedIndex;
			if (preferredViewportContainsSelection && (firstSelectedIndex = foundset.getSelectedIndex()) >= 0)
			{
				int startIdx, vpSize;
				if (preferredViewportCentersOnSelected)
				{
					// "scrolling"; center selection in viewport if possible
					startIdx = Math.max(0, firstSelectedIndex - (preferredViewPortSize / 2));
				}
				else
				{
					// "paging"; calculate the page that contains selection
					startIdx = preferredViewPortSize * (firstSelectedIndex / preferredViewPortSize);
				}
				vpSize = Math.min(preferredViewPortSize, foundset.getSize() - startIdx); // if selection is at the beginning and we can send more records after it do that (so selection won't be centered, but we still try to send preferred size)

				// if selection is at the end and we can send more records from before selection, do that (so selection won't be centered, but we still try to send preferred size)
				if (preferredViewportCentersOnSelected && vpSize < preferredViewPortSize && startIdx > 0)
				{
					startIdx = Math.max(0, foundset.getSize() - preferredViewPortSize);
					vpSize = Math.min(preferredViewPortSize, foundset.getSize() - startIdx);
				}

				setBounds(startIdx, vpSize);
			}
			else
			{
				setBounds(0, Math.min(preferredViewPortSize, foundset.getSize()));
			}
		}
		else
		{
			setBounds(0, 0);
		}
	}

	protected void selectionChanged()
	{
		// as for example when showing the form the first time foundset will have size 0
		// then it will gain records with selection -1 then selection will be set to 0 then the on show can change selection again -
		// all this in the same event - we must set preferred viewport bounds based on last initial selection
		if (callPreferredViewportBoundsOnSelectionChange || (preferredViewportContainsSelection && sendingInitialPreferredViewport))
		{
			this.callPreferredViewportBoundsOnSelectionChange = false;
			setPreferredViewportBounds();
		}
	}

	protected void clearSendingInitialPreferredViewport()
	{
		// TODO can we apply preferred viewport of selection nicer? so detect last selection to be used somehow without plugging into selection change listener and clear changes?
		sendingInitialPreferredViewport = false;
	}

	public int getStartIndex()
	{
		return startIndex;
	}

	public int getSize()
	{
		return size;
	}

	/**
	 * The viewPort needs to change to the new startIndex/size.
	 */
	public void setBounds(int startIndex, int size)
	{
		int oldStartIndex = this.startIndex;
		int oldSize = this.size;

		correctAndSetViewportBoundsInternal(startIndex, size);

		if (oldStartIndex != this.startIndex || oldSize != this.size) changeMonitor.viewPortCompletelyChanged();
	}

	/**
	 * Extends the viewport - useful for sending more records to client without re-sending the whole viewport.
	 *
	 * @param positiveOrNegativeRecordNo the number of records to extend the viewPort with. A positive value
	 * will append records at the end of the viewPort and a negative one will prepend (add to the beginning).
	 */
	public void loadExtraRecords(int positiveOrNegativeRecordNo)
	{
		int oldStartIndex = this.startIndex;
		int oldSize = this.size;

		int oldFoundsetSize = 0;
		if (foundset != null)
		{
			foundset.removeFoundSetEventListener(getFoundsetEventListener());
			oldFoundsetSize = foundset.getSize();
		}
		if (positiveOrNegativeRecordNo >= 0)
		{
			correctAndSetViewportBoundsInternal(oldStartIndex, oldSize + positiveOrNegativeRecordNo);
			if (oldStartIndex != startIndex || oldSize != size)
				changeMonitor.extendClientViewport(this.startIndex + oldSize, this.startIndex + this.size - 1, oldSize, this);
		}
		else
		{
			int wantedStartIndex = Math.max(positiveOrNegativeRecordNo + startIndex, 0);
			correctAndSetViewportBoundsInternal(wantedStartIndex, size + (oldStartIndex - wantedStartIndex));
			if (oldStartIndex != startIndex || oldSize != size) changeMonitor.extendClientViewport(this.startIndex, oldStartIndex - 1, oldSize, this);
		}

		if (oldStartIndex != startIndex || oldSize != size) changeMonitor.viewPortBoundsOnlyChanged();
		if (foundset != null)
		{
			if (foundset.getSize() != oldFoundsetSize) changeMonitor.foundSetSizeChanged();
			foundset.addFoundSetEventListener(getFoundsetEventListener());
		}
	}

	/**
	 * Narrow down the viewport - useful for discarding some records from the client viewport start or end without re-sending the rest of the viewport.
	 *
	 * @param positiveOrNegativeRecordNo the number of records to shrink the viewPort with. A positive value
	 * will remove records from the beginning and a negative value will remove records from the end of the viewport.
	 */
	public void loadLessRecords(int positiveOrNegativeRecordNo)
	{
		int oldStartIndex = this.startIndex;
		int oldSize = this.size;

		int oldFoundsetSize = 0;
		if (foundset != null)
		{
			foundset.removeFoundSetEventListener(getFoundsetEventListener());
			oldFoundsetSize = foundset.getSize();
		}
		if (positiveOrNegativeRecordNo >= 0)
		{
			// remove from the beginning
			correctAndSetViewportBoundsInternal(oldStartIndex + positiveOrNegativeRecordNo, oldSize - positiveOrNegativeRecordNo);
			if (oldStartIndex != startIndex || oldSize != size)
			{
				if (size == 0) changeMonitor.viewPortCompletelyChanged(); // the correctAndSetViewportBoundsInternal above, in case of a larger-then-viewport shrink
				// request could reset the viewport bounds to 0, 0 even if previously the viewport had a different start index; so it would actually shrink by less
				// then what is calculated in the else branch below
				else changeMonitor.shrinkClientViewport(0, startIndex - oldStartIndex - 1, oldSize); // shrink needs old viewport relative removed interval
			}
		}
		else
		{
			// remove from the end; it's negative
			correctAndSetViewportBoundsInternal(oldStartIndex, oldSize + positiveOrNegativeRecordNo);
			// normally start index has not changed
			if (oldSize != size) changeMonitor.shrinkClientViewport(size, oldSize - 1, oldSize);
		}

		if (oldStartIndex != startIndex || oldSize != size) changeMonitor.viewPortBoundsOnlyChanged();
		if (foundset != null)
		{
			if (foundset.getSize() != oldFoundsetSize) changeMonitor.foundSetSizeChanged();
			foundset.addFoundSetEventListener(getFoundsetEventListener());
		}
	}

	/**
	 * Corrects bounds given new bounds to be valid and then applies them to current viewport.
	 *
	 * This method can also load more records into the foundset (thus firing foundset events) in case of large foundsets with 'hadMoreRecords' true,
	 * in case the given new bounds require new records.
	 */
	protected void correctAndSetViewportBoundsInternal(int newStartIndex, int newSize)
	{
		if (foundset != null)
		{
			IRecordInternal firstRec = foundset.getRecord(newStartIndex); // this can trigger a query for more records if foundset hadMoreRows is true; that in turn can update through listener serverSize and hadMoreRows related flags on the change monitor

			if (firstRec != null)
			{
				if (newSize > 0)
				{
					IRecordInternal lastRec = foundset.getRecord(newStartIndex + newSize - 1); // this can trigger a query for more records if foundset hadMoreRows is true; that in turn can update through listener serverSize and hadMoreRows related flags on the change monitor
					startIndex = newStartIndex; // do this after getRecord above would potentially load more records, trigger inserted event and potentially wrongly adjust current viewport bounds
					if (lastRec == null)
					{
						size = foundset.getSize() - startIndex;
					}
					else
					{
						size = newSize;
					}
				}
				else
				{
					startIndex = newStartIndex;
					size = 0;
				}
			}
			else
			{
				startIndex = 0;
				size = 0;
			}
		}
		else
		{
			startIndex = 0;
			size = 0;
		}
	}

	protected IFoundSetEventListener getFoundsetEventListener()
	{
		if (foundsetEventListener == null)
		{
			foundsetEventListener = new IFoundSetEventListener()
			{
				@Override
				public boolean wantsFoundSetDefinitionChanges()
				{
					return foundsetDefinitionListener;
				}

				@Override
				public void foundSetChanged(FoundSetEvent event)
				{
					if (event.getType() == FoundSetEvent.FIND_MODE_CHANGE) changeMonitor.findModeChanged(foundset.isInFindMode());
					else if (event.getType() == FoundSetEvent.CONTENTS_CHANGED)
					{
						// partial change only push the changes.
						if (event.getChangeType() == FoundSetEvent.CHANGE_DELETE)
						{
							if (log.isTraceEnabled()) log.trace("Viewport '" + hashCode() + "' (start: " + startIndex + ", size: " + size +
								") foundset listener - got DELETE (" + event.getFirstRow() + "," +
								event.getLastRow() + "; foundset " + (foundset != null ? "size is now: " + foundset.getSize() : "is null."));

							changeMonitor.recordsDeleted(event.getFirstRow(), event.getLastRow(), FoundsetTypeViewport.this);

							if (log.isTraceEnabled()) log.trace("Viewport '" + hashCode() + "' has handled the DELETE; it is now (start: " + startIndex +
								", size: " + size + "); foundset size: " + "; foundset " +
								(foundset != null ? "size is now: " + foundset.getSize() : "is null."));
						}
						else if (event.getChangeType() == FoundSetEvent.CHANGE_INSERT)
						{
							if (log.isTraceEnabled()) log.trace("Viewport '" + hashCode() + "' (start: " + startIndex + ", size: " + size +
								") foundset listener - got INSERT (" + event.getFirstRow() + "," +
								event.getLastRow() + "; foundset size is now: " + "; foundset " +
								(foundset != null ? "size is now: " + foundset.getSize() : "is null."));

							changeMonitor.recordsInserted(event.getFirstRow(), event.getLastRow(), size, FoundsetTypeViewport.this); // true - slide if first so that viewPort follows the first record

							if (log.isTraceEnabled()) log.trace("Viewport '" + hashCode() + "' has handled the INSERT; it is now (start: " + startIndex +
								", size: " + size + "; foundset " + (foundset != null ? "size is now: " + foundset.getSize() : "is null."));
						}
						else if (event.getChangeType() == FoundSetEvent.CHANGE_UPDATE)
						{
							changeMonitor.recordsUpdated(event.getFirstRow(), event.getLastRow(), foundset.getSize(), FoundsetTypeViewport.this,
								event.getDataProviders());
						}
						else if (event.getChangeType() == FoundSetEvent.FOUNDSET_INVALIDATED && changeMonitor.isVisible()) foundset.getSize(); // getSize on a related foundset (that can be invalidated) will validate the foundset and send any changes as subsequent events; do that as we are actively monitoring this foundset

						changeMonitor.checkHadMoreRows();
					}
					else if (event.getType() == FoundSetEvent.SELECTION_MODE_CHANGE) changeMonitor.multiSelectChanged();
					else if (event.getType() == FoundSetEvent.FOUNDSET_DEFINITION_CHANGE) changeMonitor.foundsetDefinitionChanged();
				}
			};
		}
		return foundsetEventListener;
	}

	protected void dispose()
	{
		if (foundset != null) foundset.removeFoundSetEventListener(getFoundsetEventListener());
	}

	/**
	 * Slides the viewPort (startIndex) to higher or lower values and then corrects viewPort bounds (if they became invalid due to foundset changes).<br/>
	 * Call this only when the viewPort data remains the same or when viewPort data will be updated through granular add/remove operations.
	 *
	 * @param delta can be a positive or negative value.
	 */
	protected void slideAndCorrect(int delta)
	{
		int oldStartIndex = startIndex;
		int oldSize = size;

		correctAndSetViewportBoundsInternal(oldStartIndex + delta, oldSize);

		if (oldStartIndex != startIndex || oldSize != size) changeMonitor.viewPortBoundsOnlyChanged();
	}

	/**
	 * Sets the preferred viewport size.
	 */
	public void setPreferredViewportSize(int preferredViewPortSize)
	{
		this.preferredViewPortSize = preferredViewPortSize;
	}

	/**
	 * If this is true, then server side foundset property will initially send to client a viewport of 'preferredViewPortSize' based on currently selected row.
	 * If this is false, then server side foundset property will initially send to client a viewport of 'preferredViewPortSize' starting from row 0.
	 */
	public void setPreferredViewportContainsSelection(boolean preferredViewportContainsSelection)
	{
		this.preferredViewportContainsSelection = preferredViewportContainsSelection;
	}

	/**
	 * This value is only relevant if preferredViewportContainsSelection is true.
	 *
	 * If 'preferredViewportCentersOnSelected' is true, then server side foundset property will initially send to client a viewport of 'preferredViewPortSize'
	 * with selected row centered in that viewport. If selection is close to foundset start or end index it will still try to send closest
	 * 'preferredViewPortSize' records, even if selected index will not be centered in that viewport.<br/><br/>
	 *
	 * If 'preferredViewportCentersOnSelected' is false, then server side foundset property will initially send to client the 'page' viewport (based on pages of
	 * 'preferredViewPortSize' records) that contains the selected record. For example preferredViewPortSize = 15 and selected row is 17 - it will send a viewport
	 * of 15 - 29 (if foundset size is > 29).
	 */
	public void setPreferredViewportCentersOnSelected(boolean preferredViewportCentersOnSelected)
	{
		this.preferredViewportCentersOnSelected = preferredViewportCentersOnSelected;
	}


	@Override
	public String toString()
	{
		return "FTVP: " + foundset;
	}

}
