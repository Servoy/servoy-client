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
package com.servoy.j2db;


import java.util.Map;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JMenuItem;

import com.servoy.j2db.util.IProvideButtonModel;
import com.servoy.j2db.util.gui.ActionCheckBoxMenuItem;
import com.servoy.j2db.util.gui.ActionMenuItem;

/**
 * Mode manager, like browse,find and print preview
 * 
 * @author jblok
 */
public class SwingModeManager extends ModeManager
{
	private ButtonGroup modeGroup = new ButtonGroup();
	private ButtonModel browse = null;
	private ButtonModel find = null;
	private ButtonModel preview = null;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public SwingModeManager(IApplication app)
	{
		super(app);
		modeGroup = new ButtonGroup();
	}

	public JMenuItem getBrowseModeMenuItem(Map actions)
	{
		JMenuItem mi = null;
		Action action = (Action)actions.get("cmdbrowsemode"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionCheckBoxMenuItem(action);
//			mi.setSelected(true);
			if (action instanceof IProvideButtonModel)
			{
				browse = ((IProvideButtonModel)action).getModel();
			}
			modeGroup.add(mi);
		}
		return mi;
	}

	public JMenuItem getFindModeMenuItem(Map actions)
	{
		JMenuItem mi = null;
		Action action = (Action)actions.get("cmdfindmode"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			if (action instanceof IProvideButtonModel)
			{
				find = ((IProvideButtonModel)action).getModel();
			}
			modeGroup.add(mi);
		}
		return mi;
	}

	public JMenuItem getPreviewModeMenuItem(Map actions)
	{
		JMenuItem mi = null;
		Action action = (Action)actions.get("cmdpreviewmode"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			if (action instanceof IProvideButtonModel)
			{
				preview = ((IProvideButtonModel)action).getModel();
			}
			modeGroup.add(mi);
		}
		return mi;
	}

	@Override
	public void setMode(int m)
	{
		switch (m)
		{
			case FIND_MODE :
				if (find != null && !find.isSelected())
				{
					find.setSelected(true);
				}
				break;

			case PREVIEW_MODE :
				if (preview != null && !preview.isSelected())
				{
					preview.setSelected(true);
				}
				break;

			case EDIT_MODE :
				if (browse != null && !browse.isSelected())
				{
					browse.setSelected(true);
				}
				break;
		}
		super.setMode(m);
	}
}
