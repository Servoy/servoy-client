/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.servoy.j2db.ISessionClient;

import jakarta.servlet.ServletRequest;

/**
 * Factory for headless clients
 * @author jblok
 */
public class HeadlessClientFactory
{
	public static ISessionBean createSessionBean(ServletRequest req, String solutionname) throws Exception
	{
		return createSessionBean(req, solutionname, null, null, null);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, String username, String password) throws Exception
	{
		return createSessionBean(req, solutionname, username, password, null);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, Object[] solutionOpenMethodArgs) throws Exception
	{
		return createSessionBean(req, solutionname, null, null, solutionOpenMethodArgs);
	}

	public static ISessionBean createSessionBean(ServletRequest req, String solutionname, String username, String password, Object[] solutionOpenMethodArgs)
		throws Exception
	{
		final ISessionClient sb = HeadlessClientFactoryInternal.createSessionBean(req, solutionname, username, password, solutionOpenMethodArgs);
		return (ISessionBean)Proxy.newProxyInstance(HeadlessClientFactory.class.getClassLoader(), new Class[] { ISessionBean.class }, new InvocationHandler()
		{
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{
				return method.invoke(sb, args);
			}
		});
	}

	public static IHeadlessClient createHeadlessClient(String solutionname) throws Exception
	{
		return createHeadlessClient(solutionname, null, null, null);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password) throws Exception
	{
		return createHeadlessClient(solutionname, username, password, null);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, Object[] solutionOpenMethodArgs) throws Exception
	{
		return createHeadlessClient(solutionname, null, null, solutionOpenMethodArgs);
	}

	public static IHeadlessClient createHeadlessClient(String solutionname, String username, String password, Object[] solutionOpenMethodArgs) throws Exception
	{
		final ISessionClient sb = HeadlessClientFactoryInternal.createHeadlessClient(solutionname, username, password, solutionOpenMethodArgs);
		return (IHeadlessClient)Proxy.newProxyInstance(HeadlessClientFactory.class.getClassLoader(),
			new Class[] { IHeadlessClient.class, ISessionClient.class }, new InvocationHandler()
			{
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
				{
					return method.invoke(sb, args);
				}
			});
	}
}
