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

package com.servoy.j2db.server.ngclient.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.IntPropertyType;

import com.servoy.j2db.server.ngclient.component.RhinoMapOrArrayWrapper;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;

/**
 * @author acostescu
 */
public class RhinoMapWrapperTest
{

	RhinoMapOrArrayWrapper w;
	HashMap<String, Object> wrappedMapValue;

	@Before
	public void setUp()
	{
		Context cx = Context.enter();

		TopLevel toplevelScope = new ImporterTopLevel(cx);
		PropertyDescription pd = new PropertyDescription("myCustomObjectProp", new NGCustomJSONObjectType("myCustomObjectType", null));
		PropertyDescription copd = new PropertyDescription("myCustomObjectType", pd.getType());
		((NGCustomJSONObjectType)pd.getType()).setCustomJSONDefinition(copd);

		copd.putProperty("someInt", new PropertyDescription("someInt", IntPropertyType.INSTANCE));

		wrappedMapValue = new HashMap<String, Object>();
		wrappedMapValue.put("someInt", 111);
		w = new RhinoMapOrArrayWrapper(wrappedMapValue, null, pd, toplevelScope);

		Context.exit();
	}

	@Test
	public void seeThatOnlySpecDefinedPropertiesEndUpInMapAlthoughOthersWorkInScriptingOnlyAnyway()
	{
		Context cx = Context.enter();

		w.put("notDefinedInSpec", w, 222);
		assertNull("Property that is not in spec should not end up in wrapped map", wrappedMapValue.get("notDefinedInSpec"));
		assertEquals("Property that is not in spec should still be useable in Rhino scripting", ScriptableObject.getProperty(w, "notDefinedInSpec"), 222);

		Context.exit();
	}

}
