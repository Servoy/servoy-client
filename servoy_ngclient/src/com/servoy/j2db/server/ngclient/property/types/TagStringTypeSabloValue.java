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

import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;

import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.HTMLTagsConverter;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.IDataLinkedPropertyValue;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * Runtime value stored in WebFormComponents for properties of type {@link TagStringPropertyType} that do need to replace tags (%%x%%).
 * Handles any needed listeners and deals with to and from browser communications.
 *
 * @author acostescu
 */
public class TagStringTypeSabloValue extends BasicTagStringTypeSabloValue implements IDataLinkedPropertyValue
{

	protected String tagReplacedValueBeforeHTMLConvert; // we keep this separate, not just the tagReplacedValueAfter... below in order to use it for change detection in updateTagReplacedValue()

	protected String tagReplacedValueAfterHTMLConvertForClient; // HTML conversion needs to be done or not based on computedPushToServer that unfortunately is available only in to/from client JSON conversion; so we might not have it from the start (if value is instantiated either from a formelement -> sablo or rhino -> sablo conversion)
	boolean htmlConvertForClientWasHandled = false;

	protected String tagReplacedValueAfterHTMLConvertForRhino;
	boolean htmlConvertForRhinoWasHandled = false;

	protected IChangeListener changeMonitor;
	protected IServoyDataConverterContext dataConverterContext;
	protected final TargetDataLinks dataLinks;
	private final PropertyDescription pd;
	private final FormElement formElement;

	/**
	 * @param computedPushToServer give this argument only if the caller has access to a {@link IBrowserConverterContext#getComputedPushToServerValue()}. Otherwise give null.
	 */
	public TagStringTypeSabloValue(String designValue, DataAdapterList dataAdapterList, IServoyDataConverterContext dataConverterContext,
		PropertyDescription pd, FormElement formElement)
	{
		super(designValue, dataAdapterList);

		this.dataConverterContext = dataConverterContext;
		this.pd = pd;
		this.formElement = formElement;
		dataLinks = ((TagStringPropertyType)pd.getType()).getDataLinks(getOperatingDesignValue(), pd, dataConverterContext.getSolution(), formElement);

		updateTagReplacedValue();
	}

	@Override
	public String getTagReplacedValueForRhino()
	{
		if (!htmlConvertForRhinoWasHandled)
		{
			if (HtmlUtils.startsWithHtml(tagReplacedValueBeforeHTMLConvert))
			{
				tagReplacedValueAfterHTMLConvertForRhino = HTMLTagsConverter.convert(tagReplacedValueBeforeHTMLConvert, dataConverterContext, false);
			}
			else tagReplacedValueAfterHTMLConvertForRhino = tagReplacedValueBeforeHTMLConvert;

			htmlConvertForRhinoWasHandled = true;
		}
		return tagReplacedValueAfterHTMLConvertForRhino;
	}

	@Override
	public String getTagReplacedValueForClient(PushToServerEnum computedPushToServer)
	{
		if (!htmlConvertForClientWasHandled)
		{
			if (HtmlUtils.startsWithHtml(tagReplacedValueBeforeHTMLConvert))
			{
				tagReplacedValueAfterHTMLConvertForClient = HTMLTagsConverter.convert(tagReplacedValueBeforeHTMLConvert, dataConverterContext, false);
			}
			else tagReplacedValueAfterHTMLConvertForClient = tagReplacedValueBeforeHTMLConvert;

			htmlConvertForClientWasHandled = true;
		}

		return tagReplacedValueAfterHTMLConvertForClient;
	}

	@Override
	public void attachToBaseObject(IChangeListener changeNotifier, IWebObjectContext webObjectContext)
	{
		this.changeMonitor = changeNotifier;
		getDataAdapterList().addDataLinkedProperty(this, dataLinks);
	}

	@Override
	public void detach()
	{
		getDataAdapterList().removeDataLinkedProperty(this);
		htmlConvertForClientWasHandled = false; // if it gets re-attached to a different place, it may be that computedPushToServer is different and we use that to determine if we do hml convert or not
		// htmlConvertForRhinoWasHandled flag should not be affected by this as it doesn't care about computedPushToServer
	}

	@Override
	public void dataProviderOrRecordChanged(IRecordInternal record, String dataProvider, boolean isFormDP, boolean isGlobalDP, boolean fireChangeEvent)
	{
		if (updateTagReplacedValue())
		{
			changeMonitor.valueChanged();
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof TagStringTypeSabloValue)
		{
			TagStringTypeSabloValue value = (TagStringTypeSabloValue)obj;
			return value.pd == pd && value.formElement == formElement &&
				Utils.equalObjects(value.getDesignValueBeforeInitialI18NConversion(), getDesignValueBeforeInitialI18NConversion());
		}
		return false;
	}

	protected boolean updateTagReplacedValue()
	{
		String oldTagReplacedValueBeforeHTMLConvert = tagReplacedValueBeforeHTMLConvert;
		tagReplacedValueBeforeHTMLConvert = Text.processTags(getOperatingDesignValue(), getDataAdapterList()); // shouldn't this be done after HTMLTagsConverter.convert?

		boolean changed = ((oldTagReplacedValueBeforeHTMLConvert != tagReplacedValueBeforeHTMLConvert) &&
			(oldTagReplacedValueBeforeHTMLConvert == null || !oldTagReplacedValueBeforeHTMLConvert.equals(tagReplacedValueBeforeHTMLConvert)));

		if (changed)
		{
			// do the HTML conversions once again - when needed
			htmlConvertForClientWasHandled = false;
			htmlConvertForRhinoWasHandled = false;
		}

		// changed or not
		return changed;
	}

}