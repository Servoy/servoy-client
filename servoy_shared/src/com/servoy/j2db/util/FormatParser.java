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
package com.servoy.j2db.util;


/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class FormatParser
{
	private boolean allUpperCase;
	private boolean allLowerCase;
	private boolean numberValidator;
	private boolean raw;
	private boolean mask;

	private String editOrPlaceholder;
	private String displayFormat;
	private String format;
	private Integer maxLength;

	public FormatParser()
	{
	}

	public FormatParser(String format)
	{
		setFormat(format);
	}

	/**
	 * Parsers a format string, current supported formats:
	 * 
	 * numbers/integers: display, display|edit
	 * date: display, display|edit, display|mask, display|placeholder|mask
	 * text: |U , |L , |#, display, display|raw, display|placeholder, display|placeholder|raw
	 *  
	 * @param format
	 */
	public void setFormat(String format)
	{
		this.format = format;
		this.allLowerCase = false;
		this.allUpperCase = false;
		this.numberValidator = false;
		this.maxLength = null;
		this.raw = false;
		this.mask = false;

		String dFormat = format;
		String eFormat = null;

		if (format != null)
		{
			int index = format.indexOf("|");
			if (index != -1)
			{
				dFormat = format.substring(0, index);
				eFormat = format.substring(index + 1);
				if (dFormat.length() == 0 && eFormat.length() == 1)
				{
					if (eFormat.charAt(0) == 'U')
					{
						allUpperCase = true;
					}
					else if (eFormat.charAt(0) == 'L')
					{
						allLowerCase = true;
					}
					else if (eFormat.charAt(0) == '#')
					{
						numberValidator = true;
					}
					dFormat = null;
					eFormat = null;
				}
				else
				{
					String ml = eFormat;
					index = ml.indexOf("|#(");
					if (index != -1 && ml.endsWith(")"))
					{
						eFormat = ml.substring(0, index);
						ml = ml.substring(index + 1);
					}
					if (ml.startsWith("#("))
					{
						try
						{
							maxLength = Integer.valueOf(ml.substring(2, ml.length() - 1));
							if (ml == eFormat)
							{
								eFormat = "";
							}
						}
						catch (Exception e)
						{
							Debug.log(e);
						}
					}
					if (eFormat.endsWith("raw"))
					{
						raw = true;
						eFormat = trim(eFormat.substring(0, eFormat.length() - "raw".length()));
					}
					if (eFormat.endsWith("mask"))
					{
						mask = true;
						eFormat = trim(eFormat.substring(0, eFormat.length() - "mask".length()));
						// re test raw
						if (eFormat.endsWith("raw"))
						{
							raw = true;
							eFormat = trim(eFormat.substring(0, eFormat.length() - "raw".length()));
						}
					}
					else eFormat = trim(eFormat);

					if (eFormat.equals("")) eFormat = null;
				}
			}

		}
		this.displayFormat = dFormat;
		this.editOrPlaceholder = eFormat;
	}

	/**
	 * @return the maxLength
	 */
	public Integer getMaxLength()
	{
		return maxLength;
	}

	/**
	 * @param eFormat
	 * @return
	 */
	private String trim(String eFormat)
	{
		String tmp = eFormat.trim();
		if (tmp.startsWith("|")) tmp = tmp.substring(1);
		if (tmp.endsWith("|")) tmp = tmp.substring(0, tmp.length() - 1);
		return tmp;
	}

	public String getDateMask()
	{
		StringBuilder maskPattern = new StringBuilder(displayFormat.length());
		int counter = 0;
		while (counter < displayFormat.length())
		{
			char ch = displayFormat.charAt(counter++);
			switch (ch)
			{
				case 'y' :
				case 'M' :
				case 'w' :
				case 'W' :
				case 'D' :
				case 'd' :
				case 'F' :
				case 'H' :
				case 'k' :
				case 'K' :
				case 'h' :
				case 'm' :
				case 's' :
				case 'S' :
					maskPattern.append('#');
					break;
				case 'a' :
					maskPattern.append('?');
					break;
				default :
					maskPattern.append(ch);
			}

		}
		return maskPattern.toString();
	}

	public boolean hasEditFormat()
	{
		// if it is a mask format then the editorplaceholder is always the place holder. 
		// currently we dont have display and edit (with mask) support
		return !mask && (editOrPlaceholder != null && !editOrPlaceholder.equals(displayFormat));
	}

	/**
	 * @return the format
	 */
	public String getFormat()
	{
		return format;
	}

	public char getPlaceHolderCharacter()
	{
		if (editOrPlaceholder != null && editOrPlaceholder.length() > 0) return editOrPlaceholder.charAt(0);
		return 0;
	}

	public String getPlaceHolderString()
	{
		if (editOrPlaceholder != null && editOrPlaceholder.length() > 1) return editOrPlaceholder;
		return null;
	}

	/**
	 * @return the displayFormat
	 */
	public String getDisplayFormat()
	{
		return displayFormat;
	}

	/**
	 * @return the editFormat
	 */
	public String getEditFormat()
	{
		return editOrPlaceholder;
	}

	/**
	 * @return the allLowerCase
	 */
	public boolean isAllLowerCase()
	{
		return allLowerCase;
	}

	/**
	 * @return the allUpperCase
	 */
	public boolean isAllUpperCase()
	{
		return allUpperCase;
	}

	/**
	 * @return the mask
	 */
	public boolean isMask()
	{
		return mask;
	}

	/**
	 * @return the numberValidator
	 */
	public boolean isNumberValidator()
	{
		return numberValidator;
	}

	/**
	 * @return the raw
	 */
	public boolean isRaw()
	{
		return raw;
	}
}
