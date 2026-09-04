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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;

/**
 * The JSON binding the MCP SDK needs, over the Jackson the server already carries.
 *
 * <p>The SDK ships its own Jackson binding as <code>mcp-json-jackson2</code>, but that artifact is
 * not an OSGi bundle: <code>com.servoy.eclipse.developer.mcp</code> has to embed the jar in its
 * <code>Bundle-ClassPath</code> to use it. Eight small methods are less to carry than a binary in
 * the repository and an entry in the build, and Jackson is already an import here.</p>
 *
 * @author Servoy
 */
public class McpJacksonMapper implements McpJsonMapper
{
	private final ObjectMapper mapper;

	public McpJacksonMapper()
	{
		this(new ObjectMapper());
	}

	public McpJacksonMapper(ObjectMapper mapper)
	{
		this.mapper = mapper;
	}

	@Override
	public <T> T readValue(String content, Class<T> type) throws IOException
	{
		return mapper.readValue(content, type);
	}

	@Override
	public <T> T readValue(byte[] content, Class<T> type) throws IOException
	{
		return mapper.readValue(content, type);
	}

	@Override
	public <T> T readValue(String content, TypeRef<T> type) throws IOException
	{
		return mapper.readValue(content, mapper.constructType(type.getType()));
	}

	@Override
	public <T> T readValue(byte[] content, TypeRef<T> type) throws IOException
	{
		return mapper.readValue(content, mapper.constructType(type.getType()));
	}

	@Override
	public <T> T convertValue(Object fromValue, Class<T> type)
	{
		return mapper.convertValue(fromValue, type);
	}

	@Override
	public <T> T convertValue(Object fromValue, TypeRef<T> type)
	{
		return mapper.convertValue(fromValue, mapper.constructType(type.getType()));
	}

	@Override
	public String writeValueAsString(Object value) throws IOException
	{
		return mapper.writeValueAsString(value);
	}

	@Override
	public byte[] writeValueAsBytes(Object value) throws IOException
	{
		return mapper.writeValueAsBytes(value);
	}
}
