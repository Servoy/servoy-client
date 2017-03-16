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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFormattedTextField.AbstractFormatter;

import org.mozilla.javascript.Scriptable;

import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.scripting.ScriptVariableScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;

/**
 * Resolver class for tags in text
 * @author rgansevles
 */
public class TagResolver
{

	private static class RecordTagResolver implements ITagResolver
	{

		private final IRecordInternal record;

		public RecordTagResolver(IRecordInternal record)
		{
			this.record = record;
		}

		public String getStringValue(String dataProviderID)
		{
			if (record == null) return null;
			Object value = record.getValue(dataProviderID);
			return formatObject(value, record.getParentFoundSet().getFoundSetManager().getApplication());
		}
	}

	private static class ScriptableTagResolver implements ITagResolver
	{

		private final Scriptable scriptobj;
		private final IServiceProvider application;

		public ScriptableTagResolver(Scriptable obj, IServiceProvider application)
		{
			this.scriptobj = obj;
			this.application = application;
		}

		public String getStringValue(String dataProviderID)
		{
			if (scriptobj == null) return null;
			Object value = ScriptVariableScope.unwrap(scriptobj.get(dataProviderID, scriptobj));
			return formatObject(value, application);
		}
	}

	/**
	 * Factory method to create a resolver.
	 *
	 * @param o
	 * @return resolver
	 */
	public static ITagResolver createResolver(IRecordInternal o)
	{
		return new RecordTagResolver(o);
	}

	/**
	 * Factory method to create a resolver.
	 *
	 * @param o
	 * @return resolver
	 */
	public static ITagResolver createResolver(Scriptable o, IServiceProvider application)
	{
		return new ScriptableTagResolver(o, application);
	}


	public static String formatObject(Object value, Locale locale, ParsedFormat format, AbstractFormatter maskFormatter)
	{
		if (format == null || value == null || value == Scriptable.NOT_FOUND)
		{
			return null;
		}

		if (value instanceof String)
		{
			if (format.isAllLowerCase())
			{
				return value.toString().toLowerCase();
			}
			else if (format.isAllUpperCase())
			{
				return value.toString().toUpperCase();
			}
			else if (format.getDisplayFormat() != null)
			{
				try
				{
					return maskFormatter.valueToString(value);
				}
				catch (ParseException ex)
				{
					Debug.error(ex);
				}
			}
		}

		if (format.getDisplayFormat() == null)
		{
			return value.toString();
		}

		if (value instanceof Date)
		{
			return new SimpleDateFormat(format.getDisplayFormat(), locale).format(value);
		}

		if (value instanceof Number)
		{
			return new DecimalFormat(format.getDisplayFormat(), RoundHalfUpDecimalFormat.getDecimalFormatSymbols(locale)).format(value);
		}

		return value.toString();
	}

	public static String formatObject(Object value, IServiceProvider application)
	{

		return formatObject(value, null, application);
	}


	public static String formatObject(Object value, String format, IServiceProvider application)
	{

		if (value == null || value == Scriptable.NOT_FOUND)
		{
			return null;
		}

		String formatString = null;
		int maxLength = -1;
		if (format != null)
		{
			ParsedFormat parsedFormat = FormatParser.parseFormatProperty(format);
			formatString = parsedFormat.getDisplayFormat();
			if (parsedFormat.getMaxLength() != null)
			{
				maxLength = parsedFormat.getMaxLength().intValue();
				if (formatString == null)
				{
					char[] chars = new char[maxLength];
					for (int i = 0; i < chars.length; i++)
						chars[i] = '#';
					formatString = new String(chars);
				}
			}
		}
		else
		{
			formatString = getFormatString(value.getClass(), application);
		}
		if (formatString == null)
		{
			return value.toString();
		}

		if (value instanceof Date)
		{
			return new SimpleDateFormat(formatString).format(value);
		}

		if (value instanceof Number /* Integer extends Number */)
		{
			DecimalFormat decimalFormat = new DecimalFormat(formatString, RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale()));
			String formatedValue = decimalFormat.format(value);
			if (maxLength > -1 && maxLength <= formatedValue.length()) formatedValue = formatedValue.substring(0, maxLength);
			return formatedValue;
		}

		return value.toString();
	}

	/**
	 * Get the format string for an object based on the settings.
	 *
	 * @param clazz
	 * @param settings
	 * @return
	 */
	public static String getFormatString(Class clazz, IServiceProvider application)
	{
		Properties settings = application.getSettings();
		String formatString = null;

		if (Date.class.isAssignableFrom(clazz))
		{
			String pattern = application.getI18NMessageIfPrefixed(settings.getProperty("locale.dateformat")); //$NON-NLS-1$
			// Note: new SimpleDateFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new SimpleDateFormat().toPattern() : pattern;
		}
		// Note that Integer extends Number, so first check for Integer, then for Number
		else if (Integer.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz))
		{
			String pattern = application.getI18NMessageIfPrefixed(settings.getProperty("locale.integerformat")); //$NON-NLS-1$
			// Note: new DecimalFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new DecimalFormat().toPattern() : pattern;
		}
		else if (Number.class.isAssignableFrom(clazz))
		{
			String pattern = application.getI18NMessageIfPrefixed(settings.getProperty("locale.numberformat")); //$NON-NLS-1$
			// Note: new DecimalFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new DecimalFormat().toPattern() : pattern;
		}

		return formatString;
	}

	public static String getDefaultFormatForType(IServiceProvider application, int type)
	{
		switch (type)
		{
			case IColumnTypes.DATETIME :
				return getFormatString(Date.class, application);

			case IColumnTypes.TEXT :
				return getFormatString(String.class, application);

			case IColumnTypes.NUMBER :
				return getFormatString(Number.class, application);

			case IColumnTypes.INTEGER :
				return getFormatString(Integer.class, application);

			case IColumnTypes.MEDIA :
				return getFormatString(byte[].class, application);

			default :
				return null;
		}
	}


}
