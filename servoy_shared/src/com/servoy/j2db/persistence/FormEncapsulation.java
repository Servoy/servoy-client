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

import com.servoy.j2db.FlattenedSolution;

/**
 * Interface that specifies the encapsulation flags of the {@link Form#getEncapsulation()}
 * 
 * @author jcompagner
 *
 */
public final class FormEncapsulation
{
	/**
	 * Everything is public.
	 */
	public static final int DEFAULT = 0;

	/**
	 * Completely private form, only used for example in designer in a related tab.
	 * Doesn't show up in the code completion (forms.xxx) at all
	 */
	public static final int PRIVATE = 1;

	/**
	 * Flags the form as module private, only the module itself will see the form, solutions/modules
	 * that have included the module of this form will not see this form in the code completion or 
	 * any other dialog like place tab.
	 */
	public static final int MODULE_PRIVATE = 2;

	/**
	 * Hides the selected record dataproviders (columns,calculations and relations) from code completion. 
	 */
	public static final int HIDE_DATAPROVIDERS = 4;

	/**
	 * Hides the foundset property of the form.
	 */
	public static final int HIDE_FOUNDSET = 8;

	/**
	 * Hides the controller property of the form.
	 */
	public static final int HIDE_CONTROLLER = 16;

	/**
	 * Hides the elements property of the form.
	 */
	public static final int HIDE_ELEMENTS = 32;

	public static boolean isPrivate(Form form, FlattenedSolution fs)
	{
		if (!isModulePrivate(form, fs))
		{
			return (form.getEncapsulation() & PRIVATE) == PRIVATE;
		}
		return true;
	}

	public static boolean isModulePrivate(Form form, FlattenedSolution fs)
	{
		if ((form.getEncapsulation() & MODULE_PRIVATE) == MODULE_PRIVATE || (form.getEncapsulation() & PRIVATE) == PRIVATE)
		{
			return !fs.getSolution().equals(form.getSolution());
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

	public static boolean hideDataproviders(Form form)
	{
		return (form.getEncapsulation() & HIDE_DATAPROVIDERS) == HIDE_DATAPROVIDERS;
	}

}
