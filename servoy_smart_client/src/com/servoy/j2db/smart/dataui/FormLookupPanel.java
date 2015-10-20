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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.ui.IFormLookupPanel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.EnablePanel;
import com.servoy.j2db.util.ITabPaneAlike;

public class FormLookupPanel extends EnablePanel implements IFormLookupPanel
{
	private final String formName;
	private final String relationName;
	private final IApplication application;

	FormLookupPanel(IApplication app, String tabname, String relationName, String formName)
	{
		application = app;
		this.relationName = relationName;
		this.formName = formName;
		setName(tabname);
		setLayout(new BorderLayout());
		setOpaque(true);
		// this is a fix for 136387
		setFocusable(false);
	}

	@Override
	public Color getBackground()
	{
		Container parent = getParent();
		while (parent != null && !parent.isOpaque())
		{
			parent = parent.getParent();
		}
		if (parent != null)
		{
			return parent.getBackground();
		}
		return super.getBackground();
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		if (isReady())
		{
			getFormPanel().setReadOnly(readOnly);
		}
		else
		{
			((FormManager)application.getFormManager()).setFormReadOnly(formName, readOnly);
		}
	}

	public boolean isReadOnly()
	{
		if (isReady())
		{
			return getFormPanel().isReadOnly();
		}
		return false;
	}

	/**
	 * @see com.servoy.j2db.ui.IComponent#setComponentVisible(boolean)
	 */
	public void setComponentVisible(boolean b_visible)
	{
		setVisible(b_visible);
	}


	/*
	 * method overriden for printing of tabpanel public int getComponentCount() { int count = super.getComponentCount(); if (count == 0) { createFormPanel();
	 * return super.getComponentCount(); } return count; }
	 */
	@Override
	public void setVisible(boolean aFlag)
	{
		if (aFlag)
		{
			if (super.getComponentCount() == 0)
			{
// Test if this is really needed, enabled then execution flow (onload/onshow) goes wrong, tabpanels go first.
//				createFormPanel();
			}
		}
		super.setVisible(aFlag);
	}

	private FormController createFormPanel()
	{
		FormManager fm = (FormManager)application.getFormManager();
		FormController fp = fm.getFormController(formName, this);
		if (fp != null)
		{
			IFormUIInternal ui = fp.getFormUI();
			if (ui instanceof Component)
			{
				add((Component)ui, BorderLayout.CENTER);
				ui.setComponentVisible(true);//just to be sure the cardlayout of main panel does return them as not visible

				//delegate readOnly, really set it once from the form manager state
				fp.setReadOnly(fm.isFormReadOnly(formName));

				Container con = getParent();
				if (con != null && (con instanceof ITabPaneAlike) && !con.isEnabled())
				{
					// reaply the isEnabled state of the tabpanel to its child tabs (tabs are added after enabled state is set); only if the tabpanel is disabled
					this.setEnabled(con.isEnabled());
				}
				while (con != null)
				{
					if (con instanceof IFormUIInternal)
					{
						fp.getUndoManager().setFormUndoManager(((IFormUIInternal)con).getUndoManager());
						break;
					}
					con = con.getParent();
				}
				// invalidate later so that everything is first visible (like the datamodel of a tableview)
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						validate();
					}
				});
			}
		}
		return fp;
	}

	public String getFormName()
	{
		return formName;
	}

	private List<SortColumn> defaultSort = null;

	public List<SortColumn> getDefaultSort()
	{
		if (defaultSort == null)
		{
			try
			{
				FormController fc = getFormPanel();
				if (fc != null)
				{
					Form f = fc.getForm();
					defaultSort = ((FoundSetManager)application.getFoundSetManager()).getSortColumns(f.getDataSource(), f.getInitialSort());
				}
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
				defaultSort = new ArrayList<SortColumn>(1);
			}
		}
		return defaultSort;
	}

	public FormController getFormPanel()
	{
		Component[] childs = getComponents();
		if (childs.length == 0)
		{
			return createFormPanel();
		}
		if (childs.length == 1)
		{
			IFormUIInternal ui = (IFormUIInternal)childs[0];
			return ui.getController();
		}
		return null;
	}

	public String getRelationName()
	{
		return relationName;
	}

	public boolean isReady()
	{
		Component[] childs = getComponents();
		return (childs.length != 0);
	}

	/**
	 * This method must be called on the event thread
	 */
	public boolean notifyVisible(boolean visible, List invokeLaterRunnables)
	{
		Component[] childs = getComponents();
		if (childs.length == 1)
		{
			IFormUIInternal ui = (IFormUIInternal)childs[0];
			FormController child = ui.getController();
			return child.notifyVisible(visible, invokeLaterRunnables);
		}
		return true;
	}

	public boolean stopUIEditing(boolean looseFocus)
	{
		Component[] childs = getComponents();
		if (childs.length == 1)
		{
			return ((IFormUIInternal)childs[0]).getController().stopUIEditing(looseFocus);
		}
		return true;
	}

	/**
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString()
	{
		return "Form:" + formName + " ready:" + isReady() + "," + super.toString();
	}

}
