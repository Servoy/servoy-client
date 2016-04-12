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

package com.servoy.j2db.server.ngclient.scripting;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.debug.Debugger;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * A {@link Scriptable} for wrapping a client side service.
 * So that model values can be get and set and api functions can be called.
 *
 * @author jcompagner
 */
public class WebServiceScriptable implements Scriptable
{
	private static final ConcurrentMap<URI, Pair<Script, Long>> scripts = new ConcurrentHashMap<>();

	private static Script getScript(Context context, URL serverScript) throws URISyntaxException, IOException
	{
		Pair<Script, Long> pair = scripts.get(serverScript.toURI());
		long lastModified = serverScript.openConnection().getLastModified();
		if (pair == null || pair.getRight().longValue() < lastModified)
		{
			String name = "";
			URI uri = serverScript.toURI();
			if ("file".equals(uri.getScheme()))
			{
				File file = new File(uri);
				if (file.exists())
				{
					name = file.getAbsolutePath();
				}
			}
			Debugger debugger = null;
			int lvl = context.getOptimizationLevel();
			Script script;
			try
			{
				if ("".endsWith(name))
				{
					context.setGeneratingDebug(false);
					debugger = context.getDebugger();
					if (debugger != null)
					{

						context.setOptimizationLevel(9);
						context.setDebugger(null, null);
					}
				}
				context.setGeneratingSource(false);
				script = context.compileString(Utils.getURLContent(serverScript), name, 1, null);
			}
			finally
			{
				if (debugger != null)
				{
					context.setDebugger(debugger, null);
					context.setOptimizationLevel(lvl);
				}
			}
			pair = new Pair<Script, Long>(script, Long.valueOf(lastModified));
			scripts.put(uri, pair);
		}
		return pair.getLeft();
	}

	/**
	 * Compiles the server side script, enabled debugging if possible.
	 * It returns the $scope object
	 *
	 * @param serverScript
	 * @param app
	 */
	public static Scriptable compileServerScript(URL serverScript, Scriptable model, IApplication app)
	{
		Scriptable scopeObject = null;
		Context context = Context.enter();
		try
		{
			Scriptable topLevel = ScriptableObject.getTopLevelScope(model);
			if (topLevel == null)
			{
				// This should not really happen anymore.
				Debug.log("toplevel object not found for creating serverside script: " + serverScript);
				topLevel = context.initStandardObjects();
			}
			Scriptable apiObject = null;
			Scriptable execScope = context.newObject(topLevel);
			execScope.setParentScope(topLevel);
			scopeObject = context.newObject(execScope);
			apiObject = context.newObject(execScope);
			scopeObject.put("api", scopeObject, apiObject);
			scopeObject.put("model", scopeObject, model);
			execScope.put("$scope", execScope, scopeObject);
			getScript(context, serverScript).exec(context, execScope);
			apiObject.setPrototype(model);

			execScope.put("console", execScope,
				new NativeJavaObject(execScope, new ConsoleObject(app), new InstanceJavaMembers(execScope, ConsoleObject.class)));
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		finally
		{
			Context.exit();
		}
		return scopeObject;
	}

	private final INGApplication application;
	private final WebObjectSpecification serviceSpecification;
	private Scriptable prototype;
	private Scriptable parent;
	private Scriptable apiObject;
	private Scriptable scopeObject;

	/**
	 * @param ngClient
	 * @param serviceSpecification
	 * @param solutionScope
	 */
	public WebServiceScriptable(INGApplication application, WebObjectSpecification serviceSpecification, SolutionScope solutionScope)
	{
		this.application = application;
		setParentScope(solutionScope);
		this.serviceSpecification = serviceSpecification;
		URL serverScript = serviceSpecification.getServerScript();
		if (serverScript != null)
		{
			scopeObject = compileServerScript(serverScript, this, application);
			apiObject = (Scriptable)scopeObject.get("api", scopeObject);
		}
	}

	public Object executeScopeFunction(String function, JSONArray args)
	{
		Object object = scopeObject.get(function, scopeObject);
		if (object instanceof Function)
		{
			Context context = Context.enter();
			try
			{
				Object[] array = new Object[args.length()];
				for (int i = 0; i < args.length(); i++)
				{
					array[i] = args.get(i);
				}
				Object retValue = ((Function)object).call(context, scopeObject, scopeObject, array);
				return retValue == Undefined.instance ? null : retValue;
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				return null;
			}
			finally
			{
				Context.exit();
			}
		}
		else
		{
			throw new RuntimeException(
				"trying to call a function '" + function + "' that does not exists on a the service with spec: " + serviceSpecification.getName());
		}
	}

	@Override
	public String getClassName()
	{
		return "WebServiceScriptable";
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		WebObjectFunctionDefinition apiFunction = serviceSpecification.getApiFunction(name);
		if (apiFunction != null && apiObject != null)
		{
			Object serverSideFunction = apiObject.get(apiFunction.getName(), apiObject);
			if (serverSideFunction instanceof Function)
			{
				return serverSideFunction;
			}
		}
		if (apiFunction != null)
		{
			return new WebServiceFunction(application.getWebsocketSession(), apiFunction, serviceSpecification.getName());
		}
		BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getClientService(serviceSpecification.getName());
		Object value = service.getProperty(name);
		PropertyDescription desc = serviceSpecification.getProperty(name);
		if (desc != null)
		{
			if (desc.getType() instanceof ISabloComponentToRhino< ? >)
			{
				return NGConversions.INSTANCE.convertSabloComponentToRhinoValue(value, desc, service, start);
			}
			else
			{
				return value; // types that don't implement the sablo <-> rhino conversions are by default available and their value is accessible directly
			}
		}
		else return getParentScope().get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		PropertyDescription desc = serviceSpecification.getProperty(name);
		if (desc != null)
		{
			BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getClientService(serviceSpecification.getName());
			IPropertyType< ? > type = desc.getType();
			// it is available by default, so if it doesn't have conversion, or if it has conversion and is explicitly available
			return !(type instanceof ISabloComponentToRhino< ? >) ||
				((ISabloComponentToRhino)type).isValueAvailableInRhino(service.getProperty(name), desc, service);
		}
		WebObjectFunctionDefinition apiFunction = serviceSpecification.getApiFunction(name);
		if (apiFunction != null) return true;
		return application.getWebsocketSession().getClientService(serviceSpecification.getName()).getProperties().content.containsKey(name);
	}

	@Override
	public boolean has(int index, Scriptable start)
	{
		return false;
	}

	@Override
	public void put(String name, Scriptable start, Object value)
	{
		PropertyDescription desc = serviceSpecification.getProperty(name);
		BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getClientService(serviceSpecification.getName());
		if (desc != null)
		{
			Object previousVal = service.getProperty(name);
			Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, desc, service);

			if (val != previousVal) service.setProperty(name, val);
		}
		else
		{
			WebObjectFunctionDefinition apiFunction = serviceSpecification.getApiFunction(name);
			// don't allow api to be overwritten.
			if (apiFunction != null) return;
			// TODO conversion should happen from string (color representation) to Color object.
//		DesignConversion.toObject(value, type)
			service.setProperty(name, value);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value)
	{
	}

	@Override
	public void delete(String name)
	{
	}

	@Override
	public void delete(int index)
	{
	}

	@Override
	public Scriptable getPrototype()
	{
		return prototype;
	}

	@Override
	public void setPrototype(Scriptable prototype)
	{
		this.prototype = prototype;
	}

	@Override
	public Scriptable getParentScope()
	{
		return parent;
	}

	@Override
	public void setParentScope(Scriptable parent)
	{
		this.parent = parent;
	}

	@Override
	public Object[] getIds()
	{
		ArrayList<String> al = new ArrayList<>();
		BaseWebObject service = null;
		if (application != null)
		{
			service = (BaseWebObject)application.getWebsocketSession().getClientService(serviceSpecification.getName());
		}
		for (String name : serviceSpecification.getAllPropertiesNames())
		{
			PropertyDescription pd = serviceSpecification.getProperty(name);
			IPropertyType< ? > type = pd.getType();
			if (service == null || !(type instanceof ISabloComponentToRhino< ? >) ||
				((ISabloComponentToRhino)type).isValueAvailableInRhino(service.getProperty(name), pd, service))
			{
				al.add(name);
			}
		}
		al.addAll(serviceSpecification.getApiFunctions().keySet());
		return al.toArray();
	}

	@Override
	public Object getDefaultValue(Class< ? > hint)
	{
		return serviceSpecification.toString();
	}

	@Override
	public boolean hasInstance(Scriptable instance)
	{
		return false;
	}

}
