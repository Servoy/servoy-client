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

package com.servoy.j2db.server.ngclient.property.types;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.IContextProvider;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetTypeSabloValue;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent;

/**
 * Property type similar to TagStringPropertyType, with the addition that it uses as default value
 * the title property of the db column, if it is linked to a db based dataprovider
 *
 * @author gboros
 */
public class TitleStringPropertyType extends TagStringPropertyType implements IFormElementDefaultValueToSabloComponent<String, BasicTagStringTypeSabloValue>
{
	public static final TitleStringPropertyType NG_INSTANCE = new TitleStringPropertyType();
	public static final String NG_TYPE_NAME = "titlestring";

	protected TitleStringPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return NG_TYPE_NAME;
	}

	@Override
	public TitleStringConfig parseConfig(JSONObject json)
	{
		TagStringConfig tagStringConfig = super.parseConfig(json);
		String forDataprovider = null;
		if (json != null)
		{
			forDataprovider = json.optString(TitleStringConfig.FOR_DATAPROVIDER_CONFIG_OPT, null);
		}
		return new TitleStringConfig(tagStringConfig.getDisplayTagsPropertyName(), tagStringConfig.shouldDisplayTags(), tagStringConfig.useParsedValueInRhino(),
			forDataprovider);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementDefaultValueToSabloComponent#toSabloComponentDefaultValue(org.sablo.
	 * specification.PropertyDescription, com.servoy.j2db.server.ngclient.INGFormElement, com.servoy.j2db.server.ngclient.WebFormComponent,
	 * com.servoy.j2db.server.ngclient.DataAdapterList)
	 */
	@Override
	public BasicTagStringTypeSabloValue toSabloComponentDefaultValue(PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		TitleStringConfig titleStringConfig = (TitleStringConfig)pd.getConfig();
		String forDataprovider = titleStringConfig.getForDataprovider();
		if (forDataprovider != null)
		{
			PropertyDescription forProperty = formElement.getPropertyDescription(forDataprovider);
			if (forProperty != null)
			{
				IPropertyType< ? > type = forProperty.getType();
				if (type instanceof FoundsetLinkedPropertyType)
				{
					Object config = forProperty.getConfig();
					if (config instanceof FoundsetLinkedConfig && ((FoundsetLinkedConfig)config).getForFoundsetName() != null)
					{
						String forFoundset = ((FoundsetLinkedConfig)config).getForFoundsetName();
						String dataproviderID = (String)formElement.getPropertyValue(forDataprovider);
						JSONObject foundsetValue = (JSONObject)formElement.getPropertyValue(forFoundset);
						if (foundsetValue != null)
						{
							String foundsetID = foundsetValue.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
							INGApplication application = ((WebFormComponent)component.getUnderlyingWebObject()).getDataConverterContext().getApplication();
							Form form = ((IContextProvider)component.getUnderlyingWebObject()).getDataConverterContext().getForm().getForm();
							ITable table = FoundsetTypeSabloValue.getTableBasedOfFoundsetPropertyFromFoundsetIdentifier(foundsetID, application, form);
							if (table != null)
							{
								Column dataproviderColumn = table.getColumn(dataproviderID);
								if (dataproviderColumn != null)
								{
									return toSabloComponentValue(dataproviderColumn.getTitle(), pd, formElement, component, dataAdapterList);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
}
