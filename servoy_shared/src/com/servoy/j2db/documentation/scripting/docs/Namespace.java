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
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Namespace", scriptingName = "Namespace")
public class Namespace
{
	/**
	 * Identifies the prefix of this namespace, if applicable.
	 * 
	 * @sample
	 * var xmlElement = <xhtml:p xmlns:xhtml="http://www.w3.org/1999/xhtml">Hello World!</xhtml:p>;
	 * var namespace = xmlElement.namespace();
	 * application.output("Prefix: " + namespace.prefix); //will output: 'xhtml'
	 * application.output("URI: " + namespace.uri); //will output: 'http://www.w3.org/1999/xhtml'
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_getPrefix()
	{
		return null;
	}

	public void js_setPrefix(String prefix)
	{
	}

	/**
	 * Identifies the namespace of this Namespace, if applicable.
	 * 
	 * @sampleas js_getPrefix()
	 * 
	 * @link http://www.ecma-international.org/publications/files/ECMA-ST-WITHDRAWN/Ecma-357.pdf
	 */
	public String js_getUri()
	{
		return null;
	}

	public void js_setUri(String uri)
	{
	}
}
