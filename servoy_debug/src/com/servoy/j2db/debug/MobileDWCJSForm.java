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

package com.servoy.j2db.debug;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.base.scripting.solutionhelper.BaseSolutionHelper;
import com.servoy.base.scripting.solutionhelper.IBaseSMFormInternal;
import com.servoy.base.solutionmodel.IBaseSMComponent;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.scripting.solutionmodel.JSBase;
import com.servoy.j2db.scripting.solutionmodel.JSBean;
import com.servoy.j2db.scripting.solutionmodel.JSButton;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSFooter;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSHeader;
import com.servoy.j2db.scripting.solutionmodel.JSInsetList;
import com.servoy.j2db.scripting.solutionmodel.JSLabel;
import com.servoy.j2db.scripting.solutionmodel.JSPortal;
import com.servoy.j2db.solutionmodel.ISMComponent;

/**
 * Solution model JSForm implementation for when a mobile solution is debugged using the web-client.
 * It has some mobile specific behavior, for example filtering out title labels.
 * 
 * @author acostescu
 */
public class MobileDWCJSForm extends JSForm implements IBaseSMFormInternal
{
	public MobileDWCJSForm(IApplication application, Form form, boolean isNew)
	{
		super(application, form, isNew);
	}

	@Override
	public JSComponent< ? >[] getComponents()
	{
		return getComponentsInternal(false, null);
	}

	@Override
	public JSLabel[] getLabels()
	{
		return filterOutLabels(super.getLabels(), getComponentsInternal(true, null), new JSLabel[0]);
	}

	@Override
	public JSComponent< ? >[] getComponentsInternal(boolean showInternal, Integer componentType)
	{
		JSComponent< ? >[] allComponents = super.getComponents();
		if (componentType != null)
		{
			List<JSComponent< ? >> components = Arrays.asList(allComponents);
			for (JSComponent< ? > component : allComponents)
			{
				if (component.getBaseComponent(false).getTypeID() != componentType.intValue())
				{
					components.remove(component);
				}
			}
			allComponents = components.toArray(new JSComponent< ? >[0]);
		}
		if (!showInternal) allComponents = filterOutLabels(allComponents, allComponents, new JSComponent< ? >[0]);

		return allComponents;
	}

	private <T extends JSComponent< ? >> T[] filterOutLabels(T[] toFilter, JSComponent< ? >[] allComponents, T[] resultArrayType)
	{
		List<JSComponent< ? >> filtered = new ArrayList<JSComponent< ? >>(Arrays.asList(toFilter));
		// filter out title labels
		for (JSComponent< ? > c : toFilter)
		{
			BaseComponent base = c.getBaseComponent(false);
			if (base.getTypeID() == IRepositoryConstants.GRAPHICALCOMPONENTS && !ComponentFactory.isButton((GraphicalComponent)base))
			{
				String group = base.getGroupID();
				if (group != null)
				{
					if (Boolean.TRUE.equals(base.getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName)))
					{
						filtered.remove(c);
					}
					else
					{
						// legacy... (before IMobileProperties.COMPONENT_TITLE was introduced)
						// if it's grouped with a field it's a title as well depending on location (legacy); if it's grouped with another label
						// it's the title if the other doesn't have mobile property + correct position
						for (JSComponent< ? > coleague : allComponents)
						{
							BaseComponent coleagueBase = coleague.getBaseComponent(false);
							if (coleague != c && group.equals(coleagueBase.getGroupID()) &&
								!Boolean.TRUE.equals(coleagueBase.getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName)) &&
								(c.getY() < coleague.getY() || (c.getY() == coleague.getY() && c.getX() < coleague.getX())))
							{
								filtered.remove(c);
							}
						}
					}
				}
			}
		}
		return filtered.toArray(resultArrayType);
	}

	@Override
	public boolean removeField(String name)
	{
		JSField f = getField(name);
		boolean deleted = super.removeField(name);
		if (deleted && f != null) deleteTitle(f);
		return deleted;
	}

	@Override
	public boolean removeLabel(String name)
	{
		JSLabel l = getLabel(name);
		boolean deleted = super.removeLabel(name);
		if (deleted && l != null) deleteTitle(l);
		return deleted;
	}

	private void deleteTitle(JSComponent< ? > c)
	{
		String group = c.getGroupID();
		if (group != null)
		{
			JSLabel[] allLabels = getLabels();
			for (JSLabel l : allLabels)
			{
				if (group.equals(l.getGroupID()) &&
					(Boolean.TRUE.equals(l.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName)) || (l.getY() < c.getY() || (l.getY() == c.getY() && l.getX() < c.getX()))))
				{
					l.getBaseComponent(false).getParent().removeChild(l.getBaseComponent(false));
					break;
				}
			}
		}
	}

	@Override
	@JSFunction
	public JSField newField(Object dataprovider, int type, int x, int y, int width, int height)
	{
		return applyDeveloperSettings(super.newField(dataprovider, type, x, y, width, height));
	}

	@Override
	@JSFunction
	public JSButton newButton(String txt, int x, int y, int width, int height, Object action)
	{
		return applyDeveloperSettings(super.newButton(txt, x, y, width, height, action));
	}

	@Override
	@JSFunction
	public JSLabel newLabel(String txt, int x, int y, int width, int height, Object action)
	{
		return applyDeveloperSettings(super.newLabel(txt, x, y, width, height, action));
	}

	@Override
	@JSFunction
	public JSBean newBean(String name, int y)
	{
		return applyDeveloperSettings(newBean(name, null, 0, y, 10, 10));
	}

	private <T extends ISMComponent> T applyDeveloperSettings(T jscomp)
	{
		jscomp.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
		return jscomp;
	}

	@Override
	public JSHeader newHeader()
	{
		JSHeader header = getHeader();
		if (header != null)
		{
			return header;
		}
		return (JSHeader)newTitleHeaderPart(40);
	}

	@Override
	public JSHeader getHeader()
	{
		JSHeader header = (JSHeader)getPart(Part.TITLE_HEADER);
		if (header == null)
		{
			header = (JSHeader)getPart(Part.HEADER);
		}
		return header;
	}

	@Override
	public JSFooter newFooter()
	{
		JSFooter footer = getFooter();
		if (footer != null)
		{
			return footer;
		}
		return (JSFooter)newTitleFooterPart(600);
	}

	@Override
	public JSFooter getFooter()
	{
		JSFooter footer = (JSFooter)getPart(Part.TITLE_FOOTER);
		if (footer == null)
		{
			footer = (JSFooter)getPart(Part.FOOTER);
		}
		return footer;
	}

	@Override
	public JSInsetList newInsetList(int yLocation, String relationName, String headerText, String textDataProviderID)
	{
		String autoGeneratedInsetListName = BaseSolutionHelper.AUTO_CREATED_LIST_INSETLIST_NAME;
		int i = 1;
		while (getComponent(autoGeneratedInsetListName) != null)
		{
			autoGeneratedInsetListName = BaseSolutionHelper.AUTO_CREATED_LIST_INSETLIST_NAME + '_' + (i++);
		}

		// create portal
		JSPortal jsportal = newPortal(autoGeneratedInsetListName, relationName, 0, yLocation, 0, 0);
		Portal portal = jsportal.getBaseComponent(true);
		portal.putCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName, Boolean.TRUE);
		// for debug in developer
		portal.setSize(new Dimension(350, 300));
		portal.setAnchors(IAnchorConstants.ALL);

		// create list abstraction


		// for debug in developer
		JSInsetList insetList = new JSInsetList(jsportal, this);

		// create other persists for remaining contents of list
		if (headerText != null) insetList.setHeaderText(headerText);
		if (textDataProviderID != null) insetList.setTextDataProviderID(textDataProviderID);

		return insetList;
	}

	@Override
	public JSInsetList getInsetList(String name)
	{
		if (name == null) return null;

		JSPortal jsportal = getPortal(name);
		if (jsportal != null && Boolean.TRUE.equals(jsportal.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName)))
		{
			return new JSInsetList(jsportal, this);
		}
		return null;
	}

	@Override
	public JSInsetList[] getInsetLists()
	{
		JSPortal[] jsportals = getPortals();
		List<JSInsetList> insetLists = new ArrayList<JSInsetList>(jsportals.length);
		for (JSPortal jsportal : jsportals)
		{
			if (Boolean.TRUE.equals(jsportal.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName)))
			{
				insetLists.add(new JSInsetList(jsportal, this));
			}
		}
		return insetLists.toArray(new JSInsetList[insetLists.size()]);
	}

	@Override
	public boolean removeInsetList(String name)
	{
		JSPortal jsportal = getPortal(name);
		return jsportal != null &&
			Boolean.TRUE.equals(jsportal.getBaseComponent(false).getCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName)) && removePortal(name);
	}

	@Override
	public void setComponentOrder(IBaseSMComponent[] components)
	{
		int currentHeight = 1;
		if (components != null && components.length > 0)
		{
			boolean footerItems = Boolean.TRUE.equals(((JSBase< ? >)components[0]).getBaseComponent(false).getCustomMobileProperty(
				IMobileProperties.FOOTER_ITEM.propertyName));
			for (IBaseSMComponent comp : components)
			{
				if (comp != null)
				{
					if (footerItems)
					{
						comp.setX(currentHeight);
					}
					else
					{
						comp.setY(currentHeight);
					}
					currentHeight += footerItems ? comp.getWidth() : comp.getHeight();
				}
			}
		}
	}

	@Override
	public <T> T getMobilePropertyValue(IBaseSMComponent c, MobileProperty<T> property)
	{
		return (T)((JSComponent< ? >)c).getBaseComponent(false).getCustomMobileProperty(property.propertyName);
	}

	@Override
	public <T> void setMobilePropertyValue(IBaseSMComponent c, MobileProperty<T> property, T value)
	{
		((JSComponent< ? >)c).getBaseComponent(true).putCustomMobileProperty(property.propertyName, value);
	}
}
