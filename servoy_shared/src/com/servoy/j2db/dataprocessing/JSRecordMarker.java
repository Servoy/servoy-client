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
 * The <code>JSRecordMarker</code> scripting wrapper represents a specific validation failure report for a record during a save operation.
 * It allows detailed reporting of issues tied to particular columns or data in a single record.
 *
 * <h2>Functionality</h2>
 * <p>The <code>customObject</code> property stores a user-defined object associated with the record marker, enabling the inclusion of custom metadata.
 * The <code>dataprovider</code> specifies the column where the issue occurred, providing precise error localization.
 * Message handling includes the <code>message</code> property, which holds the problem description and can be an internationalization (i18n) key,
 * while the <code>i18NMessage</code> property resolves the i18n key when applicable.</p>
 *
 * <p>The <code>level</code> property indicates the logging level assigned to the record marker, classifying the severity of the issue.
 * The <code>record</code> property links the marker to the specific record it references, facilitating direct association with the affected data.</p>
 *
 * <p>This wrapper is closely related to <a href="https://docs.servoy.com/reference/servoycore/dev-api/database-manager/jsrecordmarkers">JSRecordMarkers</a>, which manages validation states and aggregates multiple <code>JSRecordMarker</code>
 * instances for broader reporting and validation workflows.</p>
 *
 * <h2>Reference</h2>
 * <p></p>
 *
 * @author jcompanger
 * @since 2020.09
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRecordMarker implements IJavaScriptType
{
	private final String message;
	private final String dataprovider;
	private final int level;
	private final Object customObject;
	private final IRecord record;
	private final I18NProvider application;
	private final Object[] messageKeyParams;

	/**
	 * @param application
	 * @param message
	 * @param dataprovider
	 * @param level
	 * @param customObject
	 * @param messageKeyParams
	 */
	public JSRecordMarker(IRecord record, I18NProvider application, String message, String dataprovider, int level, Object customObject,
		Object[] messageKeyParams)
	{
		this.record = record;
		this.application = application;
		this.message = message;
		this.dataprovider = dataprovider;
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
	public String getDataprovider()
	{
		return dataprovider;
	}

	/**
	 * The LOGGINGLEVEL the users did give the the JSRecordMarkers.report() method.
	 *
	 * @return the level
	 */
	@JSReadonlyProperty
	public int getLevel()
	{
		return level;
	}

	/**
	 * The custom object the users did give the the JSRecordMarkers.report() method.
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
		return "JSRecordMarker[message=" + message + ", dataprovider=" + dataprovider + ", level=" + level + ", customObject=" + customObject + ", record=" +
			record +
			"]";
	}

}
