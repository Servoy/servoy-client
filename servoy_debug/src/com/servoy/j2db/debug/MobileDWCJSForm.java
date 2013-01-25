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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.base.scripting.solutionhelper.IBaseSMFormInternal;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSLabel;

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
		return getComponentsInternal(false);
	}

	@Override
	public JSLabel[] getLabels()
	{
		return getLabelsInternal(false);
	}

	@Override
	public JSComponent< ? >[] getComponentsInternal(boolean showInternal)
	{
		JSComponent< ? >[] allComponents = super.getComponents();
		if (!showInternal) allComponents = filterOutLabels(allComponents, allComponents, new JSComponent< ? >[0]);

		return allComponents;
	}

	@Override
	public JSLabel[] getLabelsInternal(boolean showInternal)
	{
		JSLabel[] allLabels = super.getLabels();
		if (!showInternal) allLabels = filterOutLabels(allLabels, super.getComponents(), new JSLabel[0]);

		return allLabels;
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
			JSLabel[] allLabels = getLabelsInternal(true);
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

}
