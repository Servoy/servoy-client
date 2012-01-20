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

import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.util.ServoyException;

/**
 * SwingFoundSetFactory
 * @author jblok
 */
public class SwingFoundSetFactory implements IFoundSetFactory
{
	public IFoundSetInternal createFoundSet(IFoundSetManagerInternal fsm, SQLSheet sheet, QuerySelect pkSelect, List<SortColumn> defaultSortColumns)
		throws ServoyException
	{
		return new SwingFoundSet(fsm, sheet, pkSelect, defaultSortColumns);
	}

	public IFoundSetInternal createRelatedFoundSet(IDataSet data, QuerySelect querySelect, IFoundSetManagerInternal fsm, IRecordInternal parent,
		String relationName, SQLSheet sheet, List<SortColumn> defaultSortColumns, QuerySelect aggregateSelect, IDataSet aggregateData) throws ServoyException
	{
		return new SwingRelatedFoundSet(data, querySelect, fsm, parent, relationName, sheet, defaultSortColumns, aggregateSelect, aggregateData);
	}

	public IFoundSetInternal createRelatedFindFoundSet(IFoundSetManagerInternal fsm, IRecordInternal parentRecord, String relationName, SQLSheet childSheet)
		throws ServoyException
	{
		return new SwingRelatedFoundSet(fsm, parentRecord, relationName, childSheet);
	}
}
