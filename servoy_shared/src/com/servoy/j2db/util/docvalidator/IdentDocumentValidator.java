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
package com.servoy.j2db.util.docvalidator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.servoy.j2db.util.Debug;

public class IdentDocumentValidator implements ValidatingDocument.IDocumentValidator
{
	private final int type;

	// Type of identifier determines the rules for the allowed name
	public static final int TYPE_SERVOY = 1;
	public static final int TYPE_SQL = 2;
	public static final int TYPE_JSON = 3;

	public IdentDocumentValidator(int type)
	{
		this.type = type;
	}

	public static String checkName(String name)
	{
		String validated = validateIdentifier(name, TYPE_SERVOY, true);
		if (!(validated != null && validated.equals(name)))
		{
			Debug.warn("The name '" + name + //$NON-NLS-1$
				"' is not a valid indentifier for a component/form, please change this (avoid '-' or other invalid chars, replace those with '_', also can't start with a number), replacement could be: '" + //$NON-NLS-1$
				validated + "'"); //$NON-NLS-1$
		}
		return name;
	}


	// Returns true if s is a legal Java identifier.
	public static boolean isJavaIdentifier(String s)
	{
		String validated = validateIdentifier(s, TYPE_SERVOY, true);
		return validated != null && validated.equals(s);
	}

	// Returns true if s is a legal SQL identifier.
	public static boolean isSQLIdentifier(String s)
	{
		String validated = validateIdentifier(s, TYPE_SQL, true);
		return validated != null && validated.equals(s);
	}

	public String validateInsertString(Document document, int offs, String str, AttributeSet a) throws BadLocationException
	{
		if (validateIdentifier(str, type, offs == 0) != null)
		{
			return str;
		}
		return null;
	}

	public String validateReplace(Document document, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
	{
		String validString = null;
		if (document.getLength() == 0 && text.length() == 0)
		{
			// no replace (this is triggered on the mac)
			return text;
		}
		if (offset == 0 && document.getLength() > 0)
		{
			if (text.length() != 0) validString = validateIdentifier(text, type, true);
			else validString = validateIdentifier((text + document.getText(length, document.getLength() - length)), type, true);
			// delete of start of string
			if ((text.length() == 0 && length == document.getLength()) /* allow make empty */
				|| validString != null)
			{
				return validString;
			}
		}
		else
		{
			validString = validateIdentifier(text, type, document.getLength() == 0);
			if (validString != null)
			{
				return validString;
			}
		}
		return null;
	}

	protected static String validateIdentifier(String str, int type, boolean isStart)
	{
		char[] source = str.toCharArray();
		if (isStart)
		{
			if (source.length == 0)
			{
				return null;
			}
			if (!Character.isJavaIdentifierStart(source[0]))
			{
				return validateIdentifier('_' + str, type, isStart);
			}
			if (type == TYPE_SQL && source[0] == '_') // oracle does not like tables and columns to start with underscore
			{
				return null;
			}
		}

		for (int i = 0; i < source.length; i++)
		{
			if (type == TYPE_JSON)
			{
				if ((source[i] == '\\' || source[i] == '/' || source[i] == '"'))
				{
					return null;
				}
			}
			else if (i > 0)
			{
				if (!Character.isJavaIdentifierPart(source[i]))
				{
					source[i] = '_';
				}
			}
			else if (!Character.isJavaIdentifierPart(source[i]))
			{
				if (type == TYPE_SQL && source[0] == '_') // oracle does not like tables and columns to start with underscore
				{
					return null;
				}
				if (source.length == 0)
				{
					return null;
				}
				return null;
			}
		}
		return new String(source);
	}

}
