/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.util.ValueReference;

import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 * @since 2023.3.8
 *
 */
@SuppressWarnings("hiding")
public class ModifiablePropertyType extends ServoyStringPropertyType implements IPropertyCanDependsOn
{
	public static final String TYPE_NAME = "modifiable"; //$NON-NLS-1$

	public static final ModifiablePropertyType INSTANCE = new ModifiablePropertyType();

	private String[] dependencies;

	@Override
	public String fromJSON(Object newJSONValue, String previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return previousSabloValue; // you can't change the value from the client
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@SuppressWarnings("nls")
	@Override
	public Object parseConfig(JSONObject config)
	{
		String dataprovider = "";
		try
		{
			dataprovider = config.optString("for");
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		dependencies = getDependencies(config, dependencies);
		return dataprovider;
	}

	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}
}
