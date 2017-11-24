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
package com.servoy.j2db.documentation.scripting.docs;

import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "XMLList")
public class XMLList
{
	/**
	 * It calls the method attribute of each object in this XMLList and returns the results in order 
	 * in an XMLList.
	 *
	 * @sample xmlList.attribute(attributeName)
	 * 
	 * @param attributeName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_attribute(String attributeName)
	{
		return null;
	}

	/**
	 * Calls the method attributes of each object in this XMLList and returns an XMLList with 
	 * the results in order.
	 *
	 * @sample xmlList.attributes()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_attributes()
	{
		return null;
	}

	/**
	 * Calls the method child of each XML object in this XMLList object to return an XMLList 
	 * with the matching children in order.
	 *
	 * @sample xmlList.child(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_child(String propertyName)
	{
		return null;
	}

	/**
	 * Returns an XMLList with the children of all XML objects in this XMLList.
	 *
	 * @sample xmlList.children()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_children()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all the comment child nodes of XML objects in this XMLList in order.
	 *
	 * @sample xmlList.comments()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_comments()
	{
		return null;
	}

	/**
	 * Returns true if there is (at least) one XML object in the list that compares equal to the value
	 *
	 * @sample xmlList.contains(value)
	 * 
	 * @param value 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_contains(Object value)
	{
		return null;
	}

	/**
	 * Returns a deep copy of the XMLList it is called on.
	 *
	 * @sample xmlList.copy()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_copy()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all of the matching descendants of all XML objects.
	 *
	 * @sample xmlList.descendants([name])
	 *  
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_descendants()
	{
		return null;
	}

	/**
	 * @clonedesc js_descendants()
	 * @sampleas js_descendants()
	 * 
	 * @param name
	 * 
	 */
	public XMLList js_descendants(String name)
	{
		return null;
	}

	/**
	 * Returns an XMLList with the matching element children of all XML objects in this XMLList.
	 *
	 * @sample xmlList.elements([name])
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_elements(String name)
	{
		return null;
	}

	/**
	 * Returns true if the XMLList contains exactly one XML object which has complex content or if 
	 * the XMLList contains several XML objects.
	 *
	 * @sample xmlList.hasComplexContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_hasComplexContent()
	{
		return null;
	}

	/**
	 * Returns true if the XMLList object has a property of that name and false otherwise.
	 *
	 * @sample xmlList.hasOwnProperty(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_hasOwnProperty(String propertyName)
	{
		return null;
	}

	/**
	 * Returns true if the XMLList is empty or contains exactly one XML object which has simple 
	 * content or contains no elements at all.
	 *
	 * @sample xmlList.hasSimpleContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_hasSimpleContent()
	{
		return null;
	}

	/**
	 * Returns the number of XML objects this XMLList contains.
	 *
	 * @sample xmlList.length()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Number js_length()
	{
		return null;
	}

	/**
	 * Returns the XMLList object it is called on after joining adjacent text nodes 
	 * and removing empty text nodes.
	 *
	 * @sample xmlList.normalize()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_normalize()
	{
		return null;
	}

	/**
	 * Returns the common parent of all XML objects in this XMLList if all those objects 
	 * have the same parent.
	 *
	 * @sample xmlList.parent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_parent()
	{
		return null;
	}

	/**
	 * Returns an XMLList with all the matching processing instruction child nodes of all 
	 * XML objects in this XMLList.
	 *
	 * @sample xmlList.processingInstructions([name])
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_processingInstructions()
	{
		return null;
	}

	/**
	 * @clonedesc js_processingInstructions()
	 * @sampleas js_processingInstructions()
	 * 
	 * @param name
	 * 
	 */
	public XMLList js_processingInstructions(String name)
	{
		return null;
	}

	/**
	 * Returns true if the property name converted to a number is greater than or equal to 
	 * 0 and less than the length of this XMLList.
	 *
	 * @sample xmlList.propertyIsEnumerable(propertyName)
	 * 
	 * @param propertyName 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_propertyIsEnumerable(String propertyName)
	{
		return null;
	}

	/**
	 * Returns an XMLList containing all the text child nodes of all the XML objects contained in this XMLList.
	 *
	 * @sample xmlList.text()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_text()
	{
		return null;
	}

	/**
	 * Returns a string representation of the XMLList
	 *
	 * @sample xmlList.toString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public java.lang.String js_toString()
	{
		return null;
	}

	/**
	 * Returns the concatenation of toXMLString called on each XML object. The result for each XML 
	 * object is put on a separate line if XML.prettyPrinting is true.
	 *
	 * @sample xmlList.toXMLString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_toXMLString()
	{
		return null;
	}

	/**
	 * Simply returns the XMLList object it is called on.
	 *
	 * @sample xmlList.valueOf()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_valueOf()
	{
		return null;
	}
}
