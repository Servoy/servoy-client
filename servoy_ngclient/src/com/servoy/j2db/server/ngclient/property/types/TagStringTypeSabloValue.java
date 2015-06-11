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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sablo.BaseWebObject;
import org.sablo.IChangeListener;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Text;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link TagStringPropertyType} that do need to replace tags (%%x%%).
 * Handles any needed listeners and deals with to and from browser communications.
 *
 * @author acostescu
 */
public class TagStringTypeSabloValue extends BasicTagStringTypeSabloValue implements IDataLinkedPropertyValue
{

	protected String tagReplacedValue;
	protected IChangeListener changeMonitor;
	protected IServoyDataConverterContext dataConverterContext;
	protected final TargetDataLinks dataLinks;
	private Set<String> tagsDataLinks;

	public TagStringTypeSabloValue(String designValue, DataAdapterList dataAdapterList, IServoyDataConverterContext dataConverterContext,
		PropertyDescription pd, FormElement formElement, boolean initialFormElementBasedValue)
	{
		super(designValue, dataAdapterList);

		this.dataConverterContext = dataConverterContext;
		TargetDataLinks dl = null;
		if (initialFormElementBasedValue)
		{
			dl = (TargetDataLinks)formElement.getPreprocessedPropertyInfo(IDataLinkedType.class, pd);
		}
		if (dl == null)
		{
			dl = ((TagStringPropertyType)pd.getType()).getDataLinks(getDesignValue(), pd, dataConverterContext.getSolution(), formElement);
		}
		dataLinks = dl;

		updateTagReplacedValue();
	}

	@Override
	public String getTagReplacedValue()
	{
		return tagReplacedValue;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, BaseWebObject component)
	{
		this.changeMonitor = changeNotifier;
		getDataAdapterList().addDataLinkedProperty(this, dataLinks);
	}

	@Override
	public void detach()
	{
		getDataAdapterList().removeDataLinkedProperty(this);
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (updateTagReplacedValue())
		{
			changeMonitor.valueChanged();
		}
	}

	protected boolean updateTagReplacedValue()
	{
		String oldTagReplacedValue = tagReplacedValue;
		tagReplacedValue = Text.processTags(getDesignValue(), getDataAdapterList()); // shouldn't this be done after HTMLTagsConverter.convert?

		DataAdapterList dataAdapterList = getDataAdapterList();
		if (tagReplacedValue.contains("%%") || tagReplacedValue.startsWith("i18n:"))
		{
			Pair<String, TargetDataLinks> replaced = TagStringPropertyType.processTags(tagReplacedValue, dataAdapterList, dataConverterContext);
			tagReplacedValue = replaced.getLeft();

			if (tagsDataLinks == null || tagsDataLinks.size() != replaced.getRight().dataProviderIDs.length ||
				!tagsDataLinks.containsAll(new HashSet<String>(Arrays.asList(replaced.getRight().dataProviderIDs))))
			{
				dataAdapterList.removeDataLinkedProperty(this);
				dataAdapterList.addDataLinkedProperty(this, dataLinks.concatDataLinks(replaced.getRight()));
				tagsDataLinks = new HashSet<String>(Arrays.asList(replaced.getRight().dataProviderIDs));
			}
		}
		else if (tagsDataLinks != null)
		{
			//remove links if the dataprovider value doesn't contain tags anymore
			dataAdapterList.removeDataLinkedProperty(this);
			dataAdapterList.addDataLinkedProperty(this, dataLinks);
			tagsDataLinks = null;
		}

		// changed or not
		return ((oldTagReplacedValue != tagReplacedValue) && (oldTagReplacedValue == null || !oldTagReplacedValue.equals(tagReplacedValue)));
	}

}