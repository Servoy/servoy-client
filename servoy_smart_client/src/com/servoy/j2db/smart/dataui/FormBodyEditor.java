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


import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.servoy.j2db.util.editlist.IEditListEditor;
import com.servoy.j2db.util.editlist.JEditList;

/**
 * Editor part for a tableview
 *
 * @author jblok
 */
public class FormBodyEditor extends AbstractCellEditor implements IEditListEditor
{
	private DataRenderer editor = null;

	public FormBodyEditor(DataRenderer c)
	{
		editor = c;
	}

	public Object getCellEditorValue()
	{
		editor.stopUIEditing(true);
		return null;
	}

	/**
	 * @see javax.swing.AbstractCellEditor#stopCellEditing()
	 */
	@Override
	public boolean stopCellEditing()
	{
		if (!editor.stopUIEditing(true))
		{
			return false;
		}
		fireEditingStopped();
		return true;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent)
	{
		if (editor == null) return false;
		if (anEvent instanceof MouseEvent)
		{
			Component src = (Component)anEvent.getSource();
			if (src instanceof JEditList)
			{
				JEditList list = (JEditList)src;
				Point p = ((MouseEvent)anEvent).getPoint();
				int row = list.locationToIndex(p);
				Component comp = getListCellEditorComponent((JEditList)src, list.getModel().getElementAt(list.getSelectedIndex()), true, row);
				comp.setBounds(((JEditList)src).getCellBounds(row, row));
				list.add(comp);
				comp.doLayout();
				Point p2 = SwingUtilities.convertPoint(src, p, comp);
//				Point p2 = new Point(p);
//				p2.x = p2.x - row*comp.getBounds().height;
				Component dispatchComponent = SwingUtilities.getDeepestComponentAt(comp, p2.x, p2.y);
				list.remove(comp);
				if (dispatchComponent != null)
				{
					if (!dispatchComponent.isEnabled()) return false;
//					if(dispatchComponent instanceof JTextComponent)
//					{
//						JTextComponent text = (JTextComponent)dispatchComponent;
//						System.out.println(text.getText());
//						if(!text.isEditable() && "".equals(text.getText()))
//						{
//							return false;
//						}
//					}
				}

				if (dispatchComponent instanceof DataCalendar) return true;
				if (dispatchComponent instanceof AbstractScriptLabel && ((AbstractScriptLabel)dispatchComponent).hasActionCommand())
				{
					return true;
				}

				if (dispatchComponent == null ||
					(dispatchComponent != null && (dispatchComponent == comp || dispatchComponent instanceof JPanel || dispatchComponent instanceof JLabel)))
				{
					return ((MouseEvent)anEvent).getClickCount() >= 2;
				}
			}
		}
		return true;
	}

	public Component getListCellEditorComponent(JEditList list, Object value, boolean isSelected, int row)
	{
		if (editor == null) return new JLabel("Please add body part or do not display form.");
		return editor.getListCellRendererComponent(list, value, row, isSelected, true);
	}

	public DataRenderer getDataRenderer()
	{
		return editor;
	}

}
