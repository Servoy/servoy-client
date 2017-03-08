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
package com.servoy.j2db.dataprocessing;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.Document;

import org.mozilla.javascript.Scriptable;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.dataui.IServoyAwareVisibilityBean;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.scripting.ScopesScope;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.ui.IDataRenderer;
import com.servoy.j2db.ui.ISupportOnRenderCallback;
import com.servoy.j2db.ui.RenderEventExecutor;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;

/**
 * This class encapsulates all the dataproviders for a form part (mainly the body) ,it does the creation and setup of dataAdapters<br>
 * An object of this class is usually hold inside a part 'Renderer'.
 *
 * @author jblok
 */
public class DataAdapterList implements IModificationListener, ITagResolver
{
	private boolean visible = false;
	private LinkedHashMap<String, IDataAdapter> dataAdapters;
	private final IDataProviderLookup dataProviderLookup;
	private List<IServoyAwareBean> servoyAwareBeans;
	private List<IDisplayRelatedData> relatedDataAdapters;//for portals
	private List<IDisplayData> dataDisplays;
	private IRecordInternal currentRecord;
	private final IApplication application;
	private final ControllerUndoManager undoManager;
	private final FormController formController;

	/**
	 * Constructor I<br>
	 * Creates all data adapters, and dataChangeListeners between them.
	 *
	 * @param p the part this adapterlist is working for
	 * @param formObjects , map with IPersist -> IDisplay
	 * @param displays may provide map with dataProviderID -> IDataAdapter for already created adapters, or null/empty if none
	 */
	public DataAdapterList(IApplication app, IDataProviderLookup dataProviderLookup, Map<IPersist, ? extends Object> formObjects, FormController formController,
		LinkedHashMap<String, IDataAdapter> dataAdapters, ControllerUndoManager undoManager) throws RepositoryException
	{
		application = app;
		this.formController = formController;

		//initialize the lists
		relatedDataAdapters = new ArrayList<IDisplayRelatedData>();

		// beans
		servoyAwareBeans = new ArrayList<IServoyAwareBean>(2);

		// data displays
		dataDisplays = new ArrayList<IDisplayData>();

		//set available undoManager
		this.undoManager = undoManager;

		if (dataAdapters == null)
		{
			//create temp list, dataProviderID -> IDataAdapter
			this.dataAdapters = new LinkedHashMap<String, IDataAdapter>();
		}
		else
		{
			this.dataAdapters = dataAdapters;
		}
		this.dataProviderLookup = dataProviderLookup;

		RelatedFieldHolder rfh = null;
		ITable table = null;
		if (dataProviderLookup != null)
		{
			table = dataProviderLookup.getTable();
		}
		//add
		FlattenedSolution fs = application.getFlattenedSolution();
		for (Object obj : formObjects.values())
		{
			if (obj instanceof IDisplayData)
			{
				IDisplayData display = (IDisplayData)obj;
				dataDisplays.add(display);

				Document doc = display.getDocument();
				if (doc != null && undoManager != null) //add undoManager if needed
				{
					doc.addUndoableEditListener(undoManager);
				}

				IDataAdapter da = null;

				String dataProviderID = display.getDataProviderID();
				if (dataProviderID != null && !ScopesUtils.isVariableScope(dataProviderID))
				{
					int idx = dataProviderID.lastIndexOf('.');
					if (idx != -1) //handle related fields
					{
						Relation[] relations = fs.getRelationSequence(dataProviderID.substring(0, idx));
						boolean ok = relations != null;
						if (ok)
						{
							for (Relation relation : relations)
							{
								ok &= Relation.isValid(relation, fs);
							}
						}
						if (ok)
						{
							if (rfh == null)
							{
								rfh = new RelatedFieldHolder(this);
								relatedDataAdapters.add(rfh);
							}
							rfh.addDisplay(display);
						}
					}
				}
				da = this.dataAdapters.get(dataProviderID);

				if (da == null)
				{
					da = new DisplaysAdapter(application, this, dataProviderID, display);
					this.dataAdapters.put(dataProviderID, da);
				}
				else if (da instanceof DisplaysAdapter)
				{
					((DisplaysAdapter)da).addDisplay(display);//for more than one field on form showing same content...
				}

				if (display.needEditListener() && da instanceof IEditListener)
				{
					display.addEditListener((IEditListener)da);
				}
			}
			if (obj instanceof IDisplayRelatedData)//portals, etc
			{
				relatedDataAdapters.add((IDisplayRelatedData)obj);
			}
			if (obj instanceof IServoyAwareBean)
			{
				servoyAwareBeans.add((IServoyAwareBean)obj);
			}
		}

		//see if relookup listeners must be added
		if (table != null)
		{
			for (Column c : table.getColumns())
			{
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
				{
					String lookup = ci.getLookupValue();
					if (lookup != null && lookup.length() != 0 && !ScopesUtils.isVariableScope(lookup))
					{
						int index = lookup.lastIndexOf('.');
						if (index != -1)
						{
							try
							{
								Relation[] relations = fs.getRelationSequence(lookup.substring(0, index));
								if (relations != null && relations.length > 0)
								{
									// add a RelookupAdapter for the last relation
									Relation relation = relations[relations.length - 1];
									IDataProvider[] dps = relation.getPrimaryDataProviders(fs);
									if (dps != null)
									{
										IDataAdapter la = this.dataAdapters.get("LA:" + lookup); //$NON-NLS-1$
										if (la == null)
										{
											StringBuilder prefix = new StringBuilder();
											for (int r = 0; r < relations.length - 1; r++)
											{
												prefix.append(relations[r].getName());
												prefix.append('.');
											}
											Set<String> dataProviderIDs = new HashSet<String>();
											for (IDataProvider dp : dps)
											{
												if (dp instanceof LiteralDataprovider) continue;
												String dataProviderID;
												if (ScopesUtils.isVariableScope(dp.getDataProviderID()))
												{
													dataProviderID = dp.getDataProviderID();
												}
												else
												{
													dataProviderID = prefix.toString() + dp.getDataProviderID();
												}
												dataProviderIDs.add(dataProviderID);
											}

											la = new RelookupAdapter(this, c.getDataProviderID(), lookup, dataProviderIDs);
											this.dataAdapters.put("LA:" + lookup, la); //$NON-NLS-1$
										}
										else
										{
											((RelookupAdapter)la).addDataProviderId(c.getDataProviderID());
										}

										// Add data adapters for the other relations
										addDataAdaptersForRelationSequence(la, relations, relations.length - 1);
									}
								}
							}
							catch (RepositoryException e)
							{
								Debug.error(e);
							}
						}
					}
				}
			}
		}

		if (Debug.tracing())
		{
			for (IDataAdapter da : this.dataAdapters.values())
			{
				Debug.trace("-->" + da); //$NON-NLS-1$
			}
			for (IDisplayRelatedData drd : relatedDataAdapters)
			{
				Debug.trace("R->" + drd); //$NON-NLS-1$
			}
		}

		//add listener for var changes
		if (formController != null) // can happen for a design component
		{
			formController.getFormScope().getModificationSubject().addModificationListener(this);
			application.getScriptEngine().getScopesScope().getModificationSubject().addModificationListener(this);
		}
	}

	/**
	 * Walk over the relation sequence to add data adapters.
	 *
	 * @param dataAdapter
	 * @param relations
	 * @param rel
	 * @throws RepositoryException
	 */
	protected void addDataAdaptersForRelationSequence(IDataAdapter dataAdapter, Relation[] relations, int rel) throws RepositoryException
	{
		Relation relation = relations[rel];
		IDataProvider[] dps = relation.getPrimaryDataProviders(application.getFlattenedSolution());
		if (dps != null)
		{
			for (IDataProvider dp : dps)
			{
				if (dp instanceof LiteralDataprovider) continue;
				StringBuilder prefix = new StringBuilder();
				for (int r = 0; r < rel; r++)
				{
					prefix.append(relations[r].getName());
					prefix.append('.');
				}
				String dataProviderID;
				if (ScopesUtils.isVariableScope(dp.getDataProviderID()))
				{
					dataProviderID = dp.getDataProviderID();
				}
				else
				{
					dataProviderID = prefix.toString() + dp.getDataProviderID();
				}
				IDataAdapter da = dataAdapters.get(dataProviderID);
				if (da == null)
				{
					da = new DataAdapter(dataProviderID);
					dataAdapters.put(dataProviderID, da);
				}
				da.addDataListener(dataAdapter);
				if (rel > 0)
				{
					addDataAdaptersForRelationSequence(da, relations, rel - 1);
				}
			}
		}
	}

	public IRecordInternal getState()
	{
		return currentRecord;
	}

	/**
	 * Inform all dataAdapters about the new state. <br><br>
	 *
	 * Note on param: stopAnyEdit, the Renderer paints state 6 (record 6) after that the editor comes in and edits
	 * that state 6, then another row must be repainted so State 7 and it calls stop editing on 6, but the editor is
	 * still editing that state 6
	 *
	 * @param state the new state (can be null to delete old state)
	 */
	public void setRecord(IRecordInternal state, boolean stopAnyEdit)
	{
		if (destroyed)
		{
			Debug.error("calling setRecord on a destroyed DataAdapterList, formcontroller: " + formController + ", currentRecord: " + currentRecord,
				new RuntimeException());
			return;
		}
		if (undoManager != null) undoManager.setIgnoreEdits(true);

		if (currentRecord != null)
		{
			// NEVER enable this ,values in the state could be changed: if (currentState.equals(state)) return;//same
			if (!currentRecord.equals(state))
			{
				stopUIEditing(false);
			}
			if (state != currentRecord)
			{
				currentRecord.removeModificationListener(this);//unregister
			}
		}

		if (state != null && state != currentRecord)
		{
			state.addModificationListener(this);//register so we are notified about javascript changes on non global vars from here
		}
		currentRecord = state;
		//1) handle first related data (needed for comboboxes)
		for (IDisplayRelatedData drd : relatedDataAdapters)
		{
			if (state != null)// && !(state instanceof PrototypeState))
			{
				if (!(drd instanceof RelatedFieldHolder))//performance enhancement
				{
					drd.setRecord(state, stopAnyEdit);
				}
			}
			else
			{
				drd.setRecord(null, true);//clear
			}
		}
		// check if destroyed.
		if (dataAdapters == null) return;


		//2) handle all fields
		Iterator<IDataAdapter> it = dataAdapters.values().iterator();
		while (it.hasNext())
		{
			IDataAdapter da = it.next();
			da.setRecord(state);
		}

		if (currentRecord != null && servoyAwareBeans.size() > 0)
		{
			ServoyBeanState sbr = new ServoyBeanState(state, getFormScope());
			for (IServoyAwareBean da : servoyAwareBeans)
			{
				try
				{
					da.setSelectedRecord(sbr);
				}
				catch (RuntimeException e)
				{
					//never make the app break on faulty beans
					Debug.error(e);
				}
			}
		}
		if (undoManager != null) undoManager.setIgnoreEdits(false);
	}

	//called by relation adapter
	void resetRelatedFoundSet(IDisplayRelatedData drd)//called when something on the relation input(LH) changed
	{
		if (currentRecord != null)
		{
			drd.setRecord(currentRecord, true);
		}
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationListner
	 */
	/**
	 * listen for global var changes via own listener and state vars(mainly columns) via state listener if via javascript any var is changed it will be noted
	 * here,and dispatched to refresh the displays
	 */
	public void valueChanged(ModificationEvent e)
	{
		if (destroyed)
		{
			Debug.error("Destroyed DataAdapterList " + formController + " was still attached to the record: " + e.getRecord() +
				", removing it if possible, currentRecord: " + currentRecord, new RuntimeException());
			if (e.getRecord() != null) e.getRecord().removeModificationListener(this);
			else if (currentRecord != null) currentRecord.removeModificationListener(this);
			return;
		}
		if (formController != null && formController.isDestroyed())
		{
			Debug.error("Destroying DataAdapterList of a destroyed " + formController, new RuntimeException());
			destroy();
			return;
		}
		FormScope formScope = getFormScope();
		if (visible && (currentRecord != null || (formScope != null && formScope.has(e.getName(), formScope))))
		{
			for (IDataAdapter da : dataAdapters.values())
			{
				da.valueChanged(e); // dataAdapter should call state.getValue if name from event is same as its dataProviderID
			}

			// check if a related adapter depends on he global
			if (e != null && e.getName() != null)
			{
				for (IDisplayRelatedData drd : relatedDataAdapters)
				{
					boolean depends = false;
					String[] allRelationNames = drd.getAllRelationNames();
					for (int a = 0; !depends && allRelationNames != null && a < allRelationNames.length; a++)
					{
						Relation[] relations = application.getFlattenedSolution().getRelationSequence(allRelationNames[a]);
						for (int r = 0; !depends && relations != null && r < relations.length; r++)
						{
							try
							{
								IDataProvider[] primaryDataProviders = relations[r].getPrimaryDataProviders(application.getFlattenedSolution());
								for (int p = 0; !depends && primaryDataProviders != null && p < primaryDataProviders.length; p++)
								{
									depends = e.getName().equals(primaryDataProviders[p].getDataProviderID());
								}
							}
							catch (RepositoryException ex)
							{
								Debug.log(ex);
							}
						}
					}
					if (depends)
					{
						// related adapter depends on the modified global
						if (drd instanceof IDisplayDependencyData) ((IDisplayDependencyData)drd).dependencyChanged(currentRecord);
						else drd.setRecord(currentRecord, true);
					}
				}
			}

			// inform servoy aware beans
			for (IServoyAwareBean drd : servoyAwareBeans)
			{
				if (drd instanceof IModificationListener)
				{
					try
					{
						((IModificationListener)drd).valueChanged(e);
					}
					catch (RuntimeException ex)
					{
						//never make the app break on faulty beans
						Debug.error(ex);
					}
				}
			}
		}
	}

	//should be disabled if not showing!
	public void notifyVisible(boolean b, List<Runnable> invokeLaterRunnables)
	{
		if (visible == b) return;
		visible = b;
		for (IDisplayRelatedData drd : relatedDataAdapters)
		{
			drd.notifyVisible(b, invokeLaterRunnables);
		}

		for (IServoyAwareBean bean : servoyAwareBeans)
		{
			if (bean instanceof IServoyAwareVisibilityBean)
			{
				((IServoyAwareVisibilityBean)bean).notifyVisible(b);
			}
		}

		if (b)
		{
			//this guaranties that if related data is changes in other form bases directly on that data, the changes are visible in the relted data of this form
			setRecord(currentRecord, true);
		}
	}

	public void setFindMode(boolean b)
	{
		//IDataAdapter do cover all IDisplayData's
		for (IDataAdapter da : dataAdapters.values())
		{
			da.setFindMode(b);
		}
		for (IDisplayRelatedData drd : relatedDataAdapters)
		{
			if (drd instanceof IDisplayData)
			{
				IDisplayData drdDisplay = (IDisplayData)drd;
				if (getFormScope() != null && drdDisplay.getDataProviderID() != null &&
					getFormScope().get(drdDisplay.getDataProviderID()) != Scriptable.NOT_FOUND) continue; // skip for form variables
			}
			drd.setValidationEnabled(!b);
		}
		for (IServoyAwareBean drd : servoyAwareBeans)
		{
			try
			{
				drd.setValidationEnabled(!b);
			}
			catch (RuntimeException e)
			{
				//never make the app break on faulty beans
				Debug.error(e);
			}
		}
	}

	public FormScope getFormScope()
	{
		if (formController == null) // can happen for a design component
		{
			return null;
		}
		if (destroyed)
		{
			Debug.error("calling getFormScope on a destroyed DataAdapterList, formcontroller: " + formController + ", currentRecord: " + currentRecord,
				new RuntimeException());
			return null;
		}
		FormScope formScope = formController.getFormScope();
		if (formScope == null && formController.isDestroyed())
		{
			Debug.error("calling getFormScope in none destroyed DataAdapterList (destroying it now) for a destroyed formcontroller: " + formController +
				", currentRecord: " + currentRecord, new RuntimeException());
			destroy();
			return null;
		}
		return formScope;
	}

	public FormController getFormController()
	{
		return formController;
	}

	/**
	 * when the panel gets focus the fields lose focus,the editing is presument to be stopped
	 */
	private boolean isStoppingUIEditing = false;

	public boolean stopUIEditing(boolean looseFocus)
	{
		// if current record is null the ui cant be in a edit mode because there is nothing to edit.
		if (isStoppingUIEditing || currentRecord == null)
		{
			return true;
		}
		isStoppingUIEditing = true;
		try
		{
			for (IDisplayRelatedData drd : relatedDataAdapters)
			{
				if (!drd.stopUIEditing(looseFocus)) return false;
			}

			for (IServoyAwareBean drd : servoyAwareBeans)
			{
				try
				{
					if (!drd.stopUIEditing(looseFocus)) return false;
				}
				catch (RuntimeException e)
				{
					//never make the app break on faulty beans
					Debug.error(e);
				}
			}

			if (currentDisplay != null)
			{
				if (!currentDisplay.stopUIEditing(looseFocus))
				{
					return false;
				}
				if (anyInvalidDisplays(looseFocus, currentDisplay)) return false; // currentDisplay is second parameter in order not to try to stop it again
				if (looseFocus)
				{
					currentDisplay = null;
				}
			}
			return true;
		}
		finally
		{
			isStoppingUIEditing = false;
		}
	}

	private IDisplay currentDisplay;

	public void setCurrentDisplay(IDisplay cd)
	{
		if (currentDisplay != cd)
		{
			if (currentDisplay != null)
			{
				if (!currentDisplay.stopUIEditing(true)) return;
				if (anyInvalidDisplays(true, cd)) return;
			}
			currentDisplay = cd;
		}
	}

	private boolean anyInvalidDisplays(boolean looseFocus, IDisplay newCurrentDisplay)
	{
		// see if there is any invalid display - if it is, and it is not getting focus now,
		// try to call stopUIEditing() on it - so that it either validates or requests focus (you can end
		// up having multiple invalid displays - in case of check-boxes, radios, calendars and so on - and this is needed
		// to make sure all of them are validated)
		for (IDisplayData display : dataDisplays)
		{
			if ((display != newCurrentDisplay) && (!display.isValueValid()) && (!display.stopUIEditing(looseFocus))) return true;
		}

		return false;
	}

	/**
	 * Checks whatever the dataAdapter is for count aggregate
	 *
	 * @param dataAdapter
	 * @return true if dataAdapter is for count aggregate, otherwise false
	 */
	public boolean isCountOrAvgOrSumAggregateDataProvider(IDataAdapter dataAdapter)
	{
		return isCountOrAvgOrSumAggregateDataProvider(dataAdapter.getDataProviderID(), dataProviderLookup);
	}

	private static boolean isCountOrAvgOrSumAggregateDataProvider(String dataProvider, IDataProviderLookup dataProviderLookup)
	{
		try
		{
			if (dataProviderLookup == null)
			{
				return false;
			}
			IDataProvider dp = dataProviderLookup.getDataProvider(dataProvider);
			if (dp instanceof ColumnWrapper)
			{
				dp = ((ColumnWrapper)dp).getColumn();
			}
			if (dp instanceof AggregateVariable)
			{
				int aggType = ((AggregateVariable)dp).getType();
				return aggType == QueryAggregate.COUNT || aggType == QueryAggregate.AVG || aggType == QueryAggregate.SUM;
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		return false;
	}

	public IApplication getApplication()
	{
		return application;
	}

	private boolean destroyed = false;

	/**
	 *
	 */
	public void destroy()
	{
		if (currentRecord != null)
		{
			// With prototype you can still get global foundsets
			//setRecord(new PrototypeState(currentRecord.getParentFoundSet()), true);
			setRecord(null, false);
		}

		if (formController != null && !formController.isDestroyed() && formController.getFormScope() != null)
		{
			formController.getFormScope().getModificationSubject().removeModificationListener(this);
		}
		IExecutingEnviroment er = application.getScriptEngine();
		if (er != null)
		{
			SolutionScope ss = er.getSolutionScope();
			if (ss != null)
			{
				ScopesScope gs = ss.getScopesScope();
				if (gs != null)
				{
					gs.getModificationSubject().removeModificationListener(this);
				}
			}
		}

		if (servoyAwareBeans != null)
		{
			for (IServoyAwareBean b : servoyAwareBeans)
			{
				try
				{
					if (b instanceof IDestroyable)
					{
						((IDestroyable)b).destroy();
					}
				}
				catch (RuntimeException e)
				{
					//never make the app break on faulty beans
					Debug.error(e);
				}
			}
		}
		servoyAwareBeans = null;


		if (relatedDataAdapters != null)
		{
			for (IDisplayRelatedData drd : relatedDataAdapters)
			{
				drd.destroy();
			}
		}
		relatedDataAdapters = null;

		if (dataDisplays != null)
		{
			for (IDisplayData dd : dataDisplays)
			{
				if (dd instanceof IDestroyable)
				{
					((IDestroyable)dd).destroy();
				}
			}
		}
		dataDisplays = null;

		if (dataAdapters != null)
		{
			for (IDataAdapter da : dataAdapters.values())
			{
				if (da instanceof IDestroyable)
				{
					((IDestroyable)da).destroy();
				}
			}
		}
		dataAdapters = null;

		currentDisplay = null;
		visible = false;
		destroyed = true;
		if (currentRecord != null)
		{
			Debug.error("After destroy there is still a current record in DataAdapterList of formcontroller: " + formController, new RuntimeException()); //$NON-NLS-1$
			currentRecord.removeModificationListener(this);
		}
	}

	public String getStringValue(String name)
	{
		String stringValue = TagResolver.formatObject(getValueObject(currentRecord, name), application.getLocale(), application.getSettings());
		return processValue(stringValue, name, dataProviderLookup);
	}

	public static String processValue(String stringValue, String dataProviderID, IDataProviderLookup dataProviderLookup)
	{
		if (stringValue == null)
		{
			if ("selectedIndex".equals(dataProviderID) || isCountOrAvgOrSumAggregateDataProvider(dataProviderID, dataProviderLookup)) //$NON-NLS-1$
			{
				return "0"; //$NON-NLS-1$
			}
		}
		return stringValue;
	}

	/**
	 * Only meant to be used when a dataprovider value needs to be changed as a result of an operation other than display editing of this dataprovider. (for
	 * example upload media would need to change "m_filename" and "m_mimetype", but media field's display adapter only handles "m").
	 *
	 * @param dataProviderID
	 * @param obj
	 */
	public void setValueObject(String dataProviderID, Object obj)
	{
		try
		{
			setValueObject(currentRecord, getFormScope(), dataProviderID, obj);
		}
		catch (IllegalArgumentException ex)
		{
			Debug.trace(ex);
			getApplication().handleException(null, new ApplicationException(ServoyException.INVALID_INPUT, ex));
		}
	}

	// helper method; not static because needs form scope
	public Object getValueObject(IRecord record, String dataProviderId)
	{
		return getValueObject(record, getFormScope(), dataProviderId);
	}

	public static Object getValueObject(IRecord record, FormScope fs, String dataProviderID)
	{
		if (dataProviderID == null || fs == null) return null;

		Object value = null;
		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			try
			{
				GlobalScope gs = fs.getFormController().getApplication().getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft());
				value = gs == null ? null : gs.get(scope.getRight());
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (record != null)
		{
			value = record.getValue(dataProviderID);
		}
		if ((value == Scriptable.NOT_FOUND || value == null) && fs.has(dataProviderID, fs))
		{
			value = fs.get(dataProviderID);
		}
		return value;
	}

	public static Object setValueObject(IRecord record, FormScope fs, String dataProviderID, Object obj)
	{
		if (dataProviderID == null) return null;

		Pair<String, String> scope = ScopesUtils.getVariableScope(dataProviderID);
		if (scope.getLeft() != null)
		{
			try
			{
				if (record == null)
				{
					return fs.getFormController().getApplication().getScriptEngine().getScopesScope().getGlobalScope(scope.getLeft()).put(scope.getRight(),
						obj);
				}
				//does an additional fire in foundset!
				return record.getParentFoundSet().setDataProviderValue(dataProviderID, obj);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
		else if (fs.has(dataProviderID, fs))
		{
			return fs.put(dataProviderID, obj);
		}
		else if (record != null)
		{
			return record.setValue(dataProviderID, obj);
		}
		return null;
	}

	public static void setDataRendererComponentsRenderState(IDataRenderer dataRenderer, IRecordInternal rec)
	{
		if (rec != null)
		{
			Object[] recordStatus = null;

			if (dataRenderer.getOnRenderComponent().getRenderEventExecutor().hasRenderCallback())
			{
				recordStatus = getRecordIndexAndSelectStatus(rec);
				dataRenderer.getOnRenderComponent().getRenderEventExecutor().setRenderState(rec, ((Integer)recordStatus[0]).intValue(),
					((Boolean)recordStatus[1]).booleanValue(), true);
			}


			@SuppressWarnings("rawtypes")
			Iterator compIte = dataRenderer.getComponentIterator();
			Object comp;
			while (compIte.hasNext())
			{
				comp = compIte.next();
				if (comp instanceof IScriptableProvider)
				{
					IScriptable scriptable = ((IScriptableProvider)comp).getScriptObject();
					if (scriptable instanceof ISupportOnRenderCallback)
					{
						RenderEventExecutor rendererEventExecutor = ((ISupportOnRenderCallback)scriptable).getRenderEventExecutor();
						if (rendererEventExecutor != null && rendererEventExecutor.hasRenderCallback())
						{
							if (recordStatus == null) recordStatus = getRecordIndexAndSelectStatus(rec);
							rendererEventExecutor.setRenderState(rec, ((Integer)recordStatus[0]).intValue(), ((Boolean)recordStatus[1]).booleanValue(), true);
						}
					}
				}
			}
		}
	}

	private static Object[] getRecordIndexAndSelectStatus(IRecordInternal rec)
	{
		int index = -1;
		boolean isSelected = false;
		IFoundSetInternal parentFoundSet = rec.getParentFoundSet();
		if (parentFoundSet != null)
		{
			index = parentFoundSet.getRecordIndex(rec);
			if (parentFoundSet instanceof FoundSet)
			{
				int[] selectedIdxs = ((FoundSet)parentFoundSet).getSelectedIndexes();
				isSelected = Arrays.binarySearch(selectedIdxs, index) >= 0;
			}
			else
			{
				isSelected = parentFoundSet.getSelectedIndex() == index;
			}
		}

		return new Object[] { new Integer(index), new Boolean(isSelected) };
	}

	/**
	 * @return
	 */
	public boolean isDestroyed()
	{
		return destroyed;
	}
}
