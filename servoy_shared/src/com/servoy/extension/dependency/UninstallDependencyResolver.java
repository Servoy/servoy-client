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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.IFileBasedExtensionProvider;

/**
 * Class responsible for resolving uninstall extension dependencies and listing affected libs.<br>
 * Given an extension id, it is able to traverse the dependent extension tree and turn that into a list of extension that will need to be uninstalled (in the order they should be uninstalled).
 *  
 * @author acostescu
 */
@SuppressWarnings("nls")
public class UninstallDependencyResolver extends AbstractResolver
{

	private final IFileBasedExtensionProvider installedExtensionProvider;
	private DependencyMetadata[] installedExtensions;

	// the following members may be altered and restored while traversing the dependent extensions tree
	private Map<String, List<TrackableLibDependencyDeclaration>> allInstalledLibs; // initially created from installedExtensions; <libID, array(lib_declarattions)>
	private Map<String, List<TrackableLibDependencyDeclaration>> uninstalledLibs; // remember which libs were uninstalled, cause a lib choice might be needed in this case (if only installed libs remain we still need to change active lib version)

	private Set<String> visitedExtensions;
	private List<InstallStep> treePath;
	private DependencyPath results;

	/**
	 * See class docs.
	 * @param installedExtensionProvider the extensionProvider for installed extensions.
	 */
	public UninstallDependencyResolver(IFileBasedExtensionProvider installedExtensionProvider)
	{
		this.installedExtensionProvider = installedExtensionProvider;
	}

	/**
	 * Returns the extension provider for installed extensions.
	 * @return the extension provider for installed extensions.
	 */
	protected IFileBasedExtensionProvider getExtensionProvider()
	{
		return installedExtensionProvider;
	}

	/**
	 * Constructs a valid dependent extension tree, starting with the given extension id and version.
	 * @param extensionId the id of the extension that should be the tree's root.
	 * @param version the version of the extension that should be the tree's root.
	 */
	public void resolveDependencies(String extensionId, String version)
	{
		results = null;
		messages.clearMessages();

		if (extensionId != null)
		{
			DependencyMetadata[] extension = installedExtensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(extensionId, version, version));
			if (extension != null && extension.length == 1)
			{
				processInstalledExtensions(); // prepare working copies of installed extensions/libs

				visitedExtensions = new HashSet<String>();
				uninstalledLibs = new HashMap<String, List<TrackableLibDependencyDeclaration>>();
				treePath = new ArrayList<InstallStep>();
				resolveDependentRecursive(extension[0]);
				LibChoice[] libChoices = findLibChoices();
				results = new DependencyPath(treePath.toArray(new InstallStep[treePath.size()]), libChoices);
			}
			else
			{
				messages.addWarning("Cannot find extension to uninstall ('" + extensionId + "', " + version + ").");
			}
		}
	}

	/**
	 * Can be called after a call to {@link #resolveDependencies(String, String)}.
	 * @return a dependency path to be uninstalled. Each element of the list is an extension that should get uninstalled.<br>
	 */
	public DependencyPath getResults()
	{
		return results;
	}

	protected void processInstalledExtensions()
	{
		installedExtensions = installedExtensionProvider.getAllAvailableExtensions();
		allInstalledLibs = new HashMap<String, List<TrackableLibDependencyDeclaration>>();
		if (installedExtensions == null) installedExtensions = new DependencyMetadata[0];
		for (DependencyMetadata extension : installedExtensions)
		{
			if (extension.getLibDependencies() != null)
			{
				addToLibsMap(extension.id, extension.version, extension.getLibDependencies(), allInstalledLibs, true);
			}
		}
	}

	/**
	 * Recursively resolves an extension's dependent extensions.<br>
	 * It enforces no extension dependency cycles and also checks for removed libs that need to be dealt with.<br><br>
	 * 
	 * @param extension description of the given extension.
	 */
	protected void resolveDependentRecursive(DependencyMetadata extension)
	{
		// check compatibility with current servoy installation
		if (!visitedExtensions.contains(extension.id))
		{
			InstallStep uninstallStep = null;

			// so this extension needs to be uninstalled; check for it's dependent extensions recursive
			uninstallStep = new InstallStep(InstallStep.UNINSTALL, new ExtensionNode(extension.id, null, extension.version));
			visitedExtensions.add(extension.id);

			addToLibsMap(extension.id, extension.version, extension.getLibDependencies(), uninstalledLibs, false);
			removeFromLibsMap(extension.id, extension.getLibDependencies(), allInstalledLibs);

			// find out who depends on the extension to be uninstalled
			for (DependencyMetadata dmd : installedExtensions)
			{
				if (dmd.getExtensionDependencies() != null && dmd.getExtensionDependencies().length > 0)
				{
					for (int i = 0; i < dmd.getExtensionDependencies().length; i++)
					{
						// see if it's a dependent extension
						if (dmd.getExtensionDependencies()[i].id.equals(extension.id))
						{
							resolveDependentRecursive(dmd);
						}
					}
				}
			}

			treePath.add(uninstallStep);
		} // else this extension is already marked to be uninstalled
	}

	/**
	 * Finds any lib choices that must be taken as a result of the uninstall.
	 * Also detects and warns when the uninstall will result in a lib conflict that was not there before.
	 * @return an array of lib version lists (per lib id) in case multiple version declarations are found for that lib id. Also specified if that lib id has conflicts or not.
	 */
	protected LibChoice[] findLibChoices()
	{
		// check the lib situation after this simulated uninstall
		List<LibChoice> libChoices = null;

		Iterator<String> uit = uninstalledLibs.keySet().iterator();
		while (uit.hasNext())
		{
			String uninstalledLibId = uit.next();
			List<TrackableLibDependencyDeclaration> stillInstalledWithSameId = allInstalledLibs.get(uninstalledLibId);
			if (stillInstalledWithSameId != null && stillInstalledWithSameId.size() > 0)
			{
				List<TrackableLibDependencyDeclaration> uninstalled = uninstalledLibs.get(uninstalledLibId);

				// check to see if it's a conflict, and if it is, check to see if it was a conflict before
				boolean isConflictingAfterUninstall = haveLibConflicts(stillInstalledWithSameId, new ArrayList<TrackableLibDependencyDeclaration>());
				if (libChoices == null) libChoices = new ArrayList<LibChoice>();

				LibChoice libChoice = new LibChoice(isConflictingAfterUninstall,
					stillInstalledWithSameId.toArray(new TrackableLibDependencyDeclaration[stillInstalledWithSameId.size()]),
					uninstalled.toArray(new TrackableLibDependencyDeclaration[uninstalled.size()]));
				libChoices.add(libChoice);

				if (isConflictingAfterUninstall)
				{
					boolean wasConflictingBeforeUninstall = haveLibConflicts(stillInstalledWithSameId, uninstalled);
					if (!wasConflictingBeforeUninstall)
					{
						messages.addWarning("Library dependency incompatibility will be generated by uninstall for lib id '" + uninstalledLibId + "': " +
							libChoice);
					} // if it was conflicting even before uninstall then just ignore it - nothing new here
				}
			}
		}

		return libChoices == null ? null : libChoices.toArray(new LibChoice[libChoices.size()]);
	}

}