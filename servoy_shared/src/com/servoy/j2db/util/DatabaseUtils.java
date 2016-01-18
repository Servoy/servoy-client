/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.j2db.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersistFactory;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * @author gboros
 *
 */
public class DatabaseUtils
{
	/**
	 * Gets the table information from a .dbi (JSON format) file like structured String.
	 *
	 * @param stringDBIContent the table information in .dbi format
	 * @return the deserialized table information.
	 * @throws JSONException if the structure of the JSON in String stringDBIContent is bad.
	 */
	public static TableDef deserializeTableInfo(ServoyJSONObject dbiContents) throws JSONException
	{
		TableDef tableInfo = new TableDef();
		tableInfo.name = dbiContents.getString("name");
		tableInfo.tableType = dbiContents.getInt(TableDef.PROP_TABLE_TYPE);
		tableInfo.hiddenInDeveloper = dbiContents.has(TableDef.HIDDEN_IN_DEVELOPER) ? dbiContents.getBoolean(TableDef.HIDDEN_IN_DEVELOPER) : false;
		tableInfo.isMetaData = dbiContents.has(TableDef.IS_META_DATA) ? dbiContents.getBoolean(TableDef.IS_META_DATA) : false;

		if (dbiContents.has(TableDef.PROP_COLUMNS))
		{
			JSONArray columns = dbiContents.getJSONArray(TableDef.PROP_COLUMNS);
			for (int i = 0; i < columns.length(); i++)
			{
				JSONObject cobj = columns.getJSONObject(i);
				if (cobj == null) continue;
				ColumnInfoDef cid = new ColumnInfoDef();

				cid.creationOrderIndex = cobj.getInt(ColumnInfoDef.CREATION_ORDER_INDEX);
				cid.name = cobj.getString("name");
				// Note, since 6.1 dataType and length are interpreted as configured type/length
				cid.columnType = ColumnType.getInstance(cobj.getInt(ColumnInfoDef.DATA_TYPE),
					cobj.has(ColumnInfoDef.LENGTH) ? cobj.optInt(ColumnInfoDef.LENGTH) : 0,
					cobj.has(ColumnInfoDef.SCALE) ? cobj.optInt(ColumnInfoDef.SCALE) : 0);
				cid.compatibleColumnTypes = cobj.has(ColumnInfoDef.COMPATIBLE_COLUMN_TYPES)
					? XMLUtils.parseColumnTypeArray(cobj.optString(ColumnInfoDef.COMPATIBLE_COLUMN_TYPES)) : null;
				cid.allowNull = cobj.getBoolean(ColumnInfoDef.ALLOW_NULL);
				cid.autoEnterType = cobj.has(ColumnInfoDef.AUTO_ENTER_TYPE) ? cobj.optInt(ColumnInfoDef.AUTO_ENTER_TYPE) : ColumnInfo.NO_AUTO_ENTER;
				cid.autoEnterSubType = cobj.has(ColumnInfoDef.AUTO_ENTER_SUB_TYPE) ? cobj.optInt(ColumnInfoDef.AUTO_ENTER_SUB_TYPE)
					: ColumnInfo.NO_SEQUENCE_SELECTED;
				cid.sequenceStepSize = cobj.has(ColumnInfoDef.SEQUENCE_STEP_SIZE) ? cobj.optInt(ColumnInfoDef.SEQUENCE_STEP_SIZE) : 1;
				cid.preSequenceChars = cobj.has(ColumnInfoDef.PRE_SEQUENCE_CHARS) ? cobj.optString(ColumnInfoDef.PRE_SEQUENCE_CHARS) : null;
				cid.postSequenceChars = cobj.has(ColumnInfoDef.POST_SEQUENCE_CHARS) ? cobj.optString(ColumnInfoDef.POST_SEQUENCE_CHARS) : null;
				cid.defaultValue = cobj.has(ColumnInfoDef.DEFAULT_VALUE) ? cobj.optString(ColumnInfoDef.DEFAULT_VALUE) : null;
				cid.lookupValue = cobj.has(ColumnInfoDef.LOOKUP_VALUE) ? cobj.optString(ColumnInfoDef.LOOKUP_VALUE) : null;
				cid.databaseSequenceName = cobj.has(ColumnInfoDef.DATABASE_SEQUENCE_NAME) ? cobj.optString(ColumnInfoDef.DATABASE_SEQUENCE_NAME) : null;
				cid.titleText = cobj.has(ColumnInfoDef.TITLE_TEXT) ? cobj.optString(ColumnInfoDef.TITLE_TEXT) : null;
				cid.description = cobj.has(ColumnInfoDef.DESCRIPTION) ? cobj.optString(ColumnInfoDef.DESCRIPTION) : null;
				cid.foreignType = cobj.has(ColumnInfoDef.FOREIGN_TYPE) ? cobj.optString(ColumnInfoDef.FOREIGN_TYPE) : null;
				cid.converterName = cobj.has(ColumnInfoDef.CONVERTER_NAME) ? cobj.optString(ColumnInfoDef.CONVERTER_NAME) : null;
				cid.converterProperties = cobj.has(ColumnInfoDef.CONVERTER_PROPERTIES) ? cobj.optString(ColumnInfoDef.CONVERTER_PROPERTIES) : null;
				cid.validatorProperties = cobj.has(ColumnInfoDef.VALIDATOR_PROPERTIES) ? cobj.optString(ColumnInfoDef.VALIDATOR_PROPERTIES) : null;
				cid.validatorName = cobj.has(ColumnInfoDef.VALIDATOR_NAME) ? cobj.optString(ColumnInfoDef.VALIDATOR_NAME) : null;
				cid.defaultFormat = cobj.has(ColumnInfoDef.DEFAULT_FORMAT) ? cobj.optString(ColumnInfoDef.DEFAULT_FORMAT) : null;
				cid.elementTemplateProperties = cobj.has(ColumnInfoDef.ELEMENT_TEMPLATE_PROPERTIES) ? cobj.optString(ColumnInfoDef.ELEMENT_TEMPLATE_PROPERTIES)
					: null;
				cid.flags = cobj.has(ColumnInfoDef.FLAGS) ? cobj.optInt(ColumnInfoDef.FLAGS) : 0;
				cid.dataProviderID = cobj.has(ColumnInfoDef.DATA_PROVIDER_ID) ? Utils.toEnglishLocaleLowerCase(cobj.optString(ColumnInfoDef.DATA_PROVIDER_ID))
					: null;
				cid.containsMetaData = cobj.has(ColumnInfoDef.CONTAINS_META_DATA) ? Integer.valueOf(cobj.optInt(ColumnInfoDef.CONTAINS_META_DATA)) : null;
				if (!tableInfo.columnInfoDefSet.contains(cid))
				{
					tableInfo.columnInfoDefSet.add(cid);
				}
			}
		}
		return tableInfo;
	}

	public static void deserializeInMemoryTable(IPersistFactory persistFactory, IServerInternal s, ITable t, ServoyJSONObject property)
		throws RepositoryException, JSONException
	{
		int existingColumnInfo = 0;
		TableDef tableInfo = deserializeTableInfo(property);
//		if (!t.getName().equals(tableInfo.name))
//		{
//			throw new RepositoryException("Table name does not match dbi file name for " + t.getName());
//		}

		List<IColumn> changedColumns = null;

		if (tableInfo.columnInfoDefSet.size() > 0)
		{
			changedColumns = new ArrayList<IColumn>(tableInfo.columnInfoDefSet.size());
			for (int j = 0; j < tableInfo.columnInfoDefSet.size(); j++)
			{
				ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(j);

				String cname = cid.name;
				Column c = t.getColumn(cname);

				if (c == null)
				{
					c = t.createNewColumn(DummyValidator.INSTANCE, cid.name, cid.columnType.getSqlType(), cid.columnType.getScale());
					existingColumnInfo++;
					int element_id = persistFactory.getNewElementID(null);
					ColumnInfo ci = new ColumnInfo(element_id, true);
					ci.setAutoEnterType(cid.autoEnterType);
					ci.setAutoEnterSubType(cid.autoEnterSubType);
					ci.setSequenceStepSize(cid.sequenceStepSize);
					ci.setPreSequenceChars(cid.preSequenceChars);
					ci.setPostSequenceChars(cid.postSequenceChars);
					ci.setDefaultValue(cid.defaultValue);
					ci.setLookupValue(cid.lookupValue);
					ci.setDatabaseSequenceName(cid.databaseSequenceName);
					ci.setTitleText(cid.titleText);
					ci.setDescription(cid.description);
					ci.setForeignType(cid.foreignType);
					ci.setConverterName(cid.converterName);
					ci.setConverterProperties(cid.converterProperties);
					ci.setValidatorProperties(cid.validatorProperties);
					ci.setValidatorName(cid.validatorName);
					ci.setDefaultFormat(cid.defaultFormat);
					ci.setElementTemplateProperties(cid.elementTemplateProperties);
					ci.setDataProviderID(cid.dataProviderID);
					ci.setContainsMetaData(cid.containsMetaData);
					ci.setConfiguredColumnType(cid.columnType);
					ci.setCompatibleColumnTypes(cid.compatibleColumnTypes);
					ci.setFlags(cid.flags);
					c.setDatabasePK((cid.flags & Column.PK_COLUMN) != 0);
					c.setColumnInfo(ci);
					changedColumns.add(c);
				}
			}
		}

		Iterator<Column> columns = t.getColumns().iterator();
		while (columns.hasNext())
		{
			Column c = columns.next();
			if (c.getColumnInfo() == null)
			{
				// only create servoy sequences when this was a new table and there is only 1 pk column
				createNewColumnInfo(persistFactory, c, existingColumnInfo == 0 && t.getPKColumnTypeRowIdentCount() == 1);//was missing - create automatic sequences if missing
			}
		}

//		if (t.getRowIdentColumnsCount() == 0)
//		{
//			t.setHiddenInDeveloperBecauseNoPk(true);
//			s.setTableMarkedAsHiddenInDeveloper(t.getName(), true);
//		}
//		else s.setTableMarkedAsHiddenInDeveloper(t.getName(), tableInfo.hiddenInDeveloper);

		t.setMarkedAsMetaData(tableInfo.isMetaData);

		// let table editors and so on now that a columns are loaded
		t.fireIColumnsChanged(changedColumns);
	}

	public static void createNewColumnInfo(IPersistFactory persistFactory, Column c, boolean createMissingServoySequence) throws RepositoryException
	{
		int element_id = persistFactory.getNewElementID(null);
		ColumnInfo ci = new ColumnInfo(element_id, false);
		if (createMissingServoySequence && c.getRowIdentType() != Column.NORMAL_COLUMN && c.getSequenceType() == ColumnInfo.NO_SEQUENCE_SELECTED &&
			(Column.mapToDefaultType(c.getConfiguredColumnType().getSqlType()) == IColumnTypes.INTEGER ||
				Column.mapToDefaultType(c.getConfiguredColumnType().getSqlType()) == IColumnTypes.NUMBER))
		{
			ci.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
			ci.setAutoEnterSubType(ColumnInfo.SERVOY_SEQUENCE);
			ci.setSequenceStepSize(1);
		}
		ci.setFlags(c.getFlags()); // when column has no columninfo and no flags it will return Column.PK_COLUMN for db pk column.
		c.setColumnInfo(ci);
	}
}
