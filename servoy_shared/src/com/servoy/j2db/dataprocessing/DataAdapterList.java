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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.text.Document;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.ControllerUndoManager;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ServoyBeanState;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.dataui.IServoyAwareVisibilityBean;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.QueryAggregate;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDestroyable;
import com.servoy.j2db.util.ITagResolver;

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
	public DataAdapterList(IApplication app, IDataProviderLookup dataProviderLookup, Map<IPersist, ? extends Object> formObjects,
		FormController formController, LinkedHashMap<String, IDataAdapter> dataAdapters, ControllerUndoManager undoManager) throws RepositoryException
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
		Table table = null;
		if (dataProviderLookup != null)
		{
			table = dataProviderLookup.getTable();
		}
		//add
		Iterator<IPersist> it = formObjects.keySet().iterator();
		while (it.hasNext())
		{
			IPersist persist = it.next();
			Object obj = formObjects.get(persist);
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
				if (dataProviderID != null)
				{
					int idx = dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX) ? -1 : dataProviderID.lastIndexOf('.');
					if (idx != -1) //handle related fields
					{
						Relation[] relations = application.getFlattenedSolution().getRelationSequence(dataProviderID.substring(0, idx));
						boolean ok = relations != null;
						if (ok)
						{
							for (Relation relation : relations)
							{
								ok &= relation.isValid();
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

					da = this.dataAdapters.get(dataProviderID);
				}
				else
				{
					da = this.dataAdapters.get(null);
				}

				if (da == null)
				{
					da = new DisplaysAdapter(application, this, dataProviderID, display);
					this.dataAdapters.put(dataProviderID, da);
				}
				else if (da instanceof DisplaysAdapter)
				{
					((DisplaysAdapter)da).addDisplay(display);//for more than one field on form showing same content...
				}

				if (display.needEditListner() && da instanceof IEditListener)
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
			Iterator<Column> it44 = table.getColumns().iterator();
			while (it44.hasNext())
			{
				Column c = it44.next();
				ColumnInfo ci = c.getColumnInfo();
				if (ci != null && ci.getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
				{
					String lookup = ci.getLookupValue();
					if (lookup != null && lookup.length() != 0 && !lookup.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
					{
						int index = lookup.lastIndexOf('.');
						if (index != -1)
						{
							try
							{
								Relation[] relations = application.getFlattenedSolution().getRelationSequence(lookup.substring(0, index));
								if (relations != null && relations.length > 0)
								{
									// add a RelookupAdapter for the last relation
									Relation relation = relations[relations.length - 1];
									IDataProvider[] dps = relation.getPrimaryDataProviders(application.getFlattenedSolution());
									if (dps != null)
									{
										IDataAdapter la = this.dataAdapters.get("LA:" + lookup); //$NON-NLS-1$
										if (la == null)
										{
											StringBuffer prefix = new StringBuffer();
											for (int r = 0; r < relations.length - 1; r++)
											{
												prefix.append(relations[r].getName());
												prefix.append('.');
											}
											Set<String> dataProviderIDs = new HashSet<String>();
											for (IDataProvider dp : dps)
											{
												String dataProviderID;
												if (dp.getDataProviderID().startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
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
			formController.getFormScope().addModificationListener(this);
			application.getScriptEngine().getGlobalScope().addModificationListener(this);
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
				StringBuffer prefix = new StringBuffer();
				for (int r = 0; r < rel; r++)
				{
					prefix.append(relations[r].getName());
					prefix.append('.');
				}
				String dataProviderID;
				if (dp.getDataProviderID().startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
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
	 * Inform all dataAdapters about the new state
	 * 
	 * @param state the new state (can be null to delete old state)
	 * 
	 *            Note on param: stopAnyEdit,de renderer paint state 6 ddarna komt de editor en die edit state 6 ondertussen moet een andere rij gerepaint
	 *            worden dan zet de renderer dus state 7 er in en roept stop editing aan op 6!!! terwijl de editor nog aan het editen is
	 */
	public void setRecord(IRecordInternal state, boolean stopAnyEdit)
	{
		if (destroyed && state != null)
		{
			Debug.error("trying to set the record in a destroyed DataAdapterList of formcontroller: " + formController, new RuntimeException());
			return;
		}
		if (undoManager != null) undoManager.setIgnoreEdits(true);

		if (currentRecord != null)
		{
			// NEVER enable this ,values in the state could be changed: if (currentState.equals(state)) return;//same
			if (!currentRecord.equals(state))
			{
				stopUIEditing(false);
				currentRecord.removeModificationListener(this);//unregister
			}
		}

		//1) handle first related data (needed for comboboxes)
		if (state != null)
		{
			if (!state.equals(currentRecord))
			{
				state.addModificationListener(this);//register so we are notified about javascript changes on non global vars from here
			}
		}
		currentRecord = state;
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
			Debug.error("calling getFormScope on a destroyed DataAdapterList, formcontroller: " + formController, new RuntimeException());
			return null;
		}
		return formController.getFormScope();
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
		return isCountOrAvgOrSumAggregateDataProvider(dataAdapter.getDataProviderID());
	}

	private boolean isCountOrAvgOrSumAggregateDataProvider(String dataProvider)
	{
		try
		{
			if (dataProviderLookup == null)
			{
				return false;
			}
			IDataProvider dp = dataProviderLookup.getDataProvider(dataProvider);
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

		IExecutingEnviroment er = application.getScriptEngine();
		if (er != null)
		{
			SolutionScope ss = er.getSolutionScope();
			if (ss != null)
			{
				GlobalScope gs = ss.getGlobalScope();
				if (gs != null)
				{
					gs.removeModificationListener(this);
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

		currentRecord = null;
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
		String stringValue = TagResolver.formatObject(getValueObject(currentRecord, name), application.getSettings());
		if (stringValue == null)
		{
			if ("selectedIndex".equals(name) || isCountOrAvgOrSumAggregateDataProvider(name)) //$NON-NLS-1$
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
		setValueObject(currentRecord, getFormScope(), dataProviderID, obj);
	}

	// helper method; not static because needs form scope
	public Object getValueObject(IRecord record, String dataProviderId)
	{
		return getValueObject(record, getFormScope(), dataProviderId);
	}

	public static Object getValueObject(IRecord record, FormScope fs, String dataProviderID)
	{
		if (dataProviderID == null) return null;

		Object value = null;
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			try
			{
				String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
				GlobalScope gs = fs.getFormController().getApplication().getScriptEngine().getSolutionScope().getGlobalScope();
				value = gs.get(restName);
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
		if (value == Scriptable.NOT_FOUND && fs.has(dataProviderID, fs))
		{
			value = fs.get(dataProviderID);
		}
		return value;
	}

	public static Object setValueObject(IRecord record, FormScope fs, String dataProviderID, Object obj)
	{
		if (dataProviderID == null) return null;

		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			try
			{
				if (record == null)
				{
					String restName = dataProviderID.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length());
					GlobalScope gs = fs.getFormController().getApplication().getScriptEngine().getSolutionScope().getGlobalScope();
					return gs.put(restName, obj);
				}
				else
				{
					//does an additional fire in foundset!
					return record.getParentFoundSet().setDataProviderValue(dataProviderID, obj);
				}
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
			try
			{
				return record.setValue(dataProviderID, obj);
			}
			catch (IllegalArgumentException e)
			{
				Debug.trace(e);
			}
		}
		return null;
	}
}
