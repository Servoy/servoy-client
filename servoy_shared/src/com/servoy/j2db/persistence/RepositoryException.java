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
package com.servoy.j2db.persistence;

import java.rmi.RemoteException;

import com.servoy.j2db.util.ServoyException;


/**
 * Wrapper Exception
 * 
 * @author jblok
 */
public class RepositoryException extends ServoyException
{
	public RepositoryException(int errorCode)
	{
		super(errorCode);
	}

	public RepositoryException(String msg)
	{
		super(ServoyException.InternalCodes.CUSTOM_REPOSITORY_ERROR, new Object[] { msg });
	}

	public RepositoryException(int errorCode, Object[] values)
	{
		super(errorCode, values);
	}

	public RepositoryException(int errorCode, Exception ex)
	{
		super(errorCode);
		initCause(ex);
	}

	public RepositoryException(RemoteException ex)
	{
		super(ServoyException.InternalCodes.INVALID_RMI_SERVER_CONNECTION);
		initCause(ex);
	}

	public RepositoryException(Exception cause)
	{
		this(cause.toString());
		initCause(cause);
	}

	public RepositoryException(String msg, Exception ex)
	{
		this(msg);
		initCause(ex);
	}
}
