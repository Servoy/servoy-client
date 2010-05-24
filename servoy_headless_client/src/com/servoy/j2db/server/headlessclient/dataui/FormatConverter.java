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

import java.text.Format;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.StateFullSimpleDateFormat;
import com.servoy.j2db.util.Utils;

final class FormatConverter implements IConverter
{
	private static final long serialVersionUID = 1L;

	private final FormComponent formComponent;
	private final Format displayFormatter;
	private final Format editFormatter;
	private final WebEventExecutor eventExecutor;
	private final FormatParser format;


	FormatConverter(FormComponent field, WebEventExecutor eventExecutor, Format displayFormatter, FormatParser format)
	{
		this.formComponent = field;
		this.eventExecutor = eventExecutor;
		this.displayFormatter = displayFormatter;
		this.editFormatter = null;
		this.format = format;
	}

	FormatConverter(FormComponent field, WebEventExecutor eventExecutor, Format displayFormatter, Format editFormatter, FormatParser format)
	{
		this.formComponent = field;
		this.eventExecutor = eventExecutor;
		this.displayFormatter = displayFormatter;
		this.editFormatter = editFormatter;
		this.format = format;
	}

	/**
	 * @see wicket.util.convert.IConverter#convertToObject(java.lang.String, java.util.Locale)
	 */
	public Object convertToObject(String value, Locale locale)
	{
		if (value == null) return null;

		try
		{
			if (!eventExecutor.getValidationEnabled()) return value;
			if (editFormatter != null)
			{
				try
				{
					return convertString(value, editFormatter);
				}
				catch (ParseException pe)
				{
					return convertString(value, displayFormatter);
				}
			}
			else
			{
				return convertString(value, displayFormatter);
			}
		}
		catch (Exception ex)
		{
			String extraMsg = ""; //$NON-NLS-1$
			if (formComponent instanceof IComponent && ((IComponent)formComponent).getName() != null)
			{
				extraMsg = " on component " + ((IComponent)formComponent).getName(); //$NON-NLS-1$
			}
			if (formComponent instanceof IDisplayData)
			{
				extraMsg += " with dataprovider: " + ((IDisplayData)formComponent).getDataProviderID(); //$NON-NLS-1$
			}
			throw new ConversionException("Can't convert from string '" + value + "' to object with format: " + format.getEditFormat() + extraMsg, ex).setConverter(this); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @param str
	 * @return
	 * @throws ParseException
	 */
	private Object convertString(String value, Format formatter) throws ParseException
	{
		if (format.getDisplayFormat().equals(value)) return null;

		String str = value;
		if (format.isMask())
		{
			if (format.getPlaceHolderCharacter() != 0)
			{
				str = str.replace(format.getPlaceHolderCharacter(), ' ');
			}
			String placeHolder = format.getDisplayFormat().replace('y', ' ').replace('M', ' ').replace('w', ' ').replace('W', ' ').replace('D', ' ').replace(
				'd', ' ').replace('F', ' ').replace('a', ' ').replace('H', ' ').replace('k', ' ').replace('K', ' ').replace('h', ' ').replace('m', ' ').replace(
				's', ' ').replace('S', ' ');
			if (placeHolder.endsWith(str)) return null;

		}
		str = value.toString().trim();
		if ("".equals(str)) return null; //$NON-NLS-1$
		Object result;
		if (formatter instanceof StateFullSimpleDateFormat)
		{
			((StateFullSimpleDateFormat)formatter).setOriginal((Date)formComponent.getModelObject());
			formatter.parseObject(str);
			result = ((StateFullSimpleDateFormat)formatter).getMergedDate();
		}
		else
		{
			result = formatter.parseObject(str);
		}

		if (formComponent instanceof IProviderStylePropertyChanges && !Utils.equalObjects(value, formatter.format(result)))
		{
			// this can happen for example when date formatting uses lenient mode (hour 10:65 will be valid and interpreted as 11:05) - what the user entered is
			// valid and represents the same date, but re-rendering the component need to happen to display it normally...
			// another example is when formatting numbers with "." as grouping separator and user types "0.7" => after leaving the field it should be shown as "7"
			((IProviderStylePropertyChanges)formComponent).getStylePropertyChanges().setValueChanged();
		}

		return result;
	}

	public String convertToEditString(Object value, Locale locale)
	{
		return convertToString(value, editFormatter);
	}

	/**
	 * @see wicket.util.convert.IConverter#convertToString(java.lang.Object, java.util.Locale)
	 */
	public String convertToString(Object value, Locale locale)
	{
		return convertToString(value, displayFormatter);
	}

	@SuppressWarnings("nls")
	private String convertToString(Object value, Format formatter)
	{
		// if value is already a string (for example it was a converted by a converter) just return that.
		if (value == null || value instanceof String) return (String)value;
		if (!eventExecutor.getValidationEnabled() && value instanceof String) return (String)value;
		try
		{
			return formatter.format(value);
		}
		catch (Exception ex)
		{
			String extraMsg = ""; //$NON-NLS-1$
			if (formComponent instanceof IComponent && ((IComponent)formComponent).getName() != null)
			{
				extraMsg = " on component " + ((IComponent)formComponent).getName();
			}
			if (formComponent instanceof IDisplayData)
			{
				extraMsg += " with dataprovider: " + ((IDisplayData)formComponent).getDataProviderID();
			}
			String className = value != null ? value.getClass().getSimpleName() : ""; //$NON-NLS-1$
			Debug.error("Can't parse the " + value + " (" + className + ") to a string with format: " + format.getDisplayFormat() + extraMsg, ex);
			return value.toString();
		}
	}

	public void setLenient(boolean lenient)
	{
		if (editFormatter instanceof StateFullSimpleDateFormat) ((StateFullSimpleDateFormat)editFormatter).setLenient(lenient);
		if (displayFormatter instanceof StateFullSimpleDateFormat) ((StateFullSimpleDateFormat)displayFormatter).setLenient(lenient);
	}

}