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

@ServoyDocumented(category = ServoyDocumented.JSLIB, publicName = "Statements")
public class Statements
{
	/**
	 * @sample break
	 */
	public void js_flow_break()
	{
	}

	/**
	 * @sample const #;
	 */
	public void js_flow_const()
	{
	}

	/**
	 * @sample continue
	 */
	public void js_flow_continue()
	{
	}

	/**
	 * loop
	 *
	 * @sample
	 * do
	 * {
	 * }
	 * while ( # )
	 */
	public void js_flow_do_while()
	{
	}

	/**
	 * loop
	 *
	 * @sample
	 * for ( var i = 0 ; i < # ; i++ )
	 * {
	 * }
	 */
	public void js_flow_for()
	{
	}

	/**
	 * loop
	 *
	 * @sample
	 * for ( var item in obj )
	 * {
	 * }
	 */
	public void js_flow_for_each_in()
	{
	}

	/**
	 * 
	 *
	 * @sample
	 * if ( # )
	 * {
	 * }
	 */
	public void js_flow_if()
	{
	}

	/**
	 * 
	 *
	 * @sample
	 * if ( # )
	 * {
	 * }
	 * else
	 * {
	 * }
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
	 */
	public void js_flow_label()
	{
	}

	/**
	 * 
	 *
	 * @sample
	 * switch( # )
	 * {
	 * case:
	 * default:
	 * }
	 */
	public void js_flow_switch()
	{
	}

	/**
	 * 
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
	 */
	public void js_flow_try_catch()
	{
	}

	/**
	 * 
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
	 */
	public void js_flow_try_catch_finally()
	{
	}

	/**
	 * @sample var #;
	 */
	public void js_flow_var()
	{
	}

	/**
	 * loop
	 *
	 * @sample
	 * while ( # )
	 * {
	 * 	#
	 * }
	 */
	public void js_flow_while()
	{
	}
}
