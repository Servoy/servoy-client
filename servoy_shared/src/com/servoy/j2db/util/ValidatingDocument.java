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

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


public class ValidatingDocument extends PlainDocument
{
	private final Map validators = new HashMap();
	private IDocumentValidator[] validatorInstances;

	public ValidatingDocument()
	{
	}

	/**
	 * @param validators
	 */
	public ValidatingDocument(IDocumentValidator[] validators)
	{
		super();
		setAnonymousValidators(validators);
	}

	/**
	 * @param validators
	 */
	public ValidatingDocument(IDocumentValidator validator)
	{
		this(new IDocumentValidator[] { validator });
	}

	/**
	 * @param c
	 * @param validators
	 */
	public ValidatingDocument(Content c, IDocumentValidator[] validators)
	{
		super(c);
		setAnonymousValidators(validators);
	}

	private void setAnonymousValidators(IDocumentValidator[] validators)
	{
		for (IDocumentValidator element : validators)
		{
			// anonymous validators
			this.validators.put(new Object(), element);
		}
		validatorInstances = validators;
	}


	/**
	 * Set a validator by name, can be retrieved back in getValidator(String)
	 * 
	 * @param name
	 * @param validator
	 */
	public void setValidator(String name, IDocumentValidator validator)
	{
		validators.put(name, validator);
		validatorInstances = new IDocumentValidator[validators.size()];
		Iterator it = validators.values().iterator();
		for (int i = 0; it.hasNext(); i++)
		{
			validatorInstances[i] = (IDocumentValidator)it.next();
		}
	}

	/**
	 * Get a named validator.
	 * 
	 * @param name
	 * @return
	 */
	public IDocumentValidator getValidator(String name)
	{
		return (IDocumentValidator)validators.get(name);
	}

	/**
	 * If one of the validators disapprove, reject the insert
	 */
	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
	{
		String s = str;
		if (s != null)
		{
			for (int i = 0; validatorInstances != null && i < validatorInstances.length; i++)
			{
				String s2 = validatorInstances[i].validateInsertString(this, offs, s, a);
				if (!s.equalsIgnoreCase(s2))
				{
					try
					{
						Toolkit.getDefaultToolkit().beep();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
					if (s2 == null)
					{
						return;
					}
				}
				s = s2;
			}
		}
		super.insertString(offs, s, a);
	}

	/**
	 * If one of the validators disapprove, reject the replace
	 */
	@Override
	public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException
	{
		String s = text;
		if (s != null)
		{
			for (int i = 0; validatorInstances != null && i < validatorInstances.length; i++)
			{
				String s2 = validatorInstances[i].validateReplace(this, offset, length, s, attrs);
				if (!s.equalsIgnoreCase(s2))
				{
					try
					{
						Toolkit.getDefaultToolkit().beep();
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
					if (s2 == null)
					{
						return;
					}
				}
				s = s2;
			}
		}
		super.replace(offset, length, s, attrs);
	}

	/**
	 * Interface for validating the input.
	 * 
	 * @author rgansevles
	 * 
	 */
	public interface IDocumentValidator
	{

		/**
		 * Validate and/or modify the string to be in inserted
		 * 
		 * @param offs
		 * @param str
		 * @param a
		 * @return modified string or null when insert should be rejected
		 * @throws BadLocationException
		 */
		public String validateInsertString(Document document, int offs, String str, AttributeSet a) throws BadLocationException;

		/**
		 * Validate and/or modify the string to be in replaced
		 * 
		 * @param offset
		 * @param length
		 * @param text
		 * @param attrs
		 * @return modified string or null when replace should be rejected
		 * @throws BadLocationException
		 */
		public String validateReplace(Document document, int offset, int length, String text, AttributeSet attrs) throws BadLocationException;

	}

}
