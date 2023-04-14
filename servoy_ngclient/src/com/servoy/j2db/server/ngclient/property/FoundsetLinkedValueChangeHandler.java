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

import com.servoy.j2db.dataprocessing.IRecordInternal;

/**
 * This class handles underlying foundset linked property notifications,
 * and is in charge for making sure those changes are sent correctly to client - if needed.
 *
 * @author acostescu
 */
public class FoundsetLinkedValueChangeHandler
{

	private final FoundsetTypeSabloValue foundsetPropValue;
	private boolean applyingDPValueFromClient;
	private boolean restoreSelectionToFoundsetDALWhenApplyFinishes;

	public FoundsetLinkedValueChangeHandler(FoundsetTypeSabloValue foundsetPropValue)
	{
		this.foundsetPropValue = foundsetPropValue;
	}

	public void valueChangedInFSLinkedUnderlyingValue(String propertyName, ViewportDataChangeMonitor< ? > viewPortChangeMonitor)
	{
		// for example foundset linked properties can change due to other reasons then the foundset change listener firing (either they have special behavior or for example related DPs that get updates from the DAL
		// on the current record from the FoundsetDAL) so without an actual change in the record itself; any actual change in the record; in this case we need to mark it correctly in viewport as a change
		IRecordInternal record = foundsetPropValue.getDataAdapterList().getRecord();
		if (record != null)
		{
			queueCellChangeOnRecord(propertyName, record, viewPortChangeMonitor);
		}
	}

	private void queueCellChangeOnRecord(final String propertyName, final IRecordInternal record,
		final ViewportDataChangeMonitor< ? > viewPortChangeMonitor)
	{
		int idx = foundsetPropValue.getFoundset().getRecordIndex(record);
		if (idx >= 0)
		{
			FoundsetTypeViewport viewPort = foundsetPropValue.getViewPort();
			int relativeIdx = idx - viewPort.getStartIndex();
			if (relativeIdx >= 0 && relativeIdx < viewPort.getSize())
			{
				viewPortChangeMonitor.queueCellChange(relativeIdx, viewPort.getSize(), propertyName);
			}
		}
	}

	public void setApplyingDPValueFromClient(boolean applyingDPValueFromClient)
	{
		if (applyingDPValueFromClient) this.applyingDPValueFromClient = true;
		else
		{
			this.applyingDPValueFromClient = false;

			// see comment in FoundsetLinkedTypeSabloValue.updatePropertyValueForRecord(...) to see why we do this here; it is similar in case of foundset linked component type props - with DP props.
			if (restoreSelectionToFoundsetDALWhenApplyFinishes)
			{
				restoreSelectionToFoundsetDALWhenApplyFinishes = false;
				foundsetPropValue.setDataAdapterListToSelectedRecord();
			}
		}
	}

	public boolean willRestoreSelectedRecordToFoundsetDALLater()
	{
		if (applyingDPValueFromClient)
		{
			restoreSelectionToFoundsetDALWhenApplyFinishes = true;
			return true; // if DP changes are being applied from client, restore foundset selection to FoundsetDAL after all the apply operation finished
		}
		return false;
	}

}