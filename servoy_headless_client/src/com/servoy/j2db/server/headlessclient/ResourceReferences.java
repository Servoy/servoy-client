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

package com.servoy.j2db.server.headlessclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ResourceReference;

import com.servoy.j2db.util.Pair;

/**
 * Class that holds a sequence of JS/CSS resources and is able to provide them in the order in which they were added.
 * 
 * @author acostescu
 */
public class ResourceReferences implements IProvideGlobalResources
{

	public static final Byte JS = Byte.valueOf((byte)0);
	public static final Byte CSS = Byte.valueOf((byte)1);

	protected List<Pair<Byte, Object>> resources = new ArrayList<Pair<Byte, Object>>();

	public void addGlobalJSResourceReference(ResourceReference resource)
	{
		if (resource == null) return;
		Pair<Byte, Object> tmp = new Pair<Byte, Object>(JS, resource);
		if (!resources.contains(tmp))
		{
			resources.add(tmp);
		}
	}

	@Override
	public void addGlobalJSResourceReference(String url)
	{
		if (url == null) return;
		Pair<Byte, Object> tmp = new Pair<Byte, Object>(JS, url);
		if (!resources.contains(tmp))
		{
			resources.add(tmp);
		}
	}

	@Override
	public void addGlobalCSSResourceReference(ResourceReference resource)
	{
		if (resource == null) return;
		Pair<Byte, Object> tmp = new Pair<Byte, Object>(CSS, resource);
		if (!resources.contains(tmp))
		{
			resources.add(tmp);
		}
	}

	@Override
	public void addGlobalCSSResourceReference(String url)
	{
		if (url == null) return;
		Pair<Byte, Object> tmp = new Pair<Byte, Object>(CSS, url);
		if (!resources.contains(tmp))
		{
			resources.add(tmp);
		}
	}

	@Override
	public void removeGlobalResourceReference(ResourceReference resource)
	{
		removeGlobalReference(resource);
	}

	protected void removeGlobalReference(Object o)
	{
		if (o != null)
		{
			Iterator<Pair<Byte, Object>> it = resources.iterator();
			while (it.hasNext())
			{
				if (o.equals(it.next().getRight())) it.remove();
			}
		}
	}

	@Override
	public void removeGlobalResourceReference(String url)
	{
		removeGlobalReference(url);
	}

	@Override
	public List<Object> getGlobalJSResources()
	{
		ArrayList<Object> tmp = new ArrayList<Object>();
		for (Pair<Byte, Object> el : resources)
		{
			if (JS.byteValue() == el.getLeft().byteValue()) tmp.add(el.getRight());
		}

		return tmp;
	}

	@Override
	public List<Object> getGlobalCSSResources()
	{
		ArrayList<Object> tmp = new ArrayList<Object>();
		for (Pair<Byte, Object> el : resources)
		{
			if (CSS.byteValue() == el.getLeft().byteValue()) tmp.add(el.getRight());
		}

		return new ArrayList<Object>(resources);
	}

	public List<Pair<Byte, Object>> getAllResources()
	{
		return resources;
	}

}