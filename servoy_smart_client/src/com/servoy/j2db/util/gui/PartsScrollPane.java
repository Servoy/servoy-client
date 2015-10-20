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


import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicBorders;

import com.servoy.j2db.smart.dataui.StyledEnablePanel;
import com.servoy.j2db.ui.IScrollPane;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.EnableScrollPanel;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
public class PartsScrollPane extends EnablePanel implements javax.swing.plaf.UIResource, IScrollPane
{
	protected StyledEnablePanel innerPanel;
	protected FixedJScrollPane scroll;
	protected EnablePanel scrollColumnHeaderPanel;

	public PartsScrollPane()
	{
		setLayout(new VerticalBorderLayout());
		innerPanel = new StyledEnablePanel();
		innerPanel.setLayout(new BorderLayout());
		add(innerPanel, BorderLayout.CENTER);
		scroll = new PartsScroller();
		setBorder(BasicBorders.getTextFieldBorder());
		//    	setBorder(scroll.getBorder());//couses excpetion
		scroll.setBorder(BorderFactory.createEmptyBorder());
		innerPanel.add(scroll, BorderLayout.CENTER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		scrollColumnHeaderPanel = new EnablePanel();
		scrollColumnHeaderPanel.setLayout(new BorderLayout());
		scrollColumnHeaderPanel.setOpaque(false);
		scroll.setColumnHeaderView(scrollColumnHeaderPanel);
		if (scroll.getColumnHeader() != null) scroll.getColumnHeader().setOpaque(false);

		setOpaque(true);
	}

	class PartsScroller extends EnableScrollPanel
	{

		@Override
		public JScrollBar createHorizontalScrollBar()
		{
			return new EditListScrollBar(this, Adjustable.HORIZONTAL);
		}

		@Override
		public JScrollBar createVerticalScrollBar()
		{
			return new EditListScrollBar(this, Adjustable.VERTICAL);
		}


		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.Component#processMouseWheelEvent(java.awt.event.MouseWheelEvent)
		 */
		@Override
		protected void processMouseWheelEvent(MouseWheelEvent e)
		{
			int value = getVerticalScrollBar().getValue();
			super.processMouseWheelEvent(e);
			if (value == getVerticalScrollBar().getValue())
			{
				// if nothing happened
				Container p = getParent();
				while (p != null && !(p instanceof PartsScroller))
				{
					p = p.getParent();
				}

				if (p instanceof PartsScroller)
				{
					p.dispatchEvent(new MouseWheelEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), e.getX(), e.getY(), e.getXOnScreen(),
						e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e.getWheelRotation()));
				}
			}
		}

		//Very special ScrollBar implementation which send its change only on mouse release must be innerclass
		protected class EditListScrollBar extends ScrollBar implements ISkinnable
		{
			private static final long serialVersionUID = 1L;
			protected JScrollPane scroller;
			protected int iValue = -1;

			public EditListScrollBar(JScrollPane scroll, int orientation)
			{
				super(orientation);
				this.scroller = scroll;
			}

			@Override
			public int getUnitIncrement(int direction)
			{
				int unitInc = super.getUnitIncrement(direction);
				if (unitInc <= 5)
				{
					unitInc = 20;
				}
				return unitInc;
			}

			@Override
			public void setUI(ComponentUI ui)
			{
				super.setUI(ui);
			}

			private final boolean continuesScrolling = false;

			/**
			 * @see Adjustable#setValue(int)
			 */
			@Override
			public void setValue(int v)
			{
				if (iValue == -1)
				{
					super.setValue(v);
				}
				else
				{
					iValue = v;
					if (continuesScrolling)
					{
						super.setValue(v);
					}
				}
			}

			/**
			 * @see Component#processMouseMotionEvent(MouseEvent)
			 */
			@Override
			protected void processMouseMotionEvent(MouseEvent e)
			{
				if (iValue == -1 && e.getID() == MouseEvent.MOUSE_DRAGGED)
				{
					iValue = getValue();
				}
				super.processMouseMotionEvent(e);
			}

			/**
			 * @see Component#processMouseEvent(MouseEvent)
			 */
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (e.getID() == MouseEvent.MOUSE_PRESSED)
				{
					if (scroller.getViewport().getView() instanceof CellEditorListener)
					{
						((CellEditorListener)scroller.getViewport().getView()).editingStopped(null);
						super.requestFocus();
					}
				}
				if (iValue != -1 && e.getID() == MouseEvent.MOUSE_RELEASED)
				{
					super.setValue(iValue);
					iValue = -1;
				}
				super.processMouseEvent(e);
			}
		}
	}

	public JComponent getTitleHeader()
	{
		return titleHeader;
	}

	protected JComponent titleHeader;

	public void setTitleHeader(JComponent c)
	{
		if (c != null)
		{
			if (titleHeader != null)
			{
				remove(titleHeader);
			}
			add(c, BorderLayout.NORTH);
			this.titleHeader = c;
		}
	}

	public JComponent getHeader()
	{
		return header;
	}

	protected JComponent header;

	public void setHeader(JComponent c)
	{
		if (c != null)
		{
			removeOldComponent(scrollColumnHeaderPanel, c.getName());
			if (header != null)
			{
				scrollColumnHeaderPanel.remove(header);
			}
			scrollColumnHeaderPanel.add(c, BorderLayout.CENTER);
			scrollColumnHeaderPanel.validate();
			this.header = c;
			// need this to have the right bgcolor in the header above the content scrollbar
			scroll.setBackground(this.header.getBackground());
		}
	}

	public void setViewportView(JComponent c)
	{
		if (c != null)
		{
			scroll.setViewportView(c);
			// need this to have the right bgcolor in the header above the content scrollbar
			scroll.setBackground(header != null ? header.getBackground() : scroll.getViewport().getBackground());
		}
	}

	public JViewport getViewport()
	{
		if (scroll != null)
		{
			return scroll.getViewport();
		}
		return null;
	}

	public JScrollBar getVerticalScrollBar()
	{
		return scroll.getVerticalScrollBar();
	}

	public JScrollBar getHorizontalScrollBar()
	{
		return scroll.getHorizontalScrollBar();
	}

	public void setHorizontalScrollBarPolicy(int policy)
	{
		scroll.setHorizontalScrollBarPolicy(policy);
	}

	public void setVerticalScrollBarPolicy(int policy)
	{
		scroll.setVerticalScrollBarPolicy(policy);
	}

	public JComponent getLeadingGrandSummary()
	{
		return leadingGrandSummary;
	}

	protected JComponent leadingGrandSummary;

	public void setLeadingGrandSummary(JComponent c)
	{
		if (c != null)
		{
			removeOldComponent(scrollColumnHeaderPanel, c.getName());
			if (leadingGrandSummary != null)
			{
				scrollColumnHeaderPanel.remove(leadingGrandSummary);
			}
			scrollColumnHeaderPanel.add(c, BorderLayout.SOUTH);
			scrollColumnHeaderPanel.validate();
			this.leadingGrandSummary = c;
		}
	}

	public JComponent getTrailingGrandSummary()
	{
		return trailingGrandSummary;
	}

	protected JComponent trailingGrandSummary;

	public void setTrailingGrandSummary(JComponent c)
	{
		if (c != null)
		{
			if (trailingGrandSummary != null)
			{
				innerPanel.remove(trailingGrandSummary);
			}
			innerPanel.add(c, BorderLayout.SOUTH);
			innerPanel.validate();
			this.trailingGrandSummary = c;
		}
	}

	public JComponent getFooter()
	{
		return footer;
	}

	protected JComponent footer;

	public void setFooter(JComponent c)
	{
		if (c != null)
		{
			removeOldComponent(this, c.getName());
			if (footer != null)
			{
				remove(footer);
			}
			add(c, BorderLayout.SOUTH);
			this.footer = c;
		}
	}

	public JComponent getWest()
	{
		return west;
	}

	protected JComponent west;

	public void setWest(JComponent c)
	{
		if (c != null)
		{
			add(c, BorderLayout.LINE_START);
		}
		else
		{
			if (west != null) remove(west);
		}
		this.west = c;
	}

	public void destroy()
	{
		removeAll();
		footer = null;
		header = null;
		leadingGrandSummary = null;
		trailingGrandSummary = null;
		west = null;
	}

	@Override
	public void setEnabled(boolean b)
	{
		if (west != null) west.setEnabled(b);
		if (header != null) header.setEnabled(b);
		if (footer != null) footer.setEnabled(b);
		if (leadingGrandSummary != null) leadingGrandSummary.setEnabled(b);
		if (trailingGrandSummary != null) trailingGrandSummary.setEnabled(b);
		super.setEnabled(b);
	}

	protected void removeOldComponent(JPanel panel, String name)
	{
		if (Utils.stringIsEmpty(name))
		{
			return;
		}
		Component[] components = panel.getComponents();
		for (Component element : components)
		{
			if (!Utils.stringIsEmpty(element.getName()) && name.equals(element.getName()))
			{
				panel.remove(element);
				break;
			}
		}
	}

	// Just make sure that when the opacity is set, it gets propagated to all child parts.
	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		if (innerPanel != null) innerPanel.setOpaque(isOpaque);
		if (scroll != null)
		{
			scroll.setOpaque(isOpaque);
			if (scroll.getViewport() != null)
			{
				scroll.getViewport().setOpaque(isOpaque);
				JComponent a = (JComponent)scroll.getViewport().getView();
				if (a != null)
				{
					a.setOpaque(isOpaque);
					if (!isOpaque) a.setBackground(new Color(0, 0, 0, 254));
				}
			}
			if (scroll.getColumnHeader() != null)
			{
				scroll.getColumnHeader().setOpaque(isOpaque);
			}
		}
		if (west != null) west.setOpaque(isOpaque);
		if (titleHeader != null) titleHeader.setOpaque(isOpaque);
		if (header != null) header.setOpaque(isOpaque);
		if (footer != null) footer.setOpaque(isOpaque);
		if (leadingGrandSummary != null) leadingGrandSummary.setOpaque(isOpaque);
		if (trailingGrandSummary != null) trailingGrandSummary.setOpaque(isOpaque);
	}
}
