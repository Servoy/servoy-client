/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import org.json.JSONObject;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.dataprocessing.RelatedFoundSet;

/**
 * @author jcompagner
 */
public class RelationPropertyType implements IPropertyType<RelatedFoundSet>
{
	public static RelationPropertyType INSTANCE = new RelationPropertyType();

	private RelationPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return "relation";
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public RelatedFoundSet defaultValue()
	{
		return null;
	}

}
