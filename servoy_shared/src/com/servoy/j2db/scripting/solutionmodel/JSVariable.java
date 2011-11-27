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
package com.servoy.j2db.scripting.solutionmodel;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSVariable implements IConstantsObject
{
	/**
	 * Constant to be used when the type of a variable needs to be specified.
	 * 
	 * @sample
	 * var dateVar = solutionModel.newGlobalVariable('globals', 'gDate', JSVariable.DATETIME);
	 * dateVar.defaultValue = 'now';
	 * application.output(scopes.globals.gDate); // Prints the current date and time.
	 */
	public static final int DATETIME = IColumnTypes.DATETIME;

	/**
	 * @clonedesc DATETIME
	 * 
	 * @sample
	 * var txtVar = solutionModel.newGlobalVariable('globals', 'gText', JSVariable.TEXT);
	 * txtVar.defaultValue = '"some text"'; // Use two pairs of quotes if you want to assing a String as default value.
	 * application.output(scopes.globals.gText); // Prints 'some text' (without quotes).
	 */
	public static final int TEXT = IColumnTypes.TEXT;

	/**
	 * @clonedesc DATETIME
	 * 
	 * @sample
	 * var numberVar = solutionModel.newGlobalVariable('globals', 'gNumber', JSVariable.NUMBER);
	 * numberVar.defaultValue = 192.334;
	 * application.output(scopes.globals.gNumber); // Prints 192.334
	 */
	public static final int NUMBER = IColumnTypes.NUMBER;

	/**
	 * @clonedesc DATETIME
	 * 
	 * @sample
	 * var intVar = solutionModel.newGlobalVariable('globals', 'gInt', JSVariable.INTEGER);
	 * intVar.defaultValue = 997;
	 * application.output(scopes.globals.gInt); // Prints 997
	 */
	public static final int INTEGER = IColumnTypes.INTEGER;

	/**
	 * @clonedesc DATETIME
	 * 
	 * @sample
	 * var mediaVar = solutionModel.newGlobalVariable('globals', 'gMedia', JSVariable.MEDIA);
	 * mediaVar.defaultValue = 'new Array(1, 2, 3, 4)';
	 * application.output(scopes.globals.gMedia); // Prints out the array with four elements.
	 */
	public static final int MEDIA = IColumnTypes.MEDIA;

	private ScriptVariable variable;
	private boolean isCopy;
	private final JSForm form;
	private final IApplication application;

	public JSVariable(IApplication application, ScriptVariable variable, boolean isNew)
	{
		this.application = application;
		this.form = null;
		this.variable = variable;
		this.isCopy = isNew;
	}

	/**
	 * @param variable
	 */
	public JSVariable(IApplication application, JSForm form, ScriptVariable variable, boolean isNew)
	{
		this.form = form;
		this.application = application;
		this.variable = variable;
		this.isCopy = isNew;
	}

	void checkModification()
	{
		if (form != null)
		{
			form.checkModification();
			// make copy if needed
			if (!isCopy)
			{
				// then get the replace the item with the item of the copied relation.
				variable = (ScriptVariable)form.getSupportChild().getChild(variable.getUUID());
				isCopy = true;
			}
		}
		else if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			variable = application.getFlattenedSolution().createPersistCopy(variable);
			isCopy = true;
		}
	}

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
	 * txtVar.defaultValue = '"some text"'; // Use two pairs of quotes if you want to assing a String as default value.
	 * application.output(scopes.globals.gText); // Prints 'some text' (without quotes).
	 * var mediaVar = solutionModel.newGlobalVariable('globals', 'gMedia', JSVariable.MEDIA);
	 * mediaVar.defaultValue = 'new Array(1, 2, 3, 4)';
	 * application.output(scopes.globals.gMedia); // Prints out the array with four elements.
	 */
	public String js_getDefaultValue()
	{
		return variable.getDefaultValue();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ScriptVariable#getName()
	 *
	 * @sample 
	 * var gVar = solutionModel.newGlobalVariable('globals', 'gtext', JSVariable.TEXT);
	 * gVar.name = 'anotherName';
	 * gVar.defaultValue = '"default text"';
	 * // The next two lines will print the same output.
	 * application.output(scopes.globals[gVar.name]);
	 * application.output(scopes.globals.anotherName);
	 */
	public String js_getName()
	{
		return variable.getName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ISupportScope#getScopeName()
	 * 
	 * @sample 
	 * var globalVariables = solutionModel.getGlobalVariables();
	 * for (var i in globalVariables)
	 * 	application.output(globalVariables[i].name + ' is defined in scope ' + globalVariables[i].getScopeName());
	 */
	public String js_getScopeName()
	{
		return variable.getScopeName();
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ScriptVariable#getVariableType()
	 *
	 * @sample 
	 * var g = solutionModel.newGlobalVariable('globals', 'gtext',JSVariable.TEXT);
	 * scopes.globals.gtext = 'some text';
	 * g.variableType = JSVariable.DATETIME;
	 * scopes.globals.gtext = 'another text'; // This will raise an error now, because the variable is not longer of type text.
	 */
	public int js_getVariableType()
	{
		return variable.getVariableType();
	}

	public void js_setDefaultValue(String arg)
	{
		checkModification();
		variable.setDefaultValue(arg);
		if (form == null)
		{
			application.getScriptEngine().getScopesScope().getOrCreateGlobalScope(variable.getScopeName()).put(variable, true);
		}
	}


	public void js_setName(String name)
	{
		checkModification();
		if (!name.equals(variable.getName()))
		{
			try
			{
				variable.updateName(new ScriptNameValidator(application.getFlattenedSolution()), name);
				if (form == null)
				{
					application.getScriptEngine().getScopesScope().getOrCreateGlobalScope(variable.getScopeName()).put(variable, false);
				}
			}
			catch (RepositoryException e)
			{
				throw new RuntimeException("Error updating the name from " + variable.getName() + " to " + name, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	public void js_setVariableType(int arg)
	{
		checkModification();
		variable.setVariableType(arg);

		if (form == null)
		{
			application.getScriptEngine().getScopesScope().getOrCreateGlobalScope(variable.getScopeName()).put(variable, true);
		}

	}

	/**
	 * @return
	 */
	ScriptVariable getScriptVariable()
	{
		return variable;
	}

	/**
	 * Returns the UUID of the variable
	 * 
	 * @sample
	 * var dateVar = solutionModel.newGlobalVariable('globals', 'gDate', JSVariable.DATETIME);
	 * application.output(dateVar.getUUID().toString());
	 */
	public UUID js_getUUID()
	{
		return variable.getUUID();
	}
}
