/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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


import org.mozilla.javascript.Function;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.scripting.annotations.JSSignature;
import com.servoy.j2db.util.ServoyException;

/** JSBaseSQLFoundSet is the base class for SQL based foundsets (either a JSFoundSet or a ViewFoundSet).
 *
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "JSBaseSQLFoundSet", scriptingName = "JSBaseSQLFoundSet", extendsComponent = "JSBaseFoundSet")
public interface IJSBaseSQLFoundSet extends IJSBaseFoundSet
{
	@JSSetter
	void setMultiSelect(boolean multiSelect);

	@JSGetter
	boolean isMultiSelect();

	@JSFunction
	boolean dispose();

	@JSFunction
	IJSBaseSQLFoundSet duplicateFoundSet() throws ServoyException;

	@JSFunction
	String getCurrentSort();

	IJSBaseSQLRecord js_getRecordByPk(Object... pk);

	boolean js_loadAllRecords() throws ServoyException;

	@JSFunction
	void revertEditedRecords() throws ServoyException;

	@JSFunction
	public boolean save() throws ServoyException;

	@JSFunction
	public void sort(String sortString) throws ServoyException;

	@JSFunction
	public void sort(String sortString, Boolean defer) throws ServoyException;

	@JSFunction
	@JSSignature(arguments = { Function.class })
	public void sort(Object recordComparisonFunction);

	/**
	 * Returns the internal SQL of the JSFoundset.
	 * Optionally, the foundset and table filter params can be excluded in the sql (includeFilters=false).
	 *
	 * @sample var sql = %%prefix%%foundset.getSQL(true)
	 *
	 * @return String representing the sql of the JSFoundset.
	 */
	@JSFunction
	default String getSQL() throws ServoyException
	{
		return getSQL(true);
	}

	/**
	 * @clonedesc getSQL()
	 *
	 * @sampleas getSQL()
	 *
	 * @param includeFilters include the foundset and table filters [default true].
	 *
	 * @return String representing the sql of the JSFoundset.
	 */
	@JSFunction
	default String getSQL(boolean includeFilters) throws ServoyException
	{
		if (getTable() != null)
		{
			QuerySet querySet = getFoundSetManager().getQuerySet(getCurrentStateQuery(true, false), includeFilters);
			return querySet.getSelect().getSql();
		}
		return null;
	}

	/**
	 * Returns the parameters for the internal SQL of the QBSelect.
	 * Table filters are on by default.
	 *
	 * @sample var parameters = %%prefix%%foundset.getSQLParameters(true)
	 *
	 * @return An Array with the sql parameter values.
	 */
	@JSFunction
	default Object[] getSQLParameters() throws ServoyException
	{
		return getSQLParameters(true);
	}

	/**
	 * @clonedesc getSQLParameters()
	 *
	 * @sampleas getSQLParameters()
	 *
	 * @param includeFilters include the foundset and table filters [default true].
	 *
	 * @return An Array with the sql parameter values.
	 */
	@JSFunction
	default Object[] getSQLParameters(boolean includeFilters) throws ServoyException
	{
		if (getTable() != null)
		{
			QuerySet querySet = getFoundSetManager().getQuerySet(getCurrentStateQuery(true, false), includeFilters);
			Object[][] qsParams = querySet.getSelect().getParameters();
			if (qsParams == null || qsParams.length == 0)
			{
				return null;
			}
			return qsParams[0];
		}
		return null;
	}


}