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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.component.DesignConversion;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * A {@link Scriptable} for wrapping a client side service.
 * So that model values can be get and set and api functions can be called.
 *
 * @author jcompagner
 */
public class WebServiceScriptable implements Scriptable
{
	/**
	 * Compiles the server side script, enabled debugging if possible.
	 *
	 * @param serverScript
	 */
	public static Scriptable compileServerScript(URL serverScript, Scriptable model)
	{
		Scriptable apiObject = null;
		Context context = Context.enter();
		try
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
			if ("".endsWith(name))
			{
				context.setGeneratingDebug(false);
				if (context.getDebugger() != null)
				{
					context.setOptimizationLevel(9);
					context.setDebugger(null, null);
				}
			}
			context.setGeneratingSource(false);
			Script script = context.compileString(Utils.getURLContent(serverScript), name, 1, null);
			//TODO can this be done if the context did already exisit (so this call comes from inside a scripting call)
			// should we just take the toplevel scope of the scriptengine?
			ScriptableObject topLevel = context.initStandardObjects();
			Scriptable scopeObject = context.newObject(topLevel);
			apiObject = context.newObject(topLevel);
			scopeObject.put("api", scopeObject, apiObject);
			scopeObject.put("model", scopeObject, model);
			topLevel.put("$scope", topLevel, scopeObject);
			script.exec(context, topLevel);
			apiObject.setPrototype(model);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
		finally
		{
			Context.exit();
		}
		return apiObject;
	}

	private final INGApplication application;
	private final WebComponentSpecification serviceSpecification;
	private Scriptable prototype;
	private Scriptable parent;
	private Scriptable apiObject;

	/**
	 * @param ngClient
	 * @param serviceSpecification
	 * @param solutionScope
	 */
	public WebServiceScriptable(INGApplication application, WebComponentSpecification serviceSpecification, SolutionScope solutionScope)
	{
		this.application = application;
		setParentScope(solutionScope);
		this.serviceSpecification = serviceSpecification;
		URL serverScript = serviceSpecification.getServerScript();
		if (serverScript != null)
		{
			apiObject = compileServerScript(serverScript, this);
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
		WebComponentApiDefinition apiFunction = serviceSpecification.getApiFunction(name);
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
		BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getService(serviceSpecification.getName());
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
				return DesignConversion.toStringObject(value, desc.getType());
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
			BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getService(serviceSpecification.getName());
			IPropertyType< ? > type = desc.getType();
			// it is available by default, so if it doesn't have conversion, or if it has conversion and is explicitly available
			return !(type instanceof ISabloComponentToRhino< ? >) ||
				((ISabloComponentToRhino)type).isValueAvailableInRhino(service.getProperty(name), desc, service);
		}
		WebComponentApiDefinition apiFunction = serviceSpecification.getApiFunction(name);
		if (apiFunction != null) return true;
		return application.getWebsocketSession().getService(serviceSpecification.getName()).getProperties().content.containsKey(name);
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
		BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getService(serviceSpecification.getName());
		if (desc != null)
		{
			Object previousVal = service.getProperty(name);
			Object val = NGConversions.INSTANCE.convertRhinoToSabloComponentValue(value, previousVal, desc, service);

			if (val != previousVal) service.setProperty(name, val);
		}
		else
		{
		WebComponentApiDefinition apiFunction = serviceSpecification.getApiFunction(name);
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
		BaseWebObject service = (BaseWebObject)application.getWebsocketSession().getService(serviceSpecification.getName());
		for (String name : serviceSpecification.getAllPropertiesNames())
		{
			PropertyDescription pd = serviceSpecification.getProperty(name);
			IPropertyType< ? > type = pd.getType();
			if (!(type instanceof ISabloComponentToRhino< ? >) ||
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
