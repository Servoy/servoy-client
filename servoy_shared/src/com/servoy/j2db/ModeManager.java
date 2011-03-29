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

import com.servoy.j2db.dataprocessing.SQLGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IProvideButtonModel;
import com.servoy.j2db.util.gui.ActionCheckBoxMenuItem;
import com.servoy.j2db.util.gui.ActionMenuItem;

/**
 * Mode manager, like browse,find and print preview
 * 
 * @author jblok
 */
public class ModeManager implements IModeManager
{
	public static final int DESIGN_MODE = 4;

/*
 * _____________________________________________________________ Declaration of attributes
 */

	private ButtonGroup modeGroup = new ButtonGroup();
	private ButtonModel browse = null;
	private ButtonModel find = null;
	private ButtonModel design = null;
	private ButtonModel preview = null;
	private int mode;
	private final IApplication application;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public ModeManager(IApplication app)
	{
		application = app;
		modeGroup = new ButtonGroup();
/*
 * browse = new FixedToggleButtonModel(); find = new FixedToggleButtonModel(); design = new DesignButtonModel(); preview = new FixedToggleButtonModel();
 */
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

	public JMenuItem getDesignModeMenuItem(Map actions)
	{
		JMenuItem mi = null;
		Action action = (Action)actions.get("cmddesignmode"); //$NON-NLS-1$
		if (action != null)
		{
			mi = new ActionMenuItem(action);
			if (action instanceof IProvideButtonModel)
			{
				design = ((IProvideButtonModel)action).getModel();
			}
//			modeGroup.add(mi);	
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

	public void setMode(int m)
	{
		if (m == DESIGN_MODE)
		{
			if (application.getSolution() == null) return;
			try
			{
				if (!application.getSolution().getForms(null, false).hasNext())
				{
					return;
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
				return;
			}
		}
		int oldMode = mode;
		mode = m;

		switch (mode)
		{
			case FIND_MODE :
				if (application.getApplicationType() == IApplication.CLIENT) //makes no sense to show rich client status text in web
				{
					application.setStatusText(application.getI18NMessage("servoy.modeManager.status.findText"), SQLGenerator.getFindToolTip(application)); //$NON-NLS-1$
				}
				if (find != null && !find.isSelected())
				{
					find.setSelected(true);
				}
				if (design != null && design.isSelected()) design.setSelected(false);
				break;

			case PREVIEW_MODE :
				application.setStatusText(application.getI18NMessage("servoy.modeManager.status.preview"), null); //$NON-NLS-1$
				if (preview != null && !preview.isSelected())
				{
					preview.setSelected(true);
				}
				if (design != null && design.isSelected()) design.setSelected(false);
				break;

			case EDIT_MODE :
				application.setStatusText(application.getI18NMessage("servoy.general.status.ready"), null); //$NON-NLS-1$
				if (browse != null && !browse.isSelected())
				{
					browse.setSelected(true);
				}
				if (design != null && design.isSelected()) design.setSelected(false);
				break;

			case DESIGN_MODE :
				application.setStatusText(application.getI18NMessage("servoy.modeManager.status.design"), null); //$NON-NLS-1$
				if (design != null && !design.isSelected())
				{
					design.setSelected(true);
				}
				break;
		}
		//now fire external, do not earlier becouse a dialog may be shown and status and menuitems inbackground are then not yet in the correct state
		J2DBGlobals.firePropertyChange(this, "mode", new Integer(oldMode), new Integer(mode)); //$NON-NLS-1$
	}

	public int getMode()
	{
		return mode;
	}

	/**
	 * @see com.servoy.j2db.IManager#flushCachedItems()
	 */
	public void flushCachedItems()
	{
		//ignore
	}

	/**
	 * @see com.servoy.j2db.IManager#init()
	 */
	public void init()
	{
		//ignore
	}
}
