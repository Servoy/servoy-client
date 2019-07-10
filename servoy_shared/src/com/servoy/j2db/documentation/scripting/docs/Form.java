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
import com.servoy.j2db.BasicFormController.JSForm;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Helper class for easier documentation of our JavaScript API for forms.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "RuntimeForm", scriptingName = "RuntimeForm")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Form
{
	/**
	 * Get the foundset of the form.
	 * 
	 * @sample
	 * application.output("selected index in form foundset: " + %%prefix%%foundset.getSelectedIndex());
	 */
	public FoundSet js_getFoundset()
	{
		return null;
	}

	public void js_setFoundset(FoundSet foundset)
	{
	}

	/**
	 * Get all script names of the form.
	 * 
	 * @sample
	 * var methodNames = %%prefix%%allmethods;
	 * application.output("This form has " + methodNames.length + " methods defined.")
	 * for (var i=0; i<methodNames.length; i++)
	 * 	application.output(methodNames[i]);
	 * 
	 * @special
	 * @deprecated use solutionModel - JSForm.getMethods(Boolean) or JSForm.getMethods() instead;
	 * an exact replacement, if you need it would be 'solutionModel.getForm(controller.getName()).getMethods().map(function (jsMethod) { return jsMethod.getName() } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allmethods()
	{
		return null;
	}

	/**
	 * Get the names of all elements on the form as an array.
	 * 
	 * @sample
	 * var names = %%prefix%%allnames;
	 * application.output("This form has " + names.length + " named items.")
	 * for (var i=0; i<names.length; i++)
	 * 	application.output(names[i]);
	 * 
	 * @special
	 * @deprecated use elements.allnames instead.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allnames()
	{
		return null;
	}

	/**
	 * Get all dataproviders of the form's foundset.
	 * 
	 * @sample
	 * var dataprovidersNames = %%prefix%%alldataproviders;
	 * application.output("This form has " + dataprovidersNames.length + " data providers.")
	 * for (var i=0; i<dataprovidersNames.length; i++)
	 * 	application.output(dataprovidersNames[i]);
	 * 
	 * @special
	 * @deprecated use foundset.alldataproviders instead.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] alldataproviders()
	{
		return null;
	}

	/**
	 * Get all form variable names.
	 * 
	 * @sample
	 * var varNames = %%prefix%%allvariables;
	 * application.output("This form has " + varNames.length + " variables defined.")
	 * for (var i=0; i<varNames.length; i++)
	 * 	application.output(varNames[i]);
	 * 
	 * @special 
	 * @deprecated use solutionModel - JSForm.getVariables(Boolean) or JSForm.getVariables() instead;
	 * an exact replacement, if you need it would be 'solutionModel.getForm(controller.getName()).getVariables().map(function (jsVariable) { return jsVariable.name } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allvariables()
	{
		return null;
	}

	/**
	 * Get all relation names of the form.
	 * 
	 * @sample
	 * var relationsNames = %%prefix%%allrelations;
	 * application.output("This form has " + relationsNames.length + " relations.")
	 * for (var i=0; i<relationsNames.length; i++)
	 * 	application.output(relationsNames[i]);
	 * 
	 * @special
	 * @deprecated use solutionModel.getRelations(dataSource) instead, where dataSource is provided by solution model's JSForm.dataSource or controller.getDataSource();
	 * an exact replacement, if you need it would be 'solutionModel.getRelations(controller.getDataSource()).map(function (jsRelation) { return jsRelation.name } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allrelations()
	{
		return null;
	}

	/**
	 * Get an array with the elements in the form.
	 * 
	 * @sample
	 * var elems = %%prefix%%elements;
	 * application.output("This form has " + elems.length + " named elements.")
	 * for (var i=0; i<elems.length; i++)
	 * 	application.output(elems[i].getName());
	 */
	@JSReadonlyProperty
	public FormElements elements()
	{
		return null;
	}

	/**
	 * Get the controller of the form.
	 * 
	 * @sample
	 * %%prefix%%controller.enabled = !%%prefix%%controller.enabled;
	 */
	@JSReadonlyProperty
	public JSForm controller()
	{
		return null;
	}

	/**
	 * Provides access to the methods of the parent of this form.
	 * 
	 * @sample
	 * %%prefix%%_super.some_method();
	 */
	@JSReadonlyProperty
	@ServoyClientSupport(mc = false, wc = true, sc = true)
	public FormScope _super()
	{
		return null;
	}

}