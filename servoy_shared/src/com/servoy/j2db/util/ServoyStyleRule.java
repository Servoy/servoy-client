/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import java.util.List;

import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;

/**
 * CSS rule wrapper around actual css parser.
 * 
 * @author lvostinar
 *
 */
public class ServoyStyleRule implements IStyleRule
{
	private final CascadedStyle cascadedStyle;

	public ServoyStyleRule(CascadedStyle cascadedStyle)
	{
		this.cascadedStyle = cascadedStyle;
	}

	public int getAttributeCount()
	{
		return cascadedStyle.countAssigned();
	}

	public List<String> getAttributeNames()
	{
		List<String> names = new ArrayList<String>();
		for (java.util.Iterator i = cascadedStyle.getCascadedPropertyDeclarations(); i.hasNext();)
		{
			PropertyDeclaration pd = (PropertyDeclaration)i.next();
			names.add(pd.getPropertyName());
		}
		return names;
	}

	public boolean hasAttribute(String attributeName)
	{
		CSSName cssName = CSSName.getByPropertyName(attributeName);
		if (cssName != null)
		{
			return cascadedStyle.hasProperty(cssName);
		}
		return false;
	}

	public String getValue(String attributeName)
	{
		CSSName cssName = CSSName.getByPropertyName(attributeName);
		if (cssName != null && cascadedStyle.hasProperty(cssName))
		{
			CSSPrimitiveValue value = cascadedStyle.propertyByName(cssName).getValue();
			if (value instanceof PropertyValue && ((PropertyValue)value).getValues() != null && ((PropertyValue)value).getValues().size() > 0)
			{
				StringBuilder builder = new StringBuilder();
				for (Object innerValue : ((PropertyValue)value).getValues())
				{
					if (builder.length() > 0)
					{
						builder.append(" ");
					}
					builder.append(((CSSPrimitiveValue)innerValue).getCssText());
				}
				return builder.toString();
			}
			else
			{
				return value.getCssText();
			}
		}
		return null;
	}

	public PropertyDeclaration getPropertyDeclaration(String attributeName)
	{
		CSSName cssName = CSSName.getByPropertyName(attributeName);
		if (cssName != null && cascadedStyle.hasProperty(cssName))
		{
			return cascadedStyle.propertyByName(cssName);
		}
		return null;
	}
}
