/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.util;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SimpleTimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

public class ServoyJSONObject extends JSONObject implements Serializable, Cloneable
{
	protected boolean noQuotes = true;
	protected boolean newLines = true;
	protected boolean noBrackets = false;
	private static final SimpleDateFormat ISO_DATE_FORMAT; // from rhino NativeDate

	/**
	 * JSONObject.NULL is nice but has one problem - java code that manipulates JSON needs to see the difference between javascript null
	 * (which is meant to be JSONObject.NULL) and javascript undefined (which would be Java null).
	 *
	 * But JSONObject.NULL.equals() says it's equal to Java null and because of that java code is not able to handle correctly the difference between null and JSONObject.NULL.
	 * This NULL_FOR_JAVA has the same meaning in Javascript but is for temporary use in Java portions of code that need to make that difference. Be sure to convert that back to JSONObject.NULL when working with org.json.
	 */
	public static final Object NULL_FOR_JAVA = new NullObject();

	static
	{
		ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		ISO_DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "UTC"));
		ISO_DATE_FORMAT.setLenient(false);
	}

	public ServoyJSONObject()
	{
		super();
	}

	public ServoyJSONObject(boolean noQuotes, boolean newLines)
	{
		super();
		this.noQuotes = noQuotes;
		this.newLines = newLines;
	}

	public ServoyJSONObject(JSONObject objToCopyFrom, String[] propsToCopy, boolean noQuotes, boolean newLines) throws JSONException
	{
		super(objToCopyFrom, propsToCopy);
		this.noQuotes = noQuotes;
		this.newLines = newLines;
	}

	public ServoyJSONObject(Map<String, Object> props)
	{
		super(props);
	}

	public ServoyJSONObject(Map<String, Object> props, boolean noQuotes, boolean newLines)
	{
		super(props);
		this.noQuotes = noQuotes;
		this.newLines = newLines;
	}

	public ServoyJSONObject(String data, boolean noBrackets) throws JSONException
	{
		this(data, noBrackets, true, true);
	}

	public ServoyJSONObject(String data, boolean noBrackets, boolean noQuotes, boolean newLines) throws JSONException
	{
		super((noBrackets ? "{" : "") + (newLines ? replaceEmbeddedStringNewlines(data) : data) + (noBrackets ? "}" : ""));
		this.noQuotes = noQuotes;
		this.newLines = newLines;
		this.noBrackets = noBrackets;
	}

	public boolean isNoQuotes()
	{
		return noQuotes;
	}

	public void setNoQuotes(boolean noQuotes)
	{
		this.noQuotes = noQuotes;
	}

	public boolean isNewLines()
	{
		return newLines;
	}

	public void setNewLines(boolean newLines)
	{
		this.newLines = newLines;
	}

	public boolean isNoBrackets()
	{
		return noBrackets;
	}

	public void setNoBrackets(boolean noBrackets)
	{
		this.noBrackets = noBrackets;
	}

	/**
	 * Replace newlines with \n when found within quoted strings.
	 *
	 * @param data
	 * @return
	 */
	public static String replaceEmbeddedStringNewlines(String data)
	{
		if (data == null || data.indexOf('\n') < 0) return data;

		StringBuffer sb = new StringBuffer();
		char[] chars = data.toCharArray();
		boolean instring = false;
		boolean escape = false;
		for (char c : chars)
		{
			if (!escape)
			{
				switch (c)
				{
					case '"' :
						instring = !instring;
						break;
					case '\n' :
						if (instring)
						{
							sb.append('\\');
							c = 'n';
						}
						break;

					default :
						break;
				}
			}
			if (c != '\r')
			{
				escape = !escape && (c == '\\');
				// strip out carriage returns.
				sb.append(c);
			}
		}

		return sb.toString();
	}

	/**
	 * Replace \n with newlines when found within quoted strings.
	 *
	 * @param data
	 * @return
	 */
	public static String replaceEmbeddedStringEscapedNewlines(String data)
	{
		if (data == null || data.indexOf("\\n") < 0) return data; //$NON-NLS-1$

		StringBuffer sb = new StringBuffer();
		char[] chars = data.toCharArray();
		boolean instring = false;
		boolean escape = false;
		for (char c : chars)
		{
			if (escape)
			{
				if (instring && c == 'n')
				{
					sb.append('\\');
					c = '\n';
				}
				else
				{
					sb.append('\\');
				}
				escape = false;
			}
			else
			{
				switch (c)
				{
					case '"' :
						instring = !instring;
						break;
					case '\\' :
						escape = true;
						break;

					default :
						break;
				}
			}
			if (!escape) sb.append(c);
		}
		if (escape) sb.append('\\');

		return sb.toString();
	}

	@Override
	public String toString()
	{
		return toString(this, noQuotes, newLines, noBrackets);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ServoyJSONObject)
		{
			return ServoyJSONObject.toString((ServoyJSONObject)obj, true, false, true).equals(ServoyJSONObject.toString(this, true, false, true));
		}

		return false;
	}

	@Override
	public ServoyJSONObject clone()
	{
		String s = ServoyJSONObject.toString(this, isNoQuotes(), isNewLines(), isNoBrackets());
		try
		{
			return new ServoyJSONObject(s, isNoBrackets(), isNoQuotes(), isNewLines());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			return null;
		}
	}

	/**
	 * Return keys in sorted order.
	 */
	@Override
	public Iterator<String> keys()
	{
		Iterator<String> keys = super.keys();
		String[] keysArray = Utils.<String> asArray(keys, String.class);
		Arrays.sort(keysArray);
		return Arrays.asList(keysArray).iterator();
	}

	public String toString(boolean noBrackets)
	{
		return toString(this, noQuotes, newLines, noBrackets);
	}

	public static String toString(JSONObject json, boolean noQuotes, boolean newLines, boolean noBrackets)
	{
		try
		{
			return appendtoString(new StringBuffer(), json, noQuotes, newLines, noBrackets).toString();
		}
		catch (JSONException e)
		{
			throw new RuntimeException("Error serializing json object", e);
		}
	}

	private static StringBuffer appendtoString(StringBuffer sb, Object value, boolean noQuotes, boolean newLines, boolean noBrackets) throws JSONException
	{
		if (value == null || value.equals(null))
		{
			sb.append("null"); //$NON-NLS-1$
		}
		else if (value instanceof JSONString)
		{
			Object o;
			try
			{
				o = ((JSONString)value).toJSONString();
			}
			catch (Exception e)
			{
				throw new JSONException(e);
			}
			if (o instanceof String)
			{
				sb.append((String)o);
			}
			throw new JSONException("Bad value from toJSONString: " + o);
		}
		else if (value instanceof Number)
		{
			sb.append(numberToString((Number)value));
		}
		else if (value instanceof Boolean)
		{
			sb.append(value.toString());
		}
		else if (value instanceof JSONObject)
		{
			if (value instanceof ServoyJSONObject)
			{
				ServoyJSONObject svjson = (ServoyJSONObject)value;
				appendtoString(sb, new JSONWrapperMap(svjson)
				{
					@Override
					protected Object toJava(Object o)
					{
						return o;
					}
				}, svjson.noQuotes, svjson.newLines, noBrackets /* use settings from context */);
			}
			else
			{
				appendtoString(sb, new JSONWrapperMap((JSONObject)value)
				{
					@Override
					protected Object toJava(Object o)
					{
						return o;
					}
				}, noQuotes, newLines, noBrackets);
			}
		}
		else if (value instanceof Map)
		{
			boolean useNewLines = newLines;
			boolean useNoQuotes = noQuotes;

			if (value instanceof JSONWrapperMap && ((JSONWrapperMap)value).getJson() instanceof ServoyJSONObject)
			{
				ServoyJSONObject svjson = (ServoyJSONObject)((JSONWrapperMap)value).getJson();
				useNewLines = svjson.newLines;
				useNoQuotes = svjson.noQuotes;
			}
			Map<String, Object> map = (Map<String, Object>)value;
			// sort the keys
			String[] keysArray = map.keySet().toArray(new String[map.size()]);
			if (keysArray.length > 1) Arrays.sort(keysArray);

			if (!noBrackets)
			{
				sb.append('{');
				if (useNewLines) sb.append('\n');
			}

			for (int i = 0; i < keysArray.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
					if (useNewLines) sb.append('\n');
				}
				if (useNoQuotes) sb.append(keysArray[i]);
				else sb.append(quote(keysArray[i]));
				sb.append(':');
				appendtoString(sb, map.get(keysArray[i]), noQuotes, newLines, false);
			}

			if (!noBrackets)
			{
				if (useNewLines) sb.append('\n');
				sb.append('}');
			}
		}
		else if (value instanceof Collection)
		{
			appendtoString(sb, new ServoyJSONArray((Collection< ? >)value), noQuotes, newLines, false);
		}
		else if (value.getClass().isArray())
		{
			appendtoString(sb, new ServoyJSONArray(value), noQuotes, newLines, false);
		}
		else if (value instanceof JSONArray)
		{
			JSONArray array = (JSONArray)value;
			sb.append('[');
			for (int i = 0; i < array.length(); i++)
			{
				if (i > 0) sb.append(',');
				if (newLines) sb.append('\n');
				appendtoString(sb, array.opt(i), noQuotes, newLines, false);
			}
			if (newLines && array.length() > 0) sb.append('\n');
			sb.append(']');
		}
		else if (value instanceof Date) // use fixed format as used in rhino NativeDate
		{
			synchronized (ISO_DATE_FORMAT)
			{
				sb.append(quote(ISO_DATE_FORMAT.format(value)));
			}
		}
		else
		{
			sb.append(replaceEmbeddedStringEscapedNewlines(quote(value.toString())));
		}

		return sb;
	}

	public static Date parseDate(String s)
	{
		if (s != null)
		{
			try
			{
				synchronized (ISO_DATE_FORMAT)
				{
					return ISO_DATE_FORMAT.parse(s);
				}
			}
			catch (java.text.ParseException ex)
			{
				Debug.trace(ex);
			}
		}

		return null; // cannot parse via iso format
	}

	/**
	 * Convert a json object to a java object
	 *
	 * @param o
	 * @return
	 */
	public static Object toJava(Object o)
	{
		if (o instanceof JSONObject)
		{
			return new JSONWrapperMap((JSONObject)o);
		}
		if (o instanceof JSONArray)
		{
			return new JSONWrapperList((JSONArray)o);
		}
		if (JSONObject.NULL.equals(o))
		{
			return null;
		}
		return o;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException
	{
		out.writeBoolean(noQuotes);
		out.writeBoolean(newLines);
		out.writeBoolean(noBrackets);
		out.writeObject(toSerializable(this));
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		noQuotes = in.readBoolean();
		newLines = in.readBoolean();
		noBrackets = in.readBoolean();
		fromSerializable(this, in.readObject());
	}

	public static JSONObject mergeAndDeepCloneJSON(JSONObject toCopyIn, JSONObject target)
	{
		for (String key : toCopyIn.keySet())
		{
			Object toCopy = toCopyIn.get(key);
			Object current = target.opt(key);
			if (toCopy instanceof JSONObject)
			{
				if (!(current instanceof JSONObject))
				{
					current = new JSONObject();
					target.put(key, current);
				}
				mergeAndDeepCloneJSON((JSONObject)toCopy, (JSONObject)current);
			}
			else target.put(key, toCopy);
		}
		return target;
	}

	static Object toSerializable(Object o)
	{
		if (o instanceof JSONObject)
		{
			JSONObject obj = (JSONObject)o;
			HashMap<String, Object> map = new HashMap<String, Object>();
			for (String key : ServoyJSONObject.getNames(obj))
			{
				try
				{
					map.put(key, toSerializable(obj.get(key)));
				}
				catch (JSONException e)
				{
					Debug.trace(e);
				}
			}
			return map;
		}
		if (o instanceof JSONArray)
		{
			JSONArray arr = (JSONArray)o;
			Object[] array = new Object[arr.length()];
			for (int i = 0; i < arr.length(); i++)
			{
				try
				{
					array[i] = toSerializable(arr.get(i));
				}
				catch (JSONException e)
				{
					Debug.trace(e);
				}
			}
			return array;
		}
		if (o == JSONObject.NULL)
		{
			return NULL_FOR_JAVA;
		}

		return o;
	}

	static Object fromSerializable(Object parent, Object o)
	{
		if (o instanceof NullObject)
		{
			return JSONObject.NULL;
		}
		if (o instanceof Map< ? , ? >)
		{
			JSONObject obj = parent != null ? (JSONObject)parent : new ServoyJSONObject();
			@SuppressWarnings("unchecked")
			HashMap<String, Object> map = (HashMap<String, Object>)o;
			for (String key : map.keySet())
			{
				try
				{
					obj.put(key, fromSerializable(null, map.get(key)));
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
			}
			return obj;
		}
		if (o instanceof Object[])
		{
			JSONArray arr = parent != null ? (JSONArray)parent : new ServoyJSONArray();
			Object[] array = (Object[])o;
			for (int i = 0; i < array.length; i++)
			{
				try
				{
					arr.put(i, fromSerializable(null, array[i]));
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
			}
			return arr;
		}
		return o;
	}

	/**
	 * The same as {@link JSONObject#getNames(JSONObject)} but in case you give it an empty object '{}' it will not return null, but an String[0].
	 */
	public static String[] getNames(JSONObject jsonObject)
	{
		if (jsonObject == null) return new String[0];
		String[] x = JSONObject.getNames(jsonObject);
		return x == null ? new String[0] : x;
	}

	/**
	 * As java null is mapped to Javascript undefined, we need a way of treating real javascript null values.
	 * This is a helper method to check for nulls.
	 */
	public static boolean isJavascriptNull(Object o)
	{
		return o == JSONObject.NULL || o == ServoyJSONObject.NULL_FOR_JAVA;
	}

	/**
	 * Java null is mapped to Javascript undefined. This is basically a null check.
	 */
	public static boolean isJavascriptUndefined(Object o)
	{
		return o == null;
	}

	public static boolean isJavascriptNullOrUndefined(Object o)
	{
		return isJavascriptUndefined(o) || isJavascriptNull(o);
	}

	public static Object adjustJavascriptNULLForOrgJSON(Object o)
	{
		return o == ServoyJSONObject.NULL_FOR_JAVA ? JSONObject.NULL : o;
	}

	public static Object adjustJavascriptNULLForJava(Object o)
	{
		return o == JSONObject.NULL ? ServoyJSONObject.NULL_FOR_JAVA : o;
	}

	/**
	 * In Java, where generics are used in some places, JSONObject.NULL messes up types (class cast exceptions because for example what we expect to be a String
	 * can be a String, null or JSONObject.NULL in json). Null is better there.
	 *
	 * @return null in case o is JSONObject.NULL. The given object otherwise. Note that a java return value of null is equivalent to the javascript undefined from org.json point of view.
	 */
	public static Object jsonNullToNull(Object o)
	{
		return o == JSONObject.NULL ? null : o;
	}

	/**
	 * Can be used in places where a java 'null' actually means a JSON null instead of 'not defined'.
	 *
	 * @return JSONObject.NULL in case of null. The given object otherwise. Note that normally a java return value of null is equivalent to the javascript undefined from org.json point of view.
	 */
	public static Object nullToJsonNull(Object o)
	{
		return o == null ? JSONObject.NULL : o;
	}

	/**
	 * This method is useful as optString in parent implementation is flawed.
	 * If you set a null (JSONObject.NULL) value it optString(key) would return "null" which is unexpected. Null would have been expected...
	 */
	public static String optString(String key, JSONObject jsonObject, String defaultValue)
	{
		Object v = jsonNullToNull(jsonObject.opt(key));
		return v != null ? v.toString() : defaultValue;
	}

	private static final class NullObject implements Serializable
	{
		@Override
		public boolean equals(Object obj)
		{
			return super.equals(obj) || obj == JSONObject.NULL;
		}
	}
}
