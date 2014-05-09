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

package org.sablo;

import java.util.HashMap;
import java.util.Map;

import org.sablo.specification.WebComponentApiDefinition;

/**
 * Server side representation of an angular webcomponent in the browser.
 * It is defined by a strong specification api,event and property-model wise
 * @author jblok
 */
public abstract class WebComponent
{
	private final String name;

	/**
	 * model properties to interact with webcomponent values, maps name to value
	 */
	protected final Map<String, Object> properties = new HashMap<>();

	public WebComponent(String name)
	{
		this.name = name;
		properties.put("name", name);
	}

	/**
	 * Returns the component name
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Execute incoming event
	 * @param eventType
	 * @param args
	 * @return
	 */
	public abstract Object executeEvent(String eventType, Object[] args);

	public abstract Object executeApiInvoke(WebComponentApiDefinition apiDefinition, Object[] args);
}
