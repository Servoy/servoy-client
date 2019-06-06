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
package com.servoy.j2db;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;

/**
 * Interface that gives the general solution security functionality (allows the solution to use the user/security functionality without altering it).
 */
public interface ISolutionSecurityManager
{
	/**
	 * @return user_uid
	 */
	public String checkPasswordForUserName(String clientId, String username, String password) throws RemoteException, ServoyException;

	public boolean checkPasswordForUserUID(String clientId, String userUID, String password) throws RemoteException, ServoyException;

	public String[] getUserGroups(String clientId, String userUID) throws RemoteException, ServoyException;

	/**
	 * Map keys are either UUIDs (for forms or elements) or CharSequence (for tables/columns)
	 * TODO: separate so that proper generics can be used.
	 * @param solution_ids
	 * @param releaseNumbers
	 * @param groups
	 * @return
	 * @throws RepositoryException
	 * @throws RemoteException
	 */
	public Pair<Map<Object, Integer>, Set<Object>> getSecurityAccess(String clientId, int[] solution_id, int[] releaseNumber, String[] groups)
		throws RemoteException, ServoyException;
}
