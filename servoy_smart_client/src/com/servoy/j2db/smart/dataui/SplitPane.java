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
package com.servoy.j2db.smart.dataui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.ui.ISupportReadOnly;
import com.servoy.j2db.util.EnablePanel;

/**
 * This class represents a split pane in the smart client
 * 
 * @author gboros
 */
public class SplitPane extends JSplitPane implements ISupportReadOnly
{
	private boolean readOnly;
	private final boolean design;

	public SplitPane(int orient, boolean design)
	{
		this.design = design;
		int o = JSplitPane.HORIZONTAL_SPLIT;
		if (orient == TabPanel.SPLIT_VERTICAL) o = JSplitPane.VERTICAL_SPLIT;
		setOrientation(o);
		setLeftComponent(new JLabel("")); //$NON-NLS-1$
		setRightComponent(new JLabel("")); //$NON-NLS-1$
	}

	public Component[] getSplitComponents()
	{
		return new Component[] { getLeftComponent(), getRightComponent() };
	}

	public void setLeftForm(FormLookupPanel flp)
	{
		flp.setOpaque(isOpaque());
		int dividerLoc = getDividerLocation();
		setLeftComponent(flp);
		setDividerLocation(dividerLoc);
	}

	public void setRightForm(FormLookupPanel flp)
	{
		flp.setOpaque(isOpaque());
		int dividerLoc = getDividerLocation();
		setRightComponent(flp);
		setDividerLocation(dividerLoc);
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	@Override
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		if (getLeftComponent() != null) getLeftComponent().setEnabled(b);
		if (getRightComponent() != null) getRightComponent().setEnabled(b);
	}

	public void setReadOnly(boolean b)
	{
		if (getLeftComponent() instanceof EnablePanel) ((EnablePanel)getLeftComponent()).setReadOnly(b);
		if (getRightComponent() instanceof EnablePanel) ((EnablePanel)getRightComponent()).setReadOnly(b);

		readOnly = b;
	}

	public void setLeftFormMinSize(int minSize)
	{
		int width, height;
		if (orientation == JSplitPane.HORIZONTAL_SPLIT)
		{
			width = minSize;
			height = Integer.MAX_VALUE;
		}
		else
		{
			width = Integer.MAX_VALUE;
			height = minSize;
		}
		getLeftComponent().setMinimumSize(new Dimension(width, height));
	}

	public int getLeftFormMinSize()
	{
		if (orientation == JSplitPane.HORIZONTAL_SPLIT)
		{
			return getLeftComponent().getMinimumSize().width;
		}
		else
		{
			return getLeftComponent().getMinimumSize().height;
		}
	}

	public void setRightFormMinSize(int minSize)
	{
		int width, height;
		if (orientation == JSplitPane.HORIZONTAL_SPLIT)
		{
			width = minSize;
			height = Integer.MAX_VALUE;
		}
		else
		{
			width = Integer.MAX_VALUE;
			height = minSize;
		}
		getRightComponent().setMinimumSize(new Dimension(width, height));
	}

	public int getRightFormMinSize()
	{
		if (orientation == JSplitPane.HORIZONTAL_SPLIT)
		{
			return getRightComponent().getMinimumSize().width;
		}
		else
		{
			return getRightComponent().getMinimumSize().height;
		}
	}

	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		if (!settingUI && !design && !isOpaque)
		{
			SplitPaneUI invisibleUI = new InvisibleSplitPaneUI();
			setUI(invisibleUI);
			revalidate();
		}
	}

	@Override
	public void setBackground(Color bgColor)
	{
		super.setBackground(bgColor);
		if (!settingUI && !design && bgColor != null && !bgColor.equals(UIManager.getColor("SplitPane.background"))) //$NON-NLS-1$

		{
			SplitPaneUI invisibleUI = new InvisibleSplitPaneUI();
			setUI(invisibleUI);
			revalidate();
		}
	}

	private boolean settingUI;

	@Override
	public void setUI(SplitPaneUI ui)
	{
		settingUI = true;
		super.setUI(ui);
		settingUI = false;
	}

	private class InvisibleSplitPaneUI extends BasicSplitPaneUI
	{
		public InvisibleSplitPaneUI()
		{
		}

		@Override
		protected void installDefaults()
		{
			super.installDefaults();
		}

		@Override
		public BasicSplitPaneDivider createDefaultDivider()
		{
			BasicSplitPaneDivider d = new BasicSplitPaneDivider(this)
			{
				@Override
				public void paint(Graphics g)
				{
				}
			};
			return d;
		}
	}


}
