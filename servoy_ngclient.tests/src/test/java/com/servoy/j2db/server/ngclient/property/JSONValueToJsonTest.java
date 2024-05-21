/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.NativeObject;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecification.PushToServerEnum;
import org.sablo.specification.WebObjectSpecificationBuilder;
import org.sablo.specification.property.BrowserConverterContext;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.FullValueToJSONConverter;

import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.ServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.component.RhinoConversion;
import com.servoy.j2db.server.ngclient.property.DataProviderDateTest.ServiceProvider;
import com.servoy.j2db.server.ngclient.property.types.MapPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGObjectPropertyType;
import com.servoy.j2db.server.ngclient.property.types.Types;

/**
 * @author lvostinar
 *
 */
public class JSONValueToJsonTest extends Log4JToConsoleTest
{
	public static final PropertyDescription MY_OBJECT_PD = new PropertyDescriptionBuilder().withName("myobject").withType(
		NGObjectPropertyType.NG_INSTANCE).build();

	private BrowserConverterContext context;

	@Before
	public void setup()
	{
		Types.getTypesInstance().registerTypes();
		this.context = new BrowserConverterContext(new TestBaseWebObject("mycomponent", new WebObjectSpecificationBuilder().build(), false),
			PushToServerEnum.reject);
	}

	public static class TestBaseWebObject extends BaseWebObject implements IContextProvider
	{

		/**
		 * @param name
		 * @param specification
		 * @param waitForPropertyInitBeforeAttach
		 */
		public TestBaseWebObject(String name, WebObjectSpecification specification, boolean waitForPropertyInitBeforeAttach)
		{
			super(name, specification, waitForPropertyInitBeforeAttach);
			// TODO Auto-generated constructor stub
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see com.servoy.j2db.server.ngclient.IContextProvider#getDataConverterContext()
		 */
		@Override
		public IServoyDataConverterContext getDataConverterContext()
		{
			return new ServoyDataConverterContext(new ServiceProvider());
		}

	}

	@Test
	public void testObjectToJson() throws JSONException
	{
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", 123);
		jsonObj.put("key3", true);

		Object rhinoVal = RhinoConversion.defaultToRhino(jsonObj, MY_OBJECT_PD, null, new NativeObject());

		Object sabloVal = RhinoConversion.defaultFromRhino(rhinoVal);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		JSONUtils.defaultToJSONValue(FullValueToJSONConverter.INSTANCE, jsonWriter, "myobject", sabloVal, MY_OBJECT_PD, null);
		jsonWriter.endObject();

		assertEquals("Simple object toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":123,\"key3\":true}}");

	}

	@Test
	public void testObjectWithNULLToJson() throws JSONException
	{
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", JSONObject.NULL);
		jsonObj.put("key3", new JSONArray(new Object[] { "bbb", "ccc", null }));

		Object rhinoVal = RhinoConversion.defaultToRhino(jsonObj, MY_OBJECT_PD, null, new NativeObject());

		Object sabloVal = RhinoConversion.defaultFromRhino(rhinoVal);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		JSONUtils.defaultToJSONValue(FullValueToJSONConverter.INSTANCE, jsonWriter, "myobject", sabloVal, MY_OBJECT_PD, null);
		jsonWriter.endObject();

		assertEquals("Simple object toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":null,\"key3\":[\"bbb\",\"ccc\",null]}}");

	}

	@Test
	public void testSimpleObjectTypeToJson() throws JSONException
	{
		Map mapObj = new HashMap();
		mapObj.put("key1", "aaa");
		mapObj.put("key2", 123);
		mapObj.put("key3", true);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, null);
		jsonWriter.endObject();

		assertEquals("Simple object type toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":123,\"key3\":true}}");

	}

	@Test
	public void testObjectTypeWithNullToJson() throws JSONException
	{
		Map mapObj = new HashMap();
		mapObj.put("key1", "aaa");
		mapObj.put("key2", null);
		mapObj.put("key3", new Object[] { "bbb", "ccc", null });

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, null);
		jsonWriter.endObject();

		assertEquals("Simple object type with null toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":null,\"key3\":[\"bbb\",\"ccc\",null]}}");

	}

	@Test
	public void testObjectTypeWithDatesToJson() throws JSONException
	{
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		TimeZone default1 = TimeZone.getDefault();
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
			Map mapObj = new HashMap();
			mapObj.put("key1", "aaa");
			mapObj.put("key2", new Date(90, 1, 1));
			mapObj.put("key3", new Object[] { "bbb", "ccc", new Date(100, 10, 10) });


			jsonWriter.object();
			NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, null);
			jsonWriter.endObject();
		}
		finally
		{
			TimeZone.setDefault(default1);
		}

		assertEquals("Simple object type with date toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"svy_date\",\"_V\":\"1990-02-01T00:00+02:00\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"svy_date\",\"_V\":\"2000-11-10T00:00+02:00\"}]}}}}");

	}

	@Test
	public void testNestedObjectTypeWithDatesToJson() throws JSONException
	{
		TimeZone default1 = TimeZone.getDefault();
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));

			Map mapObj = new HashMap();
			Map mapObj2 = new HashMap();
			mapObj.put("key1", mapObj2);
			mapObj2.put("key1", "aaa");
			mapObj2.put("key2", new Date(90, 1, 1));
			mapObj2.put("key3", new Object[] { "bbb", "ccc", new Date(100, 10, 10) });
			List list = new ArrayList();
			list.add(new Date(100, 10, 10));
			list.add(new Date(101, 11, 11));
			list.add("bbb");
			mapObj2.put("key4", list);


			jsonWriter.object();
			NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, null);
			jsonWriter.endObject();
		}
		finally
		{
			TimeZone.setDefault(default1);
		}


		assertEquals("Simple nested object type with date toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"_T\":\"object\",\"_V\":{\"key1\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"svy_date\",\"_V\":\"1990-02-01T00:00+02:00\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"svy_date\",\"_V\":\"2000-11-10T00:00+02:00\"}]},\"key4\":{\"_T\":\"object\",\"_V\":[{\"_T\":\"svy_date\",\"_V\":\"2000-11-10T00:00+02:00\"},{\"_T\":\"svy_date\",\"_V\":\"2001-12-11T00:00+02:00\"},\"bbb\"]}}}}}}");

	}

	@Test
	public void testObjectTypeWithBrowserFunctionToJson() throws JSONException
	{
		INGApplication application = new ServiceProvider();
		Map mapObj = new HashMap();
		mapObj.put("key1", "aaa");
		mapObj.put("key2", new BrowserFunction("func1", application));
		mapObj.put("key3", new Object[] { "bbb", "ccc", new BrowserFunction("func2", application) });

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		assertEquals("Simple object type with client function toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"clientfunction\",\"_V\":\"func1\"},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"clientfunction\",\"_V\":\"func2\"}]}}}}");

	}

	@Test
	public void testObjectTypeWithServerFunctionToJson() throws JSONException
	{
		INGApplication application = new ServiceProvider();
		Map mapObj = new HashMap();
		mapObj.put("key1", "aaa");
		mapObj.put("key2", new BaseFunction());
		mapObj.put("key3", new Object[] { "bbb", "ccc", new BaseFunction() });

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		NGObjectPropertyType.NG_INSTANCE.toJSON(jsonWriter, "myobject", mapObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		JSONObject json = new JSONObject(stringWriter.toString());
		JSONObject firstHash = json.getJSONObject("myobject").getJSONObject("_V").getJSONObject("key2").getJSONObject("_V");
		firstHash.put("functionhash", "dummyhash");
		JSONObject secondHash = json.getJSONObject("myobject").getJSONObject("_V").getJSONObject("key3").getJSONArray("_V").getJSONObject(2)
			.getJSONObject("_V");
		secondHash.put("functionhash", "dummyhash");
		assertEquals("Simple object type with server function toJSON",
			json.toString(),
			"{\"myobject\":{\"_T\":\"object\",\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}},\"key3\":{\"_T\":\"object\",\"_V\":[\"bbb\",\"ccc\",{\"_T\":\"NativeFunction\",\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"}}]}}}}");

	}

	@Test
	public void testSimpleMapTypeToJson() throws JSONException
	{
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", 123);
		jsonObj.put("key3", true);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		assertEquals("Simple map type toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":123,\"key3\":true}}");

	}

	@Test
	public void testMapTypeWithNullToJson() throws JSONException
	{
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", JSONObject.NULL);
		jsonObj.put("key3", new Object[] { "bbb", "ccc", JSONObject.NULL });

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		assertEquals("Simple map type with null toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":null,\"key3\":[\"bbb\",\"ccc\",null]}}");

	}

	@Test
	public void testMapTypeWithDatesToJson() throws JSONException
	{
		TimeZone default1 = TimeZone.getDefault();
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("key1", "aaa");
			jsonObj.put("key2", new Date(90, 1, 1));
			JSONArray arr = new JSONArray();
			arr.put(new Date(100, 10, 10));
			arr.put(JSONObject.NULL);
			arr.put(false);
			jsonObj.put("key3", arr);
			jsonWriter.object();
			MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
			jsonWriter.endObject();
		}
		finally
		{
			TimeZone.setDefault(default1);
		}

		assertEquals("Simple map type with date toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":{\"_V\":\"1990-02-01T00:00+02:00\",\"_T\":\"svy_date\"},\"key3\":{\"_V\":[{\"_V\":\"2000-11-10T00:00+02:00\",\"_T\":\"svy_date\"},null,false],\"_T\":\"object\"}}}");

	}

	@Test
	public void testNestedMapTypeWithDatesToJson() throws JSONException
	{
		TimeZone default1 = TimeZone.getDefault();
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
			JSONObject jsonObj = new JSONObject();
			JSONObject jsonObj2 = new JSONObject();
			jsonObj.put("key1", jsonObj2);
			jsonObj2.put("key1", "aaa");
			jsonObj2.put("key2", new Date(90, 1, 1));
			JSONArray arr = new JSONArray();
			arr.put("bbb");
			arr.put(new Date(100, 10, 10));
			arr.put(new Date(101, 11, 11));
			jsonObj2.put("key3", arr);
			arr = new JSONArray();
			arr.put(new Date(100, 10, 10));
			arr.put(new Date(101, 11, 11));
			arr.put("bbb");
			jsonObj2.put("key4", arr);


			jsonWriter.object();
			MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
			jsonWriter.endObject();
		}
		finally
		{
			TimeZone.setDefault(default1);
		}


		assertEquals("Simple nested map type with date toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":{\"_V\":{\"key1\":\"aaa\",\"key2\":{\"_V\":\"1990-02-01T00:00+02:00\",\"_T\":\"svy_date\"},\"key3\":{\"_V\":[\"bbb\",{\"_V\":\"2000-11-10T00:00+02:00\",\"_T\":\"svy_date\"},{\"_V\":\"2001-12-11T00:00+02:00\",\"_T\":\"svy_date\"}],\"_T\":\"object\"},\"key4\":{\"_V\":[{\"_V\":\"2000-11-10T00:00+02:00\",\"_T\":\"svy_date\"},{\"_V\":\"2001-12-11T00:00+02:00\",\"_T\":\"svy_date\"},\"bbb\"],\"_T\":\"object\"}},\"_T\":\"object\"}}}");

	}

	@Test
	public void testMapTypeWithBrowserFunctionToJson() throws JSONException
	{
		INGApplication application = new ServiceProvider();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", new BrowserFunction("func1", application));
		JSONArray arr = new JSONArray();
		arr.put("bbb");
		arr.put(new BrowserFunction("func2", application));
		arr.put(new BrowserFunction("func3", application));
		jsonObj.put("key3", arr);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		assertEquals("Simple map type with client function toJSON",
			stringWriter.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":{\"_V\":\"func1\",\"_T\":\"clientfunction\"},\"key3\":{\"_V\":[\"bbb\",{\"_V\":\"func2\",\"_T\":\"clientfunction\"},{\"_V\":\"func3\",\"_T\":\"clientfunction\"}],\"_T\":\"object\"}}}");

	}

	@Test
	public void testMapTypeWithServerFunctionToJson() throws JSONException
	{
		INGApplication application = new ServiceProvider();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("key1", "aaa");
		jsonObj.put("key2", new BaseFunction());
		JSONArray arr = new JSONArray();
		arr.put("bbb");
		arr.put(new BaseFunction());
		arr.put(new BaseFunction());
		jsonObj.put("key3", arr);

		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);

		jsonWriter.object();
		MapPropertyType.INSTANCE.toJSON(jsonWriter, "myobject", jsonObj, MY_OBJECT_PD, context);
		jsonWriter.endObject();

		JSONObject json = new JSONObject(stringWriter.toString());
		JSONObject firstHash = json.getJSONObject("myobject").getJSONObject("key2").getJSONObject("_V");
		firstHash.put("functionhash", "dummyhash");
		JSONObject secondHash = json.getJSONObject("myobject").getJSONObject("key3").getJSONArray("_V").getJSONObject(2)
			.getJSONObject("_V");
		secondHash.put("functionhash", "dummyhash");
		secondHash = json.getJSONObject("myobject").getJSONObject("key3").getJSONArray("_V").getJSONObject(1)
			.getJSONObject("_V");
		secondHash.put("functionhash", "dummyhash");
		assertEquals("Simple map type with server function toJSON",
			json.toString(),
			"{\"myobject\":{\"key1\":\"aaa\",\"key2\":{\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"},\"_T\":\"NativeFunction\"},\"key3\":{\"_V\":[\"bbb\",{\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"},\"_T\":\"NativeFunction\"},{\"_V\":{\"functionhash\":\"dummyhash\",\"svyType\":\"NativeFunction\"},\"_T\":\"NativeFunction\"}],\"_T\":\"object\"}}}");

	}
}

