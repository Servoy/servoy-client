/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.component;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.IUIConverter;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.scripting.IScriptableProvider;
import com.servoy.j2db.ui.scripting.IFormatScriptComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;

/**
 * Holder for format to be applied to component.
 * Default column format and converters are applied.
 *
 * @author rgansevles
 *
 * @since 6.1
 *
 */
public class ComponentFormat
{
	public final ParsedFormat parsedFormat;
	public final int dpType; // always mapped to default
	public final int uiType; // always mapped to default

	public ComponentFormat(ParsedFormat parsedFormat, int dpType, int uiType)
	{
		this.parsedFormat = parsedFormat;
		this.dpType = Column.mapToDefaultType(dpType);
		this.uiType = Column.mapToDefaultType(uiType);
	}

	public static ComponentFormat getComponentFormat(String format, String dataProviderID, IDataProviderLookup dataProviderLookup, IServiceProvider application)
	{
		return getComponentFormat(format, dataProviderID, dataProviderLookup, application, false);
	}

	public static ComponentFormat getComponentFormat(String format, String dataProviderID, IDataProviderLookup dataProviderLookup, IServiceProvider application,
		boolean autoFillMaxLength)
	{
		int dpType = IColumnTypes.TEXT;
		String formatProperty = format;

		if (dataProviderID != null && dataProviderLookup != null)
		{
			try
			{
				return ComponentFormat.getComponentFormat(formatProperty, dataProviderLookup.getDataProvider(dataProviderID), application, autoFillMaxLength);
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}

		return ComponentFormat.getComponentFormat(formatProperty, dpType, application);
	}

	public static ComponentFormat getComponentFormat(String format, IDataProvider dataProvider, IServiceProvider application)
	{
		return getComponentFormat(format, dataProvider, application, false);
	}

	public static ComponentFormat getComponentFormat(String format, IDataProvider dataProvider, IServiceProvider application, boolean autoFillMaxLength)
	{
		int dpType = IColumnTypes.TEXT;
		String formatProperty = format;
		if (dataProvider != null)
		{
			dpType = dataProvider.getDataProviderType();
			IColumn column = null;
			if (dataProvider instanceof ColumnWrapper)
			{
				column = ((ColumnWrapper)dataProvider).getColumn();
			}
			else if (dataProvider instanceof Column)
			{
				column = (Column)dataProvider;
			}
			else if (dataProvider instanceof ScriptCalculation)
			{
				// When it is a stored calculation, the name of the calc is the name of he column
				ScriptCalculation calc = (ScriptCalculation)dataProvider;
				try
				{
					ITable table = calc.getTable();
					if (table != null)
					{
						column = table.getColumn(calc.getName());
					}
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
			}

			if (column instanceof AggregateVariable)
			{
				Column columnToAggregate = null;
				try
				{
					ITable table = column.getTable();
					if (table != null)
					{
						columnToAggregate = table.getColumn(((AggregateVariable)column).getDataProviderIDToAggregate());
					}
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}

				// Use aggregated column when they are of the same type (so not count(textcolumn))
				if (columnToAggregate != null && column.getDataProviderType() == columnToAggregate.getDataProviderType())
				{
					column = columnToAggregate;
				}
			}

			if (column instanceof Column)
			{
				ColumnInfo ci = ((Column)column).getColumnInfo();
				if (ci != null)
				{
					if (formatProperty == null || formatProperty.length() == 0)
					{
						if (ci.getDefaultFormat() != null && ci.getDefaultFormat().length() > 0)
						{
							formatProperty = ci.getDefaultFormat();
						}
					}

					dpType = application.getFoundSetManager().getConvertedTypeForColumn(column, true);
				}

			}
		}

		ComponentFormat componentFormat = ComponentFormat.getComponentFormat(formatProperty, dpType, application);
		if (autoFillMaxLength && dataProvider != null && dataProvider.getLength() > 0 && componentFormat.parsedFormat != null &&
			componentFormat.parsedFormat.getMaxLength() == null && (dpType == IColumnTypes.TEXT || dpType == IColumnTypes.MEDIA))
		{
			componentFormat.parsedFormat.updateMaxLength(Integer.valueOf(dataProvider.getLength()));
		}
		return componentFormat;
	}

	public static ComponentFormat getComponentFormat(String formatProperty, int dpType, IServiceProvider application)
	{
		if ("converter".equals(formatProperty)) //$NON-NLS-1$
		{
			return new ComponentFormat(FormatParser.parseFormatProperty(null), IColumnTypes.TEXT, IColumnTypes.TEXT);
		}

		int uiType = dpType;

		// parse format to see if it contains UI converter info
		boolean hasUIConverter = false;
		ParsedFormat parsedFormat = FormatParser.parseFormatProperty(formatProperty, null, application);
		if (parsedFormat.getUIConverterName() != null)
		{
			IUIConverter uiConverter = ((FoundSetManager)application.getFoundSetManager()).getUIConverterManager().getConverter(
				parsedFormat.getUIConverterName());
			if (uiConverter != null)
			{
				hasUIConverter = true;
				int convType = uiConverter.getToObjectType(parsedFormat.getUIConverterProperties());
				if (convType != Integer.MAX_VALUE)
				{
					uiType = Column.mapToDefaultType(convType);
				}
			}
		}

		String defaultFormat = parsedFormat.isEmpty() ? TagResolver.getDefaultFormatForType(application, uiType) : null;
		String formatString;
		if (parsedFormat.isEmpty() && !hasUIConverter && !parsedFormat.useLocalDateTime())
		{
			formatString = defaultFormat;
		}
		else
		{
			formatString = application.getI18NMessageIfPrefixed(parsedFormat.getFormatString());
		}
		return new ComponentFormat(FormatParser.parseFormatProperty(formatString, defaultFormat), dpType, uiType);
	}

	public static Object applyUIConverterToObject(Object component, Object value, String dataProviderID, IFoundSetManagerInternal foundsetManager)
	{
		if (component instanceof IScriptableProvider && ((IScriptableProvider)component).getScriptObject() instanceof IFormatScriptComponent)
		{
			ComponentFormat cf = ((IFormatScriptComponent)((IScriptableProvider)component).getScriptObject()).getComponentFormat();
			return applyUIConverterToObject(value, dataProviderID, foundsetManager, cf);
		}
		return value;
	}

	/**
	 * @param value
	 * @param dataProviderID
	 * @param foundsetManager
	 * @param cf
	 */
	public static Object applyUIConverterToObject(Object value, String dataProviderID, IFoundSetManagerInternal foundsetManager, ComponentFormat cf)
	{
		if (cf != null && cf.parsedFormat.getUIConverterName() != null)
		{
			IUIConverter conv = foundsetManager.getUIConverterManager().getConverter(cf.parsedFormat.getUIConverterName());
			if (conv == null)
			{
				throw new IllegalStateException(Messages.getString("servoy.error.converterNotFound", new Object[] { cf.parsedFormat.getUIConverterName() })); //$NON-NLS-1$
			}
			try
			{
				return conv.convertToObject(cf.parsedFormat.getUIConverterProperties(), cf.dpType, value);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException(Messages.getString("servoy.record.error.settingDataprovider", //$NON-NLS-1$
					new Object[] { dataProviderID, Column.getDisplayTypeString(cf.dpType), value }), e);
			}
		}
		return value;
	}

	public static Object applyUIConverterFromObject(Object component, Object obj, String dataProviderID, IFoundSetManagerInternal foundsetManager)
	{
		if (component instanceof IScriptableProvider && ((IScriptableProvider)component).getScriptObject() instanceof IFormatScriptComponent)
		{
			ComponentFormat cf = ((IFormatScriptComponent)((IScriptableProvider)component).getScriptObject()).getComponentFormat();
			return applyUIConverterFromObject(obj, dataProviderID, foundsetManager, cf);
		}
		return obj;
	}

	public static Object applyUIConverterFromObject(Object obj, String dataProviderID, IFoundSetManagerInternal foundsetManager, ComponentFormat cf)
	{
		if (cf != null && cf.parsedFormat.getUIConverterName() != null)
		{
			IUIConverter conv = foundsetManager.getUIConverterManager().getConverter(cf.parsedFormat.getUIConverterName());
			if (conv == null)
			{
				throw new IllegalStateException(Messages.getString("servoy.error.converterNotFound", new Object[] { cf.parsedFormat.getUIConverterName() })); //$NON-NLS-1$
			}
			try
			{
				return conv.convertFromObject(cf.parsedFormat.getUIConverterProperties(), cf.dpType, obj);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException(
					Messages.getString("servoy.record.error.settingDataprovider", new Object[] { dataProviderID, Column.getDisplayTypeString(cf.dpType), obj }), //$NON-NLS-1$
					e);
			}
		}
		return obj;
	}

	public static int getValueTypeForFormatType(String formatType)
	{
		int valueType = IColumnTypes.TEXT;
		if (formatType != null)
		{
			switch (formatType)
			{
				case "TEXT" :
					valueType = IColumnTypes.TEXT;
					break;
				case "NUMBER" :
					valueType = IColumnTypes.NUMBER;
					break;
				case "DATETIME" :
					valueType = IColumnTypes.DATETIME;
			}
		}
		return valueType;
	}

}