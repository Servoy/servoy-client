/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import org.sablo.BaseWebObject;
import org.sablo.WebComponent;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.types.EnabledSabloValue;

import com.servoy.j2db.server.ngclient.IWebFormUI;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;

public class NGEnabledSabloValue extends EnabledSabloValue
{
	/**
	 * @param value
	 * @param dataConverterContext
	 */
	public NGEnabledSabloValue(boolean value, IWrappingContext dataConverterContext)
	{
		super(value, dataConverterContext);
	}

	@Override
	public boolean getValue()
	{
		boolean val = super.getValue();
		if (val)
		{
			BaseWebObject webObject = context.getWebObject();
			if (webObject instanceof IWebFormUI && ((IWebFormUI)webObject).getParentContainer() instanceof WebComponent)
			{
				return ((WebComponent)((IWebFormUI)webObject).getParentContainer()).isEnabled();
			}
		}
		return val;
	}

	@Override
	public void flagChanged(BaseWebObject comp, String propName)
	{
		super.flagChanged(comp, propName);
		if (comp instanceof WebFormComponent)
		{
			IWebFormUI[] visibleForms = ((WebFormComponent)comp).getVisibleForms();
			for (IWebFormUI webFormUI : visibleForms)
			{
				flagChanged(((BaseWebObject)webFormUI), WebFormUI.ENABLED);
			}
		}
	}

	public boolean getComponentValue()
	{
		return value;
	}
}