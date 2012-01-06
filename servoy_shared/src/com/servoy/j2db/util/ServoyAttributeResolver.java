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

import org.xhtmlrenderer.css.extend.AttributeResolver;

public class ServoyAttributeResolver implements AttributeResolver
{

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getAttributeValue(java.lang.Object, java.lang.String)
	 */
	public String getAttributeValue(Object e, String attrName)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getAttributeValue(java.lang.Object, java.lang.String, java.lang.String)
	 */
	public String getAttributeValue(Object e, String namespaceURI, String attrName)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getClass(java.lang.Object)
	 */
	public String getClass(Object e)
	{
		String elem = (String)e;
		int index = elem.lastIndexOf('.');
		if (index != -1)
		{
			return elem.substring(index + 1);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getID(java.lang.Object)
	 */
	public String getID(Object e)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getNonCssStyling(java.lang.Object)
	 */
	public String getNonCssStyling(Object e)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getElementStyling(java.lang.Object)
	 */
	public String getElementStyling(Object e)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#getLang(java.lang.Object)
	 */
	public String getLang(Object e)
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#isLink(java.lang.Object)
	 */
	public boolean isLink(Object e)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#isVisited(java.lang.Object)
	 */
	public boolean isVisited(Object e)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#isHover(java.lang.Object)
	 */
	public boolean isHover(Object e)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#isActive(java.lang.Object)
	 */
	public boolean isActive(Object e)
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xhtmlrenderer.css.extend.AttributeResolver#isFocus(java.lang.Object)
	 */
	public boolean isFocus(Object e)
	{
		return false;
	}

}