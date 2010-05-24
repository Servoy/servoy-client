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


import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.ValueList;

/**
 * @author jblok
 */
public class ValueListFactory
{
	public static IValueList createRealValueList(IServiceProvider app, ValueList vl, int type, String format)
	{
		if (vl == null) return new CustomValueList(app, "<unknown>", "", false, type, format); //$NON-NLS-1$ //$NON-NLS-2$

		IValueList retval = null;
		if (vl.getValueListType() == ValueList.GLOBAL_METHOD_VALUES)
		{
			retval = new GlobalMethodValueList(app, vl);
		}
		else if (vl.getValueListType() == ValueList.CUSTOM_VALUES)
		{
			retval = new CustomValueList(app, vl.getName(), vl.getCustomValues(), (vl.getAddEmptyValue() == ValueList.EMPTY_VALUE_ALWAYS), type, format);
		}
		else
		{
			if (vl.getDatabaseValuesType() == ValueList.RELATED_VALUES)
			{
				retval = new RelatedValueList(app, vl);
			}
			else
			{
				retval = new DBValueList(app, vl);
			}
		}
		return retval;
	}

	public static IValueList fillRealValueList(IServiceProvider app, ValueList vl, int type, String format, int valueType, Object data)
	{
		IValueList newVl = ValueListFactory.createRealValueList(app, vl, type, format);
		if (newVl instanceof CustomValueList) ((CustomValueList)newVl).setType(valueType);
		if (data instanceof JSDataSet || data instanceof IDataSet)
		{
			IDataSet set = null;
			if (data instanceof JSDataSet)
			{
				set = ((JSDataSet)data).getDataSet();
			}
			else
			{
				set = (IDataSet)data;
			}
			if (set.getColumnCount() == 1)
			{
				Object[] displayValues = new Object[set.getRowCount()];
				for (int i = 0; i < set.getRowCount(); i++)
				{
					displayValues[i] = set.getRow(i)[0];
				}
				((CustomValueList)newVl).fillWithArrayValues(displayValues);
			}
			else if (set.getColumnCount() >= 2)
			{
				Object[] displayValues = new Object[set.getRowCount()];
				Object[] realValues = new Object[set.getRowCount()];
				for (int i = 0; i < set.getRowCount(); i++)
				{
					Object[] row = set.getRow(i);
					displayValues[i] = row[0];
					realValues[i] = row[1];
				}
				((CustomValueList)newVl).fillWithArrayValues(displayValues, realValues);
			}
		}
		return newVl;
	}
}
