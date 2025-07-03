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
import org.sablo.specification.property.IPropertyCanDependsOn;
import org.sablo.specification.property.types.DefaultPropertyType;

/**
 * @author jcompagner
 */
public class MediaOptionsPropertyType extends DefaultPropertyType<Integer> implements IPropertyCanDependsOn
{

	public static final MediaOptionsPropertyType INSTANCE = new MediaOptionsPropertyType();
	public static final String TYPE_NAME = "mediaoptions";

	private String[] dependencies;

	private MediaOptionsPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		dependencies = getDependencies(json, dependencies);
		return json;
	}

	@Override
	public String[] getDependencies()
	{
		return dependencies;
	}
}
