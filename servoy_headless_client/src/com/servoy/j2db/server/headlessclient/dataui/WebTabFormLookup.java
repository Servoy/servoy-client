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
package com.servoy.j2db.server.headlessclient.dataui;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.server.headlessclient.WebForm;
import com.servoy.j2db.ui.IFormLookupPanel;

/**
 * Web implementation of the {@link IFormLookupPanel}
 * 
 * @author jcompagner
 */
public class WebTabFormLookup implements IFormLookupPanel
{
	private final String relationName;
	private final String formName;
	private WebForm webForm;
	private final String tabName;
	private final WebMarkupContainer parent;
	private final IApplication application;
	private List<SortColumn> defaultSort;

	WebTabFormLookup(String tabname, String relationName, String formName, WebMarkupContainer parent, IApplication app)
	{
		this.tabName = tabname;
		this.formName = formName;
		this.relationName = relationName;
		this.parent = parent;
		this.application = app;
	}

	public String getRelationName()
	{
		return relationName;
	}

	public String getFormName()
	{
		return formName;
	}

	public boolean isReady()
	{
		return (webForm != null && !webForm.isDestroyed());
	}

	public boolean isFormReady()
	{
		boolean isFormReady = true;
		WebForm wf = getWebForm(false);
		if (wf != null)
		{
			MarkupContainer wfParent = wf.getParent();
			if (wfParent instanceof WebTabPanel && ((WebTabPanel)wfParent).isVisible() && wfParent.getParent() != null)
			{
				boolean isTabPanelVisible = true;

				Component c = wfParent;
				WebForm pwf;
				while ((pwf = c.findParent(WebForm.class)) != null && (isTabPanelVisible = pwf.getController().isFormVisible()) == true)
					c = pwf;

				// if the form is current in another visible tabpanel, then it is not ready for this tabpanel
				isFormReady = !(isTabPanelVisible && wfParent != parent && ((WebTabPanel)wfParent).getCurrentForm() == wf);
			}
		}

		return isFormReady;
	}

	public boolean isReadOnly()
	{
		if (isReady())
		{
			return getWebForm().getController().isReadOnly();
		}
		return false;
	}

	public void setReadOnly(boolean readOnly)
	{
		if (isReady())
		{
			getWebForm().getController().setReadOnly(readOnly);
		}
		else
		{
			((FormManager)application.getFormManager()).setFormReadOnly(formName, readOnly);
		}
	}

	public List<SortColumn> getDefaultSort(boolean create)
	{
		// cache the default sort, so that the second call will not create the form (which could be destroyed) just for the sort columns.
		if (defaultSort == null && (isReady() || create))
		{
			FormController fc = getWebForm().getController();
			if (fc != null)
			{
				defaultSort = fc.getDefaultSortColumns();
			}
		}
		if (defaultSort == null) return new ArrayList<SortColumn>();
		return defaultSort;
	}

	public WebForm getWebForm()
	{
		return getWebForm(true);
	}

	private WebForm getWebForm(boolean removeFromParent)
	{
		if (webForm != null && webForm.isDestroyed())
		{
			webForm = null;
		}
		if (webForm == null)
		{
			FormManager fm = (FormManager)application.getFormManager();
			FormController fc = fm.getFormController(formName, this);
			if (fc == null)
			{
				fc = fm.leaseFormPanel(formName);
			}
			if (fc != null)
			{
				//delegate readOnly, really set it once from the form manager state
				fc.setReadOnly(fm.isFormReadOnly(formName));

				webForm = (WebForm)fc.getFormUI();

				if (removeFromParent && webForm.getParent() != null && webForm.getParent() != parent)
				{
					webForm.remove();
				}
			}
		}
		return webForm;
	}

	public void setWebForm(WebForm webForm)
	{
		this.webForm = webForm;
	}

	public boolean notifyVisible(boolean b, List invokeLaterRunnables)
	{
		if (isReady())
		{
			IFormUIInternal ui = webForm;
			FormController child = ui.getController();
			return child.notifyVisible(b, invokeLaterRunnables);
		}
		return true;
	}

	public String getName()
	{
		return tabName;
	}
}
