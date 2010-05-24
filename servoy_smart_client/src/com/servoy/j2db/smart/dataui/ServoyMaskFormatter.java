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
package com.servoy.j2db.smart.dataui;

import java.text.ParseException;

import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.gui.FixedMaskFormatter;

/**
 * @author jcompagner
 *
 */
public final class ServoyMaskFormatter extends FixedMaskFormatter
{
	private final boolean displayFormatter;

	/**
	 * @param mask
	 */
	public ServoyMaskFormatter(String mask, boolean displayFormatter) throws ParseException
	{
		super(mask);
		this.displayFormatter = displayFormatter;
	}

	@SuppressWarnings("nls")
	@Override
	public String valueToString(Object value) throws ParseException
	{
		if ((value == null || value.toString().trim().equals("")) && displayFormatter) return "";
		try
		{
			return super.valueToString(value);
		}
		catch (ParseException pe)
		{
			return super.valueToString(""); //$NON-NLS-1$
		}
	}

	@Override
	public Object stringToValue(String value) throws ParseException
	{
		if (value == null) return null;
		try
		{
			return super.stringToValue(value);
		}
		catch (ParseException pe)
		{
			String placeHolder = getPlaceholder();
			if (placeHolder != null)
			{
				if (Utils.equalObjects(placeHolder, value))
				{
					return null;
				}
				else if (placeHolder.length() == value.length())
				{
					for (int i = 0; i < value.length(); i++)
					{
						if (value.charAt(i) == ' ') continue;
						if (value.charAt(i) == placeHolder.charAt(i)) continue;
						throw pe;
					}
					return null;
				}
			}
			else if (value.replace(getPlaceholderCharacter(), ' ').trim().equals("")) //$NON-NLS-1$
			{
				return null;
			}
			else if (value.equals(valueToString(null)))
			{
				return null;
			}
			throw pe;
		}
	}
}