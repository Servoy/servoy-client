/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.IExtensionProvider;
import com.servoy.extension.LibDependencyDeclaration;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.ServoyDependencyDeclaration;
import com.servoy.extension.VersionStringUtils;
import com.servoy.j2db.util.Debug;

/**
 * Parses the package.xml file (received as a stream) to get the dependency info.
 * 
 * @author acostescu
 * @author gboros
 */
public class ParseDependencyMetadata implements EntryInputStreamRunner<FullDependencyMetadata>
{

	private final String packageName;
	private final MessageKeeper messages;

	public ParseDependencyMetadata(String packageName, MessageKeeper messages)
	{
		this.packageName = packageName;
		this.messages = messages;
	}

	public FullDependencyMetadata runOnEntryInputStream(InputStream is)
	{
		FullDependencyMetadata dmd = null;
		SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema"); //$NON-NLS-1$
		if (factory != null)
		{
			Schema schema = null;
			try
			{
				// prepare to verify that XML adheres to our schema; because of this schema defined default values will be set as well when parsing
				schema = factory.newSchema(IExtensionProvider.class.getResource(EXPParser.EXTENSION_SCHEMA));
			}
			catch (SAXException ex)
			{
				messages.addError("Unable to validate 'package.xml' against the .xsd. Please report this problem to Servoy."); //$NON-NLS-1$
				Debug.error("Error compiling 'servoy-extension.xsd'."); //$NON-NLS-1$
			}

			if (schema != null)
			{
				try
				{
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					dbf.setSchema(schema);
					DocumentBuilder db = dbf.newDocumentBuilder();
					db.setErrorHandler(new ParseDependencyMetadataErrorHandler(packageName, messages));

					Document doc = db.parse(is); // should we use UTF-8 here?
					Element root = doc.getDocumentElement(); // "servoy-extension" tag
					root.normalize();

					// as this was already validated by schema, we need less null-checks and less structure checks
					try
					{
						String extensionId = root.getElementsByTagName(EXPParser.EXTENSION_ID).item(0).getTextContent();
						String extensionName = root.getElementsByTagName(EXPParser.EXTENSION_NAME).item(0).getTextContent();
						String version = root.getElementsByTagName(EXPParser.VERSION).item(0).getTextContent();

						NodeList dependencies = root.getElementsByTagName(EXPParser.DEPENDENCIES);

						ServoyDependencyDeclaration sdd = null;
						List<ExtensionDependencyDeclaration> edds = new ArrayList<ExtensionDependencyDeclaration>();
						List<FullLibDependencyDeclaration> ldds = new ArrayList<FullLibDependencyDeclaration>();

						if (dependencies != null && dependencies.getLength() == 1)
						{
							Element dependenciesNode = (Element)dependencies.item(0);
							dependencies = dependenciesNode.getElementsByTagName(EXPParser.SERVOY_DEPENDENCY);

							Element element;
							if (dependencies != null && dependencies.getLength() == 1)
							{
								element = ((Element)dependencies.item(0));
								sdd = new ServoyDependencyDeclaration(getMinMaxVersion(element, EXPParser.MIN_VERSION), getMinMaxVersion(element,
									EXPParser.MAX_VERSION));
							}

							int i = 0;
							dependencies = dependenciesNode.getElementsByTagName(EXPParser.EXTENSION_DEPENDENCY);
							while (dependencies != null && dependencies.getLength() > i)
							{
								element = ((Element)dependencies.item(i++));

								String minVersion = getMinMaxVersion(element, EXPParser.MIN_VERSION);
								String maxVersion = getMinMaxVersion(element, EXPParser.MAX_VERSION);

								edds.add(new ExtensionDependencyDeclaration(element.getElementsByTagName(EXPParser.ID).item(0).getTextContent(), minVersion,
									maxVersion));
							}

							i = 0;
							dependencies = dependenciesNode.getElementsByTagName(EXPParser.LIB_DEPENDENCY);
							while (dependencies != null && dependencies.getLength() > i)
							{
								element = ((Element)dependencies.item(i++));

								String libVersion = element.getElementsByTagName(EXPParser.VERSION).item(0).getTextContent();
								String path = element.getElementsByTagName(EXPParser.PATH).item(0).getTextContent();
								String minVersion = getMinMaxVersion(element, EXPParser.MIN_VERSION);
								String maxVersion = getMinMaxVersion(element, EXPParser.MAX_VERSION);

								if (minVersion == VersionStringUtils.UNBOUNDED && maxVersion == VersionStringUtils.UNBOUNDED)
								{
									// no min/max specified for lib so it is a fixed version dependency
									minVersion = maxVersion = libVersion;
								}
								ldds.add(new FullLibDependencyDeclaration(element.getElementsByTagName(EXPParser.ID).item(0).getTextContent(), libVersion,
									minVersion, maxVersion, path));
							}
						}

						// check that there are no duplicate dependency/lib declarations
						Set<String> ids = new HashSet<String>();
						for (ExtensionDependencyDeclaration dep : edds)
						{
							if (!ids.add(dep.id))
							{
								throw new IllegalArgumentException("multiple extension dependency declarations with id '" + dep.id + "' in '" + //$NON-NLS-1$ //$NON-NLS-2$
									packageName + "'"); //$NON-NLS-1$
							}
						}
						ids.clear();
						for (LibDependencyDeclaration dep : ldds)
						{
							if (!ids.add(dep.id))
							{
								throw new IllegalArgumentException("multiple lib dependency declarations with id '" + dep.id + "' in '" + //$NON-NLS-1$ //$NON-NLS-2$
									packageName + "'"); //$NON-NLS-1$
							}
						}

						// cache dependency info about this version of the extension
						dmd = new FullDependencyMetadata(extensionId, version, extensionName, sdd, (edds.size() > 0)
							? edds.toArray(new ExtensionDependencyDeclaration[edds.size()]) : null, (ldds.size() > 0)
							? ldds.toArray(new FullLibDependencyDeclaration[ldds.size()]) : null);
					}
					catch (IllegalArgumentException e)
					{
						messages.addError("Incorrect content when parsing 'package.xml' in package '" + packageName + "'. Reason: " + e.getMessage() + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
				catch (FactoryConfigurationError e)
				{
					messages.addError("Unable to parse 'package.xml'. Please report this problem to Servoy."); //$NON-NLS-1$
					Debug.error("Cannot find document builder factory."); //$NON-NLS-1$
				}
				catch (ParserConfigurationException e)
				{
					messages.addError("Cannot parse 'package.xml' in package '" + packageName + "'. Reason: " + e.getMessage() + "."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					Debug.trace("Cannot parse 'package.xml' in package '" + packageName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (SAXException e)
				{
					messages.addError("Cannot parse 'package.xml' in package '" + packageName + "'. Reason: " + e.getMessage() + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					Debug.trace("Cannot parse 'package.xml' in package '" + packageName + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (IOException e)
				{
					messages.addError("Cannot parse 'package.xml' in package '" + packageName + "'. Reason: " + e.getMessage() + "."); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					Debug.trace("Cannot parse 'package.xml' in package '" + packageName + "'.", e); //$NON-NLS-1$//$NON-NLS-2$
				}
			}
		}
		else
		{
			messages.addError("Unable to validate 'package.xml' against the .xsd. Please report this problem to Servoy."); //$NON-NLS-1$
			Debug.error("Cannot find schema factory."); //$NON-NLS-1$
		}
		return dmd;
	}

	// gets & creates a (possibly exclusive or unbounded) min or max version string from the element
	protected String getMinMaxVersion(Element element, String minOrMax)
	{
		String minMaxVersion = VersionStringUtils.UNBOUNDED;

		NodeList verNode = element.getElementsByTagName(minOrMax);
		if (verNode != null && verNode.getLength() == 1)
		{
			minMaxVersion = verNode.item(0).getTextContent();
			NamedNodeMap attrs = verNode.item(0).getAttributes();
			if (attrs != null)
			{
				Node attr = attrs.getNamedItem(EXPParser.INCLUSIVE_MIN_MAX_ATTR);
				if (attr != null && EXPParser.FALSE_VALUE.equals(attr.getNodeValue()))
				{
					minMaxVersion = VersionStringUtils.createExclusiveVersionString(minMaxVersion);
				}
			}
		}

		return minMaxVersion;
	}
}