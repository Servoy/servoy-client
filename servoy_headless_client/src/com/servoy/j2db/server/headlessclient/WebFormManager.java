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
package com.servoy.j2db.server.headlessclient;

import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.IPageMap;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageMap;
import org.apache.wicket.Session;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IDisplayRelatedData;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.server.headlessclient.dataui.WebSplitPane;
import com.servoy.j2db.server.headlessclient.dataui.WebTabPanel;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * A {@link FormManager} implementation that is used in the webclient. 
 * 
 * @author jcompagner
 */
public class WebFormManager extends FormManager
{
	private final int maxForms;

	@SuppressWarnings("nls")
	public WebFormManager(IApplication app, IMainContainer mainp)
	{
		super(app, mainp);
		int max = Utils.getAsInteger(Settings.getInstance().getProperty("servoy.max.webforms.loaded", "128"), false);
		maxForms = max == 0 ? 128 : max;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.FormManager#getMaxFormsLoaded()
	 */
	@Override
	protected int getMaxFormsLoaded()
	{
		return maxForms;
	}

	@Override
	public IFormUIInternal getFormUI(FormController formController)
	{
		if (Utils.getAsBoolean(getApplication().getRuntimeProperties().get("isPrinting"))) //$NON-NLS-1$
		{
			return new SwingForm(formController);
		}
		return new WebForm(formController);
	}

	protected void addIPersistListenerHook()
	{
	}

	@Override
	protected boolean checkAndUpdateFormUser(FormController fp, Object parentContainer)
	{
		if (parentContainer instanceof MainPage)
		{
			return ((MainPage)parentContainer).getMainWebForm() != fp.getFormUI();
		}

		return true;
	}

	@Override
	protected void enableCmds(boolean enable)
	{
	}

	@Override
	public void fillScriptMenu()
	{
	}

	public void synchViewMenu(int viewType)
	{
	}

	protected void removeIPersistListenerHook()
	{
	}

	@Override
	protected void selectFormMenuItem(Form form)
	{
	}

	@Override
	protected boolean isShowingPrintPreview()
	{
		// Web Printing doesn't have to show the forms, it prints to PDF,t hat code sits in 
		// WebForm.print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
		return false;
	}

	@Override
	protected FormController removePreview()
	{
		// Web Printing doesn't have to show the forms, it prints to PDF,t hat code sits in 
		// WebForm.print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
		return null;
	}


	@Override
	public void showPreview(FormController afp, IFoundSetInternal foundset, int zoomFactor, PrinterJob printJob)
	{
		// Web Printing doesn't have to show the forms, it prints to PDF,t hat code sits in 
		// WebForm.print(boolean showDialogs, boolean printCurrentRecordOnly, boolean showPrinterSelectDialog, PrinterJob printerJob)
	}

	@Override
	protected void removeFormUser(FormController fp)
	{
	}

	public void reload(FormController[] fcontrollers)
	{
		IMainContainer main = getMainContainer(null);
		if (main instanceof Component)
		{
			// TODO other containers?
			((Component)main).setVersioned(false);
		}

		MainPage navigatorParent = null;
		String navigatorName = null;
		for (FormController fp : fcontrollers)
		{
			if (fp != null)
			{
				boolean formVisible = fp.isFormVisible();
				FoundSet foundset = fp.getFormModel();
				WebForm wf = (WebForm)fp.getFormUI();
				MarkupContainer wfParent = wf.getParent();

				fp.destroy();

				while (wfParent != null && !(wfParent instanceof WebTabPanel) && !(wfParent instanceof WebSplitPane) &&
					!(wfParent.getParent() instanceof MainPage) && !(wfParent.getParent() instanceof WebForm))
				{
					wfParent = wfParent.getParent();
				}

				if (wfParent instanceof WebTabPanel || wfParent instanceof WebSplitPane)
				{
					if (formVisible)
					{
						leaseFormPanel(fp.getName()).loadData(foundset, null);
						List<Runnable> runnables = new ArrayList<Runnable>();
						((IDisplayRelatedData)wfParent).notifyVisible(true, runnables);
						Utils.invokeLater(getApplication(), runnables);
					}
				}
				else if (wfParent != null)
				{
					WebForm parentWF = wfParent.findParent(WebForm.class);
					if (parentWF != null)
					{
						if (!Arrays.asList(fcontrollers).contains(parentWF.getController())) parentWF.getController().destroy();
					}
					else
					{
						MainPage parent = wfParent.findParent(MainPage.class);
						if (parent != null && parent.getNavigator() == fp)
						{
							navigatorParent = parent;
							navigatorName = fp.getName();
						}
					}
				}
			}
		}

		FormController previousMainShowingForm = (currentContainer != null ? currentContainer.getController() : null);
		currentContainer.setFormController(null);
		if (previousMainShowingForm != null)
		{
			showFormInMainPanel(previousMainShowingForm.getName());
		}

		if (navigatorParent != null)
		{
			FormController navigator = getFormController(navigatorName, navigatorParent);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			navigator.notifyVisible(true, invokeLaterRunnables);
			Utils.invokeLater(getApplication(), invokeLaterRunnables);
			navigatorParent.setNavigator(navigator);
		}

		if (main instanceof Component)
		{
			// TODO and other containers?
			((Component)main).setVersioned(true);
		}
	}

	public void reload()
	{
		FormController[] array = createdFormControllers.values().toArray(new FormController[0]);
		createdFormControllers.clear();
		leaseHistory.clear();

		reload(array);
	}

	@Override
	public void showFormInDialog(String formName, Rectangle bounds, String title, boolean resizeble, boolean showTextToolbar, boolean closeAll, boolean modal,
		String windowName)
	{
		// with old implementation, non-modal dialogs in WC were actually separate browser windows; keep it compatible with new implementation
		if (modal)
		{
			super.showFormInDialog(formName, bounds, title, resizeble, showTextToolbar, closeAll, modal, windowName);
		}
		else
		{
			super.showFormInFrame(formName, bounds, title, resizeble, showTextToolbar, windowName);
		}
	}

	public void showDelayedFormInDialog(int type, String formName, Rectangle bounds, String title, boolean resizeble, boolean showTextToolbar,
		boolean closeAll, boolean modal, String windowName)
	{
		// delayed dialog needs to ignore the showFormInDialog() old behavior override above...
		if (type == JSWindow.WINDOW)
		{
			super.showFormInFrame(formName, bounds, title, resizeble, showTextToolbar, windowName);
		}
		else
		{
			super.showFormInDialog(formName, bounds, title, resizeble, showTextToolbar, closeAll, modal, windowName);
		}
	}

	/**
	 * @see com.servoy.j2db.FormManager#getOrCreateMainContainer(java.lang.String)
	 */
	@Override
	public IMainContainer getOrCreateMainContainer(String nm)
	{
		String name = nm;
		if (name != null && name.trim().length() == 0)
		{
			// blank names would make PageMap throw an exception or generate JS errors
			name = "blank_name"; //$NON-NLS-1$
		}
		IMainContainer container = getMainContainer(name);
		if (container == null)
		{
			WebClient wc = (WebClient)getApplication();
			IPageMap pageMap = PageMap.forName(name);
			container = new MainPage(wc, pageMap);
			containers.put(name, container);
		}
		return container;
	}

	@Override
	protected void destroyContainer(IMainContainer container)
	{
		if (container instanceof MainPage)
		{
			((MainPage)container).close();
		}
		super.destroyContainer(container);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.FormManager#destroySolutionSettings()
	 */
	@Override
	protected void destroySolutionSettings()
	{
		super.destroySolutionSettings();
		Session.get().setMetaData(Session.PAGEMAP_ACCESS_MDK, null); // reset all pagemap accesses. 
	}

}
