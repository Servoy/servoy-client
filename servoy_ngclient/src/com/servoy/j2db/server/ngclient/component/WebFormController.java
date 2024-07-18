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

package com.servoy.j2db.server.ngclient.component;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.sablo.WebComponent;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectApiFunctionDefinition;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.CurrentWindow;
import org.sablo.websocket.IWindow;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.DesignModeCallbacks;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IFormController;
import com.servoy.j2db.IView;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.JSApplication.FormAndComponent;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.server.ngclient.ClientDesignService;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IDataAdapterList;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWindow;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.NGRuntimeWindow;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.WebListFormUI;
import com.servoy.j2db.server.ngclient.eventthread.NGClientWebsocketSessionWindows;
import com.servoy.j2db.server.ngclient.property.types.NGTabSeqPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ReadonlySabloValue;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class WebFormController extends BasicFormController implements IWebFormController
{
	private int view = -1;
	private WebFormUI formUI;
	private boolean rendering;
	private String[] tabSequence;

	public WebFormController(INGApplication application, Form form, String name)
	{
		super(application, form, name);
		initFormUI();
		application.getFlattenedSolution().registerLiveForm(form, namedInstance);
	}

	public void initFormUI()
	{
		Object parentContainer = null;
		if (formUI != null)
		{
			parentContainer = formUI.getParentContainer();
		}
		if (form.getView() == IFormConstants.VIEW_TYPE_RECORD || form.getView() == IFormConstants.VIEW_TYPE_RECORD_LOCKED)
		{
			formUI = new WebFormUI(this);
		}
		else if (!application.getFlattenedSolution().getFlattenedForm(form).hasPart(IPartConstants.BODY))
		{
			formUI = new WebFormUI(this);
			getApplication().reportJSWarning("Form '" + form.getName() + "' is shown in record view because it does not have a body part.");
		}
		else
		{
			formUI = new WebListFormUI(this);
		}

		if (parentContainer instanceof String)
		{
			formUI.setParentWindowName((String)parentContainer);
		}
		else if (parentContainer instanceof WebFormComponent)
		{
			formUI.setParentContainer((WebFormComponent)parentContainer);
		}
	}

	@Override
	public final INGApplication getApplication()
	{
		return (INGApplication)super.getApplication();
	}

	@Override
	public IWebFormUI getFormUI()
	{
		return formUI;
	}

	@Override
	public void setView(int view)
	{
		if (view == -1) this.view = form.getView();
		else this.view = view;
	}

	@Override
	public int getView()
	{
		return view;
	}

	@Override
	public IBasicFormManager getBasicFormManager()
	{
		return getApplication().getFormManager();
	}

	@Override
	protected IView getViewComponent()
	{
		return formUI;
	}

	@Override
	public void showNavigator(List<Runnable> invokeLaterRunnables)
	{
		String parentWindowName = getFormUI().getParentWindowName();
		NGRuntimeWindow window = getApplication().getRuntimeWindowManager().getWindow(parentWindowName);
		if (window != null && window.getController() == this)
		{
			IFormController currentNavigator = window.getNavigator();
			int form_id = form.getNavigatorID();
			if (form_id > 0)
			{
				if (currentNavigator == null || currentNavigator.getForm().getID() != form_id)//is already there
				{
					if (currentNavigator != null)
					{
						currentNavigator.notifyVisible(false, invokeLaterRunnables, true);
					}
					Form navigator = application.getFlattenedSolution().getForm(form_id);
					if (navigator != null)
					{
						IFormController navigatorController = getApplication().getFormManager().getForm(navigator.getName());
						navigatorController.notifyVisible(true, invokeLaterRunnables, true);
					}
				}
				else
				{
					// Try to lease it extra so it will be added to last used screens.
					Form navigator = application.getFlattenedSolution().getForm(form_id);
					if (navigator != null)
					{
						getBasicFormManager().leaseFormPanel(navigator.getName());
					}
				}
			}
			else if (form_id != Form.NAVIGATOR_IGNORE)
			{
				if (currentNavigator != null) currentNavigator.notifyVisible(false, invokeLaterRunnables, true);
			}
			window.setNavigator(form_id);
		}
	}

	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		if (isDestroyed()) return true;
		if (!getFormUI().getDataAdapterList().stopUIEditing(looseFocus)) return false;
		if (looseFocus && form.getOnRecordEditStopMethodID() != 0)
		{
			//allow beans to store there data via method
			IRecordInternal[] records = getApplication().getFoundSetManager().getEditRecordList().getUnmarkedEditedRecords(formModel, this);
			for (IRecordInternal element : records)
			{
				boolean b = executeOnRecordEditStop(element);
				if (!b) return false;
			}
		}
		return true;
	}

	public void setRendering(boolean rendering)
	{
		if (rendering == this.rendering) throw new IllegalArgumentException("rendering is already: " + this.rendering);
		this.rendering = rendering;
	}

	@Override
	public boolean isRendering()
	{
		return rendering;
	}

	@Override
	protected void refreshAllPartRenderers(IRecordInternal[] records)
	{
		if (!isFormVisible || application.isShutDown() || rendering) return;
		// don't do anything yet when there are records but the selection is invalid
		if (formModel != null && (formModel.getSize() > 0 && (formModel.getSelectedIndex() < 0 || formModel.getSelectedIndex() >= formModel.getSize()))) return;

		// let the ui know that it will be touched, so that locks can be taken if needed.
		boolean executeOnRecordSelect = false;
		IRecordInternal[] state = records;
		if (state == null)
		{
			if (formModel != null)
			{
				state = new IRecordInternal[] { formModel.getPrototypeState() };
			}
			else
			{
				state = new IRecordInternal[] { new PrototypeState(null) };
			}
		}
		if (!(records == null && formModel != null && formModel.getRawSize() > 0) && isStateChanged(state))
		{
			lastState = state;
			executeOnRecordSelect = true;
		}
		try
		{
			IDataAdapterList dataAdapterList = getFormUI().getDataAdapterList();
			for (IRecordInternal r : state)
				dataAdapterList.setRecord(r, true);


			if (executeOnRecordSelect)
			{
				// do this at the end because dataRenderer.refreshRecord(state) will update selection
				// for related tabs - and we should execute js code after they have been updated
				executeOnRecordSelect();
			}
		}
		catch (RuntimeException re)
		{
			throw new RuntimeException("Something goes wrong with setting the recod on the form: " + getName(), re); //$NON-NLS-1$
		}

	}

	private boolean destroyOnHide;

	@Override
	public void destroy()
	{
		if (isFormVisible() && application.isSolutionLoaded())
		{
			destroyOnHide = true;
		}
		else
		{
			try
			{
				unload();
				if (formUI != null)
				{
					formUI.destroy();
					formUI = null;
				}
				super.destroy();
				IWindow window = CurrentWindow.safeGet();
				if (window instanceof INGClientWindow && application.isSolutionLoaded())
				{
					((INGClientWindow)window).destroyForm(getName());
				}
				if (getBasicFormManager() != null) getBasicFormManager().removeFormController(this);
			}
			finally
			{
				application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
			}
		}
	}

	@Override
	protected boolean focusFirstField()
	{
		return focusField(null, true);

	}

	@SuppressWarnings("nls")
	@Override
	protected boolean focusField(String fieldName, boolean skipReadonly)
	{
		WebComponent component = null;
		WebObjectApiFunctionDefinition apiFunction = null;
		if (fieldName != null)
		{
			component = formUI.getComponent(fieldName);
			if (component == null)
			{
				RuntimeWebComponent[] runtimeComponents = getWebComponentElements();
				if (runtimeComponents != null)
				{
					for (RuntimeWebComponent runtimeComponent : runtimeComponents)
					{
						if (Utils.equalObjects(fieldName, runtimeComponent.getComponent().getName()))
						{
							component = runtimeComponent.getComponent();
							break;
						}
					}
				}
			}
			if (component != null)
			{
				apiFunction = component.getSpecification().getApiFunction("requestFocus");
			}
		}
		else
		{
			Collection<WebComponent> tabSequenceComponents = getTabSequenceComponents();
			if (tabSequenceComponents != null)
			{
				for (WebComponent seqComponent : tabSequenceComponents)
				{
					apiFunction = seqComponent.getSpecification().getApiFunction("requestFocus");
					if (apiFunction != null && seqComponent.isVisible())
					{
						if (skipReadonly)
						{
							Object value = seqComponent.getProperty(WebFormUI.READONLY);
							if (value instanceof ReadonlySabloValue)
							{
								value = Boolean.valueOf(((ReadonlySabloValue)value).getValue());
							}
							if (Boolean.TRUE.equals(value))
							{
								continue;
							}
						}
						component = seqComponent;
						break;
					}
				}
			}
		}

		if (apiFunction != null && component != null)
		{
			component.invokeApi(apiFunction, null);
			return true;
		}
		return false;
	}

	@Override
	public void propagateFindMode(boolean findMode)
	{
		if (!findMode)
		{
			application.getFoundSetManager().getEditRecordList().prepareForSave(true);
		}
		IDataAdapterList dal = getFormUI().getDataAdapterList();
		dal.setFindMode(findMode); // disables related data en does getText instead if getValue on fields
	}

	@Override
	public void setReadOnly(boolean b)
	{
		getApplication().getFormManager().setFormReadOnlyScriptingState(getName(), b);
		applyReadOnly(b);
	}

	private void applyReadOnly(boolean b)
	{
		if (b) stopUIEditing(true);
		formUI.setReadOnly(b);
		application.getFormManager().setFormReadOnly(getName(), b);
	}

	@Override
	public void setComponentEnabled(boolean b)
	{
		formUI.setComponentEnabled(b);
		application.getFormManager().setFormEnabled(getName(), b);
	}

	@Override
	public boolean recreateUI()
	{
		Form oldForm = form;

		// update flattened form reference cause now we probably need to use a SM modified version
		Form f = application.getFlattenedSolution().getForm(form.getName());
		form = application.getFlattenedSolution().getFlattenedForm(f, false); // don't use cached, make sure it updates the cache

		INGClientWindow allWindowsProxy = new NGClientWebsocketSessionWindows(getApplication().getWebsocketSession());
		if (allWindowsProxy.hasFormChangedSinceLastSendToClient(form, getName()))
		{
			// hide all visible children; here is an example that explains why it's needed:
			// parent form has tabpanel with child1 and child2; child2 is visible (second tab)
			// if you recreateUI on parent, child1 would turn out visible after recreateUI without a hide event on child2 if we wouldn't do the notifyVisible below;
			// but also when you would afterwards change tab to child2 it's onShow won't be called because it thinks it's still visible which is strange;
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			notifyVisibleOnChildren(false, invokeLaterRunnables);
			Utils.invokeLater(application, invokeLaterRunnables);

			tabSequence = null;
			f = application.getFlattenedSolution().getForm(form.getName());
			form = application.getFlattenedSolution().getFlattenedForm(f);
			getFormUI().init();
			getApplication().recreateForm(form, getName());
			if (isFormVisible)
			{
				invokeLaterRunnables = new ArrayList<Runnable>();
				notifyVisibleOnChildren(true, invokeLaterRunnables);
				Utils.invokeLater(application, invokeLaterRunnables);

				if (getDesignModeCallbacks() != null)
				{
					// this form is in design mode, let the client know the ui is created:
					RuntimeWebComponent[] components = getWebComponentElements();
					String[] names = new String[components.length];
					for (int i = 0; i < names.length; i++)
					{
						names[i] = components[i].getComponent().getName();
					}
					getApplication().getWebsocketSession().getClientService(ClientDesignService.NAME).executeAsyncServiceCall("recreateUI",
						new Object[] { getName(), names });
				}
			}
		}
		else
		{
			// in case it's not already loaded on client side - so we can't rely on endpoint URL differeces - but it is modified by Solution Model and recreateUI is called, it's formUI needs to reinitialize as well
			if ((oldForm != form || application.isInDeveloper()) && !allWindowsProxy.hasForm(getName()))
			{
				tabSequence = null;
				getFormUI().init();
				Debug.trace("RecreateUI on form " + getName() + " only recreated form ui on server, because that form is not present client-side...");
			}
			else Debug.trace(
				"RecreateUI on form " + getName() + " was ignored because that form was not changed or at least not since last being sent to client...");
		}
		application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
		application.getFlattenedSolution().registerLiveForm(form, namedInstance);
		return true;
	}

	@Override
	public void refreshView()
	{
	}

	private DesignModeCallbacks designMode;


	@Override
	public boolean getDesignMode()
	{
		return designMode != null;
	}

	@Override
	public DesignModeCallbacks getDesignModeCallbacks()
	{
		return designMode;
	}

	@Override
	public void setDesignMode(DesignModeCallbacks callback)
	{
		this.designMode = callback;
		if (callback != null)
		{
			application.getFlattenedSolution().setInDesign(form);
			// todo do we have to swich list/tableviews? (see SmartClient/WebClient form controller)
			RuntimeWebComponent[] components = getWebComponentElements();
			String[] names = new String[components.length];
			for (int i = 0; i < names.length; i++)
			{
				names[i] = components[i].getComponent().getName();
			}

			getApplication().getWebsocketSession().getClientService(ClientDesignService.NAME).executeAsyncServiceCall("setFormInDesign",
				new Object[] { getName(), names });
		}
		else
		{
			application.getFlattenedSolution().setInDesign(null);
			getApplication().getWebsocketSession().getClientService(ClientDesignService.NAME).executeAsyncServiceCall("removeFormDesign",
				new Object[] { getName() });
			recreateUI();

		}
	}

	@Override
	public void setTabSequence(Object[] arrayOfElements)
	{
		if (arrayOfElements == null)
		{
			return;
		}

		Object[] elements = arrayOfElements;
		if (elements.length == 1)
		{
			if (elements[0] instanceof Object[])
			{
				elements = (Object[])elements[0];
			}
			else if (elements[0] == null)
			{
				elements = null;
				return;
			}
		}
		tabSequence = new String[elements.length];
		for (int i = 0; i < elements.length; i++)
		{
			if (elements[i] instanceof RuntimeWebComponent)
			{
				WebFormComponent component = ((RuntimeWebComponent)elements[i]).getComponent();
				WebObjectSpecification spec = component.getSpecification();
				Collection<PropertyDescription> properties = spec.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
				if (properties.size() == 1)
				{
					PropertyDescription pd = properties.iterator().next();
					Integer val = Integer.valueOf(i + 1);
					if (!val.equals(component.getProperty(pd.getName()))) component.setProperty(pd.getName(), val);
				}
				tabSequence[i] = component.getName();
			}
			else
			{
				Debug.error("Could not set the tab sequence property for element " + elements[i]);
			}
		}
	}

	private Collection<WebComponent> getTabSequenceComponents()
	{
		SortedList<WebComponent> orderedComponents = new SortedList<WebComponent>(new Comparator<WebComponent>()
		{

			@Override
			public int compare(WebComponent o1, WebComponent o2)
			{
				PropertyDescription pd1 = o1.getSpecification().getProperties(NGTabSeqPropertyType.NG_INSTANCE).iterator().next();
				Integer val1 = (Integer)o1.getProperty(pd1.getName());
				PropertyDescription pd2 = o2.getSpecification().getProperties(NGTabSeqPropertyType.NG_INSTANCE).iterator().next();
				Integer val2 = (Integer)o2.getProperty(pd2.getName());
				if (val1 == 0 && val2 == 0)
				{
					Point location1 = (Point)o1.getProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
					Point location2 = (Point)o2.getProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
					return PositionComparator.comparePoint(true, location1, location2);
				}
				return val1 - val2;
			}

		});

		for (WebComponent component : formUI.getAllComponents())
		{
			Collection<PropertyDescription> tabSeqProperties = component.getSpecification().getProperties(NGTabSeqPropertyType.NG_INSTANCE);
			if (tabSeqProperties.size() == 1)
			{
				Integer val1 = (Integer)component.getProperty(tabSeqProperties.iterator().next().getName());
				if (val1 >= 0)
				{
					orderedComponents.add(component);
				}
			}
		}
		return orderedComponents;
	}

	@Override
	public String[] getTabSequence()
	{
		if (tabSequence == null)
		{
			Map<Integer, String> map = new TreeMap<Integer, String>();
			boolean defaultTabSequence = true;
			for (WebComponent component : formUI.getScriptableComponents())
			{
				WebObjectSpecification spec = component.getSpecification();
				Collection<PropertyDescription> properties = spec.getProperties(NGTabSeqPropertyType.NG_INSTANCE);
				if (properties.size() == 1)
				{
					PropertyDescription pd = properties.iterator().next();
					Integer value = (Integer)component.getProperty(pd.getName());
					defaultTabSequence = defaultTabSequence && value.intValue() == 0;
					if (!component.getName().startsWith(FormElement.SVY_NAME_PREFIX) && value.intValue() > 0)
					{
						map.put(value, component.getName());
					}
				}
			}
			if (defaultTabSequence)
			{
				ArrayList<String> sequence = new ArrayList<String>();
				Iterator<IFormElement> it = form.getFormElementsSortedByFormIndex();
				while (it.hasNext())
				{
					IFormElement element = it.next();
					if (element.getName() != null) sequence.add(element.getName());
				}
				tabSequence = sequence.toArray(new String[sequence.size()]);
			}
			else
			{
				tabSequence = map.values().toArray(new String[map.size()]);
			}

		}
		return tabSequence;
	}

	@Override
	public int getPartYOffset(int partType)
	{
		int totalHeight = 0;
		for (Part part : Utils.iterate(getForm().getParts()))
		{
			if (part.getPartType() == partType)
			{
				break;
			}
			totalHeight = part.getHeight();
		}
		return totalHeight;
	}

	@Override
	protected FormAndComponent getJSApplicationNames(Object source, Function function, boolean useFormAsEventSourceEventually)
	{
		Object src = source;
		if (src == null && useFormAsEventSourceEventually) src = formScope;
		return new FormAndComponent(src, getName());
	}

	@Override
	protected JSEvent getJSEvent(Object src, String eventName)
	{
		JSEvent event = new JSEvent();
		event.setType(JSEvent.EventType.form);
		if (eventName != null) event.setName(RepositoryHelper.getDisplayName(eventName, Form.class));
		event.setFormName(getName());
		event.setSource(src);
		if (src instanceof WebFormComponent) event.setElementName(((WebFormComponent)src).getFormElement().getRawName());
		else event.setElementName(src instanceof WebComponent ? ((WebComponent)src).getName() : null);
		return event;
	}

	@Override
	public String toString()
	{
		if (formModel != null)
		{
			return "FormController[form: " + getName() + ", fs size:" + Integer.toString(formModel.getSize()) + ",visible: " + isFormVisible + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				", selected record: " +
				formModel.getRecord(formModel.getSelectedIndex()) + ",destroyed:" + isDestroyed() + "]"; //$NON-NLS-1$
		}
		else
		{
			return "FormController[form: " + getName() + ",destroyed:" + isDestroyed() + "]"; //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	private WeakReference<IWebFormController> parentFormController;

	public void setParentFormController(IWebFormController parentFormController)
	{
		this.parentFormController = new WeakReference<IWebFormController>(parentFormController);
	}

	public IWebFormController getParentFormController()
	{
		if (parentFormController != null)
		{
			return parentFormController.get();
		}
		return null;
	}

	@Override
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables, boolean executePreHideSteps)
	{
		if (isFormVisible == visible) return true;

		if (!visible && destroyOnHide)
		{
			Runnable run = new Runnable()
			{
				public void run()
				{
					destroy();
				}
			};
			invokeLaterRunnables.add(run);
		}
		boolean notifyVisibleSuccess = super.notifyVisible(visible, invokeLaterRunnables, executePreHideSteps);

		if (notifyVisibleSuccess)
		{
			for (WebComponent comp : getFormUI().getComponents())
			{
				RuntimeWebComponent runtimeComponent = getFormUI().getRuntimeWebComponent(comp.getName());
				if (runtimeComponent != null)
				{
					WebObjectFunctionDefinition function = null;
					if (visible)
						function = comp.getSpecification().getInternalApiFunction("onShow");
					else
						function = comp.getSpecification().getInternalApiFunction("onHide");
					if (function != null)
					{
						runtimeComponent.executeScopeFunction(function, new Object[0]);
					}
				}
			}
		}

		if (notifyVisibleSuccess) notifyVisibleOnChildren(visible, invokeLaterRunnables); // TODO should notifyVisibleSuccess be altered here? See WebFormUI/WebFormComponent notifyVisible calls.
		return notifyVisibleSuccess;
	}

	private boolean notifyVisibleOnChildren(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		if (getFormUI() != null)
		{
			return getFormUI().notifyVisible(visible, invokeLaterRunnables);
		}
		return true;
	}


	private Map<String, Object> navigatorProperties;

	@Override
	public void setNavigatorProperties(Map<String, Object> navigatorDescription)
	{
		this.navigatorProperties = navigatorDescription;
	}

	@Override
	public Map<String, Object> getNavigatorProperties()
	{
		return navigatorProperties;
	}

	public RuntimeWebComponent getWebComponentElement(String name)
	{
		Object elementScope = formScope == null ? null : formScope.get("elements");
		if (elementScope instanceof DefaultScope)
		{
			Object element = ((Scriptable)elementScope).get(name, (Scriptable)elementScope);
			if (element instanceof Wrapper)
			{
				element = ((Wrapper)element).unwrap();
			}
			if (element instanceof RuntimeWebComponent)
			{
				return (RuntimeWebComponent)element;
			}
		}
		return null;
	}

	public RuntimeWebComponent[] getWebComponentElements()
	{
		Object elementScope = formScope == null ? null : formScope.get("elements");
		if (elementScope instanceof DefaultScope)
		{
			Object[] values = ((DefaultScope)elementScope).getValues();
			List<RuntimeWebComponent> elements = new ArrayList<RuntimeWebComponent>(values.length);
			for (Object value : values)
			{
				if (value instanceof Wrapper)
				{
					value = ((Wrapper)value).unwrap();
				}
				if (value instanceof RuntimeWebComponent)
				{
					elements.add((RuntimeWebComponent)value);
				}
			}

			return elements.toArray(new RuntimeWebComponent[elements.size()]);
		}

		return new RuntimeWebComponent[0];
	}

	@Override
	public void pushParentReadOnly(boolean b)
	{
		applyReadOnly(b);
	}

	/**
	 *
	 */
	public void forceExecuteOnLoadMethod()
	{
		this.didOnload = false;
		super.executeOnLoadMethod();

	}
}
