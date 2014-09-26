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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.BasicFormManager;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IBasicMainContainer;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IModeManager;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.JSWindow;
import com.servoy.j2db.scripting.RuntimeWindow;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.ngclient.component.WebFormController;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
public class NGFormManager extends BasicFormManager implements INGFormManager, IServerService
{
	public static final String DEFAULT_DIALOG_NAME = "dialog"; //$NON-NLS-1$

	protected final ConcurrentMap<String, IWebFormController> createdFormControllers; // formName -> FormController

	private Form loginForm;

	public NGFormManager(INGApplication application)
	{
		super(application);
		this.createdFormControllers = new ConcurrentHashMap<>();
		application.getWebsocketSession().registerServerService("formService", this);
	}

	public final INGApplication getApplication()
	{
		return (INGApplication)application;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.BasicFormManager#getCachedFormController(java.lang.String)
	 */
	@Override
	protected IFormController getCachedFormController(String formName)
	{
		return createdFormControllers.get(formName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.BasicFormManager#setFormReadOnly(java.lang.String, boolean)
	 */
	@Override
	protected void setFormReadOnly(String formName, boolean b)
	{
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
					Utils.arrayMerge(solutionOpenMethodArgs, Utils.parseJSExpressions(solution.getInstanceMethodArguments("onOpenMethodID"))), false, false);
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

		if (preferedSolutionMethodName != null && application.getFlattenedSolution().isMainSolutionLoaded())
		{
			try
			{
				Object[] args = ((ClientState)application).getPreferedSolutionMethodArguments();

				// avoid stack overflows when an execute method URL is used to open the solution, and that method does call JSSecurity login
				((ClientState)application).resetPreferedSolutionMethodNameToCall();

				Pair<String, String> scope = ScopesUtils.getVariableScope(preferedSolutionMethodName);
				Object result = application.getScriptEngine().getScopesScope().executeGlobalFunction(scope.getLeft(), scope.getRight(), args, false, false);
				if (application.getSolution().getSolutionType() == SolutionMetaData.AUTHENTICATOR)
				{
					application.getRuntimeProperties().put(IServiceProvider.RT_OPEN_METHOD_RESULT, result);
				}
			}
			catch (Exception e1)
			{
				application.reportError(
					Messages.getString("servoy.formManager.error.ExecutingOpenSolutionMethod", new Object[] { preferedSolutionMethodName }), e1); //$NON-NLS-1$
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormManager#getForm(java.lang.String)
	 */
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

				f = application.getFlattenedSolution().getFlattenedForm(f);

				fp = new WebFormController((INGApplication)application, f, name);
				createdFormControllers.put(fp.getName(), fp);
				fp.init();
				fp.setView(fp.getView());
				fp.executeOnLoadMethod();
			}
			finally
			{
				application.releaseGUI();
			}
		}
		return fp;
	}

	public void setCurrentControllerJS(IWebFormController currentController)
	{
		SolutionScope ss = application.getScriptEngine().getSolutionScope();
		Context.enter();
		try
		{
			ss.put(
				"currentcontroller", ss, new NativeJavaObject(ss, currentController.initForJSUsage(), new InstanceJavaMembers(ss, com.servoy.j2db.BasicFormController.JSForm.class))); //$NON-NLS-1$
		}
		finally
		{
			Context.exit();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormManagerInternal#clearLoginForm()
	 */
	@Override
	public void clearLoginForm()
	{
		if (application.getSolution().getMustAuthenticate()) makeSolutionSettings(application.getSolution());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormManager#getCurrentForm()
	 */
	@Override
	public IWebFormController getCurrentForm()
	{
		return getCurrentMainShowingFormController();
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IFormManagerInternal#getCurrentMainShowingFormController()
	 */
	@Override
	public IWebFormController getCurrentMainShowingFormController()
	{
		return ((INGApplication)application).getRuntimeWindowManager().getCurrentWindow().getController();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IManager#init()
	 */
	@Override
	public void init()
	{
		// ignore

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IManager#flushCachedItems()
	 */
	@Override
	public void flushCachedItems()
	{
		// ignore

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		String name = evt.getPropertyName();
		if ("solution".equals(name)) //$NON-NLS-1$
		{
			final Solution s = (Solution)evt.getNewValue();
			destroySolutionSettings();//must run on same thread
			if (s != null)
			{
				makeSolutionSettings(s);
			}
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

	protected void destroySolutionSettings()
	{
		loginForm = null;
		createdFormControllers.clear();
		possibleForms.clear();

		// cleanup windows (containers)
		NGRuntimeWindowManager wm = ((INGApplication)application).getRuntimeWindowManager();
		wm.destroy();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#showFormInMainPanel(java.lang.String)
	 */
	@Override
	public IFormController showFormInMainPanel(String name)
	{
		return showFormInCurrentContainer(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#showFormInContainer(java.lang.String, com.servoy.j2db.IBasicMainContainer, java.lang.String, boolean,
	 * java.lang.String)
	 */
	@Override
	public IFormController showFormInContainer(String formName, IBasicMainContainer container, String title, boolean closeAll, String dialogName)
	{
		if (loginForm != null && loginForm.getName() != formName)
		{
			return null;//not allowed to leave here...or show anything else than login form
		}
		if (formName == null) throw new IllegalArgumentException(application.getI18NMessage("servoy.formManager.error.SettingVoidForm")); //$NON-NLS-1$

		IFormController currentMainShowingForm = container.getController();

		if (currentMainShowingForm != null && formName.equals(currentMainShowingForm.getName())) return leaseFormPanel(currentMainShowingForm.getName());

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
				if (fp != null)
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
				getHistory(getCurrentContainer()).add(fp.getName());
				// test if solution is closed in the onload method.
				if (application.getSolution() == null) return null;

				container.setController(fp);

				//show panel as main
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				fp.notifyVisible(true, invokeLaterRunnables);
				fp.recalculateTabIndex();

				String titleText = title;
				if (titleText == null) titleText = f.getTitleText();
				if (titleText == null || titleText.equals("")) titleText = fp.getName(); //$NON-NLS-1$
				if (NO_TITLE_TEXT.equals(titleText)) titleText = ""; //$NON-NLS-1$
				container.setTitle(titleText);


//				if (isNewUser)
//				{
//					final IMainContainer showContainer = currentContainer;
//					currentContainer.showBlankPanel();//to overcome paint problem in swing...
//					invokeLaterRunnables.add(new Runnable()
//					{
//						public void run()
//						{
//							// only call show if it is still the right form.
//							FormController currentController = showContainer.getController();
//							if (currentController != null && fp.getName().equals(currentController.getName()))
//							{
//								showContainer.show(fp.getName());
//								application.getRuntimeWindowManager().setCurrentWindowName(dialogName);
//							}
//						}
//					});
//				}
//				else
//				{
//					currentContainer.show(fp.getName());
//					application.getRuntimeWindowManager().setCurrentWindowName(dialogName);
//				}
//				invokeLaterRunnables.add(title_focus);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#showFormInCurrentContainer(java.lang.String)
	 */
	@Override
	public IFormController showFormInCurrentContainer(String formName)
	{
		return showFormInContainer(formName, getCurrentContainer(), null, false, getCurrentContainer().getContainerName());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#showFormInDialog(java.lang.String, java.awt.Rectangle, java.lang.String, boolean, boolean, boolean, boolean,
	 * java.lang.String)
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#showFormInFrame(java.lang.String, java.awt.Rectangle, java.lang.String, boolean, boolean, java.lang.String)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#getCurrentContainer()
	 */
	@Override
	public IBasicMainContainer getCurrentContainer()
	{
		return ((INGApplication)application).getRuntimeWindowManager().getCurrentWindow();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#getHistory(com.servoy.j2db.IBasicMainContainer)
	 */
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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#getMainContainer(java.lang.String)
	 */
	@Override
	public IBasicMainContainer getMainContainer(String windowName)
	{
		return ((INGApplication)application).getRuntimeWindowManager().getWindow(windowName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.IBasicFormManager#isCurrentTheMainContainer()
	 */
	@Override
	public boolean isCurrentTheMainContainer()
	{
		// main containers should be the once without a parent  (so tabs in browser but no dialogs)
		// so setTitle should also just set it on the current main window of the active endpoint
		return ((NGRuntimeWindow)getCurrentContainer()).getParent() == null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.IService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	@Override
	public Object executeMethod(String methodName, final JSONObject args) throws Exception
	{
		INGClientWebsocketSession websocketSession = getApplication().getWebsocketSession();
		switch (methodName)
		{
			case "startEdit" :
			{
				IWebFormUI form = getFormAndSetCurrentWindow(args.optString("formname")).getFormUI();
				form.getDataAdapterList().startEdit(form.getWebComponent(args.optString("beanname")), args.optString("property"));
				break;
			}
			case "executeInlineScript" :
			{
				try
				{
					String formName = args.optString("formname", null);
					if (formName == null)
					{
						formName = getCurrentForm().getName();
					}
					else
					{
						formName = SecuritySupport.decrypt(Settings.getInstance(), formName);
					}
					IWebFormUI form = getFormAndSetCurrentWindow(formName).getFormUI();
					form.getDataAdapterList().executeInlineScript(args.optString("script"), args.optJSONObject("params"), args.optJSONArray("params"));
				}
				catch (Exception ex)
				{
					Debug.error("Cannot execute inline script", ex);
				}
				break;
			}
			case "requestdata" :
			{
				Pair<Map<String, Map<String, Map<String, Object>>>, PropertyDescription> requestData = requestData(args);
				Map<String, Map<String, Map<String, Object>>> formData = requestData.getLeft();
				PropertyDescription formsDataTypes = requestData.getRight();
				try
				{
					WebsocketEndpoint.get().sendMessage(formData.size() == 0 ? null : Collections.singletonMap("forms", formData), formsDataTypes, true);
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
				break;
			}
			case "initialrequestdata" :
			{
				Pair<Map<String, Map<String, Map<String, Object>>>, PropertyDescription> requestData = requestData(args);
				Map<String, Map<String, Map<String, Object>>> formData = requestData.getLeft();
				PropertyDescription formsDataTypes = requestData.getRight();
				try
				{
					Map<String, Map<String, Map<String, Map<String, Object>>>> singletonMap = new HashMap<String, Map<String, Map<String, Map<String, Object>>>>();//.singletonMap("forms", formData);
					singletonMap.put("forms", formData);
					singletonMap.put("initialdatarequest", new HashMap<String, Map<String, Map<String, Object>>>());
					WebsocketEndpoint.get().sendMessage(formData.size() == 0 ? null : singletonMap, formsDataTypes, true);
				}
				catch (IOException e)
				{
					Debug.error(e);
				}
				break;
			}
			case "formreadOnly" :
			{
				IWebFormController form = getForm(args.optString("formname"));
				if (form != null)
				{
					((WebFormController)form).setReadOnly(args.optBoolean("readOnly", false));
				}
				break;
			}
			case "formenabled" :
			{
				IWebFormController form = getForm(args.optString("formname"));
				if (form != null)
				{
					((WebFormController)form).setComponentEnabled(args.optBoolean("enabled", true));
				}
				break;
			}
			case "formvisibility" :
			{
				IWebFormController parentForm = null;
				IWebFormController controller = null;
				String formName = args.optString("formname");
				if (args.has("parentForm") && !args.isNull("parentForm"))
				{
					parentForm = getFormAndSetCurrentWindow(args.optString("parentForm"));
					controller = getForm(formName);
				}
				else
				{
					controller = getFormAndSetCurrentWindow(formName);
				}
				List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
				boolean isVisible = args.getBoolean("visible");
				boolean ok = controller.notifyVisible(isVisible, invokeLaterRunnables);
				if (ok && parentForm != null)
				{
					WebFormComponent containerComponent = parentForm.getFormUI().getWebComponent(args.getString("bean"));
					if (containerComponent != null)
					{
						containerComponent.updateVisibleForm(controller.getFormUI(), isVisible, args.optInt("formIndex"));
					}
					if (args.has("relation") && !args.isNull("relation"))
					{
						String relation = args.getString("relation");
						FoundSet parentFs = parentForm.getFormModel();
						IRecordInternal selectedRecord = (IRecordInternal)parentFs.getSelectedRecord();
						if (selectedRecord != null)
						{
							controller.loadRecords(selectedRecord.getRelatedFoundSet(relation));
						}
						else
						{
							controller.loadRecords(null); // make sure its empty
						}
						parentForm.getFormUI().getDataAdapterList().addRelatedForm(controller, relation);
					}
				}
				Utils.invokeLater(getApplication(), invokeLaterRunnables);
				Form form = application.getFormManager().getPossibleForm(formName);
				if (form != null) ((INGApplication)application).getWebsocketSession().touchForm(application.getFlattenedSolution().getFlattenedForm(form),
					formName, true);
				return Boolean.valueOf(ok);
			}

		}
		return null;
	}

	private Pair<Map<String, Map<String, Map<String, Object>>>, PropertyDescription> requestData(final JSONObject args)
	{
		final String formName = args.optString("formname");

		IWebFormUI form = getFormAndSetCurrentWindow(formName).getFormUI();
		if (form instanceof WebGridFormUI)
		{
			WebGridFormUI grid = (WebGridFormUI)form;
			if (args.has("currentPage")) grid.setCurrentPage(args.optInt("currentPage"));
			if (args.has("pageSize")) grid.setPageSize(args.optInt("pageSize"));
		}
		TypedData<Map<String, Map<String, Object>>> properties = form.getAllComponentsProperties();
		Map<String, Map<String, Map<String, Object>>> formData = new HashMap<String, Map<String, Map<String, Object>>>();

		formData.put(formName, properties.content);

		PropertyDescription formDataTypes;
		PropertyDescription formsDataTypes = null;
		if (properties.contentType != null)
		{
			formDataTypes = AggregatedPropertyType.newAggregatedProperty();
			formDataTypes.putProperty(formName, properties.contentType);
			formsDataTypes = AggregatedPropertyType.newAggregatedProperty();
			formsDataTypes.putProperty("forms", formDataTypes);
		}
		return new Pair<>(formData, formsDataTypes);
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
			Debug.error("should the form " + formName + " have a window name?"); // throw new RuntimeException("window not set for form:" + formName + " (" + form + ")");
		}
		return form;
	}
}
