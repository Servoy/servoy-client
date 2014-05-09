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

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class TabSequencePropertyWithComponent
{
	private final WebFormComponent component;
	private final String tabSeqProperty;


	public TabSequencePropertyWithComponent(WebFormComponent component, String tabSeqProperty)
	{
		this.component = component;
		this.tabSeqProperty = tabSeqProperty;
	}

	public int getTabSequence()
	{
		return Utils.getAsInteger(component.getInitialProperty(tabSeqProperty));
	}

	public void setCalculatedTabSequence(int tabSequence)
	{
		component.setCalculatedTabSequence(tabSequence, tabSeqProperty);
	}

	public IPersist getPersist()
	{
		return component.getFormElement().getPersist();
	}

	public String getProperty()
	{
		return tabSeqProperty;
	}

	public WebFormComponent getComponent()
	{
		return component;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof TabSequencePropertyWithComponent))
		{
			return false;
		}
		TabSequencePropertyWithComponent other = (TabSequencePropertyWithComponent)obj;
		return Utils.equalObjects(tabSeqProperty, other.tabSeqProperty) && component == other.component;
	}
}
