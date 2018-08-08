/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.smart;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.net.URL;

import com.servoy.j2db.util.BrowserLauncher;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class StandAloneRemoteChecker implements IRemoteRunner
{
	@Override
	public boolean isRunningWebStart()
	{
		return true;
	}

	@Override
	public void setClipboardContent(Object o)
	{
		if (o instanceof String)
		{
			StringSelection stsel = new StringSelection((String)o);
			Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
			system.setContents(stsel, stsel);
		}
	}

	@Override
	public String getClipboardString()
	{
		Object obj = getClipboardContent();
		if (obj instanceof String)
		{
			return (String)obj;
		}
		return null;
	}

	@Override
	public Object getClipboardContent()
	{
		Object tmp = null;
		try
		{
			Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = system.getContents(null);
			if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				try
				{
					tmp = t.getTransferData(DataFlavor.stringFlavor);
				}
				catch (Exception e)
				{
					tmp = e.getMessage();
				}
			}
			//DISABLED:for 1.4
			//			else if(t.isDataFlavorSupported(DataFlavor.imageFlavor))
			//			{
			//				try
			//				{
			//					 tmp = t.getTransferData(DataFlavor.imageFlavor);
			//				}
			//				catch (Exception e)
			//				{
			//					tmp = e.getMessage();
			//				}
			//			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return tmp;
	}

	@Override
	public URL getWebStartURL()
	{
		try
		{
			return new URL(System.getProperty("com.servoy.remote.codebase"));
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	@Override
	public boolean showURL(URL url) throws Exception
	{
		BrowserLauncher.openURL(url.toExternalForm());
		return true;
	}
}
