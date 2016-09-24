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
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IProvideFormName;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.ILabel;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportCachedLocationAndSize;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.ui.ISupportSimulateBoundsProvider;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * Abstract scriptable component for {@link BaseComponent}.
 *
 * @author lvostinar
 * @since 6.0
 */
@SuppressWarnings("nls")
public abstract class AbstractRuntimeBaseComponent<C extends IComponent> implements IScriptable, IRuntimeComponent, Wrapper
{
	private C component;
	private IPersist persist;
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
	 * @return the persist
	 */
	public IPersist getPersist()
	{
		return persist;
	}

	/**
	 * @param component the component to set
	 */
	public void setComponent(C component, IPersist persist)
	{
		this.component = component;
		this.persist = persist;
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


	/**
	 * @param propertyName
	 * @param newValue
	 * @param oldValue
	 */
	protected void propertyChanged(String propertyName, Object newValue, Object oldValue)
	{
	}

	public String getBgcolor()
	{
		return PersistHelper.createColorString(getComponent().getBackground());
	}

	public void setBgcolor(String clr)
	{
		// always set in changesrecorder, has old value check as well
		getChangesRecorder().setBgcolor(clr);
		if (!getComponent().isOpaque())
		{
			getChangesRecorder().setTransparent(true);
		}

		String old = getBgcolor();
		if (!Utils.stringSafeEquals(old, clr))
		{
			getComponent().setBackground(PersistHelper.createColor(clr));
			propertyChanged("bgcolor", clr, old);
		}
	}

	public String getFgcolor()
	{
		return PersistHelper.createColorString(getComponent().getForeground());
	}

	public void setFgcolor(String clr)
	{
		// always set in changesrecorder, has old value check as well
		getChangesRecorder().setFgcolor(clr);

		String old = getFgcolor();
		if (!Utils.stringSafeEquals(old, clr))
		{
			getComponent().setForeground(PersistHelper.createColor(clr));
			propertyChanged("fgcolor", clr, old);
		}
	}

	public void setFont(String spec)
	{
		String old = getFont();
		if (!Utils.stringSafeEquals(old, spec))
		{
			getComponent().setFont(PersistHelper.createFont(spec));
			getChangesRecorder().setFont(spec);
			propertyChanged("font", spec, old);
		}
	}

	public String getFont()
	{
		return PersistHelper.createFontString(getComponent().getFont());
	}

	public int getWidth()
	{
		if (!sizeSet && getComponent() instanceof ISupportSimulateBoundsProvider &&
			((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider() != null)
		{
			Rectangle bounds = ((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider().getBounds(getComponent());
			if (bounds != null)
			{
				return bounds.width;
			}
		}
		return getComponent().getSize().width;
	}

	public int getHeight()
	{
		if (!sizeSet && getComponent() instanceof ISupportSimulateBoundsProvider &&
			((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider() != null)
		{
			Rectangle bounds = ((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider().getBounds(getComponent());
			if (bounds != null)
			{
				return bounds.height;
			}
		}
		return getComponent().getSize().height;
	}

	public boolean isVisible()
	{
		return getComponent().isVisible();
	}

	public void setVisible(boolean b)
	{
		boolean old = isVisible();
		if (b != old && (!(getComponent() instanceof ISupportSecuritySettings) || ((ISupportSecuritySettings)getComponent()).isViewable()))
		{
			getComponent().setComponentVisible(b);
			getChangesRecorder().setVisible(b);
			propertyChanged("visible", Boolean.valueOf(b), Boolean.valueOf(old));
		}
	}

	public boolean isTransparent()
	{
		return !getComponent().isOpaque();
	}

	public void setTransparent(boolean b)
	{
		boolean old = isTransparent();
		if (b != old)
		{
			getComponent().setOpaque(!b);
			getChangesRecorder().setTransparent(b);
			if (!b)
			{
				// was transparent before
				String background = getBgcolor();
				if (background != null)
				{
					// reapply background color
					getChangesRecorder().setBgcolor(background);
				}
			}
			if (getComponent() instanceof JComponent)
			{
				((JComponent)getComponent()).repaint();
			}
			propertyChanged("transparant", Boolean.valueOf(b), Boolean.valueOf(old));
		}
	}

	public void setEnabled(final boolean b)
	{
		boolean old = isEnabled();
		if (b != old)
		{
			getComponent().setComponentEnabled(b);
			propertyChanged("enabled", Boolean.valueOf(b), Boolean.valueOf(old));
		}
	}

	public boolean isEnabled()
	{
		return getComponent().isEnabled();
	}

	private boolean locationSet = false;
	private boolean sizeSet = false;

	public void setLocation(int x, int y)
	{
		Point newValue = new Point(x, y);
		Point oldValue = new Point(getLocationX(), getLocationY());
		if (!newValue.equals(oldValue))
		{
			getComponent().setLocation(newValue);
			getChangesRecorder().setLocation(x, y);
			if (getComponent() instanceof JComponent)
			{
				((JComponent)getComponent()).validate();
			}
			propertyChanged("location", newValue, oldValue);
		}
		if (getComponent() instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)getComponent()).setCachedLocation(new Point(x, y));
		}
		locationSet = true;
	}

	protected final void setComponentSize(Dimension size)
	{
		Dimension oldSize = getComponent().getSize();
		// sets the component, changes recorder is not called here
		if (getComponent() instanceof ISupportCachedLocationAndSize)
		{
			((ISupportCachedLocationAndSize)getComponent()).setCachedSize(size);
		}
		getComponent().setSize(size);
		if (getComponent() instanceof JComponent)
		{
			((JComponent)getComponent()).validate();
		}
		sizeSet = true;
		propertyChanged("size", size, oldSize);
	}

	public String getName()
	{
		String jsName = getComponent().getName();
		if (jsName != null && jsName.startsWith(ComponentFactory.WEB_ID_PREFIX)) jsName = null;
		return jsName;
	}


	public int getLocationX()
	{
		if (!locationSet && getComponent() instanceof ISupportSimulateBoundsProvider &&
			((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider() != null)
		{
			Rectangle bounds = ((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider().getBounds(getComponent());
			if (bounds != null)
			{
				return bounds.x;
			}
		}
		return getComponent().getLocation().x;
	}

	public int getLocationY()
	{
		if (!locationSet && getComponent() instanceof ISupportSimulateBoundsProvider &&
			((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider() != null)
		{
			Rectangle bounds = ((ISupportSimulateBoundsProvider)getComponent()).getBoundsProvider().getBounds(getComponent());
			if (bounds != null)
			{
				return bounds.y;
			}
		}
		return getComponent().getLocation().y;
	}

	public void putClientProperty(Object key, Object value)
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

	public Object getClientProperty(Object key)
	{
		if (getComponent() instanceof JComponent)
		{
			return ((JComponent)getComponent()).getClientProperty(key);
		}
		if (clientProperties == null) return null;
		return clientProperties.get(key);
	}

	/**
	 * Should we allow all html to be displayed as-is?
	 *
	 * When false, the client should sanitize the html contents.
	 */
	public boolean trustDataAsHtml()
	{
		// set at element level
		Object trustDataAsHtml = getClientProperty(IApplication.TRUST_DATA_AS_HTML);

		if (trustDataAsHtml == null)
		{
			// or at solution level
			trustDataAsHtml = application.getClientProperty(IApplication.TRUST_DATA_AS_HTML);
		}

		if (trustDataAsHtml == null)
		{
			// or at application server level
			trustDataAsHtml = Boolean.valueOf(Settings.getInstance().getProperty(Settings.TRUST_DATA_AS_HTML_SETTING, Boolean.FALSE.toString()));
		}

		return Boolean.TRUE.equals(trustDataAsHtml);
	}

	public String getBorder()
	{
		return ComponentFactoryHelper.createBorderString(getComponent().getBorder());
	}

	public void setBorder(String spec)
	{
		String old = getBorder();
		if (!Utils.stringSafeEquals(old, spec))
		{
			getComponent().setBorder(ComponentFactoryHelper.createBorder(spec));
			getChangesRecorder().setBorder(spec);
			propertyChanged("border", spec, old);
		}
	}

	public String getToolTipText()
	{
		return getComponent().getToolTipText();
	}

	public void setToolTipText(String tooltip)
	{
		String old = getToolTipText();
		if (!Utils.stringSafeEquals(old, tooltip))
		{
			getComponent().setToolTipText(tooltip);
			getChangesRecorder().setChanged();
			propertyChanged("toolTipText", tooltip, old);
		}
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.ui.runtime.HasRuntimeDesignTimeProperty#getDesignTimeProperty(java.lang.String)
	 */
	public Object getDesignTimeProperty(String key)
	{
		if (getPersist() instanceof AbstractBase)
		{
			return Utils.parseJSExpression(((AbstractBase)getPersist()).getCustomDesignTimeProperty(key));
		}
		return null;
	}

	public String toString(String valueString)
	{
		if (getComponent() == null)
		{
			return "ScriptObject (component not yet set): " + getClass(); //$NON-NLS-1$
		}
		return getElementType() + "[name:" + getName() + ",x:" + getLocationX() + ",y:" + getLocationY() + ",width:" + getWidth() + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			",height:" + getHeight() + (valueString == null ? "" : (',' + valueString)) + ']'; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

	private PropertyChangeSupport changeSupport;

	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		synchronized (this)
		{
			if (listener == null)
			{
				return;
			}
			if (changeSupport == null)
			{
				changeSupport = new PropertyChangeSupport(this);
			}
			changeSupport.addPropertyChangeListener(listener);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		synchronized (this)
		{
			if (listener == null || changeSupport == null)
			{
				return;
			}
			changeSupport.removePropertyChangeListener(listener);
		}
	}

	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
	{
		PropertyChangeSupport changeSupport;
		synchronized (this)
		{
			changeSupport = this.changeSupport;
		}
		if (changeSupport == null || (oldValue != null && newValue != null && oldValue.equals(newValue)))
		{
			return;
		}
		changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	@SuppressWarnings("cast")
	public String getFormName()
	{

		if (application instanceof IProvideFormName)
		{
			return ((IProvideFormName)application).getFormNameFor(component);
		}
		return null;
	}

	@Override
	public void addStyleClass(String styleName)
	{
		//only implemented in ngclient
	}

	@Override
	public void removeStyleClass(String styleName)
	{
		//only implemented in ngclient

	}
}