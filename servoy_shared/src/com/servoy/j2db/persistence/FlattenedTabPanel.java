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

package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;

/**
 * @author lvostinar
 *
 */
public class FlattenedTabPanel extends TabPanel implements IFlattenedPersistWrapper<TabPanel>
{
	private final TabPanel tabPanel;

	public FlattenedTabPanel(TabPanel tabPanel)
	{
		super(tabPanel.getParent(), tabPanel.getID(), tabPanel.getUUID());
		this.tabPanel = tabPanel;
		fill();
	}

	public TabPanel getWrappedPersist()
	{
		return tabPanel;
	}

	private void fill()
	{
		internalClearAllObjects();
		List<TabPanel> panels = new ArrayList<TabPanel>();
		List<Integer> existingIDs = new ArrayList<Integer>();
		TabPanel panel = tabPanel;
		while (panel != null && !panels.contains(panel))
		{
			panels.add(panel);
			panel = (TabPanel)panel.getSuperPersist();
		}
		for (TabPanel temp : panels)
		{
			for (IPersist child : temp.getAllObjectsAsList())
			{
				Integer extendsID = new Integer(((AbstractBase)child).getExtendsID());
				if (!existingIDs.contains(new Integer(child.getID())) && !existingIDs.contains(extendsID))
				{
					if (((AbstractBase)child).isOverrideOrphanElement())
					{
						// some deleted element
						continue;
					}
					internalAddChild(child);
				}
				if (extendsID.intValue() > 0 && !existingIDs.contains(extendsID))
				{
					existingIDs.add(extendsID);
				}
			}
		}
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		tabPanel.internalRemoveChild(obj);
		fill();
	}

	@Override
	public Tab createNewTab(String text, String relationName, Form f) throws RepositoryException
	{
		return tabPanel.createNewTab(text, relationName, f);
	}

	@Override
	public void addChild(IPersist obj)
	{
		tabPanel.addChild(obj);
		fill();
	}

	@Override
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return tabPanel.getTypedProperty(property);
	}

	@Override
	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		tabPanel.setTypedProperty(property, value);
	}
}
