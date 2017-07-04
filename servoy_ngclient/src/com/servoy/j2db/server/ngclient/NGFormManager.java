/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.sablo.websocket.CurrentWindow;

import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.BasicFormManager;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class NGFormManager extends BasicFormManager implements INGFormManager
{
	public static final String DEFAULT_DIALOG_NAME = "dialog"; //$NON-NLS-1$

	protected final ConcurrentMap<String, IWebFormController> createdFormControllers; // formName -> FormController

	private Form loginForm;

	private final int maxForms;

	/**
	 * @param application
	 */
	public NGFormManager(INGApplication application)
	{
		super(application);
		int max = Utils.getAsInteger(Settings.getInstance().getProperty("servoy.max.webforms.loaded", "128"), false);
		maxForms = max == 0 ? 128 : max;
		this.createdFormControllers = new ConcurrentHashMap<>();
	}

	public final INGApplication getApplication()
	{
		return (INGApplication)application;
	}

	@Override
	public IWebFormController getCachedFormController(String formName)
	{
		return createdFormControllers.get(formName);
	}

	@Override
	public List<IFormController> getCachedFormControllers()
	{
		return new ArrayList<IFormController>(createdFormControllers.values());
	}

	public List<IFormController> getCachedFormControllers(Form form)
	{
		ArrayList<IFormController> al = new ArrayList<IFormController>();
		for (IFormController controller : createdFormControllers.values())
		{
			if (controller.getForm().getName().equals(form.getName()))
			{
				al.add(controller);
			}
			else if (controller.getForm() instanceof FlattenedForm)
			{
				List<Form> formHierarchy = application.getFlattenedSolution().getFormHierarchy(controller.getForm());
				if (formHierarchy.contains(form))
				{
					al.add(controller);
				}
			}
		}
		return al;
	}


	public void makeSolutionSettings(Solution s)
	{
		Solution solution = s;
		Iterator<Form> e = application.getFlattenedSolution().getForms(true);
		// add all forms first, they may be referred to in the login form
		Form first = application.getFlattenedSolution().getForm(solution.getFirstFormID());
		boolean firstFormCanBeInstantiated = application.getFlattenedSolution().formCanBeInstantiated(first);
		while (e.hasNext())
		{
			Form form = e.next();
			if (application.getFlattenedSolution().formCanBeInstantiated(form))
			{
				if (!firstFormCanBeInstantiated) first = form;
				firstFormCanBeInstantiated = true;
			}
			// add anyway, the form may be used in scripting
			addForm(form, form.equals(first));
		}

		if (firstFormCanBeInstantiated)
		{
			application.getModeManager().setMode(IModeManager.EDIT_MODE);//start in browse mode
		}


		if (solution.getLoginFormID() > 0 && solution.getMustAuthenticate() && application.getUserUID() == null)
		{
			Form login = application.getFlattenedSolution().getForm(solution.getLoginFormID());
			if (application.getFlattenedSolution().formCanBeInstantiated(login) && loginForm == null)
			{
				loginForm = login;//must set the login form early so its even correct if onload of login form is called
				showFormInMainPanel(login.getName());
				return; //stop and recall this method from security.login(...)!
			}
		}

		IBasicMainContainer currentContainer = getCurrentContainer();
		if (solution.getLoginFormID() > 0 && solution.getMustAuthenticate() && application.getUserUID() != null && loginForm != null)
		{
			if (currentContainer.getController() != null && loginForm.getName().equals(currentContainer.getController().getForm().getName()))
			{
				hideFormIfVisible(currentContainer.getController());
				currentContainer.setController(null);
			}
			loginForm = null;//clear and continue
		}

		ScriptMethod sm = application.getFlattenedSolution().getScriptMethod(solution.getOnOpenMethodID());

		Object[] solutionOpenMethodArgs = null;
		String preferedSolutionMethodName = ((ClientState)application).getPreferedSolutionMethodNameToCall();
		if (preferedSolutionMethodName == null && ((ClientState)application).getPreferedSolutionMethodArguments() != null)
		{
			solutionOpenMethodArgs = ((ClientState)application).getPreferedSolutionMethodArguments();
		}

		if (sm != null)
		{
			try
			{
				application.getScriptEngine().getScopesScope().executeGlobalFunction(sm.getScopeName(), sm.getName(),
					Utils.arrayMerge(solutionOpenMethodArgs, Utils.parseJSExpressions(solution.getFlattenedMethodArguments("onOpenMethodID"))), false, false);
				if (application.getSolution() == null) return;
			}
			catch (Exception e1)
			{
				application.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { sm.getName() }), e1); //$NON-NLS-1$
			}
		}

		if (first != null && getCurrentForm() == null)
		{
			showFormInMainPanel(first.getName()); //we only set if the solution startup did not yet show a form already
		}

		if (preferedSolutionMethodName != null &&
			(application.getFlattenedSolution().isMainSolutionLoaded() || solution.getSolutionType() == SolutionMetaData.LOGIN_SOLUTION))
		{
			try
			{
				Object[] args = ((ClientState)application).getPreferedSolutionMethodArguments();

				Pair<String, String> scope = ScopesUtils.getVariableScope(preferedSolutionMethodName);
				GlobalScope gs = application.getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft());
				if (gs != null && gs.get(scope.getRight()) instanceof Function)//make sure the function is found before resetting preferedSolutionMethodName
				{
					// avoid stack overflows when an execute method URL is used to open the solution, and that method does call JSSecurity login
					((ClientState)application).resetPreferedSolutionMethodNameToCall();

					Object result = application.getScriptEngine().getScopesScope().executeGlobalFunction(scope.getLeft(), scope.getRight(), args, false, false);
					if (application.getSolution().getSolutionType() == SolutionMetaData.AUTHENTICATOR)
					{
						application.getRuntimeProperties().put(IServiceProvider.RT_OPEN_METHOD_RESULT, result);
					}
				}
				else if (application.getFlattenedSolution().isMainSolutionLoaded())
				{
					Debug.error("Preferred method '" + preferedSolutionMethodName + "' not found in " + application.getFlattenedSolution() + ".");
					((ClientState)application).resetPreferedSolutionMethodNameToCall();
				}
			}
			catch (Exception e1)
			{
				application.reportError(Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { preferedSolutionMethodName }), //$NON-NLS-1$
					e1);
			}
		}
	}

	@Override
	public IWebFormController getForm(String name)
	{
		IWebFormController fc = createdFormControllers.get(name);
		if (fc == null)
		{
			fc = leaseFormPanel(name);
		}
		return fc;
	}

	public void removeFormController(BasicFormController fp)
	{
		removeFromLeaseHistory(fp);
		createdFormControllers.remove(fp.getName());
	}

	@Override
	public IWebFormController leaseFormPanel(String formName)
	{
		if (formName == null) return null;

		String name = formName;

		IWebFormController fp = createdFormControllers.get(name);
		if (fp == null)
		{
			Form f = possibleForms.get(formName);
			if (f == null) return null;
			try
			{
				application.blockGUI(application.getI18NMessage("servoy.formManager.loadingForm") + formName); //$NON-NLS-1$

				f = application.getFlattenedSolution().getFlattenedForm(f, false);

				fp = new WebFormController((INGApplication)application, f, name);
				createdFormControllers.put(fp.getName(), fp);
				fp.init();
				updateLeaseHistory(fp);
				fp.setView(fp.getView());
				fp.executeOnLoadMethod();
			}
			finally
			{
				application.releaseGUI();
			}
		}
		else
		{
			addAsLastInLeaseHistory(fp);
		}
		return fp;
	}

	public void setCurrentControllerJS(IWebFormController currentController)
	{
		if (currentController != null)
		{
			SolutionScope ss = application.getScriptEngine().getSolutionScope();
			Context.enter();
			try
			{
				ss.put("currentcontroller", ss, //$NON-NLS-1$
					new NativeJavaObject(ss, currentController.initForJSUsage(),
						new InstanceJavaMembers(ss, com.servoy.j2db.BasicFormController.JSForm.class)));
			}
			finally
			{
				Context.exit();
			}
		}
	}

	@Override
	public void clearLoginForm()
	{
		if (application.getSolution().getMustAuthenticate()) makeSolutionSettings(application.getSolution());
	}

	@Override
	public IWebFormController getCurrentForm()
	{
		return getCurrentMainShowingFormController();
	}


	@Override
	public IWebFormController getCurrentMainShowingFormController()
	{
		NGRuntimeWindow currentWindow = ((INGApplication)application).getRuntimeWindowManager().getCurrentWindow();
		if (currentWindow == null) return null;
		return currentWindow.getController();
	}

	@Override
	public void init()
	{
		// ignore

	}

	@Override
	public void flushCachedItems()
	{
		// ignore

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		String name = evt.getPropertyName();
		if ("solution".equals(name)) //$NON-NLS-1$
		{
			final Solution s = (Solution)evt.getNewValue();
			Runnable run = new Runnable()
			{
				@Override
				public void run()
				{
					boolean isReload = s != null;
					destroySolutionSettings(isReload);//must run on same thread
					if (isReload)
					{
						makeSolutionSettings(s);
					}
				}
			};

			if (CurrentWindow.exists()) run.run();
			else CurrentWindow.runForWindow(new NGClientWebsocketSessionWindows(getApplication().getWebsocketSession()), run);

		}
		else if ("mode".equals(name)) //$NON-NLS-1$
		{
			int oldmode = ((Integer)evt.getOldValue()).intValue();
			int newmode = ((Integer)evt.getNewValue()).intValue();
			IFormController fp = getCurrentMainShowingFormController();

			if (oldmode == IModeManager.FIND_MODE || newmode == IModeManager.FIND_MODE)
			{
				fp.setMode(newmode);
			}
		}
	}

	protected void destroySolutionSettings(boolean reload)
	{
		loginForm = null;
		for (IFormController controller : createdFormControllers.values())
		{
			controller.destroy();
			hideFormIfVisible(controller);
		}
		clearLeaseHistory();

		possibleForms.clear();

		// cleanup windows (containers)
		NGRuntimeWindowManager wm = ((INGApplication)application).getRuntimeWindowManager();
		wm.destroy(reload);
	}

	@Override
	public IFormController showFormInMainPanel(String name)
	{
		return showFormInCurrentContainer(name);
	}

	@Override
	public IFormController showFormInContainer(String formName, IBasicMainContainer container, String title, boolean closeAll, String dialogName)
	{
		if (loginForm != null && loginForm.getName() != formName)
		{
			return null;//not allowed to leave here...or show anything else than login form
		}
		if (formName == null) throw new IllegalArgumentException(application.getI18NMessage("servoy.formManager.error.SettingVoidForm")); //$NON-NLS-1$

		IFormController currentMainShowingForm = container.getController();

		boolean sameForm = (currentMainShowingForm != null && formName.equals(currentMainShowingForm.getName()));
		if (sameForm && currentMainShowingForm.isFormVisible()) return leaseFormPanel(currentMainShowingForm.getName());

		final Form f = possibleForms.get(formName);
		if (f == null)
		{
			return null;
		}

		try
		{
			int access = application.getFlattenedSolution().getSecurityAccess(f.getUUID());
			if (access != -1)
			{
				boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
				if (!b_visible)
				{
					application.reportWarningInStatus(application.getI18NMessage("servoy.formManager.warningAccessForm")); //$NON-NLS-1$
					return null;
				}
			}

			//handle old panel
			if (currentMainShowingForm != null)
			{
				//leave forms in browse mode // TODO can this be set if notifyVisible returns false (current form is being kept)
				if (application.getModeManager().getMode() != IModeManager.EDIT_MODE)
				{
					application.getModeManager().setMode(IModeManager.EDIT_MODE);
				}
				IWebFormController fp = leaseFormPanel(currentMainShowingForm.getName());
				if (fp != null && !sameForm)
				{
					List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
					boolean ok = fp.notifyVisible(false, invokeLaterRunnables);
					Utils.invokeLater(application, invokeLaterRunnables);

					// solution closed in onhide method of previous form?
					if (application.getSolution() == null) return null;

					if (!ok)
					{
						return fp;
					}
				}
			}

			//set main
			IFormController tmpForm = currentMainShowingForm;

			final IWebFormController fp = leaseFormPanel(formName);
			currentMainShowingForm = fp;

			if (fp != null)
			{
				setCurrentControllerJS(fp);
				//add to history
				getHistory(container).add(fp.getName());


				// test if solution is closed in the onload method.
				if (application.getSolution() == null) return null;

				container.setController(fp);

				//show panel as main
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				fp.notifyVisible(true, invokeLaterRunnables);

				String titleText = title;
				if (titleText == null) titleText = f.getTitleText();
				if (titleText == null || titleText.equals("")) titleText = fp.getName(); //$NON-NLS-1$
				if (NO_TITLE_TEXT.equals(titleText)) titleText = ""; //$NON-NLS-1$
				container.setTitle(titleText);

				fp.getFormUI().setParentWindowName(container.getContainerName());

				Utils.invokeLater(application, invokeLaterRunnables);
			}
			else
			{
				container.setController(null);
			}
			J2DBGlobals.firePropertyChange(this, "form", tmpForm, currentMainShowingForm); //$NON-NLS-1$

			return fp;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public IFormController showFormInCurrentContainer(String formName)
	{
		return showFormInContainer(formName, getCurrentContainer(), null, false, getCurrentContainer().getContainerName());
	}

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

		RuntimeWindow thisWindow = application.getRuntimeWindowManager().getWindow(windowName);
		if (thisWindow != null && thisWindow.getType() != JSWindow.DIALOG && thisWindow.getType() != JSWindow.MODAL_DIALOG)
		{
			thisWindow.hide(true); // make sure it is closed before reference to it is lost
			thisWindow.destroy();
			thisWindow = null;
		}

		if (thisWindow == null)
		{
			thisWindow = application.getRuntimeWindowManager().createWindow(windowName, modal ? JSWindow.MODAL_DIALOG : JSWindow.DIALOG, null);
		}
		thisWindow.setInitialBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		thisWindow.showTextToolbar(showTextToolbar);
		thisWindow.setTitle(title);
		thisWindow.setResizable(resizeble);

		thisWindow.oldShow(formName, closeAll, legacyV3Behavior);
	}

	@Override
	public void showFormInFrame(String formName, Rectangle bounds, String windowTitle, boolean resizeble, boolean showTextToolbar, String windowName)
	{
		if (windowName == null)
		{
			windowName = DEFAULT_DIALOG_NAME;
		}

		RuntimeWindow thisWindow = application.getRuntimeWindowManager().getWindow(windowName);

		if (thisWindow != null && thisWindow.getType() != JSWindow.WINDOW)
		{
			thisWindow.hide(true); // make sure it's closed before reference to it is lost
			thisWindow.destroy();
			thisWindow = null;
		}

		if (thisWindow == null)
		{
			thisWindow = application.getRuntimeWindowManager().createWindow(windowName, JSWindow.WINDOW, null);
		}
		thisWindow.setInitialBounds(bounds.x, bounds.y, bounds.width, bounds.height);
		thisWindow.showTextToolbar(showTextToolbar);
		thisWindow.setTitle(windowTitle);
		thisWindow.setResizable(resizeble);

		thisWindow.oldShow(formName, true, false); // last two params are really not relevant for windows
	}

	@Override
	public IBasicMainContainer getCurrentContainer()
	{
		return ((INGApplication)application).getRuntimeWindowManager().getCurrentWindow();
	}

	@Override
	public History getHistory(IBasicMainContainer container)
	{
		IBasicMainContainer c = container;
		if (c == null)
		{
			IBasicMainContainer currentContainer = getCurrentContainer();
			if (currentContainer != null)
			{
				c = currentContainer;
			}
			else
			{
				c = getMainContainer(null);
			}
		}
		return c.getHistory();
	}

	@Override
	public IBasicMainContainer getMainContainer(String windowName)
	{
		return ((INGApplication)application).getRuntimeWindowManager().getWindow(windowName);
	}

	@Override
	public boolean isCurrentTheMainContainer()
	{
		// main containers should be the once without a parent  (so tabs in browser but no dialogs)
		// so setTitle should also just set it on the current main window of the active endpoint
		return ((NGRuntimeWindow)getCurrentContainer()).getParent() == null;
	}

	@Override
	public IWebFormController getFormAndSetCurrentWindow(String formName)
	{
		IWebFormController form = getForm(formName);
		if (form == null) throw new RuntimeException("form not found for formname:" + formName);
		String windowName = form.getFormUI().getParentWindowName();
		if (windowName != null)
		{
			application.getRuntimeWindowManager().setCurrentWindowName(windowName);
			setCurrentControllerJS(getCurrentForm());
		}
		else
		{
			Debug.log("should the form " + formName + " have a window name, window very likely already closed?"); // throw new RuntimeException("window not set for form:" + formName + " (" + form + ")");
		}
		return form;
	}

	@Override
	protected int getMaxFormsLoaded()
	{
		return maxForms;
	}
}
