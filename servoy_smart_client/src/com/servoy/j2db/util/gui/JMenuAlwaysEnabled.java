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

import javax.swing.Action;
import javax.swing.JMenu;

import com.servoy.j2db.LAFManager;

/**
 * Class to overcome the not back enabled menu on mac
 * 
 * @author jcompagner
 */
public class JMenuAlwaysEnabled extends JMenu
{
	public JMenuAlwaysEnabled()
	{
		super();
	}

	/**
	 * @param s
	 */
	public JMenuAlwaysEnabled(String s)
	{
		super(s);
	}

	/**
	 * @param a
	 */
	public JMenuAlwaysEnabled(Action a)
	{
		super(a);
	}

	/**
	 * @param s
	 * @param b
	 */
	public JMenuAlwaysEnabled(String s, boolean b)
	{
		super(s, b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JMenuItem#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean b)
	{
		if (!b && LAFManager.isUsingAppleLAF())
		{
			RuntimeException re = new RuntimeException();
			StackTraceElement[] e = re.getStackTrace();
			if (e[1].getClassName().indexOf("apple.laf") != -1) //$NON-NLS-1$
			{
				return;
			}
		}
		super.setEnabled(b);
	}
}
