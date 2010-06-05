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

import com.servoy.j2db.util.ServoyException;

/**
 * @author jblok
 */
@Deprecated
public interface IFoundSetDeprecated
{
	@Deprecated
	public void setRecordToStringDataProviderID(String dataProviderID);

	@Deprecated
	public String getRecordToStringDataProviderID();

	@Deprecated
	public void sort(List<SortColumn> sortColumns) throws ServoyException;

	@Deprecated
	public void makeEmpty();

	@Deprecated
	public void browseAll() throws ServoyException;

	@Deprecated
	public void deleteAll() throws ServoyException;

	@Deprecated
	public int newRecord() throws ServoyException;

	@Deprecated
	public int newRecord(boolean addOnTop) throws ServoyException;

	@Deprecated
	public int newRecord(boolean addOnTop, boolean changeSelection) throws ServoyException;

	@Deprecated
	public int duplicateRecord(int row, boolean addOnTop) throws ServoyException;
}
