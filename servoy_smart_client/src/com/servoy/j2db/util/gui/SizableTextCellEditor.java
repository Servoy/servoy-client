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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.text.BadLocationException;


/**
 * Special cell editor which can grow when content is larger then field
 * @author jblok 
*/
public class SizableTextCellEditor extends DefaultCellEditor implements FocusListener
{
	public SizableTextCellEditor()
	{
		super(new JCheckBox()); //Unfortunately, the constructor
		//expects a check box, combo box,
		//or text field.

		final SizableTextField stf = new SizableTextField();
		editorComponent = stf;
		delegate = new EditorDelegate()
		{
			@Override
			public void setValue(Object value)
			{
				stf.setValue(value);
			}

			@Override
			public Object getCellEditorValue()
			{
				try
				{
					return stf.getDocument().getText(0, stf.getDocument().getLength());
				}
				catch (BadLocationException e)
				{
					return null;
				}
			}

			@Override
			public boolean isCellEditable(EventObject anEvent)
			{
				if (anEvent instanceof MouseEvent)
				{
					//System.out.println("isCellEditable: " + (((MouseEvent) anEvent).getClickCount() >= clickCountToStart) );
					return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
				}
				return true;
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent)
			{
				//System.out.println("should select");
				stf.requestFocus();
				return false;
			}

			@Override
			public boolean startCellEditing(EventObject anEvent)
			{
				//System.out.println("cell editing");
				return true;
			}

			@Override
			public boolean stopCellEditing()
			{
				//System.out.println("stop cell editing");
				fireEditingStopped();
				return true;
			}

			@Override
			public void cancelCellEditing()
			{
				//System.out.println("cancel cell editing");
//				fireEditingStopped();
//				fireEditingCanceled();
			}
		};

		//Here's the code that brings up the dialog.
		editorComponent.addFocusListener(this);

		setClickCountToStart(2); //This is usually 1 or 2.

	}

	public void focusGained(FocusEvent e)
	{
	}

	public void focusLost(FocusEvent e)
	{
		super.fireEditingStopped();
	}

	/**
	 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(JTable, Object, boolean, int, int)
	 */
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		comp.setFont(table.getFont());
		return comp;
	}

}