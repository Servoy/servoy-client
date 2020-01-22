/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.util.Arrays;

import org.json.JSONString;
import org.junit.Test;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.JSONStringWrapper;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.j2db.util.Pair;

/**
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ViewportClientSideTypesTests
{

	@Test(expected = RuntimeException.class)
	public void errNotAllIndexesRegistered()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 20);
		keeper.registerClientSideType(5, null);
		keeper.registerClientSideType(6, null);
		keeper.getClientSideTypes();
	}

	@Test(expected = IllegalArgumentException.class)
	public void errNotUnexpectedOrder1()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 20);
		keeper.registerClientSideType(0, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void errNotUnexpectedOrder2()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 20);
		keeper.registerClientSideType(1000, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void errNotUnexpectedOrder3()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 20);
		keeper.registerClientSideType(6, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void errNotUnexpectedOrder4()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 20);
		keeper.registerClientSideType(5, null);
		keeper.registerClientSideType(6, null);
		keeper.registerClientSideType(8, null);
	}

	@Test
	public void coalesceTo1MainType1()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 11);
		keeper.registerClientSideType(5, null);
		keeper.registerClientSideType(6, null);
		keeper.registerClientSideType(7, null);
		keeper.registerClientSideType(8, null);
		keeper.registerClientSideType(9, null);
		keeper.registerClientSideType(10, null);
		keeper.registerClientSideType(11, null);

		assertEquals("Null main type expected", null, keeper.getClientSideTypes());
	}

	@Test
	public void coalesceTo1MainType2()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(5, 11);
		keeper.registerClientSideType(5, Arrays.asList(p("a", "date")));
		keeper.registerClientSideType(6, Arrays.asList(p("b", "date")));
		keeper.registerClientSideType(7, Arrays.asList(p("c", "date")));
		keeper.registerClientSideType(8, Arrays.asList(p("d", "date")));
		keeper.registerClientSideType(9, Arrays.asList(p("e", "date")));
		keeper.registerClientSideType(10, Arrays.asList(p("f", "date")));
		keeper.registerClientSideType(11, Arrays.asList(p("g", "date")));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value("date").endObject();

		assertEquals("Date main type expected", expected.toJSONString(), jsonTypes);
	}

	@Test
	public void coalesceToMainTypesPerColumn()
	{
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(0, 6);
		keeper.registerClientSideType(0, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(1, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(2, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(3, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(4, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(5, Arrays.asList(p("a", "date"), p("b", null)));
		keeper.registerClientSideType(6, Arrays.asList(p("a", "date"), p("b", null)));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value("date").key(ViewportClientSideTypes.COL_TYPES).object().key("b").object().key(
			JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(null).endObject().endObject().endObject();

		// {"mT":"date","cT":{"b":{"mT":null}}}
		assertEquals("Main type and b column main type expected", expected.toJSONString(), jsonTypes);
	}

	private Pair<String, JSONString> p(String a, String b)
	{
		return new Pair<String, JSONString>(a, b != null ? new JSONStringWrapper('"' + b + '"') : null);
	}

	@Test
	public void coalesceToMixedTypesPerColumn1()
	{
		// both column level types and cell level types
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(501, 510);
		keeper.registerClientSideType(501, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(502, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(503, Arrays.asList(p("a", "date"), p("b", null), p("c", "xyz")));
		keeper.registerClientSideType(504, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(505, Arrays.asList(p("a", "date"), p("b", null), p("c", null)));
		keeper.registerClientSideType(506, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(507, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(508, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(509, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(510, Arrays.asList(p("a", "date"), p("b", null), p("c", null)));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		// @formatter:off
		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value("date").key(ViewportClientSideTypes.COL_TYPES).object()
			.key("b").object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(null).endObject()
			.key("c").object().key(ViewportClientSideTypes.CELL_TYPES).array()
		        .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("xyz").key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(2).endArray().endObject()
		        .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(null).key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(4).value(9).endArray().endObject()
		        .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("zyx").key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(5).value(7).value(8).endArray().endObject()
	        .endArray()
			.endObject()
			.endObject().endObject();
		// @formatter:on

		JSONAssert.assertEquals(
			"Mixed types with first cell being main type. Expected:\n" + expected.toJSONString() + "\nActual:\n" + jsonTypes + "\n\nJSONAssert explanation:\n",
			expected.toJSONString(), jsonTypes, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void coalesceToMixedTypesPerColumn2()
	{
		// both column level types and cell level types
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(501, 510);
		keeper.registerClientSideType(501, Arrays.asList(p("a", "date"), p("b", null), p("c", "xyz")));
		keeper.registerClientSideType(502, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(503, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(504, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(505, Arrays.asList(p("a", "date"), p("b", null), p("c", null)));
		keeper.registerClientSideType(506, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(507, Arrays.asList(p("a", "date"), p("b", null), p("c", "date")));
		keeper.registerClientSideType(508, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(509, Arrays.asList(p("a", "date"), p("b", null), p("c", "zyx")));
		keeper.registerClientSideType(510, Arrays.asList(p("a", "date"), p("b", null), p("c", null)));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		// @formatter:off
		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value("date").key(ViewportClientSideTypes.COL_TYPES).object()
			.key("b").object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(null).endObject()
			.key("c").object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("xyz")
			                  .key(ViewportClientSideTypes.CELL_TYPES).array()
		                      .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("date").key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(1).value(2).value(3).value(6).endArray().endObject()
		                      .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(null).key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(4).value(9).endArray().endObject()
		                      .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("zyx").key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(5).value(7).value(8).endArray().endObject()
			                  .endArray()
			.endObject()
			.endObject().endObject();
		// @formatter:on

		JSONAssert.assertEquals("Mixed types with first cell not being main type. Expected:\n" + expected.toJSONString() + "\nActual:\n" + jsonTypes +
			"\n\nJSONAssert explanation:\n", expected.toJSONString(), jsonTypes, JSONCompareMode.NON_EXTENSIBLE);
	}

	@Test
	public void coalesceWithNullColumnKey_foundsetLinked1()
	{
		// foundset linked viewports don't use column names so give null as column name
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(10001, 10007);
		keeper.registerClientSideType(10001, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10002, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10003, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10004, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10005, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10006, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10007, Arrays.asList(p(null, "date")));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		// @formatter:off
		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value("date").endObject();
		// @formatter:on

		// {"mT":"date"}
		assertEquals("Main type expected", expected.toJSONString(), jsonTypes);
	}

	@Test
	public void coalesceWithNullColumnKey_foundsetLinked2()
	{
		// foundset linked viewports don't use column names so give null as column name
		ViewportClientSideTypes keeper = new ViewportClientSideTypes(10001, 10007);
		keeper.registerClientSideType(10001, Arrays.asList(p(null, null)));
		keeper.registerClientSideType(10002, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10003, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10004, Arrays.asList(p(null, null)));
		keeper.registerClientSideType(10005, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10006, Arrays.asList(p(null, "date")));
		keeper.registerClientSideType(10007, Arrays.asList(p(null, "date")));

		String jsonTypes = keeper.getClientSideTypes().toJSONString();

		// @formatter:off
		EmbeddableJSONWriter expected = new EmbeddableJSONWriter(true);
		expected.object().key(ViewportClientSideTypes.MAIN_TYPE).value(null).key(ViewportClientSideTypes.COL_TYPES).object().key(ViewportClientSideTypes.CELL_TYPES).array()
		                      .object().key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value("date").key(ViewportClientSideTypes.FOR_ROW_IDXS).array().value(1).value(2).value(4).value(5).value(6).endArray().endObject()
			                  .endArray()
			.endObject().endObject();
		// @formatter:on

		JSONAssert.assertEquals(
			"Mixed types with first cell being main type. Expected:\n" + expected.toJSONString() + "\nActual:\n" + jsonTypes + "\n\nJSONAssert explanation:\n",
			expected.toJSONString(), jsonTypes, JSONCompareMode.NON_EXTENSIBLE);
	}

}