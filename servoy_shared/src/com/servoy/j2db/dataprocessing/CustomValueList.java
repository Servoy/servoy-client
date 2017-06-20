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


import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.model.OptimizedDefaultListModel;

/**
 * user defined fixed valuelist.
 *
 * The {@link #indexOf(Object)} and {@link #realValueIndexOf(Object)} can return values between -2 and greater negative values
 * if this valuelist has a fallback valuelist.
 *
 * Then {@link #getElementAt(int)} and {@link #getRealElementAt(int)} do support those value to get the value from the fallback valuelist.
 *
 *
 * @author jblok
 */
public class CustomValueList extends OptimizedDefaultListModel implements IValueList
{
	public static class DisplayString
	{
		private final List<String> strings;
		private final String separator;

		private String completeString;

		private DisplayString(String separator)
		{
			this.separator = separator;
			strings = new ArrayList<String>();
		}

		private DisplayString append(String displayString)
		{
			completeString = null;
			strings.add(displayString);
			return this;
		}

		private DisplayString set(String displayString)
		{
			completeString = null;
			strings.clear();
			strings.add(displayString);
			return this;
		}

		private void makeString()
		{
			if (completeString == null)
			{
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < strings.size(); i++)
				{
					sb.append(strings.get(i));
					if (i < strings.size() - 1) sb.append(separator);
				}
				completeString = sb.toString();
			}
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			makeString();
			if (obj instanceof DisplayString)
			{
				return completeString.equals(obj.toString());
			}
			return completeString.equals(obj);
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			makeString();
			return completeString.hashCode();
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			makeString();
			return completeString;
		}

		/**
		 * @param txt
		 * @return
		 */
		public boolean startsWith(String txt)
		{
			if (strings.size() == 1)
			{
				return strings.get(0).toLowerCase().startsWith(txt);
			}
			String[] displayValues = Utils.stringSplit(txt, separator);
			for (int i = 0; i < strings.size(); i++)
			{
				String str = strings.get(i);
				for (String displayValue : displayValues)
				{
					if (str.toLowerCase().startsWith(displayValue)) return true;
				}
			}
			if (txt.length() > strings.get(0).length())
			{
				makeString();
				return completeString.toLowerCase().startsWith(txt);
			}
			return false;
		}

	}

	protected final ValueList valueList;
	protected final IServiceProvider application;
	protected List<Object> realValues; //optional can be null;
	protected boolean allowEmptySelection = false;
	private int valueType;
	private List<String> dataproviders;
	private ParsedFormat format;

	protected IValueList fallbackValueList;

	/*
	 * _____ Declaration and definition of constructors
	 */
	CustomValueList(IServiceProvider application, ValueList valueList)
	{
		this.valueList = valueList;
		this.application = application;
	}

	public boolean getAllowEmptySelection()
	{
		return allowEmptySelection;
	}

	public CustomValueList(IServiceProvider application, ValueList valueList, String values, boolean addEmpty, int valueType, ParsedFormat format)
	{
		this(application, valueList);
		this.valueType = valueType;
		allowEmptySelection = addEmpty;
		// always use default format for numbers
		if (Column.mapToDefaultType(valueType) != IColumnTypes.NUMBER && Column.mapToDefaultType(valueType) != IColumnTypes.INTEGER) this.format = format;
		else this.format = null;
		firstFill(values, false);
	}

	public void setFallbackValueList(IValueList list)
	{
		fallbackValueList = list;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IValueList#getFallbackValueList()
	 */
	public IValueList getFallbackValueList()
	{
		return fallbackValueList;
	}

	/**
	 * @see com.servoy.j2db.dataprocessing.IValueList#getRelationName()
	 */
	public String getRelationName()
	{
		return null;
	}

	public void deregister()
	{
		// nothing
	}

	@SuppressWarnings("nls")
	private void firstFill(String values, boolean displayValuesOnly)
	{
		if (values != null && super.getSize() == 0 && SEPARATOR_DESIGN_VALUE.equals(values.toString()))
		{
			values = null;
		}

		try
		{
			startBundlingEvents();
			//add empty row
			if (allowEmptySelection)
			{
				super.addElement(""); //$NON-NLS-1$
			}

			if (values != null)
			{
				if (values.startsWith("\n") || values.startsWith("\r")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					super.addElement(""); //$NON-NLS-1$
				}
				StringTokenizer tk = new StringTokenizer(values.trim(), "\r\n"); //$NON-NLS-1$
				while (tk.hasMoreTokens())
				{
					String line = tk.nextToken();
					String[] str = Utils.stringSplit(line, '|', '\\');

					// in case we are dealing with a valuelist with display & real values, consider \- display value not to be a separator (normally you could use \\- but it's hard to figure out)
					if (SEPARATOR_DESIGN_VALUE.equals(str[0])) str[0] = Utils.stringSplit(line, "|")[0];

					if ((SEPARATOR_DESIGN_VALUE.equals(str[0]) || str[1] != null) && !displayValuesOnly)
					{
						super.addElement(application.getI18NMessageIfPrefixed(str[0]));
						if (realValues == null)
						{
							realValues = new SafeArrayList<Object>();
							// add empty values ignore last one
							for (int i = 1; i < getSize(); i++)
							{
								realValues.add(null);
							}
						}
						if (valueType == Types.OTHER)
						{
							Object realValue = str[1];
							if (str[1] != null && str[1].startsWith("%%") && ScopesUtils.isVariableScope(str[1].substring(2)) && str[1].endsWith("%%"))
							{
								realValue = application.getFoundSetManager().getScopesScopeProvider().getDataProviderValue(
									str[1].substring(2, str[1].length() - 2));
							}
							realValues.add(realValue);
						}
						else
						{
							Object realValue = null;
							// ^ is null value
							if (!"^".equals(str[1]) && str[1] != null)
							{
								String sRealValue = str[1];
								if (sRealValue.startsWith("%%") && ScopesUtils.isVariableScope(sRealValue.substring(2)) && sRealValue.endsWith("%%")) //$NON-NLS-1$//$NON-NLS-2$
								{
									realValue = application.getFoundSetManager().getScopesScopeProvider().getDataProviderValue(
										sRealValue.substring(2, sRealValue.length() - 2));
								}
								else
								{
									realValue = str[1];
								}

								try
								{
									realValue = Column.getAsRightType(valueType, Column.NORMAL_COLUMN, realValue,
										format == null ? null : format.getDisplayFormat(), Integer.MAX_VALUE, null, true);
								}
								catch (RuntimeException ex)
								{
									Debug.error("Value List '" + getName() + "' has real value '" + str[1] + "' which cannot be converted to type:" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										Column.getDisplayTypeString(valueType), ex);
								}
								// check if it is a global var
							}
							realValues.add(realValue);
						}
					}
					else
					{
						if (realValues == null)
						{
							displayValuesOnly = true;
							String strval = application.getI18NMessageIfPrefixed(line);
							if (valueType == Types.OTHER)
							{
								super.addElement(strval);
							}
							else
							{
								if (SEPARATOR_DESIGN_VALUE.equals(strval) || ESCAPED_SEPARATOR_DESIGN_VALUE.equals(strval))
								{
									super.addElement(strval);
								}
								else
								{
									Object value = strval;
									if (strval.startsWith("%%") && ScopesUtils.isVariableScope(strval.substring(2)) && strval.endsWith("%%")) //$NON-NLS-1$//$NON-NLS-2$
									{
										value = application.getFoundSetManager().getScopesScopeProvider().getDataProviderValue(
											strval.substring(2, strval.length() - 2));
									}
									try
									{
										value = Column.getAsRightType(valueType, Column.NORMAL_COLUMN, value, format == null ? null : format.getDisplayFormat(),
											Integer.MAX_VALUE, null, true);
									}
									catch (Exception e)
									{
										Debug.error("Could not convert custom value list value '" + strval + "' to type " +
											Column.getDisplayTypeString(valueType) + " for value list " + getName() + " -- skipped: " + e.getMessage());
										continue;
									}
									super.addElement(value);
								}
							}
						}
						else
						{
							// Line found without a '|'
							realValues = null;
							stopBundlingEvents();
							removeAllElements();
							firstFill(values, true);
							break;
						}
					}
				}
			}
		}
		finally
		{
			stopBundlingEvents();
		}
	}

	private void fillWithArrayValuesImpl(Object[] display_val_array, List<Object> newRealValues)
	{
		if (display_val_array != null && display_val_array.length == 1 && display_val_array[0] != null &&
			SEPARATOR_DESIGN_VALUE.equals(display_val_array[0].toString().trim()))
		{
			//do not show "-" as only value in valuelist, makes no sense
			display_val_array = new Object[0];
			newRealValues = new ArrayList<Object>();
		}

		// make realValues empty first, they may be used in callbacks
		realValues = null;
		super.removeAllElements();

		realValues = newRealValues;

		try
		{
			startBundlingEvents();
			//add empty row
			if (allowEmptySelection)
			{
				super.addElement(""); //$NON-NLS-1$
			}

			if (display_val_array != null)
			{
				for (Object o : display_val_array)
				{
					if (o instanceof String)
					{
						o = application.getI18NMessageIfPrefixed((String)o);
					}
					if (realValues == null)
					{
						o = Column.getAsRightType(valueType, Column.NORMAL_COLUMN, o, format == null ? null : format.getDisplayFormat(), Integer.MAX_VALUE,
							null, true);
					}
					else
					{
						// if we have real values all display values should be strings
						o = (o != null) ? o.toString() : null;
					}
					super.addElement(o);
				}
			}
		}
		finally
		{
			stopBundlingEvents();
		}
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public void fillWithArrayValues(Object[] display_val_array, Object[] allRealValues)
	{
		List<Object> newRrealValues = null;
		if (allRealValues != null)
		{
			newRrealValues = new SafeArrayList<Object>(allRealValues.length);

			//add empty row
			if (allowEmptySelection)
			{
				newRrealValues.add(null);
			}

			for (Object obj : allRealValues)
			{
				if (valueType != Types.OTHER && Column.mapToDefaultType(valueType) != IColumnTypes.MEDIA)
				{
					obj = Column.getAsRightType(valueType, Column.NORMAL_COLUMN, obj, format == null ? null : format.getDisplayFormat(), Integer.MAX_VALUE,
						null, false);
				}
				newRrealValues.add(obj);
			}
		}
		fillWithArrayValuesImpl(display_val_array, newRrealValues);
	}

	/*
	 * @see com.servoy.j2db.dataprocessing.IValueList#hasRealValues()
	 */
	public boolean hasRealValues()
	{
		return realValues != null;
	}

	public int realValueIndexOf(Object obj)
	{
		if (realValues != null)
		{
			int i = realValues.indexOf(obj);
			if (i == -1 && fallbackValueList != null)
			{
				i = fallbackValueList.realValueIndexOf(obj);
				if (i != -1)
				{
					i = (i + 2) * -1; // all values range from -2 > N
				}
			}
			return i;
		}

		int ret = -1;
		if (obj == null) ret = indexOf(null);
		else
		{
			if (obj instanceof Date && format != null && valueType == IColumnTypes.DATETIME)
			{
				SimpleDateFormat sfsd = new SimpleDateFormat(format.getDisplayFormat());
				String selectedFormat = sfsd.format(obj);
				for (int i = 0; i < size(); i++)
				{
					try
					{
						Object element = getElementAt(i);
						if (!(element instanceof Date)) continue;
						String elementFormat = sfsd.format(element);
						if (Utils.equalObjects(selectedFormat, elementFormat))
						{
							return i;
						}
					}
					catch (RuntimeException e)
					{
						Debug.error(e);
					}
				}
			}
			else for (int i = 0; i < size(); i++)
			{
				if (Utils.equalObjects(obj, getElementAt(i))) return i;
			}
		}
		if (ret == -1 && fallbackValueList != null)
		{
			ret = fallbackValueList.realValueIndexOf(obj);
			if (ret != -1)
			{
				ret = (ret + 2) * -1; // all values range from -2 > N
			}
		}
		return ret;
	}

	/**
	 * Gets the real element of this custom valuelist,
	 * a negative value (excluding -1) will be the fallback valuelist realvalue.
	 *
	 * @see com.servoy.j2db.dataprocessing.IValueList#getRealElementAt(int)
	 */
	//real value, getElementAt is display value
	public Object getRealElementAt(int row)
	{
		if (row < -1 && fallbackValueList != null)
		{
			return fallbackValueList.getRealElementAt((row * -1) - 2);
		}
		if (realValues != null)
		{
			return realValues.get(row);
		}
		if (row >= 0 && row < size())
		{
			return getElementAt(row);
		}
		return null;
	}


	/**
	 * If a this valuelist has a fallback valuelist and the value is not found in this list
	 * then the fallback valuelist will be queried and if found a value between -2 and greater negative value.
	 *
	 * @see javax.swing.DefaultListModel#indexOf(java.lang.Object)
	 */
	@Override
	public int indexOf(Object elem)
	{
		int ret = super.indexOf(elem);
		if (ret == -1 && fallbackValueList != null)
		{
			ret = fallbackValueList.indexOf(elem);
			if (ret != -1)
			{
				ret = (ret + 2) * -1; // all values range from -2 > N
			}
		}
		return ret;
	}

	/**
	 * If a this valuelist has a fallback valuelist and the value is not found in this list
	 * then the fallback valuelist will be queried and if found a value between -2 and greater negative value.
	 * @see javax.swing.DefaultListModel#getElementAt(int)
	 */
	@Override
	public Object getElementAt(int index)
	{
		if (index < -1 && fallbackValueList != null)
		{
			return fallbackValueList.getElementAt((index * -1) - 2);
		}
		return super.getElementAt(index);
	}

	public void fill(IRecordInternal parentState)
	{
		if (fallbackValueList != null) fallbackValueList.fill(parentState);
	}

	public static Object[] processRow(Object[] row, int showValues, int returnValues)
	{
		Object[] returnValue = row;
		boolean appendFirstRow = (showValues & 1) == 0 && (returnValues & 1) == 0;
		boolean appendSecondRow = (showValues & 2) == 0 && (returnValues & 2) == 0;
		boolean appendThirdRow = (showValues & 4) == 0 && (returnValues & 4) == 0;
		if (!appendFirstRow && appendSecondRow && appendThirdRow)
		{
			returnValue = new Object[] { row[0], null, null };
		}
		else if (appendFirstRow && appendThirdRow)
		{
			returnValue = new Object[] { null, row[0], null };
		}
		else if (appendFirstRow && appendSecondRow)
		{
			returnValue = new Object[] { null, null, row[0] };
		}
		else if (appendFirstRow)
		{
			returnValue = new Object[] { null, row[0], row[1] };
		}
		else if (appendSecondRow)
		{
			returnValue = new Object[] { row[0], null, row[1] };
		}
		else if (appendThirdRow)
		{
			returnValue = new Object[] { row[0], row[1], null };
		}
		return returnValue;
	}

	public static Object handleRowData(ValueList vl, String[] displayFormats, boolean concat, int bitset, IRecordInternal row, IServiceProvider application)
	{
		Object[] args = new Object[3];
		if ((bitset & 1) != 0) args[0] = row.getValue(vl.getDataProviderID1());
		if ((bitset & 2) != 0) args[1] = row.getValue(vl.getDataProviderID2());
		if ((bitset & 4) != 0) args[2] = row.getValue(vl.getDataProviderID3());
		if (displayFormats != null) return handleDisplayData(vl, displayFormats, concat, bitset, args, application).toString();
		else return handleRowData(vl, concat, bitset, args, application);
	}

	public static Object handleRowData(ValueList vl, boolean concat, int bitset, Object[] row, IServiceProvider application)
	{
		Object anObject = null;
		StringBuffer showVal = null;
		if (concat) showVal = new StringBuffer();
		if ((bitset & 1) != 0)
		{
			Object val = row[0];
			if (concat)
			{
				showVal.append(convertToString(val, application));
			}
			else
			{
				anObject = val;
			}
		}
		if ((bitset & 2) != 0)
		{
			Object val = row[1];
			if (concat)
			{
				if (showVal.length() != 0) showVal.append(vl.getSeparator());
				showVal.append(convertToString(val, application));
			}
			else
			{
				anObject = val;
			}
		}
		if ((bitset & 4) != 0)
		{
			Object val = row[2];
			if (concat)
			{
				if (showVal.length() != 0) showVal.append(vl.getSeparator());
				showVal.append(convertToString(val, application));
			}
			else
			{
				anObject = val;
			}
		}
		if (concat)
		{
			return showVal.toString();
		}
		else
		{
			return anObject;
		}
	}

	public static DisplayString handleDisplayData(ValueList vl, String[] displayFormat, boolean concat, int bitset, Object[] row, IServiceProvider application)
	{
		DisplayString showVal = new DisplayString(vl.getSeparator());
		if ((bitset & 1) != 0)
		{
			String str = convertToString(row[0], displayFormat[0], application);
			if (concat)
			{
				showVal.append(str);
			}
			else
			{
				showVal.set(str);
			}
		}
		if ((bitset & 2) != 0)
		{
			String str = convertToString(row[1], displayFormat[1], application);
			if (concat)
			{
				showVal.append(str);
			}
			else
			{
				showVal.set(str);
			}
		}
		if ((bitset & 4) != 0)
		{
			String str = convertToString(row[2], displayFormat[2], application);
			if (concat)
			{
				showVal.append(str);
			}
			else
			{
				showVal.set(str);
			}
		}
		return showVal;
	}

	public static String convertToString(Object obj, IServiceProvider application)
	{
		return convertToString(obj, null, application);
	}

	public static String convertToString(Object obj, String format, IServiceProvider application)
	{
		if (obj == null)
		{
			return ""; //$NON-NLS-1$
		}
		return TagResolver.formatObject(obj, format, application);
	}

	public String getName()
	{
		return valueList == null ? "<unknown>" : valueList.getName();
	}

	public ParsedFormat getFormat()
	{
		return format;
	}

	public int getValueType()
	{
		return valueType;
	}

	public void setValueType(int t)
	{
		this.valueType = t;
	}

	@Override
	public void setElementAt(Object obj, int index)
	{
		stopBundlingEvents(); // just to be sure
		super.setElementAt(obj, index);
	}

	@Override
	public Object set(int index, Object element)
	{
		stopBundlingEvents(); // just to be sure
		return super.set(index, element);
	}

	/**
	 * @param dataprovider
	 */
	public void addDataProvider(String dataprovider)
	{
		if (dataproviders == null) dataproviders = new ArrayList<String>(3);
		dataproviders.add(dataprovider);
	}

	public List<String> getDataProviders()
	{
		return (dataproviders == null ? Collections.<String> emptyList() : dataproviders);
	}

	public ValueList getValueList()
	{
		return valueList;
	}

	@Override
	public IDataProvider[] getDependedDataProviders()
	{
		return null;
	}
}