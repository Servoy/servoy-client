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

package com.servoy.j2db.ui.scripting;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.CustomValueList;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.ValueListFactory;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.ui.IFieldComponent;
import com.servoy.j2db.ui.IScriptValuelistMethods;
import com.servoy.j2db.ui.IStylePropertyChangesRecorder;
import com.servoy.j2db.ui.ISupportValueList;

/**
 * Abstract scriptable component with valuelist support.
 * 
 * @author lvostinar
 * @since 6.0
 */
public abstract class AbstractRuntimeValuelistComponent<C extends IFieldComponent> extends AbstractRuntimeField<C> implements IScriptValuelistMethods
{
	public AbstractRuntimeValuelistComponent(IStylePropertyChangesRecorder jsChangeRecorder, IApplication application)
	{
		super(jsChangeRecorder, application);
	}

	public String js_getValueListName()
	{
		if (getComponent() instanceof ISupportValueList)
		{
			IValueList list = ((ISupportValueList)getComponent()).getValueList();
			if (list != null)
			{
				return list.getName();
			}
		}
		return null;
	}

	public void js_setValueListItems(Object value)
	{
		if (getComponent() instanceof ISupportValueList)
		{
			IValueList list = ((ISupportValueList)getComponent()).getValueList();
			if (list != null && (value instanceof JSDataSet || value instanceof IDataSet))
			{
				String name = list.getName();
				ValueList valuelist = application.getFlattenedSolution().getValueList(name);
				if (valuelist != null && valuelist.getValueListType() == ValueList.CUSTOM_VALUES)
				{
					String format = null;
					int type = 0;
					if (list instanceof CustomValueList)
					{
						format = ((CustomValueList)list).getFormat();
						type = ((CustomValueList)list).getValueType();
					}
					IValueList newVl = ValueListFactory.fillRealValueList(application, valuelist, ValueList.CUSTOM_VALUES, format, type, value);
					((ISupportValueList)getComponent()).setValueList(newVl);
				}
			}
		}
	}
}
