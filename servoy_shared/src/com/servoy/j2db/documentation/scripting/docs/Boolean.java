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

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;

/**
 * <h1>Boolean</h1>
 * <p>A <b>Boolean</b> is a data type that represents one of two values: <code>true</code> or <code>false</code>.
 * Widely used in programming for decision-making processes and logical comparisons.</p>
 *
 * <h2>Key Characteristics</h2>
 * <ol>
 *   <li>
 *     <b>Basic Definition:</b><br/>
 *     - A Boolean value is either <code>true</code> or <code>false</code>. <br/>
 *     - These values are typically the result of comparison operations or logical expressions.
 *   </li>
 *   <li>
 *     <b>Type Conversion:</b><br/>
 *     - Any value in JavaScript can be converted to a Boolean using the <code>Boolean()</code> function or double negation <code>!!</code>.<br/>
 *     - Examples:
 *       <pre>
 *       Boolean(0); // false
 *       Boolean(1); // true
 *       </pre>
 *   </li>
 *   <li>
 *     <b>Common Use Cases:</b><br/>
 *     - <b>Conditionals:</b> Boolean values are essential in <code>if</code> statements to control the flow of a program.
 *       <pre>
 *       if (isAvailable) {
 *           console.log("The item is available!");
 *       }
 *       </pre>
 *     - <b>Logical Operators:</b> Boolean values work with operators like <code>&&</code>, <code>||</code>, and <code>!</code> for complex conditions.
 *       <pre>
 *       let result = isAvailable && isAffordable;
 *       </pre>
 *   </li>
 *   <li>
 *     <b>Truthiness and Falsiness:</b><br/>
 *     - Some values in JavaScript are inherently "truthy" (treated as <code>true</code>) or "falsy" (treated as <code>false</code>).<br/>
 *     - Falsy values include: <code>false</code>, <code>0</code>, <code>""</code>, <code>null</code>, <code>undefined</code>, and <code>NaN</code>.
 *   </li>
 * </ol>
 *
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Boolean", scriptingName = "Boolean")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Boolean
{

}
