/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONString;
import org.json.JSONWriter;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.JSONUtils.EmbeddableJSONWriter;
import org.sablo.websocket.utils.JSONUtils.JSONStringWrapper;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * The whole purpose of this class is to coalesce client side types that are to be sent to the client for a viewport into one per column or one globally - if you have the same type (or none) in all rows for a column or for all columns.<br/>
 * If that is not possible (different cells have different client side types) then it will keep track of all cell types.
 *
 * @author acostescu
 */
@SuppressWarnings("nls")
public class ViewportClientSideTypes
{

	public static final String MAIN_TYPE = "mT";
	public static final String COL_TYPES = "cT";
	public static final String CELL_TYPES = "eT";
	public static final String FOR_ROW_IDXS = "i";


	private int lastRegistered = -1; // the last row on which "registerClientSideType" was called (relative to viewport)
	private int nextFoundsetIndexToBeRegistered = -1;
	private final int startIdxOfAreaToBeWritten, sizeOfAreaToBeWritten;

	private Map<String, Map<String, List<Integer>>> typesPerColumn = null; // if cells have different types we keep that for each columnName (which is the key of the map); value is a list
	// of types-to-indexes inside that column (if all in that column have the same type the list will have only one item; if they have different types within the same column (for example
	// if MediaDataproviderPropertyType ends up writing things for cells you might end up with multiple types in the list for one column);
	// later we will see which type appears most often in the column and we optimize it when writing to JSON (as usually you will have only 1 or 2 types per column, like null and 'date' for example)

	public ViewportClientSideTypes(int startIdxOfAreaToBeWritten, int endIdxOfAreaToBeWritten)
	{
		this.startIdxOfAreaToBeWritten = startIdxOfAreaToBeWritten;
		this.sizeOfAreaToBeWritten = endIdxOfAreaToBeWritten - startIdxOfAreaToBeWritten + 1;
	}


	/**
	 * Only used by {@link #registerClientSideType(Pair...)}. See the documentation of that method.
	 *
	 * @see #registerClientSideType(Pair...)
	 */
	public void nextRecordWillBe(int nextFoundsetIndex)
	{
		this.nextFoundsetIndexToBeRegistered = nextFoundsetIndex;
	}

	/**
	 * Same as {@link #registerClientSideType(int, Pair...)} where the first argument is next record (so nextFoundsetIndexToBeRegistered that was last set via #nextRecordWillBe(int)).
	 * Make sure that you always call "nextRecordWillBe" (for the correct record) before calling this.
	 *
	 * @param clientSideTypesForRow an array of column names and client side type for that column in the given row "forRowIdx"; columnName can be null if that particular ViewportRowDataProvider doesn't use multiple columns; type can also be null if it does not have a client side type; the whole arg can be null if there is no type and no column name
	 *
	 * @see #nextRecordWillBe(int)
	 * @see #registerClientSideType(int, Pair...)
	 */
	public final void registerClientSideType(List<Pair<String/* forColumn */, JSONString/* type */>> clientSideTypesForRow)
	{
		registerClientSideType(nextFoundsetIndexToBeRegistered, clientSideTypesForRow);
	}

	/**
	 * We currently expect that ViewportRowDataProvider implementations call this method EXACTLY ONCE AND IN SEQUENCE FROM THE START OF INTERVAL for each row that they are asked to write to JSON (probably from "populateRowData").<br/>
	 * Not calling this for a row of calling it in a different order will result in an IllegalArgumentException. This restriction can be removed if needed but currently this is what viewports do... (in a previous impl this restriction simplified implementation but that is no longer the case)
	 *
	 * @param forRowIdx the row of the foundset the given clientSideTypes are for
	 * @param clientSideTypesForRow an array of column names and client side type for that column in the given row "forRowIdx"; columnName can be null if that particular ViewportRowDataProvider doesn't use multiple columns; type can also be null if it does not have a client side type; the whole arg can be null if there is no type and no column name in the whole row
	 *
	 * @throws IllegalArgumentException if forRowIdx is not the expected one.
	 */
	public final void registerClientSideType(int forRowIdx, List<Pair<String/* forColumn */, JSONString/* type */>> clientSideTypesForRow)
	{
		int relativeForRow = forRowIdx - startIdxOfAreaToBeWritten;

		if (relativeForRow >= sizeOfAreaToBeWritten || relativeForRow < 0)
			throw new IllegalArgumentException("'registerClientSideType' was called for relative idx <" +
				relativeForRow + "> which is < 0 or greater then or equal to size <" + sizeOfAreaToBeWritten + "> (lastRegistered = " + lastRegistered +
				"). Absolute start idx: " + startIdxOfAreaToBeWritten + "; absolute row idx: " + forRowIdx);
		if (relativeForRow != lastRegistered + 1)
			throw new IllegalArgumentException("Expected 'registerClientSideType' to be called in sequence for relative idx <" + (lastRegistered + 1) +
				"> but it was called for <" + relativeForRow + ">.");
		lastRegistered++;

		if (clientSideTypesForRow != null)
		{
			for (Pair<String, JSONString> clST : clientSideTypesForRow)
			{
				processOneClientSideType(clST.getLeft(), clST.getRight());
			}
		}
		else
		{
			processOneClientSideType(null, null);
		}
	}

	private String toJSONString(JSONString type)
	{
		return type != null ? type.toJSONString() : null;
	}

	private void processOneClientSideType(String columnName, JSONString type)
	{
		if (typesPerColumn == null) typesPerColumn = new HashMap<>();

		String stringifiedType = toJSONString(type);

		Map<String, List<Integer>> columnTypes = typesPerColumn.get(columnName);

		if (columnTypes == null)
		{
			columnTypes = new HashMap<>();
			typesPerColumn.put(columnName, columnTypes);
		}

		List<Integer> typeIndexesInsideColumn = columnTypes.get(stringifiedType);
		if (typeIndexesInsideColumn == null)
		{
			typeIndexesInsideColumn = new ArrayList<>();
			columnTypes.put(stringifiedType, typeIndexesInsideColumn);
		}

		typeIndexesInsideColumn.add(Integer.valueOf(lastRegistered));
	}

	public void writeClientSideTypes(JSONWriter w, String key)
	{
		JSONString typesToBeWritten = getClientSideTypes();
		if (typesToBeWritten != null)
		{
			if (key != null) w.key(key);
			w.value(typesToBeWritten);
		}
	}

	/**
	 * IMPORTANT: If you update/change this you have to also update the comment/change the impl. in viewport.ts and in foundsetLinked.ts -> generateWholeViewportFromOneValue
	 *
	 * Generates a client side type(s) JSON value (which could be a simple type or a more advanced structure with types per column / cell) that is understood correctly by viewport.js...<br/>
	 * The idea is that the most common type will be written only once per viewport (main type) then for each column that has other types a column main type (most common one left in column) can
	 * be written where needed and then other cell types (that are not main type or column main type) say what indexes they have in each column.<br/><br/>
	 *
	 * Example output:
	 * <pre>
	 * {
	 *  "mT": "date",
	 *  "cT": {
	 *     "b": { "_T": null},
	 *     "c": {`
	 *         "eT":
	 *           [
	 *             { "_T": null, "i": [4] },
	 *             { "_T": "zyx", "i": [2,5,9] },
	 *             {"_T": "xyz", "i": [0] }
	 *           ]
	 *       }
	 *   }
	 * }
	 *
	 * where
	 *   JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY   == "_T"
	 *   ViewportClientSideTypes.MAIN_TYPE       == "mT"
	 *   ViewportClientSideTypes.COL_TYPES       == "cT"
	 *   ViewportClientSideTypes.CELL_TYPES      == "eT"
	 *   ViewportClientSideTypes.FOR_ROW_IDXS    == "i"
	 * </pre>
	 */
	public JSONString getClientSideTypes()
	{
		if (lastRegistered != sizeOfAreaToBeWritten - 1)
			throw new RuntimeException("getClientSideType called before all indexes in area to be written were registered...");

		if (typesPerColumn == null) return null; // no types; nothing should be written to JSON as far as types go

		Map<String, Integer> colMainTypeCounter = new HashMap<>(typesPerColumn.size());
		Map<String, Pair<String, JSONString>> colMainAndCellTypes = new HashMap<>(typesPerColumn.size());

		JSONStringWrapper t = new JSONStringWrapper();

		// ok now see which is the main column type of each column and write for later any different types and the indexes they are at
		for (Entry<String, Map<String, List<Integer>>> columnTypesEntry : typesPerColumn.entrySet())
		{
			EmbeddableJSONWriter cellTypesWriter = new EmbeddableJSONWriter(true);
			if (columnTypesEntry.getValue().size() > 0)
			{
				Entry<String, List<Integer>> columnMainTypeWinnerEntry = null; // appeared most times in viewport cells
				boolean cellTypesWritten = false;
				for (Entry<String, List<Integer>> columnTypeEntry : columnTypesEntry.getValue().entrySet())
				{
					Entry<String, List<Integer>> colTypeToWrite = null;
					if (columnMainTypeWinnerEntry == null || (columnMainTypeWinnerEntry.getValue().size() < columnTypeEntry.getValue().size()))
					{
						// another entry has more indexes, that will be come the new columnMainTypeWinnerEntry but now we write to json the old/current columnMainTypeWinnerEntry indexes as cell types
						colTypeToWrite = columnMainTypeWinnerEntry; // can be null on first iteration
						columnMainTypeWinnerEntry = columnTypeEntry;
					}
					else colTypeToWrite = columnTypeEntry;

					if (colTypeToWrite != null)
					{
						if (!cellTypesWritten)
						{
							cellTypesWriter.array(); // we make here an array of objects with type & indexes instead of a map of type -> indexes because type might not be the JSON representation of a String (could be an array see for example CustomJSONObjectType.writeClientSideTypeName(JSONWriter, String, PropertyDescription)
							cellTypesWritten = true;
						}

						cellTypesWriter.object();

						t.wrappedString = colTypeToWrite.getKey(); // the type of the cells in array below
						cellTypesWriter.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(t);

						cellTypesWriter.key(FOR_ROW_IDXS).array();
						colTypeToWrite.getValue().forEach((idx) -> cellTypesWriter.value(idx)); // cells that have this type (that is != main type and main column type)
						cellTypesWriter.endArray();

						cellTypesWriter.endObject();
					}
				}
				if (cellTypesWritten) cellTypesWriter.endArray(); // for CELL_TYPES array

				String mainColType = columnMainTypeWinnerEntry.getKey();
				Integer counterOfColumnsWithThisMainType = colMainTypeCounter.get(mainColType);
				colMainTypeCounter.put(mainColType,
					Integer.valueOf(counterOfColumnsWithThisMainType != null ? counterOfColumnsWithThisMainType.intValue() + 1 : 0));
				colMainAndCellTypes.put(columnTypesEntry.getKey(),
					new Pair<>(mainColType, cellTypesWriter.toJSONString().length() == 0 ? null : cellTypesWriter));
			}

			columnTypesEntry.getValue().clear(); // just to help GC
		}

		// now we know column main types is any; see which one appears most often
		Entry<String, Integer> mainTypeWinnerEntry = null; // appeared most times in column main types
		for (Entry<String, Integer> typeCounterEntry : colMainTypeCounter.entrySet())
		{
			if (mainTypeWinnerEntry == null || (mainTypeWinnerEntry.getValue().intValue() < typeCounterEntry.getValue().intValue()))
				mainTypeWinnerEntry = typeCounterEntry;
		}

		EmbeddableJSONWriter rootEjw = new EmbeddableJSONWriter(true);

		boolean colTypesWritten = false;
		boolean endObjNeededForAllCols = false;

		rootEjw.object();

		// now we know main type/col. main types and column cell types; write column types without redundant info (no col type is same with main type, no cell types if not needed etc.)
		Iterator<Entry<String, Pair<String, JSONString>>> it = colMainAndCellTypes.entrySet().iterator();
		while (it.hasNext())
		{
			boolean colKeyWritten = false;
			Entry<String, Pair<String, JSONString>> columnTypesEntry = it.next();
			if (columnTypesEntry.getValue().getRight() != null)
			{
				// this col. wants to write cell types so it has more then 1 type in cells of this column
				if (!colTypesWritten)
				{
					rootEjw.key(COL_TYPES);
					if (typesPerColumn.size() > 1 || columnTypesEntry.getKey() != null)
					{
						rootEjw.object(); // multiple columns; not just one column with key null (which is what foundset linked type would produce and in which case we just write the types for that columns directly no need for an object with multiple columns)
						endObjNeededForAllCols = true;
					}
					colTypesWritten = true;
				}
				if (columnTypesEntry.getKey() != null) rootEjw.key(columnTypesEntry.getKey());
				colKeyWritten = true;
				rootEjw.object().key(CELL_TYPES).value(columnTypesEntry.getValue().getRight());
			}

			if (!Utils.safeEquals(mainTypeWinnerEntry.getKey(), columnTypesEntry.getValue().getLeft()))
			{
				// so main viewport type is not equal to main column type = then we have to write the column type
				if (!colTypesWritten)
				{
					rootEjw.key(COL_TYPES);
					if (typesPerColumn.size() > 1 || columnTypesEntry.getKey() != null)
					{
						rootEjw.object(); // multiple columns; not just one column with key null (which is what foundset linked type would produce and in which case we just write the types for that columns directly no need for an object with multiple columns)
						endObjNeededForAllCols = true;
					}
					colTypesWritten = true;
				}
				if (!colKeyWritten)
				{
					if (columnTypesEntry.getKey() != null) rootEjw.key(columnTypesEntry.getKey());
					rootEjw.object();
					colKeyWritten = true;
				}

				t.wrappedString = columnTypesEntry.getValue().getLeft();
				rootEjw.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(t); // main column type
			}
			if (colKeyWritten) rootEjw.endObject(); // for this column's types
		}
		if (endObjNeededForAllCols) rootEjw.endObject(); // for COL_TYPES obj if added

		// write main type only if it's not-null or if there are already column main types or cell types already written; otherwise nothing in the viewport has types - we don't write anything
		if (mainTypeWinnerEntry.getKey() == null && !colTypesWritten) return null;

		t.wrappedString = mainTypeWinnerEntry.getKey();
		rootEjw.key(MAIN_TYPE).value(t).endObject();

		typesPerColumn.clear(); // just to help GC
		typesPerColumn = null;

		return rootEjw;
	}

}