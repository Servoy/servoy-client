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

package com.servoy.j2db.server.ngclient.component.spec;

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.util.Pair;

/**
 * @author obuligan
 *
 */
public class ApiMethod
{

	private final String name;
	private final String returnType;
	private final List<Pair<String, String>> parameters;
	private final List<String> optionalParameters;
	private final List<String> hints;

	public ApiMethod(String name, String returnType, List<String> parametersNames, List<String> parameterTypes, List<String> optionalParameters,
		List<String> hints)
	{
		this.name = name;
		this.parameters = new ArrayList<Pair<String, String>>();
		if (parametersNames != null)
		{
			for (int i = 0; i < parametersNames.size(); i++)
			{
				parameters.add(new Pair(parametersNames.get(i), parameterTypes.get(i)));

			}

		}
		this.optionalParameters = optionalParameters;
		this.returnType = returnType;
		this.hints = hints; // metaData contains more info about this method; for example, when the component is used in a table-view/grid layout, should the api method be called on all records or just a selected record?

	}

	public String getName()
	{
		return name;
	}

	public String getReturnType()
	{
		return returnType;
	}

	public String isOptionalParameter(String name)
	{
		return optionalParameters != null ? String.valueOf(optionalParameters.contains(name)) : "false";
	}

	public List<Pair<String, String>> getParameters()
	{
		return parameters;
	}

	public List<String> getHints()
	{
		return hints;
	}

}
