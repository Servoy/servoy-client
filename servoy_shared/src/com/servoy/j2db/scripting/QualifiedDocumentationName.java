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
package com.servoy.j2db.scripting;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;


public class QualifiedDocumentationName implements Comparable<QualifiedDocumentationName>
{
	private static final String ATTR_OBJECT = "object";
	private static final String ATTR_MEMBER = "member";

	private final String className;
	private final String methodName;

	public QualifiedDocumentationName(String className, String methodName)
	{
		if (className == null) throw new IllegalArgumentException("Class name cannot be null."); //$NON-NLS-1$
		//if (methodName == null) throw new IllegalArgumentException("Method name cannot be null"); //$NON-NLS-1$
		this.className = className;
		this.methodName = methodName;
	}

	public String getClassName()
	{
		return className;
	}

	public String getMethodName()
	{
		return methodName;
	}

	public int compareTo(QualifiedDocumentationName o)
	{
		int result = this.className.compareTo(o.className);
		if (result == 0) result = this.methodName.compareTo(o.methodName);
		return result;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("[").append(className).append(",").append(methodName).append("]"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		return sb.toString();
	}

	public Element toXML(String elementName)
	{
		Element root = DocumentHelper.createElement(elementName);
		if (className != null) root.addAttribute(ATTR_OBJECT, className);
		if (methodName != null) root.addAttribute(ATTR_MEMBER, methodName);
		return root;
	}

	public static QualifiedDocumentationName fromXML(Element root)
	{
		String objectName = root.attributeValue(ATTR_OBJECT);
		String memberName = root.attributeValue(ATTR_MEMBER);
		return new QualifiedDocumentationName(objectName, memberName);
	}
}
