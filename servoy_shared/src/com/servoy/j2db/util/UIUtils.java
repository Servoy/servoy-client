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
package com.servoy.j2db.util;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.UIManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.util.gui.AppletController;

public class UIUtils
{

	/**
	 * Gets the UI property from the given component (if available) or from UIManager.getDefaults(). If it cannot be found in either, the
	 * default value will be returned. For smart client use.
	 * @param component the component.
	 * @param name the name of the property.
	 * @param defaultValue the default value for this property.
	 * @return the value of the UI property from the given component (if available) or from UIManager.getDefaults(). If it cannot be found in either, the default value will be returned.
	 */
	public static Object getUIProperty(JComponent component, String name, Object defaultValue)
	{
		Object val = component.getClientProperty(name);
		if (val == null)
		{
			val = UIManager.getDefaults().get(name);
			if (val == null) val = defaultValue;
		}
		return val;
	}

	/**
	 * Find out the UI property with given name relevant to the given component. (can be used in both web client and smart client).
	 * @param component the component.
	 * @param application the Servoy application.
	 * @param name the property name.
	 * @param defaultValue default value for this property.
	 * @return the UI property with given name relevant to the given component.
	 */
	public static Object getUIProperty(IScriptBaseMethods component, IApplication application, String name, Object defaultValue)
	{
		Object val = component.js_getClientProperty(name);
		if (val == null)
		{
			val = application.getUIProperty(name);
		}
		if (val == null) val = defaultValue;
		return val;
	}

	public static boolean isOnScreen(Rectangle r)
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (ge != null)
		{
			GraphicsDevice[] gs = ge.getScreenDevices();
			for (GraphicsDevice element : gs)
			{
				GraphicsConfiguration gc = element.getDefaultConfiguration();
				if (gc != null)
				{
					Rectangle gcBounds = gc.getBounds();
					if (gcBounds.contains(r.x, r.y))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isCommandKeyDown(InputEvent e)
	{
		return Utils.isAppleMacOS() ? e.isMetaDown() : e.isControlDown();
	}

	public static int getClickInterval()
	{
		int clickInterval = 200;
		try
		{
			if (Toolkit.getDefaultToolkit() != null && Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") instanceof Integer) //$NON-NLS-1$
			{
				clickInterval = ((Integer)Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval")).intValue(); //$NON-NLS-1$
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return clickInterval;
	}

	/**
	 * @param appletContext
	 * @param applet
	 * @param initialSize
	 */
	public static void initializeApplet(AppletController appletContext, Applet applet, Dimension initialSize) throws Exception
	{
		String resourceName = applet.getClass().getName().replace('.', '/').concat(".class"); //$NON-NLS-1$

		URL objectUrl = applet.getClass().getClassLoader().getResource(resourceName);
		URL codeBase = null;
		URL docBase = null;

		String s = objectUrl.toExternalForm();
		if (s.endsWith(resourceName))
		{
			int ix = s.length() - resourceName.length();
			codeBase = new URL(s.substring(0, ix));
			docBase = codeBase;

			ix = s.lastIndexOf('/');

			if (ix >= 0)
			{
				docBase = new URL(s.substring(0, ix + 1));
			}
		}

		// Setup a default context and stub.
		appletContext.add(applet);

		AppletStub stub = new Stub(applet, appletContext, codeBase, docBase);
		applet.setStub(stub);

		if (initialSize != null) applet.setSize(initialSize.width, initialSize.height);
		else applet.setSize(100, 100);
		applet.init();

		((Stub)stub).active = true;
	}

	static class Stub implements AppletStub
	{
		transient boolean active;
		transient Applet target;
		transient AppletContext context;
		transient URL codeBase;
		transient URL docBase;

		Stub(Applet target, AppletContext context, URL codeBase, URL docBase)
		{
			this.target = target;
			this.context = context;
			this.codeBase = codeBase;
			this.docBase = docBase;
		}

		public boolean isActive()
		{
			return active;
		}

		public URL getDocumentBase()
		{
			// use the root directory of the applet's class-loader
			return docBase;
		}

		public URL getCodeBase()
		{
			// use the directory where we found the class or serialized object.
			return codeBase;
		}

		public String getParameter(String name)
		{
			return null;
		}

		public AppletContext getAppletContext()
		{
			return context;
		}

		public void appletResize(int width, int height)
		{
			// we do nothing.
		}
	}

}