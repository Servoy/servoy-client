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


import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IEditListener;
import com.servoy.j2db.ui.IEditProvider;

/**
 * Listener object for various listeners influencing the edit state
 * @author jblok
 */
public class EditProvider implements FocusListener, PropertyChangeListener, ItemListener, ActionListener, DocumentListener, DropTargetListener, IEditProvider
{
	private boolean editable = true;
	private int isAdjusting;
	private final boolean isDocumentListener;
	private boolean documentChanged;
	private IEditListener listner;
	private final IDisplayData src;
	private final IApplication application;

	public EditProvider(IDisplayData s)
	{
		this(s, false);
	}

	public EditProvider(IDisplayData s, IApplication application)
	{
		this(s, false, application);
	}

	public void resetState()
	{
		documentChanged = false;
	}

	public EditProvider(IDisplayData s, boolean documentListener)
	{
		this(s, documentListener, null);
	}

	public EditProvider(IDisplayData s, boolean documentListener, IApplication application)
	{
		src = s;
		isDocumentListener = documentListener;
		isAdjusting = 0;
		documentChanged = false;
		this.application = application;
	}

	public void addEditListener(IEditListener l)
	{
		listner = l;
	}

	public IEditListener getEditListener()
	{
		return listner;
	}

	public void focusGained(FocusEvent e)
	{
		JRootPane pane = SwingUtilities.getRootPane((JComponent)src);
		if (pane == null)
		{
//			Component comp = (Component) e.getSource();
//			if(comp != null)
//			{
//				comp.requestFocus();
//			}
		}
		else
		{
			if (((JComponent)src).isEnabled())
			{
				if (src instanceof IDisplay && ((IDisplay)src).isReadOnly())
				{
					return;
				}
				listner.startEdit(src);
			}
		}
	}

	public void focusLost(FocusEvent e)
	{
//		// only document listeners when documentChanged will do commit edit on focuslost
//		// don't turn this off because it is needed in textarea ect

		if (!isAdjusting() && isDocumentListener && documentChanged)
		{
			documentChanged = false;
			listner.commitEdit(src);
		}
	}

	public void propertyChange(PropertyChangeEvent e)
	{
		if (!isAdjusting())
		{
			listner.commitEdit(src);
			documentChanged = false;
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		// careful: e.getSource can be null (DataSpinner)
		if (e.getSource() instanceof JComboBox && e.getStateChange() == ItemEvent.DESELECTED) return;
		if (e.getSource() instanceof JCheckBox && application != null)
		{
			if (!isAdjusting())
			{
				// see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6998897 , http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6988854
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						// i have to start edit first because an edit in a list view of a checkbox
						// doesn't have always focusGained (which starts the edit)
						listner.startEdit(src);
						listner.commitEdit(src);
					}
				});
			}
		}
		else
		{
			if (!isAdjusting())
			{
				// i have to start edit first because an edit in a list view of a checkbox
				// doesn't have always focusGained (which starts the edit)
				listner.startEdit(src);
				listner.commitEdit(src);
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (!isAdjusting())
		{
			listner.commitEdit(src);
			documentChanged = false;
		}
	}

	public void setAdjusting(boolean b)
	{
		if (editable)
		{
			if (b)
			{
				isAdjusting++;
			}
			else if (isAdjusting > 0)
			{
				isAdjusting--;
			}
		}
	}

	public boolean isAdjusting()
	{
		return (isAdjusting != 0);
	}

	/**
	 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent e)
	{
		if (!isAdjusting())
		{
			documentChanged = true;
		}
	}

	/**
	 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	public void insertUpdate(DocumentEvent e)
	{
		if (!isAdjusting())
		{
			documentChanged = true;
		}
	}

	/**
	 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	public void removeUpdate(DocumentEvent e)
	{
		if (!isAdjusting())
		{
			documentChanged = true;
		}
	}

	public void commitData()
	{
		if (!isAdjusting())
		{
			listner.startEdit(src);
			listner.commitEdit(src);
		}
	}

	//if the IDisplayData recieves stopEditing() and if it should commit it's current value and call this method
	public void forceCommit()
	{
		if (isDocumentListener && !documentChanged)
		{
			return;
		}

		documentChanged = false;
		listner.startEdit(src);//make sure it is started,otherwise changes are not seen
		listner.commitEdit(src);
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent) public void valueChanged(ListSelectionEvent e) {
	 *      System.out.println(((JList)e.getSource()).getSelectedValue()); if (!isAdjusting) { listner.startEdit(e); listner.commitEdit(e); } }
	 */
	public void setEditable(boolean b)
	{
		editable = b; //prevents isAdjusting being set
		if (!editable)
		{
			isAdjusting = 1; //this prevents any change
		}
		else
		{
			isAdjusting = 0;
		}
	}

	public boolean isEditable()
	{
		return editable;
	}

	public void startEdit()
	{
		listner.startEdit(src);
	}


	public void dragEnter(DropTargetDragEvent dtde)
	{
		JRootPane pane = SwingUtilities.getRootPane((JComponent)src);
		if (pane == null)
		{
//			Component comp = (Component) e.getSource();
//			if(comp != null)
//			{
//				comp.requestFocus();
//			}
		}
		else
		{
			startEdit();
		}
	}

	public void dragExit(DropTargetEvent dte)
	{
	}

	public void dragOver(DropTargetDragEvent dtde)
	{
	}

	public void drop(DropTargetDropEvent dtde)
	{
	}

	public void dropActionChanged(DropTargetDragEvent dtde)
	{
	}
}