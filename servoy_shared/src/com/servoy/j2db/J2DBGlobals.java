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
package com.servoy.j2db;


import java.beans.PropertyChangeListener;
import java.util.WeakHashMap;

import javax.swing.event.SwingPropertyChangeSupport;

public class J2DBGlobals
{
	private static final ThreadLocal<IServiceProvider> serviceprovider = new ThreadLocal<IServiceProvider>();

	public static final String SERVOY_APPLICATION_SERVER_DIRECTORY_KEY = "servoy_application_server.dir"; //$NON-NLS-1$
	public static final String SERVOY_APPLICATION_SERVER_CONTEXT_KEY = "servoy.application_server.context"; //$NON-NLS-1$

	public static final String CLIENT_LOCAL_DIR = "/.servoy/"; //$NON-NLS-1$

	private static WeakHashMap<Object, SwingPropertyChangeSupport> changeSupportMap = new WeakHashMap<Object, SwingPropertyChangeSupport>();
	private static IServiceProvider singletonServiceProvider;

	public static IServiceProvider setSingletonServiceProvider(IServiceProvider serviceprovider)
	{
		IServiceProvider old = J2DBGlobals.singletonServiceProvider;
		J2DBGlobals.singletonServiceProvider = serviceprovider;
		return old;
	}
	
	public static IServiceProvider getThreadServiceProvider()
	{
		return serviceprovider.get();
	}

	public static IServiceProvider getSingletonServiceProvider()
	{
		return singletonServiceProvider;
	}

	// DO NOT change these methods: this is now used in jasper reports plugin
	public static void setServiceProvider(IServiceProvider provider)
	{
		serviceprovider.set(provider);
	}

	public static IServiceProvider getServiceProvider()
	{
		IServiceProvider provider = serviceprovider.get();
		if (provider == null)
		{
			return singletonServiceProvider;
		}
		return provider;
	}

	/**
	 * synchronized not needed SwingPropertyChangeSupport is thread safe
	 * 
	 * Supports reporting bound property changes. If <code>oldValue</code> and <code>newValue</code> are not equal and the <code>PropertyChangeEvent</code>
	 * listener list isn't empty, then fire a <code>PropertyChange</code> event to each listener. This method has an overloaded method for each primitive
	 * type. For example, here's how to write a bound property set method whose value is an integer:
	 * 
	 * <pre>
	 * public void setFoo(int newValue)
	 * {
	 * 	int oldValue = foo;
	 * 	foo = newValue;
	 * 	firePropertyChange(&quot;foo&quot;, oldValue, newValue);
	 * }
	 * </pre>
	 * 
	 * @param propertyName the programmatic name of the property that was changed
	 * @param oldValue the old value of the property (as an Object)
	 * @param newValue the new value of the property (as an Object)
	 * @see java.beans.PropertyChangeSupport
	 */
	public static void firePropertyChange(Object src, String propertyName, Object oldValue, Object newValue)
	{
		if (oldValue == null && newValue == null) return;
		SwingPropertyChangeSupport changeSupport = changeSupportMap.get(src);
		if (changeSupport != null && changeSupport.hasListeners(propertyName))
		{
			changeSupport.firePropertyChange(propertyName, oldValue, newValue);
		}
	}

	/**
	 * Adds a <code>PropertyChangeListener</code> to the listener list. The listener is registered for all properties.
	 * <p>
	 * A <code>PropertyChangeEvent</code> will get fired in response to setting a bound property, such as <code>setFont</code>, <code>setBackground</code>,
	 * or <code>setForeground</code>.
	 * <p>
	 * Note that if the current component is inheriting its foreground, background, or font from its container, then no event will be fired in response to a
	 * change in the inherited property.
	 * 
	 * @param listener the <code>PropertyChangeListener</code> to be added
	 */
	public static synchronized void addPropertyChangeListener(Object src, PropertyChangeListener listener)
	{
		SwingPropertyChangeSupport changeSupport = changeSupportMap.get(src);
		if (changeSupport == null)
		{
			changeSupport = new SwingPropertyChangeSupport(src);
			changeSupportMap.put(src, changeSupport);
		}
		changeSupport.addPropertyChangeListener(listener);
	}


	/**
	 * Adds a <code>PropertyChangeListener</code> for a specific property. The listener will be invoked only when a call on <code>firePropertyChange</code>
	 * names that specific property.
	 * <p>
	 * If listener is <code>null</code>, no exception is thrown and no action is performed.
	 * 
	 * @param propertyName the name of the property to listen on
	 * @param listener the <code>PropertyChangeListener</code> to be added
	 */
	public static synchronized void addPropertyChangeListener(Object src, String propertyName, PropertyChangeListener listener)
	{
		if (listener == null)
		{
			return;
		}
		SwingPropertyChangeSupport changeSupport = changeSupportMap.get(src);
		if (changeSupport == null)
		{
			changeSupport = new SwingPropertyChangeSupport(src);
			changeSupportMap.put(src, changeSupport);
		}
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Removes a <code>PropertyChangeListener</code> from the listener list. This removes a <code>PropertyChangeListener</code> that was registered for all
	 * properties.
	 * 
	 * @param listener the <code>PropertyChangeListener</code> to be removed
	 */
	public static synchronized void removePropertyChangeListener(Object src, PropertyChangeListener listener)
	{
		SwingPropertyChangeSupport changeSupport = changeSupportMap.get(src);
		if (changeSupport != null)
		{
			changeSupport.removePropertyChangeListener(listener);
//DISABLED:for 1.4			
//			if (changeSupport.getPropertyChangeListeners().length == 0)
//			{
//				changeSupportMap.remove(changeSupport);
//			}
		}
	}

	public static synchronized void removeAllPropertyChangeListeners(Object src)
	{
		changeSupportMap.remove(src);
	}

	/**
	 * Removes a <code>PropertyChangeListener</code> for a specific property. If listener is <code>null</code>, no exception is thrown and no action is
	 * performed.
	 * 
	 * @param propertyName the name of the property that was listened on
	 * @param listener the <code>PropertyChangeListener</code> to be removed
	 */
	public static synchronized void removePropertyChangeListener(Object src, String propertyName, PropertyChangeListener listener)
	{
		if (listener == null)
		{
			return;
		}
		SwingPropertyChangeSupport changeSupport = changeSupportMap.get(src);
		if (changeSupport == null)
		{
			return;
		}
		changeSupport.removePropertyChangeListener(propertyName, listener);
		if (!changeSupport.hasListeners(propertyName))
		{
			changeSupportMap.remove(changeSupport);
		}
	}
}
