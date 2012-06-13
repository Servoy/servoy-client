/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.j2db.solutionmodel;


/**
 * Solution model scripting method.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMMethod extends ISMHasUUID
{

	/**
	 * @clonedesc com.servoy.j2db.persistence.AbstractScriptProvider#getDeclaration()
	 * 
	 * @sample
	 * var method = form.newMethod('function original() { application.output("Original function."); }');
	 * application.output('original method name: ' + method.getName());
	 * application.output('original method code: ' + method.code);
	 * method.code = 'function changed() { application.output("This is another function."); }';
	 * method.showInMenu = false;
	 * var button = form.newButton('Click me!', 10, 10, 100, 30, method);
	 */
	public String getCode();

	/**
	 * @clonedesc com.servoy.j2db.persistence.AbstractScriptProvider#getName()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSMethod#getCode()
	 * 
	 * @return A String holding the name of this method.
	 */
	public String getName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ISupportScope#getScopeName()
	 * 
	 * @sample 
	 * var methods = solutionModel.getGlobalMethods(); 
	 * for (var x in methods) 
	 * 	application.output(methods[x].getName() + ' is defined in scope ' + methods[x].getScopeName());
	 */
	public String getScopeName();

	/**
	 * @clonedesc com.servoy.j2db.persistence.ScriptMethod#getShowInMenu()
	 * 
	 * @sampleas com.servoy.j2db.scripting.solutionmodel.JSMethod#getCode()
	 */
	public boolean getShowInMenu();

	public void setCode(String content);

	/**
	 * Gets the argument array for this method if that is set for the specific action this method is taken from.
	 * Will return null by default. This is only for reading, you can't alter the arguments through this array, 
	 * for that you need to create a new object through solutionModel.wrapMethodWithArguments(..) and assign it again.
	 * 
	 * @sample 
	 * var frm = solutionModel.getForm("myForm");
	 * var button = frm.getButton("button");
	 * // get the arguments from the button.
	 * // NOTE: string arguments will be returned with quotes (comp.onAction.getArguments()[0] == '\'foo\' evals to true)
	 * var arguments = button.onAction.getArguments();
	 * if (arguments && arguments.length > 1 && arguments[1] == 10) { 
	 * 	// change the value and assign it back to the onAction.
	 * 	arguments[1] = 50;
	 * 	button.onAction = solutionModel.wrapMethodWithArguments(button.onAction,arguments);
	 * }
	 * 
	 * @return Array of the arguments, null if not specified.
	 */
	public Object[] getArguments();

	public void setShowInMenu(boolean arg);
}