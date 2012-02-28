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
package com.servoy.j2db.util;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabsorb.JSONSerializer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;

public class XMLUtils
{
	public static NodeList getChildNodesByName(Node node, String name)
	{
		if (node == null) return null;

		XMLUtilsNodeList list = new XMLUtilsNodeList();
		NodeList childs = node.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++)
		{
			Node child = childs.item(i);
			if (child.getNodeName().equalsIgnoreCase(name))
			{
				list.addNode(child);
			}

		}
		return list;
	}

	public static Node getChildNodeByName(Node node, String name)
	{
		if (node == null) return null;

		NodeList childs = node.getChildNodes();
		for (int i = 0; i < childs.getLength(); i++)
		{
			Node child = childs.item(i);
			if (child.getNodeName().equalsIgnoreCase(name))
			{
				return child;
			}

		}
		// not found
		return null;
	}

	public static String getNodeValue(Node n)
	{
		if (n == null) return null;

		String val = n.getNodeValue();
		if (val == null)
		{
			Node child = n.getFirstChild();
			if (child != null)
			{
				val = child.getNodeValue();
			}
		}
		return val;
	}

	public static String getNodeAttribute(Node n, String name)
	{
		if (n == null) return null;

		NamedNodeMap attr = n.getAttributes();
		Node nameNode = attr.getNamedItem(name);
		if (nameNode != null)
		{
			String name_value = getNodeValue(nameNode);
			return name_value;
		}
		else
		{
			return null;
		}
	}


	/**
	 * Convert a string so that it can be put as PCDATA in an XML tag. This basically escapes &amp;, &lt;, and &gt; so that they are replaced by their XML
	 * entities.
	 * 
	 * @param string the string to be escaped
	 * 
	 * @return the escaped string
	 */
	public static String toPCDATA(String string)
	{
		if (string == null)
		{
			return null;
		}

		int length = string.length();
		StringBuffer buffer = new StringBuffer(length * 2);

		for (int i = 0; i < length; i++)
		{
			char c = string.charAt(i);
			switch (c)
			{
				case '<' :
					buffer.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buffer.append("&gt;"); //$NON-NLS-1$
					break;
				case '&' :
					buffer.append("&amp;"); //$NON-NLS-1$
					break;
				default :
					if (c >= ' ' || c == '\n' || c == '\t' || c == '\r') buffer.append(c);
					else Debug.error("Invalid character '" + c + "' found in string '" + string + "', deleting..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		return buffer.toString();
	}

	/**
	 * Checks that the two provided arguments are equal, and throws an exception complaining that the element was not expected if they are not.
	 * 
	 * @param element the element being checked
	 * @param against the string that it should be equal to
	 * 
	 * @throws SAXException if the two are not equal
	 */
	public static void checkElement(String element, String against) throws SAXException
	{
		if (!against.equals(element))
		{
			throw new SAXException("Missing expected '" + against + "' element"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Checks that the two provided attributed is not null, throwing an exception if it is.
	 * 
	 * @param attribute the attribute being checked
	 * @param name the name of the attribute being checked
	 * @param element the name of the element to which the attribute belongs
	 * 
	 * @throws SAXException if the two are not equal
	 */
	public static void checkAttributeNotNull(String attribute, String name, String element) throws SAXException
	{
		if (attribute == null)
		{
			throw new SAXException("Missing required '" + name + "' attribute from '" + element + "' element"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	/**
	 * Checks that the xml_version attribute matches the expected version, and throws an exception if this is not the case.
	 * 
	 * @param version the value of the xml_version attribute
	 * @param against the version number it should equal
	 * @param element the name of the element which has the xml_version attribute
	 * 
	 * @throws SAXException if the versions do not match
	 */
	public static void checkXMLVersion(String version, int against, String element) throws SAXException
	{
		checkAttributeNotNull(version, "xml_version", element); //$NON-NLS-1$
		if (parseInt(version) != against)
		{
			throw new SAXException("Invalid XML version of import, specified version " + version + ", software version " + against); //$NON-NLS-1$//$NON-NLS-2$
		}
	}

	/**
	 * Checks that the repository_version attribute matches the expected version, and throws an exception if this is not the case.
	 * 
	 * @param version the value of the repository_version attribute
	 * @param against the version number it should equal
	 * @param element the name of the element which has the repository_version attribute
	 * 
	 * @throws SAXException if the versions do not match
	 */
	public static void checkRepositoryVersion(String version, int against, String element) throws SAXException
	{
		checkAttributeNotNull(version, "repository_version", element); //$NON-NLS-1$
		if (parseInt(version) != against)
		{
			throw new SAXException("Invalid repository version of import, specified version " + version + ", software  version " + against); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Tries to parse the specified string to an integer and throws an exception if there is a parser error.
	 * 
	 * @param string the string to parse to an integer
	 * 
	 * @return the value of the string as an <code>int</code>
	 * 
	 * @throws SAXException if the string cannot be parsed
	 */
	public static int parseInt(String string) throws SAXException
	{
		try
		{
			return Integer.parseInt(string);
		}
		catch (NumberFormatException e)
		{
			throw new SAXException(e);
		}
	}

	/**
	 * Format the contents of a <code>SAXParseException</code> to the trace stream of the <code>Debug</code> class. This method is called by the SAX parsers
	 * whenever a warning or error occurs which should be sent to the tracing output.
	 * 
	 * @param type the type of error as a string, i.e. "warning"
	 * @param e the <code>SAXParseException</code> of which to output the info
	 */
	public static void trace(String type, SAXParseException e)
	{
		Debug.trace("[XML " + type + "] " + e.getPublicId() + "(" + e.getLineNumber() + ", " + e.getColumnNumber() + "): " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
	}

	/**
	 * Format the contents of a <code>SAXParseException</code> to the error stream of the <code>Debug</code> class. This method is called by the SAX parsers
	 * whenever a warning or error occurs which should be sent to the error output.
	 * 
	 * @param type the type of error as a string, i.e. "warning"
	 * @param e the <code>SAXParseException</code> of which to output the info
	 */
	public static void error(String type, SAXParseException e)
	{
		Debug.error("[XML " + type + "] " + e.getPublicId() + "(" + e.getLineNumber() + ", " + e.getColumnNumber() + "): " + e.getMessage()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$
	}

	public static XMLReader createXMLReader() throws RepositoryException
	{
		try
		{
			return XMLReaderFactory.createXMLReader();
		}
		catch (Exception e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 * Remove xml comments from the xml string.
	 * 
	 * @param xml
	 * @return
	 */
	public static String stripXMLComment(CharSequence xml)
	{
		if (xml == null)
		{
			return null;
		}

		// option DOTALL: '.' matches newlines
		// use '.+?' in stead of '.*': non-greedy matching ("<!-- --> <important-stuff\> <!-- -->" is replaced with " <important-stuff\> ")
		Pattern p = Pattern.compile("<!--.+?-->", Pattern.DOTALL); //$NON-NLS-1$
		Matcher m = p.matcher(xml);
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, ""); //$NON-NLS-1$
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Parse an array string '[[tp,len,scale], [tp,len,scale], ...]' as ColumnType list
	 */
	public static List<ColumnType> parseColumnTypeArray(String s)
	{
		if (s == null) return null;

		List<ColumnType> list = null;
		JSONSerializer serializer = new JSONSerializer();
		try
		{
			serializer.registerDefaultSerializers();
			Integer[][] array = (Integer[][])serializer.fromJSON(s);
			if (array != null && array.length > 0)
			{
				list = new ArrayList<ColumnType>(array.length);
				for (Integer[] elem : array)
				{
					list.add(ColumnType.getInstance(elem[0].intValue(), elem[1].intValue(), elem[2].intValue()));
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return list;
	}

	/**
	 * Serialize a ColumnType list as an array string '[[tp,len,scale], [tp,len,scale], ...]'
	 */
	public static String serializeColumnTypeArray(List<ColumnType> columnTypes)
	{
		if (columnTypes == null || columnTypes.size() == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < columnTypes.size(); i++)
		{
			if (i > 0) sb.append(',');
			sb.append(serializeColumnType(columnTypes.get(i)));
		}
		sb.append(']');
		return sb.toString();
	}

	public static String serializeColumnType(ColumnType columnType)
	{
		if (columnType == null)
		{
			return null;
		}

		return new StringBuilder() //
		.append('[') //
		/*        */.append(String.valueOf(columnType.getSqlType())) //
		.append(',').append(String.valueOf(columnType.getLength())) //
		.append(',').append(String.valueOf(columnType.getScale())) //
		.append(']').toString();
	}
}
