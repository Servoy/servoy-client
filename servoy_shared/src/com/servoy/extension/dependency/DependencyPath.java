/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.extension.dependency;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A extension dependency path result found when trying to resolve an install/update.
 * @author acostescu
 */
public class DependencyPath implements Serializable
{

	/** true if this is a dependency path for an uninstall operation (in which case only installSequence and libChoices are meaningful),
	 * and false if it's an install/replace path (in which case all members are meaningful) */
	public final boolean uninstall;

	/** The list of extension/version nodes in this valid install/replace dependency path. It is not used for uninstall. */
	public final transient ExtensionNode[] extensionPath;

	/** Any lib choices that must be made on this dependency path. Each lib choice is a list of conflicting/non-conflicting lib declarations with the same lib id but other versions. Can be null. */
	public final LibChoice[] libChoices;

	public final InstallStep[] installSequence;

	/**
	 * Creates a new INSTALL/REPLACE extension dependency path result.
	 * @param extensionPath the list of extension/version nodes in this valid dependency path.
	 * @param libChoices any lib choices found on this dependency path. A list of more then one conflicting/non-conflicting lib declarations with the same lib id that are affected by the INSTALL/REPLACE. Can be null.
	 */
	public DependencyPath(ExtensionNode[] extensionPath, LibChoice[] libChoices)
	{
		uninstall = false;

		this.extensionPath = extensionPath;
		this.libChoices = libChoices;

		ArrayList<InstallStep> installSeq = new ArrayList<InstallStep>(extensionPath.length * 2);
		computeInstallDep(extensionPath[0], installSeq); // at this time, all ExtensionNode children members are usable (valid search tree)
		installSequence = installSeq.toArray(new InstallStep[installSeq.size()]);
	}

	/**
	 * Creates a new UNINSTALL extension dependency path result.
	 * @param extensionPath the list of installed extensions that depend on the extension to be uninstalled.
	 * @param libChoices the uninstall operation might have deleted some libs that extensions remaining installed might still need. In
	 * this case, one of the remaining installed versions of that lib needs to be activated instead. Can be null.
	 */
	public DependencyPath(InstallStep[] uninstallSequence, LibChoice[] libChoices)
	{
		uninstall = true;

		installSequence = uninstallSequence;
		this.libChoices = libChoices;
		extensionPath = null;
	}

	protected void computeInstallDep(ExtensionNode node, List<InstallStep> installSeq)
	{
		if (node.resolveType != ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE)
		{
			// recurse through reverse broken dependencies for uninstall
			for (ExtensionNode n : node.brokenDepChildren)
			{
				computeInverseBrokenDepUninstall(n, installSeq);
			}

			installSeq.add(new InstallStep(InstallStep.UNINSTALL, node));
		}

		for (ExtensionNode n : node.depChildren)
		{
			computeInstallDep(n, installSeq);
		}
		installSeq.add(new InstallStep(InstallStep.INSTALL, node));

		if (node.resolveType != ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE)
		{
			for (ExtensionNode n : node.brokenDepChildren)
			{
				computeInverseBrokenDepInstall(n, installSeq);
			}
		}
	}

	protected void computeInverseBrokenDepUninstall(ExtensionNode node, List<InstallStep> installSeq)
	{
		for (ExtensionNode n : node.brokenDepChildren)
		{
			computeInverseBrokenDepUninstall(n, installSeq);
		}
		installSeq.add(new InstallStep(InstallStep.UNINSTALL, node));
	}

	protected void computeInverseBrokenDepInstall(ExtensionNode node, List<InstallStep> installSeq)
	{
		for (ExtensionNode n : node.depChildren)
		{
			computeInstallDep(n, installSeq);
		}
		installSeq.add(new InstallStep(InstallStep.INSTALL, node));
		for (ExtensionNode n : node.brokenDepChildren)
		{
			computeInverseBrokenDepInstall(n, installSeq);
		}
	}

	@Override
	@SuppressWarnings("nls")
	public String toString()
	{
		if (uninstall)
		{
			return "{ U OP, " + Arrays.asList(installSequence) + ", LIB Choices: " + (libChoices == null ? null : Arrays.asList(libChoices)) + "}";
		}
		else
		{
			return "{ I/R OP, " + Arrays.asList(extensionPath) + ", LIB Choices: " + (libChoices == null ? null : Arrays.asList(libChoices)) + "}";
		}
	}

}
