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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Interface for exporting solutions to xml file.
 */
public interface IXMLExporter
{
	void exportSolutionToFile(Solution solution, File file, String version, int buildNumber, boolean exportMetaData, boolean exportSampleData,
		int nrOfExportSampleData, boolean exportI18N, boolean exportUserInfo, boolean includeModules, boolean protect, ITableDefinitionsManager tableDefManager,
		IMetadataDefManager metadataDefManager, boolean exportSolution, JSONObject importSettings, Map<String, List<File>> modulesWebPackages)
		throws RepositoryException;
}
