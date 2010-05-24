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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.util.ITagResolver;

/**
 * @author rob
 * 
 */
public class TagResolver
{

	/**
	 * @author rob
	 * 
	 */
	private static class RecordTagResolver implements ITagResolver
	{

		private final IRecordInternal record;

		/**
		 * @param record2
		 */
		public RecordTagResolver(IRecordInternal record)
		{
			this.record = record;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.servoy.j2db.util.ITagResolver#getStringValue(java.lang.String)
		 */
		public String getStringValue(String dataProviderID)
		{
			if (record == null) return null;
			Object value = record.getValue(dataProviderID);
			return formatObject(value, record.getParentFoundSet().getFoundSetManager().getApplication().getSettings());
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


	public static String formatObject(Object value, Properties settings)
	{

		if (value == null || value == Scriptable.NOT_FOUND)
		{
			return null;
		}

		String formatString = getFormatString(value.getClass(), settings);
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
			return new DecimalFormat(formatString).format(value);
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
	public static String getFormatString(Class clazz, Properties settings)
	{

		String formatString = null;

		if (Date.class.isAssignableFrom(clazz))
		{
			String pattern = settings.getProperty("locale.dateformat"); //$NON-NLS-1$
			// Note: new SimpleDateFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new SimpleDateFormat().toPattern() : pattern;
		}
		// Note that Integer extends Number, so first check for Integer, then for Number
		else if (Integer.class.isAssignableFrom(clazz))
		{
			String pattern = settings.getProperty("locale.integerformat"); //$NON-NLS-1$
			// Note: new DecimalFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new DecimalFormat().toPattern() : pattern;
		}
		else if (Number.class.isAssignableFrom(clazz))
		{
			String pattern = settings.getProperty("locale.numberformat"); //$NON-NLS-1$
			// Note: new DecimalFormat() uses Locale.getDefault()
			formatString = (pattern == null || pattern.trim().length() == 0) ? new DecimalFormat().toPattern() : pattern;
		}

		return formatString;
	}

	public static String getDefaultFormatForType(Properties settings, int type)
	{
		switch (type)
		{
			case IColumnTypes.DATETIME :
				return getFormatString(Date.class, settings);

			case IColumnTypes.TEXT :
				return getFormatString(String.class, settings);

			case IColumnTypes.NUMBER :
				return getFormatString(Number.class, settings);

			case IColumnTypes.INTEGER :
				return getFormatString(Integer.class, settings);

			case IColumnTypes.MEDIA :
				return getFormatString(byte[].class, settings);

			default :
				return null;
		}
	}


}
