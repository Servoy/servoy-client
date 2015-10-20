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
package com.servoy.j2db.util;


import java.awt.Adjustable;
import java.awt.Component;

import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.plaf.ComponentUI;

import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.gui.FixedJScrollPane;

/**
 * @author		jblok
 */
public class EnableScrollPanel extends FixedJScrollPane implements IDelegate, ISkinnable
{
	public EnableScrollPanel()
	{
		super();
	}

	public EnableScrollPanel(Component view)
	{
		super(view);
	}

	@Override
	public boolean isEnabled()
	{
		Component c = getViewport().getView();
		if (c != null) return c.isEnabled();
		return false;
	}

	@Override
	public void setEnabled(boolean b)
	{
		Component c = getViewport().getView();
		if (c != null) c.setEnabled(b);
		JViewport h = getColumnHeader();
		if (h != null)
		{
			Component ch = h.getView();
			if (ch != null) ch.setEnabled(b);
		}
//		super.setEnabled(b);
	}

	public Object getDelegate()
	{
		return getViewport().getView();
	}

	@Override
	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}

	protected class SkinScrollBar extends ScrollBar implements ISkinnable
	{
		public SkinScrollBar(int orientation)
		{
			super(orientation);
		}

		@Override
		public void setUI(ComponentUI ui)
		{
			super.setUI(ui);
		}
	}

	@Override
	public JScrollBar createHorizontalScrollBar()
	{
		return new SkinScrollBar(Adjustable.HORIZONTAL);
	}

	@Override
	public JScrollBar createVerticalScrollBar()
	{
		return new SkinScrollBar(Adjustable.VERTICAL);
	}
}