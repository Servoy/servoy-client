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

import java.util.Arrays;

import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * @author lvostinar
 *
 */
@ServoyClientSupport(ng = true, mc = false, wc = false, sc = false)
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "EventType", scriptingName = "EventType")
public class EventType
{
	private final String name;
	public static final String[] DEFAULT_EVENTS = new String[] { StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID
		.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONLOADMETHODID
			.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID
				.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID
					.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID
						.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID
							.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID
								.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONBEFOREHIDEMETHODID
									.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONBEFORERECORDSELECTIONMETHODID
										.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID
											.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID
												.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID
													.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID
														.getPropertyName(), StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID.getPropertyName() };

	public EventType(String name)
	{
		this.name = name;
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
		return Arrays.stream(DEFAULT_EVENTS).anyMatch(name::equals);
	}

	public String getDisplayName()
	{
		if (Arrays.stream(DEFAULT_EVENTS).anyMatch(name::equals))
		{
			return RepositoryHelper.getDisplayName(name, Form.class);
		}
		return getName();
	}
}
