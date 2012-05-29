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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;

import com.servoy.j2db.server.headlessclient.dataui.IOwnTabSequenceHandler;
import com.servoy.j2db.server.headlessclient.dataui.ISupportWebTabSeq;
import com.servoy.j2db.server.headlessclient.dataui.ServoyAjaxEventBehavior;
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
		if (modifier == null)
		{
			if (newTabIndex != ISupportWebTabSeq.DEFAULT && isTabIndexSupported(component)) component.add(new TabIndexAttributeModifier(newTabIndex));
		}
		else if (newTabIndex != ISupportWebTabSeq.DEFAULT)
		{
			modifier.setTabIndex(newTabIndex);
		}
		else
		{
			component.remove(modifier);
		}

		if (component instanceof IProviderStylePropertyChanges)
		{
			IProviderStylePropertyChanges changeable = (IProviderStylePropertyChanges)component;
			changeable.getStylePropertyChanges().setChanged();
		}

		component.add(new ServoyAjaxEventBehavior("onblur") //$NON-NLS-1$
		{
			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				focusLost = true;
				if (rewind)
				{
					rewind = false;
					target.focusComponent(nextComponentInTabSeq);
				}
			}
		});
	}

	private static boolean isTabIndexSupported(Component component)
	{
		return !(component instanceof WebRect);
	}

	private static boolean focusLost = false;

	public static void setFocusLost(boolean b)
	{
		focusLost = b;
	}

	public static boolean getFocusLost()
	{
		return focusLost;
	}
	
	private static Component nextComponentInTabSeq;

	public static void nextComponentInTabSeq(Component c)
	{
		nextComponentInTabSeq = c;
	}

	private static boolean rewind = false;

	public static void setRewind(boolean b)
	{
		rewind = b;
	}

	public static String getTabIndex(Component c)
	{
		for (Object obeh : c.getBehaviors())
		{
			TabIndexAttributeModifier modifier = null;
			IBehavior beh = (IBehavior)obeh;
			if (beh instanceof TabIndexAttributeModifier)
			{
				modifier = (TabIndexAttributeModifier)beh;
				if (modifier != null)
				{
					modifier.getAttribute();
					IModel< ? > model = modifier.getReplacementModel();
					if (model instanceof IComponentAssignedModel)
					{
						model = ((IComponentAssignedModel< ? >)model).wrapOnAssignment(c);
					}
					Object obj = ((model != null) ? model.getObject() : null);
					if (obj != null && obj instanceof String)
					{
						return (String)obj;
					}
				}
			}
		}
		return null;
	}
}
