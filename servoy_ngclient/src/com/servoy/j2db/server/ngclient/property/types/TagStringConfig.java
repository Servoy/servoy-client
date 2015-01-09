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

/**
 * @author jcompagner
 *
 */
public class TagStringConfig
{

	private final String displayTagsPropertyName;
	private final boolean displayTags;
	private final boolean useParsedValueInRhino;

	private final String forFoundSet;

	/**
	 * @param displayTagsPropertyName Can be null. If non-null then the "tagstring" typed property will initially read the boolean value the property with name [displayTagsPropertyName].
	 * If that property is true, tags (%%x%%) will be replaced, otherwise they will be ignored (in which case it will just replace i18n). If null, tags will be replaced or not based on config option "displayTags".
	 * @param displayTags if displayTagsPropertyName is non-null it will be ignored. Otherwise, if true, this property will parse tags (%%x%%) otherwise it won't.
	 * @param useParsedValueInRhino rhino JS scripting set/get will work with: if "false" the parsed (with tags/i18n already replaced, so static) value, if true the non-parsed (value containing %%x%% or i18n:..., which will be parsed) value.
	 * @param forFoundSet
	 */
	public TagStringConfig(String displayTagsPropertyName, boolean displayTags, boolean useParsedValueInRhino, String forFoundSet)
	{
		this.displayTagsPropertyName = displayTagsPropertyName;
		this.displayTags = displayTags;
		this.useParsedValueInRhino = useParsedValueInRhino;
		this.forFoundSet = forFoundSet;
	}

	public String getDisplayTagsPropertyName()
	{
		return displayTagsPropertyName;
	}

	public boolean shouldDisplayTags()
	{
		return displayTags;
	}

	public boolean useParsedValueInRhino()
	{
		return useParsedValueInRhino;
	}

	public String getForFoundSet()
	{
		return forFoundSet;
	}

}
