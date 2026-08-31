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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Routes <code>/servoy-service/mcp/&lt;solution&gt;</code> to that solution's MCP server.
 *
 * <p>The solution is named in the path rather than configured, so one endpoint serves every MCP
 * solution the server hosts and nothing has to be set up twice. <code>rest_ws</code> works the same
 * way, and for the same reason.</p>
 *
 * <p>A name that is not a deployed solution of type <code>MCP Service</code> gets a 404. Deploying
 * the solution is the deployer's business: if it declares itself as an MCP service, it is expected
 * to be there.</p>
 *
 * @author Servoy
 */
@SuppressWarnings("nls")
@WebServlet("/mcp/*")
public class McpServlet extends HttpServlet
{
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String solutionName = solutionFromPath(request.getPathInfo());

		if (solutionName == null)
		{
			respond(response, HttpServletResponse.SC_NOT_FOUND,
				"Name the solution: /" + McpRuntime.WEBSERVICE_NAME + "/<solution>");
			return;
		}

		McpSolutionServer solutionServer = McpRuntime.getInstance().getSolutionServer(solutionName);
		if (solutionServer == null)
		{
			respond(response, HttpServletResponse.SC_NOT_FOUND,
				"No solution '" + solutionName + "' of type MCP Service is deployed");
			return;
		}

		solutionServer.service(request, response);
	}

	/**
	 * The solution out of <code>/mcp/&lt;solution&gt;</code>.
	 *
	 * <p>The dispatcher hands the request over whole, so the path still carries the service alias in
	 * front - see <code>WebServicesServlet</code>.</p>
	 *
	 * @return the solution name, or <code>null</code> when the path names none
	 */
	static String solutionFromPath(String pathInfo)
	{
		if (pathInfo == null) return null;

		String path = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;

		// drop the service alias the dispatcher matched on
		if (path.equals(McpRuntime.WEBSERVICE_NAME)) return null;
		if (path.startsWith(McpRuntime.WEBSERVICE_NAME + "/"))
		{
			path = path.substring(McpRuntime.WEBSERVICE_NAME.length() + 1);
		}

		int slash = path.indexOf('/');
		String solutionName = slash < 0 ? path : path.substring(0, slash);

		return solutionName.length() == 0 ? null : solutionName;
	}

	private static void respond(HttpServletResponse response, int status, String message) throws IOException
	{
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
	}
}
