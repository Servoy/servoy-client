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
package com.servoy.j2db.util.gui;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import com.servoy.j2db.util.ISupportFocusTransfer;

/**
 * Helper class for handling tab sequences in smart client. Certain components
 * should not hold the focus for our tab sequences, while they do hold the focus
 * in Java. This class automatically transfer the focus from these components to
 * the correct ones.
 * 
 * @author gerzse
 */
public class AutoTransferFocusListener implements FocusListener
{
	private final Component owner;
	private final ISupportFocusTransfer directionProvider;

	public AutoTransferFocusListener(Component owner, ISupportFocusTransfer directionProvider)
	{
		this.owner = owner;
		this.directionProvider = directionProvider;
	}

	public void focusGained(FocusEvent e)
	{
		if (directionProvider.isTransferFocusBackwards()) owner.transferFocusBackward();
		else owner.transferFocus();
		directionProvider.setTransferFocusBackwards(false);
	}

	public void focusLost(FocusEvent e)
	{
		// do nothing		
	}
}
