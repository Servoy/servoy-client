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


import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.ApplicationException;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.scripting.UsedDataProviderTracker.UsedAggregate;
import com.servoy.j2db.scripting.UsedDataProviderTracker.UsedDataProvider;
import com.servoy.j2db.scripting.UsedDataProviderTracker.UsedRelation;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * This class is passed as value by the JEditListModel(==FormModel) and represents 1 row
 * 
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSRecord")
public class Record implements Scriptable, IRecordInternal
{
	public static final String JS_RECORD = "JSRecord"; //$NON-NLS-1$

	/*
	 * _____________________________________________________________ JavaScript stuff
	 */
	private static Map<String, NativeJavaMethod> jsFunctions = new HashMap<String, NativeJavaMethod>();
	static
	{
		try
		{
			Method[] methods = Record.class.getMethods();
			for (Method m : methods)
			{
				if (m.getName().startsWith("js_")) //$NON-NLS-1$
				{
					String name = m.getName().substring(3);
					NativeJavaMethod nativeJavaMethod = jsFunctions.get(name);
					if (nativeJavaMethod == null)
					{
						nativeJavaMethod = new NativeJavaMethod(m, name);
					}
					else
					{
						nativeJavaMethod = new NativeJavaMethod(Utils.arrayAdd(nativeJavaMethod.getMethods(), new MemberBox(m), true), name);
					}
					jsFunctions.put(name, nativeJavaMethod);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public static final ThreadLocal<Boolean> VALIDATE_CALCS = new ThreadLocal<Boolean>();

	protected IFoundSetInternal parent;
	private Row row; //table row data (and calculations which is row related)
	//temp storage to make possible to stop edits on relatedFields, we do not cache/lookup here because the we can't flush substates globally (important for valuelists)
	private final Map<String, SoftReference<IFoundSetInternal>> relatedFoundSets;
	private final List<IModificationListener> modificationListeners;

	/**
	 * Constructor I
	 */
	public Record(IFoundSetInternal parent, Row r)
	{
		this(parent);
		if (r == null) throw new IllegalArgumentException(parent.getFoundSetManager().getApplication().getI18NMessage("servoy.record.error.nullRow")); //$NON-NLS-1$
		this.row = r;
		r.register(this);
	}

	/**
	 * Constructor II (used by substate)
	 */
	Record(IFoundSetInternal parent)
	{
		this.parent = parent;
		this.relatedFoundSets = new HashMap<String, SoftReference<IFoundSetInternal>>(3);
		this.modificationListeners = Collections.synchronizedList(new ArrayList<IModificationListener>(3));
	}

	void validateStoredCalculations()
	{
		if (VALIDATE_CALCS.get() != null) return;

		SQLSheet sheet = parent.getSQLSheet();
		List<String> storedCalcs = sheet.getStoredCalculationNames();
		row.getRowManager().flagRowCalcsForRecalculation(row.getPKHashKey(), storedCalcs);

		//recalc all stored calcs (requered due to use of plugin methods in calc)
		for (String calc : storedCalcs)
		{
			getValue(calc);
		}
	}

	public IFoundSetInternal getParentFoundSet()
	{
		return parent;
	}

	/**
	 * called by data adapter for a new value
	 * 
	 * @param dataProviderID the data requested for
	 * @param useCache, false if you want for sure the value recalculated if is calculation
	 */
	public Object getValue(String dataProviderID)
	{
		if (dataProviderID == null || parent == null) return null;

		if ("currentRecordIndex".equals(dataProviderID)) //$NON-NLS-1$
		{
			return new Integer(parent.getRecordIndex(this) + 1); //deprecated
		}
		if ("foundset".equals(dataProviderID)) //$NON-NLS-1$
		{
			return parent;
		}
		if ("exception".equals(dataProviderID)) //$NON-NLS-1$
		{
			return row.getLastException();
		}

		boolean containsCalc = row.containsCalculation(dataProviderID);
		boolean mustRecalc = containsCalc && row.mustRecalculate(dataProviderID, false);
		if ((containsCalc || row.containsDataprovider(dataProviderID)) && !mustRecalc)
		{
			return row.getValue(dataProviderID);//also stored calcs are always calculated ones(required due to use of plugin methods in calc);
		}
		if (containsCalc) //check if calculation 
		{
			UsedDataProviderTracker usedDataProviderTracker = new UsedDataProviderTracker(
				getParentFoundSet().getFoundSetManager().getApplication().getFlattenedSolution());
			Object value = parent.getCalculationValue(this, dataProviderID, null, usedDataProviderTracker);//do real calc
			if (!(value instanceof Undefined))
			{
				value = Utils.mapToNullIfUnmanageble(value);

				row.setValue(this, dataProviderID, value);
				// Reset the mustRecalculate here so if it is an every time changing calculation it will not be calculated again and again
				row.mustRecalculate(dataProviderID, false);
			}

			// re get it so that we do have the right type if the calc didn't return the type it specifies.
			// and that a converter is also applied.
			value = row.getValue(dataProviderID);
			manageCalcDependency(dataProviderID, usedDataProviderTracker);

			return value;
		}
		if (parent.containsDataProvider(dataProviderID)) //as shared (global or aggregate)
		{
			return parent.getDataProviderValue(dataProviderID);
		}
		if (dataProviderID.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
		{
			return Utils.mapToNullIfUnmanageble(parent.getDataProviderValue(dataProviderID));
		}
		int index = dataProviderID.lastIndexOf('.');
		if (index > 0) //check if is related value request
		{
			String partName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);

			if ("lazyMaxRecordIndex".equals(restName)) //$NON-NLS-1$
			{
				if (!isRelatedFoundSetLoaded(partName, restName))
				{
					return "?"; //$NON-NLS-1$
				}
				restName = "maxRecordIndex"; //$NON-NLS-1$
			}

			IFoundSetInternal foundSet = getRelatedFoundSet(partName);// partName may be multiple levels deep; check substate, will return null if not found
			if (foundSet != null)
			{
				//related data
				int selected = foundSet.getSelectedIndex();

				//in printing selected row will be set to -1, but if data is retrieved we need to use the first record again for use after printing...
				if (selected == -1 && foundSet.getSize() > 0) selected = 0;

				IRecordInternal state = foundSet.getRecord(selected);
				if (state != null)
				{
					return state.getValue(restName);
				}
				if (foundSet.containsDataProvider(restName))
				{
					return foundSet.getDataProviderValue(restName);
				}
			}
			return null;
		}

		if (parent.isValidRelation(dataProviderID))
		{
			return getRelatedFoundSet(dataProviderID, null);
		}
		return Scriptable.NOT_FOUND;
	}

	public Object setValue(String dataProviderID, Object value)
	{
		return setValue(dataProviderID, value, true);
	}

	/**
	 * called by dataadapter
	 * 
	 * @return oldvalue
	 */
	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		Object managebleValue = Utils.mapToNullIfUnmanageble(value);

		if (row.containsDataprovider(dataProviderID))
		{
			// when the value of a database column is set, the record must be in editing mode (not for unstored calcs)
			if (checkIsEditing && parent.getSQLSheet().getColumnIndex(dataProviderID) != -1 && !isEditing()) throw new IllegalStateException(
				"Record is not in edit, call startEditing() first"); //$NON-NLS-1$
			return row.setValue(this, dataProviderID, managebleValue);
		}
		else if (parent.containsDataProvider(dataProviderID)) //as shared (global or aggregate)
		{
			return parent.setDataProviderValue(dataProviderID, managebleValue);
		}

		//check if is related value request
		int index = dataProviderID.indexOf('.');
		if (index > 0)
		{
			String partName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);
			if (partName.equals(ScriptVariable.GLOBAL_PREFIX))
			{
				return parent.setDataProviderValue(dataProviderID, managebleValue);
			}
			IFoundSetInternal foundSet = getRelatedFoundSet(partName);//check substate, will return null if not found
			if (foundSet != null)
			{
				return foundSet.setDataProviderValue(restName, managebleValue);
			}
		}
		return null;
	}

	//called by DisplaysAdapter or CellAdapter
	public boolean startEditing()
	{
		return startEditing(true);
	}

	public boolean startEditing(boolean mustFireEditRecordChange)
	{
		return getParentFoundSet().getFoundSetManager().getEditRecordList().startEditing(this, mustFireEditRecordChange);
	}

	//called by DataAdapterList, return changed
	public int stopEditing()
	{
		return getParentFoundSet().getFoundSetManager().getEditRecordList().stopEditing(false, this);
	}

	public boolean isEditing()
	{
		return (parent != null ? parent.getFoundSetManager().getEditRecordList().isEditing(this) : false);
	}

	public boolean existInDataSource()
	{
		return row.existInDB();
	}

	@Deprecated
	public boolean existInDB()
	{
		return existInDataSource();
	}

	public boolean isLocked()
	{
		return false;
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationListner
	 */

	public void addModificationListener(IModificationListener listener)
	{
		if (listener != null) modificationListeners.add(listener);
	}

	public void removeModificationListener(IModificationListener listener)
	{
		if (listener != null) modificationListeners.remove(listener);
	}

	@Deprecated
	public void addModificationListner(IModificationListener l)
	{
		addModificationListener(l);
	}

	@Deprecated
	public void removeModificationListner(IModificationListener l)
	{
		removeModificationListener(l);
	}


	private void fireJSModificationEvent(String name, Object value)
	{
		if (modificationListeners.size() > 0)
		{
			fireJSModificationEvent(new ModificationEvent(name, value, this));
		}
	}

	private void fireJSModificationEvent(ModificationEvent me)
	{
		// Test if this record is in edit state for stopping it below if necessary
		boolean isEditting = parent != null ? parent.getFoundSetManager().getEditRecordList().isEditing() : false;
		me.setRecord(this);
		Object[] array = modificationListeners.toArray();
		for (Object element : array)
		{
			((IModificationListener)element).valueChanged(me);
		}
		// If it wasn't editing and now it is (see RelookupdAdapter modification) then stop it now so that every change
		// is recorded in one go and stored in one update
		if (!isEditting && isEditing())
		{
			try
			{
				this.stopEditing();
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	/*
	 * _____________________________________________________________ Scriptable impementation
	 */
	public void delete(int index)
	{
		// ignore
	}

	public void delete(String name)
	{
		// ignore
	}

	public Object get(int index, Scriptable start)
	{
		return Scriptable.NOT_FOUND;
	}

	public Object get(String name, Scriptable start)
	{
		if (FoundSet.isToplevelKeyword(name)) return Scriptable.NOT_FOUND;
		Object mobj = jsFunctions.get(name);
		if (mobj != null) return mobj;
		Object o = getValue(name);
		if (o instanceof Date)
		{
			o = new Date(((Date)o).getTime());//make copy so changes are seen (date is mutable and whould bypass equals)
		}
		else if (o instanceof DbIdentValue || o instanceof UUID)
		{
			o = new NativeJavaObject(this, o, ScriptObjectRegistry.getJavaMembers(o.getClass(), null));
		}
		return o;
	}

	public String getClassName()
	{
		return "Record"; //$NON-NLS-1$
	}

	public Object getDefaultValue(Class hint)
	{
		return toString();
	}

	public Object[] getIds()
	{
		List<String> al = new ArrayList<String>();
		if (parent != null)
		{
			String[] columns = parent.getSQLSheet().getColumnNames();
			for (String element : columns)
			{
				al.add(element);
			}
//			 columns = parent.getSQLSheet().getCalculationNames();
//			 for (int i = 0; i < columns.length; i++)
//			 {
//				 if(!al.contains(columns[i])) al.add(columns[i]);
//			 }
//			columns = parent.getSQLSheet().getAggregateNames();
//			for (String element : columns)
//			{
//				al.add(element);
//			}
		}
		al.addAll(jsFunctions.keySet());
		return al.toArray();
	}

	private Scriptable parentScope;

	public Scriptable getParentScope()
	{
		return parentScope;
	}

	private Scriptable prototypeScope;

	public Scriptable getPrototype()
	{
		return prototypeScope;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IRecord#has(java.lang.String)
	 */
	public boolean has(String dataprovider)
	{
		return has(dataprovider, this);
	}

	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	public boolean has(String name, Scriptable start)
	{
		if (name == null) return false;

		if ("foundset".equals(name) || "exception".equals(name) || jsFunctions.containsKey(name)) return true; //$NON-NLS-1$ //$NON-NLS-2$ 

		if (FoundSet.isToplevelKeyword(name)) return false;

		// TODO test for aggregates??
		int columnIndex = parent.getSQLSheet().getColumnIndex(name);
		if (columnIndex >= 0)
		{
			return true;
		}
		boolean b = (row == null ? false : row.containsCalculation(name));
		int index = 0;
		if (!b && (index = name.indexOf('.')) != -1)
		{
			String partName = name.substring(0, index);
			String restName = name.substring(index + 1);
			IFoundSetInternal foundSet = getRelatedFoundSet(partName);//check substate, will return null if not found
			if (foundSet != null)
			{
				//related data
				int selected = foundSet.getSelectedIndex();
				// in printing selected row will be -1, but aggregates should still go through record 0
				if (selected == -1 && foundSet.getSize() > 0)
				{
					selected = 0;
				}
				IRecordInternal state = foundSet.getRecord(selected);
				if (state != null)
				{
					return ((Scriptable)state).has(restName, start);
				}
			}
		}
		return b;
	}

	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

	public void put(int index, Scriptable start, Object value)
	{
		// ignore
	}

	public void put(String name, Scriptable start, final Object value)
	{
		try
		{
			if ("foundset".equals(name) || "exception".equals(name)) return; //$NON-NLS-1$ //$NON-NLS-2$
			if (jsFunctions.containsKey(name)) return;//dont allow to set 

			Object realValue = value;
			if (realValue instanceof IDelegate< ? >)
			{
				realValue = ((IDelegate< ? >)realValue).getDelegate();
				if (realValue instanceof IDataSet)
				{
					IDataSet set = (IDataSet)realValue;
					StringBuilder sb = new StringBuilder();
					sb.append('\n');
					for (int i = 0; i < set.getRowCount(); i++)
					{
						sb.append(set.getRow(i)[0]);
						sb.append('\n');
					}
					realValue = sb.toString();
				}
			}
			else if (realValue instanceof FoundSet)
			{
				StringBuilder sb = new StringBuilder();
				sb.append('\n');
				FoundSet fs = (FoundSet)realValue;
				for (int i = 0; i < fs.getSize(); i++)
				{
					IRecordInternal record = fs.getRecord(i);
					sb.append(record.getPKHashKey());
					sb.append('\n');
				}
				realValue = sb.toString();
			}
			else
			{
				Object tmp = realValue;
				while (tmp instanceof Wrapper)
				{
					tmp = ((Wrapper)tmp).unwrap();
					if (tmp == realValue)
					{
						break;
					}
				}
				realValue = tmp;
			}

			boolean dbColumn = parent.getSQLSheet().getColumnIndex(name) != -1;
			if (!dbColumn || startEditing()) //make sure any js change is noted
			{
				if (realValue instanceof Date)
				{
					//make copy so then when it is further used in js it won't change this one.
					realValue = new Date(((Date)realValue).getTime()); //make copy so changes are seen (date is mutable and whould bypass equals)
				}
				Object oldVal = setValue(name, realValue);
				if (oldVal != realValue)//did change?
				{
					fireJSModificationEvent(name, realValue);
				}
			}
			else
			{
				((FoundSetManager)parent.getFoundSetManager()).getApplication().handleException(null, new ApplicationException(ServoyException.RECORD_LOCKED));
			}
		}
		catch (RuntimeException e)
		{
			throw new WrappedException(e);
		}
	}

	public void setParentScope(Scriptable parent)
	{
		this.parentScope = parent;
	}

	public void setPrototype(Scriptable prototype)
	{
		this.prototypeScope = prototype;
	}

	/*
	 * _____________________________________________________________ Related states impementation
	 */

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		if (relationName == null || parent == null) return null;

		try
		{
			if (relationName.indexOf('.') < 0)
			{
				// one level deep relation name
				Relation relation = parent.getSQLSheet().getRelation(relationName, ((FoundSetManager)parent.getFoundSetManager()).getSQLGenerator());
				if (relation != null && relation.isGlobal())//only do handle global relations
				{
					return parent.getFoundSetManager().getGlobalRelatedFoundSet(relationName);
				}
			}

			// when relationName is multiple levels deep or a fk(null)->pk relation then the relation doesn't have to be there yet.
			IFoundSetInternal sub = parent.getRelatedFoundSet(this, relationName, defaultSortColumns);
			synchronized (relatedFoundSets)
			{
				if (sub != null)
				{
					relatedFoundSets.put(relationName, new SoftReference<IFoundSetInternal>(sub));
				}
				else
				{
					// relation was asked for but didn't return anything, don't ask for it again. (see isRelatedFoundsetLoaded())
					relatedFoundSets.put(relationName, null);
				}
				return sub;
			}
		}
		catch (ServoyException ex)
		{
			parent.getFoundSetManager().getApplication().reportError(parent.getFoundSetManager().getApplication().getI18NMessage("servoy.relation.error"), ex); //$NON-NLS-1$
			return null;
		}
	}

	public IFoundSetInternal getRelatedFoundSet(String name)
	{
		return getRelatedFoundSet(name, null);//only used for related fields, sort is irrelevant
	}

	public boolean isRelatedFoundSetLoaded(String relationName, String restName)
	{
		if (relatedFoundSets.size() > 0)
		{
			SoftReference<IFoundSetInternal> sr = null;
			synchronized (relatedFoundSets)
			{
				if (relatedFoundSets.containsKey(relationName))
				{
					sr = relatedFoundSets.get(relationName);
					if (sr == null)
					{
						// special case, relation was ask for but didn't return anything,
						// see getRelatedFoundSet()
						return true;
					}
				}
			}

			if (sr != null)
			{
				IFoundSetInternal fs = sr.get();
				if (fs instanceof RelatedFoundSet)
				{
					RelatedFoundSet rfs = (RelatedFoundSet)fs;
					return !rfs.mustQueryForUpdates() && !(rfs.mustAggregatesBeLoaded() && fs.getSQLSheet().containsAggregate(restName));
				}
			}
		}
		return ((FoundSetManager)parent.getFoundSetManager()).isRelatedFoundSetLoaded(this, relationName);
	}

	public Row getRawData()
	{
		return row;
	}

	public String getAsTabSeparated()
	{
		String leadingTabs = ""; //$NON-NLS-1$
		StringBuilder retval = new StringBuilder();
		SQLSheet.SQLDescription desc = parent.getSQLSheet().getSQLDescription(SQLSheet.SELECT);
		Iterator<String> it = desc.getDataProviderIDsDilivery().iterator();
		while (it.hasNext())
		{
			String pd = it.next();
			Object obj = row.getValue(pd);
			if (obj != null)
			{
				retval.append(obj.toString());
			}
			if (it.hasNext()) retval.append('\t');
			leadingTabs += '\t';
		}
		return retval.toString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Record)
		{
			Record rec = (Record)obj;
			return row == rec.row && parent == rec.parent;
		}
		return false;
	}

	@Override
	public String toString()
	{
		if (parent == null) return super.toString();

		String id = parent.getRecordToStringDataProviderID();
		if (id != null)
		{
			Object s = getValue(id);
			if (s == null) s = ""; //$NON-NLS-1$
			return s.toString();
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Record[DATA:"); //$NON-NLS-1$

		sb.append(row);
		sb.append(']');

		sb.append("  COLUMS: "); //$NON-NLS-1$
		Object[] objects = getIds();
		for (Object element : objects)
		{
			sb.append(element);
			sb.append(',');
		}
		return sb.toString();
	}

	public String getPKHashKey()
	{
		return row.getPKHashKey();
	}

	public Object[] getPK()
	{
		if (row != null)
		{
			// the pks might be generated by calculations with the same name; in this case, we must
			// perform the calculations and store them in the pks (with getValue()) before returning them
			String[] pks = parent.getSQLSheet().getPKColumnDataProvidersAsArray();
			for (String element : pks)
			{
				if (row.containsCalculation(element))
				{
					getValue(element); // calculate the pk value and store it in the row - let's hope the user knows what he is doing
					// because if he always returns other values for pks it will be chaos (+ caches out of synch)
					// parent.updatePk(Record state) could be made public and used here for this, but it is not normal
					// for pks to change the value all the time - so no use to add it and lower performance
				}
			}

			return row.getPK();
		}
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IRowChangeListener#notifyChange(com.servoy.j2db.scripting.ModificationEvent)
	 */
	public void notifyChange(ModificationEvent e, FireCollector collector)//this method is only called if I'm not the source of the event
	{
		fireJSModificationEvent(e);
		collector.put(this.getParentFoundSet(), this);
	}

	protected void manageCalcDependency(String calc, UsedDataProviderTracker usedDataProviderTracker)
	{
		if (usedDataProviderTracker == null)
		{
			return;
		}

		Set<String> usedGlobals = usedDataProviderTracker.getGlobals();
		if (usedGlobals != null)
		{
			for (String usedGlobal : usedGlobals)
			{
				row.getRowManager().addCalculationGlobalDependency(usedGlobal, calc);
			}
		}

		Set<UsedDataProvider> usedColumns = usedDataProviderTracker.getColumns();
		if (usedColumns != null)
		{
			for (UsedDataProvider usedColumn : usedColumns)
			{
				try
				{
					RowManager rowManager = ((FoundSetManager)parent.getFoundSetManager()).getRowManager(usedColumn.dataSource);
					if (rowManager != null)
					{
						rowManager.addCalculationDependency(usedColumn.pkHashKey, usedColumn.dataProviderId, parent.getDataSource(), getPKHashKey(), calc);
					}
				}
				catch (ServoyException e)
				{
					Debug.error(e);
				}
			}
		}

		Set<UsedRelation> uedRelations = usedDataProviderTracker.getRelations();
		if (uedRelations != null)
		{
			for (UsedRelation usedRelation : uedRelations)
			{
				row.getRowManager().addCalculationRelationDependency(usedRelation.whereArgsHash, usedRelation.name, parent.getDataSource(), getPKHashKey(),
					calc);
			}
		}

		Set<UsedAggregate> usedAggregates = usedDataProviderTracker.getAggregates();
		if (usedAggregates != null)
		{
			for (UsedAggregate usedAggregate : usedAggregates)
			{
				row.getRowManager().addCalculationAggregateDependency(usedAggregate.foundSet, usedAggregate.name, calc);
			}
		}
	}

	/**
	 * Returns true or false if the record is being edited or not.
	 *
	 * @sample 
	 * var isEditing = foundset.getSelectedRecord().isEditing() // also foundset.getRecord can be used
	 * 
	 * @return a boolean when in edit.
	 */
	public boolean js_isEditing()
	{
		return isEditing();
	}

	/**
	 * Returns an array with the primary key values of the record.
	 *
	 * @sample 
	 * var pks = foundset.getSelectedRecord().getPKs() // also foundset.getRecord can be used
	 * 
	 * @return an Array with the pk values.
	 */
	public Object[] js_getPKs()
	{
		return getPK();
	}

	/**
	 * Delete this record from the Foundset and the underlying datasource.
	 *
	 * @sample
	 * var record= %%prefix%%foundset.getRecord(index);
	 * record.deleteRecord();
	 * @deprecated
	 */
	@Deprecated
	public void js_deleteRecord()
	{
		try
		{
			getParentFoundSet().deleteRecord(this);
		}
		catch (ServoyException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * If this record exists in underlying datasource it will do a re-query to fetch the latest data from the datasource.
	 * NOTE: If you use transactions then it will be the data of your last update of this record in the transaction,
	 * not the latest committed data of that record in the datasource. 
	 * 
	 * @sample
	 * var record= %%prefix%%foundset.getSelectedRecord();
	 * record.rollbackChanges();
	 */
	public void js_rollbackChanges()
	{
		try
		{
			getRawData().rollbackFromDB();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Saves this record to the datasource if it had changes.
	 * 
	 * @sample
	 * var record= %%prefix%%foundset.getSelectedRecord();
	 * record.save();
	 * 
	 * @return true if the save was done without an error.
	 * @deprecated
	 */
	@Deprecated
	public boolean js_save()
	{
		try
		{
			return getParentFoundSet().getFoundSetManager().getEditRecordList().stopEditing(true, this) == ISaveConstants.STOPPED;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a JSDataSet with outstanding (not saved) changed data of this record.
	 * 
	 * NOTE: To return an array of records with outstanding changed data, see the function databaseManager.getEditedRecords(). 
	 *
	 * @sample
	 * var dataset = record.getChangedData()
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	application.output(dataset.getValue(i,1) +' '+ dataset.getValue(i,2) +' '+ dataset.getValue(i,3));
	 * }
	 * 
	 * @return a JSDataSet with the changed data of this record.
	 */
	public JSDataSet js_getChangedData()
	{
		if (getParentFoundSet() != null && getRawData() != null)
		{
			String[] cnames = getParentFoundSet().getSQLSheet().getColumnNames();
			Object[] oldd = getRawData().getRawOldColumnData();
			List<Object[]> rows = new ArrayList<Object[]>();
			if (oldd != null || !getRawData().existInDB())
			{
				Object[] newd = getRawData().getRawColumnData();
				for (int i = 0; i < cnames.length; i++)
				{
					Object oldv = (oldd == null ? null : oldd[i]);
					if (!Utils.equalObjects(oldv, newd[i])) rows.add(new Object[] { cnames[i], oldv, newd[i] });
				}
			}
			return new JSDataSet(getParentFoundSet().getFoundSetManager().getApplication(), new BufferedDataSet(
				new String[] { "col_name", "old_value", "new_value" }, rows)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return null;
	}

	/**
	 * Returns last occurred exception on this record (or null).
	 *
	 * @sample
	 * var exception = record.exception;
	 * 
	 * @return The occurred exception.
	 */
	public Exception js_getException()
	{
		return row.getLastException();
	}

	public void js_setException(Exception ex)
	{
		//ignore
	}

	/**
	 * Returns parent foundset of the record.
	 *
	 * @sample
	 * var parent = record.foundset;
	 * 
	 * @return The parent foundset of the record.
	 */
	public IFoundSetInternal js_getFoundset()
	{
		return parent;
	}

	public void js_setFoundset(IFoundSetInternal foundset)
	{
		//ignore
	}
}