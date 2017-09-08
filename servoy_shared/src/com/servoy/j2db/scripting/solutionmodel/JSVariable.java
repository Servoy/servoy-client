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

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.solutionmodel.ISMVariable;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSVariable implements IConstantsObject, ISMVariable
{
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
				ScriptVariable tempVariable = (ScriptVariable)form.getSupportChild().getChild(variable.getUUID());
				if (tempVariable == null)
				{
					throw new RuntimeException("Cannot find variable '" + getName() + "' to modify. Modifying inherited variables is not allowed.");
				}
				variable = tempVariable;
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
	 * txtVar.defaultValue = '"some text"'; // Use two pairs of quotes if you want to assign a String as default value.
	 * application.output(scopes.globals.gText); // Prints 'some text' (without quotes).
	 * var mediaVar = solutionModel.newGlobalVariable('globals', 'gMedia', JSVariable.MEDIA);
	 * mediaVar.defaultValue = 'new Array(1, 2, 3, 4)';
	 * application.output(scopes.globals.gMedia); // Prints out the array with four elements.
	 */
	@JSGetter
	public String getDefaultValue()
	{
		return variable.getDefaultValue();
	}

	@JSSetter
	public void setDefaultValue(String arg)
	{
		checkModification();
		variable.setDefaultValue(arg);
		refreshVariableInScope();
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
	@JSGetter
	public String getName()
	{
		return variable.getName();
	}

	@JSSetter
	public void setName(String name)
	{
		checkModification();
		if (!name.equals(variable.getName()))
		{
			try
			{
				variable.updateName(new ScriptNameValidator(application.getFlattenedSolution()), name);
				refreshVariableInScope();
			}
			catch (RepositoryException e)
			{
				throw new RuntimeException("Error updating the name from " + variable.getName() + " to " + name, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.ISupportScope#getScopeName()
	 *
	 * @sample
	 * var globalVariables = solutionModel.getGlobalVariables();
	 * for (var i in globalVariables)
	 * 	application.output(globalVariables[i].name + ' is defined in scope ' + globalVariables[i].getScopeName());
	 */
	@JSFunction
	public String getScopeName()
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
	@JSGetter
	public int getVariableType()
	{
		return variable.getVariableType();
	}

	@JSSetter
	public void setVariableType(int arg)
	{
		checkModification();
		variable.setVariableType(arg);

		refreshVariableInScope();
	}

	/**
	 * Returns the UUID of the variable
	 *
	 * @sample
	 * var dateVar = solutionModel.newGlobalVariable('globals', 'gDate', JSVariable.DATETIME);
	 * application.output(dateVar.getUUID().toString());
	 */
	@JSFunction
	public UUID getUUID()
	{
		return variable.getUUID();
	}

	ScriptVariable getScriptVariable()
	{
		return variable;
	}


	private void refreshVariableInScope()
	{
		// do not load scope if not yet loaded
		if (form == null && application.getScriptEngine().getScopesScope().has(variable.getScopeName(), null))
		{
			application.getScriptEngine().getScopesScope().getGlobalScope(variable.getScopeName()).put(variable, true);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSVariable other = (JSVariable)obj;
		if (variable == null)
		{
			if (other.variable != null) return false;
		}
		else if (!variable.getUUID().equals(other.variable.getUUID())) return false;
		return true;
	}
}
