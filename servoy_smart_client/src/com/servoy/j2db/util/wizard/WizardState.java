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
package com.servoy.j2db.util.wizard;


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * The overall wizard state
 * 
 * @author jblok
 */
public class WizardState implements IWizardState
{
/*
 * _____________________________________________________________ Declaration of attributes
 */
	private final Hashtable panels = new Hashtable();
	private final IWizard wizard;
	private final Vector panelHistory = new Vector();
	private IWizardPanel currentPanel = null;
	private final Hashtable properties = new Properties();

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public WizardState(IWizard wizard)
	{
		this.wizard = wizard;
	}

	public Object getProperty(String name)
	{
		return properties.get(name);
	}

	public void setProperty(String name, Object value)
	{
		if (value == null)
		{
			properties.remove(name);
		}
		else
		{
			properties.put(name, value);
		}
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */


/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */


/*
 * _____________________________________________________________ The methods below belong to this class
 */
	public Enumeration getAllPanels()
	{
		return panels.elements();
	}

	public void addNewPanel(IWizardPanel c)
	{
		panels.put(c.getName(), c);
	}

	public IWizardPanel getNextPanel()
	{
		String next = "start"; //$NON-NLS-1$
		if (currentPanel != null)
		{
			next = currentPanel.getNextPanelName();
			panelHistory.addElement(currentPanel);
		}
		currentPanel = (IWizardPanel)panels.get(next);
		if (currentPanel == null) throw new IllegalStateException("Panel with name: '" + next + "' not found"); //$NON-NLS-1$ //$NON-NLS-2$
		return currentPanel;
	}

	public IWizardPanel getPreviousPanel()
	{
		if (panelHistory.size() > 0)
		{
			currentPanel = (IWizardPanel)panelHistory.lastElement();
			panelHistory.removeElementAt(panelHistory.size() - 1);
			return currentPanel;
		}
		else
		{
			return null;
		}
	}

	public void handleButtons()
	{
//System.out.println("panelHistory.size() "+panelHistory.size());		
		if (panelHistory.size() == 0)
		{
			wizard.lockPrevButton();
		}
		else
		{
			wizard.unlockPrevButton();
//		    wizard.unlockNextButton();
		}

		if (currentPanel.getNextPanelName() == null)
		{
			wizard.lockNextButton();
		}
		else
		{
			wizard.unlockNextButton();
		}

		if (currentPanel.getNextPanelName() == null)
		{
			wizard.unlockFinishButton();
		}
		else
		{
			wizard.lockFinishButton();
		}
	}

	public boolean isFinished()
	{
		if (currentPanel != null)
		{
			return (currentPanel.getNextPanelName() == null);
		}
		else
		{
			return false;
		}
	}

	public boolean canProgress()
	{
		if (currentPanel != null)
		{
			return currentPanel.isDone();
		}
		else
		{
			return true;//only happens for first
		}
	}
}
