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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.IBehavior;

import com.servoy.j2db.server.headlessclient.dataui.IOwnTabSequenceHandler;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.WebRect;
import com.servoy.j2db.ui.IProviderStylePropertyChanges;
import com.servoy.j2db.util.ISupplyFocusChildren;

/**
 * Helper class for setting up tabindex attribute modifiers for components.
 *
 * @see com.servoy.j2db.server.headlessclient.TabIndexAttributeModifier
 * 
 * @author gerzse
 */
public class TabIndexHelper
{

	public static void setUpTabIndexAttributeModifier(Component comp, int newTabIndex)
	{
		if (comp instanceof IOwnTabSequenceHandler)
		{
			((IOwnTabSequenceHandler)comp).handleOwnTabIndex(newTabIndex);
		}
		else
		{
			if (comp instanceof ISupplyFocusChildren)
			{
				for (Component c : ((ISupplyFocusChildren<Component>)comp).getFocusChildren())
				{
					setUpTabIndexAttributeModifierInternal(c, newTabIndex);
				}
			}
			else
			{
				setUpTabIndexAttributeModifierInternal(comp, newTabIndex);
			}
		}
	}

	private static void setUpTabIndexAttributeModifierInternal(Component comp, int newTabIndex)
	{
		TabIndexAttributeModifier modifier = null;
		final Component component = comp;
		for (Object obeh : component.getBehaviors())
		{
			IBehavior beh = (IBehavior)obeh;
			if (beh instanceof TabIndexAttributeModifier)
			{
				modifier = (TabIndexAttributeModifier)beh;
				break;
			}
		}
		boolean componentChanged = true;
		boolean changeForClientSide = false;
		if (modifier == null)
		{
			if (newTabIndex != ISupportWebTabSeq.DEFAULT && isTabIndexSupported(component)) component.add(new TabIndexAttributeModifier(newTabIndex));
		}
		else if (newTabIndex != ISupportWebTabSeq.DEFAULT)
		{
			if (newTabIndex != modifier.getTabIndex())
			{
				modifier.setTabIndex(newTabIndex);
				if (!(component instanceof IProviderStylePropertyChanges) || !((IProviderStylePropertyChanges)component).getStylePropertyChanges().isChanged())
				{
					// if is already changed leave it to server side
					changeForClientSide = true;
				}
			}
			else
			{
				componentChanged = false;
			}
		}
		else
		{
			component.remove(modifier);
		}

		if (componentChanged)
		{
			MainPage page = component.findParent(MainPage.class);
			if (changeForClientSide)
			{
				if (page != null)
				{
					page.getPageContributor().addTabIndexChange(component.getMarkupId(), newTabIndex);
				}
				else
				{
					changeForClientSide = false;
				}
			}
			if (!changeForClientSide && component instanceof IProviderStylePropertyChanges)
			{
				IProviderStylePropertyChanges changeable = (IProviderStylePropertyChanges)component;
				changeable.getStylePropertyChanges().setChanged();
				if (page != null)
				{
					page.getPageContributor().addTabIndexChange(component.getMarkupId(), ISupportWebTabSeq.DEFAULT);
				}
			}
		}
	}

	public static int getTabIndex(Component component)
	{
		TabIndexAttributeModifier modifier = null;
		for (Object obeh : component.getBehaviors())
		{
			IBehavior beh = (IBehavior)obeh;
			if (beh instanceof TabIndexAttributeModifier)
			{
				modifier = (TabIndexAttributeModifier)beh;
				break;
			}
		}
		if (modifier != null) return modifier.getTabIndex();
		else return -1;
	}

	private static boolean isTabIndexSupported(Component component)
	{
		return !(component instanceof WebRect);
	}

}
