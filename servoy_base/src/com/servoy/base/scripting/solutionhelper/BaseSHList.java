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
import com.servoy.base.solutionmodel.IBaseSMButton;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.base.solutionmodel.IBaseSMField;
import com.servoy.base.solutionmodel.IBaseSMGraphicalComponent;
import com.servoy.base.solutionmodel.IBaseSMListContainer;
import com.servoy.base.solutionmodel.IBaseSMMethod;

/**
 * @author acostescu
 */
public class BaseSHList implements IBaseSHList
{
	protected final IBaseSMListContainer container;
	protected final BaseSolutionHelper solutionHelper;
	private IBaseSMButton textAndActionAndIconButton;
	private IBaseSMGraphicalComponent subtextComponent;
	private IBaseSMField countComponent;
	private IBaseSMField iconComponent;

	public BaseSHList(IBaseSMListContainer container, BaseSolutionHelper solutionHelper)
	{
		this.container = container;
		this.solutionHelper = solutionHelper;

		// check for existing relevant components
		for (IBaseSMComponent c : container.getComponents())
		{
			if (c instanceof IBaseSMButton && Boolean.TRUE.equals(solutionHelper.getMobileProperties(c).getPropertyValue(IMobileProperties.LIST_ITEM_BUTTON)))
			{
				textAndActionAndIconButton = (IBaseSMButton)c;
			}
			else if (c instanceof IBaseSMGraphicalComponent &&
				Boolean.TRUE.equals(solutionHelper.getMobileProperties(c).getPropertyValue(IMobileProperties.LIST_ITEM_SUBTEXT)))
			{
				subtextComponent = (IBaseSMGraphicalComponent)c;
			}
			else if (c instanceof IBaseSMField)
			{
				if (Boolean.TRUE.equals(solutionHelper.getMobileProperties(c).getPropertyValue(IMobileProperties.LIST_ITEM_COUNT))) countComponent = (IBaseSMField)c;
				else if (Boolean.TRUE.equals(solutionHelper.getMobileProperties(c).getPropertyValue(IMobileProperties.LIST_ITEM_IMAGE))) iconComponent = (IBaseSMField)c;
			}
		}
	}

	protected IBaseSMListContainer getContainer()
	{
		return container;
	}

	public String getCountDataProviderID()
	{
		return countComponent != null ? countComponent.getDataProviderID() : null;
	}

	public void setCountDataProviderID(String countDataProviderID)
	{
		createCountComponentIfNeeded();
		countComponent.setDataProviderID(countDataProviderID);
	}

	public String getText()
	{
		return textAndActionAndIconButton != null ? textAndActionAndIconButton.getText() : null;
	}

	public void setText(String text)
	{
		createTextAndActionAndIconButtonIfNeeded();
		textAndActionAndIconButton.setText(text);
	}

	public String getTextDataProviderID()
	{
		return textAndActionAndIconButton != null ? textAndActionAndIconButton.getDataProviderID() : null;
	}

	public void setTextDataProviderID(String textDataPRoviderID)
	{
		createTextAndActionAndIconButtonIfNeeded();
		textAndActionAndIconButton.setDataProviderID(textDataPRoviderID);
	}

	public void setOnAction(IBaseSMMethod method)
	{
		createTextAndActionAndIconButtonIfNeeded();
		textAndActionAndIconButton.setOnAction(method);
	}

	public IBaseSMMethod getOnAction()
	{
		return textAndActionAndIconButton != null ? textAndActionAndIconButton.getOnAction() : null;
	}

	public String getSubtext()
	{
		return subtextComponent != null ? subtextComponent.getText() : null;
	}

	public void setSubtext(String subtext)
	{
		createSubtextComponentIfNeeded();
		subtextComponent.setText(subtext);
	}

	public String getSubtextDataProviderID()
	{
		return subtextComponent != null ? subtextComponent.getDataProviderID() : null;
	}

	public void setSubtextDataProviderID(String subtextDataProviderID)
	{
		createSubtextComponentIfNeeded();
		subtextComponent.setDataProviderID(subtextDataProviderID);
	}

	public String getDataIconType()
	{
		return textAndActionAndIconButton != null ? solutionHelper.getIconType(textAndActionAndIconButton) : null;
	}

	public void setDataIconType(String iconType)
	{
		createTextAndActionAndIconButtonIfNeeded();
		solutionHelper.setIconType(textAndActionAndIconButton, iconType);
	}

	public String getDataIconDataProviderID()
	{
		return iconComponent != null ? iconComponent.getDataProviderID() : null;
	}

	public void setDataIconDataProviderID(String dataIconDataProviderID)
	{
		createIconComponentIfNeeded();
		iconComponent.setDataProviderID(dataIconDataProviderID);
	}

	private void createTextAndActionAndIconButtonIfNeeded()
	{
		if (textAndActionAndIconButton == null)
		{
			textAndActionAndIconButton = container.newButton(null, 0, 0, 50, 30, null);
			solutionHelper.getMobileProperties(textAndActionAndIconButton).setPropertyValue(IMobileProperties.LIST_ITEM_BUTTON, Boolean.TRUE);
		}
	}

	private void createSubtextComponentIfNeeded()
	{
		if (subtextComponent == null)
		{
			subtextComponent = container.newLabel(null, 0, 0, 50, 30);
			solutionHelper.getMobileProperties(subtextComponent).setPropertyValue(IMobileProperties.LIST_ITEM_SUBTEXT, Boolean.TRUE);
		}
	}

	private void createCountComponentIfNeeded()
	{
		if (countComponent == null)
		{
			countComponent = container.newField(null, IBaseSMField.TEXT_FIELD, 0, 0, 50, 30);
			solutionHelper.getMobileProperties(countComponent).setPropertyValue(IMobileProperties.LIST_ITEM_COUNT, Boolean.TRUE);
		}
	}

	private void createIconComponentIfNeeded()
	{
		if (iconComponent == null)
		{
			iconComponent = container.newField(null, IBaseSMField.TEXT_FIELD, 0, 0, 30, 30);
			solutionHelper.getMobileProperties(iconComponent).setPropertyValue(IMobileProperties.LIST_ITEM_IMAGE, Boolean.TRUE);
		}
	}

}
