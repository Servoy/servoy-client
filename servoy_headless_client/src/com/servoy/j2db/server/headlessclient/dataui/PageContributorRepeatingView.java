/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.j2db.server.headlessclient.dataui;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.servoy.j2db.server.headlessclient.IRepeatingView;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;

/**
 * @author jcompagner
 *
 */
public class PageContributorRepeatingView extends RepeatingView implements IRepeatingView
{
	private final IProviderStylePropertyChanges stylePropertyChangeProvider;
	private final Map<String, Component> externalComponents;

	/**
	 * @param id
	 */
	public PageContributorRepeatingView(String id, IProviderStylePropertyChanges stylePropertyChangeProvider)
	{
		super(id);
		this.stylePropertyChangeProvider = stylePropertyChangeProvider;
		externalComponents = new HashMap<String, Component>();
		setOutputMarkupId(true);
	}

	public boolean addComponent(String name, Component comp)
	{
		if (externalComponents.put(name, comp) == null)
		{
			add(comp);
			stylePropertyChangeProvider.getStylePropertyChanges().setChanged();
			return true;
		}
		return false;
	}

	public Component getComponent(String name)
	{
		return externalComponents.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.server.headlessclient.IPageContributor#removeComponent(java.lang.String)
	 */
	public boolean removeComponent(String name)
	{
		Component comp = externalComponents.remove(name);
		if (comp != null)
		{
			remove(comp);
			stylePropertyChangeProvider.getStylePropertyChanges().setChanged();
			return true;
		}
		return false;

	}
}
