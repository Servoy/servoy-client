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
package com.servoy.j2db.server.ngclient.property;

import org.json.JSONObject;
import org.sablo.specification.property.types.IntPropertyType;


/**
 * It is just an 'int' - but it can be marked in the .spec file with "for": "myFoundset" where "myFoundset" is of "foundset" property type.<br/>
 * Usually this is meant to work with paging components (for example extra table with paging set) and it represents the desired # of records to show per page.<br/>
 *
 * This way, the initial show of that component will receive directly only the correct 'preferred viewport size' number of records - which is the page
 * size given by this property; this way there is no need for an initial call from client to set the preferred viewport size of the foundset - as that
 * might come too late and initial show would send a large number of unneeded records initially, before the component is shown.
 *
 * @author acostescu
 */
public class FoundsetInitialPreferredViewportSizePropertyType extends IntPropertyType
{
	private static final String CONFIG_KEY_FOR = "for"; //$NON-NLS-1$

	@SuppressWarnings("hiding")
	public static final IntPropertyType INSTANCE = new FoundsetInitialPreferredViewportSizePropertyType();

	@SuppressWarnings("hiding")
	public static final String TYPE_NAME = "foundsetInitialPreferredViewportSize"; //$NON-NLS-1$

	private FoundsetInitialPreferredViewportSizePropertyType()
	{
		super();
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject config)
	{
		return config.optString(CONFIG_KEY_FOR);
	}

}
