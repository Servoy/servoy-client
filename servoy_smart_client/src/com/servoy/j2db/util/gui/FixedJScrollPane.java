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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.ViewportLayout;

import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 * 
 * Fix for bad calling if no scrollbars used ever
 */
public class FixedJScrollPane extends JScrollPane
{
	public FixedJScrollPane()
	{
		super();
	}

	public FixedJScrollPane(Component c)
	{
		super(c);
	}

	/**
	 * Fix for scrollpane not scrolleble (no vertical or horizontal scrollbar) the viewport should NOT ask the viewport component for
	 * getPreferredScrollableViewportSize but for getPreferredSize
	 */
	@Override
	protected JViewport createViewport()
	{
		return new FixedJViewport();
	}

	@Override
	public void setToolTipText(String tip)
	{
		if (!Utils.stringIsEmpty(tip))
		{
			if (!Utils.stringContainsIgnoreCase(tip, "<html")) //$NON-NLS-1$
			{
				super.setToolTipText(tip);
			}
			else if (HtmlUtils.hasUsefulHtmlContent(tip))
			{
				super.setToolTipText(tip);
			}
		}
		else
		{
			super.setToolTipText(null);
		}
	}

	static class FixedJViewport extends JViewport
	{
		@Override
		protected LayoutManager createLayoutManager()
		{
			return new FixedViewportLayout();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.JViewport#getView()
		 */
		@Override
		public Component getView()
		{
			if (getComponentCount() != 0)
			{
				return super.getView();
			}
			return null;
		}

		/**
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		@Override
		public Dimension getPreferredSize()
		{
			Dimension dim = super.getPreferredSize();
			return dim;
		}

		//private Dimension lastPreferredSize = new
		/**
		 * @see javax.swing.JViewport#getExtentSize()
		 */
		@Override
		public Dimension getExtentSize()
		{
//			Dimension pref = getPreferredSize();
			Dimension dim = super.getExtentSize();
//			System.err.println("extended: " + dim);
//			System.err.println("pref: " + pref);
//			System.err.println("view: " + getViewSize());
//			if (dim.width <= pref.width || dim.height <= pref.height)
//			{
//				return pref ;
//			}
			return dim;
		}

		/**
		 * @see java.awt.Component#setSize(java.awt.Dimension)
		 */
		@Override
		public void setSize(Dimension d)
		{
			super.setSize(d);
		}

		/**
		 * @see java.awt.Component#setSize(int, int)
		 */
		@Override
		public void setSize(int width, int height)
		{
			super.setSize(width, height);
		}

		/**
		 * @see javax.swing.JViewport#getViewSize()
		 */
		@Override
		public Dimension getViewSize()
		{
			Dimension viewSize = super.getViewSize();
			return viewSize;
		}
	}

	static class FixedViewportLayout extends ViewportLayout
	{
		@Override
		public Dimension preferredLayoutSize(Container parent)
		{
			JScrollPane scrollPane = (JScrollPane)parent.getParent();
			Component view = ((JViewport)parent).getView();

			if (view == null)
			{
				return new Dimension(0, 0);
			}
			else if (scrollPane != null && scrollPane.getHorizontalScrollBarPolicy() == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER &&
				scrollPane.getVerticalScrollBarPolicy() == ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
			{
				return view.getPreferredSize();
			}
			else if (view instanceof Scrollable)
			{
				return ((Scrollable)view).getPreferredScrollableViewportSize();
			}
			else
			{
				return view.getPreferredSize();
			}
		}
	}
}
