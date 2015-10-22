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

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.ListModel;

import com.servoy.j2db.util.editlist.JEditList;
import com.servoy.j2db.util.model.IEditListModel;

public class JNavigableEditList extends JEditList
{
	public JNavigableEditList()
	{
		super();
		configureTabbing();
		this.addFocusListener(new FocusListener()
		{
			public void focusGained(FocusEvent e)
			{
				repaint();
			}

			public void focusLost(FocusEvent e)
			{
				repaint();
			}
		});
		if (getModel() != null && getModel().getSize() > 0) setSelectedIndex(0);
	}

	@Override
	public void setModel(ListModel model)
	{
		super.setModel(model);
		if (getModel() != null && getModel().getSize() > 0 && (getSelectedIndex() < 0 || getSelectedIndex() >= getModel().getSize())) setSelectedIndex(0);
	}

	private void configureTabbing()
	{
		KeyStroke forwardKey = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
		KeyStroke backwardKey = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
		KeyStroke spaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);

		InputMap im = getInputMap(WHEN_FOCUSED);
		im.put(forwardKey, "go_down"); //$NON-NLS-1$
		im.put(backwardKey, "go_up"); //$NON-NLS-1$
		im.put(spaceKey, "select_current"); //$NON-NLS-1$
		ActionMap am = getActionMap();
		am.put("go_down", new MoveUpDownAction(true)); //$NON-NLS-1$
		am.put("go_up", new MoveUpDownAction(false)); //$NON-NLS-1$
	}

	private class MoveUpDownAction extends AbstractAction
	{
		private static final long serialVersionUID = -3934960970488755711L;
		private final boolean moveDown;

		public MoveUpDownAction(boolean moveDown)
		{
			this.moveDown = moveDown;
		}

		public void actionPerformed(ActionEvent e)
		{
			if (isEditable())
			{
				IEditListModel mlm = (IEditListModel)getModel();
				int oldIndex = getSelectedIndex();
				if (oldIndex < 0) oldIndex = 0;
				int newIndex = oldIndex;
				if (moveDown) newIndex++;
				else newIndex--;
				if (newIndex < 0) newIndex += mlm.getSize();
				newIndex %= mlm.getSize();
				setSelectedIndex(newIndex);
			}
		}
	}
}
