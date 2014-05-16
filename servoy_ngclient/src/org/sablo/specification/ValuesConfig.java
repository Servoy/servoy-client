/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package org.sablo.specification;

/**
 * Specific config class for values type
 */
public class ValuesConfig
{
	private boolean hasDefault = false;
	private Object[] real;
	private String[] display;
	private Object realDefault;
	private String displayDefault;
	private boolean editable;
	private boolean multiple;

	public ValuesConfig addDefault(Object realDef, String displayDef)
	{
		this.hasDefault = true;
		this.realDefault = realDef;
		this.displayDefault = displayDef;
		return this;
	}

	public boolean hasDefault()
	{
		return hasDefault;
	}

	public Object getRealDefault()
	{
		return realDefault;
	}

	public String getDisplayDefault()
	{
		return displayDefault;
	}

	public ValuesConfig setValues(Object[] real, String[] display)
	{
		this.real = real;
		this.display = display;
		return this;
	}

	public ValuesConfig setValues(Object[] realAndDisplay)
	{
		String[] strings;
		if (realAndDisplay instanceof String[])
		{
			strings = (String[])realAndDisplay;
		}
		else
		{
			strings = new String[realAndDisplay.length];
			for (int i = 0; i < realAndDisplay.length; i++)
			{
				strings[i] = realAndDisplay[i] == null ? "" : realAndDisplay[i].toString();
			}
		}
		return setValues(realAndDisplay, strings);
	}

	public Object[] getReal()
	{
		return real;
	}

	public String[] getDisplay()
	{
		return display;
	}

	public int getRealIndexOf(Object value)
	{
		if (real != null)
		{
			for (int i = 0; i < real.length; i++)
			{
				if ((value == null && real[i] == null) || (value != null && value.equals(real[i])))
				{
					return hasDefault ? i + 1 : i;
				}
			}
		}
		return -1;
	}

	public ValuesConfig setEditable(boolean editable)
	{
		this.editable = editable;
		return this;
	}

	public boolean isEditable()
	{
		return editable;
	}

	public ValuesConfig setMultiple(boolean multiple)
	{
		this.multiple = multiple;
		return this;
	}

	public boolean isMultiple()
	{
		return multiple;
	}
}