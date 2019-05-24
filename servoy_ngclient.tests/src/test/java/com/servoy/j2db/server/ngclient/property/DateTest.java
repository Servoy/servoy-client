package com.servoy.j2db.server.ngclient.property;
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

import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.junit.Assert;
import org.junit.Test;
import org.sablo.websocket.utils.DataConversion;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.servoy.j2db.server.ngclient.property.types.NGDatePropertyType;
import com.servoy.j2db.server.ngclient.utils.NGUtils;

/**
 * @author lvostinar
 *
 */
public class DateTest
{
	@Test
	public void testDatesToJson() throws JSONException
	{
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(70, 1, 2), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(
			new JSONObject("{\"mydate\" : \"1970-02-02T00:00" +
				OffsetDateTime.ofInstant(new java.util.Date(70, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString() + "\"}"),
			new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(19, 1, 2), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		String stripOffsetSeconds = stripOffsetSeconds(
			"1919-02-02T00:00" + OffsetDateTime.ofInstant(new java.util.Date(19, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString());
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"" + stripOffsetSeconds + "\"}"), new JSONObject(stringWriter.toString()),
			JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(39, 1, 2), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(
			new JSONObject("{\"mydate\" : \"1939-02-02T00:00" +
				OffsetDateTime.ofInstant(new java.util.Date(39, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString() + "\"}"),
			new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(41, 1, 2), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(
			new JSONObject("{\"mydate\" : \"1941-02-02T00:00" +
				OffsetDateTime.ofInstant(new java.util.Date(41, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString() + "\"}"),
			new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(118, 5, 5, 11, 50, 55), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(),
			null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(
			new JSONObject("{\"mydate\" : \"2018-06-05T11:50:55" +
				OffsetDateTime.ofInstant(new java.util.Date(118, 5, 5, 11, 50, 55).toInstant(), ZoneId.systemDefault()).getOffset().toString() + "\"}"),
			new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		TimeZone default1 = TimeZone.getDefault();
		try
		{
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bucharest"));
			stringWriter = new StringWriter();
			jsonWriter = new JSONWriter(stringWriter);
			jsonWriter.object();
			NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(19, 1, 2), NGUtils.DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
			jsonWriter.endObject();
			JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"1919-02-02T00:00+01:44\"}"), new JSONObject(stringWriter.toString()),
				JSONCompareMode.STRICT);
		}
		finally
		{
			TimeZone.setDefault(default1);
		}
	}

	@Test
	public void testDatesToJsonLocalDate() throws JSONException
	{
		StringWriter stringWriter = new StringWriter();
		JSONWriter jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(70, 1, 2), NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"1970-02-02T00:00\"}"), new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(19, 1, 2), NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"1919-02-02T00:00\"}"), new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(39, 1, 2), NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"1939-02-02T00:00\"}"), new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(41, 1, 2), NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD, new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"1941-02-02T00:00\"}"), new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);

		stringWriter = new StringWriter();
		jsonWriter = new JSONWriter(stringWriter);
		jsonWriter.object();
		NGDatePropertyType.NG_INSTANCE.toJSON(jsonWriter, "mydate", new Date(118, 5, 5, 11, 50, 55), NGUtils.LOCAL_DATE_DATAPROVIDER_CACHED_PD,
			new DataConversion(), null);
		jsonWriter.endObject();
		JSONAssert.assertEquals(new JSONObject("{\"mydate\" : \"2018-06-05T11:50:55\"}"), new JSONObject(stringWriter.toString()), JSONCompareMode.STRICT);
	}

	@Test
	public void testDatesFromJson() throws JSONException
	{
		Assert.assertEquals(new Date(70, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON(
			"1970-02-02T00:00" + OffsetDateTime.ofInstant(new java.util.Date(70, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString(), false));

		Assert.assertEquals(new Date(19, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON(
			"1919-02-02T00:00" + OffsetDateTime.ofInstant(new java.util.Date(19, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString(), false));

		Assert.assertEquals(new Date(39, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON(
			"1939-02-02T00:00" + OffsetDateTime.ofInstant(new java.util.Date(39, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString(), false));

		Assert.assertEquals(new Date(41, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON(
			"1941-02-02T00:00" + OffsetDateTime.ofInstant(new java.util.Date(41, 1, 2).toInstant(), ZoneId.systemDefault()).getOffset().toString(), false));

		Assert.assertEquals(new Date(118, 5, 5, 11, 50, 55), NGDatePropertyType.NG_INSTANCE.fromJSON("2018-06-05T11:50:55" +
			OffsetDateTime.ofInstant(new java.util.Date(118, 5, 5, 11, 50, 55).toInstant(), ZoneId.systemDefault()).getOffset().toString(), false));

		Assert.assertEquals(new Date(new Date(70, 1, 2).getTime() + 3600000),
			NGDatePropertyType.NG_INSTANCE.fromJSON("1970-02-02T00:00" + ZoneOffset.ofTotalSeconds(
				ZoneId.systemDefault().getRules().getOffset(new java.util.Date(70, 1, 2).toInstant()).getTotalSeconds() - 3600).toString(), false));

		Assert.assertEquals(new Date(new Date(70, 1, 2).getTime() - 3600000),
			NGDatePropertyType.NG_INSTANCE.fromJSON("1970-02-02T00:00" + ZoneOffset.ofTotalSeconds(
				ZoneId.systemDefault().getRules().getOffset(new java.util.Date(70, 1, 2).toInstant()).getTotalSeconds() + 3600).toString(), false));

		Assert.assertEquals(new Date(new Date(70, 5, 2).getTime() + 3600000),
			NGDatePropertyType.NG_INSTANCE.fromJSON("1970-06-02T00:00" + ZoneOffset.ofTotalSeconds(
				ZoneId.systemDefault().getRules().getOffset(new java.util.Date(70, 5, 2).toInstant()).getTotalSeconds() - 3600).toString(), false));
	}

	private String stripOffsetSeconds(String dateTime)
	{
		String strippedDate = dateTime;
		if (strippedDate != null && strippedDate.indexOf('+') != -1)
		{
			String[] sDateA = strippedDate.split("\\+");
			String[] offset = sDateA[1].split(":");
			if (offset.length > 1) // seconds in offset, cut it, as it can't be handled in js
			{
				StringBuilder sDateBuilder = new StringBuilder(sDateA[0]).append('+');
				sDateBuilder.append(offset[0]).append(':').append(offset[1]);
				strippedDate = sDateBuilder.toString();
			}
		}
		return strippedDate;
	}

	@Test
	public void testDatesFromJsonLocalDate() throws JSONException
	{
		Assert.assertEquals(new Date(70, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1970-02-02T00:00+02:00", true));
		Assert.assertEquals(new Date(70, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1970-02-02T00:00+03:00", true));
		Assert.assertEquals(new Date(70, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1970-02-02T00:00-03:00", true));

		Assert.assertEquals(new Date(19, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1919-02-02T00:00+02:00", true));
		Assert.assertEquals(new Date(19, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1919-02-02T00:00+03:00", true));
		Assert.assertEquals(new Date(19, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1919-02-02T00:00-03:00", true));

		Assert.assertEquals(new Date(39, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1939-02-02T00:00+02:00", true));
		Assert.assertEquals(new Date(39, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1939-02-02T00:00+03:00", true));
		Assert.assertEquals(new Date(39, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1939-02-02T00:00-03:00", true));

		Assert.assertEquals(new Date(41, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1941-02-02T00:00+02:00", true));
		Assert.assertEquals(new Date(41, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1941-02-02T00:00+03:00", true));
		Assert.assertEquals(new Date(41, 1, 2), NGDatePropertyType.NG_INSTANCE.fromJSON("1941-02-02T00:00-03:00", true));

		Assert.assertEquals(new Date(118, 5, 5, 11, 50, 55), NGDatePropertyType.NG_INSTANCE.fromJSON("2018-06-05T11:50:55+02:00", true));
		Assert.assertEquals(new Date(118, 5, 5, 11, 50, 55), NGDatePropertyType.NG_INSTANCE.fromJSON("2018-06-05T11:50:55+03:00", true));
		Assert.assertEquals(new Date(118, 5, 5, 11, 50, 55), NGDatePropertyType.NG_INSTANCE.fromJSON("2018-06-05T11:50:55-03:00", true));

	}
}
