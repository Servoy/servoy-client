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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.IPropertyWithClientSideConversions;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.scripting.FormScope;
import com.servoy.j2db.scripting.FunctionWrapper;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.scripting.ScriptVariableScope;
import com.servoy.j2db.scripting.solutionmodel.IJSParent;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSMethod;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IRhinoDesignConverter;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class ServoyFunctionPropertyType extends FunctionPropertyType
	implements IConvertedPropertyType<Object>, IFormElementToTemplateJSON<Object, Object>, IRhinoDesignConverter, IPropertyWithClientSideConversions<Object>
{
	public static final ServoyFunctionPropertyType SERVOY_INSTANCE = new ServoyFunctionPropertyType();

	private ServoyFunctionPropertyType()
	{
	}

	@Override
	public Object fromJSON(Object newValue, Object previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof JSONObject)
		{
			Map<String, Object> value = new HashMap<String, Object>();
			Iterator<String> it = ((JSONObject)newValue).keys();
			while (it.hasNext())
			{
				String key = it.next();
				try
				{
					value.put(key, ((JSONObject)newValue).get(key));
				}
				catch (JSONException ex)
				{
					Debug.error(ex);
				}
			}
			return value;
		}
		return newValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object object, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		return toJSON(writer, key, object, pd,
			dataConverterContext.getWebObject() instanceof IContextProvider
				? ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication().getFlattenedSolution() : null,
			dataConverterContext.getWebObject() instanceof WebFormComponent ? ((WebFormComponent)dataConverterContext.getWebObject()).getFormElement() : null,
			dataConverterContext.getWebObject() instanceof WebFormComponent ? (WebFormComponent)dataConverterContext.getWebObject() : null);
	}

	public JSONWriter toJSON(JSONWriter writer, String key, Object object, PropertyDescription pd, FlattenedSolution fs, FormElement fe,
		WebFormComponent formComponent) throws JSONException
	{
		Map<String, Object> map = new HashMap<>();
		if (object != null && fs != null)
		{
			String[] components = object.toString().split("-"); //$NON-NLS-1$
			if (components.length == 5)
			{
				String scriptString = null;
				// this is a uuid
				ScriptMethod sm = fs.getScriptMethod(object.toString());
				if (sm != null)
				{
					ISupportChilds parent = sm.getParent();
					if (parent instanceof Solution)
					{
						scriptString = "scopes." + sm.getScopeName() + "." + sm.getName();
					}
					else if (parent instanceof Form)
					{
						if (formComponent != null)
						{
							// use the real, runtime form
							scriptString = formComponent.getDataAdapterList().getForm().getForm().getName() + "." + sm.getName();
						}
						else
						{
							scriptString = ((Form)parent).getName() + "." + sm.getName();
						}
					}
					else if (parent instanceof TableNode && fe != null)
					{
						scriptString = "entity." + fe.getForm().getName() + "." + sm.getName();
					}
					object = scriptString;
				}
				else Debug.log("can't find a scriptmethod for: " + object);
			}
		}
		try
		{
			if (object instanceof String)
			{
				addScriptToMap((String)object, map, fs);
			}
			else if (object instanceof NativeFunction)
			{
				nativeFunctionToJSON((NativeFunction)object, map, fs);
			}
			else if (object instanceof FunctionWrapper && ((FunctionWrapper)object).getWrappedFunction() instanceof NativeFunction)
			{
				nativeFunctionToJSON((NativeFunction)((FunctionWrapper)object).getWrappedFunction(), map, fs);
			}
			else if (object instanceof Map)
			{
				map = new HashMap<String, Object>((Map<String, Object>)object);
				if (map.get("script") instanceof String) addScriptToMap((String)map.get("script"), map, fs);
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return JSONUtils.toBrowserJSONFullValue(writer, key, map.size() == 0 ? null : map, null, null);
	}

	private void nativeFunctionToJSON(NativeFunction function, Map<String, Object> map, FlattenedSolution fs) throws Exception
	{
		String functionName = function.getFunctionName();
		Scriptable parentScope = function.getParentScope();
		while (parentScope != null && !(parentScope instanceof ScriptVariableScope))
		{
			parentScope = parentScope.getParentScope();
		}
		if (parentScope instanceof FormScope && ((FormScope)parentScope).getFormController() != null)
		{
			String formName = ((FormScope)parentScope).getFormController().getName();
			map.put("script", fs.getEncryptionHandler().encrypt("forms." + formName + "." + functionName + "()"));
			map.put("formname", formName);
		}
		else if (parentScope instanceof GlobalScope)
		{
			map.put("script",
				fs.getEncryptionHandler().encrypt("scopes." + ((GlobalScope)parentScope).getScopeName() + "." + functionName + "()"));
		}
	}

	private void addScriptToMap(String script, Map<String, Object> map, FlattenedSolution fs) throws Exception
	{
		if (script.startsWith(ScriptVariable.SCOPES_DOT_PREFIX) || script.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			// scope method
			map.put("script", fs.getEncryptionHandler().encrypt(script + "()"));
		}
		else if (script.startsWith("entity."))
		{
			String formName = script.substring(7, script.indexOf('.', 7));
			map.put("script", fs.getEncryptionHandler().encrypt(script + "()"));
			map.put("formname", formName);
		}
		else if (script.contains("."))
		{
			// form method: formname.formmethod
			String formName = script.substring(0, script.indexOf('.'));
			map.put("script", fs.getEncryptionHandler().encrypt("forms." + script + "()"));
			map.put("formname", formName);
		}
		else
		{
			Debug.log("Can't create a function callback for " + script);
		}
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		return toJSON(writer, key, formElementValue, pd, formElementContext != null ? formElementContext.getFlattenedSolution() : null,
			formElementContext != null ? formElementContext.getFormElement() : null, null);
	}

	@Override
	public Object fromRhinoToDesignValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		if (value instanceof JSMethod)
		{
			return ((JSMethod)value).getUUID().toString();
//			return new Integer(JSBaseContainer.getMethodId(application, webComponent.getBaseComponent(false), ((JSMethod)value).getScriptMethod()));
		}
		return value;
	}

	@Override
	public Object fromDesignToRhinoValue(Object value, PropertyDescription pd, IApplication application, JSWebComponent webComponent)
	{
		IJSParent< ? > jsParent = webComponent.getJSParent();
		while (!(jsParent instanceof JSForm) && (jsParent != null))
		{
			jsParent = jsParent.getJSParent();
		}
		int methodId = Utils.getAsInteger(value);
		if (methodId > 0)
		{
			return JSForm.getEventHandler(application, webComponent.getBaseComponent(false), methodId, jsParent, pd.getName());
		}
		else if (value instanceof String)
		{
			// it is a uuid string
			return JSForm.getEventHandler(application, webComponent.getBaseComponent(false), (String)value, jsParent, pd.getName());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.specification.property.IPropertyWithClientSideConversions#writeClientSideTypeName(org.json.JSONWriter, java.lang.String,
	 * org.sablo.specification.PropertyDescription)
	 */
	@Override
	public boolean writeClientSideTypeName(JSONWriter w, String keyToAddTo, PropertyDescription pd)
	{
		JSONUtils.addKeyIfPresent(w, keyToAddTo);
		w.value(TYPE_NAME);
		return true;
	}

}
