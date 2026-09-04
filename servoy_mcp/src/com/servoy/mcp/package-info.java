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
/**
 * The MCP server - SVY-21326.
 *
 * <p>Publishes a solution's <code>&#64;Tool</code> scope functions over the Model Context Protocol,
 * at <code>/servoy-service/mcp/&lt;solution&gt;</code>. A solution declares itself by being of type
 * <code>MCP Service</code>; there is nothing to configure.</p>
 *
 * <p>The protocol is the official MCP SDK's, the same one the Developer MCP servers use. What is
 * Servoy's own is either side of it: finding the tools by reading documentation blocks out of the
 * solution model, and running one in a pooled headless client as the user the bearer token
 * identifies.</p>
 */
package com.servoy.mcp;
