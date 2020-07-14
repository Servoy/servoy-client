/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;
import com.servoy.j2db.util.ILogLevel;

/**
 * @since 2020.09
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSValidationObject implements IJavaScriptType
{
	private final IRecord record;
	private final List<Exception> genericExceptions = new ArrayList<>(3);
	private final List<JSProblem> problems = new ArrayList<>(3);
	private boolean invalid = false;
	private boolean onBeforeUpdateFailed;
	private boolean onBeforeInsertFailed;

	/**
	 * @param record
	 */
	public JSValidationObject(IRecord record)
	{
		this.record = record;
	}

	/**
	 * @param e
	 */
	public void addGenericException(Exception e)
	{
		invalid = true;
		genericExceptions.add(e);
	}

	public boolean isInvalid()
	{
		return invalid;
	}

	public void setOnBeforeUpdateFailed()
	{
		invalid = true;
		this.onBeforeUpdateFailed = true;
	}

	public void setOnBeforeInsertFailed()
	{
		invalid = true;
		this.onBeforeInsertFailed = true;
	}

	/**
	 * The record for which this JSValidationObject is for.
	 * @return the record
	 */
	@JSReadonlyProperty
	public IRecord getRecord()
	{
		return record;
	}

	/**
	 * Method to report a validation problem for this record.
	 *
	 * @param message The problem message
	 */
	@JSFunction
	public void report(String message)
	{
		report(message, null, ILogLevel.ERROR, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param column The column for which this message is generated for
	 */
	@JSFunction
	public void report(String message, String column)
	{
		report(message, column, ILogLevel.ERROR, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param column The column for which this message is generated for
	 * @param level The the log level for this problem
	 */
	@JSFunction
	public void report(String message, String column, int level)
	{
		report(message, column, level, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The problem message
	 * @param column The column for which this message is generated for
	 * @param level The the log level for this problem
	 * @param customObject Some user state that can be given to use later on.
	 */
	@JSFunction
	public void report(String message, String column, int level, Object customObject)
	{
		invalid = true;
		problems.add(new JSProblem(record, message, column, level, customObject));
	}

	/**
	 * @return the onBeforeInsertFailed
	 */
	@JSReadonlyProperty
	public boolean isOnBeforeInsertFailed()
	{
		return onBeforeInsertFailed;
	}

	/**
	 * @return the onBeforeUpdateFailed
	 */
	@JSReadonlyProperty
	public boolean isOnBeforeUpdateFailed()
	{
		return onBeforeUpdateFailed;
	}

	/**
	 * Returns a list of all the generic exceptions that did happen when the various methods where called.
	 *
	 * @return the genericExceptions
	 */
	@JSFunction
	public Object[] getGenericExceptions()
	{
		return genericExceptions.toArray();
	}

	/**
	 *  This returns all the problems found when validation the record.
	 *
	 * @return all the problems that where reported by a report() call.
	 */
	@JSFunction
	public JSProblem[] getProblems()
	{
		return problems.toArray(new JSProblem[problems.size()]);
	}


	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSValidationObject[problems=" + problems + ", onBeforeUpdateFailed=" + onBeforeUpdateFailed +
			", onBeforeInsertFailed=" + onBeforeInsertFailed + ", genericExceptions=" + genericExceptions + ", record=" + record + "]";
	}

}
