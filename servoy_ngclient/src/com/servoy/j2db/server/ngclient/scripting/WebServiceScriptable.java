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

import org.mozilla.javascript.Scriptable;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentApiDefinition;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.component.DesignConversion;

/**
 * A {@link Scriptable} for wrapping a client side service.
 * So that model values can be get and set and api functions can be called.
 *
 * @author jcompagner
 */
public class WebServiceScriptable implements Scriptable
{
	private final INGApplication application;
	private final WebComponentSpecification serviceSpecification;
	private Scriptable prototype;
	private Scriptable parent;

	/**
	 * @param ngClient
	 * @param serviceSpecification
	 */
	public WebServiceScriptable(INGApplication application, WebComponentSpecification serviceSpecification)
	{
		this.application = application;
		this.serviceSpecification = serviceSpecification;
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
		if (apiFunction != null)
		{
			return new WebServiceFunction(application.getWebsocketSession(), apiFunction, serviceSpecification.getName());
		}
		Object value = application.getWebsocketSession().getService(serviceSpecification.getName()).getProperty(name);
		PropertyDescription desc = serviceSpecification.getProperty(name);
		if (desc != null) return DesignConversion.toStringObject(value, desc.getType());
		return value;
	}

	@Override
	public Object get(int index, Scriptable start)
	{
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
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
		WebComponentApiDefinition apiFunction = serviceSpecification.getApiFunction(name);
		// don't allow api to be overwritten.
		if (apiFunction != null) return;
		// TODO conversion should happen from string (color representation) to Color object.
//		DesignConversion.toObject(value, type)
		application.getWebsocketSession().getService(serviceSpecification.getName()).setProperty(name, value, null);
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
		return serviceSpecification.getAllPropertiesNames().toArray();
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
