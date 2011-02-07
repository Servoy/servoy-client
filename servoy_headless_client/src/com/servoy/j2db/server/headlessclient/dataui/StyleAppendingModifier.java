/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.server.headlessclient.dataui;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.server.headlessclient.dataui.TemplateGenerator.TextualStyle;

/**
 * Simple {@link AttributeModifier} that can creates a style attribute for a tag.
 * 
 * @author jcompagner
 * 
 */
public class StyleAppendingModifier extends AttributeModifier
{
	private static final long serialVersionUID = 1L;

	/**
	 * Construct.
	 * 
	 * @param attribute
	 * @param replaceModel
	 */
	public StyleAppendingModifier(IModel replaceModel)
	{
		super("style", true, replaceModel);
	}

	public StyleAppendingModifier(TextualStyle style)
	{
		this(new Model<String>(style.getOnlyProperties().intern()));
	}

	@Override
	protected String newValue(final String currentValue, String replacementValue)
	{
		LinkedHashMap<String, String> currentProperties = parseStyleString(currentValue);
		LinkedHashMap<String, String> replacementProperties = parseStyleString(replacementValue);
		currentProperties.putAll(replacementProperties);

		StringBuffer sb = new StringBuffer();
		for (String propKey : currentProperties.keySet())
		{
			sb.append(propKey);
			sb.append(":"); //$NON-NLS-1$
			sb.append(currentProperties.get(propKey));
			sb.append(";"); //$NON-NLS-1$
		}
		return sb.toString().length() == 0 ? null : sb.toString();
	}

	private static final Pattern reCssProperty = Pattern.compile("^([^:]*):(.*)$"); //$NON-NLS-1$

	private static LinkedHashMap<String, String> parseStyleString(String styleString)
	{
		LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
		if (styleString == null) return properties;
		String[] sProperties = styleString.split(";");
		for (int i = 0; i < sProperties.length; i++)
		{
			String property = sProperties[i];
			Matcher mOneProperty = reCssProperty.matcher(property);
			if (mOneProperty.find())
			{
				String propertyName = mOneProperty.group(1);
				if (propertyName.trim().length() > 0)
				{
					String propertyValue = mOneProperty.group(2);
					properties.put(propertyName.trim(), propertyValue.trim());
				}
			}
			else if (i > 0)
			{
				for (int k = i - 1; k >= 0; k--)
				{
					String previousProperty = sProperties[k];
					Matcher prevOneProperty = reCssProperty.matcher(previousProperty);
					if (prevOneProperty.find())
					{
						String propertyName = prevOneProperty.group(1);
						String propertyValue = properties.get(propertyName);
						if (propertyValue != null)
						{
							propertyValue += ";" + property.trim();
							properties.put(propertyName, propertyValue);
						}
						break;
					}
				}
			}
		}
		return properties;
	}
}
