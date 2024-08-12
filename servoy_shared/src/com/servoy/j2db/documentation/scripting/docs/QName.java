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
 * QName, related to inline xml - as supported by the Rhino engine (but the support for direct XML inside javascript source code - as proposed by standards (Ecma-357) - is deprecated).
 *
 * @link https://ecma-international.org/publications-and-standards/standards/ecma-357/
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "QName", scriptingName = "QName")
public class QName
{
	/**
	 * Identifies the local name of the QName.
	 *
	 * @sample
	 * var Qnamevar = new QName('http://www.w3.org/1999/xhtml', 'author');
	 * application.output(Qnamevar.localName);
	 * application.output(Qnamevar.uri);
	 *
	 * @link https://ecma-international.org/publications-and-standards/standards/ecma-357/
	 */
	public String js_getLocalName()
	{
		return null;
	}

	public void js_setLocalName(String localName)
	{
	}

	/**
	 * Identifies the namespace of the QName, if applicable.
	 *
	 * @sampleas js_getLocalName()
	 *
	 * @link https://ecma-international.org/publications-and-standards/standards/ecma-357/
	 */
	public String js_getUri()
	{
		return null;
	}

	public void js_setUri(String uri)
	{
	}
}
