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

import java.awt.Color;
import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.lang.reflect.Method;
import java.util.concurrent.RejectedExecutionException;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.UIManager;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;

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
	public static Object getUIProperty(IRuntimeComponent component, IApplication application, String name, Object defaultValue)
	{
		Object val = component.getClientProperty(name);
		if (val == null)
		{
			val = application.getClientProperty(name);
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

	public static Rectangle getCenteredBoundsOn(Rectangle srcBounds, int width, int height)
	{
		return new Rectangle(srcBounds.x + (srcBounds.width - width) / 2, srcBounds.y + (srcBounds.height - height) / 2, width, height);
	}

	// TODO how can we refactor this to handle exceptions of unknown subtypes nicer, allowing a variable number of exception types?
	public static abstract class ThrowingRunnable<E1T extends Exception, E2T extends Exception> implements Runnable
	{

		protected E1T e1;
		protected E2T e2;

		public void throwExceptionIfNeeded() throws E1T, E2T
		{
			if (e1 != null) throw e1;
			if (e2 != null) throw e2;
		}

	}

	public static <E1T extends Exception, E2T extends Exception> void runWhileDispatchingEvents(final ThrowingRunnable<E1T, E2T> r,
		IServiceProvider serviceProvider) throws E1T, E2T
	{
		final boolean done[] = new boolean[1];
		Runnable tmp = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					r.run();
				}
				finally
				{
					done[0] = true;
				}
			}
		};

		if (serviceProvider.isEventDispatchThread())
		{
			// dispatch events for UI responsiveness while the actual work runs in a background thread
			try
			{
				serviceProvider.getScheduledExecutor().execute(tmp);
			}
			catch (RejectedExecutionException e)
			{
				tmp.run();
			}

			while (!done[0])
			{
				SwingHelper.dispatchEvents(20);
				try
				{
					Thread.sleep(20);
				}
				catch (InterruptedException e)
				{
					Debug.warn(e);
				}
			}
		}
		else
		{
			tmp.run();
		}
		r.throwExceptionIfNeeded();
	}

	public static boolean setWindowTransparency(Window w, Container contentPane, boolean undecoratedW, boolean transparent, boolean complainInLogs)
	{
		boolean applied = false;
		if ((JDialog.isDefaultLookAndFeelDecorated() || undecoratedW))
		{
			if (contentPane instanceof JComponent || transparent != contentPane.isOpaque())
			{
				// set on window if possible
				if (JavaVersion.CURRENT_JAVA_VERSION.major >= 7)
				{
					// set it the Java 7 way with bg color that has alpha 0
					if (transparent)
					{
						try
						{
							Color oldC = w.getBackground();
							Color newC = (oldC != null) ? new Color(oldC.getRed(), oldC.getGreen(), oldC.getBlue(), 0) : new Color(255, 255, 255, 0);
							w.setBackground(newC);
							applied = true;
						}
						catch (Exception ex)
						{
							if (complainInLogs) Debug.trace("Error while trying to set transparency on window using v7 API; the capability might be missing.",
								ex);
						}
					}
					else
					{
						w.setBackground(null);
						applied = true;
					}
				}
				else if (JavaVersion.CURRENT_JAVA_VERSION.major == 6 && JavaVersion.CURRENT_JAVA_VERSION.update >= 10) // see http://docs.oracle.com/javase/tutorial/uiswing/misc/trans_shaped_windows.html
				{
					try
					{
						// for java 1.6 u10 or later try this, as the above will only work in java 7
						// AWTUtilities.setWindowOpaque(boolean)
						Class< ? > awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
						Method mSetWindowOpaque = awtUtilitiesClass.getMethod("setWindowOpaque", Window.class, boolean.class);
						mSetWindowOpaque.invoke(null, w, Boolean.valueOf(!transparent));
						applied = true;
					}
					catch (Exception ex)
					{
						if (complainInLogs) Debug.trace("Error while trying to set transparency on window using v6 API; the capability might be missing.", ex);
					}
				}
				else
				{
					if (complainInLogs) Debug.warn("Cannot set transparency on window; it is supported only with Java 6 update 10 or higher.");
				}

				// also set it on intermediate panes
				if (applied && contentPane instanceof JComponent) ((JComponent)contentPane).setOpaque(!transparent); // normally it will be a JComponent
			}
			else
			{
				if (complainInLogs) Debug.warn("Transparency will no be applied. Can't change opaque value (not JComponent).");
			}
		}
		else
		{
			if (complainInLogs) Debug.warn("Transparency will no be applied to some decorated dialogs. It can lead to strange visual effects.");
		}
		return applied;
	}
}