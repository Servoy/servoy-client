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


import com.servoy.j2db.dataprocessing.SQLGenerator;

/**
 * Mode manager, like browse,find and print preview
 * 
 * @author jblok
 */
public class ModeManager implements IModeManager
{
	protected int mode;
	protected final IApplication application;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public ModeManager(IApplication app)
	{
		application = app;
	}

	public void setMode(int m)
	{
		int oldMode = mode;
		mode = m;

		switch (mode)
		{
			case FIND_MODE :
				if (application.getApplicationType() == IApplication.CLIENT) //makes no sense to show rich client status text in web
				{
					application.setStatusText(application.getI18NMessage("servoy.modeManager.status.findText"), SQLGenerator.getFindToolTip(application)); //$NON-NLS-1$
				}
				break;

			case PREVIEW_MODE :
				application.setStatusText(application.getI18NMessage("servoy.modeManager.status.preview"), null); //$NON-NLS-1$
				break;

			case EDIT_MODE :
				application.setStatusText(application.getI18NMessage("servoy.general.status.ready"), null); //$NON-NLS-1$
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
