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
package com.servoy.j2db.gui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.smart.dataui.DataCalendar;
import com.servoy.j2db.smart.dataui.DataChoice;
import com.servoy.j2db.smart.dataui.DataComboBox;
import com.servoy.j2db.smart.dataui.DataImgMediaField;
import com.servoy.j2db.smart.dataui.DataTextArea;
import com.servoy.j2db.smart.dataui.DataTextEditor;
import com.servoy.j2db.smart.dataui.ScriptButton;
import com.servoy.j2db.smart.dataui.SpecialTabPanel;

/**
 * Listens to focus change in smart client and makes sure that the focused
 * component is always visible on the screen.
 * 
 * @author gerzse
 */
public class GlobalAutoScrollerFocusListener implements PropertyChangeListener
{

	public void propertyChange(PropertyChangeEvent e)
	{
		if ("focusOwner".equals(e.getPropertyName())) //$NON-NLS-1$
		{
			Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

//			Object newVal = owner;
//			if (newVal != null)
//			{
//				System.out.print("======== ");
//				System.out.println(newVal.getClass().getCanonicalName());
//				if (newVal instanceof Component)
//				{
//					Component c = (Component)newVal;
//					while (c != null)
//					{
//						String name = c.getName();
//						if (c instanceof SwingForm)
//							name = ((SwingForm)c).getController().getName();
//						System.out.println("->" + c.getClass().getCanonicalName() + "/" + name);
//						c = c.getParent();
//					}
//				}
//				System.out.println("===========");
//			}

			if (owner != null)
			{
//				System.out.println("focus moved to ->" + owner.getClass().getCanonicalName() + "/" + owner + "<-");
//				if (owner instanceof Component) System.out.println("\towner name is: " + (owner).getName());
//				Container focusRoot = KeyboardFocusManager.getCurrentKeyboardFocusManager().getCurrentFocusCycleRoot();
//				System.out.println("focus cycle root is now ->" + focusRoot.getClass().getCanonicalName() + "/" + focusRoot + "<-");

				owner = getEventualParentScrollPane(owner);
				/* Auto scroll only when our specific fields receive focus. */
				if (owner instanceof IDisplayData || owner instanceof ScriptButton)
				{
					/* Find a scrollable parent, if any. */
					JComponent scrollableParent = findScrollableParent(owner);
					while (scrollableParent != null)
					{
						/* Compute the coordinates of the owner relative to the scrollable parent. */
						Rectangle r = owner.getBounds();
						Component c = owner.getParent();
						while (c != scrollableParent && c != null)
						{
							Rectangle s = c.getBounds();
							r.setLocation(r.x + s.x, r.y + s.y);
							c = c.getParent();
						}
						scrollableParent.scrollRectToVisible(r);


						// If we are inside a tab panel, then we go up one level and scroll everything again,
						// because the tab panel itself can be out of the screen now.
						scrollableParent = findTabPanelParent(scrollableParent);
						if (scrollableParent != null) scrollableParent = findScrollableParent(scrollableParent.getParent());
					}
				}
			}
		}
	}

	// Checks two levels of parenthood, in order to detect special controls like
	// combobox, radios, checkboxes, etc.
	private Component getEventualParentScrollPane(Component owner)
	{
		if (owner != null)
		{
			Component parent = owner.getParent();
			if (parent != null)
			{
				if ((parent instanceof DataCalendar) || (parent instanceof DataComboBox)) return parent;
				if (parent instanceof JViewport)
				{
					parent = parent.getParent();
					if ((parent instanceof DataChoice) || (parent instanceof DataTextArea) || (parent instanceof DataTextEditor) ||
						(parent instanceof DataImgMediaField)) return parent;
				}
			}
		}
		return owner;
	}

	private JComponent findTabPanelParent(Component c)
	{
		while ((c != null) && !(c instanceof SpecialTabPanel))
			c = c.getParent();
		if (c != null) return (JComponent)c;
		else return null;
	}

	private JComponent findScrollableParent(Component owner)
	{
		if (owner == null) return null;
		Component c = owner.getParent();
		Component prev = null;

		//if it is an element that has a column header (ex ListView with header) , skip the first JScrollPane
		while ((c != null) &&
			(!((c instanceof JScrollPane) && (prev != ((JScrollPane)c).getColumnHeader()) && (prev != ((JScrollPane)c).getRowHeader()) &&
				(prev != ((JScrollPane)c).getCorner(ScrollPaneConstants.LOWER_LEFT_CORNER)) &&
				(prev != ((JScrollPane)c).getCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER)) &&
				(prev != ((JScrollPane)c).getCorner(ScrollPaneConstants.UPPER_LEFT_CORNER)) && (prev != ((JScrollPane)c).getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER)))))
		{
			prev = c;
			c = c.getParent();
		}
		if (c != null)
		{
			JScrollPane sp = (JScrollPane)c;
			Component view = sp.getViewport().getView();
			if (view instanceof JComponent) return (JComponent)view;
			else return null;
		}
		else return null;
	}
}
