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
package com.servoy.j2db.smart.dataui;


import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ISkinnable;

/**
 * @author jblok
 */
public class SolutionSkin
{
	public static UIDefaults load(IApplication app, byte[] skinjardata)
	{
		try
		{
			LookAndFeel skinlaf = app.getLAFManager().createInstance("com.l2fprod.gui.plaf.skin.SkinLookAndFeel"); //$NON-NLS-1$
			if (skinlaf != null)
			{
//				UIDefaults defs = SolutionSkinLoader.load(app, laf, skinjardata);
//				return defs;
				Method m1 = skinlaf.getClass().getMethod("loadThemePack", new Class[] { InputStream.class }); //$NON-NLS-1$
				Object skin = m1.invoke(null, new Object[] { new ByteArrayInputStream(skinjardata) });
				Class clazz = skinlaf.getClass().getClassLoader().loadClass("com.l2fprod.gui.plaf.skin.Skin"); //$NON-NLS-1$
//				Class clazz = skin.getClass();
//Debug.trace(clazz.getName());				
				Method m2 = skinlaf.getClass().getMethod("setSkin", new Class[] { clazz }); //$NON-NLS-1$
				m2.invoke(null, new Object[] { skin });
				Method m3 = skinlaf.getClass().getMethod("initialize", new Class[0]); //$NON-NLS-1$
				m3.invoke(skinlaf, new Object[0]);
				Method m4 = skinlaf.getClass().getMethod("getDefaults", new Class[0]); //$NON-NLS-1$
				return (UIDefaults)m4.invoke(skinlaf, new Object[0]);
			}
			return null;
		}
		catch (Throwable e)
		{
			Debug.error(e);
			return null;
		}
	}

	public static void updateComponentTreeUI(UIDefaults defs, Component c)
	{
		updateComponentTreeUI0(defs, c);
		c.invalidate();
		c.validate();
		c.repaint();
	}

	private static void updateComponentTreeUI0(UIDefaults defs, Component c)
	{
		if (c instanceof ISkinnable && c instanceof JComponent)
		{
//            ((JComponent) c).updateUI();
			if (defs != null)
			{
				((ISkinnable)c).setUI(defs.getUI((JComponent)c));
			}
		}
		Component[] children = null;
		if (c instanceof JMenu)
		{
			children = ((JMenu)c).getMenuComponents();
		}
		else if (c instanceof Container)
		{
			children = ((Container)c).getComponents();
		}
		if (children != null)
		{
			for (Component element : children)
			{
				updateComponentTreeUI0(defs, element);
			}
		}
	}

}

//class SolutionSkinLoader
//{
//	static UIDefaults load(IApplication app,LookAndFeel laf, byte[] skinjardata)
//	{
//		try
//		{
//			
//			com.l2fprod.gui.plaf.skin.SkinLookAndFeel skinlaf = (com.l2fprod.gui.plaf.skin.SkinLookAndFeel)laf;
//			com.l2fprod.gui.plaf.skin.Skin s = skinlaf.loadThemePack(new ByteArrayInputStream(skinjardata));
//			skinlaf.setSkin(s);
//			skinlaf.initialize();
//			return skinlaf.getDefaults();
//		}
//		catch (Throwable e)
//		{
//			Debug.error(e);
//			return null;
//		}
//	}
// }
