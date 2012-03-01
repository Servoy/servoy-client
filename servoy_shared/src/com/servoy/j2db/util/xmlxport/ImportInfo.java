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

package com.servoy.j2db.util.xmlxport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.servoy.j2db.util.UUID;

/**
 * * The <code>ImportInfo</code> contains all state information which is needed during the import.
 */
public class ImportInfo
{
	public boolean cleanImport = false;
	public Map<Integer, Map<String, String>> substitutionMap = new HashMap<Integer, Map<String, String>>();
	public Map<String, UUID> cleanImportUUIDMap = new HashMap<String, UUID>();
	public Set<UserInfo> userInfoSet = null;
	public Map<String, Set<TableDef>> databaseInfoMap = null;
	public Map<String, Set<MetadataDef>> metadataMap = null;
	public Set<RootObjectImportInfo> rootObjectInfoSet = new HashSet<RootObjectImportInfo>();
	public RootObjectImportInfo main = null;
	public Map<String, Object> blobIdMap = new HashMap<String, Object>();
	public MappedUnresolvedUUIDResolver uuidResolver = new MappedUnresolvedUUIDResolver();
	public boolean isProtected = false;

}
