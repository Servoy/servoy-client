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

package com.servoy.j2db.scripting.solutionhelper;

import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMForm;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMGraphicalComponent;
import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMTabPanel;

/**
 * @author acostescu
 */
public class BaseSHInsetList extends BaseSHList implements IBaseSHInsetList
{

	private final IBaseSMTabPanel tabPanel;
	private IBaseSMGraphicalComponent headerComponent;

	public BaseSHInsetList(IBaseSMTabPanel tabPanel, IBaseSMForm listForm, BaseSolutionHelper solutionHelper)
	{
		super(listForm, solutionHelper);
		this.tabPanel = tabPanel;
	}

	public String getRelationName()
	{
		return tabPanel.getTabs()[0].getRelationName();
	}

	public void setRelationName(String relationName)
	{
		tabPanel.getTabs()[0].setRelationName(relationName);
	}

	public String getHeaderText()
	{
		return headerComponent != null ? headerComponent.getText() : null;
	}

	public void setHeaderText(String headerText)
	{
		createHeaderComponentIfNeeded();
		headerComponent.setText(headerText);
	}

	public String getHeaderDataProviderID()
	{
		return headerComponent != null ? headerComponent.getDataProviderID() : null;
	}

	public void setHeaderDataProviderID(String headerDataProviderID)
	{
		createHeaderComponentIfNeeded();
		headerComponent.setDataProviderID(headerDataProviderID);
	}

	private void createHeaderComponentIfNeeded()
	{
		if (headerComponent == null)
		{
			headerComponent = form.newLabel(null, 0, 0, 0, 0);
			solutionHelper.getMobileProperties(headerComponent).setPropertyValue(IMobileProperties.LIST_ITEM_HEADER, Boolean.TRUE);
		}
	}

}
