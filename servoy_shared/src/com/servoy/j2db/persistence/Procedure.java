/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.persistence;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author rgansevles
 *
 */
public class Procedure implements Serializable
{

	private final String name;

	private final List<ProcedureColumn> parameters;
	private final LinkedHashMap<String, List<ProcedureColumn>> columns;

	/**
	 * @param procedureName
	 */
	public Procedure(String name, List<ProcedureColumn> parameters, LinkedHashMap<String, List<ProcedureColumn>> columns)
	{
		this.name = name;
		this.parameters = parameters;
		this.columns = columns;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the parameters
	 */
	public List<ProcedureColumn> getParameters()
	{
		return parameters;
	}

	/**
	* @return the columns
	*/
	public LinkedHashMap<String, List<ProcedureColumn>> getColumns()
	{
		return columns;
	}

	@Override
	public String toString()
	{
		return "Procedure[" + name + ", param:" + parameters + ", columns:" + columns + "]";
	}
}
