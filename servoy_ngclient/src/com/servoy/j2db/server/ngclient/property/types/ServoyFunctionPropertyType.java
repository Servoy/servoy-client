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

import java.util.Iterator;

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
@SuppressWarnings("nls")
public class ServoyFunctionPropertyType extends FunctionPropertyType
	implements IConvertedPropertyType<Object>, IFormElementToTemplateJSON<Object, Object>, IRhinoDesignConverter, IPropertyWithClientSideConversions<Object>
{
	private static final String FORMNAME = "formname";
	private static final String SCRIPT = "script";

	public static final ServoyFunctionPropertyType SERVOY_INSTANCE = new ServoyFunctionPropertyType();

	private ServoyFunctionPropertyType()
	{
	}

	@Override
	public Object fromJSON(Object newValue, Object previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof JSONObject jo && (jo.has(SCRIPT) || jo.has(FunctionRefType.FUNCTION_HASH)))
		{
			// this is a jsonobject that is send by us, the script should be an encrypted string.
			return newValue;
		}
		// function property type should not be able to send anything to the server.
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object object, PropertyDescription pd, IBrowserConverterContext dataConverterContext)
		throws JSONException
	{
		return toJSON(writer, key, object,
			dataConverterContext.getWebObject() instanceof IContextProvider
				? ((IContextProvider)dataConverterContext.getWebObject()).getDataConverterContext().getApplication().getFlattenedSolution() : null,
			dataConverterContext.getWebObject() instanceof WebFormComponent ? ((WebFormComponent)dataConverterContext.getWebObject()).getFormElement() : null,
			dataConverterContext.getWebObject() instanceof WebFormComponent ? (WebFormComponent)dataConverterContext.getWebObject() : null,
			dataConverterContext);
	}

	private JSONWriter toJSON(JSONWriter writer, String key, Object object, FlattenedSolution fs, FormElement fe,
		WebFormComponent formComponent, IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (object != null && fs != null)
		{
			String[] components = object.toString().split("-"); //$NON-NLS-1$
			if (components.length == 5)
			{
				// this is a uuid
				ScriptMethod sm = fs.getScriptMethod(object.toString());
				if (sm != null)
				{
					object = sm;
				}
				else Debug.log("can't find a scriptmethod for: " + object);
			}
		}
		try
		{
			if (object instanceof String s)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.object();
				addScriptToMap(s, writer, fs);
				writer.endObject();
			}
			else if (object instanceof ScriptMethod sm)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.object();
				addScriptMethodToMap(sm, writer, fs, fe, formComponent);
				writer.endObject();
			}
			else if (object instanceof NativeFunction nf)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.object();
				nativeFunctionToJSON(nf, writer, fs, dataConverterContext);
				writer.endObject();
			}
			else if (object instanceof FunctionWrapper fw && fw.getWrappedFunction() instanceof NativeFunction nf)
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.object();
				nativeFunctionToJSON(nf, writer, fs, dataConverterContext);
				writer.endObject();
			}
			else if (object instanceof JSONObject jo && (jo.has(SCRIPT) || jo.has(FunctionRefType.FUNCTION_HASH)))
			{
				JSONUtils.addKeyIfPresent(writer, key);
				writer.object();
				if (jo.has(SCRIPT))
				{
					writer.key(SCRIPT).value(jo.getString(SCRIPT));
				}
				if (jo.has(FORMNAME))
				{
					writer.key(FORMNAME).value(jo.getString(FORMNAME));
				}
				if (jo.has(FunctionRefType.FUNCTION_HASH))
				{
					writer.key(FunctionRefType.FUNCTION_HASH).value(jo.getString(FunctionRefType.FUNCTION_HASH));
				}
				writer.endObject();
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		return writer;
	}

	/**
	 * @param sm
	 * @param map
	 * @param fs
	 * @param formComponent
	 * @param fe
	 * @throws Exception
	 */
	private void addScriptMethodToMap(ScriptMethod sm, JSONWriter writer, FlattenedSolution fs, FormElement fe, WebFormComponent formComponent)
		throws Exception
	{
		ISupportChilds parent = sm.getParent();
		if (parent instanceof Solution)
		{
			writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt("scopes." + sm.getScopeName() + "." + sm.getName() + "()"));
		}
		else if (parent instanceof Form)
		{
			String formName = ((Form)parent).getName() + "." + sm.getName();
			if (formComponent != null)
			{
				// use the real, runtime form
				formName = formComponent.getDataAdapterList().getForm().getForm().getName();
			}
			writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt("forms." + formName + "." + sm.getName() + "()"));
			writer.key(FORMNAME).value(formName);
		}
		else if (parent instanceof TableNode && fe != null)
		{
			writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt("entity." + fe.getForm().getName() + "." + sm.getName() + "()"));
			writer.key(FORMNAME).value(fe.getForm().getName());
		}
	}

	private void nativeFunctionToJSON(NativeFunction function, JSONWriter writer, FlattenedSolution fs, IBrowserConverterContext dataConverterContext)
		throws Exception
	{
		Object functionName = function.get("_methodname_", function);
		functionName = functionName.equals(Scriptable.NOT_FOUND) ? null : functionName;

		if (functionName == null)
		{
			// if function name is null then just save it as a FunctionRef
			writer.key(FunctionRefType.FUNCTION_HASH).value(FunctionRefType.INSTANCE.addReference(function, dataConverterContext));
		}

		Scriptable parentScope = function.getParentScope();
		while (parentScope != null && !(parentScope instanceof ScriptVariableScope))
		{
			parentScope = parentScope.getParentScope();
		}
		if (parentScope instanceof FormScope && ((FormScope)parentScope).getFormController() != null)
		{
			String formName = ((FormScope)parentScope).getFormController().getName();
			writer.key(FORMNAME).value(formName);
			if (functionName != null)
			{
				writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt("forms." + formName + "." + functionName + "()"));
			}
		}
		else if (parentScope instanceof GlobalScope && functionName != null)
		{
			writer.key(SCRIPT).value(
				fs.getEncryptionHandler().encrypt("scopes." + ((GlobalScope)parentScope).getScopeName() + "." + functionName + "()"));
		}
	}

	private void addScriptToMap(String script, JSONWriter writer, FlattenedSolution fs) throws Exception
	{
		Debug.warn("Function property is given a string: " + script + ", this is should not happen");
		boolean generated = false;
		if (script.startsWith(ScriptVariable.SCOPES_DOT_PREFIX) || script.startsWith(ScriptVariable.GLOBALS_DOT_PREFIX))
		{
			// scope method
			if (fs.getScriptMethod(script) != null)
			{
				writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt(script + "()"));
				generated = true;
			}
		}
		else if (script.startsWith("entity."))
		{
			String formName = script.substring(7, script.indexOf('.', 7));
			Form form = fs.getForm(formName);
			if (form != null)
			{
				String methodName = script.substring(formName.length() + 8);
				Iterator<TableNode> tableNodes = fs.getTableNodes(form.getDataSource());
				while (tableNodes.hasNext())
				{
					TableNode tableNode = tableNodes.next();
					if (tableNode.getFoundsetMethod(methodName) != null)
					{
						writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt(script + "()"));
						writer.key(FORMNAME).value(formName);
						generated = true;
						break;
					}
				}
			}
		}
		else if (script.contains("."))
		{
			// form method: formname.formmethod
			String formName = script.substring(0, script.indexOf('.'));
			Form form = fs.getForm(formName);
			if (form != null && form.getScriptMethod(script.substring(formName.length() + 1)) != null)
			{
				writer.key(SCRIPT).value(fs.getEncryptionHandler().encrypt("forms." + script + "()"));
				writer.key(FORMNAME).value(formName);
				generated = true;
			}
		}
		else
		{
			Debug.log("Can't create a function callback for " + script);
		}
		if (!generated)
		{
			Debug.error("Can't create a function callback for " + script + " because it didn't match on any function");
		}
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd, FormElementContext formElementContext)
		throws JSONException
	{
		if (formElementValue == null) return writer;

		return toJSON(writer, key, formElementValue, formElementContext != null ? formElementContext.getFlattenedSolution() : null,
			formElementContext != null ? formElementContext.getFormElement() : null, null, null);
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
