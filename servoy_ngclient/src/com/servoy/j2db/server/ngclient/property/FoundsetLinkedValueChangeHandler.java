/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import com.servoy.j2db.dataprocessing.IRecordInternal;

/**
 * This class handles underlying foundset linked property notifications,
 * and is in charge for making sure those changes are sent correctly to client - if needed.
 *
 * @author acostescu
 */
public class FoundsetLinkedValueChangeHandler
{

	protected List<Runnable> changesWhileUpdatingFoundsetBasedDPFromClient;
	private final FoundsetTypeSabloValue foundsetPropValue;

	public FoundsetLinkedValueChangeHandler(FoundsetTypeSabloValue foundsetPropValue)
	{
		this.foundsetPropValue = foundsetPropValue;
	}

	public void valueChangedInFSLinkedUnderlyingValue(String propertyName, ViewportDataChangeMonitor< ? > viewPortChangeMonitor, boolean granularUpdate)
	{
		// for example foundset linked properties can change due to other reasons then the foundset change listener firing (either they have special behavior or for example related DPs that get updates from the DAL
		// on the current record from the FoundsetDAL) so without an actual change in the record itself; any actual change in the record; in this case we need to mark it correctly in viewport as a change
		IRecordInternal record = foundsetPropValue.getDataAdapterList().getRecord();
		if (record != null)
		{
			Runnable queueChangeRunnable = queueCellChangeOnRecord(propertyName, record, viewPortChangeMonitor, granularUpdate);

			if (changesWhileUpdatingFoundsetBasedDPFromClient != null)
			{
				// if for example a dataprovider property change does in its fromJSON a monitor.valueChanged() (for example an integer DP getting client update of 1.15 would want to send back 1.00)
				// it will end up here; we do want to send that back to the client but as the new value is not
				// yet pushed to the record, we don't want the new value to be reverted by a DAL.setRecord() that happens when queuing changes for a specific record index
				// so we need to handle this change at a later time
				changesWhileUpdatingFoundsetBasedDPFromClient.add(queueChangeRunnable);
			}
			else
			{
				queueChangeRunnable.run();
			}
		}
	}

	public void setApplyingDPValueFromClient(boolean applyInProgress)
	{
		if (applyInProgress)
		{
			changesWhileUpdatingFoundsetBasedDPFromClient = new ArrayList<>(); // we prevent a fromJSON on the dataprovider value that triggers valueChanged (so propertyFlaggedAsDirty) to re-apply (old) record values to DPs (effectively reverting the new value)
			// this can happen for example with integer DPs that get a double value from the browser and they round/trunc thus need to resend the value to client
			// we will execute the propertyFlaggedAsDirty code later, after DP value was applied
			// TODO shouldn't we apply in one go? so apply directly the value to record instead of setting it first in the component DP property?
		}
		else
		{
			if (changesWhileUpdatingFoundsetBasedDPFromClient != null)
			{
				for (Runnable r : changesWhileUpdatingFoundsetBasedDPFromClient)
					r.run();
				changesWhileUpdatingFoundsetBasedDPFromClient = null;
			}
		}
	}

	private Runnable queueCellChangeOnRecord(final String propertyName, final IRecordInternal record,
		final ViewportDataChangeMonitor< ? > viewPortChangeMonitor, final boolean granularUpdate)
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				int idx = foundsetPropValue.getFoundset().getRecordIndex(record);
				if (idx >= 0)
				{
					FoundsetTypeViewport viewPort = foundsetPropValue.getViewPort();
					int relativeIdx = idx - viewPort.getStartIndex();
					if (relativeIdx >= 0 && relativeIdx < viewPort.getSize())
					{
						viewPortChangeMonitor.queueCellChange(relativeIdx, idx, propertyName, foundsetPropValue.getFoundset(), granularUpdate);
					}
				}
			}
		};
	}

}