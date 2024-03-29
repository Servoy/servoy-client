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

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IClassPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.UUID;

/**
 * @author acostescu
 *
 */
public class NGUUIDPropertyType extends DefaultPropertyType<UUID> implements IClassPropertyType<UUID>, IFormElementToTemplateJSON<UUID, UUID>
{

	public static final NGUUIDPropertyType NG_INSTANCE = new NGUUIDPropertyType();
	public static final String TYPE_NAME = "uuid"; //$NON-NLS-1$

	protected NGUUIDPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public UUID fromJSON(Object newJSONValue, UUID previousSabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newJSONValue instanceof String) return UUID.fromString((String)newJSONValue);
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, UUID sabloValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		JSONUtils.addKeyIfPresent(writer, key);
		return writer.value(sabloValue.toString());
	}

	@Override
	public Class<UUID> getTypeClass()
	{
		return UUID.class;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, UUID formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;
		return toJSON(writer, key, formElementValue, pd, null);
	}

}
