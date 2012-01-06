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

import org.xhtmlrenderer.css.extend.TreeResolver;

public class ServoyTreeResolver implements TreeResolver
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#getParentElement(java.lang.Object)
	 */
	public Object getParentElement(Object element)
	{
		String elem = (String)element;
		int index = elem.lastIndexOf(' ');
		if (index > 0)
		{
			return elem.substring(0, index);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#getElementName(java.lang.Object)
	 */
	public String getElementName(Object element)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#getPreviousSiblingElement(java.lang.Object)
	 */
	public Object getPreviousSiblingElement(Object node)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#isFirstChildElement(java.lang.Object)
	 */
	public boolean isFirstChildElement(Object element)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#isLastChildElement(java.lang.Object)
	 */
	public boolean isLastChildElement(Object arg0)
	{
		return false;
	}

	public int getPositionOfElement(Object arg0)
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.TreeResolver#matchesElement(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public boolean matchesElement(Object element, String namespaceURI, String name)
	{
		String elem = (String)element;
		int index = elem.lastIndexOf(' ');
		if (index > 0)
		{
			elem = elem.substring(index + 1);
		}
		return elem.startsWith(name);
	}
}