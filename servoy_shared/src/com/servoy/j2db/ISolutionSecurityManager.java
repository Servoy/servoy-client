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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.UUID;

/**
 * Interface that gives the general solution security functionality (allows the solution to use the user/security functionality without altering it).
 */
public interface ISolutionSecurityManager extends Remote
{
	/**
	 * @return user_uid
	 */
	public String checkPasswordForUserName(String clientId, String username, String password) throws RemoteException, ServoyException;

	public boolean checkPasswordForUserUID(String clientId, String userUID, String password) throws RemoteException, ServoyException;

	public String[] getUserGroups(String clientId, String userUID) throws RemoteException, ServoyException;

	/**
	 * Map keys are either UUIDs (for forms or elements) or CharSequence (for tables/columns).<br/>
	 * The set represents the ones that have implicit rights. UUIDs for form elements and Strings for table columns.
	 *
	 * @deprecated use {@link #getSecurityAccessForTablesAndForms(String, UUID[], int[], String[])} instead; in order to work correctly with
	 *              form component component children, now, the key given for forms and elements changed
	 *              to a String in the new method. UUIDs given by this method were not enough to uniquely identify those.
	 *              A different method was created although this signature with "Object" could be reused in order to not run into runtime
	 *              exceptions or bugs where old code expects UUIDs.
	 * @throws RemoteException
	 */
	@Deprecated
	public Pair<Map<Object, Integer>, Set<Object>> getSecurityAccess(String clientId, UUID[] solution_uuids, int[] releaseNumbers, String[] groups)
		throws RemoteException, ServoyException;

	/**
	 * The returned tableSecurityAccessInfo identifier keys are PersistIdentifier.toJSONString Strings for forms or elements.<br/>
	 * The returned formSecurityAccessInfo identifier keys are Strings identifying the table column.<br/>
	 */
	public TableAndFormSecurityAccessInfo getSecurityAccessForTablesAndForms(String clientId, UUID[] solution_uuids, int[] releaseNumbers,
		String[] groups)
		throws RemoteException, ServoyException;

	public static record TableAndFormSecurityAccessInfo(SecurityAccessInfo tableSecurityAccessInfo, SecurityAccessInfo formSecurityAccessInfo)
	{
	}
	public static record SecurityAccessInfo(Map<String, Integer> explicitIdentifierToAccessMap, Set<String> implicitAccessIdentifiers)
	{
	}

}
