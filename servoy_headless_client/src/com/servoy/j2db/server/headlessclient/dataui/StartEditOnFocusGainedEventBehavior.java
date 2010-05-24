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
package com.servoy.j2db.server.headlessclient.dataui;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.servoy.j2db.dataprocessing.IDisplayData;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.ui.IEventExecutor;

/**
 * Behavior for focus gained events.
 * 
 * @author Andrei Costescu
 */
public class StartEditOnFocusGainedEventBehavior extends ServoyAjaxEventBehavior
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new focus gained behavior that will call startEditing when focus is received on the component it is added to.
	 * 
	 */
	public StartEditOnFocusGainedEventBehavior()
	{
		super("onfocus"); //$NON-NLS-1$
	}

	@Override
	protected void onEvent(AjaxRequestTarget target)
	{
		startEditing(getComponent(), target);
	}

	/**
	 * Add a focus gained behavior when it is not there yet.
	 * 
	 * @param component
	 * @return added
	 */
	public static boolean addNewBehaviour(Component component)
	{
		List behaviors = component.getBehaviors();
		if (behaviors != null)
		{
			for (Object behavior : behaviors)
			{
				if (StartEditOnFocusGainedEventBehavior.class.isAssignableFrom(behavior.getClass()))
				{
					return false;
				}
			}
		}
		component.add(new StartEditOnFocusGainedEventBehavior());
		return true;
	}

	public static boolean startEditing(Component component, int modifiers, AjaxRequestTarget target)
	{
		if (component instanceof IDisplayData && ((IDisplayData)component).getDataProviderID() != null &&
			!((IDisplayData)component).getDataProviderID().startsWith(ScriptVariable.GLOBAL_DOT_PREFIX) && !((IDisplayData)component).isReadOnly())
		{
			Object record = component.getInnermostModel().getObject();
			if (record instanceof IRecordInternal)
			{
				return WebEventExecutor.setSelectedIndex(component, target, modifiers) && ((IRecordInternal)record).startEditing();
				// TODO what to do if startEdit == false, do a blur?
			}
		}

		return true;
	}

	public static boolean startEditing(Component component, AjaxRequestTarget target)
	{
		return startEditing(component, IEventExecutor.MODIFIERS_UNSPECIFIED, target);
	}
}