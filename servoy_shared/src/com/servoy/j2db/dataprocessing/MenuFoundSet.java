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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Symbol;
import org.mozilla.javascript.SymbolKey;
import org.mozilla.javascript.SymbolScriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.ISQLSelect;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.querybuilder.IQueryBuilder;
import com.servoy.j2db.scripting.JSMenu;
import com.servoy.j2db.scripting.JSMenuItem;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.AlwaysRowSelectedSelectionModel;

/**
 * <p><code>MenuFoundSet</code> enables a menu structure to function as a datasource, allowing menu items to be treated as records
 * within the Servoy Developer environment. This provides access to menu properties as dataproviders, which are read-only, and
 * supports hierarchical relationships, such as parent-child structures based on <code>parentid</code>.
 * These capabilities allow components like <a href="../../../servoyextensions/ui-components/visualization/dbtreeview.md">DBTreeView</a>
 * to work seamlessly with menu records and enable complex menu representations with FormComponents.</p>
 *
 * <h3>Example Usage</h3>
 * <pre>
 * elements.myDbtreeview.addRoots(datasources.menu.treemenu.getFoundSet());
 * elements.myDbtreeview.setTextDataprovider(datasources.menu.treemenu.getDataSource(), 'menuText');
 * elements.myDbtreeview.setNRelationName(
 *   datasources.menu.treemenu.getDataSource(),
 *   datasources.menu.treemenu.getParentToChildrenRelationName()
 * );
 * </pre>
 *
 * <p>For further details on setting up and working with datasources, see
 * <a href="../datasources/jsdatasource.md">Datasource Setup</a>.</p>
 *
 * @author lvostinar
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "MenuFoundSet", scriptingName = "MenuFoundSet")
public class MenuFoundSet extends AbstractTableModel implements ISwingFoundSet, IFoundSetScriptBaseMethods, SymbolScriptable
{
	public static final String MENU_FOUNDSET = "MenuFoundSet";

	private static Callable symbol_iterator = (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
		return new IterableES6Iterator(scope, ((MenuFoundSet)thisObj));
	};

	private final IFoundSetManagerInternal manager;
	private final String datasource;
	private final List<MenuItemRecord> records = new ArrayList<>();
	private String relationName;

	private int foundsetID = 0;
	private final List<IFoundSetEventListener> foundSetEventListeners = new ArrayList<>(3);
	private transient AlwaysRowSelectedSelectionModel selectionModel;
	private transient TableAndListEventDelegate tableAndListEventDelegate;
	// forms might force their foundset to remain at a certain multiselect value
	// if a form 'pinned' multiselect, multiSelect should not be changeable by foundset JS access
	// if more then 1 form wishes to pin multiselect at a time, the form with lowest elementid wins
	private String multiSelectPinnedForm = null;
	private int multiSelectPinLevel;

	public MenuFoundSet(JSMenu menu, IFoundSetManagerInternal manager)
	{
		this.datasource = DataSourceUtils.createMenuDataSource(menu.getName());
		//listen to changes?
		//menu.addChangeListener();
		this.manager = manager;
		createSelectionModel();
		createRecords(menu);
		setSelectedIndex(0);
	}

	public MenuFoundSet(JSMenuItem menuItem, String relationName, IFoundSetManagerInternal manager, String datasource)
	{
		this.datasource = datasource;
		this.relationName = relationName;
		//listen to changes?
		//menu.addChangeListener();
		this.manager = manager;
		createSelectionModel();
		createRecords(menuItem);
		setSelectedIndex(0);
	}

	@Override
	public SQLSheet getSQLSheet()
	{
		return null;
	}

	@Override
	public PrototypeState getPrototypeState()
	{
		return new PrototypeState(this);
	}

	@Override
	public void addParent(IRecordInternal record)
	{
		// TODO?

	}

	@Override
	public void fireAggregateChangeWithEvents(IRecordInternal record)
	{

	}

	@Override
	public boolean isValidRelation(String name)
	{
		return true;
	}

	@Override
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal record, String relationName, List<SortColumn> defaultSortColumns) throws ServoyException
	{
		// TODO?
		return null;
	}

	@Override
	public Object getCalculationValue(IRecordInternal record, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker)
	{
		return null;
	}

	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException
	{

	}

	@Override
	public void sort(Comparator<Object[]> recordPKComparator)
	{

	}

	@Override
	public List<SortColumn> getSortColumns()
	{
		return null;
	}

	@Override
	public IFoundSetManagerInternal getFoundSetManager()
	{
		return manager;
	}

	@Override
	public ITable getTable()
	{
		return null;
	}

	@Override
	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create a copy from the  current record of a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public IFoundSetInternal copy(boolean unrelate) throws ServoyException
	{
		return this;
	}

	@Override
	public MenuItemRecord getRecord(int row)
	{
		if (row < 0 || row >= records.size()) return null;
		return records.get(row);
	}

	@Override
	public IRecordInternal[] getRecords(int startrow, int count)
	{
		int toIndex = startrow + count;
		toIndex = records.size() < toIndex ? records.size() : toIndex;
		List<MenuItemRecord> subList = records.subList(startrow, toIndex);
		return subList.toArray(new IRecordInternal[subList.size()]);
	}

	@Override
	public void deleteAllInternal(IDeleteTrigger deleteTrigger) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public void addAggregateModificationListener(IModificationListener listener)
	{

	}

	@Override
	public void removeAggregateModificationListener(IModificationListener listener)
	{

	}

	@Override
	public boolean hadMoreRows()
	{
		return false;
	}

	@Override
	public void deleteRecord(IRecordInternal record) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete record from a Menu Foundset of datasource " + this.datasource);

	}

	@Override
	public IRecordInternal getRecord(Object[] pk)
	{
		for (IRecordInternal record : records)
		{
			if (Utils.equalObjects(record.getPK(), pk)) return record;
		}
		return null;
	}

	@Override
	public int getRecordIndex(String pkHash, int startHint)
	{
		int hintStart = Math.min(startHint + 5, getSize());

		int start = (hintStart < 0 || hintStart > records.size()) ? 0 : hintStart;

		for (int i = start; --i >= 0;)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		for (int i = start; i < records.size(); i++)
		{
			String recordPkHash = null;
			IRecordInternal record = records.get(i);
			recordPkHash = record.getPKHashKey();
			if (pkHash.equals(recordPkHash))
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getID()
	{
		// we do not automatically assign an id to each foundset so that ng client client side code cannot send in random ints to target any foundset;
		// in this way, only foundsets that were sent to client already (getID() was called on them in various property types to send it to client) can be targeted
		if (foundsetID == 0) foundsetID = this.manager.getNextFoundSetID();
		return foundsetID;
	}

	@Override
	public int getIDInternal()
	{
		return foundsetID;
	}

	@Override
	public boolean isInitialized()
	{
		return true;
	}

	@Override
	public int getRawSize()
	{
		return records.size();
	}

	@Override
	public void fireFoundSetChanged()
	{
		fireFoundSetEvent(0, records.size() - 1, FoundSetEvent.CHANGE_UPDATE);
	}

	@Override
	public QuerySelect getCurrentStateQuery(boolean reduceSearch, boolean clone) throws ServoyException
	{
		return null;
	}

	@Override
	public ISQLSelect getQuerySelectForReading()
	{
		return null;
	}

	@Override
	public boolean containsCalculation(String dataProviderID)
	{
		return false;
	}

	@Override
	public boolean containsAggregate(String name)
	{
		return false;
	}

	@Override
	public int getColumnIndex(String dataProviderID)
	{
		return 0;
	}

	@Override
	public String[] getDataProviderNames(int type)
	{
		return null;
	}

	/**
	 * Returns the datasource (menu:name) for this MenuFoundSet.
	 *
	 * @sample
	 * solutionModel.getForm("x").dataSource  = menuFoundSet.getDataSource();
	 */
	@JSFunction
	@Override
	public String getDataSource()
	{
		return datasource;
	}

	/**
	 * Get foundset name (menu name).
	 *
	 * @sample
	 * var name = foundset.getName()
	 *
	 * @return name.
	 */
	@JSFunction
	public String getName()
	{
		String name = this.datasource;
		if (name != null)
		{
			name = DataSourceUtils.getMenuDataSourceName(datasource);
		}
		return name;
	}

	@Override
	public String getRelationName()
	{
		return relationName;
	}

	/**
	 * Get the number of records in this menu foundset. All records are loaded when foundset is initialized.
	 *
	 * @sample
	 * var nrRecords = %%prefix%%vfs.getSize()
	 *
	 * for (var i = 1; i <= %%prefix%%foundset.getSize(); i++)
	 * {
	 * 	var rec = %%prefix%%vfs.getRecord(i);
	 * }
	 *
	 * @return int current size.
	 */
	@JSFunction
	@Override
	public int getSize()
	{
		return records.size();
	}

	@JSFunction
	public MenuItemRecord getSelectedRecord()
	{
		int selectedIndex = getSelectedIndex();
		if (selectedIndex >= 0 && selectedIndex < records.size()) return records.get(selectedIndex);
		return null;
	}

	/**
	 * Get the selected records.
	 * When the menu foundset is in multiSelect mode (see property multiSelect), selection can be a more than 1 record.
	 *
	 * @sample var selectedRecords = %%prefix%%foundset.getSelectedRecords();
	 * @return Array current records.
	 */
	@JSFunction
	public MenuItemRecord[] getSelectedRecords()
	{
		int[] selectedIndexes = getSelectedIndexes();
		List<MenuItemRecord> selectedRecords = new ArrayList<MenuItemRecord>(selectedIndexes.length);
		for (int index : selectedIndexes)
		{
			MenuItemRecord record = getRecord(index);
			if (record != null)
			{
				selectedRecords.add(record);
			}
		}

		return selectedRecords.toArray(new MenuItemRecord[selectedRecords.size()]);
	}

	/**
	 * Get the MenuItemRecord object at the given index.
	 * Argument "index" is 1 based (so first record is 1).
	 *
	 * @sample var record = %%prefix%%vfs.getRecord(index);
	 *
	 * @param index int record index (1 based).
	 *
	 * @return MenuItemRecord record.
	 */
	public MenuItemRecord js_getRecord(int row)
	{
		return getRecord(row - 1);
	}

	/**
	 * Get the record index. Will return -1 if the record can't be found.
	 *
	 * @sample var index = %%prefix%%foundset.getRecordIndex(record);
	 *
	 * @param record MenuItemRecord
	 *
	 * @return int index.
	 */
	@JSFunction
	public int getRecordIndex(MenuItemRecord record)
	{
		int recordIndex = getRecordIndex((IRecord)record);
		if (recordIndex == -1) return -1;
		return recordIndex + 1;
	}

	@Override
	public void addFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			if (!foundSetEventListeners.contains(l))
			{
				foundSetEventListeners.add(l);
			}
		}

	}

	@Override
	public void removeFoundSetEventListener(IFoundSetEventListener l)
	{
		synchronized (foundSetEventListeners)
		{
			foundSetEventListeners.remove(l);
		}
	}

	@Override
	public Object forEach(IRecordCallback callback)
	{
		FoundSetIterator foundsetIterator = new FoundSetIterator();
		Scriptable scriptableFoundset = null;
		Context.enter();
		try
		{
			scriptableFoundset = (Scriptable)Context.javaToJS(this, this.getFoundSetManager().getApplication().getScriptEngine().getSolutionScope());
		}
		finally
		{
			Context.exit();
		}
		while (foundsetIterator.hasNext())
		{
			IRecord currentRecord = foundsetIterator.next();
			Object returnValue = callback.handleRecord(currentRecord, foundsetIterator.currentIndex, scriptableFoundset);
			if (returnValue != null && returnValue != Undefined.instance)
			{
				return returnValue;
			}
		}
		return null;
	}

	@Override
	public int getRecordIndex(IRecord record)
	{
		return records.indexOf(record);
	}

	@Override
	public boolean isRecordEditable(int row)
	{
		return false;
	}

	@Override
	public boolean isInFindMode()
	{
		return false;
	}

	@Override
	public boolean find()
	{
		return false;
	}

	@Override
	public int search() throws Exception
	{
		return 0;
	}

	@Override
	public void loadAllRecords() throws ServoyException
	{

	}

	@Override
	public void clear()
	{

	}

	@Override
	public void deleteRecord(int row) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete record from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public void deleteAllRecords() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records from a Menu Foundset of datasource " + this.datasource);

	}

	@Override
	public int newRecord(int indexToAdd, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create new record in a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int recordIndex, int indexToAdd) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records in a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int getSelectedIndex()
	{
		if (selectionModel == null) createSelectionModel();

		return selectionModel.getSelectedRow();
	}

	@Override
	public boolean setSelectedIndex(int selectedRow)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRow(selectedRow);
		return true;

	}

	@Override
	@JSSetter
	public void setMultiSelect(boolean multiSelect)
	{
		if (multiSelectPinnedForm == null) setMultiSelectInternal(multiSelect); // if a form is currently overriding this, ignore js call

	}

	@Override
	@JSGetter
	public boolean isMultiSelect()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectionMode() == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
	}

	@Override
	public boolean setSelectedIndexes(int[] indexes)
	{
		if (selectionModel == null) createSelectionModel();
		selectionModel.setSelectedRows(indexes);
		return true;

	}

	@Override
	public int[] getSelectedIndexes()
	{
		if (selectionModel == null) createSelectionModel();
		return selectionModel.getSelectedRows();
	}

	@Override
	public String getSort()
	{
		return null;
	}

	@Override
	public void setSort(String sortString) throws ServoyException
	{

	}

	@Override
	public IQueryBuilder getQuery()
	{
		return null;
	}

	@Override
	public boolean loadByQuery(IQueryBuilder query) throws ServoyException
	{
		return false;
	}

	@Override
	public void setRecordToStringDataProviderID(String dataProviderID)
	{

	}

	@Override
	public String getRecordToStringDataProviderID()
	{
		return null;
	}

	@Override
	public void sort(List<SortColumn> sortColumns) throws ServoyException
	{

	}

	@Override
	public void makeEmpty()
	{
		clear();

	}

	@Override
	public void browseAll() throws ServoyException
	{
		loadAllRecords();

	}

	@Override
	public void deleteAll() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't delete records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord() throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException
	{
		throw new UnsupportedOperationException("Can't create records from a Menu Foundset of datasource " + this.datasource);
	}

	@Override
	public boolean containsDataProvider(String dataProviderID)
	{
		return false;
	}

	@Override
	public Object getDataProviderValue(String dataProviderID)
	{
		return null;
	}

	@Override
	public Object setDataProviderValue(String dataProviderID, Object value)
	{
		return null;
	}

	@Override
	public Iterator<IRecord> iterator()
	{
		return new FoundSetIterator();
	}

	@Override
	public void completeFire(Map<IRecord, List<String>> entries)
	{
		int start = Integer.MAX_VALUE;
		int end = -1;
		List<String> dataproviders = null;
		for (Entry<IRecord, List<String>> entry : entries.entrySet())
		{
			int index = getRecordIndex(entry.getKey());
			if (index != -1 && start > index)
			{
				start = index;
			}
			if (end < index)
			{
				end = index;
			}
			if (dataproviders == null) dataproviders = entry.getValue();
			else dataproviders.addAll(entry.getValue());
		}
		if (start != Integer.MAX_VALUE && end != -1)
		{
			fireFoundSetEvent(start, end, FoundSetEvent.CHANGE_UPDATE, dataproviders);
		}

	}

	@Override
	public int getRowCount()
	{
		return getSize();
	}

	@Override
	public int getColumnCount()
	{
		//do nothing handled in CellAdapter
		return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		//do nothing handled in CellAdapter
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex)
	{
		return isRecordEditable(rowIndex);
	}

	@Override
	public void setElementAt(Object aValue, int rowIndex)
	{

	}

	@Override
	public Object getElementAt(int index)
	{
		return getRecord(index);
	}

	@Override
	public void addListDataListener(ListDataListener l)
	{
		getTableAndListEventDelegate().addListDataListener(l);

	}

	@Override
	public void removeListDataListener(ListDataListener l)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.removeListDataListener(l);
		}

	}

	@Override
	public Object get(Symbol key, Scriptable start)
	{
		if (SymbolKey.ITERATOR.equals(key))
		{
			return symbol_iterator;
		}
		return Scriptable.NOT_FOUND;
	}

	@Override
	public boolean has(Symbol key, Scriptable start)
	{
		return (SymbolKey.ITERATOR.equals(key));
	}

	@Override
	public void put(Symbol key, Scriptable start, Object value)
	{

	}

	@Override
	public void delete(Symbol key)
	{

	}

	@Override
	public AlwaysRowSelectedSelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	@Override
	public void fireTableModelEvent(int firstRow, int lastRow, int column, int type)
	{
		if (tableAndListEventDelegate != null)
		{
			tableAndListEventDelegate.fireTableModelEvent(firstRow, lastRow, column, type);
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.ISwingFoundSet#pinMultiSelectIfNeeded(boolean, java.lang.String, int)
	 */
	@Override
	public void pinMultiSelectIfNeeded(boolean multiSelect, String formName, int pinLevel)
	{
		if (multiSelectPinnedForm == null)
		{
			// no current pinning; just pin
			multiSelectPinLevel = pinLevel;
			multiSelectPinnedForm = formName;
			setMultiSelectInternal(multiSelect);
		}
		else if (pinLevel < multiSelectPinLevel)
		{
			// current pin was for hidden form, this is a visible form
			multiSelectPinLevel = pinLevel;
			if (multiSelectPinnedForm != formName)
			{
				multiSelectPinnedForm = formName;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (pinLevel == multiSelectPinLevel)
		{
			// same pin level, different forms; always choose one with lowest "name"
			if (formName.compareTo(multiSelectPinnedForm) < 0)
			{
				multiSelectPinnedForm = formName;
				setMultiSelectInternal(multiSelect);
			}
		}
		else if (formName == multiSelectPinnedForm) // && (pinLevel > multiSelectPinLevel) implied
		{
			// pinlevel is higher then current; if this is the current pinned form, update the pin level
			// maybe other visible forms using this foundset want to pin selection mode in this case (visible pinning form became hidden)
			multiSelectPinLevel = pinLevel;
			fireSelectionModeChange();
		}

	}

	@Override
	public void unpinMultiSelectIfNeeded(String formName)
	{
		if (multiSelectPinnedForm == formName)
		{
			multiSelectPinnedForm = null;
			fireSelectionModeChange(); // this allows any other forms that might be currently using this foundset to apply their own selectionMode to it
		}

	}

	private void createSelectionModel()
	{
		if (selectionModel == null)
		{
			selectionModel = new AlwaysRowSelectedSelectionModel(this);
			addListDataListener(selectionModel);
		}
	}

	private void createRecords(JSMenu menu)
	{
		records.clear();
		for (JSMenuItem menuItem : menu.getMenuItemsWithSecurity())
		{
			records.add(new MenuItemRecord(menuItem, getMenuItemData(menuItem), this));
		}
	}

	private void createRecords(JSMenuItem menuItem)
	{
		if (menuItem != null)
		{
			for (JSMenuItem childMenuItem : menuItem.getMenuItemsWithSecurity())
			{
				records.add(new MenuItemRecord(childMenuItem, getMenuItemData(childMenuItem), this));
			}
		}
	}

	private Map<String, Object> getMenuItemData(JSMenuItem item)
	{
		Map<String, Object> itemMap = new HashMap<String, Object>();
		itemMap.put("itemID", item.getName());
		itemMap.put("menuText", item.getMenuText());
		itemMap.put("styleClass", item.getStyleClass());
		itemMap.put("iconStyleClass", item.getIconStyleClass());
		itemMap.put("tooltipText", item.getTooltipText());
		itemMap.put("enabled", item.getEnabledWithSecurity());
		itemMap.put("callbackArguments", item.getCallbackArguments());
		Map<String, Map<String, Object>> extraProperties = item.getExtraProperties();
		if (extraProperties != null)
		{
			for (Map<String, Object> properties : extraProperties.values())
			{
				itemMap.putAll(properties);
			}
		}
		return itemMap;
	}

	protected TableAndListEventDelegate getTableAndListEventDelegate()
	{
		if (tableAndListEventDelegate == null) tableAndListEventDelegate = new TableAndListEventDelegate(this);
		return tableAndListEventDelegate;
	}

	private void fireFoundSetEvent(int firstRow, int lastRow, int changeType)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow));
	}

	private void fireFoundSetEvent(int firstRow, int lastRow, int changeType, List<String> dataproviders)
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.CONTENTS_CHANGED, changeType, firstRow, lastRow, dataproviders));
	}

	private void fireSelectionModeChange()
	{
		fireFoundSetEvent(new FoundSetEvent(this, FoundSetEvent.SELECTION_MODE_CHANGE, FoundSetEvent.CHANGE_UPDATE));
	}

	protected void setMultiSelectInternal(boolean isMultiSelect)
	{
		if (selectionModel == null) createSelectionModel();
		if (isMultiSelect != isMultiSelect())
		{
			selectionModel.setSelectionMode(isMultiSelect ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
		}
	}

	protected void fireFoundSetEvent(final FoundSetEvent e)
	{
		if (foundSetEventListeners.size() > 0)
		{
			Runnable run = new Runnable()
			{
				@Override
				public void run()
				{
					if (foundSetEventListeners.size() > 0)
					{
						final IFoundSetEventListener[] array;
						synchronized (foundSetEventListeners)
						{
							array = foundSetEventListeners.toArray(new IFoundSetEventListener[foundSetEventListeners.size()]);
						}

						for (IFoundSetEventListener element : array)
						{
							element.foundSetChanged(e);
						}
					}
				}
			};
			if (this.manager.getApplication().isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				this.manager.getApplication().invokeLater(run);
			}
		}
		int type = e.getChangeType();
		int firstRow = e.getFirstRow();
		int lastRow = e.getLastRow();
		// always fire also if there are no listeners (because of always-first-selection rule)
		if (type == FoundSetEvent.CHANGE_INSERT || type == FoundSetEvent.CHANGE_DELETE)
		{
			// if for example both a record view and a table view listen for this event and the record view changes the selection before the table view tries to adjust it due to the insert (on the same selectionModel)
			// selection might become wrong (selection = 165 when only 164 records are available); so selectionModel needs to know; similar situations might happen for delete also
			boolean before = selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(true);
			try
			{
				getTableAndListEventDelegate().fireTableAndListEvent(getFoundSetManager().getApplication(), firstRow, lastRow, type);
			}
			finally
			{
				selectionModel.setFoundsetIsFiringSizeChangeTableAndListEvent(before);
			}
		}
		else if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
		{
			getTableAndListEventDelegate().fireTableAndListEvent(getFoundSetManager().getApplication(), firstRow, lastRow, type);
		}
	}

	private class FoundSetIterator implements Iterator<IRecord>
	{
		private int currentIndex = -1;
		private Object[] currentPK = null;
		private final List<Object[]> processedPKS = new ArrayList<Object[]>();
		private IRecord currentRecord = null;

		public FoundSetIterator()
		{
		}

		@Override
		public boolean hasNext()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			return currentRecord != null;
		}

		@Override
		public IRecord next()
		{
			if (currentRecord == null)
			{
				currentRecord = getNextRecord();
			}
			IRecord returnRecord = currentRecord;
			currentRecord = null;
			return returnRecord;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private IRecord getNextRecord()
		{

			int nextIndex = currentIndex + 1;
			IRecord nextRecord = getRecord(nextIndex);
			while (nextRecord instanceof PrototypeState)
			{
				nextIndex = currentIndex + 1;
				nextRecord = getRecord(nextIndex);
			}
			if (currentIndex >= 0)
			{
				IRecord tmpCurrentRecord = getRecord(currentIndex);
				if (tmpCurrentRecord == null || !Utils.equalObjects(tmpCurrentRecord.getPK(), currentPK))
				{
					// something is changed in the foundset, recalculate
					if (tmpCurrentRecord == null)
					{
						int size = records.size();
						if (size == 0)
						{
							return null;
						}
						currentIndex = size - 1;
						tmpCurrentRecord = getRecord(currentIndex);
					}
					if (!listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
					{
						// substract current index
						while (tmpCurrentRecord != null && !listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex - 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex + 1;
						nextRecord = getRecord(nextIndex);
					}
					else
					{
						// increment current index
						while (tmpCurrentRecord != null && listContainsArray(processedPKS, tmpCurrentRecord.getPK()))
						{
							currentIndex = currentIndex + 1;
							tmpCurrentRecord = getRecord(currentIndex);
						}
						nextIndex = currentIndex;
						nextRecord = tmpCurrentRecord;
					}
					if (nextRecord == null)
					{
						return null;
					}
				}
			}
			if (nextRecord != null)
			{
				currentPK = nextRecord.getPK();
			}
			currentIndex = nextIndex;
			processedPKS.add(currentPK);
			return nextRecord;
		}

		private boolean listContainsArray(List<Object[]> list, Object[] value)
		{
			if (list != null)
			{
				for (Object[] array : list)
				{
					if (Utils.equalObjects(array, value))
					{
						return true;
					}
				}
			}
			return false;
		}
	}
}
