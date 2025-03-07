/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db.scripting.info;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.IConstantsObject;

/**
 * This is a class that represents the events in the solution.
 * This can be the build in events (mostly form events) like EventType.onShow
 * But also custom events that are added via the Solution eventTypes property.
 *
 * @author lvostinar
 * @since 2025.03
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "EventType", scriptingName = "EventType")
public final class EventType implements IConstantsObject
{
	private static final Map<String, EventType> DEFAULT_EVENTS = new HashMap<String, EventType>();

	/**
	 * The form onHide event, fired when the form is hidden.
	 */
	public static final EventType onHide = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onLoad event, fired when the form is loaded/created.
	 */
	public static final EventType onLoad = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onRecordEdit event, fired when a record of a form is going into edit mode.
	 */
	public static final EventType onRecordEditStart = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onRecordStop event, fired when the a record of a form is going out of edit mode.
	 */
	public static final EventType onRecordEditStop = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onSelection event, fired when the form's foundset selection is changed.
	 */
	public static final EventType onRecordSelection = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onResze event, fired when the form is resized.
	 */
	public static final EventType onResize = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onShow event, fired when the form is shown.
	 */
	public static final EventType onShow = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onBeforeHide event, fired when the form is about the be hidden.
	 */
	public static final EventType onBeforeHide = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONBEFOREHIDEMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onBeforeSelection event, fired when the form's foundset is about to change its selection.
	 */
	public static final EventType onBeforeRecordSelection = new EventType(RepositoryHelper.getDisplayName(
		StaticContentSpecLoader.PROPERTY_ONBEFORERECORDSELECTIONMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onSort event, fired when the form's foundset is sorted
	 */
	public static final EventType onSort = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onUnLoad event, fired when the form is being unloaded/destroyed.
	 */
	public static final EventType onUnload = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onElementFocusGained event, fired when the form element gets focus.
	 */
	public static final EventType onElementFocusGained = new EventType(RepositoryHelper.getDisplayName(
		StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onElementFocusLost event, fired when the form element loses its focus.
	 */
	public static final EventType onElementFocusLost = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName(), Form.class), true);
	/**
	 * The form onDataChange event, fired when the form element data is changed.
	 */
	public static final EventType onElementDataChange = new EventType(
		RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID.getPropertyName(), Form.class), true);


	public static Map<String, EventType> getDefaultEvents()
	{
		return Collections.unmodifiableMap(DEFAULT_EVENTS);
	}

	private final String name;

	public EventType(String name)
	{
		this(name, false);
	}

	private EventType(String name, boolean defaultEvent)
	{
		this.name = name;
		if (defaultEvent) DEFAULT_EVENTS.put(name, this);
	}

	/**
	 * @return the name
	 */
	@JSFunction
	public String getName()
	{
		return name;
	}

	/**
	 * @return whether is default (Form level) or custom event type (added via Solution eventTypes property)
	 */
	@JSFunction
	public boolean isDefaultEvent()
	{
		return DEFAULT_EVENTS.containsKey(name);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (super.equals(obj)) return true;
		if (obj instanceof EventType)
		{
			return name.equals(((EventType)obj).name);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
