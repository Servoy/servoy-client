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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public class MethodArgument implements IMethodArgument
{
	public static final String TAG_METHODARGUMENT = "methodargument";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";

	private final String name;
	private final String description;
	private final ArgumentType type;
	private final boolean isOptional;

	public MethodArgument(String name, ArgumentType type, String description)
	{
		this(name, type, description, false);
	}

	public MethodArgument(String name, ArgumentType type, String description, boolean isOptional)
	{
		this.name = name;
		this.type = type;
		this.description = description;
		this.isOptional = isOptional;
	}

	public MethodArgument(IMethodArgument arg)
	{
		this(arg.getName(), arg.getType(), arg.getDescription(), arg.isOptional());
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

	public boolean isOptional()
	{
		return isOptional;
	}

	@Override
	public String toString()
	{
		return name + ':' + type.getName() + " (" + (description == null ? "" : description) + ')';
	}

	public Element toXML(Document doc)
	{
		Element root = doc.createElement(TAG_METHODARGUMENT);
		root.setAttribute(ATTR_NAME, name);
		root.setAttribute(ATTR_TYPE, type.toString());
		root.appendChild(doc.createCDATASection(description));
		return root;
	}

	public static MethodArgument fromXML(Element root)
	{
		if (!root.getNodeName().equals(TAG_METHODARGUMENT)) return null;
		String name = root.getAttribute(ATTR_NAME);
		String typeStr = root.getAttribute(ATTR_TYPE);
		ArgumentType type = ArgumentType.valueOf(typeStr);
		String description = root.getTextContent();
		MethodArgument marg = new MethodArgument(name, type, description);
		return marg;
	}
}
