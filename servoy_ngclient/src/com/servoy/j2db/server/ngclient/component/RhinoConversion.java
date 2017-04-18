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

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mozilla.javascript.CharSequenceBuffer;
import org.mozilla.javascript.NativeDate;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.IFormController;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.util.Utils;

/**
 * @author emera
 */
public class RhinoConversion
{

	/**
	 * Default conversion used to convert from Rhino property types that do not explicitly implement component <-> Rhino conversions. <BR/><BR/>
	 * Values of types that don't implement the sablo <-> rhino conversions are by default accessible directly.
	 */
	public static Object defaultFromRhino(Object propertyValue, Object oldValue, PropertyDescription pd, IServoyDataConverterContext converterContext)
	{
		// convert simple values to json values
		if (propertyValue == UniqueTag.NOT_FOUND || propertyValue == Undefined.instance)
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
				map.put(id, defaultFromRhino(no.get(id), oldMap != null ? oldMap.get(id) : null, pd != null ? pd.getProperty(id) : null, converterContext));
			}
			return map;
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
		if (propertyValue instanceof CharSequenceBuffer)
		{
			return propertyValue.toString();
		}
		return propertyValue;
	}

	/**
	 * Default conversion used to convert to Rhino property types that do not explicitly implement component <-> Rhino conversions. <BR/><BR/>
	 * Types that don't implement the sablo <-> rhino conversions are by default available and their value is accessible directly.
	 */
	public static Object defaultToRhino(final Object webComponentValue, final PropertyDescription pd, final BaseWebObject componentOrService,
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

		if (webComponentValue instanceof Map && !(webComponentValue instanceof NativeObject))
		{
			NativeObject nativeObject = new NativeObject()
			{
				@Override
				public void put(String name, Scriptable start, Object value)
				{
					super.put(name, start, value);
					if (!Utils.equalObjects(((Map)webComponentValue).get(name), value))
					{
						((Map)webComponentValue).put(name, value);
						if (componentOrService != null) componentOrService.flagPropertyAsDirty(pd.getName(), true);
					}
				}

				@Override
				public void delete(String name)
				{
					super.delete(name);
					((Map)webComponentValue).remove(name);
					if (componentOrService != null) componentOrService.flagPropertyAsDirty(pd.getName(), true);
				}
			};
			for (Object key : ((Map)webComponentValue).keySet())
			{
				nativeObject.defineProperty(key.toString(), ((Map)webComponentValue).get(key), ScriptableObject.EMPTY);
			}
			return nativeObject;
		}

		return webComponentValue;
	}

}
