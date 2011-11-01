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

import com.servoy.j2db.FormController.JSForm;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Helper class for easier documentation of our JavaScript API for globals.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Globals", scriptingName = "globals")
public class Globals
{
	/**
	 * Get all global script names of the current solution.
	 * 
	 * @sample
	 * var allMethodNames = scopes.globals.allmethods;
	 * application.output("There are " + allMethodNames.length + " global methods.");
	 * for (var i=0; i<allMethodNames.length; i++)
	 * 	application.output(allMethodNames[i]);
	 *  
	 * @special
	 */
	public Array js_getAllmethods()
	{
		return null;
	}

	public void js_setAllmethods(Array allmethods)
	{
	}

	/**
	 * Get all global variable names of the current solution.
	 * 
	 * @sample
	 * var allVarNames = globals.allvariables;
	 * application.output("There are " + allVarNames.length + " global variables.");
	 * for (var i=0; i<allVarNames.length; i++)
	 * 	application.output(allVarNames[i]);
	 * 
	 * @special
	 */
	public Array js_getAllvariables()
	{
		return null;
	}

	public void js_setAllvariables(Array allvariables)
	{
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
	 */
	public Array js_getAllrelations()
	{
		return null;
	}

	public void js_setAllrelations(Array allrelations)
	{
	}

	/**
	 * Get the controller of the top level form in the currently active dialog.
	 * 
	 * @sample application.output("Current controller is: " + currentcontroller.getName());
	 */
	public JSForm js_getCurrentcontroller()
	{
		return null;
	}

	public void js_setCurrentcontroller(JSForm currentcontroller)
	{
	}
}
