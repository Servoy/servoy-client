/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.util.Debug;


/**
 * Spec file configuration options for foundset property types are kept in here.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class FoundsetPropertyTypeConfig
{

	public static final boolean DEFAULT_SEND_DEFAULT_FORMATS = false;
	public static final boolean DEFAULT_SEND_SELECTION_VIEWPORT_INITIALLY = false;
	public static final int DEFAULT_INITIALL_PREFERRED_VIEWPORT_SIZE = 50;

	public static final String SEND_DEFAULT_FORMATS = "provideColumnFormats";
	public static final String DATAPROVIDERS = "dataproviders";
	public static final String DYNAMIC_DATAPROVIDERS = "dynamicDataproviders";
	public static final String SEND_SELECTION_VIEWPORT_INITIALLY = "sendSelectionViewportInitially";
	public static final String INITIAL_PREFERRED_VIEWPORT_SIZE = "initialPreferredViewPortSize";

	public final boolean sendDefaultFormats;
	public final boolean hasDynamicDataproviders;
	public final String[] dataproviders;
	public final int initialPreferredViewPortSize;
	public final boolean sendSelectionViewportInitially;

	public FoundsetPropertyTypeConfig(boolean sendDefaultFormats, boolean hasDynamicDataproviders, String[] dataproviders,
		boolean sendSelectionViewportInitially, int initialPreferredViewPortSize)
	{
		this.sendDefaultFormats = sendDefaultFormats;
		this.hasDynamicDataproviders = hasDynamicDataproviders;
		this.dataproviders = dataproviders;
		this.sendSelectionViewportInitially = sendSelectionViewportInitially;
		this.initialPreferredViewPortSize = initialPreferredViewPortSize;
	}

	public FoundsetPropertyTypeConfig(JSONObject config)
	{
		this.sendDefaultFormats = (config == null ? false : config.has(FoundsetPropertyTypeConfig.SEND_DEFAULT_FORMATS)
			? config.optBoolean(FoundsetPropertyTypeConfig.SEND_DEFAULT_FORMATS, false) : DEFAULT_SEND_DEFAULT_FORMATS);

		this.hasDynamicDataproviders = (config != null && (config.has(DYNAMIC_DATAPROVIDERS) ? config.optBoolean(DYNAMIC_DATAPROVIDERS) : false));
		this.sendSelectionViewportInitially = (config != null && config.has(SEND_SELECTION_VIEWPORT_INITIALLY)
			? config.optBoolean(SEND_SELECTION_VIEWPORT_INITIALLY) : DEFAULT_SEND_SELECTION_VIEWPORT_INITIALLY);
		this.initialPreferredViewPortSize = (config != null && config.has(INITIAL_PREFERRED_VIEWPORT_SIZE) ? config.optInt(INITIAL_PREFERRED_VIEWPORT_SIZE)
			: DEFAULT_INITIALL_PREFERRED_VIEWPORT_SIZE);

		String[] dps = null;
		if (config != null)
		{
			JSONArray dataprovidersJSON = config.optJSONArray(DATAPROVIDERS);
			if (dataprovidersJSON != null)
			{
				try
				{
					dps = new String[dataprovidersJSON.length()];
					for (int i = 0; i < dataprovidersJSON.length(); i++)
					{
						dps[i] = dataprovidersJSON.get(i).toString();
					}
				}
				catch (JSONException ex)
				{
					Debug.error(ex);
				}
			}
		}

		this.dataproviders = dps;
	}

}
