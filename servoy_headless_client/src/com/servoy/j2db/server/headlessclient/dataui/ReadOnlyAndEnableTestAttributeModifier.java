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
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.servoy.j2db.ui.IScriptBaseMethods;
import com.servoy.j2db.ui.IScriptInputMethods;

/**
 * Attribute modifier that checks if the component is enabled and editable else it will disable itself.
 * 
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
class ReadOnlyAndEnableTestAttributeModifier extends AbstractBehavior
{
	private static final long serialVersionUID = 1L;

	/** The attribute */
	private final String attribute;

	/** The value to set */
	private final IModel<CharSequence> model;

	/**
	 * @param eventExecutor
	 * @param attribute
	 * @param value
	 */
	ReadOnlyAndEnableTestAttributeModifier(String attribute, final CharSequence value)
	{
		if (attribute == null)
		{
			throw new IllegalArgumentException("Argument [attr] cannot be null");
		}
		if (value == null)
		{
			throw new IllegalArgumentException("Argument [value] cannot be null");
		}
		this.attribute = attribute;
		this.model = new AbstractReadOnlyModel<CharSequence>()
		{

			@Override
			public CharSequence getObject()
			{
				return value;
			}

		};
	}

	ReadOnlyAndEnableTestAttributeModifier(String attribute, final IModel<CharSequence> value)
	{
		if (attribute == null)
		{
			throw new IllegalArgumentException("Argument [attr] cannot be null");
		}
		if (value == null)
		{
			throw new IllegalArgumentException("Argument [value] cannot be null");
		}
		this.attribute = attribute;
		this.model = value;
	}

	/**
	 * @see com.servoy.j2db.server.headlessclient.dataui.FindModeDisabledSimpleAttributeModifier#isEnabled(org.apache.wicket.Component)
	 */
	@Override
	public boolean isEnabled(Component component)
	{
		if (super.isEnabled(component))
		{
			if (component instanceof IScriptBaseMethods && !((IScriptBaseMethods)component).js_isEnabled()) return false;
			if (component instanceof IScriptInputMethods && !((IScriptInputMethods)component).js_isEditable()) return false;
		}
		return true;
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#onComponentTag(org.apache.wicket.Component,
	 *      org.apache.wicket.markup.ComponentTag)
	 */
	@Override
	public void onComponentTag(final Component component, final ComponentTag tag)
	{
		if (isEnabled(component))
		{
			tag.getAttributes().put(attribute, model.getObject());
		}
	}

}
