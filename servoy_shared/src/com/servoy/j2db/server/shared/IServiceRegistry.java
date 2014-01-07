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

package com.servoy.j2db.server.shared;

import java.util.Dictionary;


/**
 * An service registry which is implemented by OSGi or simple local map in non OSGI env
 * @author jblok
 */
public interface IServiceRegistry
{
	public <S> S getService(Class<S> reference);

	public <S> S getService(Class<S> clazz, String filter);

	public <S> void registerService(Class<S> clazz, S service);

	public <S> void registerService(Class<S> clazz, S service, Dictionary properties);

	public <S> void ungetService(Class<S> clazz);
}
