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


import com.servoy.j2db.scripting.api.solutionmodel.IBaseSMVariable;


/**
 * Solution model scriptig variable.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */

public interface ISMVariable extends IBaseSMVariable, ISMHasUUID
{

	/**
	 * @clonedesc com.servoy.j2db.persistence.ScriptVariable#getDefaultValue()
	 *
	 * @sample 
	 * var intVar = solutionModel.newGlobalVariable('globals', 'gInt', JSVariable.INTEGER);
	 * intVar.defaultValue = 997;
	 * application.output(scopes.globals.gInt); // Prints 997
	 * var numberVar = solutionModel.newGlobalVariable('globals', 'gNumber', JSVariable.NUMBER);
	 * numberVar.defaultValue = 192.334;
	 * application.output(scopes.globals.gNumber); // Prints 192.334
	 * var dateVar = solutionModel.newGlobalVariable('globals', 'gDate', JSVariable.DATETIME);
	 * dateVar.defaultValue = 'now';
	 * application.output(scopes.globals.gDate); // Prints the current date and time.
	 * var txtVar = solutionModel.newGlobalVariable('globals', 'gText', JSVariable.TEXT);
	 * txtVar.defaultValue = '"some text"'; // Use two pairs of quotes if you want to assign a String as default value.
	 * application.output(scopes.globals.gText); // Prints 'some text' (without quotes).
	 * var mediaVar = solutionModel.newGlobalVariable('globals', 'gMedia', JSVariable.MEDIA);
	 * mediaVar.defaultValue = 'new Array(1, 2, 3, 4)';
	 * application.output(scopes.globals.gMedia); // Prints out the array with four elements.
	 */
	public String getDefaultValue();

}