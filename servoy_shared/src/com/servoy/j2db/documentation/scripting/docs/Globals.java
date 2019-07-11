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
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * Helper class for easier documentation of our JavaScript API for globals.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Globals", scriptingName = "globals")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Globals
{
	/**
	 * Get all script names of this global scope.
	 * 
	 * @sample
	 * var allMethodNames = scopes.globals.allmethods;
	 * application.output("There are " + allMethodNames.length + " global methods.");
	 * for (var i=0; i<allMethodNames.length; i++)
	 * 	application.output(allMethodNames[i]);
	 *  
	 * @special
	 * @deprecated use solutionModel.getGlobalMethods("scopeName") instead;
	 * an exact replacement, if you need it would be 'solutionModel.getGlobalMethods("scopeName").map(function (jsMethod) { return jsMethod.getName() } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allmethods()
	{
		return null;
	}

	/**
	 * Get all variable names of this global scope.
	 * 
	 * @sample
	 * var allVarNames = scopes.globals.allvariables;
	 * application.output("There are " + allVarNames.length + " global variables.");
	 * for (var i=0; i<allVarNames.length; i++)
	 * 	application.output(allVarNames[i]);
	 * 
	 * @special
	 * @deprecated use solutionModel.getGlobalVariables("scopeName") instead;
	 * an exact replacement, if you need it would be 'solutionModel.getGlobalVariables("scopeName").map(function (jsVariable) { return jsVariable.name } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allvariables()
	{
		return null;
	}

	/**
	 * Get all global relation names of the current solution.
	 * 
	 * @sample
	 * var allRelationNames = scopes.globals.allrelations;
	 * application.output("There are " + allRelationNames.length + " global relations.");
	 * for (var i=0; i<allRelationNames.length; i++)
	 * 	application.output(allRelationNames[i]);
	 * 
	 * @special
	 * @deprecated use solutionModel.getRelations(null) instead;
	 * an exact replacement, if you need it would be 'solutionModel.getRelations(null).map(function (jsRelation) { return jsRelation.name } )'.
	 */
	@Deprecated
	@JSReadonlyProperty
	public String[] allrelations()
	{
		return null;
	}

	/**
	 * Get the controller of the top level form in the currently active dialog.
	 * 
	 * @sample application.output("Current controller is: " + currentcontroller.getName());
	 * 
	 * @deprecated use forms.myform.controller instead, currentcontroller usage can be confusing when using multiple windows
	 */
	@Deprecated
	public JSForm js_getCurrentcontroller()
	{
		return null;
	}

	@Deprecated
	public void js_setCurrentcontroller(JSForm currentcontroller)
	{
	}
}
