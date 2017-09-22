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

package com.servoy.j2db.server.ngclient.property;

public class DataproviderConfig
{
	public static final String DISPLAY_TAGS_PROPERTY_NAME_CONFIG_OPT = "displayTagsPropertyName"; //$NON-NLS-1$
	public static final String DISPLAY_TAGS_CONFIG_OPT = "displayTags"; //$NON-NLS-1$
	public static final String RESOLVE_VALUELIST_CONFIG_OPT = "resolveValuelist"; //$NON-NLS-1$

	private final String onDataChange;
	private final String onDataChangeCallback;
	private final boolean parseHtml;
	private final String displayTagsPropertyName;
	private final boolean displayTags;
	private final boolean resolveValuelist;

	public DataproviderConfig(String onDataChange, String onDataChangeCallback, boolean parseHtml, String displayTagsPropertyName, boolean displayTags,
		boolean resolveValuelist)
	{
		this.onDataChange = onDataChange;
		this.onDataChangeCallback = onDataChangeCallback;
		this.parseHtml = parseHtml;
		this.displayTagsPropertyName = displayTagsPropertyName;
		this.displayTags = displayTags;
		this.resolveValuelist = resolveValuelist;
	}

	public String getOnDataChange()
	{
		return onDataChange;
	}

	public String getOnDataChangeCallback()
	{
		return onDataChangeCallback;
	}

	public boolean hasParseHtml()
	{
		return parseHtml;
	}

	public String getDisplayTagsPropertyName()
	{
		return displayTagsPropertyName;
	}

	public boolean shouldDisplayTags()
	{
		return displayTags;
	}

	public boolean shouldResolveValuelist()
	{
		return resolveValuelist;
	}
}