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

package com.servoy.j2db.persistence;

/**
 * Encapsulation interface for persists that support it.
 *
 * @author lvostinar
 *
 */
public interface ISupportEncapsulation
{

	/**
	 * @param arg the encapsulation mode/level of the persist.
	 */
	void setEncapsulation(int arg);

	/**
	 * The encapsulation mode of this persist. The following can be used/checked:
	 *
	 * <ul>
	 *   <li>Public (not a separate option - if none of the below options are selected)</li>
	 *   <li>Hide in scripting; Module Scope - it will not be available in scripting from any other context except the form itself. Available in designer for the same module</li>
	 *   <li>Module Scope - it will be available in both scripting and designer, but only in the same module</li>
	 *   <li>Hide Dataproviders (checked by default) - the form's dataproviders will not be available from any other context except from the form itself</li>
	 *   <li>Hide Foundset (checked by default) - the form's foundset will not be available from any other context except from the form itself</li>
	 *   <li>Hide Controller - the form's controller will not be available from any other context except from the form itself</li>
	 *   <li>Hide Elements (checked by default) - the form's elements will not be available from any other context except from the form itself</li>
	 *   <li>Hide Containers (checked by default) - the form's containers will not be available from any other context except from the form itself.</li>
	 * </ul>
	 *
	 * @sample "Hide Dataproviders, Hide Foundset, Hide Elements, Hide Containers"
	 * @return the encapsulation mode/level of the persist.
	 */
	int getEncapsulation();

}
