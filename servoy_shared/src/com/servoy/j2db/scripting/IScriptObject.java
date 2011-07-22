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
package com.servoy.j2db.scripting;


/**
 * Interface to be implemented by all Java objects that must be accessed by JavaScript
 * 
 * @author jblok
 */
public interface IScriptObject extends IReturnedTypesProvider, IScriptable
{
	/**
	 * Get the sample for a methodName
	 * 
	 * @param methodName
	 * @return the sample code
	 */
	public String getSample(String methodName);

	/**
	 * Get the tooltip for a methodName
	 * 
	 * @param methodName
	 * @return the tooltip
	 */
	public String getToolTip(String methodName);

	/**
	 * Get the parameterNames for a methodName
	 * 
	 * @param methodName
	 * @return the parameterNames
	 */
	public String[] getParameterNames(String methodName);

	/**
	 * Will hide methods from developer treeview, but you could leave them in to code so scripting will not break
	 */
	public boolean isDeprecated(String methodName);
}
