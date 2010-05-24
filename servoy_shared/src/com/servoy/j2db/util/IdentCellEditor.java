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
package com.servoy.j2db.util;


import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * <class description> 
*/
public class IdentCellEditor extends DefaultCellEditor implements ActionListener //,PropertyEditor
{
	private final JTextField editorComponent;

	public IdentCellEditor(int identType)
	{
		this(0, identType);
	}

	public IdentCellEditor(int maxlength, int identType)
	{
		super(new JCheckBox()); //Unfortunately, the constructor
		//expects a check box, combo box,
		//or text field.
//		setClickCountToStart(2); //This is usually 1 or 2.
		editorComponent = new JTextField();
		editorComponent.setMargin(new Insets(1, 2, 1, 1));
		if (maxlength == 0)
		{
			editorComponent.setDocument(new ValidatingDocument(new IdentDocumentValidator(identType)));
		}
		else
		{
			editorComponent.setDocument(new ValidatingDocument(
				new ValidatingDocument.IDocumentValidator[] { new IdentDocumentValidator(identType), new LengthDocumentValidator(maxlength) }));
		}
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
		return editorComponent.getText();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		if (value != null)
		{
			editorComponent.setText(value.toString());
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					editorComponent.selectAll();
				}
			});
		}
		else
		{
			editorComponent.setText(""); //$NON-NLS-1$
		}
		return editorComponent;
	}
}
