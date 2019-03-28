/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.dataprocessing.RowManager;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;

/**
 * @author jcompagner
 * @since 2019.03
 */
public class DbIdentPropertyType extends DefaultPropertyType<DbIdentValue> implements IClassPropertyType<DbIdentValue>
{
	public static final String TYPE_NAME = "dbidentvalue"; //$NON-NLS-1$

	public static final DbIdentPropertyType INSTANCE = new DbIdentPropertyType();

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public DbIdentValue fromJSON(Object newJSONValue, DbIdentValue previousSabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext context, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		// cant be pushed from the client.
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, DbIdentValue sabloValue, PropertyDescription propertyDescription, DataConversion clientConversion,
		IBrowserConverterContext context) throws JSONException
	{
		// sabloValue should always be != null as this type is a DefaultPropertyType<DbIdentValue> - so the value is an instance of DbIdentValue
		return FullValueToJSONConverter.INSTANCE.toJSONValue(writer, key, RowManager.createPKHashKeyFromDBIdent(sabloValue), propertyDescription,
			clientConversion, context);
	}

	@Override
	public Class<DbIdentValue> getTypeClass()
	{
		return DbIdentValue.class;
	}

}
