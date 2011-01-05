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
public class FlattenedTabPanel extends TabPanel
{
	private final TabPanel tabPanel;

	public FlattenedTabPanel(TabPanel tabPanel)
	{
		super(tabPanel.getParent(), tabPanel.getID(), tabPanel.getUUID());
		this.tabPanel = tabPanel;
		fill();
	}

	private void fill()
	{
		internalClearAllObjects();
		List<TabPanel> panels = new ArrayList<TabPanel>();
		panels.add(tabPanel);
		Form form = (Form)tabPanel.getAncestor(IRepository.FORMS);
		while (form.getExtendsForm() != null)
		{
			if (form.getExtendsForm().getChild(uuid) instanceof TabPanel)
			{
				panels.add((TabPanel)form.getExtendsForm().getChild(uuid));
			}
			form = form.getExtendsForm();
		}
		for (TabPanel panel : panels)
		{
			for (IPersist child : panel.getAllObjectsAsList())
			{
				if (this.getChild(child.getUUID()) == null)
				{
					internalAddChild(child);
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
