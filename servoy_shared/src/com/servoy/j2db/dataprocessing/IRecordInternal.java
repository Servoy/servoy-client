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
package com.servoy.j2db.dataprocessing;

import java.util.List;

/**
 * None public api interface for an {@link IRecord} object.
 * 
 * @author jblok
 */
public interface IRecordInternal extends IRecord, IRowChangeListener
{
	public Row getRawData();

	public boolean isRelatedFoundSetLoaded(String relationName, String restName);

	public String getPKHashKey();

	public String getAsTabSeparated();

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(String relationName);

	public IFoundSetInternal getRelatedFoundSet(String relationName, List<SortColumn> defaultSortColumns);

	public IFoundSetInternal getParentFoundSet();

	public Object setValue(String dataProviderID, Object value, boolean checkIsEditing);
}
