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
@SuppressWarnings("nls")
public class MethodArgument
{
	private static final String TAG_METHODARGUMENT = "methodargument";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";

	public static class ArgumentType
	{
		public static final ArgumentType String = new ArgumentType("String");
		public static final ArgumentType Number = new ArgumentType("Number");
		public static final ArgumentType Boolean = new ArgumentType("Boolean");
		public static final ArgumentType Color = new ArgumentType("Color");
		public static final ArgumentType Exception = new ArgumentType("Exception");
		public static final ArgumentType JSRecord = new ArgumentType("JSRecord");
		public static final ArgumentType JSEvent = new ArgumentType("JSEvent");
		public static final ArgumentType JSDataSet = new ArgumentType("JSDataSet");
		public static final ArgumentType Object = new ArgumentType("Object");

		private final String name;

		/**
		 * @param string2
		 */
		private ArgumentType(String name)
		{
			this.name = name;
		}

		/**
		 * @return the name
		 */
		public String getName()
		{
			return name;
		}

		@Override
		public String toString()
		{
			return getName();
		}

		/**
		 * @param typeStr
		 * @return
		 */
		public static ArgumentType valueOf(String type)
		{
			if (type == null || Object.getName().equals(type)) return Object;
			if (String.getName().equals(type)) return String;
			if (Number.equals(type)) return Number;
			if (Boolean.getName().equals(type)) return Boolean;
			if (Color.getName().equals(type)) return Color;
			if (Exception.getName().equals(type)) return Exception;
			if (JSRecord.getName().equals(type)) return JSRecord;
			if (JSEvent.getName().equals(type)) return JSEvent;
			if (JSDataSet.getName().equals(type)) return JSDataSet;
			return new ArgumentType(type);
		}

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
		return name + ':' + type.getName() + " (" + (description == null ? "" : description) + ')';
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
