/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.j2db.server.ngclient.utils;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormAndTableDataProviderLookup;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.util.Debug;

/**
 * Utility methods for NGClient.
 *
 * @author acostescu
 */
public abstract class NGUtils
{

	public static final PropertyDescription DATAPROVIDER_PD = new PropertyDescription("some dp", DataproviderPropertyType.INSTANCE);

	public static PropertyDescription getDataProviderPropertyDescription(String dataProviderName, ITable table)
	{
		if (table == null || dataProviderName == null) return null;
		return getDataProviderPropertyDescription(table.getColumnType(dataProviderName));
	}

	public static PropertyDescription getDataProviderPropertyDescription(String dataProviderName, FlattenedSolution flattenedSolution, Form form, ITable table)
	{
		FormAndTableDataProviderLookup dpLookup = new FormAndTableDataProviderLookup(flattenedSolution, form, table);
		IDataProvider dp = null;
		try
		{
			dp = dpLookup.getDataProvider(dataProviderName);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		if (dp != null) return getDataProviderPropertyDescription(dp.getDataProviderType());
		return null;
	}

	public static PropertyDescription getDataProviderPropertyDescription(int type)
	{
		// column/var types might be needed in the future when DataproviderPropertyType is more aware of them
		IPropertyType< ? > propType = null;
		if (type == IColumnTypes.DATETIME)
		{
			propType = TypesRegistry.getType(DatePropertyType.TYPE_NAME);
		}
//		else if (type == IColumnTypes.MEDIA)
//		{
//			// this is byte array, not url or int media type (so media from repo which would be MediaPropertyType)
////			propType = TypesRegistry.getType(MediaPropertyType.TYPE_NAME);
//		}
//		else if (type == IColumnTypes.INTEGER)
//		{
//			propType = TypesRegistry.getType(LongPropertyType.TYPE_NAME);
//		}
//		else if (type == IColumnTypes.NUMBER)
//		{
//			propType = TypesRegistry.getType(DoublePropertyType.TYPE_NAME);
//		}
//		else if (type == IColumnTypes.TEXT)
//		{
//			propType = TypesRegistry.getType(StringPropertyType.TYPE_NAME);
//		}

		// TODO in the future all will probably be DATAPROVIDER_PD but type-aware ones; for now DataproviderPropertyType for example doesn't handle correctly dates
		return propType != null ? new PropertyDescription("generated DP prop", propType) : DATAPROVIDER_PD;
	}

}
