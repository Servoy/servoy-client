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


import java.awt.Frame;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Properties;

import javax.swing.undo.UndoableEdit;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.PageSetupDialog;

/**
 * @author jblok
 */
public class CmdPageSetup extends AbstractCmd
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public CmdPageSetup(ISmartClientApplication app)
	{
		super(
			app,
			"CmdPageSetup", app.getI18NMessage("servoy.menuitem.pageSetup"), "servoy.menuitem.pageSetup", app.getI18NMessage("servoy.menuitem.pageSetup.mnemonic").charAt(0), app.loadImage("page_setup.gif")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		setActionCommand("pagesetup"); //$NON-NLS-1$
		setEnabled(true);
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass AbstractCmd
 */
	@Override
	public UndoableEdit doIt(java.util.EventObject ae)
	{
		try
		{
			PageFormat pf = getPageFormat(application.getPageFormat(), application.getSettings(), application.getMainApplicationFrame());
			if (pf != null)
			{
				((J2DBClient)application).setPageFormat(pf);
			}
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.menuitem.pageSetup.error"), ex); //$NON-NLS-1$
		}
		return null;
	}

	public static PageFormat getPageFormat(PageFormat pf, Properties settings, Frame frame)
	{
		PageFormat npf;
		if (Utils.getAsBoolean(settings.getProperty("useSystemPageDialog", "false"))) //$NON-NLS-1$ //$NON-NLS-2$
		{
			PrinterJob pj = PrinterJob.getPrinterJob();
			npf = pj.pageDialog(pf == null ? new PageFormat() : pf);
			pj.cancel();//TODO:figure out if this is needed...
		}
		else
		{
			PageSetupDialog psd = new PageSetupDialog(frame, false);
			psd.showDialog(pf);
			npf = psd.getPageFormat();
		}

		return npf;
	}

/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */


}