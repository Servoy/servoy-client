/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.servoy.j2db.server.ngclient.property.PropertyType;

/**
 * Parsed web component api function definition.
 * 
 * @author rgansevles
 *
 */
public class WebComponentApiDefinition
{
	private final String name;
	private final List<WebComponentFunctionParameter> parameters = new ArrayList<>();
	private PropertyType returnType;

	public WebComponentApiDefinition(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void addParameter(WebComponentFunctionParameter parameter)
	{
		parameters.add(parameter);
	}

	public List<WebComponentFunctionParameter> getParameters()
	{
		return Collections.unmodifiableList(parameters);
	}

	public void setReturnType(PropertyType returnType)
	{
		this.returnType = returnType;
	}

	public PropertyType getReturnType()
	{
		return returnType;
	}
}
