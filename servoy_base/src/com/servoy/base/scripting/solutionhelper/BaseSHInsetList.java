/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.scripting.solutionhelper;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMGraphicalComponent;
import com.servoy.base.solutionmodel.IBaseSMLabel;
import com.servoy.base.solutionmodel.IBaseSMPortal;

/**
 * @author acostescu
 */
public class BaseSHInsetList extends BaseSHList implements IBaseSHInsetList
{
	private IBaseSMGraphicalComponent headerComponent;

	public BaseSHInsetList(IBaseSMPortal portal, BaseSolutionHelper solutionHelper)
	{
		super(portal, solutionHelper);
		for (IBaseSMComponent c : container.getComponents())
		{
			if (c instanceof IBaseSMGraphicalComponent &&
				Boolean.TRUE.equals(solutionHelper.getMobileProperties(c).getPropertyValue(IMobileProperties.LIST_ITEM_HEADER)))
			{
				headerComponent = (IBaseSMGraphicalComponent)c;
			}
		}
	}

	protected IBaseSMPortal getPortal()
	{
		return (IBaseSMPortal)container;
	}

	public String getRelationName()
	{
		return getPortal().getRelationName();
	}

	public void setRelationName(String relationName)
	{
		getPortal().setRelationName(relationName);
	}

	public String getHeaderText()
	{
		return headerComponent != null ? headerComponent.getText() : null;
	}

	public void setHeaderText(String headerText)
	{
		getOrCreateHeaderComponent().setText(headerText);
	}

	public String getHeaderDataProviderID()
	{
		return headerComponent != null ? headerComponent.getDataProviderID() : null;
	}

	public void setHeaderDataProviderID(String headerDataProviderID)
	{
		getOrCreateHeaderComponent().setDataProviderID(headerDataProviderID);
	}

	protected IBaseSMGraphicalComponent getOrCreateHeaderComponent()
	{
		if (headerComponent == null)
		{
			headerComponent = createHeaderComponent();
		}
		return headerComponent;
	}

	protected IBaseSMLabel createHeaderComponent()
	{
		IBaseSMLabel header = container.newLabel(null, 0, 0, 50, 30);
		header.setDisplaysTags(true);
		solutionHelper.getMobileProperties(header).setPropertyValue(IMobileProperties.LIST_ITEM_HEADER, Boolean.TRUE);
		return header;
	}

	public String getName()
	{
		return getPortal().getName();
	}

	public void setName(String name)
	{
		getPortal().setName(name);
	}
}
