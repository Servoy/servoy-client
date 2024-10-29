/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.DefaultJavaScope;
import com.servoy.j2db.scripting.JSMenuItem;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.Utils;

/**
 * <pre data-puremarkdown>
`MenuItemRecord` represents a single menu item within a `MenuFoundSet`, allowing access to a record’s data and its parent `foundset`. This structure enables menu items to behave as records, facilitating component interactions that treat menu items as data entries.

For more information on working with menu datasources, refer to [MenuFoundSet](menufoundset.md).
 * </pre>
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "MenuItemRecord", scriptingName = "MenuItemRecord")
public class MenuItemRecord implements IRecordInternal, Scriptable
{
	public static final Map<String, NativeJavaMethod> jsFunctions = DefaultJavaScope.getJsFunctions(MenuItemRecord.class);
	public static final String MENUITEM_RECORD = "MenuItemRecord"; //$NON-NLS-1$
	public static final String MENUITEM_RELATION_NAME = "children_menu_items"; //$NON-NLS-1$

	private final Map<String, Object> values = new HashMap<>();
	private final MenuFoundSet foundset;
	private final JSMenuItem menuItem;
	private Object[] pk; // array of 1 string: the hash of all values

	private MenuFoundSet relatedFoundSet;
	private final List<IModificationListener> modificationListeners;

	private Scriptable prototype;
	private Scriptable parentScope;

	public MenuItemRecord(JSMenuItem menuItem, Map<String, Object> data, MenuFoundSet foundset)
	{
		this.foundset = foundset;
		this.menuItem = menuItem;
		this.values.putAll(data);
		this.modificationListeners = synchronizedList(new ArrayList<>(3));
	}

	@Override
	public boolean startEditing()
	{
		return false;
	}

	@Override
	public Object setValue(String dataProviderID, Object value)
	{
		return null;
	}

	@Override
	public Object getValue(String dataProviderID)
	{
		return getValue(dataProviderID, true);
	}

	@Override
	public boolean has(String dataProviderID)
	{
		if ("foundset".equals(dataProviderID) || jsFunctions.containsKey(dataProviderID)) return true; //$NON-NLS-1$
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

	@Override
	public boolean existInDataSource()
	{
		return true;
	}

	@Override
	public boolean isFlaggedForDeletion()
	{
		return false;
	}

	@Override
	public boolean isLocked()
	{
		return false;
	}

	@Override
	public void addModificationListener(IModificationListener listener)
	{
		if (listener != null) modificationListeners.add(listener);

	}

	@Override
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
	public String getClassName()
	{
		return "MenuItemRecord";
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
		//noop

	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{

	}

	@Override
	public void delete(String name)
	{
		//noop
	}

	@Override
	public void delete(int index)
	{

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.mozilla.javascript.Scriptable#getPrototype()
	 */
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

	@Override
	public Row getRawData()
	{
		return null;
	}

	@Override
	public boolean isRelatedFoundSetLoaded(String relationName, String restName)
	{
		return MENUITEM_RELATION_NAME.equals(relationName);
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
		if (relationName == null || foundset == null || !MENUITEM_RELATION_NAME.equals(relationName)) return null;

		if (relatedFoundSet == null)
		{
			relatedFoundSet = new MenuFoundSet(menuItem, MENUITEM_RELATION_NAME, getParentFoundSet().getFoundSetManager(), getParentFoundSet().getDataSource());
			getParentFoundSet().getFoundSetManager().registerRelatedMenuFoundSet(relatedFoundSet);
		}
		return relatedFoundSet;
	}

	@Override
	public IFoundSetInternal getParentFoundSet()
	{
		return foundset;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IRecordInternal#getValue(java.lang.String, boolean)
	 */
	@Override
	public Object getValue(String dataProviderID, boolean converted)
	{
		if ("foundset".equals(dataProviderID)) //$NON-NLS-1$
		{
			return foundset;
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
	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing)
	{
		return value;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IRecordInternal#setRecordMarkers(com.servoy.j2db.dataprocessing.JSRecordMarkers)
	 */
	@Override
	public void setRecordMarkers(JSRecordMarkers object)
	{

	}

	@Override
	public JSRecordMarkers getRecordMarkers()
	{
		return null;
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

	/**
	 * Returns the records datasource string (menu:name).
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

	@Override
	public Object getKey()
	{
		return getPK()[0];
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj != null && obj.getClass() == getClass())
		{
			return getKey().equals(((ViewRecord)obj).getKey());
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return getKey().hashCode();
	}

	@Override
	public String toString()
	{
		return "MenuItemRecord[" + values + ']';
	}
}
