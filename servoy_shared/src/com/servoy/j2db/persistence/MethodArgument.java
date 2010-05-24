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
package com.servoy.j2db.persistence;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;


/**
 * @author jcompagner
 * 
 */
public class MethodArgument
{
	private static final String TAG_METHODARGUMENT = "methodargument";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";

	public static enum ArgumentType
	{
		String, Number, Boolean, Color, Exception, JSRecord, JSEvent, JSDataSet, Object, Other
	}

	private final String name;
	private final String description;
	private final ArgumentType type;

	public MethodArgument(String name, ArgumentType type, String description)
	{
		this.name = name;
		this.type = type;
		this.description = description;
	}

	public String getName()
	{
		return name;
	}

	public ArgumentType getType()
	{
		return type;
	}

	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return name + ':' + type.name() + " (" + (description == null ? "" : description) + ')';
	}

	public Element toXML()
	{
		Element root = DocumentHelper.createElement(TAG_METHODARGUMENT);
		root.addAttribute(ATTR_NAME, name);
		root.addAttribute(ATTR_TYPE, type.toString());
		root.addCDATA(description);
		return root;
	}

	public static MethodArgument fromXML(Element root)
	{
		if (!root.getName().equals(TAG_METHODARGUMENT)) return null;
		String name = root.attributeValue(ATTR_NAME);
		String typeStr = root.attributeValue(ATTR_TYPE);
		ArgumentType type = ArgumentType.valueOf(typeStr);
		String description = root.getText();
		MethodArgument marg = new MethodArgument(name, type, description);
		return marg;
	}
}
