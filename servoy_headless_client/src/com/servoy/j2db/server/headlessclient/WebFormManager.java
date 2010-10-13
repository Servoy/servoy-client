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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.IPageMap;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageMap;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebRequestCycle;

import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;
import com.servoy.j2db.smart.SwingForm;
import com.servoy.j2db.util.Utils;

/**
 * A {@link FormManager} implementation that is used in the webclient. 
 * 
 * @author jcompagner
 */
public class WebFormManager extends FormManager
{
	public WebFormManager(IApplication app, IMainContainer mainp)
	{
		super(app, mainp);
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
	protected void fillScriptMenu()
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
				WebForm wf = (WebForm)fp.getFormUI();
				MarkupContainer wfParent = wf.getParent();

				fp.destroy();

				// in case this form has a parent, destroy the container WebForm
				//TODO: only rebuild the parent
				if (wfParent != null)
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


	/**
	 * @see com.servoy.j2db.FormManager#closeFormInDialog(java.lang.String)
	 */
	@Override
	public boolean closeFormInDialog(IMainContainer container)
	{
		boolean ok = true;
		FormController fp = container.getController();
		if (fp != null)
		{
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			ok = fp.notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(getApplication(), invokeLaterRunnables);
		}
		if (ok)
		{
			((MainPage)container).close();
		}
		return ok;
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

	/**
	 * @see com.servoy.j2db.FormManager#showFormInDialog(com.servoy.j2db.persistence.Form, java.awt.Rectangle, java.lang.String, boolean, boolean, boolean,
	 *      java.lang.String)
	 */
	@Override
	public void showFormInDialog(String formName, Rectangle bounds, String title, boolean resizeble, boolean showTextToolbar, boolean closeAll, boolean modal,
		String dialogName)
	{
		IMainContainer currContainer = getCurrentContainer();
		if (((WebRequestCycle)RequestCycle.get()).getWebRequest().isAjax() && !((MainPage)currContainer).isShowPageInDialogDelayed() &&
			!((MainPage)currContainer).isPopupClosing())
		{
			boolean legacyV3Behavior = (dialogName == null); // first window is modal, second reuses same dialog

			IMainContainer dialogContainer = getOrCreateMainContainer(dialogName == null ? DEFAULT_DIALOG_NAME : dialogName);
			// In case this modal dialog wants to show another modal dialog during onStart event, we make sure it
			// will be showed in postponed mode. Otherwise a stack of nested modal dialogs will not display OK.
			((MainPage)dialogContainer).setShowPageInDialogDelayed(true);

			if (formName != null)
			{
				final FormController fp = showFormInMainPanel(formName, dialogContainer, title, closeAll || !legacyV3Behavior, dialogName);
				if (fp != null && fp.getName().equals(formName) && dialogContainer != currContainer)
				{
					Rectangle r2;
					if (bounds == FormManager.FULL_SCREEN)
					{
						r2 = bounds;
					}
					else
					{
						r2 = getSizeAndLocation(bounds, dialogContainer, fp);
						if (Application.get().getDebugSettings().isAjaxDebugModeEnabled())
						{
							r2.height += 40;
						}
					}
					((MainPage)currContainer).showPopupPage((MainPage)dialogContainer, title, r2, resizeble, closeAll || !legacyV3Behavior, modal);
				}
			}
		}
		else
		{
			((MainPage)currContainer).getPageContributor().showFormInDialogDelayed(formName, bounds, title, resizeble, showTextToolbar, closeAll, modal,
				dialogName);
			// Now we need to disable the delayed show, otherwise the modal child would be continuously postponed.
			((MainPage)currContainer).setShowPageInDialogDelayed(false);
		}
	}

	@Override
	public void showFormInFrame(String formName, Rectangle bounds, String windowTitle, boolean resizeble, boolean showTextToolbar, String windowName)
	{
		showFormInDialog(formName, bounds, windowTitle, resizeble, showTextToolbar, true, false, windowName);
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

	/**
	 * @param r
	 * @param container
	 * @param fp
	 * @return
	 */
	private Rectangle getSizeAndLocation(Rectangle r, IMainContainer container, final FormController fp)
	{
		int navid = fp.getForm().getNavigatorID();
		Dimension size = fp.getForm().getSize();
		if (navid == Form.NAVIGATOR_DEFAULT && fp.getForm().getView() != FormController.TABLE_VIEW &&
			fp.getForm().getView() != FormController.LOCKED_TABLE_VIEW)
		{
			size.width += WebDefaultRecordNavigator.DEFAULT_WIDTH;
			if (size.height < WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB) size.height = WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB;
		}
		else if (navid != Form.NAVIGATOR_NONE)
		{
			FormController currentNavFC = container.getNavigator();
			if (currentNavFC != null)
			{
				size.width += currentNavFC.getForm().getSize().width;
				int navHeight = currentNavFC.getForm().getSize().height;
				if (size.height < navHeight) size.height = navHeight;
			}
		}

		// Why 22 here? From Wicket CSS:
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-left" property
		// "div.wicket-modal div.w_right_1" brings 10px through "margin-right" property
		// "div.wicket-modal div.w_right_1" brings 2px through "border" property (1px from left and 1px from right)
		size.width += 22;

		Rectangle r2 = new Rectangle(size);
		if (r != null)
		{
			if (r.height > 0)
			{
				r2.height = r.height;
			}
			if (r.width > 0)
			{
				r2.width = r.width;
			}
			r2.x = r.x;
			r2.y = r.y;
		}
		return r2;
	}

	@Override
	public boolean restoreWindowBounds(IMainContainer container)
	{
		return false;
	}

	@Override
	public void storeDialogSize(IMainContainer container)
	{
	}

}
