/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import com.servoy.j2db.server.shared.IServiceRegistry;

/**
 * Provides service registry in non OSGI env  
 * @author jblok
 */
public class LocalRegistry implements IServiceRegistry
{
	private final Map<String, Object> map = new HashMap<String, Object>();

	@Override
	public <S> S getService(Class<S> reference)
	{
		return (S)map.get(reference.getName());
	}

	@Override
	public <S> void registerService(Class<S> clazz, S service)
	{
		map.put(clazz.getName(), service);
	}

	@Override
	public <S> void ungetService(Class<S> clazz)
	{
		map.remove(clazz.getName());
	}

	@Override
	public <S> S getService(Class<S> clazz, String filter)
	{
		return null;
	}

	@Override
	public <S> void registerService(Class<S> clazz, S service, Dictionary properties)
	{
		map.put(clazz.getName(), service);
	}

	@Override
	public Object getService(String reference)
	{
		return map.get(reference);
	}

	@Override
	public void registerService(String reference, Object service)
	{
		map.put(reference, service);
	}
}
