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
package com.servoy.j2db.smart.cmd;


import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

import javax.swing.KeyStroke;
import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.smart.RemoteRunnerChecker;
import com.servoy.j2db.util.BrowserLauncher;
import com.servoy.j2db.util.Debug;

/**
 * @author jblok
 */
public class CmdHelp extends AbstractCmd
{
/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public CmdHelp(ISmartClientApplication app)
	{
		super(app, "CmdHelp", app.getI18NMessage("servoy.menuitem.help"), "servoy.menuitem.help", app.getI18NMessage("servoy.menuitem.help.mnemonic").charAt(0), //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			app.loadImage("help.gif")); //$NON-NLS-1$
		setActionCommand("help"); //$NON-NLS-1$
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		setEnabled(true);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		if (RemoteRunnerChecker.getInstance().isRunningWebStart())
		{
			try
			{
				URL url = new URL(application.getServerURL(), "/client_manual/index.htm"); //$NON-NLS-1$
				RemoteRunnerChecker.getInstance().showURL(url);
			}
			catch (Exception ex)
			{
				Debug.error(ex);
			}
		}
		else
		{
			try
			{
				String user_dir = application.getSettings().getProperty(J2DBGlobals.SERVOY_APPLICATION_SERVER_DIRECTORY_KEY);
				BrowserLauncher.openURL(new File(user_dir, "server/webapps/ROOT/client_manual/index.htm").getCanonicalFile().toURI().toURL().toString()); //$NON-NLS-1$
			}
			catch (Throwable e)//catch all for apple mac
			{
				Debug.error(e);
			}
		}
		return null;
	}


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}