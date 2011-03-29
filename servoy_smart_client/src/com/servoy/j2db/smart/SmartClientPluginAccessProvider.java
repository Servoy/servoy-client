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

import java.awt.Window;
import java.net.URLStreamHandler;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JMenu;

import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.MediaURLStreamHandler;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.toolbar.IToolbarPanel;


/**
 * A special {@link IClientPluginAccess} that also implements {@link ISmartClientPluginAccess} to override behavior that is specific for the smartclient.
 * @author jblok
 */
public class SmartClientPluginAccessProvider extends ClientPluginAccessProvider implements ISmartClientPluginAccess
{
	private final ISmartClientApplication application;

	public SmartClientPluginAccessProvider(J2DBClient client)
	{
		super(client);
		this.application = client;
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#exportObject(java.rmi.Remote)
	 */
	@Override
	public void exportObject(Remote object) throws RemoteException
	{
		application.exportObject(object);
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
		application.addURLStreamHandler(protocolName, handler);
	}

	/**
	 * @see com.servoy.j2db.plugins.IClientPluginAccess#getMediaURLStreamHandler()
	 */
	@Override
	public URLStreamHandler getMediaURLStreamHandler()
	{
		return new MediaURLStreamHandler(application);
	}

	@Override
	public Window getWindow(String windowName)
	{
		if (windowName == null) return application.getMainApplicationFrame();

		Object window = application.getJSWindowManager().getWindowWrappedObject(windowName);
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
	public Window getCurrentWindow()
	{
		Object window = application.getJSWindowManager().getCurrentWindowWrappedObject();
		if ((window instanceof Window) && ((Window)window).isVisible())
		{
			return (Window)window;
		}
		else
		{
			return application.getMainApplicationFrame();
		}
	}

	@Override
	public IToolbarPanel getToolbarPanel()
	{
		return application.getToolbarPanel();
	}

	@Override
	public JMenu getImportMenu()
	{
		return application.getImportMenu();
	}

	@Override
	public JMenu getExportMenu()
	{
		return application.getExportMenu();
	}

	@Deprecated
	@Override
	public JFrame getMainApplicationFrame()
	{
		return application.getMainApplicationFrame();
	}
}
