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
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptTransparentMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;

/**
 * Abstract scriptable component for {@link BaseComponent}.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeBaseComponent<C extends IComponent> implements IScriptable, IScriptBaseMethods, IScriptTransparentMethods, Wrapper
{
	private C component;
	private final IStylePropertyChangesRecorder jsChangeRecorder;
	private Map<Object, Object> clientProperties;
	protected final IApplication application;

	public AbstractRuntimeBaseComponent(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		this.jsChangeRecorder = jsChangeRecorder;
		this.application = application;
	}

	/**
	 * @return the component
	 */
	public C getComponent()
	{
		return component;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(C component)
	{
		this.component = component;
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
		return PersistHelper.createColorString(getComponent().getBackground());
	}

	public void js_setBgcolor(String clr)
	{
		getComponent().setBackground(PersistHelper.createColor(clr));
		if (getComponent().isOpaque()) getChangesRecorder().setBgcolor(clr);
	}

	public String js_getFgcolor()
	{
		return PersistHelper.createColorString(getComponent().getForeground());
	}

	public void js_setFgcolor(String clr)
	{
		getComponent().setForeground(PersistHelper.createColor(clr));
		getChangesRecorder().setFgcolor(clr);
	}

	public void js_setFont(String spec)
	{
		getComponent().setFont(PersistHelper.createFont(spec));
		getChangesRecorder().setFont(spec);
	}

	public String js_getFont()
	{
		return PersistHelper.createFontString(getComponent().getFont());
	}

	public int js_getWidth()
	{
		return getComponent().getSize().width;
	}

	public int js_getHeight()
	{
		return getComponent().getSize().height;
	}

	public boolean js_isVisible()
	{
		return getComponent().isVisible();
	}

	public void js_setVisible(boolean b)
	{
		if (!(getComponent() instanceof ISupportSecuritySettings) || ((ISupportSecuritySettings)getComponent()).isViewable())
		{
			getComponent().setComponentVisible(b);
			getChangesRecorder().setVisible(b);
		}
	}

	public boolean js_isTransparent()
	{
		return !getComponent().isOpaque();
	}

	public void js_setTransparent(boolean b)
	{
		getComponent().setOpaque(!b);
		getChangesRecorder().setTransparent(b);
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).repaint();
		}
	}

	public void js_setEnabled(final boolean b)
	{
		getComponent().setComponentEnabled(b);
	}

	public boolean js_isEnabled()
	{
		return getComponent().isEnabled();
	}

	public void js_setLocation(int x, int y)
	{
		getComponent().setLocation(new Point(x, y));
		getChangesRecorder().setLocation(x, y);
		if (getComponent() instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)getComponent()).setCachedLocation(new Point(x, y));
		}
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).validate();
		}
	}

	protected void setComponentSize(int x, int y)
	{
		// sets the component, changes recorder is not called here
		if (getComponent() instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)getComponent()).setCachedSize(new Dimension(x, y));
		}
		getComponent().setSize(new Dimension(x, y));
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).validate();
		}
	}

	public String js_getName()
	{
		String jsName = getComponent().getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}


	public int js_getLocationX()
	{
		return getComponent().getLocation().x;
	}

	public int js_getLocationY()
	{
		return getComponent().getLocation().y;
	}

	public void js_putClientProperty(Object key, Object value)
	{
		if (clientProperties == null)
		{
			clientProperties = new HashMap<Object, Object>();
		}
		clientProperties.put(key, value);
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).putClientProperty(key, value);
		}
	}

	public Object js_getClientProperty(Object key)
	{
		if (getComponent() instanceof JComponent)
		{
			return ((JComponent)getComponent()).getClientProperty(key);
		}
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	public String js_getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getComponent().getBorder());
	}

	public void js_setBorder(String spec)
	{
		getComponent().setBorder(ComponentFactoryHelper.createBorder(spec));
		getChangesRecorder().setBorder(spec);
	}

	public String getValueString()
	{
		if (getComponent() instanceof IDisplayData)
		{
			return "value: " + ((IDisplayData)getComponent()).getValueObject(); //$NON-NLS-1$
		}
		if (getComponent() instanceof ILabel)
		{
			return "label: " + ((ILabel)getComponent()).getText(); //$NON-NLS-1$
		}
		return null;
	}

	public String toString(String valueString)
	{
		if (getComponent() == null)
		{
			return "ScriptObject (component not yet set): " + getClass(); //$NON-NLS-1$
		}
		return js_getElementType() + "[name:" + js_getName() + ",x:" + js_getLocationX() + ",y:" + js_getLocationY() + ",width:" + js_getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			",height:" + js_getHeight() + (valueString == null ? "" : (',' + valueString)) + ']'; //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	@Override
	public String toString()
	{
		return toString(getComponent() == null ? null : getValueString());
	}
}
