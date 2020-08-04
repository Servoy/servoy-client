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
import java.util.stream.Collectors;

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
public class JSRecordMarkers implements IJavaScriptType, IRecordMarkers
{
	private final IRecord record;
	private final I18NProvider application;
	private final List<Exception> genericExceptions = new ArrayList<>(3);
	private final List<JSRecordMarker> markers = new ArrayList<>(3);
	private boolean invalid = false;
	private boolean onBeforeUpdateFailed;
	private boolean onBeforeInsertFailed;
	private final Object state;

	/**
	 * @param record
	 * @param application
	 */
	public JSRecordMarkers(IRecord record, I18NProvider application)
	{
		this(record, application, null);
	}

	/**
	 * @param record
	 * @param application
	 * @param state
	 */
	public JSRecordMarkers(IRecord record, I18NProvider application, Object state)
	{
		this.record = record;
		this.application = application;
		this.state = state;
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
		return onBeforeInsertFailed || onBeforeUpdateFailed || genericExceptions.size() > 0 || markers.stream().anyMatch(problem -> problem.getLevel() >= 3);
	}

	/**
	 * The record for which this JSRecordMarkers is for.
	 * @return the record
	 */
	@JSReadonlyProperty
	public IRecord getRecord()
	{
		return record;
	}

	/**
	 * Create a new JSMarker by reporting a message, this message can be an i18n key (should then start with 'i18n')
	 * Optionally you can give a dataprovider for which this marker is reported, a LOGGINGLEVEL for this marker, some custom javascript object for later use
	 * and a array of message keys if the message was an i18n key with variables.
	 *
	 * @param message The message (can be i18n)
	 */
	@Override
	@JSFunction
	public void report(String message)
	{
		report(message, null, ILogLevel.ERROR, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The message (can be i18n)
	 * @param dataprovider The dataprovider for which this marker is for.
	 *
	 */
	@Override
	@JSFunction
	public void report(String message, String dataprovider)
	{
		report(message, dataprovider, ILogLevel.ERROR, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The message (can be i18n)
	 * @param dataprovider The dataprovider for which this marker is for.
	 * @param level The LOGGINGLEVEL like ERROR or WARNING
	 *
	 */
	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level)
	{
		report(message, dataprovider, level, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The message (can be i18n)
	 * @param dataprovider The dataprovider for which this marker is for.
	 * @param level The LOGGINGLEVEL like ERROR or WARNING
	 * @param customObject A custom object is default the customObject of the validate() call.
	 *
	 */
	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level, Object customObject)
	{
		report(message, dataprovider, level, customObject, null);
	}

	/**
	 * @clonedesc report(String)
	 *
	 * @param message The message (can be i18n)
	 * @param dataprovider The dataprovider for which this marker is for.
	 * @param level The LOGGINGLEVEL like ERROR or WARNING
	 * @param customObject A custom object is default the customObject of the validate() call.
	 * @param messageKeyParams Some variables if he message is an i18n key that has place holders.
	 *
	 */
	@Override
	@JSFunction
	public void report(String message, String dataprovider, int level, Object customObject, Object[] messageKeyParams)
	{
		invalid = true;
		markers.add(new JSRecordMarker(record, application, message, dataprovider, level, customObject == null ? state : customObject, messageKeyParams));
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
	public JSRecordMarker[] getMarkers()
	{
		return markers.toArray(new JSRecordMarker[markers.size()]);
	}

	/**
	 *  This returns the problems found when validation the record filtered by the given level
	 *
	 * @param level a level of a marker that should be returned.
	 *
	 * @return all the problems that where reported by a report() call.
	 */
	@JSFunction
	public JSRecordMarker[] getMarkers(int level)
	{
		return markers.stream().filter(marker -> marker.getLevel() == level).collect(Collectors.toList()).toArray(new JSRecordMarker[0]);
	}


	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSRecordMarkers[markers=" + markers + ", onBeforeUpdateFailed=" + onBeforeUpdateFailed +
			", onBeforeInsertFailed=" + onBeforeInsertFailed + ", genericExceptions=" + genericExceptions + ", record=" + record + "]";
	}

}
