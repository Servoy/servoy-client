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
package com.servoy.j2db.smart;


import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.UIManager;

import com.servoy.j2db.util.Debug;

/**
 * Webstart helper class
 * @author jblok
 */
public class WebStart
{
	/**
	 * Check if running as webstart client
	 */
	private static Boolean bWebstart = null;

	public static boolean isRunningWebStart()
	{
		// Lookup the javax.jnlp.BasicService object, done in such a way that it is possible to not have any webstart class installed and not crashing
		if (bWebstart == null)
		{
			try
			{
				Class clazz = Class.forName("javax.jnlp.ServiceManager"); //$NON-NLS-1$
				bWebstart = new Boolean(true);
				UIManager.put("ClassLoader", WebStart.class.getClassLoader()); //$NON-NLS-1$
			}
			catch (Throwable ue)
			{
				bWebstart = new Boolean(false);
			}
		}
		return bWebstart.booleanValue();
	}

	public static void setClipboardContent(Object o)
	{
		if (WebStart.isRunningWebStart())
		{
			WebStartImpl.putOnClipboard(o);
		}
		else
		{
			if (o instanceof String)
			{
				StringSelection stsel = new StringSelection((String)o);
				Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
				system.setContents(stsel, stsel);
			}
//	 		else if(o instanceof Icon)
//	 		{
//	 			// TODO
//	 		}
		}
	}

	public static String getClipboardString()
	{
		Object obj = getClipboardContent();
		if (obj instanceof String)
		{
			return (String)obj;
		}
		return null;
	}

	public static Object getClipboardContent()
	{
		if (WebStart.isRunningWebStart())
		{
			return WebStartImpl.getFromClipboard();
		}
		else
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
	}

	public static URL getWebStartURL()
	{
		if (isRunningWebStart())
		{
			return WebStartImpl.getWebStartURL();
		}
		else
		{
			try
			{
				return new URL("http://" + InetAddress.getLocalHost().getHostName());
			}
			catch (Exception ex)
			{
				return null;
			}
		}
	}

	public static PageFormat showPageFormatDialog(PageFormat pageFormat) throws Exception
	{
		return WebStartImpl.showPageFormatDialog(pageFormat);
	}

	public static void print(Printable printable) throws Exception
	{
		WebStartImpl.print(printable);
	}

	public static void print(Pageable printable) throws Exception
	{
		WebStartImpl.print(printable);
	}

	public static boolean saveFile(InputStream is, String fileName, String[] extensions) throws Exception
	{
		return WebStartImpl.saveFile(is, fileName, extensions);
	}

	public static InputStream loadFile(String[] extensions) throws Exception
	{
		return WebStartImpl.loadFile(extensions);
	}

	public static boolean showURL(URL url) throws Exception
	{
		if (isRunningWebStart())
		{
			return WebStartImpl.showURL(url);
		}
		return false;
	}
}

class WebStartImpl
{
	static void putOnClipboard(Object o)
	{
		if (o instanceof String)
		{
			StringSelection stsel = new StringSelection((String)o);
			try
			{
				javax.jnlp.ClipboardService cs = (javax.jnlp.ClipboardService)javax.jnlp.ServiceManager.lookup("javax.jnlp.ClipboardService"); //$NON-NLS-1$ 
				cs.setContents(stsel);
			}
			catch (Throwable e)
			{
				Debug.error(e);
			}
		}
		else
		{
			// ICON/IMAGE
		}
	}

	static Object getFromClipboard()
	{
		try
		{
			javax.jnlp.ClipboardService cs = (javax.jnlp.ClipboardService)javax.jnlp.ServiceManager.lookup("javax.jnlp.ClipboardService"); //$NON-NLS-1$ 
			Transferable t = cs.getContents();

			if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
			{
				return cs.getContents().getTransferData(DataFlavor.stringFlavor);
			}
//DISABLED:for 1.4			
//	        else if(t.isDataFlavorSupported(DataFlavor.imageFlavor))
//	        {
//					return cs.getContents().getTransferData(DataFlavor.imageFlavor);
//	        }
		}
		catch (Throwable e)
		{
			Debug.error(e);
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Get the Webstart URL. Do isRunningWebStart check before
	 */
	private static URL url;

	static URL getWebStartURL()
	{
		if (url != null) return url;
		try
		{
			// Lookup the javax.jnlp.BasicService object 
			javax.jnlp.BasicService bs = (javax.jnlp.BasicService)javax.jnlp.ServiceManager.lookup("javax.jnlp.BasicService"); //$NON-NLS-1$
			url = bs.getCodeBase();
			return url;
		}
		catch (Throwable e)
		{
			Debug.error(e);
			try
			{
				url = new URL("http://localhost:8080"); //for debugging only //$NON-NLS-1$
				return url;
			}
			catch (MalformedURLException ex)
			{
				Debug.error(ex);
				return null;
			}
		}
	}

	static boolean showURL(URL url) throws Exception
	{
		// Lookup the javax.jnlp.BasicService object 
		javax.jnlp.BasicService bs = (javax.jnlp.BasicService)javax.jnlp.ServiceManager.lookup("javax.jnlp.BasicService"); //$NON-NLS-1$
		// Invoke the showDocument method 
		return bs.showDocument(url);
	}

	static boolean saveFile(InputStream is, String fileName, String[] extensions) throws Exception
	{
		// Lookup the javax.jnlp.BasicService object 
		javax.jnlp.FileSaveService bs = (javax.jnlp.FileSaveService)javax.jnlp.ServiceManager.lookup("javax.jnlp.FileSaveService"); //$NON-NLS-1$
		Object fc = bs.saveFileDialog(null, extensions, is, fileName);
		return (fc != null);
	}

	static InputStream loadFile(String[] extensions) throws Exception
	{
		// Lookup the javax.jnlp.BasicService object 
		javax.jnlp.FileOpenService bs = (javax.jnlp.FileOpenService)javax.jnlp.ServiceManager.lookup("javax.jnlp.FileOpenService"); //$NON-NLS-1$
		javax.jnlp.FileContents fc = bs.openFileDialog(null, extensions);
		if (fc != null)
		{
			return fc.getInputStream();
		}
		else
		{
			return null;
		}
	}

	static PageFormat showPageFormatDialog(PageFormat pageFormat) throws Exception
	{
		// Lookup the Service object 
		javax.jnlp.PrintService ps = (javax.jnlp.PrintService)javax.jnlp.ServiceManager.lookup("javax.jnlp.PrintService"); //$NON-NLS-1$
		if (pageFormat == null) pageFormat = ps.getDefaultPage();
		pageFormat = ps.showPageFormatDialog(pageFormat);
		return pageFormat;
	}

	static void print(Printable printable) throws Exception
	{
		// Lookup the Service object 
		javax.jnlp.PrintService ps = (javax.jnlp.PrintService)javax.jnlp.ServiceManager.lookup("javax.jnlp.PrintService"); //$NON-NLS-1$
		ps.print(printable);
	}

	static void print(Pageable printable) throws Exception
	{
		// Lookup the Service object 
		javax.jnlp.PrintService ps = (javax.jnlp.PrintService)javax.jnlp.ServiceManager.lookup("javax.jnlp.PrintService"); //$NON-NLS-1$
		ps.print(printable);
	}
}
