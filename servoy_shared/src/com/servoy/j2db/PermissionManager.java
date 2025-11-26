/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.j2db;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Scriptable;

import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.scripting.DefaultScope;
import com.servoy.j2db.scripting.info.JSPermission;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * @author lvostinar
 *
 */
public class PermissionManager extends DefaultScope implements IPermissionManager
{
	private final IApplication application;

	public PermissionManager(IApplication application, Scriptable scope)
	{
		super(scope);
		this.application = application;
	}

	@Override
	public Object get(String name, Scriptable start)
	{
		if (IRepository.ADMIN_GROUP.equals(name))
		{
			return JSPermission.Administrators;
		}
		try
		{
			return application.getUserManager().getGroups(application.getClientID()).getRows().stream().filter(row -> row[1].equals(name)).findFirst()
				.map(row -> new JSPermission((String)row[1]))
				.orElse(null);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

	@Override
	public boolean has(String name, Scriptable start)
	{
		if (IRepository.ADMIN_GROUP.equals(name))
		{
			return true;
		}
		try
		{
			return application.getUserManager().getGroupId(application.getClientID(), name) != -1;
		}
		catch (RemoteException | ServoyException e)
		{
			Debug.error(e);
		}
		return false;
	}

	@Override
	public Object[] getIds()
	{
		List<String> names = new ArrayList<String>();
		try
		{
			application.getUserManager().getGroups(application.getClientID()).getRows().stream().map(row -> (String)row[1]).forEach(names::add);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		if (!names.contains(IRepository.ADMIN_GROUP))
		{
			names.add(IRepository.ADMIN_GROUP);
		}
		return names.toArray();
	}
}