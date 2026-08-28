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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the sample solution that ships with these tests, and cuts its scope files into the
 * declarations the scanner would see.
 *
 * <p><code>testresources/workspace/mcp_sample</code> is a real Servoy solution - the one the server
 * is exercised against by hand - kept here so the parsing tests run against genuine documentation
 * blocks rather than strings invented to suit them. A test that only ever sees hand-written input
 * proves the parser handles what the author imagined, which is not the same thing.</p>
 *
 * <p>Finding it has to work from two places: a plain JUnit run, where the working directory is the
 * project, and a plug-in test, where it is not. So the directory is looked for upwards from both the
 * working directory and the class files.</p>
 *
 * @author Servoy
 */
public final class McpTestFixture
{
	/** The solution as it appears in the URL: /servoy-service/mcp/mcp_sample */
	public static final String SOLUTION = "mcp_sample"; //$NON-NLS-1$

	/** Its authenticator module - it is what decides who a bearer token belongs to. */
	public static final String AUTHENTICATOR = "mcp_sample_auth"; //$NON-NLS-1$

	private static final String WORKSPACE = "testresources/workspace"; //$NON-NLS-1$

	private McpTestFixture()
	{
	}

	/**
	 * The sample solution's directory.
	 *
	 * @throws IllegalStateException when it cannot be found, which is a broken checkout rather than a
	 *         failing test - saying so plainly beats a stack of null pointer failures
	 */
	public static File solutionDirectory()
	{
		for (File start : new File[] { new File("."), codeSourceDirectory() }) //$NON-NLS-1$
		{
			File found = searchUpwards(start);
			if (found != null) return found;
		}

		throw new IllegalStateException("could not find " + WORKSPACE + "/" + SOLUTION + //$NON-NLS-1$ //$NON-NLS-2$
			" - it should sit in the servoy_mcp_tests bundle"); //$NON-NLS-1$
	}

	/**
	 * The text of one of the sample solution's scope files.
	 *
	 * @param scopeName the scope, without the .js
	 */
	public static String scopeSource(String scopeName) throws IOException
	{
		return Files.readString(new File(solutionDirectory(), scopeName + ".js").toPath(), StandardCharsets.UTF_8); //$NON-NLS-1$
	}

	/**
	 * Cuts a scope file into one string per function, each starting at its documentation block.
	 *
	 * <p>This is what {@code ScriptMethod.getDeclaration()} hands the scanner: the documentation and
	 * the function together, which is why the marker can be read at all.</p>
	 */
	public static List<String> declarations(String source)
	{
		List<String> declarations = new ArrayList<String>();

		int index = source.indexOf("/**"); //$NON-NLS-1$
		while (index >= 0)
		{
			int next = source.indexOf("/**", index + 3); //$NON-NLS-1$
			declarations.add(next < 0 ? source.substring(index) : source.substring(index, next));
			index = next;
		}

		return declarations;
	}

	/**
	 * The name of the function a declaration declares, or <code>null</code> when it declares none.
	 */
	public static String functionName(String declaration)
	{
		int at = declaration.indexOf("function "); //$NON-NLS-1$
		if (at < 0) return null;

		int open = declaration.indexOf('(', at);
		if (open < 0) return null;

		return declaration.substring(at + "function ".length(), open).trim(); //$NON-NLS-1$
	}

	private static File searchUpwards(File start)
	{
		File directory = start == null ? null : start.getAbsoluteFile();

		for (int depth = 0; directory != null && depth < 8; depth++)
		{
			File candidate = new File(new File(directory, WORKSPACE), SOLUTION);
			if (candidate.isDirectory()) return candidate;
			directory = directory.getParentFile();
		}

		return null;
	}

	private static File codeSourceDirectory()
	{
		try
		{
			return new File(McpTestFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception e)
		{
			// running from somewhere without a usable code source; the working directory is still tried
			return null;
		}
	}
}
