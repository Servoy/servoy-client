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
import java.util.Iterator;
import java.util.List;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;

public class ScriptingUtils
{
	private static void addGlobalVariables(List<String> variables, Node parent)
	{
		if (parent != null)
		{
			Node node = parent.getFirstChild();
			addGlobalVariables(variables, node);
			while (node != null)
			{
				if (node.getType() == Token.BINDNAME && !variables.contains(node.getString()))
				{
					variables.add(node.getString());
				}
				node = node.getNext();
				addGlobalVariables(variables, node);
			}
		}
	}

	public static String[] getGlobalVariables(String s)
	{
		Context context = Context.enter();
		try
		{
			List<String> variables = new ArrayList<String>();
			CompilerEnvirons m_compilerEnv = new CompilerEnvirons();
			m_compilerEnv.initFromContext(context);
			Parser p = new Parser(m_compilerEnv, context.getErrorReporter());
			ScriptOrFnNode tree = p.parse(s, null, 0);
			if (tree != null && tree.getFunctionCount() == 1)
			{
				FunctionNode function = tree.getFunctionNode(0);
				addGlobalVariables(variables, function);
				Iterator<String> it = variables.iterator();
				while (it.hasNext())
				{
					String name = it.next();
					int index = function.getParamOrVarIndex(name);
					if (index >= 0) it.remove();
				}
			}
			return variables.toArray(new String[] { });
		}
		catch (RhinoException e)
		{
			Debug.error(e);
			return null;
		}
		finally
		{
			Context.exit();
		}
	}
}
