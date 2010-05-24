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

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.servoy.j2db.util.Debug;

public class ReservedKeywordsList
{
	private static final String TAG_KEYWORDS = "keywords"; //$NON-NLS-1$
	private static final String TAG_KEYWORD = "keyword"; //$NON-NLS-1$
	private static final String ATTR_CATEGORY = "category"; //$NON-NLS-1$

	private final SortedSet<String> keywords;
	private final String category;

	public ReservedKeywordsList(String category)
	{
		this.keywords = new TreeSet<String>();
		this.category = category;
	}

	public String getCategory()
	{
		return category;
	}

	public void addKeyword(String keyword)
	{
		keywords.add(keyword);
	}

	public Element toXML()
	{
		Element root = DocumentHelper.createElement(TAG_KEYWORDS);
		root.addAttribute(ATTR_CATEGORY, category);
		for (String k : keywords)
		{
			root.addElement(TAG_KEYWORD).addText(k);
		}
		return root;
	}

	public static ReservedKeywordsList fromXML(Element root)
	{
		if (!root.getName().equals(TAG_KEYWORDS))
		{
			Debug.error("Please provide a <" + TAG_KEYWORDS + "> element for extracting keywords."); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}

		String category = root.attributeValue(ATTR_CATEGORY);
		if (category == null)
		{
			Debug.error("The <" + TAG_KEYWORDS + "> element has no attribute named '" + ATTR_CATEGORY + "'."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			return null;
		}

		ReservedKeywordsList result = new ReservedKeywordsList(category);
		Iterator<Element> kwIter = root.elementIterator();
		while (kwIter.hasNext())
		{
			Element kw = kwIter.next();
			result.addKeyword(kw.getTextTrim());
		}
		return result;
	}
}
