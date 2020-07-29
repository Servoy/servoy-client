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
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewRecord", scriptingName = "ViewRecord")
public final class ViewRecord implements IRecordInternal, Scriptable
{
	public static final String VIEW_RECORD = "ViewRecord"; //$NON-NLS-1$

	private final Map<String, Object> values = new HashMap<>();
	private Object[] pk; // array of 1 string: the hash of all values
	private Map<String, Object> changes;
	private final List<IModificationListener> modificationListeners;
	private final ViewFoundSet foundset;
	private Exception lastException;

	public ViewRecord(String[] columnNames, Object[] data, ViewFoundSet foundset)
	{
		this.foundset = foundset;
		for (int i = 0; i < data.length; i++)
		{
			values.put(columnNames[i], data[i]);
		}
		this.modificationListeners = Collections.synchronizedList(new ArrayList<IModificationListener>(3));
		initJSFunctions(foundset != null ? foundset.getFoundSetManager().getApplication() : null);
	}

	private Map<String, NativeJavaMethod> jsFunctions;

	@SuppressWarnings("unchecked")
	private void initJSFunctions(IServiceProvider serviceProvider)
	{
		if (serviceProvider != null)
		{
			jsFunctions = (Map<String, NativeJavaMethod>)serviceProvider.getRuntimeProperties().get(IServiceProvider.RT_JSVIEWRECORD_FUNCTIONS);
		}
		if (jsFunctions == null)
		{
			jsFunctions = DefaultJavaScope.getJsFunctions(ViewRecord.class);
			if (serviceProvider != null)
			{
				serviceProvider.getRuntimeProperties().put(IServiceProvider.RT_JSVIEWRECORD_FUNCTIONS, jsFunctions);
			}
		}
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
			if (!changes.containsKey(dataProviderID))
			{
				changes.put(dataProviderID, prevValue);
			}
		}
		return prevValue;
	}

	private Object setValueImpl(String dataProviderID, Object value)
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

	@Override
	public boolean isEditing()
	{
		return changes != null && changes.size() > 0;
	}

	@Override
	public int stopEditing()
	{
		return foundset.save(this);
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
		return false;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns)
	{
		// TODO Auto-generated method stub
		return null;
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

	private JSValidationObject validateObject;

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
		setValue(name, value);
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
	 * var validationObject = record.validationObject;
	 *
	 * @return The last validtion object if the record was not validated.
	 */
	@JSGetter
	public JSValidationObject getValidationObject()
	{
		return validateObject;
	}

	/**
	 * Returns the validation object if there where validation failures for this record
	 * Can be set to null again if you checked the problems, will also be set to null when a save was succesful.
	 *
	 * @sample
	 * var validationObject = record.validationObject;
	 *
	 * @return The last validtion object if the record was not validated.
	 */
	@JSSetter
	public void setValidationObject(JSValidationObject object)
	{
		validateObject = object;
	}
}
