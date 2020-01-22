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

	private JSONString mainType = null; // if all cells (from all rows/columns being written) have the same type or no type this will be it; otherwise it's just a fallback for "typesPerColumn"
	private Map<String, Pair<JSONString, Map<String, List<Integer>>>> typesPerColumn = null; // if cells have different types we keep that for each columnName (which is the key of the map) - which could be a JSONString as a column-level type or column-level fallback type (if all in that column have the same type (in which case the map in the Pair is null) or a JSONString map (index -> JSONSTring) if they have different types within the same column (for example if MediaDataproviderPropertyType end up writing things for cells you might end up with multiple types in Pair.getRight())

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
	 * Not calling this for a row of calling it in a different order will result in an IllegalArgumentException. This restriction is in place in order to simplify the implementation.
	 *
	 * @param forRowIdx the row of the foundset the given clientSideTypes are for
	 * @param clientSideTypesForRow an array of column names and client side type for that column in the given row "forRowIdx"; columnName can be null if that particular ViewportRowDataProvider doesn't use multiple columns; type can also be null if it does not have a client side type; the whole arg can be null if there is no type and no column name in the whole row
	 *
	 * @throws IllegalArgumentException if forRowIdx is not the expected one.
	 */
	public final void registerClientSideType(int forRowIdx, List<Pair<String/* forColumn */, JSONString/* type */>> clientSideTypesForRow)
	{
		int relativeForRow = forRowIdx - startIdxOfAreaToBeWritten;

		if (relativeForRow >= sizeOfAreaToBeWritten) throw new IllegalArgumentException("'registerClientSideType' was called for relative idx <" +
			relativeForRow + "> which is greater then or equal to size <" + sizeOfAreaToBeWritten + "> (lastRegistered = " + lastRegistered + ").");
		if (relativeForRow != lastRegistered + 1)
			throw new IllegalArgumentException("Expected 'registerClientSideType' to be called in sequence for relative idx <" + (lastRegistered + 1) +
				"> but it was called for <" + relativeForRow + ">.");
		lastRegistered++;

		boolean firstClientSideType = (lastRegistered == 0);
		if (clientSideTypesForRow != null)
		{
			for (Pair<String, JSONString> clST : clientSideTypesForRow)
			{
				processOneClientSideType(firstClientSideType, clST.getLeft(), clST.getRight());
				firstClientSideType = false;
			}
		}
		else
		{
			processOneClientSideType(firstClientSideType, null, null);
		}
	}

	private boolean checkJSONStringEquals(JSONString a, JSONString b)
	{
		return Utils.safeEquals(a != null ? a.toJSONString() : null, b != null ? b.toJSONString() : null);
	}

	private void processOneClientSideType(boolean firstClientSideType, String columnName, JSONString type)
	{
		if (firstClientSideType)
		{
			// so it is the first clientSideTypes we are looking at; we can safely store it in allHaveThisType as it's the only one for now
			mainType = type;
		}
		else
		{
			boolean checkAtColumnLevel = false;
			if (typesPerColumn == null)
			{
				if (!checkJSONStringEquals(mainType, type))
				{
					// so until now all were of "allHaveThisType" type
					// we have more then one type - create then typesPerColumn
					typesPerColumn = new HashMap<>();
					checkAtColumnLevel = true;
				}
				else checkAtColumnLevel = false;// else it's still the same type; nothing to do
			}
			else checkAtColumnLevel = true;

			if (checkAtColumnLevel)
			{
				// so we already have multiple types; see how it fits in the existing column types
				Pair<JSONString, Map<String, List<Integer>>> columnTypes = typesPerColumn.get(columnName);
				boolean storeCellLevelType;
				if (columnTypes == null)
				{
					if (!checkJSONStringEquals(mainType, type))
					{
						if (lastRegistered == 0)
						{
							// so it is the first row that is being registered for this column and it does not match the main type of the whole viewport
							typesPerColumn.put(columnName, new Pair<>(type, null));
							storeCellLevelType = false;
						}
						else
						{
							// this also means that lastRegistered > 0 - see if above (so all previous indexes in this column matched the main viewport type); now we have a difference; so main type is also the type of this column
							columnTypes = new Pair<>(mainType, null);
							typesPerColumn.put(columnName, columnTypes);
							storeCellLevelType = true;
						}
					}
					else storeCellLevelType = false; // else it's still the same type; nothing to do
				}
				else storeCellLevelType = !checkJSONStringEquals(columnTypes.getLeft(), type);

				if (storeCellLevelType)
				{
					String typeKey = (type != null ? type.toJSONString() : null);
					if (columnTypes.getRight() == null)
					{
						// all cells in this column had the same type so far
						Map<String, List<Integer>> typesOfCol = new HashMap<>();
						List<Integer> l = new ArrayList<>();
						l.add(Integer.valueOf(lastRegistered));
						typesOfCol.put(typeKey, l);
						columnTypes.setRight(typesOfCol);
					}
					else
					{
						// we already have different types for cells in this row
						List<Integer> l = columnTypes.getRight().get(typeKey);
						if (l == null)
						{
							l = new ArrayList<>();
							columnTypes.getRight().put(typeKey, l);
						}
						l.add(Integer.valueOf(lastRegistered));
					}
				} // else it's still the same type; nothing to do
			}
		}
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
	 * Generates a client side type(s) JSON value (which could be a simple type or a more advanced structure with types per column / cell) that is understood correctly by viewport.js...
	 */
	public JSONString getClientSideTypes()
	{
		if (lastRegistered != sizeOfAreaToBeWritten - 1)
			throw new RuntimeException("getClientSideType called before all indexes in area to be written were registered...");

		if (mainType == null && typesPerColumn == null) return null; // no types; nothing should be written to JSON as far as types go

		EmbeddableJSONWriter ejw = new EmbeddableJSONWriter(true);
		ejw.object().key(MAIN_TYPE).value(mainType);

		if (typesPerColumn != null)
		{
			ejw.key(COL_TYPES);
			if (typesPerColumn.size() == 1 && typesPerColumn.keySet().iterator().next() == null)
			{
				// this means that there is only one column with no name - probably this is for a foundset-linked type so we write it directly then
				writeColTypes(ejw, typesPerColumn.get(null));
			}
			else
			{
				ejw.object();
				for (Entry<String, Pair<JSONString, Map<String, List<Integer>>>> e : typesPerColumn.entrySet())
				{
					ejw.key(e.getKey());
					writeColTypes(ejw, e.getValue());
				}
				ejw.endObject();
			}
		}
		ejw.endObject();
		return ejw;
	}

	private void writeColTypes(EmbeddableJSONWriter ejw, Pair<JSONString, Map<String, List<Integer>>> columnTypes)
	{
		ejw.object();
		if (mainType != columnTypes.getLeft())
		{
			ejw.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(columnTypes.getLeft()); // main/fallback type of this column if different from main viewport fallback type
		}
		Map<String, List<Integer>> columnCellTypes = columnTypes.getRight();
		if (columnCellTypes != null)
		{
			ejw.key(CELL_TYPES);
			ejw.array(); // we make here an array of objects with type & indexes instead of a map of type -> indexes because type might not be the JSON representation of a String (could be an array see for example CustomJSONObjectType.writeClientSideTypeName(JSONWriter, String, PropertyDescription)
			JSONStringWrapper t = new JSONStringWrapper();
			for (Entry<String, List<Integer>> cctE : columnCellTypes.entrySet())
			{
				ejw.object();
				t.wrappedString = cctE.getKey();
				ejw.key(JSONUtils.CONVERSION_CL_SIDE_TYPE_KEY).value(t);

				ejw.key(FOR_ROW_IDXS).array();
				cctE.getValue().forEach((idx) -> ejw.value(idx));
				ejw.endArray();

				ejw.endObject();
			}
			ejw.endArray();
		}
		ejw.endObject();
	}

}