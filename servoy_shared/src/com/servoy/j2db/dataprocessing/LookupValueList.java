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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.CustomValueList.DisplayString;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.OrCondition;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Valuelist for lookup of values
 *
 * @author jcompagner, jblok
 */
public class LookupValueList implements IValueList
{
	private TableChangeListener tableListener;
	private final Map<ListDataListener, Object> listeners = new WeakHashMap<ListDataListener, Object>();

	private final List<Object> alReal = new SafeArrayList<Object>();
	private final List<Object> alDisplay = new ArrayList<Object>();

	private final ValueList valueList;
	private final IServiceProvider application;
	private boolean dontQuery;
	private final Table table;

	private final int showValues;
	private final int returnValues;
	private final boolean concatReturnValues;
	private final boolean concatShowValues;
	private IRecordInternal parentState;
	private IFoundSetInternal relatedFoundset;
	private final IValueList secondLookup;
	private final String displayFormat;
	protected final int maxValuelistRows;

	public LookupValueList(ValueList list, IServiceProvider application, IValueList fallback, String displayFormat) throws Exception
	{
		this.valueList = list;
		this.application = application;
		this.secondLookup = fallback;
		this.displayFormat = displayFormat;

		String dataSource = null;
		Relation[] relations = null;
		if (list.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
		{
			dataSource = list.getDataSource();
		}
		else
		{
			relations = application.getFlattenedSolution().getRelationSequence(list.getRelationName());
			if (relations != null)
			{
				dataSource = relations[relations.length - 1].getForeignDataSource();
			}
		}

		int maxRowsSetting = (application instanceof IApplication)
			? Utils.getAsInteger(((IApplication)application).getClientProperty(IApplication.VALUELIST_MAX_ROWS)) : 0;
		maxValuelistRows = (maxRowsSetting > 0 && maxRowsSetting <= 1000) ? maxRowsSetting
			: ((FoundSetManager)application.getFoundSetManager()).pkChunkSize * 4;

		table = (Table)application.getFoundSetManager().getTable(dataSource);

		showValues = list.getShowDataProviders();
		returnValues = list.getReturnDataProviders();

		//more than one value -> concat
		concatShowValues = ((showValues != 1) && (showValues != 2) && (showValues != 4));
		concatReturnValues = ((returnValues != 1) && (returnValues != 2) && (returnValues != 4));

		if (table != null && showValues != returnValues && tableListener == null)
		{
			//register
			tableListener = new TableChangeListener();
			if (table != null)
			{
				((FoundSetManager)application.getFoundSetManager()).addTableListener(table, tableListener);

				FlattenedSolution fs = application.getFlattenedSolution();
				for (int i = 0; relations != null && i < relations.length - 1; i++)
				{
					((FoundSetManager)application.getFoundSetManager()).addTableListener(fs.getTable(relations[i].getForeignDataSource()), tableListener);
				}
			}
		}
	}

	public void setFallbackValueList(IValueList list)
	{
		// ignore is being done in the constructor
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IValueList#getFallbackValueList()
	 */
	public IValueList getFallbackValueList()
	{
		return secondLookup;
	}

	private final class TableChangeListener implements ITableChangeListener
	{
		public void tableChange(TableEvent e)
		{
			clear();
			Iterator<ListDataListener> it = listeners.keySet().iterator();
			ListDataEvent lde = new ListDataEvent(LookupValueList.this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
			while (it.hasNext())
			{
				it.next().contentsChanged(lde);
			}
		}
	}

	private Object getAsRightType(String dataprovider, Object val)
	{
		if (dataprovider != null && table != null)
		{
			return table.getColumn(dataprovider).getAsRightType(val);
		}
		return null;
	}

	public String getName()
	{
		return valueList.getName();
	}

	public ValueList getValueList()
	{
		return valueList;
	}

	public Object getRealElementAt(int index)
	{
		if (index < -1 && secondLookup != null)
		{
			return secondLookup.getRealElementAt(index * -1 - 2);
		}
		return alReal.get(index);
	}

	public String getRelationName()
	{
		return valueList.getRelationName();
	}

	public void fill(IRecordInternal ps)
	{
		if (valueList.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES)
		{
			this.parentState = ps;
			if (parentState != null)
			{
				IFoundSetInternal relation = parentState.getRelatedFoundSet(getRelationName());
				if (relatedFoundset != relation)
				{
					clear();
					if (relation != null && relation.getSize() > 0)
					{
						// try to load what is already in memory
						int count = relation.getSize();
						// don't trigger an extra load.
						if (relation.hadMoreRows()) count--;
						// max the number of rows.
						if (count > maxValuelistRows)
						{
							count = maxValuelistRows;
						}
						IRecordInternal[] records = relation.getRecords(0, count);
						Object[][] data = new Object[records.length][];
						for (int i = 0; i < records.length; i++)
						{
							data[i] = new Object[3];
							if (valueList.getDataProviderID1() != null) data[i][0] = records[i].getValue(valueList.getDataProviderID1());
							if (valueList.getDataProviderID2() != null) data[i][1] = records[i].getValue(valueList.getDataProviderID2());
							if (valueList.getDataProviderID3() != null) data[i][2] = records[i].getValue(valueList.getDataProviderID3());
						}

						String[] displayFormats = getDisplayFormat();
						for (Object[] element : data)
						{
							DisplayString obj = CustomValueList.handleDisplayData(valueList, displayFormats, concatShowValues, showValues, element,
								application);
							if (obj != null && !obj.equals("")) //$NON-NLS-1$
							{
								alDisplay.add(obj);
								alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, element, application));
							}
						}
					}
				}
				relatedFoundset = relation;
			}
			else clear();
		}
		if (secondLookup != null) secondLookup.fill(ps);
	}

	public void setDoNotQuery(boolean b)
	{
		dontQuery = b;
	}

	public void addRow(Object real, Object display)
	{
		int index = alReal.indexOf(real);
		if (index == -1)
		{
			alReal.add(real);
			alDisplay.add(display);
		}
		else
		{
			alDisplay.set(index, display);
		}
	}

	private void fill(Object display, Object real) throws ServoyException, RemoteException
	{
		if (dontQuery || table == null) return;

		Object value = null;
		int values = 0;
		if (display != null)
		{
			values = showValues;
			value = display;
		}
		else
		{
			values = returnValues;
			value = real;
		}

		QuerySelect select = null;
		BaseQueryTable qTable = null;
		if (valueList.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
		{
			select = DBValueList.createValuelistQuery(application, valueList, table);
			if (select != null)
			{
				qTable = select.getTable();
			}
		}
		else
		{
			Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
			Pair<QuerySelect, BaseQueryTable> pair = RelatedValueList.createRelatedValuelistQuery(application, valueList, relations, parentState);
			if (pair != null)
			{
				select = pair.getLeft();
				qTable = pair.getRight();
			}
		}
		if (select == null)
		{
			return;
		}

		String[] displayValues = null;
		String separator = valueList.getSeparator();
		if (values == showValues && value != null && separator != null && !separator.equals("")) //$NON-NLS-1$
		{
			if (values != 1 && values != 2 && values != 4)
			{
				// its a combination
				displayValues = Utils.stringSplit(value.toString(), separator);
			}
		}

		OrCondition where = new OrCondition();
		if ((values & 1) != 0)
		{
			String dp1 = valueList.getDataProviderID1();
			if (displayValues != null)
			{

				for (String displayValue : displayValues)
				{
					where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp1),
						getAsRightType(dp1, displayValue)));
				}
			}
			// also just add the complete value, for the possibility that it was a value with a separator.
			value = getAsRightType(dp1, value);
			where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp1), value));
		}
		if ((values & 2) != 0)
		{
			String dp2 = valueList.getDataProviderID2();
			if (displayValues != null)
			{
				for (String displayValue : displayValues)
				{
					where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp2),
						getAsRightType(dp2, displayValue)));
				}
			}
			value = getAsRightType(dp2, value);
			where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp2), value));
		}
		if ((values & 4) != 0)
		{
			String dp3 = valueList.getDataProviderID3();
			if (displayValues != null)
			{
				for (String displayValue : displayValues)
				{
					where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp3),
						getAsRightType(dp3, displayValue)));
				}
			}
			value = getAsRightType(dp3, value);
			where.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, DBValueList.getQuerySelectValue(table, qTable, dp3), value));
		}
		select.setCondition(SQLGenerator.CONDITION_SEARCH, where);

		FoundSetManager foundSetManager = ((FoundSetManager)application.getFoundSetManager());
		String transaction_id = foundSetManager.getTransactionID(table.getServerName());
		ArrayList<TableFilter> tableFilterParams = foundSetManager.getTableFilterParams(table.getServerName(), select);
		if (valueList.getUseTableFilter()) //apply name as filter on column valuelist_name
		{
			if (tableFilterParams == null)
			{
				tableFilterParams = new ArrayList<TableFilter>();
			}
			tableFilterParams.add(new TableFilter("lookupValueList.nameFilter", table.getServerName(), table.getName(), table.getSQLName(), //$NON-NLS-1$
				DBValueList.NAME_COLUMN, IBaseSQLCondition.EQUALS_OPERATOR, valueList.getName()));
		}
		SQLStatement trackingInfo = null;
		if (foundSetManager.getEditRecordList().hasAccess(table, IRepository.TRACKING_VIEWS))
		{
			trackingInfo = new SQLStatement(ISQLActionTypes.SELECT_ACTION, table.getServerName(), table.getName(), null, null);
			trackingInfo.setTrackingData(select.getColumnNames(), new Object[][] { }, new Object[][] { }, application.getUserUID(),
				foundSetManager.getTrackingInfo(), application.getClientID());
		}
		IDataSet set = application.getDataServer().performQuery(application.getClientID(), table.getServerName(), transaction_id, select, tableFilterParams,
			!select.isUnique(), 0, maxValuelistRows, IDataServer.VALUELIST_QUERY, trackingInfo);
		String[] displayFormats = getDisplayFormat();
		for (int i = 0; i < set.getRowCount(); i++)
		{
			Object[] row = CustomValueList.processRow(set.getRow(i), showValues, returnValues);
			DisplayString obj = CustomValueList.handleDisplayData(valueList, displayFormats, concatShowValues, showValues, row, application);
			if (obj != null && !obj.equals("")) //$NON-NLS-1$
			{
				alDisplay.add(obj);
				alReal.add(CustomValueList.handleRowData(valueList, concatReturnValues, returnValues, row, application));
			}
		}
	}

	public boolean hasRealValues()
	{
		return showValues != returnValues;
	}

	public int realValueIndexOf(Object obj)
	{
		return realValueIndexOf(obj, true);
	}

	public int realValueIndexOf(Object obj, boolean addIfNotPresent)
	{
		if (obj == null) return -1;
		int index = alReal.indexOf(obj);
		if (index == -1)
		{
			try
			{
				fill(null, obj);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			index = alReal.indexOf(obj);
			// If a real object still isn't there after the fill
			// add it to the list so that it won't be queried at again and again
			if (index == -1)
			{
				if (secondLookup != null)
				{
					try
					{
						index = secondLookup.realValueIndexOf(obj);
						if (index != -1)
						{
							return (index + 2) * -1;
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}
				if (addIfNotPresent)
				{
					index = alReal.size();
					alReal.add(obj);
					alDisplay.add(CustomValueList.convertToString(obj, application));
				}
			}
			else if (showValues != returnValues)
			{
				// Check if display wasn't stored in real/display together.
				Object displayValue = alDisplay.get(index);
				if (!(displayValue instanceof DisplayString))
				{
					int index2 = alReal.indexOf(displayValue);
					if (index2 > -1)
					{
						alDisplay.remove(index2);
						alReal.remove(index2);
					}
				}
			}
		}
		return index;
	}

	private int searchDisplayArray(Object value)
	{
		for (int i = 0; i < alDisplay.size(); i++)
		{
			Object element = alDisplay.get(i);
			if (element == null)
			{
				if (value == null) return i;
			}
			else
			{
				if (element.equals(value))
				{
					return i;
				}
			}
		}
		return -1;
	}

	public int indexOf(Object elem)
	{
		int index = searchDisplayArray(elem);
		if (index == -1 && elem != null && !"".equals(elem)) //$NON-NLS-1$
		{
			try
			{
				fill(elem, null);
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			index = searchDisplayArray(elem);

			if (index == -1 && secondLookup != null)
			{
				index = secondLookup.indexOf(elem);
				if (index != -1)
				{
					index = (index + 2) * -1; // all values range from -2 > N
				}
			}
		}
		return index;
	}

	public void deregister()
	{
		clear();
		if (tableListener != null && table != null)
		{
			FlattenedSolution fs = application.getFlattenedSolution();
			((FoundSetManager)application.getFoundSetManager()).removeTableListener(table, tableListener);

			Relation[] relations = application.getFlattenedSolution().getRelationSequence(valueList.getRelationName());
			for (int i = 0; relations != null && i < relations.length - 1; i++)
			{
				((FoundSetManager)application.getFoundSetManager()).removeTableListener(fs.getTable(relations[i].getForeignDataSource()), tableListener);
			}
			tableListener = null;
		}
	}

	public void clear()
	{
		alReal.clear();
		alDisplay.clear();
		relatedFoundset = null;
	}

	public boolean getAllowEmptySelection()
	{
		return valueList.getAddEmptyValue() == IValueListConstants.EMPTY_VALUE_ALWAYS;
	}

	public int getSize()
	{
		return alReal.size();
	}

	public Object getElementAt(int index)
	{
		if (index < -1 && secondLookup != null)
		{
			return secondLookup.getElementAt(index * -1 - 2);
		}
		Object object = alDisplay.get(index);
		if (object instanceof DisplayString)
		{
			return object.toString();
		}
		return object;
	}

	public void addListDataListener(ListDataListener l)
	{
		listeners.put(l, null);
	}

	public void removeListDataListener(ListDataListener l)
	{
		listeners.remove(l);
	}

	public String[] getDisplayFormat()
	{
		String[] displayFormats = new String[3];
		if (displayFormat == null || hasRealValues()) // format is linked to dataproviderid, so returning it could lead to incorrect display
		{
			if (table != null)
			{
				Column col1 = table.getColumn(valueList.getDataProviderID1());
				if (col1 != null && col1.getColumnInfo() != null) displayFormats[0] = col1.getColumnInfo().getDefaultFormat();
				Column col2 = table.getColumn(valueList.getDataProviderID2());
				if (col2 != null && col2.getColumnInfo() != null) displayFormats[1] = col2.getColumnInfo().getDefaultFormat();
				Column col3 = table.getColumn(valueList.getDataProviderID3());
				if (col3 != null && col3.getColumnInfo() != null) displayFormats[2] = col3.getColumnInfo().getDefaultFormat();
			}
		}
		else
		{
			for (int i = 0; i < displayFormats.length; i++)
			{
				displayFormats[i] = displayFormat;
			}
		}
		return displayFormats;
	}

	/**
	 * Get real value list, used when this LookupValueList was used as fallback value list.
	 */
	public IValueList getRealValueList()
	{
		return ComponentFactory.getRealValueList(application, valueList, true, 0, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#isRecordLinked()
	 */
	@Override
	public boolean isRecordLinked()
	{
		return valueList.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES ||
			valueList.getDatabaseValuesType() == IValueListConstants.GLOBAL_METHOD_VALUES;
	}
}
