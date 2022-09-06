/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import org.sablo.IChangeListener;

/**
 * @author acostescu
 */
public class FoundsetLinkedTypeViewportDataChangeMonitor<YF, YT>
	extends ViewportDataChangeMonitor<com.servoy.j2db.server.ngclient.property.FoundsetLinkedViewportRowDataProvider<YF, YT>>
{

	private final FoundsetLinkedTypeSabloValue<YF, YT> foundsetLinkedTypeSabloValue;

	public FoundsetLinkedTypeViewportDataChangeMonitor(IChangeListener chMonitor,
		FoundsetLinkedViewportRowDataProvider<YF, YT> foundsetLinkedViewportRowDataProvider, FoundsetLinkedTypeSabloValue<YF, YT> foundsetLinkedTypeSabloValue)
	{
		super(chMonitor, foundsetLinkedViewportRowDataProvider);
		this.foundsetLinkedTypeSabloValue = foundsetLinkedTypeSabloValue;
	}

	@Override
	public boolean queueCellChangeDueToColumn(int relativeRowIndex, int oldViewportSize, String columnDPName)
	{
		if (foundsetLinkedTypeSabloValue.isLinkedToRecordDP(columnDPName))
			return super.queueCellChange(relativeRowIndex, oldViewportSize, FoundsetLinkedTypeSabloValue.DUMMY_COL_NAME);
		else return false;
	}

}
