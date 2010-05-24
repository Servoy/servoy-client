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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;

public class FilterBackspaceKeyAttributeModifier extends AttributeModifier {

	private static final long serialVersionUID = 2158927362909285334L;

	public static final String SCRIPT = "filterBackKey(event)"; //$NON-NLS-1$
	
	public FilterBackspaceKeyAttributeModifier(Model model)
	{
		super("onKeyDown", true, model); //$NON-NLS-1$
	}

    protected String newValue(String currentValue, String replacementValue)
    {
    	if (replacementValue == null)
    	{
    		return currentValue;
    	}
    	else
    	{
	    	if (currentValue == null || currentValue.trim().length() == 0)
	    		return "return " + replacementValue + ";"; //$NON-NLS-1$ //$NON-NLS-2$
	    	else
	    		return "if (" + replacementValue + ") { " + currentValue + " } else return false;";   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    	}
    }
	
}
