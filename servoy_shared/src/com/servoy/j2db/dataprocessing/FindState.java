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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.IRelation;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ISQLTableJoin;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * This class is passed as value by the JEditListModel(==FormModel) and represents 1 row
 *
 * @author jblok
 */
public class FindState implements Scriptable, IRecordInternal, Serializable, IJSRecord
{
	private final Map<String, Object> columndata;//actual find columndata
	private final IFoundSetInternal parent;
	private final Map<String, IFoundSetInternal> relatedStates;

	/**
	 * Constructor
	 */
	FindState(IFoundSetInternal parent)
	{
		this.parent = parent;

		columndata = new HashMap<String, Object>();
		relatedStates = new HashMap<String, IFoundSetInternal>();
	}

	List<Relation> getValidSearchRelations()
	{
		List<Relation> retval = new ArrayList<Relation>();

		try
		{
			Iterator<Relation> it = parent.getFoundSetManager().getApplication().getFlattenedSolution().getRelations(parent.getTable(), true, false);
			while (it.hasNext())
			{
				Relation element = it.next();
				if (element.isUsableInSearch())
				{
					retval.add(element);
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return retval;
	}

	/**
	 * Duplicate this findState.
	 */
	public FindState duplicate()
	{
		FindState dup = new FindState(parent);
		dup.columndata.putAll(columndata);
		dup.relatedStates.putAll(relatedStates);
		return dup;
	}

	public IFoundSetInternal getParentFoundSet()
	{
		return parent;
	}

	/**
	 * Store columns, aggregates and calculations in find state. Default find will ignore calculations and aggregates but onSearch method may use them in
	 * customized searches.
	 *
	 * @param dataProviderID
	 * @return
	 */
	private boolean storeDataProvider(String dataProviderID)
	{
		SQLSheet parentSheet = parent.getSQLSheet();
		return parentSheet.getColumnIndex(dataProviderID) != -1 || parentSheet.containsCalculation(dataProviderID) ||
			parentSheet.containsAggregate(dataProviderID);
	}


	/**
	 * called by data adapter for a new value (calcs MUST recalc)
	 *
	 * @param dataProviderID the data requested for
	 */
	public Object getValue(String dataProviderID)
	{
		return getValue(dataProviderID, true);
	}

	public Object getValue(String dataProviderID, boolean converted)
	{
		if (storeDataProvider(dataProviderID))
		{
			return columndata.get(dataProviderID);
		}

		if (ScopesUtils.isVariableScope(dataProviderID))
		{
			// Do return the global values, needed for global relations.
			return parent.getDataProviderValue(dataProviderID);
		}

		int index = dataProviderID.indexOf('.');
		if (index > 0)
		{
			String partName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);

			IFoundSetInternal foundSet = getRelatedFoundSet(partName);//check substate, will return null if not found
			if (foundSet != null)
			{
				IRecordInternal state = foundSet.getRecord(0);
				if (state != null)
				{
					return state.getValue(restName, converted);
				}
			}
			return null;
		}

		IFoundSetInternal fs = getRelatedFoundSet(dataProviderID);
		if (fs != null)
		{
			return fs;
		}
		return Scriptable.NOT_FOUND;
	}

	public Object setValue(String dataProviderID, Object value)
	{
		return setValue(dataProviderID, value, true);
	}

	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		Object oldValue = setValueImpl(dataProviderID, value);
		if (oldValue != value)//did change?
		{
			fireJSModificationEvent(dataProviderID, value);
		}
		return oldValue;
	}

	private Object setValueImpl(String dataProviderID, Object value)
	{
		if (storeDataProvider(dataProviderID))
		{
			return columndata.put(dataProviderID, Utils.mapToNullIfUnmanageble(value));
		}

		if (ScopesUtils.isVariableScope(dataProviderID))
		{
			// do set the global in the global scope so that globals just work in find mode for global relations
			return parent.setDataProviderValue(dataProviderID, value);
		}

		//check if is related value request
		int index = dataProviderID.indexOf('.');
		if (index > 0)
		{
			String partName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);
			IFoundSetInternal foundSet = getRelatedFoundSet(partName);//check substate, will return null if not found
			if (foundSet != null)
			{
				// set this related foundset in editing mode, so that the value are stored in db.
				Object oldVal = foundSet.setDataProviderValue(restName, value);
				if (oldVal != value)
				{
					IRecordInternal state = foundSet.getRecord(0);
					if (state != null) state.startEditing();
				}
				return oldVal;
			}
		}

		Debug.log("Ignoring unknown data provider '" + dataProviderID + "' in find mode"); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	public IFoundSetInternal getRelatedFoundSet(String name)
	{
		return getRelatedFoundSet(name, null);//only used for related fields, sort is irrelevant
	}

	private boolean isEditing = false;

	public boolean startEditing(boolean b)
	{
		isEditing = true;
		return isEditing;
	}

	public boolean startEditing()
	{
		return startEditing(true);
	}

	public int stopEditing()
	{
		isEditing = false;
		return ISaveConstants.STOPPED;
	}

	public boolean isEditing()
	{
		return isEditing;
	}

	public boolean isLocked()
	{
		return false;
	}

	public boolean isChanged()
	{
		return (columndata.size() != 0);
	}

	/*
	 * _____________________________________________________________ JavaScriptModificationListner
	 */

	private final List<IModificationListener> modificationListner = Collections.synchronizedList(new ArrayList<IModificationListener>(3));//only one possible on State

	public void addModificationListener(IModificationListener listner)
	{
		if (listner != null) modificationListner.add(listner);
	}

	public void removeModificationListener(IModificationListener listner)
	{
		if (listner != null) modificationListner.remove(listner);
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
		if (modificationListner.size() > 0)
		{
			ModificationEvent me = new ModificationEvent(name, value, this);
			fireJSModificationEvent(me);
		}
	}

	private void fireJSModificationEvent(ModificationEvent me)
	{
		// Test if this record is in edit state for stopping it below if nessesary
		boolean editState = this.isEditing();
		me.setRecord(this);
		Object[] array = modificationListner.toArray();
		for (Object element : array)
		{
			((IModificationListener)element).valueChanged(me);
		}
		// If it wasn't editting and now it is (see RelookupdAdapter modification) then stop it now so that every change
		// is recorded in one go and stored in one update
		if (!editState && isEditing())
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
	 * _____________________________________________________________ Scriptable implementation
	 */
	public void delete(int index)
	{
		Debug.trace("ignore State:delete " + index); //$NON-NLS-1$
	}

	public void delete(String name)
	{
		Debug.trace("ignore State:delete " + name); //$NON-NLS-1$
	}

	public Object get(int index, Scriptable start)
	{
		Debug.trace("ignore State:get " + index + " " + start); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	public Object get(String name, Scriptable start)
	{
		Object o = getValue(name);
		if (o == null && !storeDataProvider(name))
		{
			o = Scriptable.NOT_FOUND;
		}
		return o;
	}

	public String getClassName()
	{
		return "FindRecord"; //$NON-NLS-1$
	}

	public Object getDefaultValue(Class hint)
	{
		return toString();
	}

	public Object[] getIds()
	{
		SQLSheet parentSheet = parent.getSQLSheet();
		return parentSheet.getColumnNames();
	}

	public Scriptable getParentScope()
	{
		Debug.trace("ignore State:getParentScope"); //$NON-NLS-1$
		return null;
	}

	public Scriptable getPrototype()
	{
//	 	Debug.trace("ignore State:getPrototype");
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IRecord#has(java.lang.String)
	 */
	public boolean has(String dataprovider)
	{
		return true;
	}

	public boolean has(int index, Scriptable start)
	{
		Debug.trace("ignore State:has " + index + " " + start); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}

	public boolean has(String name, Scriptable start)
	{
		return true;//TODO: is this oke??
	}

	public boolean hasInstance(Scriptable instance)
	{
		Debug.trace("ignore State:hasInstance " + instance); //$NON-NLS-1$
		return false;
	}

	public void put(int index, Scriptable start, Object value)
	{
		Debug.trace("ignore State:put " + index + " " + start + " " + value); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void put(String name, Scriptable start, Object val)
	{
		Object value = val;
		if (value instanceof IDelegate)
		{
			value = ((IDelegate)value).getDelegate();
			if (value instanceof IDataSet)
			{
				IDataSet set = (IDataSet)value;
				StringBuffer sb = new StringBuffer();
				sb.append("\n"); //$NON-NLS-1$
				for (int i = 0; i < set.getRowCount(); i++)
				{
					sb.append(set.getRow(i)[0]);
					sb.append("\n"); //$NON-NLS-1$
				}
				value = sb.toString();
			}
		}
		else if (value instanceof FoundSet)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("\n"); //$NON-NLS-1$
			FoundSet fs = (FoundSet)value;
			for (int i = 0; i < fs.getSize(); i++)
			{
				IRecordInternal record = fs.getRecord(i);
				sb.append(record.getPKHashKey());
				sb.append("\n"); //$NON-NLS-1$
			}
			value = sb.toString();
		}
		else
		{
			Object tmp = value;
			while (tmp instanceof Wrapper)
			{
				tmp = ((Wrapper)tmp).unwrap();
				if (tmp == value)
				{
					break;
				}
			}
			value = tmp;
		}
		setValue(name, value);
	}

	public void setParentScope(Scriptable parent)
	{
		Debug.trace("ignore State:setParentScope " + parent); //$NON-NLS-1$
	}

	public void setPrototype(Scriptable prototype)
	{
		Debug.trace("ignore State:setPrototype " + prototype); //$NON-NLS-1$
	}

	/*
	 * _____________________________________________________________ Related states implementation
	 */

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		if (relationName == null || parent == null) return null;

		String partName = relationName;
		String restName = null;
		int index = relationName.indexOf('.');
		if (index > 0)
		{
			partName = relationName.substring(0, index);
			restName = relationName.substring(index + 1);
		}

		IFoundSetInternal rfs = relatedStates.get(partName);
		if (rfs == null)
		{
			Relation r = parent.getFoundSetManager().getApplication().getFlattenedSolution().getRelation(partName);
			if (r == null) return null; //safety
			try
			{
				if (r.isGlobal() || r.isParentRef())
				{
					rfs = parent.getRelatedFoundSet(this, partName, defaultSortColumns);
					// do not store in relatedStates because it is not a relatedfindfoundset
				}
				else
				{
					if (!getValidSearchRelations().contains(r))
					{
						String reason = "";
						if (r.isGlobal()) reason = "global relation";
						else if (r.isMultiServer()) reason = "multi server";
						else if (!r.isValid()) reason = "server/table not valid/loaded";
						else
						{
							reason = "relation primary datasource: " + r.getPrimaryDataSource() + " != findstate primary datasource: " + parent.getDataSource();
						}
						Debug.warn("Find: skip related search for '" + partName + "', relation cannot be used in search, because: " + reason);
						parent.getFoundSetManager().getApplication().reportJSError(
							Messages.getString("servoy.relation.find.unusable", new Object[] { partName }) + " (" + reason + ')', null); //$NON-NLS-2$
						return null;
					}
					SQLSheet sheet = parent.getSQLSheet().getRelatedSheet(
						((FoundSetManager)parent.getFoundSetManager()).getApplication().getFlattenedSolution().getRelation(partName),
						((FoundSetManager)parent.getFoundSetManager()).getSQLGenerator());
					rfs = ((FoundSetManager)parent.getFoundSetManager()).createRelatedFindFoundSet(this, partName, sheet);
					((FoundSet)rfs).addParent(this);
					((FoundSet)rfs).setFindMode();
					relatedStates.put(partName, rfs);
				}
			}
			catch (ServoyException ex)
			{
				Debug.error("Error making related findstate", ex); //$NON-NLS-1$
				return null;
			}
		}
		if (restName != null)
		{
			IRecordInternal record = rfs.getRecord(rfs.getSelectedIndex());
			if (record == null) return null;
			return record.getRelatedFoundSet(restName);
		}
		return rfs;
	}

	public boolean existInDataSource()
	{
		return true;//pretend to be stored, we never want to store this
	}

	@Deprecated
	public boolean existInDB()
	{
		return existInDataSource();
	}

	public String getAsTabSeparated()
	{
		return null;
	}

	private final HashMap<String, ParsedFormat> formats = new HashMap<String, ParsedFormat>();

	public void setFormat(String dataProviderID, ParsedFormat format)
	{
		if (format == null || format.getDisplayFormat() == null || ScopesUtils.isVariableScope(dataProviderID)) return;

		int index = dataProviderID.lastIndexOf('.');
		if (index > 0)
		{
			String partName = dataProviderID.substring(0, index);
			String restName = dataProviderID.substring(index + 1);
			IFoundSetInternal foundSet = getRelatedFoundSet(partName);//check substate, will return null if not found
			if (foundSet != null)
			{
				FindState state = (FindState)foundSet.getRecord(0);
				if (state != null)
				{
					state.setFormat(restName, format);
				}
			}
			return;
		}
		if (!formats.containsKey(dataProviderID))
		{
			formats.put(dataProviderID, format);
		}
	}

	public ParsedFormat getFormat(String dataProviderID)
	{
		return formats.get(dataProviderID);
	}

	public String getPKHashKey()
	{
		return ""; //$NON-NLS-1$
	}

	public Object[] getPK()
	{
		return null;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IState#flagExistInDB()
	 */
	public void flagExistInDB()
	{
	}

	/**
	 * @return nothing
	 */
	public Row getRawData()
	{
		return null;
	}

	public Map<String, Object> getColumnData()
	{
		return columndata;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IRowChangeListener#notifyChange(com.servoy.j2db.scripting.ModificationEvent)
	 */
	public void notifyChange(ModificationEvent e, FireCollector col)
	{
		//not needed here
	}


	/**
	 * Find all processable related find states and create joins. A find state is processable when it has changed or when a related find state has changed.
	 * @param sqlSelect
	 * @param relations path to this state
	 * @param selectTable
	 * @param provider
	 * @return
	 * @throws RepositoryException
	 */
	public List<RelatedFindState> createFindStateJoins(QuerySelect sqlSelect, List<IRelation> relations, BaseQueryTable selectTable, IGlobalValueEntry provider)
		throws RepositoryException
	{
		List<RelatedFindState> relatedFindStates = null;

		List<Relation> searchRelations = getValidSearchRelations();
		// find processable find states of related find states
		for (int i = 0; i < searchRelations.size(); i++)
		{
			Relation relation = searchRelations.get(i);
			if (relation != null)
			{
				IFoundSetInternal set = relatedStates.get(relation.getName());
				if (set != null && set.getSize() > 0)
				{
					ISQLTableJoin existingJoin = (ISQLTableJoin)sqlSelect.getJoin(selectTable, relation.getName());
					BaseQueryTable foreignQTable;
					if (existingJoin == null)
					{
						ITable foreignTable = relation.getForeignTable();
						foreignQTable = new QueryTable(foreignTable.getSQLName(), foreignTable.getDataSource(), foreignTable.getCatalog(),
							foreignTable.getSchema());
					}
					else
					{
						foreignQTable = existingJoin.getForeignTable();
					}

					FindState fs = (FindState)set.getRecord(0);
					List<IRelation> nextRelations = new ArrayList<IRelation>(relations);
					nextRelations.add(relation);
					List<RelatedFindState> rfs = fs.createFindStateJoins(sqlSelect, nextRelations, foreignQTable, provider);
					if (rfs != null && rfs.size() > 0)
					{
						// changed related findstate, add self with join
						if (relatedFindStates == null)
						{
							relatedFindStates = rfs;
						}
						else
						{
							relatedFindStates.addAll(rfs);
						}
						if (existingJoin == null)
						{
							sqlSelect.addJoin(SQLGenerator.createJoin(parent.getFoundSetManager().getApplication().getFlattenedSolution(), relation,
								selectTable, foreignQTable, provider));
						}
					}
				}
			}
		}

		// add yourself if you have changed or one or more related states has changed
		if (isChanged() || (relatedFindStates != null && relatedFindStates.size() > 0))
		{
			if (relatedFindStates == null)
			{
				relatedFindStates = new ArrayList<RelatedFindState>();
			}
			relatedFindStates.add(new RelatedFindState(this, relations, selectTable));
		}

		return relatedFindStates;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.awt.Component#toString()
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("FindRecord[COLUMS: {"); //$NON-NLS-1$
		Object[] objects = getIds();
		if (objects != null)
		{
			for (Object element : objects)
			{
				sb.append(element);
				sb.append(","); //$NON-NLS-1$
			}
		}
		sb.append("} DATA:"); //$NON-NLS-1$
		sb.append(columndata);
		sb.append(",  RELATED: "); //$NON-NLS-1$
		sb.append(relatedStates);
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	public boolean isRelatedFoundSetLoaded(String relationName, String restName)
	{
		return true;//return true to prevent async loading.
	}

	public IJSDataSet getChangedData()
	{
		return null;
	}

	public String getDataSource()
	{
		return parent.getDataSource();
	}

	public Exception getException()
	{
		return null;
	}

	public IJSFoundSet getFoundset()
	{
		return (IJSFoundSet)parent;
	}

	public Object[] getPKs()
	{
		return null;
	}

	public boolean hasChangedData()
	{
		return false;
	}

	public boolean isNew()
	{
		return false;
	}

	public void revertChanges()
	{

	}

	public void rowRemoved()
	{
	}

	/**
	 * @author rgansevles
	 *
	 */
	public static class RelatedFindState
	{
		private final FindState findState;
		private final BaseQueryTable primaryTable;
		private final List<IRelation> relations;

		/**
		 * @param findState
		 * @param relation
		 */
		public RelatedFindState(FindState findState, List<IRelation> relations, BaseQueryTable primaryTable)
		{
			this.findState = findState;
			this.relations = relations;
			this.primaryTable = primaryTable;
		}

		public FindState getFindState()
		{
			return findState;
		}

		public List<IRelation> getRelations()
		{
			return relations;
		}

		public BaseQueryTable getPrimaryTable()
		{
			return primaryTable;
		}
	}

}
