/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import static com.servoy.j2db.dataprocessing.FireCollector.getFireCollector;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewRecord", scriptingName = "ViewRecord")
public final class ViewRecord implements IRecordInternal, Scriptable
{
	public static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(ViewRecord.class);

	public static final String VIEW_RECORD = "ViewRecord"; //$NON-NLS-1$

	private final Map<String, Object> values = new HashMap<>();
	private Object[] pk; // array of 1 string: the hash of all values
	private Map<String, Object> changes;
	private final List<IModificationListener> modificationListeners;
	private final ViewFoundSet foundset;
	private Exception lastException;
	private final Map<String, SoftReference<IFoundSetInternal>> relatedFoundSets;

	public ViewRecord(String[] columnNames, Object[] data, ViewFoundSet foundset)
	{
		this.foundset = foundset;
		for (int i = 0; i < data.length; i++)
		{
			if (i < columnNames.length)
			{
				values.put(columnNames[i], data[i]);
			}
			else
			{
				Debug.error("Error creating ViewRecord, column names length does not match the data length: " + Utils.getScriptableString(columnNames) + " , " +
					Utils.getScriptableString(data));
				break;
			}
		}
		this.modificationListeners = Collections.synchronizedList(new ArrayList<IModificationListener>(3));
		this.relatedFoundSets = new HashMap<String, SoftReference<IFoundSetInternal>>(3);
	}

	@Override
	public Object setValue(String dataProviderID, Object value)
	{
		return setValue(dataProviderID, value, false);
	}

	@Override
	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		Object prevValue = setValueImpl(dataProviderID, value);
		if (prevValue != value)
		{
			// it really did change register this to the changes:
			if (changes == null)
			{
				changes = new HashMap<String, Object>(3);
				foundset.addEditedRecord(this);
			}
			else if (foundset.isFailedRecord(this))
			{
				lastException = null;
				foundset.addEditedRecord(this);
			}
			if (!changes.containsKey(dataProviderID))
			{
				changes.put(dataProviderID, prevValue);
			}
		}
		return prevValue;
	}

	Object setValueImpl(String dataProviderID, Object value)
	{
		Object managebleValue = Utils.mapToNullIfUnmanageble(value);
		Object prevValue = values.get(dataProviderID);
		if (!Utils.equalObjects(managebleValue, prevValue))
		{
			values.put(dataProviderID, managebleValue);
			try (FireCollector fireCollector = getFireCollector())
			{
				notifyChange(new ModificationEvent(dataProviderID, value, this), fireCollector);
			}
			return prevValue;
		}
		return value;
	}

	public Object getOldVaue(String dataProviderID)
	{
		if (changes != null && changes.containsKey(dataProviderID))
		{
			return changes.get(dataProviderID);
		}
		return values.get(dataProviderID);
	}

	@Override
	public Object getValue(String dataProviderID, boolean converted)
	{
		if ("foundset".equals(dataProviderID)) //$NON-NLS-1$
		{
			return foundset;
		}
		if ("exception".equals(dataProviderID)) //$NON-NLS-1$
		{
			return lastException;
		}
		if (values.containsKey(dataProviderID)) return values.get(dataProviderID);
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

		// this relaxes the way related foundsets are loaded through a ViewRecord;
		// if there is a hit then we just assume it is fine, only when nothing is returned we check it using ".isValidRelation()";
		// we will test if this is a valid relation for this viewrecord simply by getting it to avoid the warning that isValidRelation generates.
		// this is because the WHERE arguments could be fine for this viewrecord for that relation,
		// because this could be a just a quick ViewFoundset (filtering etc.) that replaces an actual Foundset but it's based on the same datasource columns
		// (So same whereargs) and that datasource is compatible with the relation
		IFoundSetInternal rfs = getRelatedFoundSet(dataProviderID);
		if (rfs == null) foundset.isValidRelation(dataProviderID);
		else return rfs;

		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object getValue(String dataProviderID)
	{
		return getValue(dataProviderID, true);
	}

	@Override
	public boolean has(String dataProviderID)
	{
		if ("foundset".equals(dataProviderID) || "exception".equals(dataProviderID) || jsFunctions.containsKey(dataProviderID)) return true; //$NON-NLS-1$ //$NON-NLS-2$
		return values.containsKey(dataProviderID);
	}

	@Override
	public Object[] getPK()
	{
		if (pk == null)
		{
			pk = new Object[] { Utils.calculateMD5HashBase64(RowManager.createPKHashKey(values.values().toArray())) };
		}
		return pk;
	}

	/**
	 * Returns true if the current record is a new record or false otherwise. New record means not saved to database.
	 * Because this record is part of a view foundset, this method will always return false.
	 *
	 * @sample
	 * var isNew = viewFoundset.getSelectedRecord().isNew();
	 *
	 * @return true if the current record is a new record, false otherwise;
	 */
	@JSFunction
	public boolean isNew()
	{
		return getRawData() != null && !existInDataSource();
	}

	/**
	 * Returns an array with the primary key values of the record.
	 *
	 * @sample
	 * var pks = foundset.getSelectedRecord().getPKs() // also foundset.getRecord can be used
	 *
	 * @return an Array with the pk values.
	 */
	@JSFunction
	public Object[] getPKs()
	{
		return getPK();
	}

	@Override
	public boolean existInDataSource()
	{
		return true;
	}

	@Override
	public boolean isLocked()
	{
		return false;
	}

	public void addModificationListener(IModificationListener listener)
	{
		if (listener != null) modificationListeners.add(listener);
	}

	public void removeModificationListener(IModificationListener listener)
	{
		if (listener != null) modificationListeners.remove(listener);
	}


	/**
	 * Returns last occurred exception on this record (or null).
	 *
	 * @sample
	 * var exception = record.exception;
	 *
	 * @return The occurred exception.
	 */
	@JSReadonlyProperty
	@Override
	public Exception getException()
	{
		return lastException;
	}

	/**
	 * Returns the records datasource string.
	 *
	 * @sample
	 * var ds = record.getDataSource();
	 *
	 * @return The datasource string of this record.
	 */
	@JSFunction
	public String getDataSource()
	{
		return foundset.getDataSource();
	}

	/**
	 * Returns parent foundset of the record.
	 *
	 * @sample
	 * var parent = record.foundset;
	 *
	 * @return The parent foundset of the record.
	 */
	@JSReadonlyProperty
	public IJSFoundSet getFoundset()
	{
		return (IJSFoundSet)foundset;
	}

	@Override
	public boolean existInDB()
	{
		return true;
	}

	@Override
	public void addModificationListner(IModificationListener l)
	{
		addModificationListener(l);
	}

	@Override
	public void removeModificationListner(IModificationListener l)
	{
		removeModificationListener(l);
	}

	@Override
	public void notifyChange(ModificationEvent e, FireCollector collector)
	{
		e.setRecord(this);
		Object[] array = modificationListeners.toArray();
		for (Object element : array)
		{
			((IModificationListener)element).valueChanged(e);
		}
		collector.put(this.getParentFoundSet(), this, e.getName());
	}


	@Override
	public boolean startEditing()
	{
		return startEditing(false);
	}

	@Override
	public boolean startEditing(boolean mustFireEditRecordChange)
	{
		return true;
	}

	/**
	 * Returns true or false if the related foundset is already loaded. Will not load the related foundset.
	 *
	 * @sample
	 * var isLoaded = viewfoundset.getSelectedRecord().isRelatedFoundSetLoaded(relationName)
	 *
	 * @param relationName name of the relation to check for
	 *
	 * @return true if related foundset is loaded.
	 */
	@JSFunction
	public boolean isRelatedFoundSetLoaded(String relationName)
	{
		return isRelatedFoundSetLoaded(relationName, null);
	}

	/**
	 * Returns true or false if the record has changes or not.
	 *
	 * As opposed to isEditing() of regular records, this method actually returns whether there are unsaved changes
	 * on this record, since there is no edit mode for view records.
	 *
	 * @return true if unsaved changes are detected.
	 */
	@Override
	@JSFunction
	public boolean isEditing()
	{
		return changes != null && changes.size() > 0;
	}

	@Override
	public int stopEditing()
	{
		return foundset.doSave(this);
	}

	@Override
	public void rowRemoved()
	{
	}

	@Override
	public Row getRawData()
	{
		// TODO Auto-generated method stub
		return null;
	}

	void updateValues(String[] columnNames, Object[] data)
	{
		try (FireCollector fireCollector = getFireCollector())
		{
			for (int i = columnNames.length; --i >= 0;)
			{
				setValueImpl(columnNames[i], data[i]);
			}
			pk = null;
		}
	}

	@Override
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
		return ((FoundSetManager)foundset.getFoundSetManager()).isRelatedFoundSetLoaded(this, relationName);
	}

	@Override
	public String getPKHashKey()
	{
		return RowManager.createPKHashKey(getPK());
	}

	@Override
	public String getAsTabSeparated()
	{
		return null;
	}

	@Override
	public IFoundSetInternal getRelatedFoundSet(String relationName)
	{
		return getRelatedFoundSet(relationName, null);
	}

	@Override
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		if (relationName == null || foundset == null) return null;

		try
		{
			if (relationName.indexOf('.') < 0)
			{
				// one level deep relation name
				Relation relation = foundset.getFoundSetManager().getApplication().getFlattenedSolution().getRelation(relationName);
				if (relation != null && relation.isGlobal())//only do handle global relations
				{
					return foundset.getFoundSetManager().getGlobalRelatedFoundSet(relationName);
				}
			}

			// when relationName is multiple levels deep or a fk(null)->pk relation then the relation doesn't have to be there yet.
			IFoundSetInternal sub = foundset.getRelatedFoundSet(this, relationName, defaultSortColumns);
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
			foundset.getFoundSetManager().getApplication().reportError(foundset.getFoundSetManager().getApplication().getI18NMessage("servoy.relation.error"), //$NON-NLS-1$
				ex);
			return null;
		}
	}

	@Override
	public IFoundSetInternal getParentFoundSet()
	{
		return foundset;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj != null && obj.getClass() == getClass())
		{
			return getPK()[0].equals(((ViewRecord)obj).getPK()[0]);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return getPK()[0].hashCode();
	}

	@Override
	public String toString()
	{
		return "ViewRecord[" + values + ']';
	}


	// Scriptable impementation
	private Scriptable prototype;
	private Scriptable parentScope;

	private JSRecordMarkers recordMarkers;

	@Override
	public String getClassName()
	{
		return "ViewRecord";
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		Object mobj = jsFunctions.get(name);
		if (mobj != null)
		{
			ScriptRuntime.setFunctionProtoAndParent((BaseFunction)mobj, start);
			return mobj;
		}
		Object o = getValue(name);
		if (o != null && o != Scriptable.NOT_FOUND && !(o instanceof Scriptable))
		{
			Context context = Context.getCurrentContext();
			if (context != null) o = context.getWrapFactory().wrap(context, start, o, o.getClass());
		}
		return o;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return Scriptable.NOT_FOUND;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		return values.containsKey(name);
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
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
		setValue(name, tmp);
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
	}

	@Override
	public void delete(String name)
	{
		values.remove(name);
		notifyChange(new ModificationEvent(name, null, this), null);
	}

	@Override
	public void delete(int index)
	{
	}

	@Override
	public Scriptable getPrototype()
	{
		return prototype;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototype = prototype;
	}

	@Override
	public Scriptable getParentScope()
	{
		if (parentScope == null)
		{
			return foundset.getFoundSetManager().getApplication().getScriptEngine().getSolutionScope().getParentScope();
		}
		return parentScope;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		this.parentScope = parent;
	}

	@Override
	public Object[] getIds()
	{
		return values.keySet().toArray();
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return toString();
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

	Map<String, Object> getChanges()
	{
		return changes;
	}

	ViewRecord revertChangesImpl()
	{
		if (changes != null)
		{
			changes.forEach((key, value) -> values.put(key, value));
		}
		clearChanges();
		return this;
	}

	void clearChanges()
	{
		changes = null;
	}

	/**
	 * Returns a JSDataSet with outstanding (not saved) changed data of this record.
	 * column1 is the column name, colum2 is the old data and column3 is the new data.
	 *
	 * NOTE: To return an array of records with outstanding changed data, see the function foundset.getEditedRecords().
	 *
	 * @sample
	 * /** @type {JSDataSet} *&#47;
	 * var dataset = record.getChangedData()
	 * for( var i = 1 ; i <= dataset.getMaxRowIndex() ; i++ )
	 * {
	 * 	application.output(dataset.getValue(i,1) +' '+ dataset.getValue(i,2) +' '+ dataset.getValue(i,3));
	 * }
	 *
	 * @return a JSDataSet with the changed data of this record.
	 */
	@JSFunction
	public IJSDataSet getChangedData()
	{
		List<Object[]> rows = new ArrayList<Object[]>();
		if (changes != null)
		{
			changes.forEach((key, value) -> {
				if (value != null && !Utils.equalObjects(value, values.get(key)))
				{
					rows.add(new Object[] { key, value, values.get(key) });
				}
			});
			return new JSDataSet(getParentFoundSet().getFoundSetManager().getApplication(),
				new BufferedDataSet(new String[] { "col_name", "old_value", "new_value" }, rows)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return new JSDataSet(getParentFoundSet().getFoundSetManager().getApplication(),
			new BufferedDataSet(new String[] { "col_name", "old_value", "new_value" }, rows));
	}

	/**
	 * Returns true if the current record has outstanding/changed data.
	 *
	 * @sample
	 * var hasChanged = record.hasChangedData();
	 *
	 * @return true if the current record has outstanding/changed data.
	 */
	@JSFunction
	public boolean hasChangedData()
	{
		return changes != null;
	}

	/**
	 * Creates and returns a new validation object for this record, which allows for markers to be used outside the validation flow.
	 * Will overwrite the current markers if present.
	 * Can be set to null again if you checked the problems, will also be set to null when a save was successful.
	 *
	 * @sample var recordMarkers = record.createMarkers();
	 *
	 * @return A new validation object.
	 */
	@JSFunction
	public JSRecordMarkers createMarkers()
	{
		this.recordMarkers = new JSRecordMarkers(this, this.foundset.getFoundSetManager().getApplication());
		return this.recordMarkers;
	}

	/**
	 * Reverts the in memory outstanding (not saved) changes of the record.
	 *
	 * @sample
	 * var record= %%prefix%%foundset.getSelectedRecord();
	 * record.revertChanges();
	 */
	@JSFunction
	public void revertChanges()
	{
		try
		{
			// this does a call back to revertChangesImpl
			foundset.revertEditedRecords(new ViewRecord[] { this });
		}
		catch (Exception e)
		{
			setLastException(e);
			throw new RuntimeException(e);
		}
	}

	public void setLastException(Exception ex)
	{
		lastException = ex;
	}

	/**
	 * Returns the validation object if there where validation failures for this record
	 * Can be set to null again if you checked the problems, will also be set to null when a save was succesful.
	 *
	 * @sample
	 * var recordMarkers = record.recordMarkers;
	 *
	 * @return The last validtion object if the record was not validated.
	 */
	@JSGetter
	public JSRecordMarkers getRecordMarkers()
	{
		return recordMarkers;
	}

	/**
	 * Returns the validation object if there where validation failures for this record
	 * Can be set to null again if you checked the problems, will also be set to null when a save was succesful.
	 *
	 * @sample
	 * var recordMarkers = record.recordMarkers;
	 *
	 * @return The last validtion object if the record was not validated.
	 */
	@JSSetter
	public void setRecordMarkers(JSRecordMarkers object)
	{
		recordMarkers = object == null ? null : object.getRecord() == this ? object : null;
	}
}
