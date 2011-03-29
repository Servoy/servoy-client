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

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.ISwingFormManager;
import com.servoy.j2db.Messages;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.printing.PrintPreview;
import com.servoy.j2db.smart.cmd.MenuMethodsAction;
import com.servoy.j2db.smart.scripting.ScriptMenuItem;
import com.servoy.j2db.util.SwingHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.JMenuAlwaysEnabled;

public class SwingFormManager extends FormManager implements ISwingFormManager, ActionListener
{
	protected Map formUser; // FormUIController -> Container    

	// Map thats stores containers that er in printpreview: IMainContainer -> PrintPreviewHolder
	protected Map printPreviews;

	protected WindowMenuDialog windowMenuDialog;
	private JMenu scriptMenu;

	public SwingFormManager(ISmartClientApplication app, IMainContainer mainContainer)
	{
		super(app, mainContainer);
		windowMenuDialog = new WindowMenuDialog(app, this);
		formUser = new HashMap();
		printPreviews = new HashMap();
	}

	public JComponent getFormPanel(String name, Container parent)
	{
		return (JComponent)getFormController(name, parent).getFormUI();
	}

	public void actionPerformed(ActionEvent e)
	{
		final String name = e.getActionCommand();
		getApplication().invokeLater(new Runnable()
		{
			public void run()
			{
				if (getApplication().getSolution() != null)
				{
					try
					{
						getApplication().blockGUI(Messages.getString("servoy.formManager.showingForm")); //$NON-NLS-1$
						SwingHelper.dispatchEvents(100);

//								//save any outstanding change
//								IForm dm = getCurrentForm();
//								if (dm != null) dm.saveData();//make sure all data is saved on close();

//						Form f = (Form)windowMenuDialog.getFormController(name);
						switch (getApplication().getModeManager().getMode())
						{
							case IModeManager.PREVIEW_MODE :
//		  this does not work.
//									removePreview();
//									showPreview(f);
//									break;
//			
							case IModeManager.FIND_MODE :
							case IModeManager.EDIT_MODE :
								((FormManager)getApplication().getFormManager()).showFormInMainPanel(name);
						}
					}
					finally
					{
						getApplication().releaseGUI();
					}
				}
			}
		});
	}

	public JMenu getScriptMenu()
	{
		if (scriptMenu == null)
		{
			scriptMenu = new JMenuAlwaysEnabled(new MenuMethodsAction(getApplication()));
		}
		return scriptMenu;
	}

	//return the window menu
	public JMenu getWindowMenu()
	{
		return windowMenuDialog.getWindowMenu();
	}

	@Override
	public void addForm(Form form, boolean selected)
	{
		super.addForm(form, selected);
		if (getApplication().getFlattenedSolution().formCanBeInstantiated(form))
		{
			JRadioButtonMenuItem mi = windowMenuDialog.addForm(form);
			if (mi != null) mi.setSelected(selected);
		}
	}

	/**
	 * @see com.servoy.j2db.FormManager#removeForm(com.servoy.j2db.persistence.Form)
	 */
	@Override
	public boolean removeForm(Form form)
	{
		if (super.removeForm(form))
		{
			windowMenuDialog.removeForm(form);
			return true;
		}
		return false;
	}

	@Override
	protected void selectFormMenuItem(Form form)
	{
		if (currentContainer == getMainContainer(null))
		{
			windowMenuDialog.selectForm(currentContainer.getController().getForm());
		}
	}

	/**
	 * @see com.servoy.j2db.FormManager#makeSolutionSettings(com.servoy.j2db.persistence.Solution)
	 */
	@Override
	protected void makeSolutionSettings(Solution s)
	{
		// have to do this when security.login() is used.
		windowMenuDialog.destroy();
		super.makeSolutionSettings(s);
		if (getCurrentForm() instanceof FormController)
		{
			windowMenuDialog.selectForm(((FormController)getCurrentForm()).getForm());
		}
	}


	protected boolean getShowFormsAllInWindowMenu()
	{
		return false;
	}

	@Override
	public void removeAllFormPanels()
	{
		super.removeAllFormPanels();
		if (scriptMenu != null)
		{
			scriptMenu.removeAll();
		}
	}

	@Override
	protected void handleModeChange(int oldmode, int newmode)
	{
		if (scriptMenu != null)
		{
			scriptMenu.setEnabled(newmode == IModeManager.EDIT_MODE);
		}
		super.handleModeChange(oldmode, newmode);
	}

	/**
	 * @param f
	 */
	public void synchViewMenu(int view)
	{
		if (views != null)
		{
			boolean enableViewButtons = false;
			if (view < views.length)
			{
				enableViewButtons = true;
				JRadioButtonMenuItem mi2 = views[view];
				if (mi2 != null && !mi2.isSelected())
				{
					mi2.setSelected(true);
				}
			}
			for (JRadioButtonMenuItem element : views)
			{
				if (element != null) element.setEnabled(enableViewButtons);
			}
		}
	}

	/**
	 * @param f
	 */
	@Override
	public void enableCmds(boolean enable)
	{
		Form currentMainShowingForm = null;
		if (currentContainer != null)
		{
			// only enable command on the current container. They must be enabled else commands are disabled or not 
			// disabled based on the main container. Now they will be set to enabled based on who has the focus
			//if (currentContainer != getMainContainer(null) || 
			if (currentContainer.getController() == null) return;
			currentMainShowingForm = currentContainer.getController().getForm();
		}
		if (enable && currentMainShowingForm == null) return;
		boolean findMode = false; // see also CmdManager.ableFormRelatedDataEditActions - the actions that are not used in there
		// but are in here should make use of findMode here - so as not to be enabled in find mode
		if (currentMainShowingForm != null) findMode = currentContainer.getController().isInFindMode();
		ICmdManager cm = getApplication().getCmdManager();
		Action a = null;
		a = cm.getRegisteredAction("cmdnewrecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnNewRecordCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdduplicaterecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnDuplicateRecordCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmddeleterecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnDeleteRecordCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmddeleteallrecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnDeleteAllRecordsCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdfindmode"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnFindCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdfindall"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnShowAllRecordsCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdomitrecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnOmitRecordCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdshowomitrecords"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnShowOmittedRecordsCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdrevertrecords"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnInvertRecordsCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdpreviewmode"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnPrintPreviewCmdMethodID() >= 0);
		a = cm.getRegisteredAction("cmdsort"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && !findMode && currentMainShowingForm.getOnSortCmdMethodID() >= 0);

		a = cm.getRegisteredAction("cmdnextrecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnNextRecordCmdMethodID() >= 0);

		a = cm.getRegisteredAction("cmdprevrecord"); //$NON-NLS-1$
		if (a != null) a.setEnabled(enable && currentMainShowingForm.getOnPreviousRecordCmdMethodID() >= 0);

	}

	// fill the scripts menu
	@Override
	public void fillScriptMenu()
	{
		JMenu menu = getScriptMenu();
		// Remove old script methods.
		menu.removeAll();

		FlattenedSolution sol = getApplication().getFlattenedSolution();
		if (sol.getSolution() == null) return;

		List globalMenus = new ArrayList();
		int menuCount = 1;
		Iterator globalMethods = sol.getScriptMethods(true);
		while (globalMethods.hasNext())
		{
			ScriptMethod sm = (ScriptMethod)globalMethods.next();
			ScriptMenuItem item = getScriptMenuItem(sm, null, menuCount);
			if (item != null)
			{
				globalMenus.add(item);
				if (menuCount > 0 && menuCount < 9)
				{
					menuCount++;
				}
				else
				{
					menuCount = -1;
				}
				// just break after 50, doesnt make sense to have more in the menu..
				if (globalMenus.size() > 50) break;
			}
		}

		JMenu globalMenu = menu;
		if (globalMenus.size() > 20)//if big create sub menu vor global methods
		{
			globalMenu = new JMenu(Messages.getString("servoy.formManager.menuGlobalMethods"));
			menu.add(globalMenu);
		}
		Iterator it = globalMenus.iterator();
		while (it.hasNext())
		{
			ScriptMenuItem item = (ScriptMenuItem)it.next();
			globalMenu.add(item);
		}
		boolean insertSeparator = menu.getMenuComponentCount() > 0;

		FormController fp = getCurrentMainShowingFormController();
		if (fp != null)
		{
			Iterator<ScriptMethod> formMethods = fp.getForm().getScriptMethods(true);
			while (formMethods.hasNext())
			{
				ScriptMethod sm = formMethods.next();
				ScriptMenuItem item = getScriptMenuItem(sm, fp, -1);
				if (item != null)
				{
					if (insertSeparator)
					{
						menu.add(new JSeparator());
						insertSeparator = false;
					}
					menu.add(item);
				}
			}
		}

		if (menu.getMenuComponentCount() == 0)
		{
			menu.setEnabled(false);
		}
		else
		{
			menu.setEnabled(true);
		}
	}

	/**
	 * Method showScriptInMenu.
	 * 
	 * @param sm
	 * @return boolean
	 */
	protected ScriptMenuItem getScriptMenuItem(ScriptMethod sm, FormController fp, int autoSortcut)
	{
		if (sm.getShowInMenu())
		{
			return new ScriptMenuItem(getApplication(), fp, sm.getName(), autoSortcut);
		}
		return null;
	}


	private JRadioButtonMenuItem[] views = null;

	public void setViews(JRadioButtonMenuItem[] viewbuttons)
	{
		views = viewbuttons;

	}

	//uninit
	@Override
	protected void destroySolutionSettings()
	{
		super.destroySolutionSettings();
		windowMenuDialog.destroy();
		if (scriptMenu != null)
		{
			getScriptMenu().removeAll();
		}
		formUser = new HashMap();

		printPreviews = new HashMap();
	}

	@Override
	protected void removeFormUser(FormController fp)
	{
		Container user = (Container)formUser.remove(fp);
		if (user != null)
		{
			user.remove((Container)fp.getFormUI());
		}
	}

	@Override
	protected boolean checkAndUpdateFormUser(FormController fp, Object parentContainer)
	{
		Container c = (Container)parentContainer;
		boolean isNewUser = false;
		Container user = (Container)formUser.get(fp);
		if (user == null)
		{
			formUser.put(fp, c);//register new user
			isNewUser = true;
		}
		else if (!c.equals(user) || ((Container)fp.getFormUI()).getParent() == null)
		{
			// register first then remove (else 2 times remove with FormLookupPanel)
			formUser.put(fp, c);//register new user
			user.remove(((Container)fp.getFormUI()));
			isNewUser = true;
		}
		return isNewUser;
	}

	@Override
	public void showPreview(final FormController afp, final IFoundSetInternal foundset, int zoomFactor, final PrinterJob printJob)
	{
		removePreview();

		final FormController fc = currentContainer.getController();
		if (fc != null)
		{
			List invokeLaterRunnables = new ArrayList();
			boolean ok = fc.notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(getApplication(), invokeLaterRunnables);
			if (!ok)
			{
				return;//cannot hide...so skip preview
			}
		}

		showFormInMainPanel(afp.getName(), currentContainer, null, true, null);
		getApplication().getModeManager().setMode(IModeManager.PREVIEW_MODE);

		boolean isNewUser = checkAndUpdateFormUser(afp, currentContainer);
		if (isNewUser)
		{
			IFormUIInternal ui = afp.getFormUI();
			currentContainer.add(ui, afp.getName());
		}

		try
		{
			final PrintPreview printPreview = new PrintPreview((ISmartClientApplication)getApplication(), afp, foundset, zoomFactor, printJob);
			Runnable r = new Runnable()
			{
				public void run()
				{
					printPreviews.put(currentContainer, new PrintPreviewHolder(printPreview, fc));
					currentContainer.add(printPreview, "@preview"); //$NON-NLS-1$
					currentContainer.show("@preview"); //$NON-NLS-1$

					//handle navigator propertly
					List invokeLaterRunnables = new ArrayList();
					afp.showNavigator(invokeLaterRunnables);
					Utils.invokeLater(getApplication(), invokeLaterRunnables);

					printPreview.showPages();
				}
			};
			getApplication().invokeLater(r);
		}
		catch (Exception ex)
		{
			getApplication().reportError(Messages.getString("servoy.formManager.error.PrintPreview"), ex); //$NON-NLS-1$
		}
	}

	@Override
	protected FormController removePreview()
	{
		return removePreview(currentContainer);
	}

	protected FormController removePreview(IMainContainer container)
	{
		PrintPreviewHolder printPreviewHolder = (PrintPreviewHolder)printPreviews.remove(container);
		if (printPreviewHolder != null)
		{
			container.remove(printPreviewHolder.printPreview);
			printPreviewHolder.printPreview.destroy();
			return printPreviewHolder.formBeforePrintPreview;
		}
		//currentMainShowingForm
		return container.getController();//just return current
	}

	@Override
	protected boolean isShowingPrintPreview()
	{
		return (printPreviews.containsKey(currentContainer));
	}

	@Override
	public IFormUIInternal getFormUI(FormController formController)
	{
		return new SwingForm(formController);
	}

	@Override
	public IMainContainer getOrCreateMainContainer(String name)
	{
		IMainContainer container = getMainContainer(name);

		if (container == null)
		{
			container = new MainPanel(getApplication(), name);
			containers.put(name, container);
		}
		return container;
	}

	private class PrintPreviewHolder
	{
		private final PrintPreview printPreview;
		private final FormController formBeforePrintPreview;

		private PrintPreviewHolder(PrintPreview printPreview, FormController formBeforePrintPreview)
		{
			this.printPreview = printPreview;
			this.formBeforePrintPreview = formBeforePrintPreview;
		}
	}

}
