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

import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

/**
 * @author jblok
 */
public class LengthDocumentValidator implements ValidatingDocument.IDocumentValidator
{
	int maxLength;

	public LengthDocumentValidator(int length)
	{
		maxLength = length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.text.AbstractDocument#replace(int, int, java.lang.String, javax.swing.text.AttributeSet)
	 */
	public String validateReplace(Document document, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
	{
		if (maxLength == 0) // fix for some of databases returning 0 length for varchar columns
		{
			return text;
		}
		int res = maxLength - document.getLength() + length;
		if (res < 1)
		{
			return null;
		}
		char[] source = text.toCharArray();
		char[] result = new char[Math.min(res, text.length())];
		int j = 0;

		for (int i = 0; i < result.length; i++)
		{
			result[j++] = source[i];
		}
		return new String(result, 0, j);
	}

	public String validateInsertString(Document document, int offs, String str, AttributeSet a) throws BadLocationException
	{
		if (maxLength == 0) // fix for some of databases returning 0 length for varchar columns
		{
			return str;
		}
		int res = maxLength - document.getLength();
		if (res < 1)
		{
			return null;
		}
		char[] source = str.toCharArray();
		char[] result = new char[Math.min(res, str.length())];
		int j = 0;

		for (int i = 0; i < result.length; i++)
		{
			result[j++] = source[i];
		}
		return new String(result, 0, j);
		//		if (res < source.length)
		//		{
		//			//this is disabled for formatted text fields, they should use a
		//			// documentfilter instead of this document!
		//			//            Toolkit.getDefaultToolkit().beep();
		//		}
	}
}
