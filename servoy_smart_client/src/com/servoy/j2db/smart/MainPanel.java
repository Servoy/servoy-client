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
package com.servoy.j2db.smart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormManager.History;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.ISupportNavigator;
import com.servoy.j2db.scripting.JSWindowImpl;
import com.servoy.j2db.smart.dataui.ServoyFocusTraversalPolicy;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IFocusCycleRoot;
import com.servoy.j2db.util.gui.FixedCardLayout;

public class MainPanel extends JPanel implements ISupportNavigator, IMainContainer, IFocusCycleRoot<Component>
{
	private static final long serialVersionUID = 1L;

	protected IApplication application;
	protected FixedCardLayout forms;
	protected JPanel tableFormPanel;
	private final String containerName;

	private List<Component> tabSeqComponentList = new ArrayList<Component>();

	public MainPanel(IApplication app, String containerName)
	{
		super(new BorderLayout());
		this.containerName = containerName;
		application = app;
		tableFormPanel = new JPanel();
		tableFormPanel.setName("Main_panel__table_form_panel"); //$NON-NLS-1$
		forms = new FixedCardLayout();
		tableFormPanel.setLayout(forms);
		add(tableFormPanel, BorderLayout.CENTER);

		emptyPanel.setName("Main_panel__empty_panel"); //$NON-NLS-1$

		setFocusCycleRoot(true);
		setFocusTraversalPolicy(ServoyFocusTraversalPolicy.datarenderPolicy);
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getContainerName()
	 */
	public String getContainerName()
	{
		return containerName;
	}

	public void add(IComponent c, String name)
	{
		tableFormPanel.add((Component)c, name);
	}

	public void show(String name)
	{
		forms.show(tableFormPanel, name);

		Component cmp = null;
		for (int i = 0; i < tableFormPanel.getComponentCount(); i++)
		{
			Component c = tableFormPanel.getComponent(i);
			if ((c != null) && c.isVisible())
			{
				cmp = c;
				break;
			}
		}

		// Place into the tab sequence the current form and, if exists, the navigator.
		tabSeqComponentList.clear();
		if (cmp instanceof SwingForm)
		{
			SwingForm sf = (SwingForm)cmp;
			tabSeqComponentList.add(sf);
		}

		if (getNavigator() != null)
		{
			IFormUIInternal fui = getNavigator().getFormUI();
			if (fui instanceof SwingForm)
			{
				SwingForm sf = (SwingForm)fui;
				tabSeqComponentList.add(sf);
			}
		}
	}

	public void remove(IComponent c)
	{
		tableFormPanel.remove((Component)c);
	}

	@Override
	public void removeAll()
	{
		flushCachedItems();
	}

	public void flushCachedItems()
	{
		navigator = null;
		currentForm = null;
		tableFormPanel.removeAll();
		tabSeqComponentList.clear();
		if (history != null)
		{
			history.clear();
		}
	}

	private JLabel loadingLabel = null;

	public void showSolutionLoading(boolean b)
	{
		if (b)
		{
			if (loadingLabel == null)
			{
				String branding = application.getSettings().getProperty("servoy.branding", "false"); //$NON-NLS-1$ //$NON-NLS-2$
				String loadingImage = application.getSettings().getProperty("servoy.branding.loadingimage"); //$NON-NLS-1$
				if (branding.equals("true") && loadingImage != null && application.getApplicationType() == IApplication.CLIENT) //$NON-NLS-1$
				{
					if (loadingImage.equals("")) //$NON-NLS-1$
					{
						loadingLabel = new JLabel();
					}
					else
					{
						URL webstartUrl = WebStart.getWebStartURL();
						try
						{
							URL url = new URL(webstartUrl.getProtocol(), webstartUrl.getHost(), webstartUrl.getPort(), loadingImage);
							loadingLabel = new JLabel(new ImageIcon(url), SwingConstants.CENTER);
						}
						catch (MalformedURLException ex)
						{
							Debug.error("Error loading the solution loading image", ex); //$NON-NLS-1$
						}
					}
				}

				if (loadingLabel == null)
				{
					loadingLabel = new JLabel(application.loadImage("solutionloading.gif"), SwingConstants.CENTER); //$NON-NLS-1$
				}
			}

			tableFormPanel.add(loadingLabel, "LoadingLabel"); //$NON-NLS-1$

			forms.show(tableFormPanel, "LoadingLabel"); //$NON-NLS-1$
		}
		else if (loadingLabel != null)
		{
			tableFormPanel.remove(loadingLabel);
		}
	}

	private final JPanel emptyPanel = new JPanel();

	public void showBlankPanel()
	{
		if (emptyPanel.getParent() == null)
		{
			tableFormPanel.add(emptyPanel, "_empty_"); //$NON-NLS-1$
		}
		forms.show(tableFormPanel, "_empty_"); //$NON-NLS-1$
	}


	public FormController getNavigator()
	{
		return navigator;
	}

	private FormController navigator;
	private FormController currentForm;
	private History history;

	public FormController setNavigator(FormController c)
	{
		FormController retval = null;
		if (c == navigator) return retval;//same
		if (navigator != null)
		{
			retval = navigator;
			remove((Component)navigator.getFormUI());
		}
		if (c != null)
		{
			// c.setEnabled(true);//even there are no records, still enable all elements
			JComponent form = (JComponent)c.getFormUI();
			Dimension size = c.getForm().getSize();
			if (form.getBorder() != null)
			{
				Insets insets = form.getBorder().getBorderInsets(form);
				size.width += insets.right + insets.left;
			}
			form.setPreferredSize(size);
			add(form, BorderLayout.LINE_START);
			c.getFormUI().setComponentVisible(true);
		}

		doLayout();// make sure it is shown
		if (c != null) ((Component)c.getFormUI()).repaint();
		navigator = c;
		return retval;
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	public void setComponentVisible(boolean b)
	{
		setVisible(b);
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}

	public Iterator getComponentIterator()
	{
		return null;
	}

	public void setFormController(FormController f)
	{
		this.currentForm = f;
		if (f != null)
		{
			add(f.getFormUI(), f.getName());
		}
	}

	public FormController getController()
	{
		if (currentForm != null)
		{
			if (((Component)currentForm.getFormUI()).getParent() == tableFormPanel)
			{
				return currentForm;
			}
			currentForm = null;
		}
		return currentForm;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#getHistory()
	 */
	public History getHistory()
	{
		if (history == null)
		{
			history = new FormManager.History(application, this);
		}
		return history;
	}

	/**
	 * @see com.servoy.j2db.IMainContainer#setTitle(java.lang.String)
	 */
	public void setTitle(String titleText)
	{
		if (((FormManager)application.getFormManager()).getMainContainer(null) == this)
		{
			application.setTitle(titleText);
		}
		else
		{
			JSWindowImpl w = application.getJSWindowManager().getWindow(getContainerName());
			if (w != null) w.setTitle(titleText);
		}
	}

	public boolean isTraversalPolicyEnabled()
	{
		return true;
	}

	public Component getFirstFocusableField()
	{
		if (tabSeqComponentList != null && tabSeqComponentList.size() > 0)
		{
			return tabSeqComponentList.get(0);
		}
		return null;
	}

	public Component getLastFocusableField()
	{
		if (tabSeqComponentList != null && tabSeqComponentList.size() > 0)
		{
			return tabSeqComponentList.get(tabSeqComponentList.size() - 1);
		}
		return null;
	}

	public List<Component> getTabSeqComponents()
	{
		return tabSeqComponentList;
	}

	public void setTabSeqComponents(List<Component> tabSequence)
	{
		this.tabSeqComponentList = tabSequence;
	}
}