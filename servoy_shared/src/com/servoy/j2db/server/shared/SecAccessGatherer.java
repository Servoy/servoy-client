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

package com.servoy.j2db.server.shared;

/**
 * Just a helper class to allow us to add either to one map & set (old getSecurityAccess) or to two (new getSecurityAccessForTablesAndForms).
 */
public interface SecAccessGatherer<FormAndElementIdentifierT>
{

	/**
	 * Should conceptually do an equivalent to explicitMap.put(UUID, Integer.valueOf(Utils.getAsInteger(explicitMap.get(UUID)) | element_access))
	 */
	void explicitFormElementAccessFoundForMerge(FormAndElementIdentifierT identifier, int element_access);

	/**
	 * Just add it to the form element implicit map.
	 */
	void implicitFormElementAccess(FormAndElementIdentifierT identifier);

	/**
	 * Should conceptually do an equivalent to explicitMap.put(UUID, Integer.valueOf(Utils.getAsInteger(explicitMap.get(UUID)) | element_access))
	 */
	void explicitColumnAccessFoundForMerge(CharSequence qualifiedColumn, int columninfo_access);

	/**
	 * Just add it to the implicit map for table columns.
	 */
	void implicitColumnAccess(CharSequence lastQualifiedColumn);

}