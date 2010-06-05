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
package com.servoy.j2db.documentation;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class ParameterDocumentation
{
	// top level tags
	private static final String TAG_PARAMETER = "parameter"; //$NON-NLS-1$

	// top level attributes
	private static final String ATTR_OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$

	private static final String TAG_DESCRIPTION = "description"; //$NON-NLS-1$

	private final String name;
	private final String type;
	private final String description;
	private final boolean optional;

	public ParameterDocumentation(String name, String type, String description, boolean optional)
	{
		this.name = name;
		this.type = type;
		this.description = description;
		this.optional = optional;
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public String getDescription()
	{
		return description;
	}

	public boolean isOptional()
	{
		return optional;
	}

	public Element toXML()
	{
		Element argumentElement = DocumentHelper.createElement(TAG_PARAMETER);

		argumentElement.addAttribute(ATTR_NAME, getName());
		if (getType() != null && getType().trim().length() > 0) argumentElement.addAttribute(ATTR_TYPE, getType());
		if (isOptional()) argumentElement.addAttribute(ATTR_OPTIONAL, Boolean.TRUE.toString());

		if (getDescription() != null && getDescription().trim().length() > 0) argumentElement.addElement(TAG_DESCRIPTION).addCDATA(getDescription());

		return argumentElement;
	}

	public static ParameterDocumentation fromXML(Element argumentElement)
	{
		if (!TAG_PARAMETER.equals(argumentElement.getName())) return null;

		String name = argumentElement.attributeValue(ATTR_NAME);
		String type = argumentElement.attributeValue(ATTR_TYPE);
		boolean optional = Boolean.TRUE.toString().equals(argumentElement.attributeValue(ATTR_OPTIONAL));

		String description = argumentElement.elementText(TAG_DESCRIPTION);

		return new ParameterDocumentation(name, type, description, optional);
	}
}
