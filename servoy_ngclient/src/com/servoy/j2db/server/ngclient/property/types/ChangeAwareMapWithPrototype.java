/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.sablo.CustomObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.ChangeAwareMap;

/**
 * A change-aware map that is based on a value that came from Rhino - in which case it does keep the prototype of the set-from-Rhino-value
 * as that prototype might contain methods that should still work with the 'intrumented' object that we return back to Rhino after copying the values from the initial object.<br/><br/>
 *
 * For example window_server.js and it's popups use this kind of approach.
 *
 * @author acostescu
 */
public class ChangeAwareMapWithPrototype<SabloT, SabloWT> extends ChangeAwareMap<SabloT, SabloWT> implements IRhinoPrototypeProvider
{

	protected final Scriptable prototype;

	public ChangeAwareMapWithPrototype(Map<String, SabloT> wrappedMap, Scriptable prototype, int initialVersion,
		CustomObjectContext<SabloT, SabloWT> customObjectContext, PropertyDescription customObjectPD)
	{
		super(wrappedMap, initialVersion, customObjectContext, customObjectPD);
		this.prototype = prototype;
	}

	@Override
	public Scriptable getRhinoPrototype()
	{
		return prototype;
	}

}
