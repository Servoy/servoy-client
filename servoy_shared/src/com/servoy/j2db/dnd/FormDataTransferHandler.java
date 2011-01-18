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
package com.servoy.j2db.dnd;

import java.awt.Point;

import javax.swing.TransferHandler;

import com.servoy.j2db.FormController;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.scripting.JSEvent.EventType;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.ui.IDataRenderer;


/**
 * Class used for handling drag and drop operations in smart client on forms
 * 
 * @author gboros
 */
public class FormDataTransferHandler extends CompositeTransferHandler
{
	private static CompositeTransferHandler instance;

	public static TransferHandler getInstance()
	{
		if (instance == null) instance = new FormDataTransferHandler();

		return instance;
	}

	@Override
	protected JSDNDEvent createScriptEvent(EventType type, ICompositeDragNDrop ddComponent, Object event)
	{
		JSDNDEvent jsEvent = super.createScriptEvent(type, ddComponent, event);

		if (ddComponent instanceof IFormDataDragNDrop)
		{
			IFormDataDragNDrop formDataDDComponent = (IFormDataDragNDrop)ddComponent;
			jsEvent.setFormName(formDataDDComponent.getDragFormName());
			Point location = getEventXY(event);
			if (location != null)
			{
				Object dragSource = ddComponent.getDragSource(location);
				if (dragSource instanceof IDataRenderer)
				{
					IDataRenderer dr = (IDataRenderer)dragSource;
					FormController fc = dr.getDataAdapterList().getFormController();
					jsEvent.setSource(fc.getFormScope());
				}
				else if (dragSource instanceof IComponent)
				{
					jsEvent.setSource(dragSource);
					if (dragSource != null)
					{
						String name = ((IComponent)dragSource).getName();
						if (name != null && name.startsWith(ComponentFactory.WEB_ID_PREFIX))
						{
							name = null;
						}
						jsEvent.setElementName(name);
					}
				}
				IRecordInternal dragRecord = formDataDDComponent.getDragRecord(location);
				if (dragRecord instanceof Record) jsEvent.setRecord((Record)dragRecord);
			}
		}

		return jsEvent;
	}
}
