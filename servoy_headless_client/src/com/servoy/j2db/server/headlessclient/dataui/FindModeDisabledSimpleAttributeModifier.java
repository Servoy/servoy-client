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

import org.apache.wicket.Component;

import com.servoy.j2db.ui.IEventExecutor;

/**
 * A base {@link ReadOnlyAndEnableTestAttributeModifier} that is disabled when findmode is enabled for the given component.
 * 
 * @author jcompagner
 *
 */
final class FindModeDisabledSimpleAttributeModifier extends ReadOnlyAndEnableTestAttributeModifier
{
	private final IEventExecutor eventExecutor;

	/**
	 * @param eventExecutor 
	 * @param attribute
	 * @param value
	 */
	FindModeDisabledSimpleAttributeModifier(IEventExecutor eventExecutor, String attribute, CharSequence value)
	{
		super(attribute, value);
		this.eventExecutor = eventExecutor;
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (eventExecutor.getValidationEnabled())
		{
			return super.isEnabled(component);
		}
		return false;
	}
}