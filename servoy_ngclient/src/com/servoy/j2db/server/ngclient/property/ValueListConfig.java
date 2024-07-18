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

package com.servoy.j2db.server.ngclient.property;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class ValueListConfig
{
	private final String dataprovider;
	private final String configPropertyName;
	private final String defaultValue;
	private final int maxCount;
	private final boolean logMax;
	boolean lazyLoading = false;

	public ValueListConfig(String dataprovider, String def, int maxCount, boolean logMax, boolean lazyLoading, String configPropertyName)
	{
		super();
		this.dataprovider = dataprovider;
		this.defaultValue = def;
		this.maxCount = maxCount;
		this.logMax = logMax;
		this.lazyLoading = lazyLoading;
		this.configPropertyName = configPropertyName;
	}


	public String getFor()
	{
		return dataprovider;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public int getMaxCount(INGApplication application)
	{
		if (application.getClientProperty(IApplication.VALUELIST_MAX_ROWS) != null)
		{
			return Utils.getAsInteger(application.getClientProperty(IApplication.VALUELIST_MAX_ROWS));
		}
		return maxCount;
	}

	public boolean shouldLogWhenOverMax()
	{
		return logMax;
	}

	public boolean getLazyLoading()
	{
		return lazyLoading;
	}

	/**
	 * @return the configPropertyName
	 */
	public String getConfigPropertyName()
	{
		return configPropertyName;
	}
}
