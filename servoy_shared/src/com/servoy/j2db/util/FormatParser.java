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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;


/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class FormatParser
{

	public static ParsedFormat parseFormatProperty(String formatProperty)
	{
		return parseFormatProperty(formatProperty, null);
	}

	public static ParsedFormat parseFormatProperty(String formatProperty, String defaultFormat)
	{
		if (formatProperty != null && formatProperty.startsWith("{") && formatProperty.endsWith("}"))
		{
			// json
			try
			{
				Map<String, Object> props = new JSONWrapperMap<Object>(new ServoyJSONObject(formatProperty, false, true, false));

				String uiConverterName = null;
				Map<String, String> uiConverterProperties = null;
				Map<String, Object> converterInfo = (Map<String, Object>)props.get("converter");
				if (converterInfo != null)
				{
					uiConverterName = (String)converterInfo.get("name");
					uiConverterProperties = (Map<String, String>)converterInfo.get("properties");
				}

				boolean useLocalDateTime = Boolean.TRUE.equals(props.get("useLocalDateTime"));

				String formatString = (String)props.get("format");
				if (formatString == null) formatString = defaultFormat;
				if (formatString != null)
				{
					return parseFormatString(formatString, uiConverterName, uiConverterProperties, useLocalDateTime);
				}

				// all in json
				boolean allUpperCase = Boolean.TRUE.equals(props.get("allUpperCase"));
				boolean allLowerCase = Boolean.TRUE.equals(props.get("allLowerCase"));
				boolean numberValidator = Boolean.TRUE.equals(props.get("numberValidator"));
				boolean raw = Boolean.TRUE.equals(props.get("raw"));
				boolean mask = Boolean.TRUE.equals(props.get("mask"));
				String editOrPlaceholder = (String)props.get("editOrPlaceholder");
				String displayFormat = (String)props.get("displayFormat");
				Integer maxLength = (Integer)props.get("maxLength");
				String allowedCharacters = (String)props.get("allowedCharacters");

				return new ParsedFormat(allUpperCase, allLowerCase, numberValidator, raw, mask, editOrPlaceholder, displayFormat, maxLength, uiConverterName,
					uiConverterProperties, allowedCharacters, useLocalDateTime);
			}
			catch (JSONException e)
			{
				Debug.error("Could not parse format properties: '" + formatProperty + "'", e);
			}
		}

		// plain format string
		return parseFormatString(formatProperty, null, null, false);
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
	private static ParsedFormat parseFormatString(String fmtString, String uiConverterName, Map<String, String> uiConverterProperties, boolean useLocalDateTime)
	{
		String formatString = fmtString == null || fmtString.length() == 0 ? null : fmtString;
		boolean allLowerCase = false;
		boolean allUpperCase = false;
		boolean numberValidator = false;
		Integer maxLength = null;
		boolean raw = false;
		boolean mask = false;

		String displayFormat = null;
		String editOrPlaceholder = null;

		if (formatString != null)
		{
			int index = formatString.indexOf("|");
			if (index == -1)
			{
				displayFormat = formatString;
			}
			else
			{
				displayFormat = formatString.substring(0, index);
				editOrPlaceholder = formatString.substring(index + 1);
				if (displayFormat.length() == 0 && (editOrPlaceholder.length() == 1 || editOrPlaceholder.startsWith("U[") ||
					editOrPlaceholder.startsWith("L[") || editOrPlaceholder.startsWith("#[")))
				{
					if (editOrPlaceholder.charAt(0) == 'U')
					{
						allUpperCase = true;
					}
					else if (editOrPlaceholder.charAt(0) == 'L')
					{
						allLowerCase = true;
					}
					else if (editOrPlaceholder.charAt(0) == '#')
					{
						numberValidator = true;
					}
					if (editOrPlaceholder.length() > 1)
					{
						maxLength = Integer.valueOf(editOrPlaceholder.substring(2, editOrPlaceholder.length() - 1));
					}
					displayFormat = null;
					editOrPlaceholder = null;
				}
				else
				{
					String ml = editOrPlaceholder;
					index = ml.indexOf("|#(");
					if (index != -1 && ml.endsWith(")"))
					{
						editOrPlaceholder = ml.substring(0, index);
						ml = ml.substring(index + 1);
					}
					if (ml.startsWith("#("))
					{
						try
						{
							maxLength = Integer.valueOf(ml.substring(2, ml.length() - 1));
							if (ml == editOrPlaceholder)
							{
								editOrPlaceholder = "";
							}
						}
						catch (Exception e)
						{
							Debug.log(e);
						}
					}
					if (editOrPlaceholder.endsWith("raw"))
					{
						raw = true;
						editOrPlaceholder = trim(editOrPlaceholder.substring(0, editOrPlaceholder.length() - "raw".length()));
					}
					if (editOrPlaceholder.endsWith("mask"))
					{
						mask = true;
						editOrPlaceholder = trim(editOrPlaceholder.substring(0, editOrPlaceholder.length() - "mask".length()));
						// re test raw
						if (editOrPlaceholder.endsWith("raw"))
						{
							raw = true;
							editOrPlaceholder = trim(editOrPlaceholder.substring(0, editOrPlaceholder.length() - "raw".length()));
						}
					}
					else editOrPlaceholder = trim(editOrPlaceholder);
				}
			}
		}

		return new ParsedFormat(allUpperCase, allLowerCase, numberValidator, raw, mask, editOrPlaceholder, displayFormat, maxLength, uiConverterName,
			uiConverterProperties == null ? null : Collections.unmodifiableMap(uiConverterProperties), null, useLocalDateTime);
	}

	/**
	 * @param eFormat
	 * @return
	 */
	private static String trim(String eFormat)
	{
		String tmp = eFormat.trim();
		if (tmp.startsWith("|")) tmp = tmp.substring(1);
		if (tmp.endsWith("|")) tmp = tmp.substring(0, tmp.length() - 1);
		return tmp;
	}

	/**
	 * Immutable parsed format.
	 *
	 * @author rgansevles
	 *
	 */
	public static class ParsedFormat
	{
		private final boolean allUpperCase;
		private final boolean allLowerCase;
		private final boolean numberValidator;
		private final boolean raw;
		private final boolean mask;

		private final String editOrPlaceholder;
		private final String displayFormat;
		private Integer maxLength;

		private final String uiConverterName;
		private final Map<String, String> uiConverterProperties;
		private final String allowedCharacters;

		private final boolean useLocalDateTime;

		public ParsedFormat(boolean allUpperCase, boolean allLowerCase, boolean numberValidator, boolean raw, boolean mask, String editOrPlaceholder,
			String displayFormat, Integer maxLength, String uiConverterName, Map<String, String> uiConverterProperties, String allowedCharacters,
			boolean useLocalDateTime)
		{
			this.allUpperCase = allUpperCase;
			this.allLowerCase = allLowerCase;
			this.numberValidator = numberValidator;
			this.raw = raw;
			this.mask = mask;

			this.editOrPlaceholder = editOrPlaceholder == null || editOrPlaceholder.length() == 0 ? null : editOrPlaceholder;
			this.displayFormat = displayFormat == null || displayFormat.length() == 0 ? null : displayFormat;
			this.maxLength = maxLength;

			this.uiConverterName = uiConverterName;
			this.uiConverterProperties = uiConverterProperties; // constructor is private, all callers should wrap with unmodifiable map
			this.allowedCharacters = allowedCharacters == null || allowedCharacters.length() == 0 ? null : allowedCharacters;

			this.useLocalDateTime = useLocalDateTime;
		}

		public String toFormatProperty()
		{
			if (uiConverterName == null && allowedCharacters == null && !useLocalDateTime)
			{
				// old style
				return toSimpleFormatProperty();
			}

			// create json string
			try
			{
				ServoyJSONObject json = new ServoyJSONObject(false, false);

				if (allUpperCase) json.put("allUpperCase", Boolean.TRUE);
				if (allLowerCase) json.put("allLowerCase", Boolean.TRUE);
				if (numberValidator) json.put("numberValidator", Boolean.TRUE);
				if (raw) json.put("raw", Boolean.TRUE);
				if (mask) json.put("mask", Boolean.TRUE);
				if (editOrPlaceholder != null) json.put("editOrPlaceholder", editOrPlaceholder);
				if (displayFormat != null) json.put("displayFormat", displayFormat);
				if (maxLength != null) json.put("maxLength", maxLength);
				if (allowedCharacters != null) json.put("allowedCharacters", allowedCharacters);

				if (uiConverterName != null)
				{
					ServoyJSONObject conv = new ServoyJSONObject(false, false);
					json.put("converter", conv);
					conv.put("name", uiConverterName);
					if (uiConverterProperties != null)
					{
						ServoyJSONObject props = new ServoyJSONObject(false, false);
						for (Entry<String, String> entry : uiConverterProperties.entrySet())
						{
							props.put(entry.getKey(), entry.getValue());
						}
						conv.put("properties", props);
					}
				}

				if (useLocalDateTime) json.put("useLocalDateTime", Boolean.TRUE);

				return json.toString(false);
			}
			catch (JSONException e)
			{
				Debug.error(e);
				return null;
			}
		}

		/**
		 * @return
		 */
		private String toSimpleFormatProperty()
		{
			if (allUpperCase) return maxLength != null ? "|U[" + maxLength.intValue() + ']' : "|U";
			if (allLowerCase) return maxLength != null ? "|L[" + maxLength.intValue() + ']' : "|L";
			if (numberValidator) return maxLength != null ? "|#[" + maxLength.intValue() + ']' : "|#";

			StringBuilder sb = new StringBuilder();

			if (displayFormat != null)
			{
				sb.append(displayFormat);
				if (editOrPlaceholder != null) sb.append('|').append(editOrPlaceholder);
				if (mask) sb.append("|mask");
				if (raw) sb.append("|raw");
			}

			if (maxLength != null) sb.append("|#(").append(maxLength.intValue()).append(')');

			return sb.toString();
		}

		/**
		 * @return the maxLength
		 */
		public Integer getMaxLength()
		{
			return maxLength;
		}

		public void updateMaxLength(Integer maxLength)
		{
			this.maxLength = maxLength;
		}

		public String getDateMask()
		{
			if (!mask) return null;
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
			return !mask && editOrPlaceholder != null && !editOrPlaceholder.equals(displayFormat);
		}

		/**
		 * @return the format
		 */
		public String getFormatString()
		{
			return toFormatProperty();
		}

		public boolean isEmpty()
		{
			return !allUpperCase && !allLowerCase && !numberValidator && displayFormat == null && maxLength == null;
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

		public String getUIConverterName()
		{
			return uiConverterName;
		}

		public Map<String, String> getUIConverterProperties()
		{
			return uiConverterProperties;
		}

		public ParsedFormat getCopy(String newUIConverterName, Map<String, String> newUIConverterProperties)
		{
			return new ParsedFormat(this.allUpperCase, this.allLowerCase, this.numberValidator, this.raw, this.mask, this.editOrPlaceholder, this.displayFormat,
				this.maxLength, newUIConverterName, newUIConverterProperties == null ? null : Collections.unmodifiableMap(newUIConverterProperties),
				allowedCharacters, useLocalDateTime);
		}

		/**
		 * @return
		 */
		public String getAllowedCharacters()
		{
			return allowedCharacters;
		}

		public boolean useLocalDateTime()
		{
			return useLocalDateTime;
		}
	}
}
