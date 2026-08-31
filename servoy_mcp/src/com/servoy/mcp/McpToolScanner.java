/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
package com.servoy.mcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.plugins.ClientPluginAccessProvider;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.shared.IHeadlessClient;
import com.servoy.j2db.util.Debug;

/**
 * Finds the scope functions of the configured solution that are marked as tools.
 *
 * <p>Reaching the solution model from a server plugin is not obvious, because {@link IServerAccess}
 * offers no access to it and {@link IClientPluginAccess#getSolutionModel()} returns the scripting
 * facade rather than the model. The route used here is the one the platform itself uses in
 * <code>FunctionDefinition.exists(IClientPluginAccess)</code>: cast the plugin access to
 * {@link ClientPluginAccessProvider}, take its {@link IApplication}, and read
 * {@link IApplication#getFlattenedSolution()} from there.</p>
 *
 * <p>Two rules are inherited from that same code and must not be dropped:</p>
 * <ul>
 * <li>the solution model is touched inside {@link IApplication#invokeAndWait(Runnable)}, never
 * straight from the servlet thread;</li>
 * <li>the cast is guarded, and a missing solution is reported rather than silently producing an
 * empty tool list.</li>
 * </ul>
 *
 * <p>The declaration is read with {@link ScriptMethod#getDeclaration()}, which returns the raw
 * source including the documentation block. Anything that "reads the function" by matching braces
 * will strip that block and find no markers at all.</p>
 *
 * @author Servoy
 */
public class McpToolScanner
{
	private McpToolScanner()
	{
	}

	/**
	 * Scans every scope of the solution open in the given client.
	 *
	 * @param client a client with the configured solution loaded
	 * @return the tools found, never <code>null</code>
	 * @throws McpScanException when the solution model cannot be reached
	 */
	public static List<McpTool> scan(IHeadlessClient client) throws McpScanException
	{
		IClientPluginAccess access = client.getPluginAccess();
		if (!(access instanceof ClientPluginAccessProvider))
		{
			throw new McpScanException("Cannot reach the solution model: unexpected plugin access implementation " + //$NON-NLS-1$
				(access == null ? "null" : access.getClass().getName())); //$NON-NLS-1$
		}

		final IApplication application = ((ClientPluginAccessProvider)access).getApplication();
		final List<McpTool> tools = new ArrayList<McpTool>();
		final String[] failure = new String[1];

		application.invokeAndWait(new Runnable()
		{
			public void run()
			{
				if (application.getSolution() == null)
				{
					failure[0] = "no solution loaded in the client"; //$NON-NLS-1$
					return;
				}

				FlattenedSolution solution = application.getFlattenedSolution();
				if (solution == null)
				{
					failure[0] = "the client has no flattened solution"; //$NON-NLS-1$
					return;
				}

				collectTools(solution, tools);
			}
		});

		if (failure[0] != null) throw new McpScanException(failure[0]);

		return tools;
	}

	/**
	 * Walks every scope of the solution. A solution can declare several scopes, so this must not be
	 * narrowed down to <code>globals</code>.
	 */
	static void collectTools(FlattenedSolution solution, List<McpTool> tools)
	{
		Collection<String> scopeNames = solution.getScopeNames();
		if (scopeNames == null) return;

		for (String scopeName : scopeNames)
		{
			Iterator<ScriptMethod> methods = solution.getScriptMethods(scopeName, false);
			if (methods == null) continue;

			while (methods.hasNext())
			{
				ScriptMethod method = methods.next();
				if (method == null) continue;

				try
				{
					McpTool tool = McpJsDoc.parse(scopeName, method.getName(), method.getDeclaration());
					if (tool != null) tools.add(tool);
				}
				catch (RuntimeException e)
				{
					// one unreadable function must not take the whole tool list down
					Debug.error("mcp: could not read scopes." + scopeName + "." + method.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * Raised when the solution model cannot be reached, which is a configuration or lifecycle
	 * problem rather than "this solution declares no tools".
	 */
	public static class McpScanException extends Exception
	{
		public McpScanException(String message)
		{
			super(message);
		}
	}
}
