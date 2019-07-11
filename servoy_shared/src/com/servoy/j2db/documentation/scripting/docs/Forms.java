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
package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Helper class for easier documentation of our JavaScript API for forms.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Forms", scriptingName = "forms")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Forms
{
	/**
	 * Get all form names of the current solution.
	 * 
	 * @sample 
	 * var allFormNames = forms.allnames;
	 * application.output("There are " + allFormNames.length + " forms.");
	 * for (var i=0; i<allFormNames.length; i++) 
	 * {
	 * 	var f = forms[allFormNames[i]];
	 * 	application.output("Form " + allFormNames[i] + " has selected index " + f.controller.getSelectedIndex());
	 * }
	 * 
	 * @special
	 * @deprecated use solutionModel.getForms() instead;
	 * an exact replacement, if you need it would be 'solutionModel.getForms().map(function (jsForm) { return jsForm.name } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allnames()
	{
		return null;
	}

	/**
	 * Get the number of forms loaded into memory.
	 *
	 * @sample
	 * application.output("Number of forms loaded into memory: " + forms.length);
	 */
	public Number js_getLength()
	{
		return null;
	}

	public void js_setLength(Number length)
	{
	}

	/**
	 * Get a form by name.
	 * 
	 * @sample 
	 * var allForms = solutionModel.getForms();
	 * application.output("There are " + allForms.length + " forms.");
	 * for (var i=0; i<allForms.length; i++) 
	 * {
	 * 	var f = forms[allForms[i].name];
	 * 	application.output("Form " + allForms[i].name + " has selected index " + f.controller.getSelectedIndex());
	 * }
	 */
	@JSReadonlyProperty
	public FormScope array__indexedby_name()
	{
		return null;
	}

}
