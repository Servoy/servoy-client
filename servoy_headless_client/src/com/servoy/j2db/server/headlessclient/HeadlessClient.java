/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import javax.servlet.ServletRequest;

/**
 * @author rgansevles
 */
public class HeadlessClient extends SessionClient
{
	/**
	 * @param req
	 * @param uname
	 * @param pass
	 * @param method
	 * @param methodArgs
	 * @param solution
	 * @throws Exception
	 */
	protected HeadlessClient(ServletRequest req, String uname, String pass, String method, Object[] methodArgs, String solution) throws Exception
	{
		super(req, uname, pass, method, methodArgs, solution);
	}


	@Override
	public boolean closeSolution(final boolean force, final Object[] args)
	{
		final boolean[] res = new boolean[] { false };
		invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				res[0] = superCloseSolution(force, args);
			}
		});

		return res[0];
	}

	private boolean superCloseSolution(final boolean force, final Object[] args)
	{
		return super.closeSolution(force, args);
	}
}
