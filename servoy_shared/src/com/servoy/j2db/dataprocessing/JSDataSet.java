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


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.scripting.IExecutingEnviroment;
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
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSDataSet extends IdScriptableObject implements Wrapper, IDelegate<IDataSet>
{
	private static final long serialVersionUID = 1L;

	private static JSDataSet prototype;

	private static final int /* Id_constructor = 1, */Id_toString = 2, Id_getAsHTML = 3, Id_getColumnAsArray = 4, Id_getColumnName = 5, Id_getExceptionMsg = 6,
		Id_getMaxColumnIndex = 7, Id_getMaxRowIndex = 8, Id_getRowIndex = 9, Id_getValue = 10, Id_hadMoreData = 11, Id_setRowIndex = 12, Id_sort = 13,
		Id_getAsText = 14, Id_getRowAsArray = 15, Id_removeRow = 16, Id_getAsTableModel = 17, Id_setValue = 18, Id_addRow = 19, Id_getException = 20,
		Id_addHTMLProperty = 21, Id_addColumn = 22, Id_removeColumn = 23, Id_createDataSource = 24, Id_MAX = 24;


	private static final Object DATASET_TAG = new Object();

	static
	{
		prototype = new JSDataSet();
		prototype.activatePrototypeMap(Id_MAX);
	}

	private IDataSet set;
	private ServoyException exception;
	private int rowIndex = -1;//1 based !!

	private final IServiceProvider application;

	public JSDataSet() //only for use JS engine
	{
		this.application = null;
		this.set = new BufferedDataSet();
	}

	public JSDataSet(IServiceProvider application)
	{
		this(application, new BufferedDataSet());
	}

	public JSDataSet(IServiceProvider application, int rows, String[] cols)
	{
		this.application = application;
		if (rows >= 0 && cols.length >= 0)
		{
			List<Object[]> emptyRows = new ArrayList<Object[]>(rows);
			for (int i = 0; i < rows; i++)
			{
				emptyRows.add(new Object[cols.length]);
			}
			this.set = new BufferedDataSet(cols, emptyRows);
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
		this.set = set;
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
		set = null;
		exception = e;
	}

	/*
	 * @see org.mozilla.javascript.ScriptableObject#getPrototype()
	 */
	@Override
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
			return rowIndex;
		}
		return -1;
	}

	public void js_setRowIndex(int r)
	{
		if (set != null)
		{
			if (r > 0 && r <= set.getRowCount())
			{
				rowIndex = r;
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
	 * dataset.addRow(2, new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns after row 2
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
	 * dataset.addRow(2, new Array(1,2,3,4,5,6,7,7)); //adds a row with 8 columns after row 2
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
	 * adds a column with the specified name to the dataset.
	 *
	 * @sample
	 * //assuming the variable dataset contains a dataset
	 * var success = dataset.addColumn('columnName',1);
	 *
	 * @param name column name.
	 *
	 * @param index optional column index number between 1 and getMaxColumnIndex().
	 *
	 * @param type optional the type of column, see JSColumn constants.
	 * 
	 * @return true if succeeded, else false.
	 */
	public boolean js_addColumn(Object[] vargs)
	{
		if (vargs.length == 0 || set == null) return false;

		String columnName = ScriptRuntime.toString(vargs[0]);
		int columnIndex = -1;
		if (vargs.length > 1)
		{
			columnIndex = Utils.getAsInteger(vargs[1]) - 1;//all Javascript calls are 1 based
		}
		int columnType = 0;
		if (vargs.length > 2)
		{
			columnType = Utils.getAsInteger(vargs[2]);
		}

		boolean result = set.addColumn(columnIndex, columnName, columnType);
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
					if (pair.getLeft().intValue() > index) pair.setLeft(new Integer(pair.getLeft().intValue() + plus_minus));
				}
				else
				{
					//col
					if (pair.getRight().intValue() > index) pair.setRight(new Integer(pair.getRight().intValue() + plus_minus));
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
	 * Create a data source from the data set with specified name and using specified types.
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
	 * @param name data source name
	 * 
	 * @param types array of types as defined in JSColumn
	 * 
	 * @return String uri reference to the created data source.
	 */
	public String js_createDataSource(String name, Object types) throws ServoyException
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
		int[] intTypes = null;
		if (types instanceof Object[])
		{
			intTypes = new int[((Object[])types).length];
			for (int i = 0; i < ((Object[])types).length; i++)
			{
				intTypes[i] = Utils.getAsInteger(((Object[])types)[i]);
			}
		}

		String dataSource = application.getFoundSetManager().createDataSourceFromDataSet(name, set, intTypes /* inferred from dataset when null */);
		if (dataSource != null)
		{
			// create a new foundSet for the temp table
			IFoundSetInternal foundSet = application.getFoundSetManager().getSharedFoundSet(dataSource, null);
			foundSet.loadAllRecords();

			// wrap the new foundSet to redirect all IDataSet methods to the foundSet
			set = new FoundsetDataSet(foundSet, dataSource);
		}
		return dataSource;
	}

	/**
	 * Create a data source from the data set with specified name and using specified types.
	 * The types are inferred from the data if possible.
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
	 * @param name data source name
	 * 
	 * @return String uri reference to the created data source.
	 */
	public String js_createDataSource(String name) throws ServoyException
	{
		return js_createDataSource(name, null);
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
	 * @param escape_values optional if true, replaces illegal HTML characters with corresponding valid escape sequences.
	 *
	 * @param escape_spaces optional if true, replaces text spaces with non-breaking space tags ( ) and tabs by four non-breaking space tags.
	 *
	 * @param multi_line_markup optional if true, multiLineMarkup will enforce new lines that are in the text; single new lines will be replaced by <br>, multiple new lines will be replaced by <p>
	 *
	 * @param pretty_indent optional if true, adds indentation for more readable HTML code.
	 *
	 * @param add_column_names optional if false, column headers will not be added to the table.
	 * 
	 * @return String html.
	 */
	public String js_getAsHTML(Object[] vargs)
	{
		boolean escapeValues = false;
		boolean escapeSpaces = false;
		boolean multiLineMarkup = false;
		boolean prettyIndent = false;
		boolean addColumnNames = true;
		if (vargs != null && vargs.length > 0)
		{
			if (vargs.length >= 1) escapeValues = Utils.getAsBoolean(vargs[0]);
			if (vargs.length >= 2) escapeSpaces = Utils.getAsBoolean(vargs[2]);
			if (vargs.length >= 3) multiLineMarkup = Utils.getAsBoolean(vargs[2]);
			if (vargs.length >= 4) prettyIndent = Utils.getAsBoolean(vargs[3]);
			if (vargs.length >= 5) addColumnNames = Utils.getAsBoolean(vargs[4]);
		}

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
			if (columnNames != null && addColumnNames)
			{
				if (prettyIndent) out.append("\n\t"); //$NON-NLS-1$
				out.append("<TR"); //$NON-NLS-1$
				if (!Utils.stringIsEmpty(getHTMLProperties(-1, -1)))
				{
					out.append(' ');//any row					
					out.append(getHTMLProperties(-1, -1));//any row					
				}
				out.append('>');
				for (int x = 0; x < numberOfColumns; x++)
				{
					if (prettyIndent) out.append("\n\t\t"); //$NON-NLS-1$
					out.append("<TD"); //$NON-NLS-1$
					if (!Utils.stringIsEmpty(getHTMLProperties(-1, x)))
					{
						out.append(' ');
						out.append(getHTMLProperties(-1, x));//specific column
					}
					out.append('>');
					out.append("<B>"); //$NON-NLS-1$
					if (escapeValues)
					{
						String val = HtmlUtils.escapeMarkup(columnNames[x], escapeSpaces).toString();
						if (multiLineMarkup == false)
						{
							out.append(val);
						}
						else
						{
							out.append(Utils.toMultilineMarkup(val).toString());
						}
					}
					else
					{
						out.append(columnNames[x]);
					}
					out.append("</B></TD>"); //$NON-NLS-1$
				}
				if (prettyIndent) out.append("\n\t"); //$NON-NLS-1$
				out.append("</TR>"); //$NON-NLS-1$
			}
			for (int j = 0; j < set.getRowCount(); j++)
			{
				Object[] row = set.getRow(j);
				if (prettyIndent) out.append("\n\t"); //$NON-NLS-1$
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
					if (prettyIndent) out.append("\n\t\t"); //$NON-NLS-1$
					out.append("<TD"); //$NON-NLS-1$
					if (!Utils.stringIsEmpty(getHTMLProperties(-1, x)) || !Utils.stringIsEmpty(getHTMLProperties(j, x)))
					{
						out.append(' ');
					}
					out.append(getHTMLProperties(-1, x));//specific column
					out.append(getHTMLProperties(j, x));//specific cell
					out.append('>');

					if (escapeValues)
					{
						String val = HtmlUtils.escapeMarkup("" + row[x], escapeSpaces).toString(); //$NON-NLS-1$
						if (multiLineMarkup == false)
						{
							out.append(val);
						}
						else
						{
							out.append(Utils.toMultilineMarkup(val).toString());
						}
					}
					else
					{
						out.append(row[x]);
					}
					out.append("</TD>"); //$NON-NLS-1$
				}
				if (prettyIndent) out.append("\n\t"); //$NON-NLS-1$
				out.append("</TR>"); //$NON-NLS-1$
			}
			if (prettyIndent) out.append('\n');
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
	 * @param value_delimiter the specified value delimiter; example: double quote '"'.
	 *
	 * @param add_column_names boolean if true column names will be added as a first row. 
	 * 
	 * @return String formatted text.
	 */
	public String js_getAsText(String column_separator, String row_separator, String value_delimiter, boolean add_column_names)
	{
		if (column_separator == null || row_separator == null || value_delimiter == null) return null;

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
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(new Integer(rowIdent), new Integer(c));
		Map<String, String> value = htmlAttributes.get(key);
		if (value == null)
		{
			value = new HashMap<String, String>();
			htmlAttributes.put(key, value);
		}
		value.put(pname, pvalue);
	}

	private String getHTMLProperties(int rowIdent, int c)
	{
		StringBuilder sb = new StringBuilder();
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(new Integer(rowIdent), new Integer(c));
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
	 * dataset.getValue(2, 1,'data');
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
	 * Sort the dataset on the given column in ascending or descending, or
	 * sort the dataset using a comparator function.
	 * 
	 * If the first argument is a number, sort the dataset on the given column index (1-based),
	 * using the second argument as sort order (true for ascending, false for descending)
	 * 
	 * If the first argument is a function, sort the dataset using the function as comparator.
	 * The comparator function is called to compare two rows, that are passed as arguments, and
	 * it will return -1/0/1 if the first row is less/equal/greater then the second row.
	 *
	 * @sample
	 * // sort using column number
	 * //assuming the variable dataset contains a dataset
	 * dataset.sort(1,false)
	 * 
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
	 * @param col/comparator column number, 1-based or comparator function
	 * 
	 * @param sort_direction optional boolean used only if the first argument is a column number 
	 */
	public void js_sort(Object[] vargs)
	{
		if (vargs == null || vargs.length == 0) return;
		if (vargs[0] instanceof Function)
		{
			sort((Function)vargs[0]);
		}
		else if (vargs.length == 2)
		{
			int columnIndex = (int)ScriptRuntime.toInteger(vargs[0]);
			boolean ascending = ScriptRuntime.toBoolean(vargs[1]);

			sort(columnIndex, ascending);
		}
	}

	public void sort(int col, boolean sort_direction)
	{
		if (set != null && col > 0 && col <= set.getColumnCount())
		{
			set.sort(col - 1, sort_direction);
		}
	}

	public void sort(final Function rowComparator)
	{
		if (set != null && rowComparator != null)
		{
			final IExecutingEnviroment scriptEngine = application.getScriptEngine();
			final Scriptable rowComparatorScope = rowComparator.getParentScope();
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
								// hide servoy internal pk column
								Object[] res = new Object[param1.length - 1];
								System.arraycopy(param1, 1, res, 0, res.length);
								param1 = res;

								param2 = ((FoundSet)foundset).getRecord(o2).getRawData().getRawColumnData();
								// hide servoy internal pk column
								res = new Object[param2.length - 1];
								System.arraycopy(param2, 1, res, 0, res.length);
								param2 = res;
							}
						}
						Object compareResult = scriptEngine.executeFunction(rowComparator, rowComparatorScope, rowComparatorScope,
							new Object[] { param1, param2 }, false, true);
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
		if (columnameMap == null) makeColumnMap();
		TreeMap<Integer, String> colNamesSorted = new TreeMap<Integer, String>();
		for (String cname : columnameMap.keySet())
		{
			Integer cindex = columnameMap.get(cname);
			colNamesSorted.put(cindex, cname);
		}
		Object[] row = set.getRow(index - 1);
		Context cx = Context.enter();
		try
		{
			Scriptable ar = ScriptRuntime.newArrayLiteral(new Object[0], null, cx, this);
			for (int i = 0; i < row.length; i++)
			{
				String cname = colNamesSorted.get(new Integer(i + 1));
				ar.put("(" + (i + 1) + ") " + cname, ar, row[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return ar;
		}
		finally
		{
			Context.exit();
		}
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (getPrototype() == null)
		{
			return super.get(name, start);
		}

		if (set != null)
		{
			if ("rowIndex".equals(name)) //$NON-NLS-1$
			{
				return new Integer(js_getRowIndex());
			}
			if (columnameMap == null)
			{
				makeColumnMap();
			}
			Integer iindex = columnameMap.get(name);
			if (iindex != null)
			{
				int index = iindex.intValue();
				if (set != null && rowIndex > 0 && rowIndex <= set.getRowCount())
				{
					Object[] array = set.getRow(rowIndex - 1);
					if (index > 0 && index <= array.length)
					{
						return array[index - 1];
					}
				}
				else
				{
					return null;
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
			if (columnameMap == null)
			{
				makeColumnMap();
			}
			TreeMap<Integer, String> colNamesSorted = new TreeMap<Integer, String>();
			for (String cname : columnameMap.keySet())
			{
				Integer cindex = columnameMap.get(cname);
				colNamesSorted.put(cindex, cname);
			}
			return NativeJavaArray.wrap(this, colNamesSorted.values().toArray());
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

	@Override
	public Object get(int index, Scriptable start)
	{
		if (set != null)
		{
			if (rowIndex > 0 && rowIndex <= set.getRowCount())
			{
				Object[] array = set.getRow(rowIndex - 1);
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
						Scriptable arrayScriptable = ScriptRuntime.newArrayLiteral(array, null, cx, this);
						if (columnameMap == null)
						{
							makeColumnMap();
						}
						Iterator<Entry<String, Integer>> iterator = columnameMap.entrySet().iterator();
						while (iterator.hasNext())
						{
							Entry<String, Integer> entry = iterator.next();
							arrayScriptable.put(entry.getKey(), arrayScriptable, array[entry.getValue().intValue() - 1]);
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

	/**
	 * @see org.mozilla.javascript.IdScriptableObject#findPrototypeId(java.lang.String)
	 */
	@Override
	protected int findPrototypeId(String name)
	{
		if (name.equals("toString")) //$NON-NLS-1$
		{
			return Id_toString;
		}
		if (name.equals("getAsHTML")) //$NON-NLS-1$
		{
			return Id_getAsHTML;
		}
		if (name.equals("getColumnAsArray")) //$NON-NLS-1$
		{
			return Id_getColumnAsArray;
		}
		if (name.equals("getColumnName")) //$NON-NLS-1$
		{
			return Id_getColumnName;
		}
		if (name.equals("getExceptionMsg")) //$NON-NLS-1$
		{
			return Id_getExceptionMsg;
		}
		if (name.equals("getMaxColumnIndex")) //$NON-NLS-1$
		{
			return Id_getMaxColumnIndex;
		}
		if (name.equals("getMaxRowIndex")) //$NON-NLS-1$
		{
			return Id_getMaxRowIndex;
		}
		if (name.equals("getRowIndex")) //$NON-NLS-1$
		{
			return Id_getRowIndex;
		}
		if (name.equals("getValue")) //$NON-NLS-1$
		{
			return Id_getValue;
		}
		if (name.equals("hadMoreData")) //$NON-NLS-1$
		{
			return Id_hadMoreData;
		}
		if (name.equals("setRowIndex")) //$NON-NLS-1$
		{
			return Id_setRowIndex;
		}
		if (name.equals("sort")) //$NON-NLS-1$
		{
			return Id_sort;
		}
		if (name.equals("getAsText")) //$NON-NLS-1$
		{
			return Id_getAsText;
		}
		if (name.equals("getRowAsArray")) //$NON-NLS-1$
		{
			return Id_getRowAsArray;
		}
		if (name.equals("removeRow")) //$NON-NLS-1$
		{
			return Id_removeRow;
		}
		if (name.equals("getAsTableModel")) //$NON-NLS-1$
		{
			return Id_getAsTableModel;
		}
		if (name.equals("createDataSource")) //$NON-NLS-1$
		{
			return Id_createDataSource;
		}
		if (name.equals("setValue")) //$NON-NLS-1$
		{
			return Id_setValue;
		}
		if (name.equals("addRow")) //$NON-NLS-1$
		{
			return Id_addRow;
		}
		if (name.equals("addHTMLProperty")) //$NON-NLS-1$
		{
			return Id_addHTMLProperty;
		}
		if (name.equals("getException")) //$NON-NLS-1$
		{
			return Id_getException;
		}
		if (name.equals("addColumn")) //$NON-NLS-1$
		{
			return Id_addColumn;
		}
		if (name.equals("removeColumn")) //$NON-NLS-1$
		{
			return Id_removeColumn;
		}
		return 0;
	}

	/**
	 * @see org.mozilla.javascript.IdScriptableObject#initPrototypeId(int)
	 */
	@Override
	protected void initPrototypeId(int id)
	{
		String name;
		int arity;
		switch (id)
		{
			case Id_toString :
				name = "toString"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_getAsHTML :
				name = "getAsHTML"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_getColumnAsArray :
				name = "getColumnAsArray"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_getColumnName :
				name = "getColumnName"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_getExceptionMsg :
				name = "getExceptionMsg"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_getMaxColumnIndex :
				name = "getMaxColumnIndex"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_getMaxRowIndex :
				name = "getMaxRowIndex"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_getRowIndex :
				name = "getRowIndex"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_getValue :
				name = "getValue"; //$NON-NLS-1$
				arity = 2;
				break;
			case Id_hadMoreData :
				name = "hadMoreData"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_setRowIndex :
				name = "setRowIndex"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_sort :
				name = "sort"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_getAsText :
				name = "getAsText"; //$NON-NLS-1$
				arity = 4;
				break;
			case Id_getRowAsArray :
				name = "getRowAsArray"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_removeRow :
				name = "removeRow"; //$NON-NLS-1$
				arity = 1;
				break;
			case Id_getAsTableModel :
				name = "getAsTableModel"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_createDataSource :
				name = "createDataSource"; //$NON-NLS-1$
				arity = 2;
				break;
			case Id_setValue :
				name = "setValue"; //$NON-NLS-1$
				arity = 3;
				break;
			case Id_addRow :
				name = "addRow"; //$NON-NLS-1$
				arity = 2;
				break;
			case Id_addHTMLProperty :
				name = "addHTMLProperty"; //$NON-NLS-1$
				arity = 4;
				break;
			case Id_getException :
				name = "getException"; //$NON-NLS-1$
				arity = 0;
				break;
			case Id_addColumn :
				name = "addColumn"; //$NON-NLS-1$
				arity = 2;
				break;
			case Id_removeColumn :
				name = "removeColumn"; //$NON-NLS-1$
				arity = 1;
				break;
			default :
				throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(DATASET_TAG, id, name, arity);
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
					columnameMap.put(name, new Integer(x + 1));
				}
			}
		}
		else
		{
			columnameMap.put("error", new Integer(1)); //$NON-NLS-1$
		}
	}

	/*
	 * @see org.mozilla.javascript.IdScriptable#has(java.lang.String, org.mozilla.javascript.Scriptable)
	 */
	@Override
	public boolean has(String name, Scriptable start)
	{
		if ("rowIndex".equals(name)) return true; //$NON-NLS-1$
		if (columnameMap == null) makeColumnMap();
		if (columnameMap.get(name) != null) return true;
		return super.has(name, start);
	}

	@Override
	public void put(java.lang.String name, Scriptable start, java.lang.Object value)
	{
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
					if (set != null && rowIndex > 0 && rowIndex <= set.getRowCount())
					{
						Object[] array = set.getRow(rowIndex - 1);
						if (index > 0 && index <= array.length)
						{
							array[index - 1] = value;
							set.setRow(rowIndex - 1, array);
							return;
						}
					}
				}
			}
			super.put(name, start, value);
		}
	}


	@Override
	public String getClassName()
	{
		return "JSDataSet"; //$NON-NLS-1$
	}


	@Override
	public Object[] getAllIds()
	{
		return getIds();
	}

	@Override
	public Object[] getIds()
	{
//		if (columnameMap == null)
//		{
//			makeColumnMap();
//		}
//		return columnameMap.keySet().toArray();
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
		return "JSDataSet:size:" + (set != null ? set.getRowCount() : 0) + ",selectedRow:" + rowIndex; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IDataSet getDataSet()
	{
		return set;
	}

	public IDataSet getDelegate()
	{
		return set;
	}

	/*
	 * @see org.mozilla.javascript.IdScriptable#execMethod(int, org.mozilla.javascript.IdFunction, org.mozilla.javascript.Context,
	 * org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
	{
		JSDataSet dataset = (JSDataSet)thisObj;
		int methodId = f.methodId();
		try
		{
			switch (methodId)
			{
				case Id_toString :
					return dataset.toString();
				case Id_setValue :
					int rowIdx = (int)ScriptRuntime.toInteger(args[0]);
					int columnIndex = (int)ScriptRuntime.toInteger(args[1]);
					Object obj = (args.length >= 3 ? args[2] : null);
					if (obj instanceof Wrapper)
					{
						obj = ((Wrapper)obj).unwrap();
					}
					dataset.js_setValue(rowIdx, columnIndex, obj);
					return ScriptRuntime.NaNobj;
				case Id_addHTMLProperty :
					rowIdx = (int)ScriptRuntime.toInteger(args[0]);
					columnIndex = (int)ScriptRuntime.toInteger(args[1]);
					dataset.js_addHTMLProperty(rowIdx, columnIndex, ScriptRuntime.toString(args[2]), ScriptRuntime.toString(args[3]));
					return ScriptRuntime.NaNobj;
				case Id_getAsTableModel :
					return cx.getWrapFactory().wrap(cx, scope, dataset.js_getAsTableModel(), null);
				case Id_createDataSource :
					return dataset.js_createDataSource(ScriptRuntime.toString(args[0]), args.length > 1 ? args[1] : null);
				case Id_getAsHTML :
					return dataset.js_getAsHTML(args);
				case Id_getAsText :
					return dataset.js_getAsText(ScriptRuntime.toString(args[0]), ScriptRuntime.toString(args[1]), ScriptRuntime.toString(args[2]),
						ScriptRuntime.toBoolean(args[3]));
				case Id_getColumnAsArray :
					columnIndex = (int)ScriptRuntime.toInteger(args[0]);
					return cx.getWrapFactory().wrap(cx, scope, dataset.js_getColumnAsArray(columnIndex), Object[].class);
				case Id_getColumnName :
					columnIndex = (int)ScriptRuntime.toInteger(args[0]);
					return dataset.js_getColumnName(columnIndex);
				case Id_getExceptionMsg :
					return dataset.js_getExceptionMsg();
				case Id_getException :
					return dataset.js_getException();
				case Id_getMaxColumnIndex :
					return new Integer(dataset.js_getMaxColumnIndex());
				case Id_getMaxRowIndex :
					return new Integer(dataset.js_getMaxRowIndex());
				case Id_getRowIndex :
					return new Integer(dataset.js_getRowIndex());
				case Id_getValue :
					rowIdx = (int)ScriptRuntime.toInteger(args[0]);
					columnIndex = (int)ScriptRuntime.toInteger(args[1]);
					return cx.getWrapFactory().wrap(cx, scope, dataset.js_getValue(rowIdx, columnIndex), null);
				case Id_hadMoreData :
					return dataset.js_hadMoreData() ? Boolean.TRUE : Boolean.FALSE;
				case Id_setRowIndex :
					columnIndex = (int)ScriptRuntime.toInteger(args[0]);
					dataset.js_setRowIndex(columnIndex);
					return ScriptRuntime.NaNobj;
				case Id_sort :
					dataset.js_sort(args);
					return ScriptRuntime.NaNobj;
				case Id_getRowAsArray :
					rowIdx = (int)ScriptRuntime.toInteger(args[0]);
					return cx.getWrapFactory().wrap(cx, scope, dataset.js_getRowAsArray(rowIdx), Object[].class);
				case Id_removeRow :
					rowIdx = (int)ScriptRuntime.toInteger(args[0]);
					dataset.js_removeRow(rowIdx);
					return ScriptRuntime.NaNobj;
				case Id_addRow :
					Object argData = args[args.length - 1];
					if (argData instanceof Wrapper)
					{
						argData = ((Wrapper)argData).unwrap();
					}
					if (argData instanceof Object[])
					{
						Object[] wrappedData = (Object[])argData;
						Object[] unwrappedData = new Object[wrappedData.length];
						for (int i = 0; i < wrappedData.length; i++)
						{
							if (wrappedData[i] instanceof Wrapper)
							{
								unwrappedData[i] = ((Wrapper)wrappedData[i]).unwrap();
							}
							else
							{
								unwrappedData[i] = wrappedData[i];
							}
						}
						if (args.length == 1)
						{
							dataset.js_addRow(unwrappedData);
						}
						else if (args.length == 2)
						{
							rowIdx = (int)ScriptRuntime.toInteger(args[0]);
							dataset.js_addRow(rowIdx, unwrappedData);
						}
					}
					return ScriptRuntime.NaNobj;
				case Id_addColumn :
					return Boolean.valueOf(dataset.js_addColumn(args));
				case Id_removeColumn :
					if (args.length == 1)
					{
						return Boolean.valueOf(dataset.js_removeColumn((int)ScriptRuntime.toInteger(args[0])));
					}
					return Boolean.FALSE;
			}
		}
		catch (ServoyException e)
		{
			Debug.error(e);
			throw new RuntimeException(e.getMessage());
		}
		return super.execIdCall(f, cx, scope, thisObj, args);
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
	static class FoundsetDataSet implements IDataSet, IFoundSetEventListener
	{
		private final IFoundSetInternal foundSet;
		private final String dataSource;

		private Integer rowCount = null;


		public FoundsetDataSet(IFoundSetInternal foundSet, String dataSource)
		{
			this.foundSet = foundSet;
			this.dataSource = dataSource;
			// note that this will also creates a reference from the foundSet to this FoundsetDataSet, when GC'ed both go at the same time
			foundSet.addFoundSetEventListener(this);
		}

		public IFoundSetInternal getFoundSet()
		{
			return foundSet;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#clone()
		 */
		@Override
		public FoundsetDataSet clone()
		{
			return new FoundsetDataSet(foundSet, dataSource);
		}

		public String getDataSource()
		{
			return dataSource;
		}

//		@Override
//		protected void finalize() throws Throwable
//		{
//			// drop the temp table
//			IServiceProvider application = foundSet.getFoundSetManager().getApplication();
//			ITable table = foundSet.getTable();
//			application.getDataServer().dropTemporaryTable(application.getClientID(), table.getServerName(), table.getName());
//		}

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
			return foundSet.getTable().getColumnNames().length - 1; // hide servoy internal pk column
		}

		public String[] getColumnNames()
		{
			String[] columnNames = foundSet.getTable().getColumnNames();
			// hide servoy internal pk column
			String[] res = new String[columnNames.length - 1];
			System.arraycopy(columnNames, 1, res, 0, res.length);
			return res;
		}

		public int[] getColumnTypes()
		{
			ITable table = foundSet.getTable();
			String[] columnNames = table.getColumnNames();
			// hide servoy internal pk column
			int[] res = new int[columnNames.length - 1];
			for (int i = 1; i < columnNames.length; i++)
			{
				res[i - 1] = table.getColumnType(columnNames[i]);
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
			String[] columnNames = foundSet.getTable().getColumnNames();
			// hide servoy internal pk column
			Object[] res = new Object[columnNames.length - 1];
			for (int i = 1; i < columnNames.length; i++)
			{
				res[i - 1] = record.getValue(columnNames[i]); // get value via record so that conversion is done
			}
			return res;
		}

		public int getRowCount()
		{
			if (rowCount == null)
			{
				// possibly expensive, so cache the value
				rowCount = new Integer(foundSet.getFoundSetManager().getFoundSetCount(foundSet));
			}
			return rowCount.intValue();
		}

		public boolean hadMoreRows()
		{
			return false;
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
				String[] columnNames = foundSet.getTable().getColumnNames();

				// hide servoy internal pk column
				for (int i = 0; i < array.length && (i + 1) < columnNames.length; i++)
				{
					record.setValue(columnNames[i + 1], array[i]);
				}
			}
		}

		public void sort(int column, boolean ascending)
		{
			// hide servoy internal pk column
			IColumn c = (IColumn)((Table)foundSet.getTable()).getColumns().toArray()[column + 1];
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
	}
}
