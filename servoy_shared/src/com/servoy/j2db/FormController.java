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
package com.servoy.j2db;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.border.Border;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.cmd.ICmdManagerInternal;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DataAdapterList;
import com.servoy.j2db.dataprocessing.IDisplay;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.ISaveConstants;
import com.servoy.j2db.dataprocessing.PrototypeState;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportScriptProviders;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.ElementScope;
import com.servoy.j2db.scripting.IScriptSupport;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.JSApplication.FormAndComponent;
import com.servoy.j2db.scripting.JSEvent;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.ISupportRowStyling;
import com.servoy.j2db.ui.ISupportSecuritySettings;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.TabSequenceHelper;
import com.servoy.j2db.util.Utils;

/**
 * Representation of a form
 *
 * @author jblok, jcompagner
 */
public class FormController extends BasicFormController
{
	/**
	 * @author Ovidiu
	 *  this class is an implementations of ISupportSCriptProviders Which always resolves methods and variables based on the Runtime cached FlattenedForm
	 */
	public static class RuntimeSupportScriptProviders implements ISupportScriptProviders
	{
		private IApplication application = null;
		private Form form = null;

		public RuntimeSupportScriptProviders(IApplication application, Form form)
		{
			this.application = application;
			this.form = form;
		}

		public Iterator<ScriptVariable> getScriptVariables(boolean b)
		{
			return form.getScriptVariables(b);
		}

		public Iterator< ? extends IScriptProvider> getScriptMethods(boolean sort)
		{
			return form.getScriptMethods(sort);
		}

		public ScriptMethod getScriptMethod(int methodId)
		{
			return form.getScriptMethod(methodId);
		}

		/**
		 * When a JSform changed it first creates a copy before changing . It then uses that copy throughout it's life for any new modifications
		 * This method is used for updating the scriptLookup with the correct copy .
		 * For example calling getScriptVariables will return the correct variable list.
		 */
		public void updateProviderwithCopy(Form originalForm, Form copyForm)
		{
			if (this.form.getName().equals(originalForm.getName()))
			{
				if (this.form instanceof FlattenedForm)
				{
					this.form = application.getFlattenedSolution().getFlattenedForm(copyForm, true);
				}
				else
				{
					this.form = copyForm;
				}
			}
			else if (this.form instanceof FlattenedForm)
			{
				this.form = application.getFlattenedSolution().getFlattenedForm(this.form, true);
			}
		}


		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return "RSSP[" + form.getName() + "]";
		}

	}


	/*
	 * _____________________________________________________________ Declaration of attributes
	 */

	//see IFormConstants for more
	public static final int TABLE_VIEW = IFormConstants.VIEW_TYPE_TABLE;
	public static final int LOCKED_TABLE_VIEW = IFormConstants.VIEW_TYPE_TABLE_LOCKED;
	public static final int LOCKED_LIST_VIEW = IFormConstants.VIEW_TYPE_LIST_LOCKED;

	public static final int FORM_EDITOR = Part.BODY;
	public static final int FORM_RENDERER = 0;

	private int currentViewType = -1;
	private IView view; //shows data (trough renderer(s))

	private final IDataRenderer[] dataRenderers = new IDataRenderer[Part.PART_ARRAY_SIZE]; //0 position == body_renderer

	private Color bgColor = null;

	/**
	 * Some JavaScript related instances
	 */
	private FormManager fm;
	private final IFormUIInternal containerImpl;

	/**
	 * Holds for each data renderer a map with <ISupportTabSeq, Component>. It is recreated each time createDataRenderers() is called. It is needed for setting
	 * the tab sequence in setView().
	 */
	private final TabSequenceHelper< ? > tabSequence;
	private DesignModeCallbacks designMode;

	public FormController(IApplication app, Form form, String namedInstance)
	{
		super(app, form, namedInstance);
		initStyles();
		fm = (FormManager)application.getFormManager();
		scriptExecuter = new ScriptExecuter(this);
		containerImpl = fm.getFormUI(this);
		tabSequence = new TabSequenceHelper(containerImpl, application.getDataRenderFactory());
		app.getFlattenedSolution().registerLiveForm(form, namedInstance);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.BasicFormController#getBasicFormManager()
	 */
	@Override
	public IBasicFormManager getBasicFormManager()
	{
		return fm;
	}

	/**
	 * @return
	 */
	@Override
	public boolean getDesignMode()
	{
		return designMode != null;
	}

	private int previousType = -1;

	/**
	 * @param mode
	 * @param controllerForm
	 */
	@Override
	public void setDesignMode(DesignModeCallbacks callback)
	{
		this.designMode = callback;
		if (callback != null)
		{
			application.getFlattenedSolution().setInDesign(form);
			if (currentViewType == LOCKED_TABLE_VIEW || currentViewType == LIST_VIEW || currentViewType == LOCKED_LIST_VIEW)
			{
				previousType = currentViewType;
				currentViewType = RECORD_VIEW;
				recreateUI();
			}
			else
			{
				getFormUI().setDesignMode(callback);
			}
		}
		else
		{
			application.getFlattenedSolution().setInDesign(null);
			if (previousType != -1)
			{
				currentViewType = previousType;
				recreateUI();
				previousType = -1;
			}
			else
			{
				getFormUI().setDesignMode(callback);
			}
		}
	}

	private IStyleSheet stylesheet = null;
	private IStyleRule styleRule = null, styleOdd = null, styleEven = null, styleSelected = null, styleHeader = null;
	private IStyleRule bodyRule = null;

	@Override
	public void init()
	{
		initBorder();

		try
		{
			//create and fill all needed panels
			createDataRenderers(form.getView());

			super.init();
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.formPanel.error.setupForm") + ": " + getName(), ex); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void initStylesAndBorder()
	{
		initStyles();
		initBorder();
	}

	private void initBorder()
	{
		Border border = ComponentFactoryHelper.createBorder(form.getBorderType());
		if (stylesheet != null && styleRule != null)
		{
			if (border == null)
			{
				border = stylesheet.getBorder(styleRule);
			}
			bgColor = stylesheet.getBackground(styleRule);
			if (bgColor != null)
			{
				containerImpl.setBackground(bgColor);
			}
		}
		if (border != null)
		{
			containerImpl.setBorder(border);
		}
	}

	private void initStyles()
	{
		Pair<IStyleSheet, IStyleRule> pairStyle = ComponentFactory.getCSSPairStyleForForm(application, form);
		if (pairStyle != null)
		{
			stylesheet = pairStyle.getLeft();
		}
		else
		{
			stylesheet = null;
		}
		if (stylesheet != null)
		{
			String lookupname = "form"; //$NON-NLS-1$

			if (form.getStyleClass() != null && !"".equals(form.getStyleClass())) //$NON-NLS-1$
			{
				String formStyleClass = form.getStyleClass();
				lookupname += '.' + formStyleClass;
			}
			styleRule = pairStyle.getRight();
			styleOdd = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_ODD); //$NON-NLS-1$
			styleEven = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_EVEN); //$NON-NLS-1$
			styleSelected = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_SELECTED); //$NON-NLS-1$
			styleHeader = stylesheet.getCSSRule(lookupname + " " + ISupportRowStyling.CLASS_HEADER); //$NON-NLS-1$
		}
	}

	@Override
	public boolean recreateUI()
	{
		// should return false if executing from the form's execute function but
		// can happen if it is a none component thing
		// so no onfocus/ondatachange/onaction
		// of a component on the form but
		// onshow/onload that should be allowed
		//if (isFormExecutingFunction()) return false;

		// hide all visible children; here is an example that explains why it's needed:
		// parent form has tabpanel with child1 and child2; child2 is visible (second tab)
		// if you recreateUI on parent, child1 would turn out visible after recreateUI without and hide event on child2 if we wouldn't do the notifyVisible below;
		// but also when you would afterwards change tab to child2 it's onShow won't be called because it thinks it's still visible which is strange;
		List<Runnable> ilr = new ArrayList<Runnable>();
		notifyVisibleOnChildren(false, ilr);
		Utils.invokeLater(application, ilr);

		getFormUI().touch();
		Rectangle scrollPosition = null;
		if (view != null) scrollPosition = view.getVisibleRect();
		getFormUI().setDesignMode(null);
		Form f = application.getFlattenedSolution().getForm(form.getName());
		form = application.getFlattenedSolution().getFlattenedForm(f);
		initStylesAndBorder();
		int v = currentViewType;
		currentViewType = -1;
		setView(v);
		if (isFormVisible)
		{
			// first push the latest data in the regenerated ui.
			valueChanged(null);
			List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
			IDataRenderer[] array = getDataRenderers();
			for (IDataRenderer dataRenderer : array)
			{
				if (dataRenderer != null)
				{
					dataRenderer.notifyVisible(true, invokeLaterRunnables);
				}
			}
			Utils.invokeLater(application, invokeLaterRunnables);
		}
		if (designMode != null)
		{
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					getFormUI().setDesignMode(designMode);
				}
			});
		}
		getFormUI().uiRecreated();
		// make sure this form is seen as new.
		application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
		application.getFlattenedSolution().registerLiveForm(form, namedInstance);
		if (view != null && scrollPosition != null) view.setVisibleRect(scrollPosition);
		return true;
	}

	private void createDataRenderers(int viewType) throws Exception
	{
		int v = viewType;
		if (getDataSource() == null)
		{
			v = LOCKED_RECORD_VIEW; // form not based on any datasource
		}

		application.getDataRenderFactory().prepareRenderers(application, form);

		Part bodyPart = null;
		Map<Part, IDataRenderer> part_panels = new LinkedHashMap<Part, IDataRenderer>();
		Iterator<Part> e2 = form.getParts();
		while (e2.hasNext())
		{
			Part part = e2.next();

			Color partColor = ComponentFactory.getPartBackground(application, part, form);
			//extract the body (bgcolor)
			if (part.getPartType() == Part.BODY)
			{
				bodyPart = part;
				bgColor = partColor;
				Pair<IStyleSheet, IStyleRule> partStyle = ComponentFactory.getStyleForBasicComponent(application, part, form);
				bodyRule = partStyle != null ? partStyle.getRight() : null;
			}

			if (part.getPartType() == Part.BODY && v == FormController.LOCKED_TABLE_VIEW)
			{
				continue;//don't create part, view == part
			}

			IDataRenderer dr = application.getDataRenderFactory().getEmptyDataRenderer(ComponentFactory.getWebID(form, part), part.toString(), application,
				(part.getPartType() == Part.BODY));
			dr.initDragNDrop(this, form.getPartStartYPos(part.getID()));
			dataRenderers[part.getPartType()] = dr;
			dr.setName(part.toString());
			part_panels.put(part, dr);

			if (part.getPartType() == Part.BODY)
			{
				int onRenderMethodID = form.getOnRenderMethodID();
				if (onRenderMethodID > 0)
				{
					dr.getOnRenderComponent().getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
						Utils.parseJSExpressions(form.getFlattenedMethodArguments("onRenderMethodID")));
					dr.getOnRenderComponent().getRenderEventExecutor().setRenderScriptExecuter(getScriptExecuter());
				}
			}
			dr.setBackground(partColor);
		}

		tabSequence.clear();
		application.getDataRenderFactory().completeRenderers(application, form, scriptExecuter, part_panels, form.getSize().width, false,
			containerImpl.getUndoManager(), tabSequence);

		if (bodyPart != null && (v == FormController.LOCKED_LIST_VIEW || v == IForm.LIST_VIEW))
		{
			IDataRenderer dr = application.getDataRenderFactory().getEmptyDataRenderer(ComponentFactory.getWebID(form, bodyPart), bodyPart.toString(),
				application, true);

			int onRenderMethodID = form.getOnRenderMethodID();
			if (onRenderMethodID > 0)
			{
				dr.getOnRenderComponent().getRenderEventExecutor().setRenderCallback(Integer.toString(onRenderMethodID),
					Utils.parseJSExpressions(form.getFlattenedMethodArguments("onRenderMethodID")));
				dr.getOnRenderComponent().getRenderEventExecutor().setRenderScriptExecuter(getScriptExecuter());
			}
			//apply bgcolor to renderer
			if (bgColor != null)
			{
				dr.setBackground(bgColor);
			}

			dataRenderers[FORM_RENDERER] = dr;
			dr.setName(bodyPart.toString());
			part_panels = new LinkedHashMap<Part, IDataRenderer>();
			part_panels.put(bodyPart, dr);
			application.getDataRenderFactory().completeRenderers(application, form, scriptExecuter, part_panels, form.getSize().width, false,
				containerImpl.getUndoManager(), null);
		}

		//apply security
		int access = application.getFlattenedSolution().getSecurityAccess(form.getUUID());
		if (access != -1)
		{
			boolean b_accessible = ((access & IRepository.ACCESSIBLE) != 0);
			if (!b_accessible)
			{
				for (IDataRenderer dataRenderer : dataRenderers)
				{
					if (dataRenderer != null)
					{
						Iterator< ? extends IComponent> componentIterator = dataRenderer.getComponentIterator();
						while (componentIterator.hasNext())
						{
							IComponent c = componentIterator.next();
							if (c instanceof ISupportSecuritySettings)
							{
								if (!b_accessible) ((ISupportSecuritySettings)c).setAccessible(b_accessible);
							}
						}
					}
				}
			}
		}
	}


	/**
	 * @return IDataRenderer[]
	 */
	public IDataRenderer[] getDataRenderers()
	{
		return dataRenderers;
	}

	public ITagResolver getTagResolver()
	{
		for (IDataRenderer element : dataRenderers)
		{
			if (element != null)
			{
				return element.getDataAdapterList();
			}
		}
		IRecordInternal state = null;
		if (formModel != null)
		{
			int index = formModel.getSelectedIndex();
			if (index != -1)
			{
				state = formModel.getRecord(index);
			}
			if (state == null)
			{
				state = formModel.getPrototypeState();
			}
		}
		else
		{
			state = new PrototypeState(null);
		}
		return TagResolver.createResolver(state);
	}

	/*
	 * _____________________________________________________________ The methods below belong to interface IDataManipulator
	 */
	@Override
	public ControllerUndoManager getUndoManager()
	{
		return containerImpl.getUndoManager();
	}

	@Override
	public void destroy()
	{
		try
		{
			if (this.designMode != null)
			{
				setDesignMode(null);
			}
			containerImpl.destroy();

			if (fm != null) fm.removeFormController(this);
			fm = null;

			unload();

			if (view != null) //it may not yet exist
			{
				try
				{
					view.destroy();
					view = null;
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}

			deleteRenderers();

			hmChildrenJavaMembers = null;
			if (scriptExecuter != null)
			{
				scriptExecuter.destroy();
			}
			scriptExecuter = null;
			super.destroy();
		}
		catch (Exception e)
		{
			Debug.error("Destroy error", e); //$NON-NLS-1$
		}
		finally
		{
			application.getFlattenedSolution().deregisterLiveForm(form, namedInstance);
		}
	}

	private void deleteRenderers()
	{
		for (int i = 0; i < dataRenderers.length; i++)
		{
			if (dataRenderers[i] != null)
			{
				try
				{
					dataRenderers[i].destroy();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				dataRenderers[i] = null;
			}
		}
	}

	@Override
	protected void focusFirstField()
	{
		focusField(null, false);
	}

	@Override
	protected void focusField(String fieldName, final boolean skipReadonly)
	{
		final Object field = tabSequence.getComponentForFocus(fieldName, skipReadonly);
		if (field != null)
		{
			// do it with invoke later so that other focus events are overridden.
			application.invokeLater(new Runnable()
			{
				public void run()
				{
					containerImpl.focusField(field);
				}
			});
		}
	}

	/*
	 * @see com.servoy.j2db.IForm#refreshView()
	 */
	public void refreshView()
	{
		if (formModel != null)
		{
			int selected[] = null;
			if (getView() == RECORD_VIEW || getView() == LOCKED_RECORD_VIEW)
			{
				int selIdx = formModel.getSelectedIndex();
				if (selIdx != -1) selected = new int[] { selIdx };
			}
			else
			{
				selected = formModel.getSelectedIndexes();
			}
			if (selected != null && selected.length > 0)
			{
				IRecordInternal[] row = new IRecordInternal[selected.length];
				for (int i = 0; i < selected.length; i++)
					row[i] = formModel.getRecord(selected[i]);
				refreshAllPartRenderers(row);
			}
			else
			{
				refreshAllPartRenderers(null);
			}
			if (view != null && view.isDisplayingMoreThanOneRecord())
			{
				formModel.fireFoundSetChanged();
			}
		}
	}

	@Override
	protected void refreshAllPartRenderers(IRecordInternal[] records)
	{
		if (!isFormVisible || application.isShutDown()) return;
		// don't do anything yet when there are records but the selection is invalid
		if (formModel.getSize() > 0 && (formModel.getSelectedIndex() < 0 || formModel.getSelectedIndex() >= formModel.getSize())) return;

		// let the ui know that it will be touched, so that locks can be taken if needed.
		getFormUI().touch();
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
		if (dataRenderers[FORM_EDITOR] != null && !(records == null && formModel != null && formModel.getRawSize() > 0) && isStateChanged(state))
		{
			lastState = state;
			executeOnRecordSelect = true;
		}

		for (int i = FORM_RENDERER + 1; i < dataRenderers.length; i++)
		{
			IDataRenderer dataRenderer = dataRenderers[i];
			if (dataRenderer != null)
			{
				for (IRecordInternal r : state)
					dataRenderer.refreshRecord(r);
			}
		}

		if (executeOnRecordSelect)
		{
			// do this at the end because dataRenderer.refreshRecord(state) will update selection
			// for related tabs - and we should execute js code after they have been updated
			executeOnRecordSelect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#requestFocus()
	 */
	public void requestFocus()
	{
		if (containerImpl instanceof JComponent)
		{
			JComponent sf = (JComponent)containerImpl; // swingForm, TODO: remove dependency on smart client
			sf.requestFocus();
		}
		else if (view != null)
		{
			view.requestFocus();
		}
	}

	@Override
	public boolean stopUIEditing(boolean looseFocus)
	{
		if (view != null && !view.stopUIEditing(looseFocus)) return false; //controller stops editor

		IDataRenderer[] array = getDataRenderers();
		for (int i = 0; i < array.length; i++)
		{
			IDataRenderer dataRenderer = array[i];
			// skip form editor because that is already done by the view.stopUIEditing()
			if (dataRenderer != null && (FormController.FORM_EDITOR != i || view == null))
			{
				if (!dataRenderer.stopUIEditing(looseFocus)) return false;
			}
		}
		if (looseFocus && form.getOnRecordEditStopMethodID() != 0)
		{
			//allow beans to store there data via method
			IRecordInternal[] records = getApplication().getFoundSetManager().getEditRecordList().getUnmarkedEditedRecords(formModel);
			for (IRecordInternal element : records)
			{
				boolean b = executeOnRecordEditStop(element);
				if (!b) return false;
			}
		}
		return true;
	}


	/*
	 * _____________________________________________________________ The methods below override methods from superclass <classname>
	 */
	//this method first overloaded setVisible but setVisible is not always called and had differences between jdks
	@Override
	public boolean notifyVisible(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		if (isFormVisible == visible || executingOnLoad) return true;
		if (formModel == null)
		{
			isFormVisible = visible;
			return true;
		}

		boolean notifyVisible = super.notifyVisible(visible, invokeLaterRunnables);

		if (notifyVisible)
		{
			notifyVisibleOnChildren(visible, invokeLaterRunnables);
		}
		return notifyVisible;
	}

	private void notifyVisibleOnChildren(boolean visible, List<Runnable> invokeLaterRunnables)
	{
		IDataRenderer[] array = getDataRenderers();
		for (IDataRenderer dataRenderer : array)
		{
			if (dataRenderer != null)
			{
				dataRenderer.notifyVisible(visible, invokeLaterRunnables);
			}
		}
	}

	/*
	 * _____________________________________________________________ The methods below belong to this class
	 */

	@Override
	public void propagateFindMode(boolean findMode)
	{
		if (!findMode)
		{
			application.getFoundSetManager().getEditRecordList().prepareForSave(true);
		}
		if (isReadOnly())
		{
			if (view != null)
			{
				view.setEditable(findMode);
			}
		}

		for (IDataRenderer dataRenderer : dataRenderers)
		{
			if (dataRenderer != null)
			{
				//				if (i == FORM_RENDERER || i == FORM_EDITOR)
				//				{
				// if the data renderer itself is a IDisplay then go through that one
				// see web cell based view that is used as a tableview and portal.
				if (dataRenderer instanceof IDisplay)
				{
					((IDisplay)dataRenderer).setValidationEnabled(!findMode);
				}
				else
				{
					DataAdapterList dal = dataRenderer.getDataAdapterList();
					dal.setFindMode(findMode);//disables related data en does getText instead if getValue on fields
					dataRenderer.setAllNonFieldsEnabled(!findMode);
				}
				//				}
				//				else
				//				{
				//					//dataRenderers[i].setUIEnabled(!findMode);
				//				}
			}
		}
	}

	@Override
	public int getView()
	{
		return currentViewType;
	}

	@Override
	public IView getViewComponent()
	{
		return view;
	}

	//set the view (record,list,table)
	@Override
	public void setView(int v)
	{
		int viewType = v;
		if (viewType == -1)
		{
			viewType = getForm().getView();
		}

		if (viewType == currentViewType)
		{
			return;//no change
		}

		if (currentViewType == LOCKED_TABLE_VIEW || currentViewType == LOCKED_LIST_VIEW || currentViewType == LOCKED_RECORD_VIEW)
		{
			return; // don't let user change
		}

		currentViewType = viewType;

		// always synch the view menu with this call
		if (application.getFormManager() instanceof IFormManagerInternal)
		{
			IBasicFormManager sfm = application.getFormManager();
			if (sfm.getCurrentMainShowingFormController() == this)
			{
				((IFormManagerInternal)sfm).synchViewMenu(viewType);
			}
		}

		boolean formReadOnly = ((FormManager)application.getFormManager()).isFormReadOnly(getName());

		//uninstall old view
		if (view != null)
		{
			// if the form manager says this is readonly or the container is currently in readonly
			// do revert it here else the readonly flag from the form manager and the containre itself are out of sync
			// with the elements that are now created again.
			if (formReadOnly || containerImpl.isReadOnly())
			{
				containerImpl.setReadOnly(false);
			}
			view.stop();
			view = null;

			deleteRenderers();
			try
			{
				createDataRenderers(viewType);
			}
			catch (Exception e)
			{
				application.reportError(application.getI18NMessage("servoy.formPanel.error.setupForm") + ": " + getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		if (bgColor != null)
		{
			containerImpl.setBackground(bgColor);
		}

		view = containerImpl.initView(application, this, viewType);


		// Do tab sequencing now.
		tabSequence.fromAbstractToNamed();

		view.start(application);
		// make sure that the formscope is created to insert the elements in it.
		getFormScope();
		if (formScope != null)
		{
			hmChildrenJavaMembers = new HashMap<String, Object[]>();
			Scriptable scope = getFormUI().makeElementsScriptObject(formScope, hmChildrenJavaMembers, dataRenderers, view);
			formScope.putWithoutFireChange("elements", scope); //$NON-NLS-1$
		}
		if (isFormVisible)
		{
			view.setModel(formModel);
		}
		view.setRowBGColorScript(form.getRowBGColorCalculation(), form.getFlattenedMethodArguments("rowBGColorCalculation")); //$NON-NLS-1$
		if (view instanceof ISupportRowStyling) ((ISupportRowStyling)view).setRowStyles(stylesheet, styleOdd, styleEven, styleSelected, styleHeader);

		if (formReadOnly)
		{
			containerImpl.setReadOnly(true);
		}

	}

	@Override
	public void showNavigator(List<Runnable> invokeLaterRunnables)
	{
		if (fm != null && fm.getCurrentMainShowingFormController() == this)//safety for tabpanels (those cannot have a custom controller)
		{
			ISupportNavigator navigatorSupport = fm.getCurrentContainer();
			if (navigatorSupport != null)
			{
				int form_id = form.getNavigatorID();
				if (form_id > 0)
				{
					FormController currentFC = navigatorSupport.getNavigator();
					if (currentFC == null || currentFC.getForm().getID() != form_id)//is already there
					{
						try
						{
							Form sliderDef = application.getFlattenedSolution().getForm(form_id);
							if (sliderDef != null)
							{
								if (sliderDef.getID() != form_id && sliderDef.getNavigatorID() >= 0) return;//safety a slider Form CANNOT HAVE A SLIDER

								FormController nav = fm.getFormController(sliderDef.getName(), navigatorSupport);
								ControllerUndoManager cum = null;
								if (nav != null)
								{
									nav.notifyVisible(true, invokeLaterRunnables);
									cum = nav.getUndoManager();
								}
								if (application.getCmdManager() instanceof ICmdManagerInternal)
								{
									((ICmdManagerInternal)application.getCmdManager()).setControllerUndoManager(cum);
								}
								FormController old_nav = navigatorSupport.setNavigator(nav);
								if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
							}
							else
							{
								// Form deleted??
								FormController old_nav = navigatorSupport.setNavigator(null);
								if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}
					else
					{
						// Try to lease it extra so it will be added to last used screens.
						Form sliderDef = application.getFlattenedSolution().getForm(form_id);
						if (sliderDef != null)
						{
							fm.leaseFormPanel(sliderDef.getName());
							currentFC.notifyVisible(true, invokeLaterRunnables);
						}
					}
				}
				else if (form_id != Form.NAVIGATOR_IGNORE)//if is ignore leave previous,if not remove
				{
					FormController old_nav = navigatorSupport.setNavigator(null);
					if (old_nav != null) old_nav.notifyVisible(false, invokeLaterRunnables);
				}
			}
		}
	}


	private ScriptExecuter scriptExecuter;

	private static class ScriptExecuter implements IScriptExecuter//this class is made only when swing has a mem leak we don't have one
	{
		private FormController delegate;

		ScriptExecuter(FormController fp)
		{
			delegate = fp;
		}

		public Object executeFunction(String cmd, Object[] args, boolean saveData, Object src, boolean focusEvent, String methodKey,
			boolean executeWhenFieldValidationFailed)
		{
			if (delegate != null) return delegate.executeFunction(cmd, args, saveData, src, focusEvent, methodKey, true, executeWhenFieldValidationFailed);
			return null;
		}

		public void setLastKeyModifiers(int modifiers)
		{
			if (delegate != null)
			{
				delegate.setLastKeyModifiers(modifiers);
			}
		}

		public FormController getFormController()
		{
			return delegate;
		}

		public void destroy()
		{
			delegate = null;
		}
	}

	/*
	 * _____________________________________________________________ Java script methods/implementation
	 */

	public void setLastKeyModifiers(int modifiers)
	{
		((IScriptSupport)application.getScriptEngine()).setLastKeyModifiers(modifiers);
	}

	@Override
	public void setReadOnly(boolean b)
	{
		if (b) stopUIEditing(true);
		containerImpl.setReadOnly(b);
		application.getFormManager().setFormReadOnly(getName(), b);
		if (b && containerImpl.getUndoManager() != null) containerImpl.getUndoManager().discardAllEdits();
	}

	@Override
	public void setComponentEnabled(boolean b)
	{
		containerImpl.setComponentEnabled(b);
		application.getFormManager().setFormEnabled(getName(), b);
	}

	@Override
	public int getPartYOffset(int partType)
	{
		IDataRenderer[] renderers = getDataRenderers();
		if (renderers != null && partType < renderers.length && renderers[partType] != null)
		{
			return renderers[partType].getYOffset();
		}
		return 0;
	}

	@Override
	protected JSEvent getJSEvent(Object src)
	{
		JSEvent event = new JSEvent();
		event.setType(JSEvent.EventType.form);
		event.setFormName(getName());
		event.setSource(src);
		event.setElementName(src instanceof IComponent ? ((IComponent)src).getName() : null);
		return event;
	}

	/**
	 * @param src
	 * @param function
	 * @param useFormAsEventSourceEventually
	 */
	@Override
	protected FormAndComponent getJSApplicationNames(Object source, Function function, boolean useFormAsEventSourceEventually)
	{
		Object src = source;
		if (src == null)
		{
			Object window = application.getRuntimeWindowManager().getCurrentWindowWrappedObject();
//			if (!(window instanceof Window) || !((Window)window).isVisible())
//			{
//				window = application.getMainApplicationFrame();
//			}

			if (window != null)
			{
				src = ((Window)window).getFocusOwner();
				while (src != null && !(src instanceof IComponent))
				{
					src = ((Component)src).getParent();
				}
				// Test if this component really comes from the the controllers UI.
				if (src instanceof Component)
				{
					Container container = ((Component)src).getParent();
					while (container != null && !(container instanceof IFormUIInternal< ? >))
					{
						container = container.getParent();
					}
					if (container != getFormUI())
					{
						// if not then this is not the trigger element for this form.
						src = null;
					}
				}
			}
		}

		Scriptable thisObject = null;
		if (src instanceof IComponent && src instanceof IScriptableProvider)
		{
			Object esObj = formScope.get("elements", formScope); //$NON-NLS-1$
			if (esObj != Scriptable.NOT_FOUND)
			{
				ElementScope es = (ElementScope)esObj;
				String name = ((IComponent)src).getName();
				if (name != null && name.length() != 0)
				{
					Object o = es.get(name, es);
					if (o instanceof Scriptable)
					{
						thisObject = (Scriptable)o;
					}
				}

				if (thisObject == null)
				{
					if (name == null || name.length() == 0)
					{
						name = ComponentFactory.WEB_ID_PREFIX + System.currentTimeMillis();
						// Check Web components always have a name! Because name can't be set
						((IComponent)src).setName(name);
					}
					try
					{
						Context.enter();
						IScriptable scriptObject = ((IScriptableProvider)src).getScriptObject();
						JavaMembers jm = ScriptObjectRegistry.getJavaMembers(scriptObject.getClass(), ScriptableObject.getTopLevelScope(formScope));
						thisObject = new NativeJavaObject(formScope, scriptObject, jm);

						es.setLocked(false);
						es.put(name, es, thisObject);
						es.setLocked(true);
					}
					finally
					{
						Context.exit();
					}
				}
			}
		}
		if (src == null && useFormAsEventSourceEventually) src = formScope;
		return new FormAndComponent(src, getName());
	}

	//	private static int isExecuting = 0;
	//	private static LinkedList executeStack = new LinkedList();

	private Map<String, Object[]> hmChildrenJavaMembers;

	public Map<String, Object[]> getJavaMembers()
	{
		return hmChildrenJavaMembers;
	}

	@Override
	public Object setUsingAsExternalComponent(boolean visibleExternal) throws ServoyException
	{
		super.setUsingAsExternalComponent(visibleExternal);
		if (visibleExternal)
		{
			initForJSUsage();
			setView(getView());
			executeOnLoadMethod();
		}
		else
		{
			// make sure that the ui will have pushed all changes
			stopUIEditing(true);
		}

		List<Runnable> invokeLaterRunnables = new ArrayList<Runnable>();
		boolean ok = notifyVisible(visibleExternal, invokeLaterRunnables);
		Utils.invokeLater(application, invokeLaterRunnables);
		if (!ok)
		{
			//TODO cannot hide...what todo?
		}
		else if (visibleExternal && getFormUI() instanceof JComponent)
		{
			// If it is a swing component, test if it has a parent.
			JComponent formUI = (JComponent)getFormUI();
			if (formUI.getParent() != null)
			{
				// remove it from the parent
				formUI.getParent().remove(formUI);
			}
		}
		// and make sure it is visible when we return it. (FixedCardLayout will set it to none visible when remove)
		getFormUI().setComponentVisible(true);
		return (visibleExternal ? getFormUI() : null);
	}

	public void selectNextRecord()
	{
		if (form.getOnNextRecordCmdMethodID() == 0)
		{
			if ((application.getFoundSetManager().getEditRecordList().stopEditing(false) & (ISaveConstants.STOPPED + ISaveConstants.AUTO_SAVE_BLOCKED)) == 0)
			{
				return;
			}

			IFoundSet fs = getFoundSet();
			int nextIndex = fs.getSelectedIndex() + 1;
			if (nextIndex >= 0 && nextIndex < fs.getSize())
			{
				final Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				fs.setSelectedIndex(nextIndex);
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getView() == IForm.LOCKED_RECORD_VIEW || getView() == IForm.RECORD_VIEW)
						{
							if (comp != null) comp.requestFocus();
						}
						else
						{
							requestFocus();
						}
					}
				});
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	public void selectPrevRecord()
	{
		if (form.getOnPreviousRecordCmdMethodID() == 0)
		{
			int edittingStoppedFlag = application.getFoundSetManager().getEditRecordList().stopEditing(false);
			if (ISaveConstants.STOPPED != edittingStoppedFlag && ISaveConstants.AUTO_SAVE_BLOCKED != edittingStoppedFlag)
			{
				return;
			}

			IFoundSet fs = getFoundSet();
			int nextIndex = fs.getSelectedIndex() - 1;
			if (nextIndex >= 0)
			{
				final Component comp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				fs.setSelectedIndex(nextIndex);
				application.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getView() == IForm.LOCKED_RECORD_VIEW || getView() == IForm.RECORD_VIEW)
						{
							if (comp != null) comp.requestFocus();
						}
						else
						{
							requestFocus();
						}
					}
				});
			}
		}
		else
		{
			executeFormMethod(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID, null, Boolean.TRUE, true, true);
		}
	}

	public IScriptExecuter getScriptExecuter()
	{
		return scriptExecuter;
	}

	@Override
	public IFormUIInternal getFormUI()
	{
		return containerImpl;
	}

	@Override
	public String toString()
	{
		if (formModel != null)
		{
			return "FormController[form: " + getName() + ", fs size:" + Integer.toString(formModel.getSize()) + ", selected record: " + formModel.getRecord(formModel.getSelectedIndex()) + ",destroyed:" + isDestroyed() + "]"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
		}
		else
		{
			return "FormController[form: " + getName() + ",destroyed:" + isDestroyed() + "]"; //$NON-NLS-1$//$NON-NLS-2$
		}
	}


	/**
	 *
	 */
	public void touch()
	{
		if (!isFormVisible && fm != null)
		{
			fm.touch(this);
		}
	}

	public void recomputeTabSequence(int baseTabSequenceIndex)
	{
		application.getDataRenderFactory().reapplyTabSequence(getFormUI(), baseTabSequenceIndex);
	}

	@Override
	public void setTabSequence(Object[] arrayOfElements)
	{
		tabSequence.setRuntimeTabSequence(arrayOfElements);
	}

	@Override
	public String[] getTabSequence()
	{
		String[] tabSequenceNames = tabSequence.getNamesInTabSequence();
		if (view instanceof IProvideTabSequence)
		{
			List<String> namesList = ((IProvideTabSequence)view).getTabSeqComponentNames();
			if (tabSequenceNames.length != 0)
			{
				if (!namesList.isEmpty()) tabSequenceNames = Utils.arrayJoin(tabSequenceNames, namesList.toArray(new String[namesList.size()]));
			}
			else return namesList.toArray(new String[namesList.size()]);
		}
		return tabSequenceNames;
	}

	public Serializable getComponentProperty(Object component, String key)
	{
		return ComponentFactory.getComponentProperty(application, component, key);
	}

	public IStyleRule getFormStyle()
	{
		return styleRule;
	}

	public IStyleRule getBodyStyle()
	{
		return bodyRule;
	}
}