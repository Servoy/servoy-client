/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetTreeTypeSabloValue.FoundsetTreeBinding;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IRhinoToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;

/**
 * @author lvostinar
 *
 */
public class FoundsetTreePropertyType extends DefaultPropertyType<FoundsetTreeTypeSabloValue>
	implements IFormElementToSabloComponent<Object, FoundsetTreeTypeSabloValue>, IPropertyConverterForBrowser<FoundsetTreeTypeSabloValue>,
	IRhinoToSabloComponent<FoundsetTreeTypeSabloValue>,
	ISabloComponentToRhino<FoundsetTreeTypeSabloValue>, IPropertyWithClientSideConversions<FoundsetTreeTypeSabloValue>
{
	public static final FoundsetTreePropertyType INSTANCE = new FoundsetTreePropertyType();

	public static final String TYPE_NAME = "foundsettree";

	private FoundsetTreePropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public FoundsetTreeTypeSabloValue fromJSON(Object newJSONValue, FoundsetTreeTypeSabloValue previousSabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext context, ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (previousSabloValue != null && (newJSONValue instanceof JSONObject))
		{
			// currently the only thing that can come from client is a filter request...
			previousSabloValue.fromJSON((JSONObject)newJSONValue);
		}
		else Debug.error("Got a client update for foundset tree property, but foundset tree is null or value can't be interpreted: " + newJSONValue + ".");
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, FoundsetTreeTypeSabloValue sabloValue, PropertyDescription propertyDescription,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		sabloValue.toJSON(writer, key, dataConverterContext);
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(FoundsetTreeTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext)
	{
		return webComponentValue != null;
	}

	@Override
	public Object toRhinoValue(FoundsetTreeTypeSabloValue webComponentValue, PropertyDescription pd, IWebObjectContext webObjectContext,
		Scriptable startScriptable)
	{
		if (webComponentValue == null) return null;
		return new DefaultScope(startScriptable)
		{
			private static final String ROOTS = "roots";

			@Override
			public Object get(String name, Scriptable start)
			{
				if (webComponentValue != null)
				{
					if (ROOTS.equals(name))
					{
						webComponentValue.flagChanged();
						return webComponentValue.roots;
					}
					if ("removeAllRoots".equals(name))
					{
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								webComponentValue.removeAllRoots();
								return null;
							}
						};
					}
					if ("getBinding".equals(name))
					{
						webComponentValue.flagChanged();
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								String datasource = (String)args[0];
								FoundsetTreeBinding binding = webComponentValue.bindings.get(datasource);
								if (binding == null)
								{
									binding = new FoundsetTreeBinding();
									webComponentValue.bindings.put(datasource, binding);
								}
								return binding;
							}
						};
					}
					if ("getNodeIDArray".equals(name))
					{
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								return webComponentValue.getNodeIDArray(((List)args[0]).toArray());
							}
						};
					}
					if ("getCheckedPks".equals(name))
					{
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								return webComponentValue.getCheckedPks((String)args[0]);
							}
						};
					}
					if ("updateCheckBoxValues".equals(name))
					{
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								if (args != null && args.length == 3 && args[0] instanceof String && args[1] instanceof NativeArray &&
									args[2] instanceof Boolean)
								{
									webComponentValue.updateCheckBoxValues((String)args[0], ((NativeArray)args[1]).toArray(), (Boolean)args[2]);
								}
								return null;
							}
						};
					}
					if ("selection".equals(name))
					{
						return webComponentValue.getSelectionPath();
					}
					if ("setLevelVisibility".equals(name))
					{
						return new Callable()
						{
							@Override
							public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args)
							{
								if (args != null && args.length == 2 && args[0] instanceof Number && args[1] instanceof Boolean)
								{
									webComponentValue.setLevelVisibility((Number)args[0], (Boolean)args[1]);
								}
								return null;
							}
						};
					}
				}
				return super.get(name, start);
			}

			@Override
			public void put(String name, Scriptable start, Object value)
			{
				if ("selection".equals(name))
				{
					webComponentValue.setSelectionPath(((List)value).toArray());
				}
				super.put(name, start, value);
			}
		};
	}

	@Override
	public FoundsetTreeTypeSabloValue toSabloComponentValue(Object rhinoValue, FoundsetTreeTypeSabloValue previousComponentValue, PropertyDescription pd,
		IWebObjectContext webObjectContext)
	{
		return new FoundsetTreeTypeSabloValue();
	}

	@Override
	public FoundsetTreeTypeSabloValue toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement,
		WebFormComponent component, DataAdapterList dataAdapterList)
	{
		return new FoundsetTreeTypeSabloValue();
	}

	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);

		w.value(TYPE_NAME);
		return true;
	}
}
