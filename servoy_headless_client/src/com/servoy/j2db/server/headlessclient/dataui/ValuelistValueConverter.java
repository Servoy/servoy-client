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
package com.servoy.j2db.server.headlessclient.dataui;

import java.util.Locale;

import org.apache.wicket.util.convert.IConverter;

import com.servoy.j2db.dataprocessing.IValueList;

/**
 * A {@link IConverter} implementation for {@link IValueList} instances.
 * 
 * @author jcompagner
 */
final class ValuelistValueConverter implements IConverter
{
	private static final long serialVersionUID = 1L;

	private final IValueList list;
	private final WebDataField component;

	ValuelistValueConverter(IValueList list, WebDataField component)
	{
		this.list = list;
		this.component = component;
	}

	/**
	 * @see wicket.util.convert.IConverter#convertToObject(java.lang.String, java.util.Locale)
	 */
	public Object convertToObject(String value, Locale locale)
	{
//		next line is commented out, because on Safari + type-ahead this lead to endless focus gain/focus lost loop when the value was commited to server
//		component.setChanged(); // mark the component as changed, to be sure it is sent back in the ajax response
		if (value == null) return null;
		if (list != null)//stringToValue
		{
			// Try to do a mapping, to treat the situations when the values in the database
			// have trailing spaces. The user input is trimmed and otherwise nothing could be
			// selected from among the values that have trailing spaces.
			if (component instanceof WebDataLookupField) value = ((WebDataLookupField)component).mapTrimmedToNotTrimmed(value);
			int index = list.indexOf(value);
			if (index != -1)
			{
				return list.getRealElementAt(index);
			}
			else if (!list.hasRealValues() || !component.getEventExecutor().getValidationEnabled())
			{
				return value;
			}
		}
		return null;
	}

	/**
	 * @see wicket.util.convert.IConverter#convertToString(java.lang.Object, java.util.Locale)
	 */
	public String convertToString(Object value, Locale locale)
	{
		if (list != null)//valueToString
		{
			int index = list.realValueIndexOf(value);
			if (index != -1)
			{
				value = list.getElementAt(index);
				if (value != null)
				{
					return value.toString();
				}
			}
			else if (!list.hasRealValues() || !component.getEventExecutor().getValidationEnabled())
			{
				return value.toString();
			}
		}
		return null;
	}
}