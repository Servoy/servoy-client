/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import static com.servoy.j2db.util.Utils.getAsBoolean;
import static com.servoy.j2db.util.Utils.getAsInteger;

import java.util.Properties;

/**
 * @author rgansevles
 *
 */
public class FoundSetManagerConfig
{
	private final int pkChunkSize;
	private final int chunkSize;
	private final int initialRelatedChunkSize;
	private final boolean loadRelatedRecordsIfParentIsNew;
	private final boolean statementBatching;
	private final boolean disableInsertsReorder;
	private final boolean verifyPKDatasetAgainstTableFilters;
	private final boolean optimizedNotifyChange;
	private final boolean optimizedChangeFires;
	private final boolean uninitializedFoundsetWhenFiltersAreAdded;
	private final boolean setRelationNameComment;
	private final boolean deleteWithAutosaveOff;

	public FoundSetManagerConfig(Properties settings)
	{
		pkChunkSize = getAsInteger(settings.getProperty("servoy.foundset.config.pkChunkSize", Integer.toString(200)));// primarykeys to be get in one roundtrip
		chunkSize = getAsInteger(settings.getProperty("servoy.foundset.chunkSize", Integer.toString(30)));// records to be get in one roundtrip
		initialRelatedChunkSize = getAsInteger(settings.getProperty("servoy.foundset.initialRelatedChunkSize", Integer.toString(chunkSize * 2))); // initial related records to get in one roundtrip
		loadRelatedRecordsIfParentIsNew = getAsBoolean(settings.getProperty("servoy.foundset.loadRelatedRecordsIfParentIsNew", "false")); // force-load of possible existing records in DB when initializing a related foundset when the parent is new and the relations is restricted on the rowIdentifier columns of the parent record
		disableInsertsReorder = getAsBoolean(settings.getProperty("servoy.disable.record.insert.reorder", "false"));
		statementBatching = getAsBoolean(settings.getProperty("servoy.foundset.statementBatching", "false")); // whether to batch inserts/updates for rows together in the same SQLStatement where possible
		verifyPKDatasetAgainstTableFilters = getAsBoolean(settings.getProperty("servoy.foundset.verifyPKDatasetAgainstTableFilters", "true")); // when false we do not trigger a query with fs.loadRecords(pk) icw table filters
		optimizedNotifyChange = getAsBoolean(settings.getProperty("servoy.foundset.optimizedNotifyChange", "true")); // whether to use new optimized mechanism to call notifyChange on IRowListeners
		optimizedChangeFires = getAsBoolean(settings.getProperty("servoy.foundset.optimizedChangeFires", "true")); // whether to use new optimized mechanism to call notifyChange on IRowListeners
		uninitializedFoundsetWhenFiltersAreAdded = getAsBoolean(settings.getProperty("servoy.foundset.unitializeWithFilter", "false")); // whether to set initialized to false for a foundset when fs filter params are added
		setRelationNameComment = getAsBoolean(settings.getProperty("servoy.client.sql.setRelationComment", "true"));
		deleteWithAutosaveOff = getAsBoolean(settings.getProperty("servoy.foundset.deleteWithAutosaveOff", "false"));
	}

	public int pkChunkSize()
	{
		return pkChunkSize;
	}

	public int chunkSize()
	{
		return chunkSize;
	}

	public int initialRelatedChunkSize()
	{
		return initialRelatedChunkSize;
	}

	public boolean loadRelatedRecordsIfParentIsNew()
	{
		return loadRelatedRecordsIfParentIsNew;
	}

	public boolean statementBatching()
	{
		return statementBatching;
	}

	public boolean disableInsertsReorder()
	{
		return disableInsertsReorder;
	}

	public boolean verifyPKDatasetAgainstTableFilters()
	{
		return verifyPKDatasetAgainstTableFilters;
	}

	public boolean optimizedNotifyChange()
	{
		return optimizedNotifyChange;
	}

	public boolean optimizedChangeFires()
	{
		return optimizedChangeFires;
	}

	public boolean uninitializedFoundsetWhenFiltersAreAdded()
	{
		return uninitializedFoundsetWhenFiltersAreAdded;
	}

	public boolean setRelationNameComment()
	{
		return setRelationNameComment;
	}

	public boolean deleteWithAutosaveOff()
	{
		return deleteWithAutosaveOff;
	}


}
