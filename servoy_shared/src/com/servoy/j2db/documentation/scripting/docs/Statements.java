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
 * Dummy class for listing methods for JavaScript types in a manner that
 * suits our documentation generator.
 * 
 * @author gerzse
 */
@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Statements")
@ServoyClientSupport(mc = true, wc = true, sc = true, ng = true)
public class Statements
{
	/**
	 * Break statement exits a loop.
	 * 
	 * @sample break
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_break()
	{
	}

	/**
	 * Constant declaration.
	 * 
	 * @sample const #;
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_const()
	{
	}

	/**
	 * Continue statement, jumps to next iteration of the loop.
	 * 
	 * @sample continue
	 * @simplifiedSignature
	 */
	public void js_flow_continue()
	{
	}

	/**
	 * do while loop
	 *
	 * @sample
	 * do
	 * {
	 * }
	 * while ( # )
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_do_while()
	{
	}

	/**
	 * for loop
	 *
	 * @sample
	 * for ( var i = 0 ; i < # ; i++ )
	 * {
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_for()
	{
	}

	/**
	 * foreach loop
	 *
	 * @sample
	 * for ( var item in obj )
	 * {
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_for_each_in()
	{
	}

	/**
	 * If statement
	 *
	 * @sample
	 * if ( # )
	 * {
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_if()
	{
	}

	/**
	 * If/Else statement.
	 *
	 * @sample
	 * if ( # )
	 * {
	 * }
	 * else
	 * {
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_if_else()
	{
	}

	/**
	 * Provides a statement with an identifier that you can refer to using a break or continue statement.
	 *
	 * For example, you can use a label to identify a loop, and then use the break or continue statements to indicate 
	 * whether a program should interrupt the loop or continue its execution.
	 * 
	 * @sample 
	 * var i = 0, j;
	 * outer_loop: while (i < 10) {
	 *	i++;
	 *	j = 0;
	 *	while (j < 10) {
	 *		j++;
	 *		if (j > i) continue outer_loop;
	 *		application.output("i=" + i + ", j=" + j);
	 *	}
	 * }
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_label()
	{
	}

	/**
	 * Switch statement.
	 *
	 * @sample
	 * switch( # )
	 * {
	 * case:
	 * default:
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_switch()
	{
	}

	/**
	 * try/catch statement
	 * 
	 * @sample
	 * try 
	 * {
	 * 	#
	 * }
	 *  catch(#) 
	 * {
	 * 	#
	 * }
	 * @simplifiedSignature
	 */
	public void js_flow_try_catch()
	{
	}

	/**
	 * try/catch/finally statement
	 *
	 * @sample
	 * try 
	 * {
	 * 	#
	 * }
	 *  catch(#) 
	 * {
	 * 	#
	 * } finally 
	 * {
	 * 	#
	 * }
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_try_catch_finally()
	{
	}

	/**
	 * Variable declaration
	 * 
	 * @sample var #;
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_var()
	{
	}

	/**
	 * while loop
	 *
	 * @sample
	 * while ( # )
	 * {
	 * 	#
	 * }
	 * 
	 * @simplifiedSignature
	 */
	public void js_flow_while()
	{
	}
}
