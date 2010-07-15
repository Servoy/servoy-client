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

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.util.Utils;

/**
 * This {@link IModelComparator} will only compare if the components value 
 * then it got from the browser is valid and then tries to compare it with the last rendered value if possible else just the model object.
 * 
 * This makes sure that only user changes compared to the last rendered value are seen as changes.
 * 
 * @author jcompagner
 * 
 */
public class ComponentValueComparator implements IModelComparator
{
	private static final long serialVersionUID = 1L;

	public static final IModelComparator COMPARATOR = new ComponentValueComparator();

	/**
	 * @see wicket.model.IModelComparator#compare(wicket.Component, java.lang.Object)
	 */
	public boolean compare(Component component, Object newObject)
	{
		// When value is not valid re-setting the original value should be accepted
		if (component instanceof IDisplayData && !((IDisplayData)component).isValueValid())
		{
			return false;
		}
		Object previous = null;
		IModel model = component.getInnermostModel();
		if (model instanceof RecordItemModel)
		{
			previous = ((RecordItemModel)model).getLastRenderedValue(component);
		}
		else
		{
			previous = model.getObject();
		}
		// if it is from "" to null or null to "" the value is the same and not overwritten.
		if (previous == null && "".equals(newObject) || "".equals(previous) && newObject == null) return true; //$NON-NLS-1$ //$NON-NLS-2$
		return Utils.equalObjects(previous, newObject);
	}

}
