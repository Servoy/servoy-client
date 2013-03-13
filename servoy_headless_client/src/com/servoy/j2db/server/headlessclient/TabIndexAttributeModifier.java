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
package com.servoy.j2db.server.headlessclient;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;

import com.servoy.j2db.server.headlessclient.dataui.WebDataHtmlView;

/**
 * An {@link AttributeModifier} that takes care of the TabIndex.
 * 
 * @author gerzse
 */
public class TabIndexAttributeModifier extends AttributeModifier
{

	private static class TabIndexModel extends Model<String>
	{
		private int tabIndex;

		public TabIndexModel(int tabIndex)
		{
			this.tabIndex = tabIndex;
		}

		public void setTabIndex(int newTabIndex)
		{
			tabIndex = newTabIndex;
		}

		@Override
		public String getObject()
		{
			return String.valueOf(tabIndex);
		}
	}

	public TabIndexAttributeModifier(int tabIndex)
	{
		super("tabIndex", true, new TabIndexModel(tabIndex)); //$NON-NLS-1$
	}

	public void setTabIndex(int newTabIndex)
	{
		((TabIndexModel)getReplaceModel()).setTabIndex(newTabIndex);
	}

	public int getTabIndex()
	{
		return ((TabIndexModel)getReplaceModel()).tabIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.wicket.AttributeModifier#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		// special case for the html view, thats just a div, doesn't need
		// to add a tabIndex, but it does need it to push it to its content (anchors)
		if (component instanceof WebDataHtmlView) return false;
		return super.isEnabled(component);
	}

}