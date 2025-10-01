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

package com.servoy.j2db.smart.plugins;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JMenu;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.IMediaUploadCallback;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.smart.ISmartClientPluginAccess;
import com.servoy.j2db.util.FileChooserUtils;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.toolbar.IToolbarPanel;


/**
 * A special {@link IClientPluginAccess} that also implements {@link ISmartClientPluginAccess} to override behavior that is specific for the smartclient.
 * @author jblok
 */
public class SmartClientPluginAccessProvider extends ClientPluginAccessProvider implements ISmartClientPluginAccess
{
	public SmartClientPluginAccessProvider(ISmartClientApplication client)
	{
		super(client);
	}

	@Override
	public ISmartClientApplication getApplication()
	{
		return (ISmartClientApplication)super.getApplication();
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#exportObject(java.rmi.Remote)
	 */
	@Override
	public void exportObject(Remote object) throws RemoteException
	{
		getApplication().exportObject(object);
	}

	/**
	 * Register a URLStreamHandler for a protocol
	 *
	 * @param protocolName
	 * @param handler
	 */
	@Override
	public void registerURLStreamHandler(String protocolName, URLStreamHandler handler)
	{
		getApplication().addURLStreamHandler(protocolName, handler);
	}

	@Override
	@Deprecated
	public Window getWindow(String windowName)
	{
		if (windowName == null) return getApplication().getMainApplicationFrame();

		Object window = getApplication().getRuntimeWindowManager().getWindowWrappedObject(windowName);
		if ((window instanceof Window) && ((Window)window).isVisible())
		{
			return (Window)window;
		}
		else
		{
			return null;
		}
	}

	@Override
	@Deprecated
	public Window getCurrentWindow()
	{
		Object window = getApplication().getRuntimeWindowManager().getCurrentWindowWrappedObject();
		if ((window instanceof Window) && ((Window)window).isVisible())
		{
			return (Window)window;
		}
		else
		{
			return getApplication().getMainApplicationFrame();
		}
	}

	@Override
	public IToolbarPanel getToolbarPanel()
	{
		return getApplication().getToolbarPanel();
	}

	@Override
	public JMenu getImportMenu()
	{
		return getApplication().getImportMenu();
	}

	@Override
	public JMenu getExportMenu()
	{
		return getApplication().getExportMenu();
	}

	@Deprecated
	@Override
	public JFrame getMainApplicationFrame()
	{
		return getApplication().getMainApplicationFrame();
	}

	@Override
	public void showFileOpenDialog(IMediaUploadCallback callback, String fileNameHint, boolean multiSelect, String[] filter, int selection, String dialogTitle)
	{
		File file = null;
		if (fileNameHint != null) file = new File(fileNameHint);
		if (multiSelect)
		{
			File[] files = FileChooserUtils.getFiles(getCurrentWindow(), file, selection, filter, dialogTitle);
			if (files != null && files.length > 0)
			{
				IUploadData[] data = new FileUploadData[files.length];
				for (int i = 0; i < files.length; i++)
				{
					data[i] = new FileUploadData(files[i]);
				}
				callback.uploadComplete(data);
			}

		}
		else
		{
			final File f = FileChooserUtils.getAReadFile(getCurrentWindow(), file, selection, filter, dialogTitle);
			if (f != null)
			{
				IUploadData data = new FileUploadData(f);
				callback.uploadComplete(new IUploadData[] { data });
			}
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static final class FileUploadData implements IUploadData
	{
		/**
		 *
		 */
		private final File f;

		/**
		 * @param f
		 */
		private FileUploadData(File f)
		{
			this.f = f;
		}

		public File getFile()
		{
			return f;
		}

		public String getName()
		{
			return f.getName();
		}

		public String getContentType()
		{
			try
			{
				return MimeTypes.getContentType(FileChooserUtils.readFile(f, 32), f.getName());
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		public byte[] getBytes()
		{
			try
			{
				return FileChooserUtils.readFile(f);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.plugins.IUploadData#getInputStream()
		 */
		public InputStream getInputStream()
		{
			try
			{
				return new BufferedInputStream(new FileInputStream(f));
			}
			catch (FileNotFoundException e)
			{
				return null;
			}
		}

		/**
		 * Currently never called
		 */
		@Override
		public long lastModified()
		{
			return f.lastModified();
		}
	}

}
