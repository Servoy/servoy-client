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

/**
 * Container object is a component that can contain other AWT components.
 * @author jblok
 */
public abstract class Container extends WebComponent
{
	protected final Map<String, WebComponent> components = new HashMap<>();

	public Container(String name)
	{
		super(name);
	}

	public void add(WebComponent component)
	{
		components.put(component.getName(), component);
	}

	public WebComponent getWebComponent(String name)
	{
		return components.get(name);
	}

	public Map<String, WebComponent> getWebComponents()
	{
		return components;
	}
}
