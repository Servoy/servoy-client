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
import java.util.List;

/**
 * One scope function that is published as an MCP tool.
 *
 * <p>A tool is identified to the outside world by {@link #getName()}. Because two scopes of the
 * same solution may declare functions with the same name, the tool name carries the scope:
 * <code>myScope_getCustomer</code>. Internally the scope and the function name are kept apart,
 * because that is what {@link McpToolExecutor} needs.</p>
 *
 * @author Servoy
 */
public class McpTool
{
	/** Separator between scope and function in the published tool name. */
	private static final String NAME_SEPARATOR = "_"; //$NON-NLS-1$

	private final String scopeName;
	private final String functionName;
	private final String description;
	private final List<Parameter> parameters;

	public McpTool(String scopeName, String functionName, String description, List<Parameter> parameters)
	{
		this.scopeName = scopeName;
		this.functionName = functionName;
		this.description = description;
		this.parameters = parameters == null ? new ArrayList<Parameter>() : parameters;
	}

	/**
	 * The name this tool is published under, unique within the solution.
	 */
	public String getName()
	{
		return scopeName + NAME_SEPARATOR + functionName;
	}

	/**
	 * The scope the function lives in, to be passed as the execution context.
	 */
	public String getScopeName()
	{
		return scopeName;
	}

	public String getFunctionName()
	{
		return functionName;
	}

	/**
	 * Human readable description, taken from the documentation block. May be empty, never null.
	 */
	public String getDescription()
	{
		return description == null ? "" : description; //$NON-NLS-1$
	}

	/**
	 * The declared parameters, in declaration order. Does not include the user token - that one is
	 * added to the published schema separately, see {@link McpToolSchema}.
	 */
	public List<Parameter> getParameters()
	{
		return parameters;
	}

	@Override
	public String toString()
	{
		return "McpTool[" + getName() + ", " + parameters.size() + " parameter(s)]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * One declared parameter of a tool, as read from an <code>&#64;param</code> tag.
	 */
	public static class Parameter
	{
		private final String name;
		private final String servoyType;
		private final String description;
		private final boolean optional;

		public Parameter(String name, String servoyType, String description, boolean optional)
		{
			this.name = name;
			this.servoyType = servoyType;
			this.description = description;
			this.optional = optional;
		}

		public String getName()
		{
			return name;
		}

		/**
		 * The type as written in the documentation block, for example <code>String</code> or
		 * <code>Array&lt;Number&gt;</code>. Never translated here - see {@link McpToolSchema} for
		 * the mapping onto JSON Schema.
		 */
		public String getServoyType()
		{
			return servoyType;
		}

		public String getDescription()
		{
			return description == null ? "" : description; //$NON-NLS-1$
		}

		/**
		 * True when the documentation marked the parameter optional, by wrapping the name in
		 * brackets: <code>&#64;param {String} [suffix] ...</code>.
		 */
		public boolean isOptional()
		{
			return optional;
		}

		@Override
		public String toString()
		{
			return name + ":" + servoyType + (optional ? "?" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}
}
