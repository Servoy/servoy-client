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
package com.servoy.j2db.util.toolbar;


import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;

import com.servoy.j2db.util.IProvideButtonModel;

/**
 * <class description>
 * 
 * @author jblok
 */

public class ToolbarToggleButton extends JToggleButton
{
/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public ToolbarToggleButton(Icon icon)
	{
		super(icon);
		init();
	}

	public ToolbarToggleButton(Action action)
	{
		super();
		if (action instanceof IProvideButtonModel)
		{
			setModel(((IProvideButtonModel)action).getModel());
		}
		setAction(action);

		init();
	}

	private void init()
	{
		setPreferredSize(new Dimension(ToolbarButton.PREF_HEIGHT, ToolbarButton.PREF_HEIGHT));
		setText(null);
		setMnemonic(0);
		setOpaque(false);
		setRequestFocusEnabled(false);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}
