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
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.RootPaneContainer;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.FormDialog;
import com.servoy.j2db.FormFrame;
import com.servoy.j2db.FormManager;
import com.servoy.j2db.FormWindow;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IFormUIInternal;
import com.servoy.j2db.IMainContainer;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.ISwingFormManager;
import com.servoy.j2db.LAFManager;
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
import com.servoy.j2db.util.UIUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.JMenuAlwaysEnabled;

public class SwingFormManager extends FormManager implements ISwingFormManager, ActionListener
{
	protected Map formUser; // FormUIController -> Container    

	// Map thats stores containers that er in printpreview: IMainContainer -> PrintPreviewHolder
	protected Map printPreviews;

	protected WindowMenuDialog windowMenuDialog;
	private JMenu scriptMenu;

	public SwingFormManager(IApplication app, IMainContainer mainContainer)
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
			final PrintPreview printPreview = new PrintPreview(getApplication(), afp, foundset, zoomFactor, printJob);
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

	/**
	 * @see com.servoy.j2db.FormManager#closeFormInDialogOrWindow(boolean, java.lang.String)
	 */
	@Override
	public boolean closeFormInDialog(IMainContainer container)
	{
		removePreview(container);
		boolean ok = true;
		FormController fp = container.getController();
		if (fp != null)
		{
			List invokeLaterRunnables = new ArrayList();
			ok = fp.notifyVisible(false, invokeLaterRunnables);
			Utils.invokeLater(getApplication(), invokeLaterRunnables);
		}
		if (ok)
		{
			Container parent = ((Component)container).getParent();
			while (parent != null && !(parent instanceof FormWindow))
			{
				parent = parent.getParent();
			}
			if (parent instanceof FormWindow)
			{
				((FormWindow)parent).closeWindow();
			}
		}
		return ok;
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

	protected FormDialog createFormDialog(IApplication app, Window owner, boolean modal, String dialogName)
	{
		if (owner == null || (!(owner instanceof JDialog || owner instanceof JFrame))) owner = app.getMainApplicationFrame();

		if (owner instanceof JDialog)
		{
			return new FormDialog(app, (JDialog)owner, modal, dialogName);
		}
		else
		{
			return new FormDialog(app, (JFrame)owner, modal, dialogName);
		}
	}

	protected FormFrame createFormFrame(IApplication app, String windowName)
	{
		FormFrame frame = new FormFrame(app, windowName);
		frame.setIconImage(getApplication().getMainApplicationFrame().getIconImage());
		return frame;
	}

	private void checkTextToolbar(boolean showTextToolbar, RootPaneContainer window)
	{
		if (showTextToolbar)
		{
			Component textToolbar = findTextToolbar(window.getContentPane().getComponents());
			if (textToolbar == null)
			{
				textToolbar = new TextToolbar(getApplication());
			}
			window.getContentPane().add(textToolbar, BorderLayout.NORTH);
		}
		else
		{
			Component textToolbar = findTextToolbar(window.getContentPane().getComponents());
			if (textToolbar != null) window.getContentPane().remove(textToolbar);
		}
	}

	private Component findTextToolbar(Component[] components)
	{
		for (Component c : components)
		{
			if (c instanceof TextToolbar) return c;
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.FormManager#showFormInDialog(com.servoy.j2db.persistence.Form, java.awt.Rectangle, java.lang.String, boolean, boolean, boolean,
	 *      java.lang.String)
	 */
	@Override
	public void showFormInDialog(String formName, Rectangle bounds, String title, boolean resizeble, boolean showTextToolbar, boolean closeAll, boolean modal,
		String windowName)
	{
		boolean legacyV3Behavior = false; // first window is modal, second reuses same dialog
		if (windowName == null)
		{
			windowName = DEFAULT_DIALOG_NAME;
			legacyV3Behavior = true;
		}
		IMainContainer currentMainContainer = getModalDialogContainer();
		Window currentWindow = getApplication().getWindow(USER_WINDOW_PREFIX + currentMainContainer.getContainerName());

		IMainContainer container = getOrCreateMainContainer(windowName);
		IMainContainer previousMainContainer = null;

		Window w = getApplication().getWindow(USER_WINDOW_PREFIX + windowName);
		FormDialog sfd = null;
		if (w instanceof FormDialog)
		{
			sfd = (FormDialog)w;
		}
		else
		{
			closeFormInDialogOrWindow(windowName, true); // make sure it's closed before reference to it is lost
		}

		// make sure the dialog has the correct owner
		if (sfd != null)
		{
			Window formDialogOwner = sfd.getOwner();
			Window owner = null;
			if (currentWindow == null)
			{
				owner = getApplication().getMainApplicationFrame();
			}
			else
			{
				owner = currentWindow;
			}
			if ((owner != sfd) && !owner.equals(formDialogOwner))
			{
				sfd = null;
			}
		}

		boolean windowModal = getDialogModalState() && ((legacyV3Behavior && sfd == null) || modal);
		if (windowModal)
		{
			previousMainContainer = setModalDialogContainer(container);
		}

		boolean bringToFrontNeeded = false;
		if (sfd == null)
		{
			sfd = createFormDialog(getApplication(), currentWindow, windowModal, windowName);
			getApplication().registerWindow(USER_WINDOW_PREFIX + windowName, sfd);
			sfd.setMainContainer(container);
		}
		else if (sfd.isVisible())
		{
			bringToFrontNeeded = true;
		}

		// For none legacy the dialog must always be really closed 
		sfd.setCloseAll(closeAll || !legacyV3Behavior);

		if (sfd.isVisible())
		{
			sfd.storeBounds();
		}

		final FormController fp = showFormInMainPanel(formName, container, title, closeAll && legacyV3Behavior, windowName);
		if (fp != null && fp.getName().equals(formName))
		{
			checkTextToolbar(showTextToolbar, sfd);

			if (!restoreWindowBounds(container))
			{
				// quickly set the form to visible if not visible.
				boolean visible = fp.getFormUI().isVisible();
				if (!visible)
				{
					((Component)fp.getFormUI()).setVisible(true);
				}
				// now calculate the preferred size
				sfd.pack();
				// if not visible before restore that state (will be set right later on)
				if (!visible) ((Component)fp.getFormUI()).setVisible(false);

				if (bounds != FormManager.FULL_SCREEN)
				{
					setWindowSize(bounds, sfd, legacyV3Behavior);
				}
			}

			boolean findModeSet = false;

			Action action = getApplication().getCmdManager().getRegisteredAction("cmdperformfind"); //$NON-NLS-1$
			if (action != null && fp.getFormModel() != null)
			{
				findModeSet = fp.getFormModel().isInFindMode() && !action.isEnabled();
				if (findModeSet) action.setEnabled(true);
			}
			final FormDialog fd = sfd;
			getApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					fd.getRootPane().requestFocus();
					if (LAFManager.isUsingAppleLAF())
					{
						getApplication().invokeLater(new Runnable()
						{
							public void run()
							{
								((Component)fp.getFormUI()).repaint();
							}
						});
					}
				}
			});
			sfd.setResizable(resizeble);
			sfd.setModal(windowModal);
			if (windowModal)
			{
				// When a modal window is closed, the old modal window state will have to be restored...
				// For example, when inside JS for an event you close a modal window and open another one,
				// the new modal window must have as owner not the closed window, but the last opened modal window
				// before the window just closed.
				// This has to happen when setVisible(false) is called on the modal dialog. We cannot simply rely
				// on executing this after sfd.setVisible(true) is unblocked, because then it will be executed
				// after the new dialog is opened by java script. (because that execution continues as the next event on the EventThread)
				sfd.setPreviousMainContainer(previousMainContainer, currentMainContainer);
			}

			// blocks in case of modal dialogs
			if (bounds == FormManager.FULL_SCREEN)
			{
				sfd.setFullScreen();
			}
			else
			{
				sfd.setVisible(true);
				if (bringToFrontNeeded)
				{
					sfd.toFront();
				}
			}

			if (findModeSet && action != null) action.setEnabled(false);
		}
	}

	@Override
	public void showFormInFrame(String formName, Rectangle bounds, String windowTitle, boolean resizeble, boolean showTextToolbar, String windowName)
	{
		if (windowName == null)
		{
			windowName = DEFAULT_DIALOG_NAME;
		}

		IMainContainer container = getOrCreateMainContainer(windowName);
		Window w = getApplication().getWindow(USER_WINDOW_PREFIX + windowName);

		FormFrame frame = null;
		if (w instanceof FormFrame)
		{
			frame = (FormFrame)w;
		}
		else
		{
			closeFormInDialogOrWindow(windowName, true); // make sure it's closed before reference to it is lost
		}

		boolean bringToFrontNeeded = false;
		if (frame == null)
		{
			frame = createFormFrame(getApplication(), windowName);
			getApplication().registerWindow(USER_WINDOW_PREFIX + windowName, frame);
			frame.setMainContainer(container);
		}
		else if (frame.isVisible())
		{
			bringToFrontNeeded = true;
		}
		if (frame.isVisible())
		{
			frame.storeBounds();
		}

		final FormController fp = showFormInMainPanel(formName, container, windowTitle, true, windowName);
		if (fp != null && fp.getName().equals(formName))
		{
			checkTextToolbar(showTextToolbar, frame);

			if (!restoreWindowBounds(container))
			{
				// quickly set the form to visible if not visible.
				boolean visible = fp.getFormUI().isVisible();
				if (!visible)
				{
					((Component)fp.getFormUI()).setVisible(true);
				}
				// now calculate the preferred size
				frame.pack();
				// if not visible before restore that state (will be set right later on)
				if (!visible) ((Component)fp.getFormUI()).setVisible(false);

				if (bounds != FormManager.FULL_SCREEN)
				{
					setWindowSize(bounds, frame, false);
				}
			}

			boolean findModeSet = false;

			Action action = getApplication().getCmdManager().getRegisteredAction("cmdperformfind"); //$NON-NLS-1$
			if (action != null && fp.getFormModel() != null)
			{
				findModeSet = fp.getFormModel().isInFindMode() && !action.isEnabled();
				if (findModeSet) action.setEnabled(true);
			}
			final FormFrame ff = frame;
			getApplication().invokeLater(new Runnable()
			{
				public void run()
				{
					ff.getRootPane().requestFocus();
					if (LAFManager.isUsingAppleLAF())
					{
						getApplication().invokeLater(new Runnable()
						{
							public void run()
							{
								((Component)fp.getFormUI()).repaint();
							}
						});
					}
				}
			});
			frame.setResizable(resizeble);

			// blocks in case of modal dialogs
			if (bounds == FormManager.FULL_SCREEN)
			{
				frame.setFullScreen();
			}
			else
			{
				frame.setVisible(true);
				if (bringToFrontNeeded)
				{
					frame.toFront();
				}
			}

			if (findModeSet && action != null) action.setEnabled(false);
		}
	}

	@Override
	public void storeDialogSize(IMainContainer container)
	{
		FormWindow window = (FormWindow)getApplication().getWindow(USER_WINDOW_PREFIX + container.getContainerName());

		if (window != null)
		{
			window.storeBounds();
		}
	}

	@Override
	public boolean restoreWindowBounds(IMainContainer container)
	{
		Window window = getApplication().getWindow(USER_WINDOW_PREFIX + container.getContainerName());

		if (window != null && container.getController() != null)
		{
			String name = container.getController().getName();

			Rectangle r = ((FormWindow)window).getFormBounds(name);
			if (r != null)
			{
				setWindowSize(r, window, false);
				return true;
			}
		}
		return false;
	}

	/**
	 * @param r
	 * @param sfd
	 * @param legacyV3Behavior
	 */
	private void setWindowSize(Rectangle r, Window sfd, boolean legacyV3Behavior)
	{
		Rectangle r2 = sfd.getBounds();
		if (r != null)
		{
			if (r.height <= 0)
			{
				r.height = r2.height;
			}
			if (r.width <= 0)
			{
				r.width = r2.width;
			}
			if (!UIUtils.isOnScreen(r)) // with multiple monitors, all locations on a monitor can be negative
			{
				sfd.setSize(r.width, r.height);
				// if the current dialog is not visible, the the location
				// else let it be what it was.
				if (!sfd.isVisible() || legacyV3Behavior)
				{
					Window ow = sfd.getOwner();
					if (ow == null) ow = getApplication().getMainApplicationFrame();
					sfd.setLocationRelativeTo(ow);
				}
			}
			else
			{
				sfd.setBounds(r);
				sfd.validate();
			}
		}
	}

	@Override
	protected Iterator<String> getOrderedContainers()
	{
		// here we have to close in reverse order of the opening
		List<String> orderedDialogs = new ArrayList<String>();
		Map<FormDialog, String> dialogs = new HashMap<FormDialog, String>();
		Iterator<String> it = containers.keySet().iterator();
		while (it.hasNext())
		{
			String key = it.next();
			if (key != null)
			{
				IMainContainer mContainer = containers.get(key);
				if (getMainContainer(null) != mContainer)
				{
					Container parent = ((Component)mContainer).getParent();
					while (parent != null && !(parent instanceof FormDialog))
					{
						parent = parent.getParent();
					}
					if (parent instanceof FormDialog)
					{
						dialogs.put((FormDialog)parent, key);
						continue;
					}
				}
			}
		}
		for (FormDialog dialog : dialogs.keySet())
		{
			addDialogsInOrder(dialog, dialogs, orderedDialogs);
		}
		if (orderedDialogs.size() < containers.keySet().size())
		{
			it = containers.keySet().iterator();
			while (it.hasNext())
			{
				String key = it.next();
				if (!orderedDialogs.contains(key))
				{
					orderedDialogs.add(key);
				}
			}
		}
		return orderedDialogs.iterator();
	}

	private void addDialogsInOrder(FormDialog dialog, Map<FormDialog, String> dialogs, List<String> orderedDialogs)
	{
		if (dialog.getOwnedWindows() != null)
		{
			for (Window window : dialog.getOwnedWindows())
			{
				if (window instanceof FormDialog)
				{
					addDialogsInOrder((FormDialog)window, dialogs, orderedDialogs);
				}
			}
		}
		if (!orderedDialogs.contains(dialogs.get(dialog)))
		{
			orderedDialogs.add(dialogs.get(dialog));
		}
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
