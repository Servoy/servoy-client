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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.specification.property.ChangeAwareList;
import org.sablo.specification.property.ChangeAwareMap;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class CustomArrayPropertyRhinoTest
{

	@Before
	public void setUp() throws Exception
	{
		Types.getTypesInstance().registerTypes();

		InputStream is = getClass().getResourceAsStream("PropertyTests.manifest");
		byte[] bytes = new byte[is.available()];
		is.read(bytes);
		String manifest = new String(bytes);
		is.close();

		is = getClass().getResourceAsStream("mycomponent.spec");
		bytes = new byte[is.available()];
		is.read(bytes);
		String comp = new String(bytes);
		is.close();

		HashMap<String, String> components = new HashMap<>();
		components.put("mycomponent.spec", comp);
		WebComponentSpecProvider.init(new IPackageReader[] { new InMemPackageReader(manifest, components) });
	}

	@After
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	public void testCustomTypeJavaBasedRhinoChanges() throws JSONException
	{
		WebComponent component = new WebComponent("mycomponent", "testComponentName");
		BrowserConverterContext allowingBrowserConverterContext = new BrowserConverterContext(component, PushToServerEnum.allow);
		PropertyDescription objectTPD = component.getSpecification().getProperty("objectT");
		PropertyDescription arrayTPD = component.getSpecification().getProperty("arrayT");
		PropertyDescription activePD = objectTPD.getProperty("active");

		// just some initial checks and setting a java value
		assertNull(component.getProperty("arrayT"));
		List<Map<String, Object>> javaV = new ArrayList<>();
		HashMap<String, Object> hm = new HashMap<String, Object>();
		javaV.add(hm);
		hm.put("text", "Just some text");
		component.setProperty("arrayT", javaV);

		// set should turn it into a change-aware list
		Object wrapped = component.getProperty("arrayT");
		assertTrue(wrapped instanceof ChangeAwareList< ? , ? >);
		ChangeAwareList<Map<String, Object>, Map<String, Object>> cal = (ChangeAwareList)wrapped;
		assertEquals("Just some text", cal.get(0).get("text"));
		assertTrue(cal.get(0) instanceof ChangeAwareMap< ? , ? >);
		ChangeAwareMap cam = ((ChangeAwareMap< ? , ? >)cal.get(0));

		// TODO I guess this kind of reference changes should be treated in the BaseWebObject directly when we have separate methods for changesToJSON and fullToJSON
		// so for now the change aware things do not report as being changed...
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());

		// still the component has to see them as changed!
		TypedData<Map<String, Object>> changes = component.getAndClearChanges();
		assertTrue(changes.content.get("arrayT") != null);
		assertTrue(changes.contentType.getProperty("arrayT").getType() instanceof CustomJSONArrayType);
		Object arrayCh = changes.content.get("arrayT");
		assertTrue(arrayCh != null);
		assertTrue(changes.contentType.getProperty("arrayT").getType() instanceof CustomJSONArrayType);
		Object mapCh = ((ChangeAwareList)arrayCh).get(0);
		assertTrue(mapCh != null);
		assertTrue(((CustomJSONPropertyType< ? >)changes.contentType.getProperty(
			"arrayT").getType()).getCustomJSONTypeDefinition().getType() instanceof CustomJSONObjectType);

		JSONUtils.writeDataWithConversions(changes.content, changes.contentType, allowingBrowserConverterContext);
		// ok now that we called component.getChanges() no changes should be present any more
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals(0, component.getAndClearChanges().content.size());
		assertEquals(0, cal.getChangedIndexes().size());
		assertEquals(0, cam.getChangedKeys().size());

		// check changing java => change reflected in Rhino
		ScriptableObject topLevel = new ScriptableObject()
		{

			@Override
			public String getClassName()
			{
				return "test_top_level_scope";
			}
		};
		Scriptable rhinoVal = (Scriptable)NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty("arrayT"), arrayTPD, component,
			topLevel);
		assertEquals("Just some text", ((Scriptable)rhinoVal.get(0, rhinoVal)).get("text", rhinoVal));
		cam.put("text", "Just some text 2");
		assertEquals(1, cam.getChangedKeys().size());
		assertEquals("text", cam.getChangedKeys().iterator().next());
		assertEquals(1, cal.getChangedIndexes().size());
		assertEquals(Integer.valueOf(0), cal.getChangedIndexes().iterator().next());
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals("Just some text 2", ((Scriptable)rhinoVal.get(0, rhinoVal)).get("text", rhinoVal));
		cam.put("active", new ArrayList());
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals(2, cam.getChangedKeys().size());
		assertTrue(cam.getChangedKeys().contains("text"));
		assertTrue(cam.getChangedKeys().contains("active"));
		cam.remove("active");
		assertTrue(!cal.mustSendAll());
		assertTrue(cam.mustSendAll());
		cal.add(new HashMap<String, Object>());
		ChangeAwareMap cam1 = ((ChangeAwareMap< ? , ? >)cal.get(1));
		assertTrue(cal.mustSendAll());
		assertTrue(cam.mustSendAll());

		// ok clear changes
		changes = component.getAndClearChanges();
		JSONUtils.writeDataWithConversions(changes.content, changes.contentType, allowingBrowserConverterContext);
		assertEquals(1, changes.content.size());
		assertEquals(0, component.getAndClearChanges().content.size());
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals(0, cal.getChangedIndexes().size());
		assertEquals(0, cam.getChangedKeys().size());

		// assign some native values
		NativeObject oneO = new NativeObject();
//		oneO.setPrototype(ScriptableObject.getObjectPrototype(topLevel));
		NativeArray activeA1 = new NativeArray(0);
//		activeA1.setPrototype(ScriptableObject.getArrayPrototype(topLevel));
		NativeObject activeA1Obj = new NativeObject();
		NativeObject activeA2Obj = new NativeObject();
//		activeA1Obj.setPrototype(ScriptableObject.getObjectPrototype(topLevel));
		rhinoVal.put(0, rhinoVal, oneO);
		assertTrue(!cal.mustSendAll());
		assertTrue(cal.getChangedIndexes().size() == 1);
		cam = ((ChangeAwareMap< ? , ? >)cal.get(0));
		activeA1Obj.put("field", activeA1Obj, 11);
		activeA1.put(0, activeA1, activeA1Obj);
		oneO.put("active", oneO, activeA1);
		assertEquals(11, ((Map)((List)((Map)cal.get(0)).get("active")).get(0)).get("field"));
		assertTrue(cam.mustSendAll());
		assertTrue(!cal.mustSendAll());
		assertEquals(1, cal.getChangedIndexes().size());

		// now change the native values using initial ref to see if it changed in java
		activeA1Obj.put("field", activeA1Obj, 98);
		assertEquals(98, ((Map)((List)((Map)cal.get(0)).get("active")).get(0)).get("field"));
		activeA1.put(1, activeA1, activeA2Obj);
		activeA2Obj.put("field", activeA2Obj, 45);
		assertEquals(45, ((Map)((List)((Map)cal.get(0)).get("active")).get(1)).get("field"));

		changes = component.getAndClearChanges();
		assertEquals(
			new JSONObject(
				"{\"arrayT\":{\"vEr\":3,\"u\":[{\"i\":0,\"v\":{\"vEr\":5,\"v\":{\"active\":{\"vEr\":2,\"v\":[{\"vEr\":2,\"v\":{\"field\":98}},{\"vEr\":2,\"v\":{\"field\":45}}],\"svy_types\":{\"1\":\"JSON_obj\",\"0\":\"JSON_obj\"}}},\"svy_types\":{\"active\":\"JSON_arr\"}}}],\"svy_types\":{\"0\":{\"v\":\"JSON_obj\"}}},\"svy_types\":{\"arrayT\":\"JSON_arr\"}}").toString(),
			new JSONObject(JSONUtils.writeChangesWithConversions(changes.content, changes.contentType, allowingBrowserConverterContext)).toString());

		// now simulate another request cycle that makes some change to the property from javascript
		rhinoVal = (Scriptable)NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty("arrayT"), arrayTPD, component, topLevel);
		Scriptable v = ((Scriptable)rhinoVal.get(0, rhinoVal));
		v = (Scriptable)v.get("active", v);
		v = (Scriptable)v.get(1, v);
		assertEquals(45, v.get("field", v));
		v.put("field", v, 56);
		assertEquals(56, ((Map)((List)((Map)cal.get(0)).get("active")).get(1)).get("field"));
		assertTrue(!cam.mustSendAll());
		assertTrue(!cal.mustSendAll());
		assertEquals(1, cal.getChangedIndexes().size());
		assertEquals(1, cam.getChangedKeys().size());
	}
}
