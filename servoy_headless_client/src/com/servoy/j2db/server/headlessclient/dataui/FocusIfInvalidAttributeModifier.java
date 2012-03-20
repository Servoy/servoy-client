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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;

import com.servoy.j2db.dataprocessing.IDisplayData;

/**
 * This modifier will make sure that invalid components will grab focus.
 * 
 * @author acostescu
 */
public class FocusIfInvalidAttributeModifier extends AttributeModifier
{

	private static final long serialVersionUID = 1L;

	public FocusIfInvalidAttributeModifier(final FormComponent component)
	{
		// please keep the case in the event name
		super("onblur", true, new Model<String>() //$NON-NLS-1$
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getObject()
				{
					Object mo = component.getModelObject();
					String value;
					if (mo instanceof Boolean)
					{
						value = mo.toString();
					}
					else
					{
						value = component.getValue();
						if (value == null)
						{
							value = ""; //$NON-NLS-1$
						}
						else
						{
							value = '\'' + Strings.escapeMarkup(value).toString() + '\'';
						}
					}
					return "scheduleRequestFocusOnInvalid(\"" + component.getMarkupId() + "\", \"" + value + "\");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			});
	}

	@Override
	public boolean isEnabled(Component component)
	{
		return !((IDisplayData)component).isValueValid();
	}

}