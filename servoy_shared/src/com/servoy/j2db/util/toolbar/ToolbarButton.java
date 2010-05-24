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
import javax.swing.JButton;

import com.servoy.j2db.util.IProvideButtonModel;

/**
 * <class description>
 * 
 * @author jblok
 */
public class ToolbarButton extends JButton
{
	public static final int PREF_HEIGHT = 22;

	public ToolbarButton(Icon icon)
	{
		super(icon);
		init(false);
	}

	public ToolbarButton(Action action)
	{
		this(action, false);
	}

	public ToolbarButton(Action action, boolean displayText)
	{
		super();
		if (action instanceof IProvideButtonModel)
		{
			setModel(((IProvideButtonModel)action).getModel());
		}
		putClientProperty("hideActionText", displayText ? Boolean.FALSE : Boolean.TRUE); //$NON-NLS-1$
		setAction(action);

		Dimension d = getPreferredSize();
		init(displayText);
		if (displayText) setPreferredSize(new Dimension(d.width, PREF_HEIGHT));
		setFocusable(false);
	}

	private void init(boolean displayText)
	{
		setPreferredSize(new Dimension(PREF_HEIGHT, PREF_HEIGHT));
		if (!displayText) setText(null);
		setMnemonic(0);
		setOpaque(false);
		setRequestFocusEnabled(false);
	}

	public ToolbarButton(String text, Icon ico)
	{
		super(text, ico);
		Dimension d = getPreferredSize();
		init(text != null);
		if (text != null)
		{
			setPreferredSize(new Dimension(d.width, PREF_HEIGHT));
		}
	}

	public ToolbarButton(String text)
	{
		super(text);
		Dimension d = getPreferredSize();
		init(true);
		setPreferredSize(new Dimension(d.width, PREF_HEIGHT));
	}

	/*
	 * @see javax.swing.AbstractButton#getMnemonic()
	 */
	@Override
	public int getMnemonic()
	{
		return 0;
	}

//	public ToolbarButton(Action action)
//	{
//		super();
//		if (action instanceof IProvideButtonModel)
//		{
//			setModel(((IProvideButtonModel)action).getModel());
//		}
//		setPreferredSize(new Dimension(24,24));
//		setAction(action);
//		setText(null);
//		setMnemonic(0);
//		setOpaque(false);
//		setRequestFocusEnabled(false);
//	}
}
