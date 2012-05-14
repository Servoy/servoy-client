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


import java.applet.Applet;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Position.Bias;

import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;
import com.servoy.j2db.util.model.IEditListModel;

//"editable" JList
public class JEditList extends JList implements CellEditorListener, ISkinnable, ListSelectionListener
{
	private IEditListEditor cellEditor = null;
	private Component editorComp = null;
	private CellEditorRemover editorRemover = null;
	private int editingRow = -1;
	private boolean isRendererSameAsEditor = false;
	private boolean surrendersFocusOnKeystroke = false;
	private final EmptyModelDataListener myListDataListener;
	private boolean editable = true;

	private boolean mustSelectFirst = false;


	private JLabel labelNoResults;

	public JEditList()
	{
		super();
		setUI(new EditListUI());
//		super.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// fix for case 188224; if you had a record view form and a list view form both visible in the same
		// window, and a text field with no onAction registered from the record view has focus, pressing ENTER
		// would result in focus being moved to the list view; we must avoid this by using WHEN_FOCUSED
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter"); //$NON-NLS-1$
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "enter"); //$NON-NLS-1$
		getActionMap().put("enter", new AbstractAction() //$NON-NLS-1$
			{
				public void actionPerformed(ActionEvent event)
				{
					if (!isEditing())
					{
						editCellAt(getSelectedIndex(), event);
						if (editorComp != null) editorComp.transferFocus();
					}
				}
			});
		myListDataListener = new EmptyModelDataListener();

		setModel(new AbstractEditListModel()
		{
			public Object getElementAt(int i)
			{
				return null;
			}

			public int getSize()
			{
				return 0;
			}
		});
	}

	protected int checkModelSize()
	{
		int sz = getModel().getSize();
//		if(sz == 0)
//		{
//			if(labelNoResults == null)
//			{
//				labelNoResults = new JLabel("No results");
//				labelNoResults.setBounds(10, 10,300,25);
//				add(labelNoResults);
//			}
//		}
//		else if(labelNoResults != null)
//		{
//			remove(labelNoResults);
//			labelNoResults = null;
//		}
		return sz;
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

	public void setUI(EditListUI ui)
	{
		if (ui instanceof EditListUI)
		{
			super.setUI(ui);
		}
	}

	@Override
	public void setUI(ComponentUI ui)
	{
//		super.setUI(ui); do nothing has his own UI
	}

//	public void setSelectionMode(int i)
//	{
//		//ignore
//	}

	/**
	 * @return Returns the editable.
	 */
	public boolean isEditable()
	{
		return editable;
	}

	/**
	 * @param editable The editable to set.
	 */
	public void setEditable(boolean editable)
	{
		this.editable = editable;
		if (!editable && isEditing())
		{
			getCellEditor().stopCellEditing();
		}
	}

	/**
	 * Sets whether editors in this JEditList get the keyboard focus when an editor is activated as a result of the JEditList forwarding keyboard events for a
	 * cell. By default, this property is false, and the JEditList retains the focus unless the cell is clicked.
	 * 
	 * @param surrendersFocusOnKeystroke true if the editor should get the focus when keystrokes cause the editor to be activated
	 * 
	 * 
	 * @see #getSurrendersFocusOnKeystroke
	 */
	public void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke)
	{
		this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
	}

	/**
	 * Returns true if the editor should get the focus when keystrokes cause the editor to be activated
	 * 
	 * @return true if the editor should get the focus when keystrokes cause the editor to be activated
	 * 
	 * @see #setSurrendersFocusOnKeystroke
	 */
	public boolean getSurrendersFocusOnKeystroke()
	{
		return surrendersFocusOnKeystroke;
	}

	/**
	 * Programmatically starts editing the cell at <code>row</code>
	 * 
	 * @param row the row to be edited
	 * @exception IllegalArgumentException if <code>row</code> is not in the valid range
	 * @return false if for any reason the cell cannot be edited
	 */
	public boolean editCellAt(int row)
	{
		return editCellAt(row, null);
	}

	/**
	 * Programmatically starts editing the cell at <code>row</code> , if the cell is editable. To prevent the <code>JEditList</code> from editing a particular
	 * table, column or cell value, return false from the <code>isCellEditable</code> method in the <code>TableModel</code> interface.
	 * 
	 * @param row the row to be edited
	 * @param e event to pass into <code>shouldSelectCell</code>; note that as of Java 2 platform v1.2, the call to <code>shouldSelectCell</code> is no longer
	 *            made
	 * @exception IllegalArgumentException if <code>row</code> is not in the valid range
	 * @return false if for any reason the cell cannot be edited
	 */
	public boolean editCellAt(int row, EventObject e)
	{
		if (getEditingRow() == row) return true;
		//System.out.println("editCellAt0");
		if (cellEditor != null && !cellEditor.stopCellEditing())
		{
			return false;
		}
		//System.out.println("editCellAt1");
		if (row < 0 || row >= getModel().getSize())
		{
			return false;
		}

		if (!editable || !((IEditListModel)getModel()).isCellEditable(row))
		{
			IEditListEditor editor = getCellEditor();
			if (e instanceof MouseEvent && editor != null)
			{
				Point p = ((MouseEvent)e).getPoint();
				Component comp = editor.getListCellEditorComponent(this, null, true, row);
				Rectangle recOldBounds = comp.getBounds();
				comp.setBounds(getCellBounds(row, row));
				add(comp);
				comp.doLayout();
				Point p2 = SwingUtilities.convertPoint(this, p, comp);
				Component dispatchComponent = SwingUtilities.getDeepestComponentAt(comp, p2.x, p2.y);
				if (dispatchComponent instanceof JButton)
				{
					JButton btn = (JButton)dispatchComponent;
					if (shouldDispatchClickToButton(btn)) btn.doClick();
				}
				remove(comp);
				repaint();
			}
			return false;
		}

		FocusManager fm = FocusManager.getCurrentManager();
		//System.out.println("editCellAt3");
		if (editorRemover == null)
		{
			editorRemover = new CellEditorRemover(fm);
			//DISABLED:SHOULDFIX:fm.addPropertyChangeListener("focusOwner", editorRemover);
		}

		//System.out.println("IEditListEditor isCellEditable: "+e);
		IEditListEditor editor = getCellEditor();
		if (editor != null && editor.isCellEditable(e))
		{
			//System.out.println(" preparing editor ");			
			editorComp = prepareEditor(editor, row);
			if (editorComp == null)
			{
				removeEditor();
				return false;
			}
			//System.out.println("editorComp "+editorComp);
			editorComp.setBounds(getCellBounds(row, row));
			add(editorComp);

			editorComp.validate();

//			setCellEditor(editor);
			setEditingRow(row);
			editor.addCellEditorListener(this);
			//fm.focusNextComponent(editorComp);

			return true;
		}
		return false;
	}

	protected boolean shouldDispatchClickToButton(JButton button)
	{
		return true;
	}

	/**
	 * Returns true if a cell is being edited.
	 * 
	 * @return true if the table is editing a cell
	 * @see #editingRow
	 */
	public boolean isEditing()
	{
		return editorComp != null;
	}

	/**
	 * Returns the component that is handling the editing session. If nothing is being edited, returns null.
	 * 
	 * @return Component handling editing session
	 */
	public Component getEditorComponent()
	{
		return editorComp;
	}

	/**
	 * Returns the index of the row that contains the cell currently being edited. If nothing is being edited, returns -1.
	 * 
	 * @return the index of the row that contains the cell currently being edited; returns -1 if nothing being edited
	 */
	public int getEditingRow()
	{
		return editingRow;
	}

	/*
	 * protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,int condition, boolean pressed) { //System.out.println("processKeyBinding"); boolean
	 * retValue = super.processKeyBinding(ks, e, condition, pressed); // Start editing when a key is typed. UI classes can disable this behavior // by setting
	 * the client property JEditList.autoStartsEdit to Boolean.FALSE. if (!retValue && condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && isFocusOwner() //&& //
	 * !Boolean.FALSE.equals((Boolean)getClientProperty("JEditList.autoStartsEdit")) ) { // We do not have a binding for the event. Component editorComponent =
	 * getEditorComponent(); //System.out.println("invoke getEditorComponent "+editorComponent); if (editorComponent == null) { //System.out.println("e "+e); //
	 * Only attempt to install the editor on a KEY_PRESSED, if (e == null || e.getID() != KeyEvent.KEY_PRESSED) { return false; } // Don't start when just a
	 * modifier is pressed int code = e.getKeyCode(); //System.out.println("code "+code); if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code
	 * == KeyEvent.VK_ALT) { return false; } // Try to install the editor int anchorRow = getSelectionModel().getAnchorSelectionIndex();
	 * //System.out.println("invoke getAnchorSelectionIndex"); if (anchorRow != -1 && !isEditing()) { //System.out.println("invoke editCellAt"); if
	 * (!editCellAt(anchorRow)) { return false; } } editorComponent = getEditorComponent(); if (editorComponent == null) { return false; } } // If the
	 * editorComponent is a JComponent, pass the event to it. if (editorComponent instanceof JComponent) { retValue =
	 * ((JComponent)editorComponent).processKeyBinding(ks, e, WHEN_FOCUSED, pressed); // If we have started an editor as a result of the user // pressing a key
	 * and the surrendersFocusOnKeystroke property // is true, give the focus to the new editor. if (getSurrendersFocusOnKeystroke()) {
	 * editorComponent.requestFocus(); } } }
	 * 
	 * return retValue; }
	 */

	//
	// Implementing the CellEditorListener interface
	//
	/**
	 * Invoked when editing is finished. The changes are saved and the editor is discarded.
	 * <p>
	 * Application code will not use these methods explicitly, they are used internally by JEditList.
	 * 
	 * @param e the event received
	 * @see CellEditorListener
	 */
	public void editingStopped(ChangeEvent e)
	{
		// Take in the new value
		IEditListEditor editor = getCellEditor();
		if (editor != null)
		{
			Object value = editor.getCellEditorValue();
			((IEditListModel)getModel()).setElementAt(value, editingRow);
			removeEditor();
		}
	}

	/**
	 * Invoked when editing is canceled. The editor object is discarded and the cell is rendered once again.
	 * <p>
	 * Application code will not use these methods explicitly, they are used internally by JEditList.
	 * 
	 * @param e the event received
	 * @see CellEditorListener
	 */
	public void editingCanceled(ChangeEvent e)
	{
		removeEditor();
	}

	/**
	 * Sets the <code>cellEditor</code> variable.
	 * 
	 * @param anEditor the IEditListEditor that does the editing
	 * @see #cellEditor
	 * @beaninfo bound: true description: The table's active cell editor, if one exists.
	 */
	public void setCellEditor(IEditListEditor anEditor)
	{
		IEditListEditor oldEditor = cellEditor;
		cellEditor = anEditor;
		firePropertyChange("listViewEditor", oldEditor, anEditor); //$NON-NLS-1$
	}

	/**
	 * Returns an appropriate editor for the cell specified by <code>row</code>.
	 * <p>
	 * <b>Note:</b> Throughout the table package, the internal implementations always use this method to provide editors so that this default behavior can be
	 * safely overridden by a subclass.
	 * 
	 * @param row the row of the cell to edit, where 0 is the first row
	 * @return the editor for this cell; if <code>null</code> return the default editor for this type of cell
	 * @see DefaultCellEditor
	 */
	public IEditListEditor getCellEditor()
	{
		return cellEditor;
	}

	/**
	 * Sets the <code>editingRow</code> variable.
	 * 
	 * @param aRow the row of the cell to be edited
	 * 
	 * @see #editingRow
	 */
	public void setEditingRow(int aRow)
	{
		editingRow = aRow;
	}

	/**
	 * Prepares the editor by querying the data model for the value and selection state of the cell at <code>row</code>.
	 * <p>
	 * <b>Note:</b> Throughout the table package, the internal implementations always use this method to prepare editors so that this default behavior can be
	 * safely overridden by a subclass.
	 * 
	 * @param editor the <code>IEditListEditor</code> to set up
	 * @param row the row of the cell to edit, where 0 is the first row
	 * @return the <code>Component</code> being edited
	 */
	public Component prepareEditor(IEditListEditor editor, int row)
	{
		Object value = getModel().getElementAt(row);
		Component comp = editor.getListCellEditorComponent(this, value, true, row);
		if (comp instanceof JComponent)
		{
			JComponent jComp = (JComponent)comp;
			if (jComp.getNextFocusableComponent() == null)
			{
				jComp.setNextFocusableComponent(this);
			}
		}
		return comp;
	}

	/**
	 * Discards the editor object and frees the real estate it used for cell rendering.
	 */
	public void removeEditor()
	{
		//DISABLED:SHOULDFIX:DefaultFocusManager.getCurrentManager().removePropertyChangeListener("focusOwner",editorRemover);

		IEditListEditor editor = getCellEditor();
		if (editor != null)
		{
			editor.removeCellEditorListener(this);

			if (editorComp != null)
			{
				remove(editorComp);
				Rectangle cellRect = getCellBounds(editingRow, editingRow);
				if (cellRect != null) repaint(cellRect);

				setEditingRow(-1);
				editorComp = null;
			}
			requestFocus();
		}
	}

	// This class tracks changes in the keyboard focus state. It is used
	// when the JEditList is editing to determine when to cancel the edit.
	// If focus switches to a component outside of the JEditList, but in the
	// same window, this will cancel editing.
	class CellEditorRemover implements PropertyChangeListener
	{
		FocusManager focusManager;

		public CellEditorRemover(FocusManager fm)
		{
			this.focusManager = fm;
		}

		public void propertyChange(PropertyChangeEvent ev)
		{
			if (!isEditing())
			{
				return;
			}

			Component c = null;//DISABLED:SHOULDFIX:focusManager.getFocusOwner();
			while (c != null)
			{
				if (c == JEditList.this)
				{
					// focus remains inside the table
					return;
				}
				else if ((c instanceof Window) || (c instanceof Applet && c.getParent() == null))
				{
					if (c == SwingUtilities.getRoot(JEditList.this))
					{
						getCellEditor().cancelCellEditing();
					}
					break;
				}
				c = c.getParent();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JList#setSelectionModel(javax.swing.ListSelectionModel)
	 */
	@Override
	public void setSelectionModel(ListSelectionModel selectionModel)
	{
		ListSelectionModel lsm = getSelectionModel();
		if (lsm != null) lsm.removeListSelectionListener(this);
		super.setSelectionModel(selectionModel);
		selectionModel.addListSelectionListener(this);
	}


	@Override
	public void setModel(ListModel model)
	{
		if (model == null) return;

		// TODO must check if listModel instanceof IEditList??
		getModel().removeListDataListener(myListDataListener);

		super.setModel(model);

		checkModelSize();

		getModel().addListDataListener(myListDataListener);
	}

	class EmptyModelDataListener implements ListDataListener
	{
		public void contentsChanged(ListDataEvent event)
		{
			checkModelSize();
		}

		public void intervalAdded(ListDataEvent event)
		{
			checkModelSize();
//			int sz = checkModelSize();
//			if ((getSelectedIndex() == -1 || getSelectedIndex() >= sz) && event.getIndex1() >= 0)
//			if (getSelectedIndex() == -1 && event.getIndex1() >= 0)
//			{
//				setSelectedIndex(0);
//			}
//Debug.trace("index "+getSelectedIndex());			
		}

		public void intervalRemoved(ListDataEvent event)
		{
			if (isEditing() && (getModel().getSize() == 0 || (getEditingRow() >= event.getIndex0() && getEditingRow() <= event.getIndex1())))
			{
				getCellEditor().stopCellEditing();
			}

			checkModelSize();
		}
	}

	/*
	 * @see JList#getNextMatch(String, int, Bias)
	 */
	@Override
	public int getNextMatch(String arg0, int arg1, Bias arg2)
	{
		// do nothing
		return -1;
	}

	/*
	 * @see JComponent#getToolTipText(MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent event)
	{
		if (event != null)
		{
			Point p = event.getPoint();
			int index = locationToIndex(p);
			ListCellRenderer r = getCellRenderer();
			Rectangle cellBounds;

			if (index != -1 && r != null && (cellBounds = getCellBounds(index, index)) != null && cellBounds.contains(p.x, p.y))
			{
				ListSelectionModel lsm = getSelectionModel();
				Component rComponent = r.getListCellRendererComponent(this, getModel().getElementAt(index), index, lsm.isSelectedIndex(index),
					(hasFocus() && (lsm.getLeadSelectionIndex() == index)));

				if (rComponent instanceof JComponent)
				{
					MouseEvent newEvent;

					p.translate(-cellBounds.x, -cellBounds.y);
					newEvent = new MouseEvent(rComponent, event.getID(), event.getWhen(), event.getModifiers(), p.x, p.y, event.getClickCount(),
						event.isPopupTrigger());

					this.add(rComponent);
					rComponent.setBounds(cellBounds);
					rComponent.doLayout();
					String tip = ((JComponent)rComponent).getToolTipText(newEvent);
					this.remove(rComponent);
					if (tip != null)
					{
						return tip;
					}
				}
			}
		}

		return super.getToolTipText();
	}

	/**
	 * Gets the mustSelectFirst. (first the row must be selected before it starts editing)
	 * 
	 * @return Returns a boolean
	 */
	public boolean getMustSelectFirst()
	{
		return mustSelectFirst;
	}

	/**
	 * Sets the mustSelectFirst.
	 * 
	 * @param mustSelectFirst The mustSelectFirst to set
	 */
	public void setMustSelectFirst(boolean mustSelectFirst)
	{
		this.mustSelectFirst = mustSelectFirst;
	}

	/**
	 * @see javax.swing.JList#setSelectedIndex(int)
	 */
	@Override
	public void setSelectedIndex(int index)
	{
		if (isEditing())
		{
			getCellEditor().stopCellEditing();
		}
		if (getSelectionModel() instanceof AlwaysRowSelectedSelectionModel && !((AlwaysRowSelectedSelectionModel)getSelectionModel()).canChangeSelection()) return;
		super.setSelectedIndex(index);
	}

	/**
	 * Returns the isRendererSameAsEditor.
	 * 
	 * @return boolean
	 */
	public boolean isRendererSameAsEditor()
	{
		return isRendererSameAsEditor;
	}

	/**
	 * Sets the isRendererSameAsEditor.
	 * 
	 * @param isRendererSameAsEditor The isRendererSameAsEditor to set
	 */
	public void setRendererSameAsEditor(boolean isRendererSameAsEditor)
	{
		this.isRendererSameAsEditor = isRendererSameAsEditor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if (getAutoscrolls() && !e.getValueIsAdjusting())
		{

			int selected = getSelectedIndex();
			ensureIndexIsVisible(selected);
		}
	}

}