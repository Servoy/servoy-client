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
package com.servoy.j2db.gui.editlist;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JToggleButton;
import javax.swing.event.CellEditorListener;

import com.servoy.j2db.util.editlist.IEditListEditor;
import com.servoy.j2db.util.editlist.JEditList;

public class NavigableCellEditor implements IEditListEditor
{
	private final IEditListEditor innerEditor;

	public NavigableCellEditor(IEditListEditor innerEditor)
	{
		this.innerEditor = innerEditor;
	}

	public Component getListCellEditorComponent(JEditList view, Object value, boolean isSelected, int row)
	{
		Component innerComponent = innerEditor.getListCellEditorComponent(view, value, isSelected, row);
		if (innerComponent instanceof JToggleButton)
		{
			JToggleButton btn = (JToggleButton)innerComponent;
			NavigableCellRenderer.decorateButton(btn, isSelected && view.hasFocus());
		}
		return innerComponent;
	}

	public void addCellEditorListener(CellEditorListener l)
	{
		innerEditor.addCellEditorListener(l);
	}

	public void cancelCellEditing()
	{
		innerEditor.cancelCellEditing();
	}

	public Object getCellEditorValue()
	{
		return innerEditor.getCellEditorValue();
	}

	public boolean isCellEditable(EventObject anEvent)
	{
		return innerEditor.isCellEditable(anEvent);
	}

	public void removeCellEditorListener(CellEditorListener l)
	{
		innerEditor.removeCellEditorListener(l);
	}

	public boolean shouldSelectCell(EventObject anEvent)
	{
		return innerEditor.shouldSelectCell(anEvent);
	}

	public boolean stopCellEditing()
	{
		return innerEditor.stopCellEditing();
	}

}
