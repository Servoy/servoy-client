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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.servoy.extension.MessageKeeper;
import com.servoy.j2db.util.Debug;

/**
 * Gathers any problem messages that could happen during XML parsing.
 * @author acostescu
 * @author gboros
 */
@SuppressWarnings("nls")
public class ParseDependencyMetadataErrorHandler implements ErrorHandler
{
	private final String packageName;
	private final MessageKeeper messages;

	public ParseDependencyMetadataErrorHandler(String packageName, MessageKeeper messages)
	{
		this.packageName = packageName;
		this.messages = messages;
	}

	public void warning(SAXParseException exception) throws SAXException
	{
		messages.addWarning("Warning when parsing 'package.xml' in package '" + packageName + "'. Warning: " + exception.getMessage() + ".");
		Debug.trace("Warning when parsing 'package.xml' in package '" + packageName + "'.", exception);
	}

	public void error(SAXParseException exception) throws SAXException
	{
		messages.addError("Error when parsing 'package.xml' in package '" + packageName + "'. Error: " + exception.getMessage() + ".");
		Debug.trace("Error when parsing 'package.xml' in package '" + packageName + "'.", exception);

	}

	public void fatalError(SAXParseException exception) throws SAXException
	{
		messages.addError("Cannot parse 'package.xml' in package '" + packageName + "'. Reason: " + exception.getMessage() + ".");
		Debug.trace("Cannot parse 'package.xml' in package '" + packageName + "'.", exception);
	}

}