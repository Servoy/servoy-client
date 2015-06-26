/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

import java.util.Collection;

import org.json.JSONException;

/**
 * @author gboros
 *
 */
public class FormElementContext
{
	private final FormElement formElement;
	private final IServoyDataConverterContext context;

	public FormElementContext(FormElement formElement)
	{
		this(formElement, null);
	}

	public FormElementContext(FormElement formElement, IServoyDataConverterContext context)
	{
		this.formElement = formElement;
		this.context = context;
	}

	public String getPropertiesString() throws JSONException
	{
		return formElement.propertiesAsTemplateJSON(null, this).toString();
	}

	public String getTypeName()
	{
		return formElement.getTypeName();
	}

	public String getName()
	{
		return formElement.getName();
	}

	public Collection<String> getHandlers()
	{
		return formElement.getHandlers();
	}

	public FormElement getFormElement()
	{
		return formElement;
	}

	public IServoyDataConverterContext getContext()
	{
		return context;
	}
}
