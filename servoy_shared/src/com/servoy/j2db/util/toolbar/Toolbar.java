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


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputListener;

/**
 * Toolbar provides a component which is useful for displaying commonly used actions. It can be dragged inside its <code>ToolbarPanel</code> to customize its
 * location.
 */
public class Toolbar extends JToolBar
/*
 * nbif compat implements MouseInputListener /*nbend
 */
{
	/** Constant of grip layout constraint. */
	private static final String GRIP = "Grip"; // NOI18N //$NON-NLS-1$
	/** Constant of action layout constraint. */
	private static final String ACTION = "Action"; // NOI18N //$NON-NLS-1$
	/** Basic toolbar height. */
	public static/*
					 * nbif compat nbelse
					 */
	final/* nbend */
	int BASIC_HEIGHT = 26;

	/**
	 * 5 pixels is tolerance of toolbar height so toolbar can be high (BASIC_HEIGHT + HEIGHT_TOLERANCE) but it will be set to BASIC_HEIGHT high.
	 */
	static int HEIGHT_TOLERANCE = 5;
	/** TOP of toolbar empty border. */
	static int TOP = 0;
	/** LEFT of toolbar empty border. */
	static int LEFT = 3;
	/** BOTTOM of toolbar empty border. */
	static int BOTTOM = 0;
	/** RIGHT of toolbar empty border. */
	static int RIGHT = 3;

	/** is toolbar floatable */
	private boolean floatable;
	/** Toolbar DnDListener */
	private DnDListener listener;
	/** Toolbar mouse listener */
	private ToolbarMouseListener mouseListener;
	/** display name of the toolbar */
	private String displayName;

	/**
	 * Create a new not floatable Toolbar with specified programmatic name and display name
	 */
	public Toolbar(String name, String displayName)
	{
		this(name, displayName, false);
	}

	/**
	 * Create a new <code>Toolbar</code>.
	 * 
	 * @param name a <code>String</code> containing the associated name
	 * @param f specified if Toolbar is floatable
	 */
	public Toolbar(String name, String displayName, boolean f)
	{
		super();
		setDisplayName(displayName);
		initAll(name, f);
		setFocusable(false);
	}

	private void initAll(String name, boolean f)
	{
		floatable = f;
		mouseListener = null;

		setName(name);

//		setFloatable (true);
//		Border border = getBorder();
		super.setFloatable(false); //never makes it really floatable
//		setBorder (new CompoundBorder (new EtchedBorder(),
//									   getBorder()));//new EmptyBorder (TOP, LEFT, BOTTOM, RIGHT)));
		setLayout(new InnerLayout(0, 0));
		putClientProperty("JToolBar.isRollover", new Boolean(true)); // NOI18N //$NON-NLS-1$
		if (f) addGrip();
	}

	@Override
	public void setEnabled(boolean b)
	{
		Component[] all = getComponents();
		for (Component element : all)
		{
			Component c = element;
			c.setEnabled(b);
		}
		super.setEnabled(b);
	}

	private boolean actAsFloatable = false;

	@Override
	public void paint(Graphics g)
	{
		actAsFloatable = floatable;
		try
		{
			super.paint(g);
		}
		finally
		{
			actAsFloatable = false;
		}
	}

	@Override
	public boolean isFloatable()
	{
		return actAsFloatable;
	}

	/** Add new Component as ACTION. */
	@Override
	public Component add(Component comp)
	{
		add(ACTION, comp);
		return comp;
	}

	/** Removes all ACTION components. */
	@Override
	public void removeAll()
	{
		super.removeAll();
		addGrip();
	}

	private ToolbarBump dragarea;

	/**
	 * When Toolbar is floatable, ToolbarGrip is added as Grip as first toolbar component
	 */
	void addGrip()
	{
		if (floatable)
		{
			dragarea = new ToolbarBump();

			if (mouseListener == null) mouseListener = new ToolbarMouseListener();

			dragarea.addMouseListener(mouseListener);
			dragarea.addMouseMotionListener(mouseListener);

			add(GRIP, dragarea);
//			addSeparator (new Dimension (4, 1));
		}
	}

	@Override
	public void setFloatable(boolean b)
	{
		if (b)
		{
			addGrip();
		}
		else if (dragarea != null)
		{
			remove(dragarea);
		}
		super.setFloatable(b);
	}

	/**
	 * Compute with HEIGHT_TOLERANCE number of rows for specific toolbar height.
	 * 
	 * @param height of some toolbar
	 * @return number of rows
	 */
	static int rowCount(int height)
	{
		int rows = 1;
		int max_height = (BASIC_HEIGHT + HEIGHT_TOLERANCE);
		while (height > max_height)
		{
			rows++;
			height -= max_height;
		}
		return rows;
	}

	/**
	 * Set DnDListener to Toolbar.
	 * 
	 * @param DndListener for toolbar
	 */
	void setDnDListener(DnDListener l)
	{
		listener = l;
	}

	/**
	 * @return Display name of this toolbar. Display name is localizable, on the contrary to the programmatic name
	 */
	public String getDisplayName()
	{
		return displayName;
	}

	/**
	 * Sets new display name of this toolbar. Display name is localizable, on the contrary to the programmatic name
	 */
	private void setDisplayName(String displayName)
	{
		//System.out.println("setting display name...");        
		this.displayName = displayName;
	}

	/**
	 * Fire drag of Toolbar
	 * 
	 * @param dx distance of horizontal dragging
	 * @param dy distance of vertical dragging
	 * @param type type of toolbar dragging
	 */
	protected void fireDragToolbar(int dx, int dy, int type)
	{
		if (listener != null) listener.dragToolbar(new DnDEvent(this, getName(), dx, dy, type));
	}

	/**
	 * Fire drop of Toolbar
	 * 
	 * @param dx distance of horizontal dropping
	 * @param dy distance of vertical dropping
	 * @param type type of toolbar dropping
	 */
	protected void fireDropToolbar(int dx, int dy, int type)
	{
		if (listener != null) listener.dropToolbar(new DnDEvent(this, getName(), dx, dy, type));
	}


	/** Toolbar mouse listener. */
	class ToolbarMouseListener implements MouseInputListener
	{
		/** Is toolbar dragging now. */
		private boolean dragging;
		/** Start point of dragging. */
		private Point startPoint;

		public ToolbarMouseListener()
		{
			dragging = false;
			startPoint = null;
		}

		/** Invoked when the mouse has been clicked on a component. */
		public void mouseClicked(MouseEvent e)
		{
		}

		/** Invoked when the mouse enters a component. */
		public void mouseEntered(MouseEvent e)
		{
		}

		/** Invoked when the mouse exits a component. */
		public void mouseExited(MouseEvent e)
		{
		}

		/** Invoked when a mouse button has been pressed on a component. */
		public void mousePressed(MouseEvent e)
		{
			startPoint = new Point(e.getX(), e.getY());
		}

		/** Invoked when a mouse button has been released on a component. */
		public void mouseReleased(MouseEvent e)
		{
			if (dragging)
			{
				fireDropToolbar(e.getX() - startPoint.x, e.getY() - startPoint.y, DnDEvent.DND_ONE);
				dragging = false;
			}
		}

		/** Invoked when a mouse button is pressed on a component and then dragged. */
		public void mouseDragged(MouseEvent e)
		{
			int m = e.getModifiers();
			int type = DnDEvent.DND_ONE;
			if (e.isControlDown()) type = DnDEvent.DND_LINE;
			else if (((m & InputEvent.BUTTON2_MASK) != 0) || ((m & InputEvent.BUTTON3_MASK) != 0)) type = DnDEvent.DND_END;
			if (startPoint == null) startPoint = new Point(e.getX(), e.getY());
			fireDragToolbar(e.getX() - startPoint.x, e.getY() - startPoint.y, type);
			dragging = true;
		}

		/**
		 * Invoked when the mouse button has been moved on a component (with no buttons no down).
		 */
		public void mouseMoved(MouseEvent e)
		{
		}
	} // end of inner class ToolbarMouseListener

	/*
	 * nbif compat public void mouseClicked (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseClicked
	 * (e); } public void mouseEntered (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseEntered (e); }
	 * public void mouseExited (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseExited (e); } public
	 * void mousePressed (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mousePressed (e); } public void
	 * mouseReleased (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseReleased (e); } public void
	 * mouseDragged (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseDragged (e); } public void
	 * mouseMoved (MouseEvent e) { if (mouseListener == null) mouseListener = new ToolbarMouseListener (); mouseListener.mouseMoved (e); } /*nbend
	 */


	/** Grip for floatable toolbar */
	private class ToolbarGrip extends JPanel
	{
		/** Horizontal gaps. */
		static final int HGAP = 1;
		/** Vertical gaps. */
		static final int VGAP = 1;
		/** Step between two grip elements. */
		static final int STEP = 1;
		/** Width of grip element. */
		static final int WIDTH = 2;

		/** Number of grip elements. */
		int columns;
		/** Minimum size. */
		Dimension dim;

		/** Create new ToolbarGrip for default number of grip elements. */
		public ToolbarGrip()
		{
			this(2);
		}

		/**
		 * Create new ToolbarGrip for specific number of grip elements.
		 * 
		 * @param col number of grip elements
		 */
		public ToolbarGrip(int col)
		{
			super();
			columns = col;
			int width = (col - 1) * STEP + col * WIDTH + 2 * HGAP;
			dim = new Dimension(width, width);
			this.setBorder(new EmptyBorder(VGAP, HGAP, VGAP, HGAP));
			this.setToolTipText(Toolbar.this.getDisplayName());
		}

		/** Paint grip to specific Graphics. */
		@Override
		public void paint(Graphics g)
		{
//			Dimension size = this.getSize();
//			int top = VGAP;
//			int bottom = size.height - 1 - VGAP;
//			int height = bottom - top;
//			g.setColor ( this.getBackground() );
//
//			for (int i = 0, x = HGAP; i < columns; i++, x += WIDTH + STEP) {
//				g.draw3DRect (x, top, WIDTH, height, true); // grip element is 3D rectangle now
//			}

		}

		/** @return minimum size */
		@Override
		public Dimension getMinimumSize()
		{
			return dim;
		}

		/** @return preferred size */
		@Override
		public Dimension getPreferredSize()
		{
			return this.getMinimumSize();
		}
	} // end of inner class ToolbarGrip

	/** Bumps for floatable toolbar */
	private class ToolbarBump extends JPanel
	{
		/** Top gap. */
		static final int TOPGAP = 2;
		/** Bottom gap. */
		static final int BOTGAP = 0;
		/** Width of bump element. */
		static final int WIDTH = 12; //was6

		/** Minimum size. */
		Dimension dim;

		/** Create new ToolbarBump. */
		public ToolbarBump()
		{
			super();
			int width = WIDTH;
			dim = new Dimension(width, width);
			this.setToolTipText(Toolbar.this.getDisplayName());
		}

		/** Paint bumps to specific Graphics. */
		@Override
		public void paint(Graphics g)
		{
//			Dimension size = this.getSize ();
//			int height = size.height - BOTGAP;
//			g.setColor (this.getBackground ());
//
//			for (int x = 0; x+1 < size.width; x+=4) {
//				for (int y = TOPGAP; y+1 < height; y+=4) {
//					g.setColor (this.getBackground ().brighter ());
//					g.drawLine (x, y, x, y);
//					if (x+5 < size.width && y+5 < height)
//						g.drawLine (x+2, y+2, x+2, y+2);
//					g.setColor (this.getBackground ().darker ().darker ());
//					g.drawLine (x+1, y+1, x+1, y+1);
//					if (x+5 < size.width && y+5 < height)
//						g.drawLine (x+3, y+3, x+3, y+3);
//				}
//			}
		}

		/** @return minimum size */
		@Override
		public Dimension getMinimumSize()
		{
			return dim;
		}

		/** @return preferred size */
		@Override
		public Dimension getPreferredSize()
		{
			return this.getMinimumSize();
		}
	} // end of inner class ToolbarBump

	/** Toolbar layout manager */
	private class InnerLayout implements LayoutManager
	{
		/** Grip component */
		private Component grip;

		/** Vector of Components */
		private final Vector actions;

		/** horizontal gap */
		private final int hgap;

		/** vertical gap */
		private final int vgap;

		/**
		 * Constructs a new InnerLayout.
		 */
		public InnerLayout()
		{
			this(5, 5);
		}

		/**
		 * Constructs a new InnerLayout with the specified gap values.
		 * 
		 * @param hgap the horizontal gap variable
		 * @param vgap the vertical gap variable
		 */
		public InnerLayout(int hgap, int vgap)
		{
			this.hgap = hgap;
			this.vgap = vgap;
			actions = new Vector();
		}

		/**
		 * Adds the specified component with the specified name to the layout.
		 */
		public void addLayoutComponent(String name, Component comp)
		{
			synchronized (comp.getTreeLock())
			{
				if (GRIP.equals(name))
				{
					grip = comp;
				}
				else if (ACTION.equals(name))
				{
					actions.addElement(comp);
				}
				else throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name); // NOI18N //$NON-NLS-1$
			}
		}

		/**
		 * Adds the specified component to the layout, using the specified constraint object.
		 */
		public void addLayoutComponent(Component comp, Object constraints)
		{
			throw new IllegalArgumentException();
		}

		/**
		 * Removes the specified component from the layout.
		 */
		public void removeLayoutComponent(Component comp)
		{
			synchronized (comp.getTreeLock())
			{
				if (grip == comp) grip = null;
				else actions.removeElement(comp);
			}
		}

		/**
		 * Calculates the preferred size dimensions for the specified panal given the components in the specified parent container.
		 */
		public Dimension preferredLayoutSize(Container target)
		{
			synchronized (target.getTreeLock())
			{
				Dimension dim = new Dimension(0, 0);
				int size = actions.size();
				if ((grip != null) && grip.isVisible())
				{
					Dimension d = grip.getPreferredSize();
					dim.width += d.width;
					dim.width += hgap;
				}
				for (int i = 0; i < size; i++)
				{
					Component comp = (Component)actions.elementAt(i);
					if (comp.isVisible())
					{
						Dimension d = comp.getPreferredSize();
						dim.width += d.width;
						dim.height = Math.max(dim.height, d.height);
						dim.width += hgap;
					}
				}
				Insets insets = target.getInsets();
				dim.width += insets.left + insets.right;
				dim.height += insets.top + insets.bottom;

				return dim;
			}
		}

		/**
		 * Calculates the minimum size dimensions for the specified panal given the components in the specified parent container.
		 */
		public Dimension minimumLayoutSize(Container target)
		{
			return preferredLayoutSize(target);
		}

		/**
		 * Calculates the maximum size dimensions for the specified panal given the components in the specified parent container.
		 */
		public Dimension maximumLayoutSize(Container target)
		{
			synchronized (target.getTreeLock())
			{
				Dimension dim = new Dimension(0, 0);
				int size = actions.size();
				if ((grip != null) && grip.isVisible())
				{
					Dimension d = grip.getPreferredSize();
					dim.width += d.width;
					dim.width += hgap;
				}
				Component last = null;
				for (int i = 0; i < size; i++)
				{
					Component comp = (Component)actions.elementAt(i);
					if (comp.isVisible())
					{
						Dimension d = comp.getPreferredSize();
						dim.width += d.width;
						dim.height = Math.max(dim.height, d.height);
						dim.width += hgap;
						last = comp;
					}
				}
				if (last != null)
				{
					Dimension prefSize = last.getPreferredSize();
					Dimension maxSize = last.getMaximumSize();
					if (prefSize != maxSize)
					{
						dim.width = dim.width - prefSize.width + maxSize.width;
						dim.height = Math.max(dim.height, maxSize.height);
					}
				}
				Insets insets = target.getInsets();
				dim.width += insets.left + insets.right;
				dim.height += insets.top + insets.bottom;

				return dim;
			}
		}

		/**
		 * Lays out the container in the specified panel.
		 */
		public void layoutContainer(Container target)
		{
			synchronized (target.getTreeLock())
			{
				Insets insets = target.getInsets();
				boolean ltr = target.getComponentOrientation().isLeftToRight();
				if (!ltr)
				{
					insets.right += insets.left;
					insets.left = insets.right - insets.left;
					insets.right -= insets.left;
				}
				Dimension dim = target.getSize();
				int fullHeight = dim.height;
				int bottom = dim.height - insets.bottom - 1;
				int top = 0 + insets.top;
				int left = insets.left;
				int maxPosition = dim.width - (insets.left + insets.right) - hgap;
				int right = dim.width - insets.right;
				maxPosition = right;
				int height = bottom - top;
				int size = actions.size();
				int w, h;
				if ((grip != null) && grip.isVisible())
				{
					Dimension d = grip.getPreferredSize();
					if (ltr)
					{
						grip.setBounds(left, top + vgap, d.width, bottom - top - 2 * vgap);
					}
					else
					{
						grip.setBounds(maxPosition - left - d.width, top + vgap, d.width, bottom - top - 2 * vgap);
					}
					grip.setBounds(left, top + vgap, d.width, bottom - top - 2 * vgap);
					left += d.width;
					left += hgap;
				}

				int iMaxHeightComp = 0;
				// First find the larges height
				for (int i = 0; i < size; i++)
				{
					Component comp = (Component)actions.elementAt(i);
					Dimension d = comp.getPreferredSize();
					iMaxHeightComp = Math.max(iMaxHeightComp, d.height);
				}

				int iY = fullHeight - (fullHeight - iMaxHeightComp) / 2 - 1;
				for (int i = 0; i < size; i++)
				{
					left += hgap;
					Component comp = (Component)actions.elementAt(i);
					Dimension d = comp.getPreferredSize();
					Dimension minSize = comp.getMinimumSize();

					w = d.width;
					h = Math.min(height, d.height);
					if ((left < maxPosition) && (left + w > maxPosition))
					{
						if (maxPosition - left >= minSize.width) w = maxPosition - left;
						else w = minSize.width;
					}

					if (target.getComponentOrientation().isLeftToRight())
					{
						comp.setBounds(left, iY - h, w, h);
					}
					else
					{
						comp.setBounds(maxPosition - left - w, iY - h, w, h);
					}
					left += d.width;
				}
			}
		}

		/**
		 * Returns the alignment along the x axis.
		 */
		public float getLayoutAlignmentX(Container target)
		{
			return 0;
		}

		/**
		 * Returns the alignment along the y axis.
		 */
		public float getLayoutAlignmentY(Container target)
		{
			return (float)0.5;
		}

		/**
		 * Invalidates the layout, indicating that if the layout manager has cached information it should ne discarded.
		 */
		public void invalidateLayout(Container target)
		{
			// check target components with local vars (grip, actions)
		}
	} // end of class InnerLayout


	/** DnDListener is Drag and Drop listener for Toolbar motion events. */
	interface DnDListener extends java.util.EventListener
	{
		/** Invoced when toolbar is dragged. */
		public void dragToolbar(DnDEvent e);

		/** Invoced when toolbar is dropped. */
		public void dropToolbar(DnDEvent e);
	} // end of interface DnDListener


	public static class ToolbarKey implements Comparable
	{
		private final int row;
		private final int rowIndex;
		private final boolean visible;
		private final Toolbar toolbar;

		public ToolbarKey(int row, int rowIndex, boolean visible, Toolbar toolbar)
		{
			this.row = row;
			this.rowIndex = rowIndex;
			this.visible = visible;
			this.toolbar = toolbar;
		}

		/*
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o)
		{
			ToolbarKey key = (ToolbarKey)o;
			int compare = row - key.row;
			if (compare == 0)
			{
				compare = rowIndex - key.rowIndex;
			}
			return compare;
		}

		/**
		 * @return Returns the row.
		 */
		public int getRow()
		{
			return row;
		}

		/**
		 * @return Returns the rowIndex.
		 */
		public int getRowIndex()
		{
			return rowIndex;
		}

		/**
		 * @return Returns the visible.
		 */
		public boolean isVisible()
		{
			return visible;
		}

		public Toolbar getToolbar()
		{
			return toolbar;
		}
	}

	/** DnDEvent is Toolbar's drag and drop event. */
	static class DnDEvent extends EventObject
	{
		/** Type of DnDEvent. Dragging with only one Toolbar. */
		public static final int DND_ONE = 1;
		/** Type of DnDEvent. Only horizontal dragging with Toolbar and it's followers. */
		public static final int DND_END = 2;
		/** Type of DnDEvent. Only vertical dragging with whole lines. */
		public static final int DND_LINE = 3;

		/** Name of toolbar where event occured. */
		/*
		 * nbif compat public nbelse
		 */
		private final/* nbend */
		String name;
		/** distance of horizontal dragging */
		/*
		 * nbif compat public nbelse
		 */
		private final/* nbend */
		int dx;
		/** distance of vertical dragging */
		/*
		 * nbif compat public nbelse
		 */
		private final/* nbend */
		int dy;
		/** Type of event. */
		/*
		 * nbif compat public nbelse
		 */
		private final/* nbend */
		int type;

		public DnDEvent(Toolbar toolbar, String name, int dx, int dy, int type)
		{
			super(toolbar);

			this.name = name;
			this.dx = dx;
			this.dy = dy;
			this.type = type;
		}

		/** @return name of toolbar where event occured. */
		public String getName()
		{
			return name;
		}

		/** @return distance of horizontal dragging */
		public int getDX()
		{
			return dx;
		}

		/** @return distance of vertical dragging */
		public int getDY()
		{
			return dy;
		}

		/** @return type of event. */
		public int getType()
		{
			return type;
		}
	} // end of class DnDEvent
} // end of class Toolbar
