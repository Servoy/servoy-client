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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.UIManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.ui.IScriptBaseMethods;

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

}