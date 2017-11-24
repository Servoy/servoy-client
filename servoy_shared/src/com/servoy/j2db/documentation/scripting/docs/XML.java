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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "XML", scriptingName = "XML")
public class XML
{
	/**
	 * If set to true, then comments in the XML are ignored when constructing new XML objects.
	 * 
	 * @sample
	 * var element = <foo><!-- my comment --><bar/></foo>;
	 * application.output(element.comments().length());
	 * application.output(element.toXMLString());
	 * 
	 * XML.ignoreComments = false;
	 * 
	 * element = <foo><!-- my comment --><bar/></foo>;
	 * application.output(element.comments().length());
	 * application.output(element.toXMLString()); 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_getIgnoreComments()
	{
		return null;
	}

	public void js_setIgnoreComments(Boolean ignoreComments)
	{
	}

	/**
	 * If set to true, then processing instructions are ignored when constructing new XML objects.
	 * 
	 * @sample
	 * XML.ignoreProcessingInstructions=false;
	 * var xmlElement = <publishing><?process author="yes"?><author type="leadership">John C. Maxwell</author></publishing>;
	 * application.output(" Element = "+ xmlElement.toXMLString());
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_getIgnoreProcessingInstructions()
	{
		return null;
	}

	public void js_setIgnoreProcessingInstructions(Boolean ignoreProcessingInstructions)
	{
	}

	/**
	 * If set to true, then whitespace in the XML is ignored when constructing new XML objects.
	 * 
	 * @sample
	 *  XML.ignoreWhitespace = false;
	 *  var xmlElement =
	 *  <publishing>
	 *  	<author>John C. Maxwell</author>
	 *  </publishing>;
	 *  application.output(xmlElement.toString());
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_getIgnoreWhitespace()
	{
		return null;
	}

	public void js_setIgnoreWhitespace(Boolean ignoreWhitespace)
	{
	}

	/**
	 * If set to true, then toString() and toXMLString() methods will normalize the output
	 * to achieve a uniform appearance.
	 * 
	 * @sampleas js_getPrettyIndent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_getPrettyPrinting()
	{
		return null;
	}

	public void js_setPrettyPrinting(Boolean prettyPrinting)
	{
	}

	/**
	 * The amount of positions used when indenting child nodes are relative to their parent
	 * if prettyPrinting is enabled.
	 * 
	 * @sample
	 * var xmlElement = <publishing><author>Tom DeMarco</author><author>Roger S. Pressman</author></publishing>;
	 * application.output(xmlElement.toXMLString());
	 * XML.prettyPrinting = true;
	 * XML.prettyIndent = 4;
	 * xmlElement = <publishing><author>Tom DeMarco</author><author>Roger S. Pressman</author></publishing>;
	 * application.output(xmlElement.toXMLString());
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_getPrettyIndent()
	{
		return null;
	}

	public void js_setPrettyIndent(Boolean prettyIndent)
	{
	}

	/**
	 * Takes one argument which can be a string with a namespace URI or a Namespace object and adds the 
	 * argument to the in scope namespaces of this XML object.
	 *
	 * @sample xml.addNamespace(namespaceToAdd)
	 * 
	 * @param namespaceToAdd 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_addNamespace(String namespaceToAdd)
	{
		return null;
	}

	/**
	 * Appends a new child at the end of this XML object's properties, the changed XML object is then returned.
	 *
	 * @sample xml.appendChild(childToAppend)
	 * 
	 * @param childToAppend 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_appendChild(XML childToAppend)
	{
		return null;
	}

	/**
	 * Takes a single argument with the attribute name and returns an XMLList with attributes 
	 * matching the argument.
	 *
	 * @sample xml.attribute(attributeName)
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
	 * Returns an XMLList with the attributes of this XML object which are in no namespace.
	 *
	 * @sample xml.attributes()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_attributes()
	{
		return null;
	}

	/**
	 * Returns an XMLList with children matching the property name.
	 *
	 * @sample xml.child(childPropertyName)
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
	 * If the XML object has no parent then the special number NaN is returned, otherwise the ordinal 
	 * position the object has in the context of its parent is returned.
	 *
	 * @sample xml.childIndex()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Number js_childIndex()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the child nodes of this XML object.
	 *
	 * @sample xml.children()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_children()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the comment nodes which are children of this XML object.
	 *
	 * @sample xml.comments()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_comments()
	{
		return null;
	}

	/**
	 * Calling xmlObject.contains(value) yields the same result as the equality comparison xmlObject == value
	 *
	 * @sample xml.contains(value)
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
	 * Returns a deep copy of the XML object it is called on where the internal parent property is set to null
	 *
	 * @sample xml.copy()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_copy()
	{
		return null;
	}

	/**
	 * Returns an object containing the default XML settings.
	 * 
	 * @sample xml.defaultSettings()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Object js_defaultSettings()
	{
		return null;
	}

	/**
	 * Returns an XMLList with the descendants matching the passed name argument or with all descendants 
	 * if no argument is passed.
	 *
	 * @sample xml.descendants([name])
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
	 * Takes one optional argument, the name of elements you are looking for, and returns an XMLList with 
	 * all matching child elements.
	 *
	 * @sample xml.elements([name])
	 *  
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_elements()
	{
		return null;
	}

	/**
	* @clonedesc js_elements()
	* @sampleas js_elements()
	 * 
	 * @param name
	 * 
	 */
	public XMLList js_elements(String name)
	{
		return null;
	}

	/**
	 * Returns false for XML objects of node kind 'text', 'attribute', 'comment', and 'processing-instruction'.
	 * For objects of kind 'element' it checks whether the element has at least one child element.
	 *
	 * @sample xml.hasComplexContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_hasComplexContent()
	{
		return null;
	}

	/**
	 * Returns true if the XML object the method is called on has a property of that name.
	 *
	 * @sample xml.hasOwnProperty(propertyName)
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
	 * Returns true for XML objects of node kind text or attribute. For XML objects of node kind 
	 * element it returns true if the element has no child elements and false otherwise.
	 * For other node kinds (comment, processing instruction) the method always returns false.
	 *
	 * @sample xml.hasSimpleContent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Boolean js_hasSimpleContent()
	{
		return null;
	}

	/**
	 * Returns an array of Namespace objects representing the namespace that are in scope for this XML object.
	 *
	 * @sample xml.inScopeNamespaces()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Array js_inScopeNamespaces()
	{
		return null;
	}

	/**
	 * Takes two arguments, an existing child to insert after and the new child to be inserted.
	 * If the first argument is null then the second argument is inserted as the first child of this XML.
	 * 
	 * @sample xml.insertChildAfter(childToInsertAfter, childToInsert)
	 * 
	 * @param childToInserAfter 
	 * @param childToInsert 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_insertChildAfter(XML childToInserAfter, XML childToInsert)
	{
		return null;
	}

	/**
	 * Takes two arguments, an existing child to insert before and the new child to be inserted.
	 * If the first argument is null then the child is inserted as the last child.
	 *
	 * @sample xml.insertChildBefore(childToInsertBefore, childToInsert)
	 * 
	 * @param childToInsertBefore 
	 * @param childToInsert 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_insertChildBefore(XML childToInsertBefore, XML childToInsert)
	{
		return null;
	}

	/**
	 * This always returns 1. This is done to blur the distinction between an XML object and an XMLList 
	 * containing exactly one value.
	 *
	 * @sample xml.length()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Number js_length()
	{
		return null;
	}

	/**
	 * returns the local name part if the XML object has a name.
	 *
	 * @sample xml.localName()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_localName()
	{
		return null;
	}

	/**
	 * Returns the qualified name (a QName object) of the XML object it is called
	 *
	 * @sample xml.name()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public QName js_name()
	{
		return null;
	}

	/**
	 * If no argument is passed to the method then it returns the namespace associated with the qualified 
	 * name of this XML object. If a prefix is passed to the method then it looks for a matching namespace 
	 * in the in scope namespace of this XML object and returns it when found, otherwise undefined is returned.
	 *
	 * @sample xml.namespace([prefix])
	 * 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Namespace js_namespace()
	{
		return null;
	}

	/**
	* @clonedesc js_namespace()
	* @sampleas js_namespace()
	 * 
	 * @param prefix
	 * 
	 */
	public Namespace js_namespace(String prefix)
	{
		return null;
	}

	/**
	 * Returns an array with the namespace declarations associated with the XML object it is called on.
	 *
	 * @sample xml.namespaceDeclarations()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Array js_namespaceDeclarations()
	{
		return null;
	}

	/**
	 * Returns a string denoting the kind of node this XML object represents. Possible values: 'element', 
	 * 'attribute', 'text', 'comment', 'processing-instruction'.
	 *
	 * @sample xml.nodeKind()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_nodeKind()
	{
		return null;
	}

	/**
	 * Returns this XML object after normalizing all text content.
	 *
	 * @sample xml.normalize()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_normalize()
	{
		return null;
	}

	/**
	 * Returns the parent XML object of this XML object or null if there is no parent.
	 *
	 * @sample xml.parent()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_parent()
	{
		return null;
	}

	/**
	 * Iinserts the given value as the first child of the XML object and returns the XML object.
	 *
	 * @sample xml.prependChild(childToPrepend)
	 * 
	 * @param childToPrepend 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_prependChild(XML childToPrepend)
	{
		return null;
	}

	/**
	 * If no argument is passed in then the method returns an XMLList with all the children of the XML 
	 * object which are processing instructions. If an argument is passed in then the method returns an 
	 * XMLList with all children of the XML object which are processing instructions where the name 
	 * matches the argument.
	 *
	 * @sample xml.processingInstructions([name])
	 * 
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
	 * Returns true if the property name is '0' and false otherwise.
	 *
	 * @sample xml.propertyIsEnumerable(propertyName)
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
	 * Removes the namespace from the in scope namespaces of this XML object if the namespace 
	 * is not used for the qualified name of the object or its attributes.
	 *
	 * @sample xml.removeNamespace(namespace)
	 * 
	 * @param namespace 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_removeNamespace(Namespace namespace)
	{
		return null;
	}

	/**
	 * Takes two arguments, the property name of the property / properties to be replaced, and the 
	 * value to replace the properties.
	 *
	 * @sample xml.replace(propertyName, replacementValue)
	 * 
	 * @param propertyName 
	 * @param replacementValue 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_replace(String propertyName, XML replacementValue)
	{
		return null;
	}

	/**
	 * Replaces all children of the XML object with this value. The method returns the XML object it 
	 * is called on.
	 *
	 * @sample xml.setChildren(value)
	 * 
	 * @param value 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_setChildren(Object value)
	{
		return null;
	}

	/**
	 * Changes the local name of this XML object to the name passed in.
	 *
	 * @sample xml.setLocalName(name)
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public void js_setLocalName(String name)
	{
	}

	/**
	 * Replaces the name of this XML object with the name passed in.
	 *
	 * @sample xml.setName(name)
	 * 
	 * @param name 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public void js_setName(String name)
	{
	}

	/**
	 * Changes the namespace associated with the name of this XML object to the new namespace.
	 *
	 * @sample xml.setNamespace(namespace)
	 * 
	 * @param namespace 
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public void js_setNamespace(Namespace namespace)
	{
	}

	/**
	 * Returns an object containing the global XML settings.
	 * 
	 * @sample xml.settings()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public Object js_settings()
	{
		return null;
	}

	/**
	 * Allows the global XML settings to be adjusted or restored to their default values.
	 * 
	 * @sample xml.setSettings(settings)
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public void js_setSettings()
	{
	}

	/**
	 * @clonedesc js_setSettings()
	 * @sampleas js_setSettings()
	 * 
	 * @param settings The new settings that should be applied globally to the XML object.
	 * 
	 */
	public void js_setSettings(Object settings)
	{
	}

	/**
	 * Returns an XMLList with all the children of this XML object that represent text nodes.
	 *
	 * @sample xml.text()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XMLList js_text()
	{
		return null;
	}

	/**
	 * Returns a convenient string value of this XML object.
	 *
	 * @sample xml.toString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public java.lang.String js_toString()
	{
		return null;
	}

	/**
	 * Returns a string with the serialized XML markup for this XML object. XML.prettyPrinting 
	 * and XML.prettyIndent settings affect the returned string.
	 *
	 * @sample xml.toXMLString()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_toXMLString()
	{
		return null;
	}

	/**
	 * The method simply returns the XML object it is called on.
	 *
	 * @sample xml.valueOf()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public XML js_valueOf()
	{
		return null;
	}
}
