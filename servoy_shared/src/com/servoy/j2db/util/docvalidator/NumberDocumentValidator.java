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


import java.text.DecimalFormatSymbols;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import com.servoy.j2db.util.ValidatingDocument;
import com.servoy.j2db.util.ValidatingDocument.IDocumentValidator;

/**
 * @author jblok 
*/
public class NumberDocumentValidator extends DocumentFilter implements ValidatingDocument.IDocumentValidator
{
	private boolean allowNegativeValues = true;
	private final DecimalFormatSymbols decimalFormatSymbols;
	private final int maxLength;

	public NumberDocumentValidator()
	{
		this(null, -1);
	}

	/**
	 * @param decimalFormatSymbols
	 */
	public NumberDocumentValidator(DecimalFormatSymbols decimalFormatSymbols, int maxLength)
	{
		this.decimalFormatSymbols = decimalFormatSymbols;
		this.maxLength = maxLength;
	}

	/**
	 * @param maxLength
	 */
	public NumberDocumentValidator(int maxLength)
	{
		this(null, maxLength);
	}

	public String validateInsertString(Document document, int offs, String str, AttributeSet a) throws BadLocationException
	{
		return validateReplace(document, offs, 0, str, a);
	}

	/**
	 * @param ch
	 * @return
	 */
	private boolean testchar(char ch)
	{
		if (Character.isDigit(ch)) return true;
		if (decimalFormatSymbols != null)
		{
			if (decimalFormatSymbols.getGroupingSeparator() == ch || decimalFormatSymbols.getPercent() == ch ||
				(allowNegativeValues && decimalFormatSymbols.getMinusSign() == ch)) return true;
			if ((decimalFormatSymbols.getDecimalSeparator() == ch || decimalFormatSymbols.getMonetaryDecimalSeparator() == ch)) return true;
			if (decimalFormatSymbols.getCurrencySymbol() != null && decimalFormatSymbols.getCurrencySymbol().indexOf(ch) != -1) return true;
			return false;
		}
		return ch == '.' || ch == ',' || (ch == '-' && allowNegativeValues);
	}

	/**
	 * @param b
	 */
	public void setAllowNegativeValues(boolean b)
	{
		allowNegativeValues = b;
	}

	public String validateReplace(Document document, int offset, int length, String str, AttributeSet attrs) throws BadLocationException
	{
		char[] source = str.toCharArray();
		char[] result = new char[source.length];
		int j = 0;

		for (int i = 0; i < result.length; i++)
		{
			char ch = source[i];
			if (testchar(ch))
			{
				result[j++] = ch;
			}
		}
		if (j > 0)
		{
			String retString = new String(result, 0, j);
			String currentTxt = document.getText(0, offset) + retString + document.getText(offset + length, document.getLength() - offset - length);
			if (decimalFormatSymbols != null)
			{
				int index1 = currentTxt.indexOf(decimalFormatSymbols.getDecimalSeparator());
				int index2 = currentTxt.lastIndexOf(decimalFormatSymbols.getDecimalSeparator());
				if (index1 != index2) return ""; //$NON-NLS-1$
				int index11 = currentTxt.indexOf(decimalFormatSymbols.getMonetaryDecimalSeparator());
				int index22 = currentTxt.lastIndexOf(decimalFormatSymbols.getMonetaryDecimalSeparator());
				if (index11 != index22) return ""; //$NON-NLS-1$

				int lastGroupIndex = currentTxt.lastIndexOf(decimalFormatSymbols.getGroupingSeparator());
				if (lastGroupIndex != -1)
				{
					if (index1 != -1 && lastGroupIndex > index1) return ""; //$NON-NLS-1$
					if (index11 != -1 && lastGroupIndex > index11) return ""; //$NON-NLS-1$
				}

				int minusIndex = currentTxt.indexOf(decimalFormatSymbols.getMinusSign());
				if (minusIndex != -1 && minusIndex != 0 && minusIndex != currentTxt.length() - 1) return ""; //$NON-NLS-1$

				int currencyIndex = currentTxt.indexOf(decimalFormatSymbols.getCurrencySymbol());
				if (currencyIndex != -1 && currencyIndex != 0 && !currentTxt.endsWith(decimalFormatSymbols.getCurrencySymbol())) return ""; //$NON-NLS-1$

				int percentIndex = currentTxt.indexOf(decimalFormatSymbols.getPercent());
				if (percentIndex != -1 && percentIndex != currentTxt.length() - 1) return ""; //$NON-NLS-1$
			}
			if (maxLength > 0)
			{
				int counter = 0;
				for (int k = 0; k < currentTxt.length(); k++)
				{
					if (Character.isDigit(currentTxt.charAt(k)))
					{
						counter++;
					}
				}
				if (counter > maxLength) return ""; //$NON-NLS-1$
			}
			return retString;
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see javax.swing.text.DocumentFilter#insertString(javax.swing.text.DocumentFilter.FilterBypass, int, java.lang.String, javax.swing.text.AttributeSet)
	 */
	@Override
	public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException
	{
		String test = validateReplace(fb.getDocument(), offset, 0, text, attrs);
		super.insertString(fb, offset, test, attrs);
	}

	/**
	 * @see javax.swing.text.DocumentFilter#replace(javax.swing.text.DocumentFilter.FilterBypass, int, int, java.lang.String, javax.swing.text.AttributeSet)
	 */
	@Override
	public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
	{
		String test = validateReplace(fb.getDocument(), offset, length, text, attrs);
		super.replace(fb, offset, length, test, attrs);
	}

}
