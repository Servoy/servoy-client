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
package com.servoy.j2db.util.gui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JProgressBar;
import javax.swing.Timer;


/**
 * A component that, by default, displays an integer value within a bounded interval. A progress bar typically communicates the progress of some work by
 * displaying its percentage of completion and possibly a textual display of this percentage.
 * 
 * <p>
 * 
 * To indicate that a task of unknown length is executing, you can put a progress bar into indeterminate mode. While the bar is in indeterminate mode, it
 * animates constantly to show that work is occurring. As soon as you can determine the task's length and amount of progress, you should update the progress
 * bar's value and switch it back to determinate mode.
 * 
 * <p>
 * 
 * Here is an example of creating a progress bar, where <code>task</code> is an object that returns information about the progress of some work:
 * 
 * <pre>
 * rogressBar = new JProgressBar(0, task.getLengthOfTask());
 * rogressBar.setValue(0);
 * rogressBar.setStringPainted(true);
 * </pre>
 * 
 * Here is an example of updating the value of the progress bar:
 * 
 * <pre>
 * rogressBar.setValue(task.getCurrent());
 * </pre>
 * 
 * Here is an example of putting a progress bar into indeterminate mode, and then switching back to determinate mode once the length of the task is known:
 * 
 * <pre>
 * rogressBar = new JProgressBar();
 * <em>
 * ...//when the task of (initially) unknown length begins:
 * </em>
 * rogressBar.setIndeterminate(true);
 * <em>
 * ...//do some work; get length of task...
 * </em>
 * rogressBar.setMaximum(newLength);
 * rogressBar.setValue(newValue);
 * rogressBar.setIndeterminate(false);
 * </pre>
 * 
 * <p>
 * 
 * For complete examples and further documentation see <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/progress.html" target="_top">How to
 * Monitor Progress</a>, a section in <em>The Java Tutorial.</em>
 * 
 * <p>
 * <strong>Warning:</strong> Serialized objects of this class will not be compatible with future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running the same version of Swing. As of 1.4, support for long term storage of all JavaBeans<sup><font
 * size="-2">TM</font></sup> has been added to the <code>java.beans</code> package. Please see {@link java.beans.XMLEncoder}.
 * 
 * @see javax.swing.plaf.basic.BasicProgressBarUI
 * 
 * @beaninfo attribute: isContainer false description: A component that displays an integer value.
 * 
 * @author Michael C. Albers
 * @author Kathy Walrath
 */
public class JDK131ProgressBar extends JProgressBar
{
	public JDK131ProgressBar()
	{
		super();
	}

	/**
	 * Whether the progress bar is indeterminate (<code>true</code>) or normal (<code>false</code>); the default is <code>false</code>.
	 * 
	 * @see #setIndeterminate
	 * @since 1.4
	 */
	private boolean indeterminate;

	/**
	 * Sets the <code>indeterminate</code> property of the progress bar, which determines whether the progress bar is in determinate or indeterminate mode. An
	 * indeterminate progress bar continuously displays animation indicating that an operation of unknown length is occurring. By default, this property is
	 * <code>false</code>. Some look and feels might not support indeterminate progress bars; they will ignore this property.
	 * 
	 * <p>
	 * 
	 * See <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/progress.html" target="_top">How to Monitor Progress</a> for examples of using
	 * indeterminate progress bars.
	 * 
	 * @param newValue <code>true</code> if the progress bar should change to indeterminate mode; <code>false</code> if it should revert to normal.
	 * 
	 * @see #isIndeterminate
	 * @see javax.swing.plaf.basic.BasicProgressBarUI
	 * 
	 * @since 1.4
	 * 
	 * @beaninfo bound: true attribute: visualUpdate true description: Set whether the progress bar is indeterminate (true) or normal (false).
	 */
	@Override
	public void setIndeterminate(boolean newValue)
	{
		boolean oldValue = indeterminate;
		indeterminate = newValue;

		if (check == null)
		{
			PropertyChangeListener[] listners = getListeners(PropertyChangeListener.class);
			for (PropertyChangeListener propertyChangeListener : listners)
			{
				if (propertyChangeListener.getClass().getName().endsWith("BasicProgressBarUI$PropertyChangeHandler")) //$NON-NLS-1$
				{
					//is 141
					check = new Boolean(true);
					break;
				}
			}
			if (check == null)
			{
				//register our own listener
				PropertyChangeHandler propertyListener = new PropertyChangeHandler();
				addPropertyChangeListener(propertyListener);

				check = new Boolean(true);
			}
		}
		firePropertyChange("indeterminate", oldValue, indeterminate); //$NON-NLS-1$
	}

	private Boolean check = null;

	private class PropertyChangeHandler implements PropertyChangeListener, ActionListener
	{
		private Timer longtimer;
		private Timer shorttimer;

		PropertyChangeHandler()
		{
			setMaximum(100);
		}

		public void propertyChange(PropertyChangeEvent e)
		{
			String prop = e.getPropertyName();
			if ("indeterminate".equals(prop)) //$NON-NLS-1$
			{
				if (isIndeterminate())
				{
					longtimer = new Timer(2000, this);
					longtimer.setRepeats(true);
					longtimer.start();

					shorttimer = new Timer(400, this);
					shorttimer.setRepeats(true);

					actionPerformed(null);
				}
				else
				{
					longtimer.stop();
					shorttimer.stop();
					setValue(0);
				}

			}
		}

		public void actionPerformed(ActionEvent e)
		{
			if (getValue() == 100)
			{
				setValue(0);
				shorttimer.stop();
			}
			else
			{
				setValue(100);
				shorttimer.start();
			}
			repaint();
		}
	}

	/**
	 * Returns the value of the <code>indeterminate</code> property.
	 * 
	 * @return the value of the <code>indeterminate</code> property
	 * @see #setIndeterminate
	 * 
	 * @since 1.4
	 * 
	 * @beaninfo description: Is the progress bar indeterminate (true) or normal (false)?
	 */
	@Override
	public boolean isIndeterminate()
	{
		return indeterminate;
	}

}
