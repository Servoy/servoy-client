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


import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;


/**
 * @author jblok 
*/
public class NumberCellEditor extends DefaultCellEditor implements ActionListener //,PropertyEditor
{
	private final NumberField editorComponent;

	public NumberCellEditor()
	{
		super(new JCheckBox()); //Unfortunately, the constructor
		//expects a check box, combo box,
		//or text field.
//        setClickCountToStart(2); //This is usually 1 or 2.

		editorComponent = new NumberField(new Integer(0));
		editorComponent.setBorder(BorderFactory.createLineBorder(Color.black));
		editorComponent.addActionListener(this);
	}

	public synchronized void actionPerformed(ActionEvent e)
	{
		fireEditingStopped();
	}

	@Override
	protected void fireEditingStopped()
	{
		super.fireEditingStopped();
	}

	@Override
	public Object getCellEditorValue()
	{
		return editorComponent.getValue();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		if (value instanceof String)
		{
			try
			{
				Integer i = new Integer((String)value);
				editorComponent.setValue(i);
			}
			catch (Exception ex)
			{
				editorComponent.setValue(new Integer(0));
			}
		}
		if (value instanceof Integer)
		{
			editorComponent.setValue((Integer)value);
		}
		return editorComponent;
	}
}
