/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.IWebObjectContext;
import org.sablo.InMemPackageReader;
import org.sablo.WebComponent;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.IWebFormController;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author Diana
 *
 */
public class RhinoConversionTest
{
	private IServoyDataConverterContext iServoyDataConverterContext;
	private IWebObjectContext iWebObjectContext;

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
		WebComponentSpecProvider.init(new IPackageReader[] { new InMemPackageReader(manifest, components) }, null);

		iServoyDataConverterContext = new IServoyDataConverterContext()
		{

			@Override
			public FlattenedSolution getSolution()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IWebFormController getForm()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public INGApplication getApplication()
			{
				// TODO Auto-generated method stub
				return null;
			}
		};

		iWebObjectContext = new IWebObjectContext()
		{

			@Override
			public PropertyDescription getPropertyDescription(String name)
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean setProperty(String propertyName, Object value)
			{
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public BaseWebObject getUnderlyingWebObject()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getRawPropertyValue(String name)
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object getProperty(String name)
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public IWebObjectContext getParentContext()
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
			{
				// TODO Auto-generated method stub

			}
		};
	}

	@After
	public void tearDown()
	{
		WebComponentSpecProvider.disposeInstance();
	}

	@Test
	public void testNativeDateFromRhino()
	{

		Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription objectTPD = component.getSpecification().getProperty("objectT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Object[] objArray = { "2011", "0", "1", "2", "3", "4", "567" };
			final Object convertedDate = RhinoConversion.defaultFromRhino(NativeDate.jsConstructor(objArray), null, objectTPD, iServoyDataConverterContext);
			assertTrue(convertedDate instanceof Date);
			assertEquals("1 Jan 2011 00:03:04 GMT", ((Date)convertedDate).toGMTString());
			assertEquals("1293840184567", String.valueOf(((Date)convertedDate).getTime()));
		}
		finally
		{
			Context.exit();
		}

	}

	@Test
	public void testNativeObjectFromRhino()
	{

		final Context context = Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription objectTPD = component.getSpecification().getProperty("objectT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Scriptable scope = context.initStandardObjects();

			NativeObject result = (NativeObject)context.evaluateString(
				scope,
				"({age:3, name:'bob'})",
				"<inline>", 1, null);

			result.put("surname", result, "Smith");

			final Object convertedMap = RhinoConversion.defaultFromRhino(result, null, objectTPD, iServoyDataConverterContext);
			assertTrue(convertedMap instanceof Map);
			assertEquals("{surname=Smith, name=bob, age=3}", convertedMap.toString());

		}
		finally
		{
			Context.exit();
		}
	}

	@Test
	public void testNativeArrayFromRhino()
	{

		final Context context = Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription arrayTPD = component.getSpecification().getProperty("arrayT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Scriptable scope = context.initStandardObjects();

			Object[] array = new Object[] { "Hello World", 4, 3.5, true, "Dis is a longe string", 35.983564, false };
			final NativeArray nativeArray = new NativeArray(array);

			final Object convertedList = RhinoConversion.defaultFromRhino(nativeArray, null, arrayTPD, iServoyDataConverterContext);
			assertTrue(convertedList instanceof List);
			assertEquals("[Hello World, 4, 3.5, true, Dis is a longe string, 35.983564, false]", convertedList.toString());

		}
		finally
		{
			Context.exit();
		}

	}

	@Test
	public void testListToNativeArrayToRhino()
	{

		final Context context = Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription arrayTPD = component.getSpecification().getProperty("arrayT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Scriptable scope = context.initStandardObjects();

			List<String> messages = Arrays.asList("Hello", "World!", "How", "Are", "You");

			final Object convertedNativeArray = RhinoConversion.defaultToRhino(messages, arrayTPD, iWebObjectContext, scope);
			assertTrue(convertedNativeArray instanceof NativeArray);
			final NativeArray no = (NativeArray)convertedNativeArray;
			assertEquals("Hello", no.get(0));
			assertEquals("World!", no.get(1));
			assertEquals("How", no.get(2));
			assertEquals("Are", no.get(3));
			assertEquals("You", no.get(4));
		}
		finally
		{
			Context.exit();
		}

	}

	@Test
	public void testMapToNativeObjectToRhino()
	{

		final Context context = Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription objectTPD = component.getSpecification().getProperty("objectT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Scriptable scope = context.initStandardObjects();

			Object[] array = new Object[] { "Hello World", 4, 3.5, true, "Dis is a longe string", 35.983564, false };
			Map<Object, Object> map = new HashMap<Object, Object>();
			int arrayLength = array.length;
			for (int index = 0; index < arrayLength; index++)
			{
				map.put(index, array[index]);
			}

			final Object convertedNativeArray = RhinoConversion.defaultToRhino(map, objectTPD, iWebObjectContext, scope);
			assertTrue(convertedNativeArray instanceof NativeObject);
			assertEquals("[object Object]", ((NativeObject)convertedNativeArray).toString());
			assertEquals("[0=Hello World, 1=4, 2=3.5, 3=true, 4=Dis is a longe string, 5=35.983564, 6=false]",
				((NativeObject)convertedNativeArray).entrySet().toString());
		}
		finally
		{
			Context.exit();
		}
	}

	@Test
	public void testJsonObjectToNativeObjectToRhino()
	{

		final Context context = Context.enter();
		try
		{
			WebComponent component = new WebComponent("mycomponent", "testComponentName");
			PropertyDescription objectTPD = component.getSpecification().getProperty("objectT");

			// just some initial checks and setting a java value
			assertNull(component.getProperty("arrayT"));
			List<Map<String, Object>> javaV = new ArrayList<>();
			HashMap<String, Object> hm = new HashMap<String, Object>();
			javaV.add(hm);
			hm.put("text", "Just some text");
			component.setProperty("arrayT", javaV);

			Scriptable scope = context.initStandardObjects();

			JSONObject item = new JSONObject();
			item.put("information", "test");
			item.put("id", 3);
			item.put("name", "course1");

			final Object convertedNativeArray = RhinoConversion.defaultToRhino(item, objectTPD, iWebObjectContext, scope);
			assertTrue(convertedNativeArray instanceof NativeObject);
			assertEquals("[object Object]", ((NativeObject)convertedNativeArray).toString());
			assertEquals("[name=course1, information=test, id=3]", ((NativeObject)convertedNativeArray).entrySet().toString());
		}
		finally
		{
			Context.exit();
		}
	}
}
