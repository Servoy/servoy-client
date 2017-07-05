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
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.api.IJSDataSet;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.scripting.IExecutingEnviroment;
import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;


/**
 * Scriptable dataset wrapper
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSDataSet")
public class JSDataSet implements Wrapper, IDelegate<IDataSet>, Scriptable, Serializable, IJSDataSet
{
	private static final long serialVersionUID = 1L;

	private Map<String, NativeJavaMethod> jsFunctions;

	@SuppressWarnings("unchecked")
	private void initJSFunctions(IServiceProvider serviceProvider)
	{
		if (serviceProvider != null)
		{
			jsFunctions = (Map<String, NativeJavaMethod>)serviceProvider.getRuntimeProperties().get(IServiceProvider.RT_JSDATASET_FUNCTIONS);
		}
		if (jsFunctions == null)
		{
			jsFunctions = new HashMap<String, NativeJavaMethod>();
			try
			{
				Method[] methods = JSDataSet.class.getMethods();
				for (Method m : methods)
				{
					String name = null;
					if (m.getName().startsWith("js_")) //$NON-NLS-1$
					{
						name = m.getName().substring(3);
					}
					else if (AnnotationManagerReflection.getInstance().isAnnotationPresent(m, JSDataSet.class, JSFunction.class))
					{
						name = m.getName();
					}
					if (name != null)
					{
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
				if (serviceProvider != null)
				{
					serviceProvider.getRuntimeProperties().put(IServiceProvider.RT_JSDATASET_FUNCTIONS, jsFunctions);
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}
	}

	private static JSDataSet prototype = new JSDataSet();

	private IDataSetWithIndex set;
	private ServoyException exception;

	private final IServiceProvider application;

	public JSDataSet() //only for use JS engine
	{
		this.application = null;
		initJSFunctions(application);
		this.set = new DataSetWithIndex(new BufferedDataSet());
	}

	public JSDataSet(IServiceProvider application)
	{
		this(application, new DataSetWithIndex(new BufferedDataSet()));
	}

	public JSDataSet(IServiceProvider application, int rows, String[] cols)
	{
		this.application = application;
		initJSFunctions(application);
		if (rows >= 0 && cols.length >= 0)
		{
			List<Object[]> emptyRows = new ArrayList<Object[]>(rows);
			for (int i = 0; i < rows; i++)
			{
				emptyRows.add(new Object[cols.length]);
			}
			this.set = new DataSetWithIndex(new BufferedDataSet(cols, emptyRows));
		}
		if (application != null)
		{
			setParentScope(application.getScriptEngine().getSolutionScope());
		}
	}

	public JSDataSet(IDataSet set)
	{
		this(null, set);
	}

	public JSDataSet(IServiceProvider application, IDataSet set)
	{
		this.application = application;
		initJSFunctions(application);
		if (set instanceof IDataSetWithIndex)
		{
			this.set = (IDataSetWithIndex)set;
		}
		else
		{
			this.set = new DataSetWithIndex(set);
		}
		if (application != null)
		{
			setParentScope(application.getScriptEngine().getSolutionScope());
		}
		else if (J2DBGlobals.getServiceProvider() != null && J2DBGlobals.getServiceProvider().getScriptEngine() != null)
		{
			setParentScope(J2DBGlobals.getServiceProvider().getScriptEngine().getSolutionScope());
		}
	}

	public JSDataSet(ServoyException e)
	{
		application = null;
		initJSFunctions(application);
		set = null;
		exception = e;
	}

	public void setPrototype(Scriptable prototype)
	{
	}

	public Scriptable getPrototype()
	{
		if (prototype != this)
		{
			return prototype;
		}
		return null;
	}

	/**
	 * Get the number of rows in the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var totalRows = dataset.getMaxRowIndex();
	 *
	 * @return int number of rows.
	 */
	public int js_getMaxRowIndex()
	{
		if (set != null)
		{
			return set.getRowCount();
		}
		return 0;
	}

	/**
	 * Get the number of columns in the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * for (var i = 1; i <= dataset.getMaxColumnIndex(); i++)
	 * {
	 * 	colArray[i-1] = dataset.getColumnName(i)
	 * 	//have to subtract 1, because an array is zero based and a dataset is 1 based.
	 * }
	 *
	 * @return int number of columns.
	 */
	public int js_getMaxColumnIndex()
	{
		if (set != null)
		{
			return set.getColumnCount();
		}
		return 0;
	}

	/**
	 * Get or set the record index of the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * //to set the rowIndex:
	 * dataset.rowIndex = 1 //sets the rowIndex to the first row (dataset is 1-based)
	 * //to retrieve the rowIndex of the currently selected row
	 * var currRow = dataset.rowIndex
	 */
	public int js_getRowIndex()
	{
		if (set != null)
		{
			return set.getRowIndex();
		}
		return -1;
	}

	public void js_setRowIndex(int r)
	{
		if (set != null)
		{
			if (r > 0 && r <= set.getRowCount())
			{
				set.setRowIndex(r);
			}
		}
	}

	/**
	 * Remove a row from the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * dataset.removeRow(1); //removes the first row
	 * dataset.removeRow(-1); //removes all rows
	 *
	 * @param row row index to remove, -1 for all rows
	 */
	public void js_removeRow(int row)
	{
		if (set != null)
		{
			if (row == -1 || (row > 0 && row <= set.getRowCount()))
			{
				set.removeRow(row == -1 ? -1 : (row - 1));
				if (row == -1)
				{
					htmlAttributes = null;
				}
				else
				{
					correctAttributeIndex(true, false, row - 1);
				}
				if (tableModelWrapper != null) tableModelWrapper.fireTableStructureChanged();
			}
		}
	}

	/**
	 * Add a row to the dataset. The row will be added as the last row.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * dataset.addRow(new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns
	 * dataset.addRow(2, new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns at row 2
	 *
	 * @param array row data
	 */
	public void js_addRow(Object[] array)
	{
		js_addRow(js_getMaxRowIndex() + 1, array);
	}

	/**
	 * Add a row to the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * dataset.addRow(new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns
	 * dataset.addRow(2, new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns at row 2
	 *
	 * @param index index to add row (1-based)
	 * @param array row data
	 */
	public void js_addRow(int index, Object[] array)
	{
		Object[] row = array;
		if (set != null && row != null)
		{
			if (row.length < set.getColumnCount())
			{
				Object[] tmp = new Object[set.getColumnCount()];
				System.arraycopy(row, 0, tmp, 0, row.length);
				row = tmp;
			}
			set.addRow(index - 1, row);
			correctAttributeIndex(true, true, index - 1);
			if (tableModelWrapper != null) tableModelWrapper.fireTableStructureChanged();
		}
	}

	/**
	 * @clonedesc js_addColumn(String)
	 * @sampleas js_addColumn(String)
	 *
	 * @param name column name.
	 * @param index column index number between 1 and getMaxColumnIndex().
	 *
	 * @return true if succeeded, else false.
	 */
	public boolean js_addColumn(String name, Number index)
	{
		return js_addColumn(name, index, Integer.valueOf(0));
	}

	/**
	 * adds a column with the specified name to the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var success = dataset.addColumn('columnName',1);
	 *
	 * @param name column name.
	 *
	 * @return true if succeeded, else false.
	 */
	public boolean js_addColumn(String name)
	{
		return js_addColumn(name, null, Integer.valueOf(0));
	}

	/**
	 * @clonedesc js_addColumn(String)
	 * @sampleas js_addColumn(String)
	 *
	 * @param name column name.
	 *
	 * @param index column index number between 1 and getMaxColumnIndex().
	 *
	 * @param type the type of column, see JSColumn constants.
	 *
	 * @return true if succeeded, else false.
	 */
	public boolean js_addColumn(String name, Number index, Number type)
	{
		if (set == null) return false;

		int _type = Utils.getAsInteger(type);
		// use Integer in stead of int to allow old vararg calls addColumn(name,null, type)
		int columnIndex = (index == null ? 0 : index.intValue()) - 1;

		boolean result = set.addColumn(columnIndex, name, _type);
		if (result)
		{
			makeColumnMap();
			correctAttributeIndex(false, true, columnIndex);
		}
		return result;
	}

	/**
	 * Remove a column by index from the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var success = dataset.removeColumn(1); // removes first column
	 *
	 * @param index index of column to remove (1-based)
	 *
	 * @return true if succeeded, else false.
	 */
	public boolean js_removeColumn(int index)
	{
		boolean result = set.removeColumn(index - 1);//all Javascript calls are 1 based
		if (result)
		{
			makeColumnMap();
			correctAttributeIndex(false, false, index - 1);
		}
		return result;
	}

	private void correctAttributeIndex(final boolean row_col, boolean add_del, int index)
	{
		if (htmlAttributes != null)
		{
			int plus_minus = (add_del ? +1 : -1);
			Map<Pair<Integer, Integer>, Map<String, String>> newhtmlAttributes = new HashMap<Pair<Integer, Integer>, Map<String, String>>();

			//replace the row ref
			List<Pair<Integer, Integer>> keys = new ArrayList<Pair<Integer, Integer>>(htmlAttributes.keySet());
			Collections.sort(keys, new Comparator<Pair<Integer, Integer>>()
			{
				public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2)
				{
					return (row_col ? o1.getLeft() - o2.getLeft() : o1.getRight() - o2.getRight());
				}
			});
			Iterator<Pair<Integer, Integer>> it = keys.iterator();
			while (it.hasNext())
			{
				Pair<Integer, Integer> pair = it.next();
				Map<String, String> value = htmlAttributes.get(pair);
				if (row_col)
				{
					//row
					if (pair.getLeft().intValue() > index) pair.setLeft(Integer.valueOf(pair.getLeft().intValue() + plus_minus));
				}
				else
				{
					//col
					if (pair.getRight().intValue() > index) pair.setRight(Integer.valueOf(pair.getRight().intValue() + plus_minus));
				}
				newhtmlAttributes.put(pair, value);//rehash
			}
			htmlAttributes = newhtmlAttributes;
		}
	}

	/**
	 * Get a column name based on index.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var firstColumName = dataset.getColumnName(1) //retrieves the first columnname into the variable firstColumName
	 * //using a loop you can get all columnames in an array:
	 * var query = 'select * from customers';
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, null, 100);
	 * var colArray = new Array()
	 * for (var i = 1; i <= dataset.getMaxColumnIndex(); i++)
	 * {
	 * 	colArray[i-1] = dataset.getColumnName(i)
	 * 	//note the -1, because an array is zero based and dataset is 1 based.
	 * }
	 *
	 * @param index index of column (1-based).
	 *
	 * @return String column name.
	 */
	public String js_getColumnName(int index)
	{
		if (set != null)
		{
			String[] columnNames = set.getColumnNames();
			if (columnNames != null && index > 0 && index <= columnNames.length)
			{
				return columnNames[index - 1];//all Javascript calls are 1 based
			}
		}
		return null;
	}

	/**
	 * Get the column names of a dataset.
	 *
	 * @sample
	 * var query = 'select * from customers';
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, null, 100);
	 * var columnNames = dataset.getColumnNames();
	 *
	 * @return String[] column names
	 */
	public String[] js_getColumnNames()
	{
		if (set != null)
		{
			String[] columnNames = set.getColumnNames();
			return columnNames;
		}
		return null;
	}

	/**
	 * Set a column name based on index.
	 *
	 * @sample
	 * var query = 'select customerid, customername from customers';
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()), query, null, -1);
	 * dataset.setColumnName(2, 'name_of_customer') // change the column name for second column.
	 *
	 * @param index index of column (1-based).
	 * @param columnName new column name.
	 */
	public void js_setColumnName(int index, String columnName)
	{
		if (set != null)
		{
			String[] columnNames = set.getColumnNames();
			if (columnNames != null && index > 0 && index <= columnNames.length)
			{
				set.setColumnName(index - 1, columnName); // all Javascript calls are 1 based
			}
		}
	}

	/**
	 * Get a column type based on index.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var firstColumnType = dataset.getColumnType(1) //retrieves the first column's type into the variable firstColumnType
	 * if (firstColumnType == JSColumn.NUMBER) { }
	 *
	 * @param index index of column (1-based).
	 *
	 * @return Number the column type (JSColumn constant)
	 * @since 6.1.4
	 */
	public Number js_getColumnType(int index)
	{
		if (set != null)
		{
			int[] types = set.getColumnTypes();
			if (types != null && index > 0 && index <= types.length)
			{
				return Integer.valueOf(Column.mapToDefaultType(types[index - 1]));//all Javascript calls are 1 based
			}
		}
		return null;
	}

	/**
	 * Get the column data of a dataset as an Array.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var dataArray = dataset.getColumnAsArray(1); //puts the contents from the first column of the dataset into an array
	 * //once you have it as an array you can loop through it or feed it to a custom valuelist for example
	 *
	 * @param index index of column (1-based).
	 *
	 * @return Object array of data.
	 */
	public Object[] js_getColumnAsArray(int index)
	{
		if (set != null)
		{
			if (index > 0 && index <= set.getColumnCount())
			{
				Object[] array = new Object[set.getRowCount()];
				for (int j = 0; j < set.getRowCount(); j++)
				{
					array[j] = set.getRow(j)[index - 1];//all Javascript calls are 1 based
				}
				return array;
			}
		}
		return null;
	}

	/**
	 * Get the row data of a dataset as an Array.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var dataArray = dataset.getRowAsArray(1); //puts the contents from the first row of the dataset into an array
	 * //once you have it as an array you can loop through it
	 *
	 * @param index index of row (1-based).
	 *
	 * @return Object array of data.
	 */
	public Object[] js_getRowAsArray(int index)
	{
		if (set != null)
		{
			if (index > 0 && index <= set.getRowCount())
			{
				return set.getRow(index - 1);
			}
		}
		return null;
	}

	private AbstractTableModel tableModelWrapper;

	/**
	 * Returns the dataset as a Swing tablemodel.
	 *
	 * @deprecated As of release 5.0, replaced by {@link #createDataSource(String, Object)}
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var model = dataset.getAsTableModel() //gets a Java/Swing tablemodel to be used in beans
	 * elements.inmemDatagridBean.setModel(model)
	 *
	 * @return TableModel
	 *
	 */
	@Deprecated
	public TableModel js_getAsTableModel()
	{
		if (tableModelWrapper == null)
		{
			tableModelWrapper = new DataModel();
		}
		return tableModelWrapper;
	}

	/**
	 * Create a datasource from the data set with specified name and using specified types.
	 *
	 * A temporary datasource cannot be removed because once created there may always be forms or relations that refer to it.
	 * When the client exits, all datasources used by that client are removed automatically.
	 *
	 * Most resources used by the datasource can be released by deleting all records:
	 *   dataset.removeRow(-1) or databaseManager.getFoundSet(datasource).deleteAllRecords()
	 *
	 * A datasource can be reused if the data has the same signature (column names and types).
	 * A new createDataSource() call will clear the datasource contents from a previous call and insert the current data.
	 *
	 * @sample
	 * ds.addColumn('my_id'); // note: use regular javascript identifiers so they can be used in scripting
	 * ds.addColumn('my_label');
	 * var uri = ds.createDataSource('mydata', [JSColumn.INTEGER, JSColumn.TEXT]);
	 * var jsform = solutionModel.newForm(fname, uri, null, true, 300, 300);
	 *
	 * var query = 'select customerid, address, city, country  from customers';
	 * var ds2 = databaseManager.getDataSetByQuery('example_data', query, null, 999);
	 * var uri2 = ds2.createDataSource('mydata2'); // types are inferred from query result
	 *
	 * @param name datasource name
	 *
	 * @param types array of types as defined in JSColumn
	 *
	 * @return String uri reference to the created datasource.
	 */
	public String js_createDataSource(String name, Object types) throws ServoyException
	{
		return js_createDataSource(name, types, null);
	}

	/**
	 * Create a datasource from the data set with specified name and using specified types.
	 *
	 * A temporary datasource cannot be removed because once created there may always be forms or relations that refer to it.
	 * When the client exits, all datasources used by that client are removed automatically.
	 *
	 * Most resources used by the datasource can be released by deleting all records:
	 *   dataset.removeRow(-1) or databaseManager.getFoundSet(datasource).deleteAllRecords()
	 *
	 * @sample
	 * ds.addColumn('my_id'); // note: use regular javascript identifiers so they can be used in scripting
	 * ds.addColumn('my_label');
	 * var uri = ds.createDataSource('mydata', [JSColumn.INTEGER, JSColumn.TEXT], ['my_id']);
	 * var jsform = solutionModel.newForm(fname, uri, null, true, 300, 300);
	 *
	 * var query = 'select customerid, address, city, country  from customers';
	 * var ds2 = databaseManager.getDataSetByQuery('example_data', query, null, 999);
	 * var uri2 = ds2.createDataSource('mydata2', null, ['customerid']); // types are inferred from query result, use customerid as pk
	 *
	 * @param name datasource name
	 *
	 * @param types array of types as defined in JSColumn, when null types are inferred from the query result
	 *
	 * @param pkNames array of pk names, when null a hidden pk-column will be added
	 *
	 * @return String uri reference to the created datasource.
	 */
	public String js_createDataSource(String name, Object types, String[] pkNames) throws ServoyException
	{
		if (set == null) return null;

		if (set instanceof FoundsetDataSet)
		{
			// already created a datasource for this data set.
			return ((FoundsetDataSet)set).getDataSource();
		}
		if (types instanceof Wrapper)
		{
			types = ((Wrapper)types).unwrap();
		}
		ColumnType[] columnTypes = null;
		if (types instanceof Object[])
		{
			columnTypes = new ColumnType[((Object[])types).length];
			for (int i = 0; i < ((Object[])types).length; i++)
			{
				columnTypes[i] = ColumnType.getColumnType(Utils.getAsInteger(((Object[])types)[i]));
			}
		}

		if (columnameMap == null)
		{
			// invent column names if none defined yet
			makeColumnMap();
		}
		String dataSource = application.getFoundSetManager().createDataSourceFromDataSet(name, set, columnTypes /* inferred from dataset when null */, pkNames,
			true);
		if (dataSource != null)
		{
			// create a new foundSet for the temp table
			IFoundSetInternal foundSet = application.getFoundSetManager().getSharedFoundSet(dataSource);
			foundSet.loadAllRecords();

			// wrap the new foundSet to redirect all IDataSet methods to the foundSet
			set = new FoundsetDataSet(foundSet, dataSource, pkNames);
		}
		return dataSource;
	}

	/**
	 * Create a datasource from the data set with specified name and using specified types.
	 * The types are inferred from the data if possible.
	 *
	 * A temporary datasource cannot be removed because once created there may always be forms or relations that refer to it.
	 * When the client exits, all datasources used by that client are removed automatically.
	 *
	 * Most resources used by the datasource can be released by deleting all records:
	 *   dataset.removeRow(-1) or databaseManager.getFoundSet(datasource).deleteAllRecords()
	 *
	 * @sample
	 * ds.addColumn('my_id'); // note: use regular javascript identifiers so they can be used in scripting
	 * ds.addColumn('my_label');
	 * var uri = ds.createDataSource('mydata', [JSColumn.INTEGER, JSColumn.TEXT]);
	 * var jsform = solutionModel.newForm(fname, uri, null, true, 300, 300);
	 *
	 * var query = 'select customerid, address, city, country  from customers';
	 * var ds2 = databaseManager.getDataSetByQuery('example_data', query, null, 999);
	 * var uri2 = ds2.createDataSource('mydata2'); // types are inferred from query result
	 *
	 * @param name datasource name
	 *
	 * @return String uri reference to the created datasource.
	 */
	public String js_createDataSource(String name) throws ServoyException
	{
		return js_createDataSource(name, null);
	}

	/**
	 * Get the dataset as an html table, do not escape values or spaces, no multi_line_markup, do not add indentation, add column names.
	 *
	 * @sampleas js_getAsHTML(Boolean, Boolean, Boolean, Boolean, Boolean)
	 *
	 * @return String html.
	 */
	public String js_getAsHTML()
	{
		return js_getAsHTML(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
	}

	/**
	 * Get the dataset as an html table, do not escape spaces, no multi_line_markup, do not add indentation, add column names.
	 *
	 * @sampleas js_getAsHTML(Boolean, Boolean, Boolean, Boolean, Boolean)
	 *
	 * @param escape_values if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @return String html.
	 */
	public String js_getAsHTML(Boolean escape_values)
	{
		return js_getAsHTML(escape_values, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
	}

	/**
	 * Get the dataset as an html table, no multi_line_markup, do not add indentation, add column names.
	 *
	 * @sampleas js_getAsHTML(Boolean, Boolean, Boolean, Boolean, Boolean)
	 *
	 * @param escape_values if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @param escape_spaces if true, replaces text spaces with non-breaking space tags ( ) and tabs by four non-breaking space tags.
	 *
	 * @return String html.
	 */
	public String js_getAsHTML(Boolean escape_values, Boolean escape_spaces)
	{
		return js_getAsHTML(escape_values, escape_spaces, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
	}

	/**
	 * Get the dataset as an html table, do not add indentation, add column names.
	 *
	 * @sampleas js_getAsHTML(Boolean, Boolean, Boolean, Boolean, Boolean)
	 *
	 * @param escape_values if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @param escape_spaces if true, replaces text spaces with non-breaking space tags ( ) and tabs by four non-breaking space tags.
	 *
	 * @param multi_line_markup if true, multiLineMarkup will enforce new lines that are in the text; single new lines will be replaced by <br>, multiple new lines will be replaced by <p>
	 *
	 * @return String html.
	 */
	public String js_getAsHTML(Boolean escape_values, Boolean escape_spaces, Boolean multi_line_markup)
	{
		return js_getAsHTML(escape_values, escape_spaces, multi_line_markup, Boolean.FALSE, Boolean.TRUE);
	}

	/**
	 * Get the dataset as an html table, add column names.
	 *
	 * @sampleas js_getAsHTML(Boolean, Boolean, Boolean, Boolean, Boolean)
	 *
	 * @param escape_values if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @param escape_spaces if true, replaces text spaces with non-breaking space tags ( ) and tabs by four non-breaking space tags.
	 *
	 * @param multi_line_markup if true, multiLineMarkup will enforce new lines that are in the text; single new lines will be replaced by <br>, multiple new lines will be replaced by <p>
	 *
	 * @param pretty_indent if true, adds indentation for more readable HTML code.
	 *
	 * @return String html.
	 */
	public String js_getAsHTML(Boolean escape_values, Boolean escape_spaces, Boolean multi_line_markup, Boolean pretty_indent)
	{
		return js_getAsHTML(escape_values, escape_spaces, multi_line_markup, pretty_indent, Boolean.TRUE);
	}


	/**
	 * Get the dataset as an html table.
	 *
	 * @sample
	 * //gets a dataset based on a query
	 * //useful to limit the number of rows
	 * var maxReturnedRows = 10;
	 * var query = 'select c1,c2,c3 from test_table where start_date = ?';
	 *
	 * //to access data by name, do not use '.' or special characters in names or aliases
	 * var args = new Array();
	 * args[0] = order_date //or new Date();
	 * var dataset = databaseManager.getDataSetByQuery(databaseManager.getDataSourceServerName(controller.getDataSource()),query,args,maxReturnedRows);
	 *
	 * // gets a dataset with escape values; escape spaces (lines will not wrap); no multi-line markup; with pretty indentation; shows column names
	 * var htmlTable = dataset.getAsHTML(true, true, false, true, true);
	 *
	 * //assigns the dataset to a field and sets the display type to HTML_AREA
	 * //assuming the html_field is a global text variable
	 * scopes.globals.html_field = '<html>'+dataset.getAsHTML()+'</html>';
	 *
	 * //Note: To display an HTML_AREA field as an HTML page, add HTML tags at the beginning '<html>' and at the end '</html>'.
	 *
	 * @param escape_values if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @param escape_spaces if true, replaces text spaces with non-breaking space tags ( ) and tabs by four non-breaking space tags.
	 *
	 * @param multi_line_markup if true, multiLineMarkup will enforce new lines that are in the text; single new lines will be replaced by <br>, multiple new lines will be replaced by <p>
	 *
	 * @param pretty_indent if true, adds indentation for more readable HTML code.
	 *
	 * @param add_column_names if false, column headers will not be added to the table.
	 *
	 * @return String html.
	 */
	public String js_getAsHTML(Boolean escape_values, Boolean escape_spaces, Boolean multi_line_markup, Boolean pretty_indent, Boolean add_column_names)
	{
		boolean _escape_values = Utils.getAsBoolean(escape_values);
		boolean _escape_spaces = Utils.getAsBoolean(escape_spaces);
		boolean _multi_line_markup = Utils.getAsBoolean(multi_line_markup);
		boolean _pretty_indent = Utils.getAsBoolean(pretty_indent);
		boolean _add_column_names = (add_column_names == null ? true : add_column_names.booleanValue());

		StringBuilder out = new StringBuilder();
		if (set != null)
		{
			if (htmlAttributes == null)
			{
				//-2=apply to container, -1=apply to all, x=apply to specific
				addHTMLProperty(-2, -2, "BORDER", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				addHTMLProperty(-2, -2, "CELLPADDING", "1"); //$NON-NLS-1$ //$NON-NLS-2$
				addHTMLProperty(-2, -2, "CELLSPACING", "0"); //$NON-NLS-1$ //$NON-NLS-2$
				addHTMLProperty(-1, -1, "class", "text"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			int numberOfColumns = set.getColumnCount();
			out.append("<TABLE "); //$NON-NLS-1$
			out.append(getHTMLProperties(-2, -2));
			out.append('>');
			String[] columnNames = set.getColumnNames();
			if (columnNames != null && _add_column_names)
			{
				if (_pretty_indent) out.append("\n\t"); //$NON-NLS-1$
				out.append("<TR"); //$NON-NLS-1$
				if (!Utils.stringIsEmpty(getHTMLProperties(-1, -1)))
				{
					out.append(' ');//any row
					out.append(getHTMLProperties(-1, -1));//any row
				}
				out.append('>');
				for (int x = 0; x < numberOfColumns; x++)
				{
					if (_pretty_indent) out.append("\n\t\t"); //$NON-NLS-1$
					out.append("<TD"); //$NON-NLS-1$
					if (!Utils.stringIsEmpty(getHTMLProperties(-1, x)))
					{
						out.append(' ');
						out.append(getHTMLProperties(-1, x));//specific column
					}
					out.append('>');
					out.append("<B>"); //$NON-NLS-1$
					if (_escape_values)
					{
						String val = HtmlUtils.escapeMarkup(columnNames[x], _escape_spaces).toString();
						if (_multi_line_markup)
						{
							out.append(Utils.toMultilineMarkup(val).toString());
						}
						else
						{
							out.append(val);
						}
					}
					else
					{
						out.append(columnNames[x]);
					}
					out.append("</B></TD>"); //$NON-NLS-1$
				}
				if (_pretty_indent) out.append("\n\t"); //$NON-NLS-1$
				out.append("</TR>"); //$NON-NLS-1$
			}
			for (int j = 0; j < set.getRowCount(); j++)
			{
				Object[] row = set.getRow(j);
				if (_pretty_indent) out.append("\n\t"); //$NON-NLS-1$
				out.append("<TR"); //$NON-NLS-1$
				if (!Utils.stringIsEmpty(getHTMLProperties(-1, -1)) || !Utils.stringIsEmpty(getHTMLProperties(j, -1)))
				{
					out.append(' ');
				}
				out.append(getHTMLProperties(-1, -1));//any row
				out.append(getHTMLProperties(j, -1));//specific row
				out.append('>');

				for (int x = 0; x < numberOfColumns; x++)
				{
					if (_pretty_indent) out.append("\n\t\t"); //$NON-NLS-1$
					out.append("<TD"); //$NON-NLS-1$
					if (!Utils.stringIsEmpty(getHTMLProperties(-1, x)) || !Utils.stringIsEmpty(getHTMLProperties(j, x)))
					{
						out.append(' ');
					}
					out.append(getHTMLProperties(-1, x));//specific column
					out.append(getHTMLProperties(j, x));//specific cell
					out.append('>');

					if (_escape_values)
					{
						String val = HtmlUtils.escapeMarkup("" + row[x], _escape_spaces).toString(); //$NON-NLS-1$
						if (_multi_line_markup)
						{
							out.append(Utils.toMultilineMarkup(val).toString());
						}
						else
						{
							out.append(val);
						}
					}
					else
					{
						out.append(row[x]);
					}
					out.append("</TD>"); //$NON-NLS-1$
				}
				if (_pretty_indent) out.append("\n\t"); //$NON-NLS-1$
				out.append("</TR>"); //$NON-NLS-1$
			}
			if (_pretty_indent) out.append('\n');
			out.append("</TABLE>"); //$NON-NLS-1$
		}
		return out.toString();
	}

	/**
	 * Get the dataset as formatted text.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * //you can create csv or tab delimited results
	 * var csv = dataset.getAsText(',','\n','"',true)
	 * var tab = dataset.getAsText('\t','\n','"',true)
	 *
	 * @param column_separator any specified column separator; examples: tab '\t'; comma ','; semicolon ';'; space ' ' .
	 *
	 * @param row_separator the specified row separator; examples: new line '\n'.
	 *
	 * @param value_delimiter the specified value delimiter; null means empty string; example: double quote '"'.
	 *
	 * @param add_column_names boolean if true column names will be added as a first row.
	 *
	 * @return String formatted text.
	 */
	public String js_getAsText(String column_separator, String row_separator, String value_delimiter, boolean add_column_names)
	{
		if (column_separator == null || row_separator == null) return null;

		if (value_delimiter == null) value_delimiter = ""; //$NON-NLS-1$

		StringBuilder out = new StringBuilder();
		if (set != null)
		{
			int numberOfColumns = set.getColumnCount();
			String[] columnNames = set.getColumnNames();
			if (add_column_names && columnNames != null)
			{
				for (int x = 0; x < numberOfColumns; x++)
				{
					out.append(value_delimiter);
					out.append(Utils.stringReplace(columnNames[x], value_delimiter, value_delimiter + value_delimiter));
					out.append(value_delimiter);
					if (x < numberOfColumns - 1) out.append(column_separator);
				}
				out.append(row_separator);
			}
			for (int j = 0; j < set.getRowCount(); j++)
			{
				for (int x = 0; x < numberOfColumns; x++)
				{
					out.append(value_delimiter);
					Object val = set.getRow(j)[x];
					out.append(Utils.stringReplace((val != null ? val.toString() : ""), value_delimiter, value_delimiter + value_delimiter)); //$NON-NLS-1$
					out.append(value_delimiter);
					if (x < numberOfColumns - 1) out.append(column_separator);
				}
				out.append(row_separator);
			}
		}
		return out.toString();
	}

	/**
	 * returns the contents of the database error message if an error occurred
	 *
	 * @deprecated As of release 5.0, replaced by {@link #getException()}
	 */
	@Deprecated
	public String js_getExceptionMsg()
	{
		return (exception == null ? null : exception.getMessage());
	}

	/**
	 * Get the database exception if an error occurred.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var dbException = dataset.getException();
	 *
	 * @return ServoyException exception or null when not available.
	 */
	public ServoyException js_getException()
	{
		return exception;
	}

	/**
	 * Get the value specified by row and column position from the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var dataAtRow2Col1 = dataset.getValue(2, 1);
	 *
	 * @param row row number, 1-based
	 *
	 * @param col column number, 1-based
	 *
	 * @return Object value
	 */
	public Object js_getValue(int row, int col)
	{
		return getValue(row - 1, col - 1);
	}

	public Object getValue(int r, int c)
	{
		if (set != null)
		{
			if (r >= 0 && r < set.getRowCount())
			{
				Object[] array = set.getRow(r);
				if (c >= 0 && c < array.length)
				{
					return array[c];
				}
			}
		}
		return null;
	}

	private Map<Pair<Integer, Integer>, Map<String, String>> htmlAttributes = null;

	/**
	 * Add an HTML property to an HTML tag produced in getAsHTML().
	 *
	 * For row and col parameters use:
	 * 1 = applies to the container
	 * 0 = applies to all
	 * >0 = applies to specific cell
	 *
	 * @sample
	 *
	 * //adds a container property (to TABLE tag)
	 * dataset.addHTMLProperty(-1,-1,'cellspacing','3');
	 * dataset.addHTMLProperty(-1,-1,'style','border-collapse:collapse;'); //to have a single line border
	 *
	 * //adds a row property to all rows (to TR tag)
	 * dataset.addHTMLProperty(0,0,'class','text');
	 *
	 * //adds a row property to second row (to TR tag)
	 * dataset.addHTMLProperty(2,0,'class','text');
	 *
	 * //adds a column property to all 3rd columns (to TD tag)
	 * dataset.addHTMLProperty(0,3,'class','redcolumn') ;
	 *
	 * //adds a specific cell property (to TD tag)
	 * dataset.addHTMLProperty(2,4,'color','blue');
	 *
	 * scopes.globals.html_field = '<html>'+dataset.getAsHTML()+'</html>';
	 *
	 * @param row row number
	 *
	 * @param col column number
	 *
	 * @param name String property name
	 *
	 * @param value String property value
	 */
	public void js_addHTMLProperty(int row, int col, String name, String value)//one based
	{
		addHTMLProperty(row - 1, col - 1, name, value);
	}

	public void addHTMLProperty(int rowIdent, int c, String pname, String pvalue)
	{
		if (htmlAttributes == null) htmlAttributes = new HashMap<Pair<Integer, Integer>, Map<String, String>>();
//		if (rowIdent instanceof Integer)
//		{
//			int r = Utils.getAsInteger(rowIdent);
//			if (r >= 0 && r < set.getRowCount())
//			{
//				rowIdent = set.getRow(r);
//			}
//		}
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(Integer.valueOf(rowIdent), Integer.valueOf(c));
		Map<String, String> value = htmlAttributes.get(key);
		if (value == null)
		{
			value = new LinkedHashMap<String, String>();
			htmlAttributes.put(key, value);
		}
		value.put(pname, pvalue);
	}

	private String getHTMLProperties(int rowIdent, int c)
	{
		StringBuilder sb = new StringBuilder();
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(Integer.valueOf(rowIdent), Integer.valueOf(c));
		Map<String, String> value = htmlAttributes.get(key);
		if (value != null)
		{
			Iterator<Map.Entry<String, String>> it = value.entrySet().iterator();
			while (it.hasNext())
			{
				Map.Entry<String, String> entry = it.next();
				sb.append(entry.getKey());
				Object val = entry.getValue();
				if (val != null)
				{
					sb.append("=\""); //$NON-NLS-1$
					sb.append(val);
					sb.append('"');
				}
				if (it.hasNext())
				{
					sb.append(' ');
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Set the value specified by row and column position from the dataset.
	 * Use row = -1, to set columnnames.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * dataset.setValue(2, 1,'data');
	 *
	 * @param row row number, 1-based
	 *
	 * @param col column number, 1-based
	 *
	 * @param obj the value to be stored at the given row and column.
	 */
	public void js_setValue(int row, int col, Object obj)//one based
	{
		setValue(row - 1, col - 1, obj);
		if (tableModelWrapper != null) tableModelWrapper.fireTableDataChanged();
	}

	public void setValue(int r, int c, Object val)
	{
		Object obj = val;
		if (set != null)
		{
			if (obj instanceof Wrapper)
			{
				obj = ((Wrapper)obj).unwrap();
			}
			if (r < 0)
			{
				if (obj == null) obj = ""; //$NON-NLS-1$
				String[] columnNames = set.getColumnNames();
				if (columnNames != null)
				{
					if (c >= 0 && c < columnNames.length)
					{
						columnNames[c] = obj.toString();
					}
					columnameMap = null;//clear to be rebuild
				}
			}
			if (r >= 0 && r < set.getRowCount())
			{
				Object[] array = set.getRow(r);
				if (c >= 0 && c < array.length)
				{
					array[c] = obj;
				}
				set.setRow(r, array);
			}
		}
	}

	/**
	 * Return true if there is more data in the resultset then specified by maxReturnedRows at query time.
	 *
	 * @sample
	 * var ds = databaseManager.getDataSetByQuery('example_data', 'select order_id from orders', null, 10000)
	 * if (ds.hadMoreData())
	 * {
	 * 	// handle large result
	 * }
	 * @return boolean more data available
	 */
	public boolean js_hadMoreData()
	{
		return set != null && set.hadMoreRows();
	}

	/**
	 * Sort the dataset on the given column (1-based) in ascending or descending.
	 *
	 * @sample
	 * // sort using column number
	 * //assuming the variable dataset contains a dataset
	 * dataset.sort(1, false)
	 *
	 * @param col column number, 1-based
	 *
	 * @param sort_direction boolean ascending (true) or descending (false)
	 */
	public void js_sort(Number col, Boolean sort_direction)
	{
		int _col = Utils.getAsInteger(col);
		boolean _sort_direction = Utils.getAsBoolean(sort_direction);

		if (set != null && _col > 0 && _col <= set.getColumnCount())
		{
			set.sort(_col - 1, _sort_direction);
		}
	}

	/**
	 * Sort the dataset using the function as comparator.
	 * The comparator function is called to compare two rows, that are passed as arguments, and
	 * it will return -1/0/1 if the first row is less/equal/greater then the second row.
	 *
	 * NOTE: starting with 7.2 release, when called on datasource(foundset) dataset, this function doesn't save the data anymore
	 *
	 * @sample
	 * //sort using comparator
	 * dataset.sort(mySortFunction);
	 *
	 * function mySortFunction(r1, r2)
	 * {
	 *	var o = 0;
	 *	if(r1[0] < r2[0])
	 *	{
	 *		o = -1;
	 *	}
	 *	else if(r1[0] > r2[0])
	 *	{
	 *		o = 1;
	 *	}
	 *	return o;
	 * }
	 *
	 * @param comparator comparator function
	 */
	public void js_sort(final Function comparator)
	{
		if (set != null && comparator != null)
		{
			final IExecutingEnviroment scriptEngine = application.getScriptEngine();
			final Scriptable rowComparatorScope = comparator.getParentScope();
			set.sort(new Comparator<Object[]>()
			{
				public int compare(Object[] o1, Object[] o2)
				{
					try
					{
						Object[] param1 = o1;
						Object[] param2 = o2;
						if (set instanceof FoundsetDataSet) // o1 and o2 are pks, get the raw data to pass to rowComparator
						{
							IFoundSetInternal foundset = ((FoundsetDataSet)set).getFoundSet();
							if (foundset instanceof FoundSet)
							{
								param1 = ((FoundSet)foundset).getRecord(o1).getRawData().getRawColumnData();
								param2 = ((FoundSet)foundset).getRecord(o2).getRawData().getRawColumnData();

								if (((FoundsetDataSet)set).pkNames == null)
								{
									// hide servoy internal pk column when pknames is null
									Object[] res = new Object[param1.length - 1];
									System.arraycopy(param1, 1, res, 0, res.length);
									param1 = res;

									res = new Object[param2.length - 1];
									System.arraycopy(param2, 1, res, 0, res.length);
									param2 = res;
								}
							}
						}
						Object compareResult = scriptEngine.executeFunction(comparator, rowComparatorScope, rowComparatorScope, new Object[] { param1, param2 },
							false, true);
						return Utils.getAsInteger(compareResult, true);
					}
					catch (Exception ex)
					{
						Debug.error(ex);
					}
					return 0;
				}
			});
		}
	}

	private Map<String, Integer> columnameMap;

	private Scriptable getNamedColumns(int index)
	{
		String[] colNamesSorted = getColumnNamesSorted();
		Object[] row = set.getRow(index - 1);
		Context cx = Context.enter();
		try
		{
			Scriptable ar = ScriptRuntime.newArrayLiteral(new Object[0], null, cx, this);
			for (int i = 0; i < row.length; i++)
			{
				String cname = colNamesSorted[i];
				ar.put("(" + (i + 1) + ") " + cname, ar, row[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ar;
		}
		finally
		{
			Context.exit();
		}
	}

	public Object get(String name, Scriptable start)
	{
		Object mobj = jsFunctions.get(name);
		if (mobj != null)
		{
			ScriptRuntime.setFunctionProtoAndParent((BaseFunction)mobj, start);
			return mobj;
		}
		if (set != null)
		{
			if ("rowIndex".equals(name)) //$NON-NLS-1$
			{
				return Integer.valueOf(js_getRowIndex());
			}
			if (columnameMap == null)
			{
				makeColumnMap();
			}
			Integer iindex = columnameMap.get(name);
			if (iindex != null)
			{
				if (set == null || set.getRowIndex() <= 0 || set.getRowIndex() > set.getRowCount())
				{
					return null;
				}
				Object[] array = set.getRow(set.getRowIndex() - 1);
				int index = iindex.intValue();
				if (index > 0 && index <= array.length)
				{
					Context cx = Context.enter();
					try
					{
						return array[index - 1] != null ? cx.getWrapFactory().wrap(cx, this, array[index - 1], array[index - 1].getClass()) : null;
					}
					finally
					{
						Context.exit();
					}
				}
			}
		}

		if (name.startsWith("row_") && set != null) //$NON-NLS-1$
		{
			int index;
			try
			{
				index = Integer.parseInt(name.substring(4));
			}
			catch (NumberFormatException e)
			{
				return Scriptable.NOT_FOUND;
			}
			return getNamedColumns(index);
		}

		if (name.endsWith("rowColumns")) //$NON-NLS-1$
		{
			return NativeJavaArray.wrap(this, getColumnNamesSorted());
		}

		if (name.endsWith("rowValue")) //$NON-NLS-1$
		{
			int selectedIndex = js_getRowIndex();
			if (selectedIndex != -1)
			{
				return getNamedColumns(selectedIndex);
			}
		}

		return Scriptable.NOT_FOUND;
	}

	public Object get(int index, Scriptable start)
	{
		if (set != null)
		{
			if (set.getRowIndex() > 0 && set.getRowIndex() <= set.getRowCount())
			{
				Object[] array = set.getRow(set.getRowIndex() - 1);
				if (index > 0 && index <= array.length)
				{
					return array[index - 1];
				}
			}
			else
			{
				Object[] array = set.getRow(index);
				if (array != null)
				{
					Context cx = Context.enter();
					try
					{
						Scriptable arrayScriptable = (Scriptable)cx.getWrapFactory().wrap(cx, start, array, Object[].class);
						String[] colNamesSorted = getColumnNamesSorted();

						for (int i = 0; i < colNamesSorted.length; i++)
						{
							String name = colNamesSorted[i];
							Object object = array[i];
							arrayScriptable.put(name, arrayScriptable, object != null ? cx.getWrapFactory().wrap(cx, start, object, object.getClass()) : null);
						}
						return arrayScriptable;
					}
					finally
					{
						Context.exit();
					}
				}
			}
		}
		return null;
	}

	private void makeColumnMap()
	{
		if (columnameMap == null) columnameMap = new HashMap<String, Integer>();
		else columnameMap.clear();
		if (set != null)
		{
			String[] columnNames = set.getColumnNames();
			if (columnNames != null)
			{
				for (int x = 0; x < columnNames.length; x++)
				{
					String name = columnNames[x];
					if (name == null)
					{
						name = "c" + (x + 1); //$NON-NLS-1$
						columnNames[x] = name;
					}
					else
					{
						name = name.toLowerCase();
					}
					columnameMap.put(name, Integer.valueOf(x + 1));
				}
			}
		}
		else
		{
			columnameMap.put("error", Integer.valueOf(1)); //$NON-NLS-1$
		}
	}

	public boolean has(String name, Scriptable start)
	{
		if ("rowIndex".equals(name)) return true; //$NON-NLS-1$

		if (columnameMap == null) makeColumnMap();
		return columnameMap.get(name) != null;
	}

	public void put(String name, Scriptable start, java.lang.Object value)
	{
		if (jsFunctions.containsKey(name)) return;//dont allow to set

		if (value instanceof Wrapper)
		{
			value = ((Wrapper)value).unwrap();
		}
		if ("rowIndex".equals(name)) //$NON-NLS-1$
		{
			js_setRowIndex(Utils.getAsInteger(value));
		}
		else
		{
			if (set != null)
			{
				if (columnameMap == null)
				{
					makeColumnMap();
				}
				Integer iindex = columnameMap.get(name);
				if (iindex != null)
				{
					int index = iindex.intValue();
					if (set != null && set.getRowIndex() > 0 && set.getRowIndex() <= set.getRowCount())
					{
						Object[] array = set.getRow(set.getRowIndex() - 1);
						if (index > 0 && index <= array.length)
						{
							array[index - 1] = value;
							set.setRow(set.getRowIndex() - 1, array);
							return;
						}
					}
				}
			}
		}
	}


	public String getClassName()
	{
		return "JSDataSet"; //$NON-NLS-1$
	}


	public Object[] getAllIds()
	{
		return getIds();
	}

	public Object[] getIds()
	{
		List<String> al = new ArrayList<String>();
		al.add("rowIndex"); //$NON-NLS-1$
		al.add("rowColumns"); //$NON-NLS-1$
		if (js_getRowIndex() != -1)
		{
			al.add("rowValue"); //$NON-NLS-1$
		}
		if (set != null)
		{
			int maxRows = set.getRowCount() > 1000 ? 999 : set.getRowCount();
			String format = maxRows < 10 ? "0" : maxRows < 100 ? "00" : "000"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			DecimalFormat df = new DecimalFormat(format);

			for (int i = 0; i < maxRows; i++)
			{
				al.add("row_" + df.format(i + 1)); //$NON-NLS-1$
			}
		}
		al.addAll(jsFunctions.keySet());
		return al.toArray();
	}


	public Object unwrap()
	{
		if (set != null)
		{
			return set;
		}
		if (exception != null)
		{
			return exception.toString();
		}
		return "<no result>"; //$NON-NLS-1$
	}

	@Override
	public String toString()
	{
		if (exception != null)
		{
			return "JSDataSet:exception:" + exception; //$NON-NLS-1$
		}
		return "JSDataSet:size:" + (set != null ? set.getRowCount() : 0) + ",selectedRow:" + (set != null ? set.getRowIndex() : -1); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IDataSet getDataSet()
	{
		return set;
	}

	public IDataSet getDelegate()
	{
		return set;
	}

	private String[] getColumnNamesSorted()
	{
		if (columnameMap == null) makeColumnMap();
		String[] colNamesSorted = new String[columnameMap.size()];
		for (Entry<String, Integer> column : columnameMap.entrySet())
		{
			colNamesSorted[column.getValue().intValue() - 1] = column.getKey();
		}
		return colNamesSorted;
	}

	private class DataModel extends AbstractTableModel
	{
		@Override
		public Class<Object> getColumnClass(int columnIndex)
		{
			return Object.class;
		}

		public int getColumnCount()
		{
			return js_getMaxColumnIndex();
		}

		@Override
		public String getColumnName(int columnIndex)
		{
			String cname = js_getColumnName(columnIndex + 1);
			if (cname == null) return ""; //$NON-NLS-1$
			return cname;
		}

		public int getRowCount()
		{
			return js_getMaxRowIndex();
		}

		public Object getValueAt(int rowIdx, int columnIndex)
		{
			return getValue(rowIdx, columnIndex);
		}

		@Override
		public boolean isCellEditable(int rowIdx, int columnIndex)
		{
			return true;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			setValue(rowIndex, columnIndex, aValue);
		}
	}


	/**
	 * Wrapper class for a foundset to implement IDataSet methods.
	 *
	 * @author rgansevles
	 *
	 */
	static class FoundsetDataSet implements IDataSetWithIndex, IFoundSetEventListener
	{
		private final IFoundSetInternal foundSet;
		private final String dataSource;

		private Integer rowCount = null;
		private final String[] pkNames;


		public FoundsetDataSet(IFoundSetInternal foundSet, String dataSource, String[] pkNames)
		{
			this.foundSet = foundSet;
			this.dataSource = dataSource;
			this.pkNames = pkNames;
			// note that this will also creates a reference from the foundSet to this FoundsetDataSet, when GC'ed both go at the same time
			foundSet.addFoundSetEventListener(this);
		}

		public IFoundSetInternal getFoundSet()
		{
			return foundSet;
		}

		@Override
		public FoundsetDataSet clone()
		{
			return new FoundsetDataSet(foundSet, dataSource, pkNames);
		}

		public String getDataSource()
		{
			return dataSource;
		}

		//////// IFoundSetEventListener methods ///////////

		public void foundSetChanged(FoundSetEvent e)
		{
			if (e.getType() == FoundSetEvent.CONTENTS_CHANGED)
			{
				// flush cached row count
				rowCount = null;
			}
		}

		//////// IDataSet methods ///////////

		public boolean addColumn(int columnIndex, String columnName, int columnType)
		{
			throw new UnsupportedOperationException("addColumn after createDataSource is not supported on data set"); //$NON-NLS-1$
		}

		public void setColumnName(int columnIndex, String columnName)
		{
			throw new UnsupportedOperationException("setColumnName after createDataSource is not supported on data set"); //$NON-NLS-1$
		}

		public void addRow(Object[] array)
		{
			addRow(-1, array);
		}

		public void addRow(int index, Object[] array)
		{
			int newIndex;
			try
			{
				newIndex = foundSet.newRecord(Integer.MAX_VALUE, true); // add at the end of the list
			}
			catch (ServoyException e)
			{
				Debug.log(e);
				return;
			}
			if (index != -1 && index != newIndex)
			{
				Debug.log("Warning: addRow index paramer after createDataSource call is ignored on data set"); //$NON-NLS-1$
			}
			setRow(newIndex, array);
		}

		public void clearHadMoreRows()
		{
		}

		public int getColumnCount()
		{
			int offset = pkNames == null ? 1 : 0;
			return foundSet.getTable().getColumnNames().length - offset; // hide servoy internal pk column when pknames is null
		}

		public String[] getColumnNames()
		{
			String[] columnNames = foundSet.getTable().getDataProviderIDs();
			if (pkNames != null)
			{
				return columnNames;
			}

			// hide servoy internal pk column when pknames is null
			String[] res = new String[columnNames.length - 1];
			System.arraycopy(columnNames, 1, res, 0, res.length);
			return res;
		}

		public int[] getColumnTypes()
		{
			ITable table = foundSet.getTable();
			String[] columnNames = table.getColumnNames();

			int offset = pkNames == null ? 1 : 0;

			// hide servoy internal pk column when pknames is null
			int[] res = new int[columnNames.length - offset];
			for (int i = offset; i < columnNames.length; i++)
			{
				res[i - offset] = table.getColumnType(columnNames[i]);
			}
			return res;
		}

		public Object[] getRow(int row)
		{
			IRecordInternal record = foundSet.getRecord(row);
			if (record == null)
			{
				return null;
			}
			String[] columnNames = foundSet.getTable().getDataProviderIDs();

			int offset = pkNames == null ? 1 : 0;
			// hide servoy internal pk column when pknames is null
			Object[] res = new Object[columnNames.length - offset];
			for (int i = offset; i < columnNames.length; i++)
			{
				res[i - offset] = record.getValue(columnNames[i]); // get value via record so that conversion is done
			}
			return res;
		}

		public int getRowCount()
		{
			if (rowCount == null)
			{
				// possibly expensive, so cache the value
				rowCount = Integer.valueOf(foundSet.getFoundSetManager().getFoundSetCount(foundSet));
			}
			return rowCount.intValue();
		}

		public boolean hadMoreRows()
		{
			return false;
		}

		@Override
		public List<Object[]> getRows()
		{
			int rc = getRowCount();
			List<Object[]> rows = new ArrayList<>();
			for (int i = 0; i < rc; i++)
			{
				rows.add(getRow(i));
			}
			return rows;
		}

		public boolean removeColumn(int columnIndex)
		{
			throw new UnsupportedOperationException("removeColumn after createDataSource is not supported on data set"); //$NON-NLS-1$
		}

		public void removeRow(int index)
		{
			try
			{
				if (index == -1)
				{
					foundSet.deleteAllRecords();
				}
				else
				{
					foundSet.deleteRecord(index);
				}
			}
			catch (ServoyException e)
			{
				Debug.log(e);
			}
		}

		public void setRow(int index, Object[] array)
		{
			IRecordInternal record = foundSet.getRecord(index);
			if (record.startEditing())
			{
				String[] columnNames = foundSet.getTable().getDataProviderIDs();

				int offset = pkNames == null ? 1 : 0;
				// hide servoy internal pk column when pknames is null
				for (int i = 0; i < array.length && (i + offset) < columnNames.length; i++)
				{
					record.setValue(columnNames[i + offset], array[i]);
				}
			}
		}

		public void sort(int column, boolean ascending)
		{
			int offset = pkNames == null ? 1 : 0;
			// hide servoy internal pk column when pknames is null
			IColumn c = (IColumn)((Table)foundSet.getTable()).getColumns().toArray()[column + offset];
			SortColumn sortColumn = new SortColumn(c);
			sortColumn.setSortOrder(ascending ? SortColumn.ASCENDING : SortColumn.DESCENDING);
			List<SortColumn> sortColumns = new ArrayList<SortColumn>(1);
			sortColumns.add(sortColumn);
			try
			{
				foundSet.sort(sortColumns, false);
			}
			catch (ServoyException e)
			{
				Debug.log(e);
			}
		}

		public void sort(Comparator<Object[]> rowComparator)
		{
			foundSet.sort(rowComparator);
		}

		@Override
		public int getRowIndex()
		{
			return foundSet.getSelectedIndex() + 1;
		}

		@Override
		public void setRowIndex(int rowIndex)
		{
			if (rowIndex >= 1)
			{
				foundSet.setSelectedIndex(rowIndex - 1);
			}
		}
	}

	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

	public void put(int index, Scriptable start, Object value)
	{
		// ignore
	}

	public void delete(String name)
	{
		// ignore
	}

	public void delete(int index)
	{
		// ignore
	}

	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	private Scriptable parentScope;

	public Scriptable getParentScope()
	{
		if (parentScope == null && application != null && application.getScriptEngine() != null)
		{
			return application.getScriptEngine().getSolutionScope();
		}
		return parentScope;
	}

	public void setParentScope(Scriptable parent)
	{
		parentScope = parent;
	}

	public Object getDefaultValue(Class< ? > hint)
	{
		return toString();
	}
}
