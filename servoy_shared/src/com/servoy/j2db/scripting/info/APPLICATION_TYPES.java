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
package com.servoy.j2db.scripting.info;

import com.servoy.base.persistence.constants.IBaseApplicationTypes;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;

/**
 * <p>VERSIONXXX - This function performs a simple addition. The JSDoc includes various tags and HTML elements to test conversion.</p>
 *
 * <ul>
 *     <li><b>Bold text item</b></li>
 *     <li><i>Italic text item</i></li>
 *     <li>Regular text item</li>
 * </ul>
 *
 * <p>The following elements test HTML to Markdown transformations:</p>
 *
 * <br/>
 * <p>Here's an example paragraph for testing paragraph tags.</p>
 *
 * // Example with pre tag <br/>
 * <pre>
 * let result = add(2, 3); //result should be 5.
 * function test() {
 *     console.log("This is a test!");
 * }
 * </pre>
 *
 * // Example with code tag <br/>
 * <code>
 * let result = add(2, 3); //result should be 5.
 * function test() {
 *     console.log("This is a test!");
 * }
 * </code>
 *
 * <pre data-puremarkdown>
 * | Column1 | Column2 |
 * |---------|---------|
 * | Data1   | Data2   |
 * </pre>
 *
 * <ol>
 *     <li>Ordered list item 1</li>
 *     <li>Ordered list item 2</li>
 * </ol>
 * <br/>
 * <a href="https://example.com">Example link</a> <br/>
 *
 * Using parentheses in text is transforming (the text inside parentheses) - which can be a problem for function parameters
 * function f(a, b); <br/>
 *
 * Note the italics, and missing spaces
 *
 * <code>inlineCode</code> and <i>preformatted</i> blocks are handled differently depending on content length.
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class APPLICATION_TYPES implements IPrefixedConstantsObject, IBaseApplicationTypes
{
	@Override
	public String toString()
	{
		return "Names of the application types";
	}
}
