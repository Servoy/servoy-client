/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.property.ISmartPropertyValue;
import org.sablo.specification.property.IWrappingContext;
import org.sablo.specification.property.types.VisibleSabloValue;

import com.servoy.j2db.server.ngclient.DefaultComponentPropertiesProvider;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class NGVisibleSabloValue extends VisibleSabloValue implements ISmartPropertyValue, IChangeListener
{
	public NGVisibleSabloValue(boolean value, IWrappingContext dataConverterContext)
	{
		super(value, dataConverterContext);
	}

	@Override
	public boolean getValue()
	{
		boolean val = super.getValue();
		if (val)
		{
			DataproviderTypeSabloValue dataProviderValue = (DataproviderTypeSabloValue)context.getWebObject()
				.getRawPropertyValue(DefaultComponentPropertiesProvider.VISIBLE_DATAPROVIDER_NAME);
			if (dataProviderValue != null)
			{
				val = Utils.getAsBoolean(dataProviderValue.getValue());
			}
		}
		return val;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		DataproviderTypeSabloValue dataProviderValue = (DataproviderTypeSabloValue)context.getWebObject()
			.getRawPropertyValue(DefaultComponentPropertiesProvider.VISIBLE_DATAPROVIDER_NAME);
		if (dataProviderValue != null)
		{
			dataProviderValue.addStateChangeListener(this);
		}
	}

	@Override
	public void detach()
	{
		DataproviderTypeSabloValue dataProviderValue = (DataproviderTypeSabloValue)context.getWebObject()
			.getRawPropertyValue(DefaultComponentPropertiesProvider.VISIBLE_DATAPROVIDER_NAME);
		if (dataProviderValue != null)
		{
			dataProviderValue.removeStateChangeListener(this);
		}
	}

	@Override
	public void valueChanged()
	{
		flagChanged(context.getWebObject(), context.getPropertyName());
	}

	public boolean getComponentValue()
	{
		return value;
	}
}
