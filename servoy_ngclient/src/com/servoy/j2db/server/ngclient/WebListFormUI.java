/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;

/**
 * List view form UI
 * @author gboros
 *
 */
public class WebListFormUI extends WebFormUI
{
	private BodyPortal listViewPortal;

	/**
	 * @param formController
	 */
	public WebListFormUI(IWebFormController formController)
	{
		super(formController);
	}

	@Override
	public void init()
	{
		listViewPortal = null;
		super.init();
	}

	@Override
	public List<FormElement> getFormElements()
	{
		if (cachedElements.size() == 0)
		{
			Form form = getController().getForm();
			List<IFormElement> elements = form.getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			Iterator<IFormElement> it = elements.iterator();
			Part body = FormElementHelper.INSTANCE.getBodyPart(form);
			int bodyStartY = form.getPartStartYPos(body.getID());
			int bodyEndY = body.getHeight();
			while (it.hasNext())
			{
				IFormElement element = it.next();
				if (bodyStartY <= element.getLocation().y && bodyEndY > element.getLocation().y)
				{
					// remove body elements
					it.remove();
				}
			}
			elements.add(getPortal());
			cachedElements = FormElementHelper.INSTANCE.getFormElements(new ArrayList<IPersist>(elements).iterator(), getDataConverterContext());
		}
		return cachedElements;
	}

	protected BodyPortal getPortal()
	{
		if (listViewPortal == null)
		{
			listViewPortal = new BodyPortal(getController().getForm());
		}
		return listViewPortal;
	}
}
