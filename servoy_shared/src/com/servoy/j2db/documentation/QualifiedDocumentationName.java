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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Models a link from any text of a documentation to a documented element.
 * The target of the link can be either a class, or a method. If the target
 * is a method, then the signature of the method is relevant, so that overloading
 * is supported.
 * 
 * @author gerzse
 */
public class QualifiedDocumentationName implements Comparable<QualifiedDocumentationName>
{
	public static final String JS_PREFIX = "js_"; //$NON-NLS-1$
	public static final String JS_FUNCTION_PREFIX = "jsFunction_"; //$NON-NLS-1$

	public static final String[] NO_ARGUMENTS = new String[] { };

	private static final String ATTR_OBJECT = "object"; //$NON-NLS-1$
	private static final String ATTR_MEMBER = "member"; //$NON-NLS-1$
	private static final String ATTR_ARGUMENTSTYPES = "argumentsTypes"; //$NON-NLS-1$

	private final String className;
	private final String memberName;
	private final String[] argumentsTypes;

	/**
	 * This constructor should never be called directly. Use the provided static methods 
	 * for creating instances of this class.
	 */
	private QualifiedDocumentationName(String className, String memberName, String[] argumentsTypes)
	{
		if (className == null)
		{
			throw new IllegalArgumentException("Class name cannot be null."); //$NON-NLS-1$
		}
		if (memberName == null)
		{
			if (argumentsTypes != null) throw new IllegalArgumentException("If no method is specified, then you can't specify a signature."); //$NON-NLS-1$
		}
		else
		{
			if (argumentsTypes == null) throw new IllegalArgumentException("Signature cannot be null if a method is specified."); //$NON-NLS-1$
		}
		this.className = className;
		this.argumentsTypes = argumentsTypes;

		if (memberName != null && memberName.startsWith(JS_PREFIX)) this.memberName = memberName.substring(JS_PREFIX.length());
		else if (memberName != null && memberName.startsWith(JS_FUNCTION_PREFIX)) this.memberName = memberName.substring(JS_FUNCTION_PREFIX.length());
		else this.memberName = memberName;
	}

	public String getClassName()
	{
		return className;
	}

	public String getMemberName()
	{
		return memberName;
	}

	public String[] getArgumentsTypes()
	{
		return argumentsTypes;
	}

	public int compareTo(QualifiedDocumentationName o)
	{
		int result = this.className.compareTo(o.className);
		if (result == 0 && memberName != null)
		{
			if (o.memberName == null)
			{
				result = 1;
			}
			else
			{
				result = this.memberName.compareTo(o.memberName);
			}
		}
		if (result == 0 && argumentsTypes != null)
		{
			if (o.argumentsTypes == null)
			{
				result = 1;
			}
			else
			{
				if (this.argumentsTypes.length < o.argumentsTypes.length)
				{
					result = -1;
				}
				else if (this.argumentsTypes.length > o.argumentsTypes.length)
				{
					result = 1;
				}
				else
				{
					for (int i = 0; i < this.argumentsTypes.length; i++)
					{
						int sigcmp = this.argumentsTypes[i].compareTo(o.argumentsTypes[i]);
						if (sigcmp != 0)
						{
							result = sigcmp;
							break;
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
		{
			return false;
		}
		else
		{
			if (other instanceof QualifiedDocumentationName)
			{
				QualifiedDocumentationName qother = (QualifiedDocumentationName)other;
				return this.compareTo(qother) == 0;
			}
			else
			{
				return false;
			}
		}
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(className);
		if (memberName != null)
		{
			sb.append("#").append(memberName); //$NON-NLS-1$
			if (argumentsTypes != NO_ARGUMENTS)
			{
				sb.append("("); //$NON-NLS-1$
				for (int i = 0; i < argumentsTypes.length; i++)
				{
					if (i > 0) sb.append(","); //$NON-NLS-1$
					sb.append(argumentsTypes[i]);
				}
				sb.append(")"); //$NON-NLS-1$
			}
			else
			{
				sb.append("[no arguments]");
			}
		}
		return sb.toString();
	}

	public Element toXML(String elementName)
	{
		Element root = DocumentHelper.createElement(elementName);
		if (className != null) root.addAttribute(ATTR_OBJECT, className);
		if (memberName != null)
		{
			root.addAttribute(ATTR_MEMBER, memberName);
			String argsAsString = argumentsArray2String(argumentsTypes);
//			if (argsAsString != null) root.addAttribute(ATTR_ARGUMENTSTYPES, argsAsString);
		}
		return root;
	}

	public static QualifiedDocumentationName fromXML(Element element)
	{
		String objectName = element.attributeValue(ATTR_OBJECT);
		String memberName = element.attributeValue(ATTR_MEMBER);
		String[] args = null;
		if (memberName != null)
		{
			String argsAsString = element.attributeValue(ATTR_ARGUMENTSTYPES);
			args = argumentsString2Array(argsAsString);
		}
		return new QualifiedDocumentationName(objectName, memberName, args);
	}

	public static QualifiedDocumentationName fromString(String text, String defaultTargetObject)
	{
		String objName = null;
		String memberName = null;
		String[] args = null;

		Pattern pattern = Pattern.compile("^([^\\(]+)\\(([^\\)]*)\\)$"); //$NON-NLS-1$

		int firstIndex = text.indexOf('#');
		int lastIndex = text.lastIndexOf('#');
		if (firstIndex != lastIndex)
		{
			System.out.println("More than one # characters used in documentation reference '" + text + "' at positions " + firstIndex + " and " + lastIndex + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				" in object '" + defaultTargetObject + "'."); //$NON-NLS-1$ //$NON-NLS-2$
			objName = text.substring(0, firstIndex).trim();
		}
		else
		{
			// If there is no # then we have either an object name, either a member name.
			if (lastIndex < 0)
			{
				// If there are no dots in the text, then it's only a method name. Use the default object.
				if (text.indexOf(".") < 0) //$NON-NLS-1$
				{
					objName = defaultTargetObject;
					memberName = text.trim();
				}
				// If there are dots, then it's an object name, without a member.
				else
				{
					objName = text.trim();
				}
			}
			else
			{
				objName = text.substring(0, lastIndex).trim();
				memberName = text.substring(lastIndex + 1).trim();
			}

			if (memberName != null)
			{
				// If there are arguments, parse them.
				Matcher matcher = pattern.matcher(memberName);
				if (matcher.find())
				{
					memberName = matcher.group(1);
					String rawArgs = matcher.group(2);
					args = argumentsString2Array(rawArgs);
				}
				else
				{
					args = NO_ARGUMENTS;
				}

				// Remove js_ prefixes from method name.
				if (memberName.startsWith("js_")) memberName = memberName.substring("js_".length()); //$NON-NLS-1$//$NON-NLS-2$
				if (memberName.startsWith("jsFunction_")) memberName = memberName.substring("jsFunction_".length()); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		QualifiedDocumentationName result = new QualifiedDocumentationName(objName, memberName, args);
		return result;
	}

	public static String[] argumentsString2Array(String args)
	{
		if (args == null)
		{
			return NO_ARGUMENTS;
		}
		else
		{
			if (args.trim().length() == 0)
			{
				return NO_ARGUMENTS;
			}
			else
			{
				String[] parts = args.split(","); //$NON-NLS-1$
				List<String> collectedParts = new ArrayList<String>();
				for (String part : parts)
					collectedParts.add(part.trim());
				String[] result = new String[collectedParts.size()];
				collectedParts.toArray(result);
				return result;
			}
		}
	}

	public static String argumentsArray2String(String[] args)
	{
		if (args == null || args == NO_ARGUMENTS)
		{
			return null;
		}
		else
		{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < args.length; i++)
			{
				if (i > 0) sb.append(","); //$NON-NLS-1$
				sb.append(args[i]);
			}
			return sb.toString();
		}
	}
}
