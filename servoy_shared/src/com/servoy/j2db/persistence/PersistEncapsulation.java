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

package com.servoy.j2db.persistence;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.FlattenedSolution;

/**
 * Interface that specifies the encapsulation flags of the {@link ISupportEncapsulation#getEncapsulation()}
 *
 * @author jcompagner
 *
 */
public final class PersistEncapsulation
{
	/**
	 * Everything is public.
	 */
	public static final int DEFAULT = IFormConstants.DEFAULT;

	/**
	 * Completely private persist as scripting is concerned, only used for example in designer in a related tab (identical to MODULE_SCOPE as far as designer is concerned).
	 * Doesn't show up in the code completion (forms.xxx) at all
	 */
	public static final int HIDE_IN_SCRIPTING_MODULE_SCOPE = IFormConstants.HIDE_IN_SCRIPTING_MODULE_SCOPE;

	/**
	 * Flags the persist as module scope, only the module itself will see the persist, solutions/modules
	 * that have included the module of this persist will not see this persist in the code completion or
	 * any other dialog like place tab.
	 */
	public static final int MODULE_SCOPE = IFormConstants.MODULE_SCOPE;

	/**
	 * Hides the selected record dataproviders (columns,calculations and relations) from code completion.
	 */
	public static final int HIDE_DATAPROVIDERS = IFormConstants.HIDE_DATAPROVIDERS;

	/**
	 * Hides the foundset property of the form.
	 */
	public static final int HIDE_FOUNDSET = IFormConstants.HIDE_FOUNDSET;

	/**
	 * Hides the controller property of the form.
	 */
	public static final int HIDE_CONTROLLER = IFormConstants.HIDE_CONTROLLER;

	/**
	 * Hides the elements property of the form.
	 */
	public static final int HIDE_ELEMENTS = IFormConstants.HIDE_ELEMENTS;

	/**
	 * Hides the containers property of the form.
	 */
	public static final int HIDE_CONTAINERS = IFormConstants.HIDE_CONTAINERS;

	public static boolean isHideInScriptingModuleScope(ISupportEncapsulation persist, FlattenedSolution fs)
	{
		if (!isModuleScope(persist, fs.getSolution()))
		{
			return (persist.getEncapsulation() & HIDE_IN_SCRIPTING_MODULE_SCOPE) == HIDE_IN_SCRIPTING_MODULE_SCOPE;
		}
		return true;
	}

	public static boolean isModuleScope(ISupportEncapsulation persist, Solution solution)
	{
		if ((persist.getEncapsulation() & MODULE_SCOPE) == MODULE_SCOPE ||
			(persist.getEncapsulation() & HIDE_IN_SCRIPTING_MODULE_SCOPE) == HIDE_IN_SCRIPTING_MODULE_SCOPE)
		{
			return !((IPersist)persist).getRootObject().equals(solution);
		}
		return false;
	}

	public static boolean hideFoundset(Form form)
	{
		return (form.getEncapsulation() & HIDE_FOUNDSET) == HIDE_FOUNDSET;
	}

	public static boolean hideController(Form form)
	{
		return (form.getEncapsulation() & HIDE_CONTROLLER) == HIDE_CONTROLLER;
	}

	public static boolean hideElements(Form form)
	{
		return (form.getEncapsulation() & HIDE_ELEMENTS) == HIDE_ELEMENTS;
	}

	public static boolean hideContainers(Form form)
	{
		return (form.getEncapsulation() & HIDE_CONTAINERS) == HIDE_CONTAINERS;
	}

	public static boolean hideDataproviders(Form form)
	{
		return (form.getEncapsulation() & HIDE_DATAPROVIDERS) == HIDE_DATAPROVIDERS;
	}

	public static boolean hasEncapsulation(ISupportEncapsulation persist, int encapsulation)
	{
		if ((encapsulation & persist.getEncapsulation()) == encapsulation) return true;
		return false;
	}

}
