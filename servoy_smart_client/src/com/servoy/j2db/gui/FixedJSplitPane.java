/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.gui;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;

/**
 * @author jcompagner
 */
public class FixedJSplitPane extends JSplitPane
{
	private double divLocation = -1;

	public FixedJSplitPane()
	{
		super();
		doFix();
	}

	public FixedJSplitPane(int orentation)
	{
		super(orentation);
		doFix();
	}

	public FixedJSplitPane(int orentation, boolean newContinuousLayout)
	{
		super(orentation, newContinuousLayout);
		doFix();
	}

	public FixedJSplitPane(int orentation, Component l, Component r)
	{
		super(orentation, l, r);
		doFix();
	}

	public FixedJSplitPane(int orentation, boolean newContinuousLayout, Component l, Component r)
	{
		super(orentation, newContinuousLayout, l, r);
		doFix();
	}

	private void doFix()
	{
		setBorder(BorderFactory.createEmptyBorder());
		setResizeWeight(1);
		setActionMap(null);//delete all actions, whas using F6 and F8 keystrokes, which is bad in debugger env
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JSplitPane#setDividerLocation(double)
	 */
	@Override
	public void setDividerLocation(double proportionalLocation)
	{
		super.setDividerLocation(proportionalLocation);
		if (!isDisplayable())
		{
			divLocation = proportionalLocation;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JSplitPane#getDividerLocation()
	 */
	@Override
	public int getDividerLocation()
	{
		if (divLocation != -1 && isDisplayable())
		{
			super.setDividerLocation(divLocation);
			divLocation = -1;
		}
		return super.getDividerLocation();
	}
}
