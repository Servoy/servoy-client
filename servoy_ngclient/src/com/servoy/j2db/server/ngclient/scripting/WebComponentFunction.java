/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.scripting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.WebObjectApiFunctionDefinition;

import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;

/**
 * Javascript function to call a client-side function in the web component api.
 *
 * @author rgansevles
 *
 */
public class WebComponentFunction extends WebBaseFunction
{
	private final WebFormComponent component;

	public WebComponentFunction(WebFormComponent component, WebObjectApiFunctionDefinition definition)
	{
		super(definition);
		this.component = component;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] arguments)
	{
		// first do rhino conversion on the types (form scope -> name, native object -> dimension, ...)
		Object[] args = convertArguments(arguments, component);
		return cx.getWrapFactory().wrap(cx, scope,
			NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.invokeApi(definition, args), definition.getReturnType(), component, thisObj),
			null);
	}
}
