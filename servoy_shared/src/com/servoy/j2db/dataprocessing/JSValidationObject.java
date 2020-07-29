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

import com.servoy.base.util.I18NProvider;
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
public class JSValidationObject implements IJavaScriptType, IValidationObject
{
	private final IRecord record;
	private final I18NProvider application;
	private final List<Exception> genericExceptions = new ArrayList<>(3);
	private final List<JSProblem> problems = new ArrayList<>(3);
	private boolean invalid = false;
	private boolean onBeforeUpdateFailed;
	private boolean onBeforeInsertFailed;

	/**
	 * @param record
	 */
	public JSValidationObject(IRecord record, I18NProvider application)
	{
		this.record = record;
		this.application = application;
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
	 * @return If this validation object has errors or only warnings which don't block the save.
	 */
	@JSReadonlyProperty
	public boolean isHasErrors()
	{
		return onBeforeInsertFailed || onBeforeUpdateFailed || genericExceptions.size() > 0 || problems.stream().anyMatch(problem -> problem.getLevel() >= 3);
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

	@Override
	@JSFunction
	public void report(String message)
	{
		report(message, null, ILogLevel.ERROR, null);
	}

	@Override
	@JSFunction
	public void report(String message, String dataprovider)
	{
		report(message, dataprovider, ILogLevel.ERROR, null);
	}

	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level)
	{
		report(message, dataprovider, level, null);
	}

	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level, Object customObject)
	{
		report(message, dataprovider, level, customObject, null);
	}

	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level, Object customObject, Object[] messageKeyParams)
	{
		invalid = true;
		problems.add(new JSProblem(record, application, message, dataprovider, level, customObject, messageKeyParams));
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
