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

package com.servoy.j2db.server.ngclient.property;

import java.util.List;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.SQLSheet;
import com.servoy.j2db.dataprocessing.SortColumn;
import com.servoy.j2db.dataprocessing.SwingRelatedFoundSet;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.ServoyException;

/**
 * @author acostescu
 */
public class TestSwingRelatedFoundset extends SwingRelatedFoundSet implements ITestFoundset
{
	public TestSwingRelatedFoundset(IDataSet data, QuerySelect querySelect, IFoundSetManagerInternal app, String relationName,
		SQLSheet sheet, List<SortColumn> defaultSortColumns, QuerySelect aggregateSelect, IDataSet aggregateData) throws ServoyException
	{
		super(data, querySelect, app, relationName, sheet, defaultSortColumns, aggregateSelect, aggregateData);
	}

	public TestSwingRelatedFoundset(IFoundSetManagerInternal fsm, String relationName, SQLSheet childSheet) throws ServoyException
	{
		super(fsm, relationName, childSheet);
	}

	public int getNumberOfFoundsetEventListeners()
	{
		return foundSetEventListeners.size();
	}
}
