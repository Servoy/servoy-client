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

package com.servoy.j2db.ui.scripting;

import java.awt.Dimension;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeBaseComponent implements IScriptBaseMethods, Wrapper
{
	protected final IComponent component;
	protected final IStylePropertyChangesRecorder jsChangeRecorder;
	private Map<Object, Object> clientProperties;
	protected final IApplication application;

	public AbstractRuntimeBaseComponent(IComponent component, IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		this.component = component;
		this.jsChangeRecorder = jsChangeRecorder;
		this.application = application;
	}

	public Object unwrap()
	{
		return component;
	}

	/**
	 * @return the jsChangeRecorder
	 */
	public IStylePropertyChangesRecorder getChangesRecorder()
	{
		return jsChangeRecorder;
	}

	public String js_getBgcolor()
	{
		return PersistHelper.createColorString(component.getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		component.setBackground(PersistHelper.createColor(clr));
		jsChangeRecorder.setBgcolor(clr);
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(component.getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		component.setForeground(PersistHelper.createColor(clr));
		jsChangeRecorder.setFgcolor(clr);
	}

	public void js_setFont(String spec)
	{
		component.setFont(PersistHelper.createFont(spec));
		jsChangeRecorder.setFont(spec);
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(component.getFont());
	}

	public int js_getWidth()
	{
		return component.getSize().width;
	}

	public int js_getHeight()
	{
		return component.getSize().height;
	}

	public boolean js_isVisible()
	{
		return component.isVisible();
	}

	public void js_setVisible(boolean b)
	{
		if (!(component instanceof ISupportSecuritySettings) || ((ISupportSecuritySettings)component).isViewable())
		{
			component.setComponentVisible(b);
			jsChangeRecorder.setVisible(b);
		}
	}

	public boolean js_isTransparent()
	{
		return !component.isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		component.setOpaque(!b);
		jsChangeRecorder.setTransparent(b);
		if (component instanceof JComponent)
		{
			((JComponent)component).repaint();
		}
	}

	public void js_setEnabled(final boolean b)
	{
		component.setComponentEnabled(b);
	}

	public boolean js_isEnabled()
	{
		return component.isEnabled();
	}

	public void js_setLocation(int x, int y)
	{
		component.setLocation(new Point(x, y));
		jsChangeRecorder.setLocation(x, y);
		if (component instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)component).setCachedLocation(new Point(x, y));
		}
		if (component instanceof JComponent)
		{
			((JComponent)component).validate();
		}
	}

	public void js_setSize(int x, int y)
	{
		if (component instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)component).setCachedSize(new Dimension(x, y));
		}
		component.setSize(new Dimension(x, y));
		if (component instanceof JComponent)
		{
			((JComponent)component).validate();
		}
	}

	public String js_getName()
	{
		String jsName = component.getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}


	public int js_getLocationX()
	{
		return component.getLocation().x;
	}

	public int js_getLocationY()
	{
		return component.getLocation().y;
	}

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
		if (component instanceof JComponent)
		{
			((JComponent)component).putClientProperty(key, value);
		}
	}

	public Object js_getClientProperty(Object key)
	{
		if (component instanceof JComponent)
		{
			return ((JComponent)component).getClientProperty(key);
		}
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(component.getBorder());
	}

	public void js_setBorder(String spec)
	{
		component.setBorder(ComponentFactoryHelper.createBorder(spec));
		jsChangeRecorder.setBorder(spec);
	}
}
