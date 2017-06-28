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
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class CustomArrayAndObjectPropertyRhinoTest
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
		assertEquals(0, cal.getIndexesChangedByRef().size());
		assertEquals(0, cal.getIndexesWithContentUpdates().size());
		assertEquals(0, cam.getKeysChangedByRef().size());
		assertEquals(0, cam.getKeysWithUpdates().size());

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
		assertEquals(1, cam.getKeysChangedByRef().size());
		assertEquals("text", cam.getKeysChangedByRef().iterator().next());
		assertEquals(1, cal.getIndexesWithContentUpdates().size());
		assertEquals(Integer.valueOf(0), cal.getIndexesWithContentUpdates().iterator().next());
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals("Just some text 2", ((Scriptable)rhinoVal.get(0, rhinoVal)).get("text", rhinoVal));
		cam.put("active", new ArrayList());
		assertTrue(!cal.mustSendAll());
		assertTrue(!cam.mustSendAll());
		assertEquals(2, cam.getKeysChangedByRef().size());
		assertTrue(cam.getKeysChangedByRef().contains("text"));
		assertTrue(cam.getKeysChangedByRef().contains("active"));
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
		assertEquals(0, cal.getIndexesChangedByRef().size());
		assertEquals(0, cal.getIndexesWithContentUpdates().size());
		assertEquals(0, cam.getKeysChangedByRef().size());
		assertEquals(0, cam.getKeysWithUpdates().size());

		// assign some native values
		NativeObject oneO = new NativeObject();
//		oneO.setPrototype(ScriptableObject.getObjectPrototype(topLevel));
		NativeArray activeA1 = new NativeArray(0);
//		activeA1.setPrototype(ScriptableObject.getArrayPrototype(topLevel));
		NativeObject activeA1Obj = new NativeObject();
//		NativeObject activeA2Obj = new NativeObject();
//		activeA1Obj.setPrototype(ScriptableObject.getObjectPrototype(topLevel));
		rhinoVal.put(0, rhinoVal, oneO);
		Scriptable oneOScriptable = (Scriptable)rhinoVal.get(0, rhinoVal); // same as 'cam' but in it's Rhino representation
		assertTrue(!cal.mustSendAll());
		assertEquals(1, cal.getIndexesChangedByRef().size());
		assertEquals(0, cal.getIndexesWithContentUpdates().size());
		cam = ((ChangeAwareMap< ? , ? >)cal.get(0));
		activeA1Obj.put("field", activeA1Obj, 11);
		activeA1.put(0, activeA1, activeA1Obj);
		oneOScriptable.put("active", oneOScriptable, activeA1);
		assertEquals(11, ((Map)((List)((Map)cal.get(0)).get("active")).get(0)).get("field"));
		((Map)((List)((Map)cal.get(0)).get("active")).get(0)).put("percent", 0.22);

		assertEquals(1, cam.getKeysChangedByRef().size());
		assertEquals(0, cam.getKeysWithUpdates().size());
		assertTrue(cam.getKeysChangedByRef().contains("active"));
		assertTrue(!cal.mustSendAll());
		assertEquals(1, cal.getIndexesChangedByRef().size()); // we havent cleared changes yet; so initial assignment still needs tosend full value
		assertEquals(0, cal.getIndexesWithContentUpdates().size());

		// now change the native values using initial ref to see if it changed in java; this is no longer supported after case SVY-11027
//		activeA1Obj.put("field", activeA1Obj, 98);
//		assertEquals(98, ((Map)((List)((Map)cal.get(0)).get("active")).get(0)).get("field"));
//		activeA1.put(1, activeA1, activeA2Obj);
//		activeA2Obj.put("field", activeA2Obj, 45);
//		assertEquals(45, ((Map)((List)((Map)cal.get(0)).get("active")).get(1)).get("field"));

		changes = component.getAndClearChanges();
		JSONAssert.assertEquals(
			"{\"svy_types\":{\"arrayT\":\"JSON_arr\"},\"arrayT\":{\"svy_types\":{\"0\":{\"v\":\"JSON_obj\"}},\"u\":[{\"v\":{\"v\":{\"active\":{\"v\":[{\"v\":{\"field\":11,\"percent\":0.22},\"vEr\":2}],\"svy_types\":{\"0\":\"JSON_obj\"},\"vEr\":2}},\"svy_types\":{\"active\":\"JSON_arr\"},\"vEr\":5},\"i\":0}],\"vEr\":3}}",
			JSONUtils.writeChangesWithConversions(changes.content, changes.contentType, allowingBrowserConverterContext), JSONCompareMode.NON_EXTENSIBLE);

		((Map)((List)((Map)cal.get(0)).get("active")).get(0)).put("percent", 0.33);

		assertEquals(0, cam.getKeysChangedByRef().size());
		assertEquals(1, cam.getKeysWithUpdates().size());

		changes = component.getAndClearChanges();
		JSONAssert.assertEquals(
				"{\"svy_types\":{\"arrayT\":\"JSON_arr\"},\"arrayT\":{\"svy_types\":{\"0\":{\"v\":\"JSON_obj\"}},\"u\":[{\"v\":{\"svy_types\":{\"0\":{\"v\":\"JSON_arr\"}},\"u\":[{\"v\":{\"svy_types\":{\"0\":{\"v\":\"JSON_obj\"}},\"u\":[{\"v\":{\"u\":[{\"v\":0.33,\"k\":\"percent\"}],\"vEr\":2},\"i\":0}],\"vEr\":2},\"k\":\"active\"}],\"vEr\":5},\"i\":0}],\"vEr\":3}}",
			JSONUtils.writeChangesWithConversions(changes.content, changes.contentType, allowingBrowserConverterContext), JSONCompareMode.NON_EXTENSIBLE);

		((List)((Map)cal.get(0)).get("active")).add(new HashMap<String, Object>());
		((Map)((List)((Map)cal.get(0)).get("active")).get(1)).put("percent", 0.99);
		component.getAndClearChanges();
		// now simulate another request cycle that makes some change to the property from javascript
		rhinoVal = (Scriptable)NGConversions.INSTANCE.convertSabloComponentToRhinoValue(component.getProperty("arrayT"), arrayTPD, component, topLevel);
		Scriptable v = ((Scriptable)rhinoVal.get(0, rhinoVal));
		v = (Scriptable)v.get("active", v);
		v = (Scriptable)v.get(1, v);
		assertEquals(0.99, v.get("percent", v));
		v.put("percent", v, 0.56);
		assertEquals(0.56, ((Map)((List)((Map)cal.get(0)).get("active")).get(1)).get("percent"));
		assertTrue(!cam.mustSendAll());
		assertTrue(!cal.mustSendAll());
		assertEquals(1, cal.getIndexesWithContentUpdates().size());
		assertEquals(0, cal.getIndexesChangedByRef().size());
		assertEquals(1, cam.getKeysWithUpdates().size());
		assertEquals("active", cam.getKeysWithUpdates().iterator().next());
	}
}
