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

import com.servoy.base.solutionmodel.IBaseSMMethod;


/**
 * Solution model scripting method.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMMethod extends IBaseSMMethod, ISMHasUUID
{

	/**
	 * Flag that tells if the method appears or not in the "Methods" menu of Servoy Client.
	 * 
	 * @sample
	 * var method = form.newMethod('function original() { application.output("Original function."); }');
	 * application.output('original method name: ' + method.getName());
	 * application.output('original method code: ' + method.code);
	 * method.code = 'function changed() { application.output("This is another function."); }';
	 * method.showInMenu = false;
	 * var button = form.newButton('Click me!', 10, 10, 100, 30, method);
	 */
	public boolean getShowInMenu();

	public void setShowInMenu(boolean arg);

}