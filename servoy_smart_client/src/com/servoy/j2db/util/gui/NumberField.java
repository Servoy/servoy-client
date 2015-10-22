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
package com.servoy.j2db.util.gui;



import javax.swing.JTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Document;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ISkinnable;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.NumberDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument;

public class NumberField extends JTextField implements ISkinnable
{

    private static final String NUMBER_VALIDATOR_NAME = "numberValidator"; //$NON-NLS-1$

	public NumberField(Integer i) 
	{
//        integerFormatter = NumberFormat.getNumberInstance();
//        integerFormatter.setParseIntegerOnly(true);
		setValue(i);
    }

    public Object getValue() 
	{
        Object retVal = new Integer(0);
        try 
		{
            retVal = new Integer(Utils.getAsInteger(getText()));//integerFormatter.parse(getText());
        } 
		catch (Exception e) 
		{
			Debug.error(e);
        }
        return retVal;
    }

    public void setValue(Integer value) 
	{
		if (value == null)
		{
			setText("0"); //$NON-NLS-1$
		}
		else
		{
        	setText(value.toString());// integerFormatter.format(value));
		}
    }

    protected Document createDefaultModel() 
    {
    	ValidatingDocument doc = new ValidatingDocument();
    	doc.setValidator(NUMBER_VALIDATOR_NAME, new NumberDocumentValidator());
        return doc;
    }

	public void setUI(ComponentUI ui)
	{
		super.setUI(ui);
	}
	
	public void setAllowNegativeValues(boolean b)
	{
		ValidatingDocument doc = (ValidatingDocument)getDocument();
		((NumberDocumentValidator)doc.getValidator(NUMBER_VALIDATOR_NAME)).setAllowNegativeValues(b);
	}

}
