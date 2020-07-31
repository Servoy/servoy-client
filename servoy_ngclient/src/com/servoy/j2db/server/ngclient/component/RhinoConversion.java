/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Undefined;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.serialize.JSONConverter;

/**
 * @author emera
 */
public class RhinoConversion
{
	/**
	 * This is a hack to create a NativeArray that is not dense in order to call the put/delete methods when calling the splice method in native javascript.
	 * The maximumInitialCapacity from NativeArray is 10000
	 * If the length of the {@link NativeArray} is greater that 10000, the array will not be dense.
	 */
	private final static long MAX_NATIVE_ARRAY_LENGTH = 10001;

	/**
	 * Default conversion used to convert from Rhino property types that do not explicitly implement component <-> Rhino conversions. <BR/><BR/>
	 * Values of types that don't implement the sablo <-> rhino conversions are by default accessible directly.
	 */
	public static Object defaultFromRhino(Object propertyValue, Object oldValue) // PropertyDescription / IWebObjectContext ... can be made available here if needed
	{
		// convert simple values to json values
		if (isUndefinedOrNotFound(propertyValue))
		{
			return null;
		}

		if (propertyValue instanceof NativeDate)
		{
			return ((NativeDate)propertyValue).unwrap();
		}
		if (propertyValue instanceof NativeObject)
		{
			Map<String, Object> map = new HashMap<>();
			Map oldMap = (oldValue instanceof Map) ? (Map)oldValue : null;
			NativeObject no = (NativeObject)propertyValue;
			Object[] ids = no.getIds();
			for (Object id2 : ids)
			{
				String id = String.valueOf(id2);
				map.put(id, defaultFromRhino(no.get(id2), oldMap != null ? oldMap.get(id) : null));
			}
			return map;
		}
		if (propertyValue instanceof NativeArray)
		{
			List<Object> list = new ArrayList<Object>();

			List<Object> oldList = (oldValue instanceof List) ? (List<Object>)oldValue : null;

			final NativeArray no = (NativeArray)propertyValue;
			final long naLength = no.getLength();
			for (long id = 0; id < naLength; id++)
			{
				list.add(
					defaultFromRhino(no.get(id), oldList != null ? oldList.get((int)id) : null));

			}
			return list;
		}
		if (propertyValue instanceof FormScope && ((FormScope)propertyValue).getFormController() != null)
			return ((FormScope)propertyValue).getFormController().getName();
		if (propertyValue instanceof IFormController) return ((IFormController)propertyValue).getName();

		if (propertyValue instanceof JSDataSet)
		{
			return ((JSDataSet)propertyValue).getDataSet();
		}

		if (propertyValue instanceof RhinoMapOrArrayWrapper)
		{
			return ((RhinoMapOrArrayWrapper)propertyValue).getWrappedValue();
		}
		if (propertyValue instanceof CharSequence)
		{
			return propertyValue.toString();
		}
		return propertyValue;
	}

	public static boolean isUndefinedOrNotFound(Object propertyValue)
	{
		return propertyValue == Scriptable.NOT_FOUND /* same as UniqueTag.NOT_FOUND */ || Undefined.isUndefined(propertyValue);
	}

	/**
	 * Default conversion used to convert to Rhino property types that do not explicitly implement component <-> Rhino conversions. <BR/><BR/>
	 * Types that don't implement the sablo <-> rhino conversions are by default available and their value is accessible directly.
	 */
	public static Object defaultToRhino(final Object webComponentValue, final PropertyDescription pd, final IWebObjectContext webObjectContext,
		Scriptable startScriptable)
	{
		// convert simple json values to Rhino values
		// this tries to reverse the conversion from #defaultFromRhino(...) where possible and needed (for example there's no use converting Date because Rhino will know how to handle them anyway)

		if (webComponentValue == null)
		{
			return null; // here we don't know if it's supposed to be null or undefined because for example if value comes from browser to sablo value null (undefined in JSON) will be null but JSONObject.NULL will also be converted into null
		}
		if (webComponentValue == JSONObject.NULL)
		{
			return null; // will probably never happen as you don't usually have JSONObject.NULL as a sablo java value; see comment above
		}

		if (webComponentValue instanceof IDataSet)
		{
			return new JSDataSet((IDataSet)webComponentValue);
		}

		if (webComponentValue instanceof List && !(webComponentValue instanceof NativeArray))
		{
			List< ? > array = (List< ? >)webComponentValue;
			Context cx = Context.enter();
			Scriptable newObject = null;
			try
			{
				final boolean[] initializing = new boolean[] { true };
				newObject = new NativeArray(MAX_NATIVE_ARRAY_LENGTH)
				{
					@Override
					public void put(int index, Scriptable start, Object value)
					{
						super.put(index, start, value);
						if (!initializing[0])
						{
							value = defaultFromRhino(value, null);

							if (index < ((List)webComponentValue).size())
							{
								if (!Utils.equalObjects(((List)webComponentValue).get(index), value))
								{
									((List)webComponentValue).set(index, value);
									if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
								}
							}
							else
							{
								for (int i = ((List)webComponentValue).size(); i < index; i++)
								{

									((List)webComponentValue).add(i, null);
								}
								((List)webComponentValue).add(index, value);
								if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());

							}
						}
					}

					@Override
					public void delete(int index)
					{
						super.delete(index);
						if (index < ((List)webComponentValue).size())
						{
							((List)webComponentValue).remove(index);
						}
						if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
					}
				};

				ScriptableObject.putProperty(newObject, "length", array.size());
				ScriptRuntime.setBuiltinProtoAndParent((NativeArray)newObject, startScriptable, TopLevel.Builtins.Array);
				for (int i = 0; i < array.size(); i++)
				{
					Object value = null;
					try
					{
						value = array.get(i);
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
					if (value != null)
					{
						value = defaultToRhino(value, pd, webObjectContext, startScriptable);
					}
					ScriptRuntime.setObjectIndex(newObject, i, value, cx);
				}
				initializing[0] = false;
				return newObject;
			}
			finally
			{
				Context.exit();
			}

		}

		if (webComponentValue instanceof Map && !(webComponentValue instanceof NativeObject))
		{
			Map< ? , ? > map = (Map)webComponentValue;
			Context cx = Context.enter();
			Scriptable newObject = null;
			try
			{
				final boolean[] initializing = new boolean[] { true };
				// code from Context.newObject(Scriptable)
				newObject = new NativeObject()
				{
					@Override
					public void put(String name, Scriptable start, Object value)
					{
						super.put(name, start, value);
						if (!initializing[0])
						{
							value = defaultFromRhino(value, null);
							if (!Utils.equalObjects(((Map)webComponentValue).get(name), value))
							{
								((Map)webComponentValue).put(name, value);
								if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
							}
						}
					}

					@Override
					public void delete(String name)
					{
						super.delete(name);
						((Map)webComponentValue).remove(name);
						if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
					}
				};

				ScriptRuntime.setBuiltinProtoAndParent((NativeObject)newObject, startScriptable, TopLevel.Builtins.Object);
				for (Object key : ((Map)webComponentValue).keySet())
				{
					((NativeObject)newObject).defineProperty(key.toString(),
						defaultToRhino(((Map)webComponentValue).get(key), pd, webObjectContext, startScriptable),
						ScriptableObject.EMPTY);
				}
				initializing[0] = false;
				return newObject;
			}
			finally
			{
				Context.exit();
			}
		}
		if (webComponentValue instanceof JSONObject)
		{
			JSONObject json = (JSONObject)webComponentValue;
			Context cx = Context.enter();
			Scriptable newObject = null;
			try
			{
				final boolean[] initializing = new boolean[] { true };
				// code from Context.newObject(Scriptable)
				newObject = new NativeObject()
				{
					private final JSONConverter converter = new JSONConverter();

					@Override
					public void put(String name, Scriptable start, Object value)
					{
						super.put(name, start, value);
						if (!initializing[0])
						{
							try
							{
								value = converter.convertToJSONValue(value);
							}
							catch (Exception e)
							{
								Debug.error(e);
							}
							if (!Utils.equalObjects(json.opt(name), value))
							{
								json.put(name, value);
								if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
							}
						}
					}

					@Override
					public void delete(String name)
					{
						super.delete(name);
						json.remove(name);
						if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
					}
				};
				ScriptRuntime.setBuiltinProtoAndParent((NativeObject)newObject, startScriptable,
					TopLevel.Builtins.Object);
				Iterator<String> iterator = json.keys();
				while (iterator.hasNext())
				{
					String key = iterator.next();
					Object value = null;
					try
					{
						value = ServoyJSONObject.jsonNullToNull(json.get(key));
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
					if (value != null)
					{
						value = defaultToRhino(value, pd, webObjectContext, startScriptable);
					}
					ScriptRuntime.setObjectElem(newObject, key, value, cx);
				}
				initializing[0] = false;
				return newObject;
			}
			finally
			{
				Context.exit();
			}
		}
		if (webComponentValue instanceof JSONArray)
		{
			JSONArray array = (JSONArray)webComponentValue;
			Context cx = Context.enter();
			NativeArray newObject = null;
			try
			{
				final boolean[] initializing = new boolean[] { true };
				newObject = new NativeArray(MAX_NATIVE_ARRAY_LENGTH)
				{
					private final JSONConverter converter = new JSONConverter();

					@Override
					public void put(int index, Scriptable start, Object value)
					{
						super.put(index, start, value);
						if (!initializing[0])
						{
							try
							{
								value = converter.convertToJSONValue(value);
							}
							catch (Exception e)
							{
								Debug.error(e);
							}
							if (!Utils.equalObjects(array.opt(index), value))
							{
								array.put(index, value);
								if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
							}
						}
					}

					@Override
					public void delete(int index)
					{
						super.delete(index);
						array.remove(index);
						if (webObjectContext != null) webObjectContext.getUnderlyingWebObject().markPropertyAsChangedByRef(pd.getName());
					}
				};

				ScriptableObject.putProperty(newObject, "length", array.length());
				ScriptRuntime.setBuiltinProtoAndParent(newObject, startScriptable,
					TopLevel.Builtins.Array);
				for (int i = 0; i < array.length(); i++)
				{
					Object value = null;
					try
					{
						value = array.get(i);
					}
					catch (JSONException e)
					{
						Debug.error(e);
					}
					if (value != null)
					{
						value = defaultToRhino(value, pd, webObjectContext, startScriptable);
					}
					ScriptRuntime.setObjectIndex(newObject, i, value, cx);
				}
				initializing[0] = false;
				return newObject;
			}
			finally
			{
				Context.exit();
			}

		}
		return webComponentValue;
	}

}
