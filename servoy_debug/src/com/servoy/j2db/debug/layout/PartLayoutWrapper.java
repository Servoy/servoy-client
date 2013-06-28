/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.debug.layout;

import java.util.ArrayList;
import java.util.List;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSPart;

/**
 * Layout wrapper for solution model parts.
 * 
 * @author rgansevles
 *
 */
public class PartLayoutWrapper implements ILayoutWrapper
{
	private final JSPart part;
	private final JSForm jsform;

	public PartLayoutWrapper(JSPart part, JSForm jsform)
	{
		this.part = part;
		this.jsform = jsform;
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		part.setHeight(y + height);
		if (PersistUtils.isHeaderPart(part.getPartType()))
		{
			MobileFormLayout.layoutHeader(getLayoutElements(part.getPartType()), x, y, width);
		}
		else if (PersistUtils.isFooterPart(part.getPartType()))
		{
			// Adjust body
			JSPart bodyPart = ((JSForm)part.getJSParent()).getBodyPart();
			if (bodyPart != null)
			{
				bodyPart.setHeight(y);
			}
			MobileFormLayout.layoutFooter(getLayoutElements(part.getPartType()), x, y, width);
		}
	}

	private List<ILayoutWrapper> getLayoutElements(int partType)
	{
		List<ILayoutWrapper> elements = new ArrayList<ILayoutWrapper>();
		for (JSComponent< ? > comp : jsform.getComponents())
		{
			if ((PersistUtils.isHeaderPart(partType) && comp.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName) != null) ||
				(PersistUtils.isFooterPart(partType) && comp.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName) != null))
			{
				elements.add(new ComponentLayoutWrapper(comp));
			}
		}
		return elements;
	}

	@Override
	public int getWidth()
	{
		return 0;
	}

	@Override
	public int getHeight()
	{
		return part.getHeight();
	}

	@Override
	public int getX()
	{
		return 0;
	}

	@Override
	public int getY()
	{
		return 0; // not used for parts
	}

	@Override
	public int getPreferredHeight()
	{
		return 0;
	}

	@Override
	public MobileFormSection getElementType()
	{
		return PersistUtils.isHeaderPart(part.getPartType()) ? MobileFormSection.Header : MobileFormSection.Footer;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMobileProperty(MobileProperty<T> property)
	{
		return (T)part.getBaseComponent(false).getCustomMobileProperty(property.propertyName);
	}
}
