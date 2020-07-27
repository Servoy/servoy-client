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

import com.servoy.base.util.I18NProvider;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.annotations.JSReadonlyProperty;

/**
 * @author jcompanger
 * @since 2020.09
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSProblem implements IJavaScriptType
{
	private final String message;
	private final String column;
	private final int level;
	private final Object customObject;
	private final IRecord record;
	private final I18NProvider application;
	private final Object[] messageKeyParams;

	/**
	 * @param application
	 * @param message
	 * @param column
	 * @param level
	 * @param customObject
	 * @param messageKeyParams
	 */
	public JSProblem(IRecord record, I18NProvider application, String message, String column, int level, Object customObject, Object[] messageKeyParams)
	{
		this.record = record;
		this.application = application;
		this.message = message;
		this.column = column;
		this.level = level;
		this.customObject = customObject;
		this.messageKeyParams = messageKeyParams;
	}

	/**
	 * The record for which this problem is generated.
	 *
	 * @return the record
	 */
	@JSReadonlyProperty
	public IRecord getRecord()
	{
		return record;
	}

	/**
	 * The message of this problem, can be a i18n key, see getI18NMessage() for a resolved one.
	 *
	 * @return the message
	 */
	@JSReadonlyProperty
	public String getMessage()
	{
		return message;
	}

	/**
	 * The the resolved i19n message if the message was an i18n key.
	 *
	 * @return the resolved message
	 */
	@JSReadonlyProperty
	public String getI18NMessage()
	{
		if (message.startsWith("i18n:")) //$NON-NLS-1$
		{
			return application.getI18NMessage(message, messageKeyParams);
		}
		return message;
	}

	/**
	 * The column of this record where this problem is reported for.
	 *
	 * @return the column
	 */
	@JSReadonlyProperty
	public String getColumn()
	{
		return column;
	}

	/**
	 * The LOGGINGLEVEL the users did give the the JSValidationObject.report() method.
	 *
	 * @return the level
	 */
	@JSReadonlyProperty
	public int getLevel()
	{
		return level;
	}

	/**
	 * The custom object the users did give the the JSValidationObject.report() method.
	 *
	 * @return the customObject
	 */
	@JSReadonlyProperty
	public Object getCustomObject()
	{
		return customObject;
	}


	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSProblem[message=" + message + ", column=" + column + ", level=" + level + ", customObject=" + customObject + ", record=" + record + "]";
	}

}
