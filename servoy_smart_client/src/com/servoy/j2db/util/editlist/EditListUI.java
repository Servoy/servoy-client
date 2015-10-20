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
package com.servoy.j2db.util.editlist;


import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicListUI;

import com.servoy.j2db.ui.BaseEventExecutor;
import com.servoy.j2db.ui.IEventExecutor;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IFormUI;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.ISupportEventExecutor;
import com.servoy.j2db.util.UIUtils;

public class EditListUI extends BasicListUI
{
	public EditListUI()
	{
		super();
	}

	@Override
	public void installUI(JComponent c)
	{
		super.installUI(c);
		list.getCellRenderer();
	}

	@Override
	protected MouseInputListener createMouseInputListener()
	{
		return new MouseInputHandler();
	}

	public void redrawList() //override / not overload!!
	{
		if (list.isVisible()) //if not visible just forget it :-)
		{
			list.revalidate();
			list.repaint();
		}
	}

	@Override
	public int locationToIndex(JList list, Point location)
	{
		int index = super.locationToIndex(list, location);
		int nrows = list.getModel().getSize();
		if (index == (nrows - 1))
		{
			Rectangle rect = getCellBounds(list, 0, index);
			if (rect == null || location == null || !rect.contains(location))
			{
				return -1;
			}
		}
		return index;
	}

	private Image bufferImage = null;

	private void createCachedRendererImage(int x, int y, int width, int height)
	{
		ListCellRenderer lcr = list.getCellRenderer();
		Component rendererComponent = lcr.getListCellRendererComponent(list, null, 0, false, false);
		rendererComponent.setEnabled(false);
		//		rendererPane.add(rendererComponent);
		//		rendererComponent.invalidate();
		//		rendererPane.validate();

		bufferImage = rendererPane.createImage(width, height);

		//		rendererComponent.setLocation(0,0);
		if (bufferImage != null)
		{
			Graphics g2 = bufferImage.getGraphics();
			rendererPane.paintComponent(g2, rendererComponent, null, x, y, width, height);
		}
		//		rendererComponent.paint(g2); // paint changes in component settings
		//		g2.setClip(0,0,width,height);

		//		rendererPane.remove(rendererComponent);
		rendererComponent.setEnabled(true);
	}

	private boolean missedPaints = false;

	/**
	 * Paint one List cell: compute the relevant state, get the "rubber stamp" cell renderer component, and then use the CellRendererPane to paint it.
	 * Subclasses may want to override this method rather than paint().
	 * 
	 * @see #paint
	 */
	@Override
	protected void paintCell(Graphics g, int row, Rectangle rowBounds, ListCellRenderer cellRenderer, ListModel dataModel, ListSelectionModel selModel,
		int leadIndex)
	{
		JEditList eList = (JEditList)list;
		if (eList.getEditingRow() == row)
		{
			Component comp = eList.getEditorComponent();
			Dimension dim = comp.getSize();
			if (dim.width != rowBounds.width || dim.height != rowBounds.height)
			{
				comp.setSize(rowBounds.width, rowBounds.height);
				comp.doLayout();
			}
			return;
		}

		if (!eList.isRendererSameAsEditor())
		{
			super.paintCell(g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
			return;
		}
		int cx = rowBounds.x;
		int cy = rowBounds.y;
		int cw = rowBounds.width;
		int ch = rowBounds.height;

		if (eList.isEditing() && bufferImage != null)
		{
			//stop painting if editing
			g.drawImage(bufferImage, cx, cy, list);
			missedPaints = true;
		}
		else
		{
			if (missedPaints)
			{
				missedPaints = false;
				list.paintImmediately(0, 0, list.getWidth(), list.getHeight());
				return;
			}
			Object value = dataModel.getElementAt(row);
			boolean cellHasFocus = list.hasFocus() && (row == leadIndex);
			boolean isSelected = selModel.isSelectedIndex(row);

			Component rendererComponent = cellRenderer.getListCellRendererComponent(list, value, row, isSelected, cellHasFocus);
			rendererPane.paintComponent(g, rendererComponent, list, cx, cy, cw, ch, true);
		}
		if (bufferImage == null) createCachedRendererImage(cx, cy, cw, ch);
	}

	/**
	 * Mouse input, and focus handling for JList. An instance of this class is added to the appropriate java.awt.Component lists at installUI() time. Note
	 * keyboard input is handled with JComponent KeyboardActions, see installKeyboardActions().
	 * <p>
	 * <strong>Warning:</strong> Serialized objects of this class will not be compatible with future Swing releases. The current serialization support is
	 * appropriate for short term storage or RMI between applications running the same version of Swing. As of 1.4, support for long term storage of all
	 * JavaBeans<sup><font size="-2">TM</font></sup> has been added to the <code>java.beans</code> package. Please see {@link java.beans.XMLEncoder}.
	 * 
	 * @see #createMouseInputListener
	 * @see #installKeyboardActions
	 * @see #installUI
	 */
	public class MouseInputHandler implements MouseInputListener
	{
		// Component receiving mouse events during editing.
		// May not be editorComponent.
		private Component dispatchComponent;
		private boolean selectedOnPress;

//		private MouseEvent event;

		public void mouseClicked(MouseEvent e)
		{
			// see comment Container -> processMouseEvent; jdk 1.6_13
			// 4508327: MOUSE_CLICKED should never be dispatched to a Component
			// other than that which received the MOUSE_PRESSED event.  If the
			// mouse is now over a different Component, don't dispatch the event.
			// The previous fix for a similar problem was associated with bug
			// 4155217.
		}

		public void mouseEntered(MouseEvent e)
		{
		}

		public void mouseExited(MouseEvent e)
		{
		}

		public void mousePressed(MouseEvent e)
		{
			if (e.isConsumed())
			{
				selectedOnPress = false;
				return;
			}
			selectedOnPress = true;
			adjustFocusAndSelection(e); // this might also repost the event to correct child of editor component
		}

		void adjustFocusAndSelection(final MouseEvent e)
		{
			if (!list.isEnabled())
			{
				return;
			}


			JEditList lst = (JEditList)list;
			if (lst.isEditing())
			{
				IEditListEditor editor = lst.getCellEditor();
				if (!editor.stopCellEditing()) return;
			}
			else if (!lst.hasFocus()) lst.requestFocus();

			/*
			 * Request focus before updating the list selection. This implies that the current focus owner will see a focusLost() event before the lists
			 * selection is updated IF requestFocus() is synchronous (it is on Windows). See bug 4122345
			 */
			//			int row = convertLocationToModel(e.getX(), e.getY());
			if (list.getLayoutOrientation() == JList.HORIZONTAL_WRAP || list.getLayoutOrientation() == JList.VERTICAL_WRAP)
			{
				// make sure layout state is correct before calculating the editing row
				updateLayoutState();
			}

			final int row = locationToIndex(list, new Point(e.getX(), e.getY()));
			if (row == -1)
			{
				list.requestFocus();
				return;
			}


			int oldSelectedRow = list.getSelectedIndex();

			if (row != -1)
			{
				// boolean adjusting = (e.getID() == MouseEvent.MOUSE_PRESSED) ? true : false;
				//				list.setValueIsAdjusting(adjusting);
				int anchorIndex = list.getAnchorSelectionIndex();
				if (UIUtils.isCommandKeyDown(e))
				{
					if (list.isSelectedIndex(row))
					{
						list.removeSelectionInterval(row, row);
					}
					else
					{
						list.addSelectionInterval(row, row);
					}
				}
				else if (e.isShiftDown() && (anchorIndex != -1))
				{
					list.setSelectionInterval(anchorIndex, row);
				}
				else
				{
					list.setSelectedIndex(row);
					if (row != list.getSelectedIndex())
					{
						// selected index not actually changed, probably due to validation failed
						return;
					}
				}
			}

			if (oldSelectedRow != row && ((JEditList)list).getMustSelectFirst())
			{
				return;
			}
			// IF you enable this then DataChoice in a editor is going wrong because MouseReleased is not followed by MousePressed.
			//			Runnable run = new Runnable()
			//			{
			//				public void run()
			//				{

			if (((JEditList)list).editCellAt(row, e))
			{
				missedPaints = false;
				setDispatchComponent(e);
				if (dispatchComponent != null && SwingUtilities.isLeftMouseButton(e)) dispatchComponent.requestFocus();
				repostEvent(e);
//				if (event != null && event.getID() == MouseEvent.MOUSE_RELEASED && event.getWhen() > e.getWhen() && dispatchComponent != null)
//				{
//					repostEvent(event);
//				}
//				event = null;
			}
			else if (!list.hasFocus() && list.isRequestFocusEnabled())
			{
				list.requestFocus();
			}
			//				}
			//			};
			//			SwingUtilities.invokeLater(run);
		}

		public void mouseDragged(MouseEvent e)
		{
			if (e.isConsumed())
			{
				return;
			}
			if (!SwingUtilities.isLeftMouseButton(e))
			{
				return;
			}
			if (!list.isEnabled())
			{
				return;
			}
			if (e.isShiftDown() || UIUtils.isCommandKeyDown(e))
			{
				return;
			}

			//			int row = convertLocationToModel(e.getX(), e.getY());
			int row = list.locationToIndex(new Point(e.getX(), e.getY()));
			if (row != -1)
			{
				Rectangle cellBounds = getCellBounds(list, row, row);
				if (cellBounds != null)
				{
					list.scrollRectToVisible(cellBounds);
					//					list.setSelectionInterval(row, row);
				}
			}
		}

		public void mouseMoved(MouseEvent e)
		{
		}

		private void setDispatchComponent(MouseEvent e)
		{
			Component editorComponent = ((JEditList)list).getEditorComponent();
			Point p = e.getPoint();
			Point p2 = SwingUtilities.convertPoint(list, p, editorComponent);
			if (editorComponent instanceof Container)
			{
				if (((Container)editorComponent).getComponentCount() > 0)
				{
					dispatchComponent = ((Container)editorComponent).findComponentAt(p2.x, p2.y);
				}
				else
				{
					dispatchComponent = editorComponent;
				}
			}
			else
			{
				dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent, p2.x, p2.y);
			}

			Component eventExecutorComponent = dispatchComponent;
			while (!(eventExecutorComponent instanceof ISupportEventExecutor || eventExecutorComponent == null || eventExecutorComponent == list))
			{
				eventExecutorComponent = eventExecutorComponent.getParent();
			}

			if (eventExecutorComponent instanceof ISupportEventExecutor && eventExecutorComponent != list)
			{
				IEventExecutor executor = ((ISupportEventExecutor)eventExecutorComponent).getEventExecutor();
				if (executor instanceof BaseEventExecutor)
				{
					((BaseEventExecutor)executor).setFormName(getFormName(eventExecutorComponent));
				}
			}
//Disabled: this breaks editing stopped !!! (if clicked besides a field)
//			//try to focus a component if panel or label
//			if (/*dispatchComponent == null || disabled, becouse cannot just focus one... */
//				 (dispatchComponent != null && (dispatchComponent instanceof JPanel || dispatchComponent instanceof JLabel)))
//			{
//				dispatchComponent = ((DefaultFocusManager) DefaultFocusManager.getCurrentManager()).getFirstComponent((Container) editorComponent);
//			}
		}

		private boolean repostEvent(MouseEvent e)
		{
			// Check for isEditing() in case another event has
			// caused the editor to be removed. See bug #4306499.
			if (dispatchComponent == null || !((JEditList)list).isEditing())
			{
//				event = e;
				// this is a fix for issue 110629
				JEditList lst = (JEditList)list;
				IEditListEditor editor = lst.getCellEditor();
				if (editor != null)
				{
					Point p = e.getPoint();
					int row = locationToIndex(list, new Point(e.getX(), e.getY()));
					if (row >= 0)
					{
						Component comp = editor.getListCellEditorComponent(lst, lst.getModel().getElementAt(row), true, row);
						if (comp != null)
						{
							Rectangle recOldBounds = comp.getBounds();
							comp.setBounds(lst.getCellBounds(row, row));
							boolean componentAdded = false;
							if (comp.getParent() != lst)
							{
								lst.add(comp);
								componentAdded = true;
							}
							comp.doLayout();
							Point p2 = SwingUtilities.convertPoint(lst, p, comp);
							Component dispatchComponent = null;
							if (comp instanceof Container)
							{
								dispatchComponent = ((Container)comp).findComponentAt(p2.x, p2.y);
							}
							else
							{
								dispatchComponent = SwingUtilities.getDeepestComponentAt(comp, p2.x, p2.y);
							}
							if (isFormElement(dispatchComponent))
							{
								Point p3 = SwingUtilities.convertPoint(comp, p2, dispatchComponent);
								MouseEvent e2 = new MouseEvent(dispatchComponent, e.getID(), e.getWhen(), e.getModifiers(), p3.x, p3.y, e.getClickCount(),
									e.isPopupTrigger());
								dispatchComponent.dispatchEvent(e2);
							}
							if (componentAdded)
							{
								lst.remove(comp);
							}
						}
					}
				}
				return false;
			}
			MouseEvent e2 = null;
			if ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK)
			{
				// hack around bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4988798
				Point p = SwingUtilities.convertPoint(list, new Point(e.getX(), e.getY()), dispatchComponent);
				e2 = new MouseEvent(dispatchComponent, e.getID(), e.getWhen(), e.getModifiers() | InputEvent.ALT_DOWN_MASK, p.x, p.y, e.getClickCount(),
					e.isPopupTrigger());

			}
			else
			{
				e2 = SwingUtilities.convertMouseEvent(list, e, dispatchComponent);
			}
			dispatchComponent.dispatchEvent(e2);
			return true;
		}

		private boolean isFormElement(Component component)
		{
			Component parent = component;
			while (parent != null)
			{
				if (parent instanceof ILabel || parent instanceof IFieldComponent)
				{
					return true;
				}
				parent = parent.getParent();
			}
			return false;
		}

		private String getFormName(Component component)
		{
			for (Component container = component; container != null; container = container.getParent())
			{
				if (container instanceof IFormUI)
				{
					return ((IFormUI)container).getController().getName();
				}
			}
			return null;
		}

		public void mouseReleased(MouseEvent e)
		{
			if (selectedOnPress)
			{
				selectedOnPress = false;
				repostEvent(e);
			}
			else
			{
				adjustFocusAndSelection(e); // this might also repost the event to correct child of editor component
			}
		}

	}
}