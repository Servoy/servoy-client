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
package com.servoy.j2db.smart;


import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.text.Style;
import javax.swing.text.html.StyleSheet;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IView;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.ISwingFoundSet;
import com.servoy.j2db.smart.dataui.DataCalendar;
import com.servoy.j2db.smart.dataui.DataRenderer;
import com.servoy.j2db.ui.IScriptReadOnlyMethods;
import com.servoy.j2db.util.editlist.EmptyEditListModel;
import com.servoy.j2db.util.editlist.IEditListModel;
import com.servoy.j2db.util.editlist.JEditList;

/**
 * The listview controller from mvc architecture
 * 
 * @author jblok
 */
public class ListView extends JEditList implements IView
{
	private IApplication application;
	private String rowBGColorScript;
	private List<Object> rowBGColorArgs;
	private int keyReleaseToBeIgnored;
	private StyleSheet styleSheet;
	private Style oddStyle, evenStyle;

	public ListView()
	{
		super();
		setAutoscrolls(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#processMouseEvent(java.awt.event.MouseEvent)
	 */
	@Override
	protected void processMouseEvent(MouseEvent e)
	{
		if (e.getID() == MouseEvent.MOUSE_PRESSED)
		{
			//search on which component is clicked becouse we do not want to save if clicked on field for example
			int row = getSelectedIndex();
			if (row != -1)
			{
				Point p = (e).getPoint();
				Component comp = getEditorComponent();
				if (comp != null)
				{
					Point p2 = SwingUtilities.convertPoint(this, p, comp);
					Component dispatchComponent = SwingUtilities.getDeepestComponentAt(comp, p2.x, p2.y);
					if (dispatchComponent == this || dispatchComponent == comp)
					{
						application.getFoundSetManager().getEditRecordList().stopEditing(false);
					}
				}
				else
				{
					comp = getCellEditor().getListCellEditorComponent(this, null, true, row);
					if (comp != null)
					{
						comp.setBounds(getCellBounds(row, row));
						add(comp);
						comp.doLayout();
						Point p2 = SwingUtilities.convertPoint(this, p, comp);
						Component dispatchComponent = SwingUtilities.getDeepestComponentAt(comp, p2.x, p2.y);
						remove(comp);
						if (dispatchComponent == this || dispatchComponent == comp)
						{
							application.getFoundSetManager().getEditRecordList().stopEditing(false);
						}
					}
				}
			}
		}
		super.processMouseEvent(e);
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		// if we are in find mode, no row is being edited and enter is pressed then we should enter edit mode
		// on selected row rather then perform the search (so the enter release must be marked as consumed/processed);
		// for example search for something => no results, choose modify last find in the dialog - now you must be able to enter
		// edit mode in the list to modify find criteria...
		boolean wasEditingBeforePress = isEditing();
		boolean keyEventWasUsed = super.processKeyBinding(ks, e, condition, pressed);
		if (!wasEditingBeforePress && isEditing() && ks.getKeyEventType() == KeyEvent.KEY_PRESSED)
		{
			keyReleaseToBeIgnored = ks.getKeyCode();
		}
		else if (keyReleaseToBeIgnored == ks.getKeyCode() && ks.getKeyEventType() == KeyEvent.KEY_RELEASED)
		{
			keyReleaseToBeIgnored = -1;
			return true;
		}
		else if (ks.getKeyEventType() != KeyEvent.KEY_TYPED)
		{
			keyReleaseToBeIgnored = -1; // get rid of this value, because this in only implemented for simple scenarios
			// and it shouldn't cause trouble
		}
		return keyEventWasUsed;
	}

	private final DefaultListSelectionModel EMPTY_SELECTION = new DefaultListSelectionModel();
	private final DefaultListModel EMPTY_DATA = new DefaultListModel();

	public void destroy()
	{
		setSelectionModel(EMPTY_SELECTION);
		super.setModel(EMPTY_DATA);
		removeAll();
	}

	/**
	 * @see com.servoy.j2db.IView#setModel(com.servoy.j2db.dataprocessing.IFoundSetInternal)
	 */
	public void setModel(IFoundSetInternal fs)
	{
		if (fs instanceof IEditListModel)
		{
			// Not needed anymore see FoundSet constuctor (selection model is a var of foundset)
//			ListSelectionModel sm = getSelectionModel();
//			if (sm instanceof ListDataListener)
//			{
//				getModel().removeListDataListener((ListDataListener)sm);
//			}
//			if (sm instanceof ListDataListener)
//			{
//				fs.addListDataListener((ListDataListener)sm);
//			}
			// JList clears the selection in setModel()
			setSelectionModel(EMPTY_SELECTION);
			super.setModel((ISwingFoundSet)fs);
			setSelectionModel(((ISwingFoundSet)fs).getSelectionModel());
		}
		else if (fs == null)
		{
			removeEditor();//safety
			setSelectionModel(EMPTY_SELECTION);
			super.setModel(EMPTY_DATA);
		}
	}

	/**
	 * @see com.servoy.j2db.IView#start()
	 */
	public void start(IApplication app)
	{
		application = app;
	}

	/**
	 * @see com.servoy.j2db.IView#stop()
	 */
	public void stop()
	{
		setSelectionModel(new DefaultListSelectionModel());
		setModel(new EmptyEditListModel());
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		if (isEditing())
		{
			return getCellEditor().stopCellEditing();
		}
		return true;
	}

	@Override
	public void removeEditor()
	{
		//System.out.println("REMOVING EDITOR");
		DataRenderer dr = ((DataRenderer)getEditorComponent());
		if (dr != null)
		{
			dr.stopUIEditing(true);
		}
		super.removeEditor();
	}

	public void setRowBGColorScript(String str, List<Object> args)
	{
		rowBGColorScript = str;
		rowBGColorArgs = args;
	}

	public String getRowBGColorScript()
	{
		return rowBGColorScript;
	}

	public List<Object> getRowBGColorArgs()
	{
		return rowBGColorArgs;
	}

	public boolean isDisplayingMoreThanOneRecord()
	{
		return true;
	}


	@Override
	public void setEditable(boolean editable)
	{
		super.setEditable(editable);
		ListCellRenderer cellRenderer = getCellRenderer();
		if (cellRenderer instanceof DataRenderer)
		{
			DataRenderer dr = (DataRenderer)cellRenderer;
			for (int i = 0; i < dr.getComponentCount(); i++)
			{
				Component c = dr.getComponent(i);
				if (c instanceof IScriptReadOnlyMethods) ((IScriptReadOnlyMethods)c).js_setReadOnly(!editable);
			}
			invalidate();
			repaint();
		}
	}

	@Override
	protected boolean shouldDispatchClickToButton(JButton button)
	{
		return !((button.getParent() != null) && (button.getParent() instanceof DataCalendar));
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#setStyles(javax.swing.text.html.StyleSheet, javax.swing.text.Style, javax.swing.text.Style)
	 */
	public void setStyles(StyleSheet styleSheet, Style oddStyle, Style evenStyle)
	{
		this.styleSheet = styleSheet;
		this.oddStyle = oddStyle;
		this.evenStyle = evenStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getOddStyle()
	 */
	public Style getOddStyle()
	{
		return oddStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getEvenStyle()
	 */
	public Style getEvenStyle()
	{
		return evenStyle;
	}

	/*
	 * @see com.servoy.j2db.ui.ISupportOddEvenStyling#getStyleSheet()
	 */
	public StyleSheet getStyleSheet()
	{
		return styleSheet;
	}

}
