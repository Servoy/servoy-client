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

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * This class monitors selection changes in the foundset in order for them to be sent to client/browser.
 * @author acostescu
 */
final class FoundsetPropertySelectionListener implements ListSelectionListener
{

	private boolean ignoreSelectionChanges;
	private final FoundsetTypeChangeMonitor changeMonitor;
	private final FoundsetTypeViewport viewPort;

	public FoundsetPropertySelectionListener(FoundsetTypeChangeMonitor changeMonitor, FoundsetTypeViewport viewPort)
	{
		this.changeMonitor = changeMonitor;
		this.viewPort = viewPort;
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		if (!e.getValueIsAdjusting() && !ignoreSelectionChanges)
		{
			changeMonitor.selectionChanged();
			viewPort.selectionChanged();
		}
	}

	/**
	 * The listener will ignore selection changes until {@link #resume()} is called.
	 */
	public void pause()
	{
		this.ignoreSelectionChanges = true;
	}

	/**
	 * The listener will resume it's normal operation after being 'paused' with {@link #pause()}.
	 */
	public void resume()
	{
		this.ignoreSelectionChanges = false;
	}

}