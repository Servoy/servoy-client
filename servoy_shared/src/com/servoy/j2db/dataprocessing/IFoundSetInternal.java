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


import java.util.Comparator;
import java.util.List;

import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.scripting.UsedDataProviderTracker;
import com.servoy.j2db.util.ServoyException;

/**
 * The foundset interface
 *
 * @author jblok
 */
public interface IFoundSetInternal extends IFoundSet, IFireCollectable
{
	public SQLSheet getSQLSheet();

	public PrototypeState getPrototypeState();

	public void addParent(IRecordInternal record);

	public void fireAggregateChangeWithEvents(IRecordInternal record);

	/**
	 * Check for valid relation name, may be multiple-levels deep
	 */
	public boolean isValidRelation(String name);

	/**
	 * Get related foundset, relationName may be multiple-levels deep
	 */
	public IFoundSetInternal getRelatedFoundSet(IRecordInternal record, String relationName, List<SortColumn> defaultSortColumns) throws ServoyException;

	//Found set is using scriptengine to recalculate the specified calculation,check first with containsCalculation before calling
	public Object getCalculationValue(IRecordInternal record, String dataProviderID, Object[] vargs, UsedDataProviderTracker usedDataProviderTracker);

	public void sort(List<SortColumn> sortColumns, boolean defer) throws ServoyException;

	public void sort(Comparator<Object[]> recordPKComparator);

	public List<SortColumn> getSortColumns();

	public IFoundSetManagerInternal getFoundSetManager();

	public ITable getTable();

	public IFoundSetInternal copyCurrentRecordFoundSet() throws ServoyException;

	public IFoundSetInternal copy(boolean unrelate) throws ServoyException;

	public IRecordInternal getRecord(int row);

	public IRecordInternal[] getRecords(int startrow, int count);

	public void deleteAllInternal() throws ServoyException;

	public void addAggregateModificationListener(IModificationListener listener);

	public void removeAggregateModificationListener(IModificationListener listener);

	public boolean hadMoreRows();

	public void deleteRecord(IRecordInternal record) throws ServoyException;

	public IRecordInternal getRecord(Object[] pk);

	public int getRecordIndex(String pkHash, int hintStart);

	public int getID();

	/**
	 * @return
	 */
	public boolean isInitialized();

	/**
	 * @return
	 */
	public int getRawSize();

	/**
	 *
	 */
	public void fireFoundSetChanged();
}