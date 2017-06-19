/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.IFindModeAwarePropertyValue;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class FindModeSabloValue implements IFindModeAwarePropertyValue
{

	private final FindModeConfig config;
	private Boolean findMode = Boolean.FALSE;

	private final HashMap<String, Boolean> saveOldConfigValues = new HashMap<String, Boolean>();

	private IChangeListener changeMonitor;
	private final DataAdapterList dataAdapterList;
	private IWebObjectContext webObjectContext;


	public FindModeSabloValue(FindModeConfig config, DataAdapterList dataAdapterList)
	{
		this.config = config;
		this.dataAdapterList = dataAdapterList;
	}

	public JSONWriter toJSON(JSONWriter writer)
	{
		try
		{
			writer.value(findMode);
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return writer;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		this.changeMonitor = changeMonitor;
		this.webObjectContext = webObjectContext;
		if (dataAdapterList != null) dataAdapterList.addFindModeAwareProperty(this);
	}

	@Override
	public void detach()
	{
		if (dataAdapterList != null) dataAdapterList.removeFindModeAwareProperty(this);
	}

	@Override
	public void findModeChanged(boolean newFindMode)
	{
		if (findMode.booleanValue() != newFindMode)
		{
			findMode = Boolean.valueOf(newFindMode);

			if (!Boolean.TRUE.equals(dataAdapterList.getApplication().getClientProperty(IApplication.LEAVE_FIELDS_READONLY_IN_FIND_MODE)))
			{
				Object readOnlyValue = webObjectContext.getProperty(WebFormUI.READONLY);
				try
				{
					// if this is a ReadonlySabloValue then let it know the findmode is changing so it can ignore this
					if (readOnlyValue instanceof ReadonlySabloValue) ((ReadonlySabloValue)readOnlyValue).setFindModeChange(true);
					if (findMode.booleanValue())
					{
						//when entering find mode we save the previous values first so that we can put them back when we exit findmode
						Set<String> configPropertiesNames = config.configPropertiesNames();
						for (String propertyName : configPropertiesNames)
						{
							if (webObjectContext.getProperty(propertyName) instanceof Boolean)
							{
								saveOldConfigValues.put(propertyName, (Boolean)webObjectContext.getProperty(propertyName));
							}
							else Debug.log("Warning! findmode config property value \"" + propertyName + //$NON-NLS-1$
								"\" is NOT a Boolean in the actual component model. This property will not be affected by the findmode toggle."); //$NON-NLS-1$
						}

						//then we set in the component the configured values
						for (String propertyName : configPropertiesNames)
						{
							Object configuredPropertyValue = config.getConfiguredPropertyValueOf(propertyName);
							if (configuredPropertyValue instanceof Boolean)
							{
								webObjectContext.setProperty(propertyName, configuredPropertyValue);
							}
							else Debug.log("Warning! findmode config property value \"" + propertyName + //$NON-NLS-1$
								"\" is NOT a Boolean. This property will not be affected by the findmode toggle."); //$NON-NLS-1$
						}
					}
					else
					{
						//when we exit findmode, we put back the old values
						for (String propertyName : saveOldConfigValues.keySet())
						{
							webObjectContext.setProperty(propertyName, saveOldConfigValues.get(propertyName));
						}
						saveOldConfigValues.clear();
					}
				}
				finally
				{
					if (readOnlyValue instanceof ReadonlySabloValue) ((ReadonlySabloValue)readOnlyValue).setFindModeChange(false);
				}
			}

			changeMonitor.valueChanged();
		}
	}
}
