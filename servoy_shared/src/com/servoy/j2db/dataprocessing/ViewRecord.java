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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 8.4
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ViewRecord", scriptingName = "ViewRecord")
public final class ViewRecord implements IRecordInternal, Scriptable
{
	private final Map<String, Object> values = new HashMap<>();
	private final List<IModificationListener> modificationListeners;
	private final IFoundSetInternal foundset;
	private final Object[] data;
	private final int pk;

	public ViewRecord(String[] columnNames, Object[] data, int pk, IFoundSetInternal foundset)
	{
		this.data = data;
		this.pk = pk;
		this.foundset = foundset;
		for (int i = 0; i < data.length; i++)
		{
			values.put(columnNames[i], data[i]);
		}
		this.modificationListeners = Collections.synchronizedList(new ArrayList<IModificationListener>(3));
	}

	@Override
	public boolean startEditing()
	{
		return false;
	}

	@Override
	public Object setValue(String dataProviderID, Object value)
	{
		return setValue(dataProviderID, value, false);
	}

	@Override
	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		Object managebleValue = Utils.mapToNullIfUnmanageble(value);
		Object prevValue = values.get(dataProviderID);
		values.put(dataProviderID, managebleValue);
		notifyChange(new ModificationEvent(dataProviderID, value, this), null);
		return prevValue;
	}

	@Override
	public Object getValue(String dataProviderID, boolean converted)
	{
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
		return values.containsKey(dataProviderID);
	}

	@Override
	public Object[] getPK()
	{
		return new Object[] { Integer.valueOf(pk) };
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

	@Override
	public Exception getException()
	{
		return null;
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
	public boolean startEditing(boolean mustFireEditRecordChange)
	{
		return false;
	}

	@Override
	public boolean isEditing()
	{
		return false;
	}

	@Override
	public int stopEditing()
	{
		return ISaveConstants.STOPPED;
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

	Object[] getData()
	{
		return data;
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
			return values.equals(((ViewRecord)obj).values);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	@Override
	public String toString()
	{
		return "ViewRecord[" + values + ']';
	}


	// Scriptable impementation
	private Scriptable prototype;
	private Scriptable parentScope;

	@Override
	public String getClassName()
	{
		return "ViewRecord";
	}

	@Override
	public Object get(String name, Scriptable start)
	{
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
}
