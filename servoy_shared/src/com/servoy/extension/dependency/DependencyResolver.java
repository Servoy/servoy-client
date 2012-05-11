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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.IExtensionProvider;
import com.servoy.extension.IMessageProvider;
import com.servoy.extension.LibDependencyDeclaration;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.VersionStringUtils;
import com.servoy.j2db.util.Utils;

/**
 * Class responsible for resolving extension & lib dependencies.<br>
 * Given an extension id, it is able to generate a valid dependency tree for that extension (not list because any dependency could have multiple compatible versions), if possible using the given IExtensionProvider.<br><br>
 * If that is not possible, it will report the reasons that prevent it.<br><br>
 * The terms "tree path" are used heavily within the documentation of this class. These tree "paths" refer to paths in a virtual tree that is traversed while trying to resolve an extension (through simulated install extension/replace existing installed extension operations).
 *  
 * @author acostescu
 */
@SuppressWarnings("nls")
public class DependencyResolver implements IMessageProvider
{

	private final IExtensionProvider extensionProvider;
	private DependencyMetadata[] installedExtensions;
	private boolean ignoreLibConflicts = false;
	private Map<String, DependencyMetadata> allInstalledExtensions; // created from installedExtensions; <extensionID, version>

	// the following members may be altered and restored while trying to resolve extensions
	private Map<String, List<TrackableLibDependencyDeclaration>> allInstalledLibs; // created from installedExtensions; <libID, array(lib_declarattions)>
	private Map<String, Integer> uninstalledLibIds; // when a tree path simulates extension uninstall, remember which libs were uninstalled, cause a lib choice might be needed in this case (if only installed libs remain we still need to change active lib version)

	private Map<String, String> visitedExtensions;
	private Map<String, List<TrackableLibDependencyDeclaration>> visitedLibs;
	private Stack<StillToResolve> stillToResolve;
	private Stack<ExtensionNode> treePath;

	private List<DependencyPath> results;
	private final MessageKeeper messages = new MessageKeeper();

	/**
	 * See class docs.
	 * @param extensionProvider the extensionProvider to use.
	 */
	public DependencyResolver(IExtensionProvider extensionProvider)
	{
		this.extensionProvider = extensionProvider;
	}

	public synchronized void setIgnoreLibConflicts(boolean ignoreLibConflicts)
	{
		this.ignoreLibConflicts = ignoreLibConflicts;
	}

	/**
	 * Tells this dependency resolver what the currently installed extension are.
	 * @param installedExtensions a map of extension id's to dependency meta-data containing info about all currently installed extensions. 
	 */
	public void setInstalledExtensions(DependencyMetadata[] installedExtensions)
	{
		this.installedExtensions = installedExtensions;
	}

	/**
	 * Returns the extension provider.
	 * @return the extension provider.
	 */
	protected IExtensionProvider getExtensionProvider()
	{
		return extensionProvider;
	}

	/**
	 * Constructs a valid dependency tree, if possible, starting with the given extension id and version.
	 * @param extensionId the id of the extension that should be the tree's root.
	 * @param version the version of the extension that should be the tree's root.
	 */
	public synchronized void resolveDependencies(String extensionId, String version)
	{
		results = null;
		messages.clear();

		if (extensionId != null)
		{
			DependencyMetadata[] extension = extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(extensionId, version, version));
			if (extension != null && extension.length == 1)
			{
				processInstalledExtensions(); // prepare working copies of installed extensions/libs

				DependencyMetadata alreadyInstalledVersion = allInstalledExtensions.get(extensionId);
				if (alreadyInstalledVersion != null && alreadyInstalledVersion.version.equals(version))
				{
					messages.addInfo("This version of the extension is already installed.");
				}
				else
				{
					visitedExtensions = new HashMap<String, String>();
					visitedLibs = new HashMap<String, List<TrackableLibDependencyDeclaration>>();
					stillToResolve = new Stack<StillToResolve>();
					treePath = new Stack<ExtensionNode>();
					uninstalledLibIds = new HashMap<String, Integer>();

					resolveDependenciesRecursive(extension[0], null, false);
				}
			}
			else
			{
				messages.addWarning("Cannot resolve extension ('" + extensionId + "', " + version + ").");
			}
		}
	}

	/**
	 * Can be called after a call to {@link #resolveDependencies(String, String)}.
	 * @return a list of resolved extension paths. Each element of the list is an array of possible extensions to be installed.<br>
	 * If none is found, null will be returned.<br>
	 * If multiple possible extension paths are resolved, you may choose which one of them you want to install.
	 */
	public synchronized List<DependencyPath> getResults()
	{
		return results != null ? new ArrayList<DependencyPath>(results) : null;
	}

	/**
	 * Can be called after a call to {@link #resolveDependencies(String, String)}.
	 * @return a list of reasons why some tree paths were not resolved. Useful especially when no tree path was resolved ({@link #getResults()} == null).
	 */
	public synchronized Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clear();
	}

	protected void processInstalledExtensions()
	{
		allInstalledLibs = new HashMap<String, List<TrackableLibDependencyDeclaration>>();
		allInstalledExtensions = new HashMap<String, DependencyMetadata>();
		if (installedExtensions == null) installedExtensions = new DependencyMetadata[0];
		for (DependencyMetadata extension : installedExtensions)
		{
			allInstalledExtensions.put(extension.id, extension);
			if (extension.getLibDependencies() != null)
			{
				addToLibsMap(extension.id, extension.version, extension.getLibDependencies(), true);
			}
		}
	}

	/**
	 * Recursively resolves an extension dependency.<br>
	 * It enforces no extension dependency cycles and also checks for lib conflicts that might occur.<br><br>
	 * It also looks for possibilities to update installed extensions so that the given dependency can be installed.
	 * 
	 * @param extension description of the given extension dependency.
	 * @param visitedExtensions keeps track of recursively visited extension ids, to avoid cycles. The map's key is the extension id, the map's value is the version.
	 * @param visitedLibs keeps track of lib dependencies encountered in current tree path.
	 * @param parentNode parent extension node in the search tree.
	 * @param brokenDependency true if this call is the result of handling an inverse broken dependency, false if it's a normal dependency.
	 * @return the root node of the resolved dependency tree. Null if the dependency could not be resolved due to missing extension(s)/version(s) or conflict(s).
	 */
	protected void resolveDependenciesRecursive(DependencyMetadata extension, ExtensionNode parentNode, boolean brokenDependency)
	{
		int[] pops = new int[] { 0 }; // used to restore "stillToResolve" to it's previous state if necessary; ARRAY to be able to modify it when sent as parameter
		ExtensionNode node = null;
		boolean ok = false;
		LibDependencyDeclaration[][] libsToBeRemovedBecauseOfReplace = new LibDependencyDeclaration[][] { null }; // if this call will result in an installed extension replacement (update), the old lib dependencies are no longer relevant; ARRAY to be able to modify it when send as parameter

		// check compatibility with current servoy installation
		if (extension.getServoyDependency() == null ||
			VersionStringUtils.belongsToInterval(VersionStringUtils.getCurrentServoyVersion(), extension.getServoyDependency().minVersion,
				extension.getServoyDependency().maxVersion))
		{
			if (visitedExtensions.containsKey(extension.id))
			{
				// so this extension is already in the dependency tree; check to see if existing version is the same one;
				// if it is then we found a valid tree path; otherwise, this is not a valid tree path
				if (VersionStringUtils.sameVersion(visitedExtensions.get(extension.id), extension.version))
				{
					ok = true;
				}
				else
				{
					// else invalid tree path
					messages.addWarning("The same extension is required twice due to dependencies, but with different versions (" +
						visitedExtensions.get(extension.id) + ", " + extension.version + "). Extension id '" + extension.id + "'.");
				}
			}
			else if (allInstalledExtensions.containsKey(extension.id))
			{
				// so this extension is already installed; if it is the same one, we are ok, otherwise see if this version can be updated
				if (VersionStringUtils.sameVersion(allInstalledExtensions.get(extension.id).version, extension.version))
				{
					ok = true;
				}
				else
				{
					node = new ExtensionNode(extension.id, extension.version, allInstalledExtensions.get(extension.id).version);
					if (parentNode != null)
					{
						if (brokenDependency)
						{
							parentNode.brokenDepChildren.add(node);
						}
						else
						{
							parentNode.depChildren.add(node);
						}
					}
					ok = handleReplace(extension, libsToBeRemovedBecauseOfReplace, pops, node);
				}
			}
			else
			{
				// new extension for this tree path (not already present in path, not in installed extensions either);
				node = new ExtensionNode(extension.id, extension.version, ExtensionNode.SIMPLE_DEPENDENCY_RESOLVE);
				if (parentNode != null) parentNode.depChildren.add(node); // can't be a broken dep here
				ok = handleDependencies(extension, pops, node);
			}
		}
		else
		{
			// else not compatible with Servoy version - invalid tree path
			messages.addWarning("Extension " + extension + " is not compatible with current Servoy version (" + VersionStringUtils.getCurrentServoyVersion() +
				"); it requires " + (extension.getServoyDependency().minVersion != null ? "minimum '" + extension.getServoyDependency().minVersion + "'" : "") +
				(extension.getServoyDependency().maxVersion != null ? " and maximum '" + extension.getServoyDependency().maxVersion + "'" : "") + ".");
		}

		if (ok)
		{
			if (node != null)
			{
				// add lib and extension dependencies to this tree path
				visitedExtensions.put(extension.id, extension.version);
				if (extension.getLibDependencies() != null) addToLibsMap(extension.id, extension.version, extension.getLibDependencies(), false);
				treePath.push(node);
				if (libsToBeRemovedBecauseOfReplace[0] != null)
				{
					removeFromLibsMap(extension.id, libsToBeRemovedBecauseOfReplace[0], allInstalledLibs);
					addUninstalledLibIds(libsToBeRemovedBecauseOfReplace[0]);
				}
			} // else it's an already resolved or already installed node

			if (stillToResolve.size() == 0)
			{
				// BINGO, this is a complete valid extension dependency tree path; check that there are no lib conflicts with already installed libs and save it if it's ok
				// we are only doing this here because while recursively creating the tree path, some installed extensions might be replaced with other versions later in the tree path;
				// so we can't check this at each recursive call - just here at the end
				boolean[] conflictsFound = new boolean[] { false };
				LibChoice[] libConflicts = findLibConflicts(visitedLibs, allInstalledLibs, conflictsFound);
				if (!conflictsFound[0] || ignoreLibConflicts)
				{
					if (results == null) results = new ArrayList<DependencyPath>();
					results.add(new DependencyPath(treePath.toArray(new ExtensionNode[treePath.size()]), libConflicts));
				} // else invalid tree path
			}
			else
			{
				StillToResolve toResolveNext = stillToResolve.pop();

				for (DependencyMetadata dependencyVersion : toResolveNext.toResolve)
				{
					resolveDependenciesRecursive(dependencyVersion, toResolveNext.parentNode, toResolveNext.brokenDependency);
				}

				stillToResolve.push(toResolveNext); // push it back for reuse in alternate similar paths
			}

			if (node != null)
			{
				// remove lib and extension dependencies from this tree path
				visitedExtensions.remove(extension.id);
				if (extension.getLibDependencies() != null) removeFromLibsMap(extension.id, extension.getLibDependencies(), visitedLibs);
				treePath.pop();
				if (libsToBeRemovedBecauseOfReplace[0] != null)
				{
					addToLibsMap(extension.id, extension.version, libsToBeRemovedBecauseOfReplace[0], true);
					removeUninstalledLibIds(libsToBeRemovedBecauseOfReplace[0]);
				}
			} // else it's an already resolved or already installed node
		}

		// restore stillToResolve's state
		while (pops[0]-- > 0)
			stillToResolve.pop();

		if (node != null && parentNode != null)
		{
			// only one of these will actually contain it
			parentNode.depChildren.remove(node);
			parentNode.brokenDepChildren.remove(node);
		}
	}

	protected boolean handleDependencies(DependencyMetadata extension, int[] pops, ExtensionNode parentNode)
	{
		boolean ok = false;
		boolean dependenciesOKForNow;

		// check for lib dependency conflicts with this version for current tree path
		dependenciesOKForNow = checkNoLibConflicts(extension.id, extension.version, extension.getLibDependencies(), visitedLibs);

		// schedule resolve on all extension dependencies of this version
		if (dependenciesOKForNow)
		{
			if (extension.getExtensionDependencies() != null && extension.getExtensionDependencies().length > 0)
			{
				for (int i = extension.getExtensionDependencies().length - 1; dependenciesOKForNow && (i >= 0); i--)
				{
					DependencyMetadata[] availableDependencyVersions = extensionProvider.getDependencyMetadata(extension.getExtensionDependencies()[i]);

					// the provider might not have available a version of this dependency that is already installed and can be used
					DependencyMetadata installedExtDep = allInstalledExtensions.get(extension.getExtensionDependencies()[i].id);
					if (installedExtDep != null &&
						VersionStringUtils.belongsToInterval(installedExtDep.version, extension.getExtensionDependencies()[i].minVersion,
							extension.getExtensionDependencies()[i].maxVersion))
					{
						boolean foundInProvider = false;
						if (availableDependencyVersions != null && availableDependencyVersions.length > 0)
						{
							for (DependencyMetadata dmd : availableDependencyVersions)
							{
								if (dmd.id.equals(installedExtDep.id) && dmd.version.equals(installedExtDep.version))
								{
									foundInProvider = true;
									break;
								}
							}
						}
						if (!foundInProvider) availableDependencyVersions = Utils.arrayAdd(availableDependencyVersions, installedExtDep, false);
					}

					if (availableDependencyVersions != null && availableDependencyVersions.length > 0)
					{
						stillToResolve.push(new StillToResolve(availableDependencyVersions, parentNode, false));
						pops[0]++;
					}
					else
					{
						messages.addWarning("Cannot find a compatible version for extension dependency: " + extension.getExtensionDependencies()[i] + ".");
						dependenciesOKForNow = false;
					}
				}

				ok = dependenciesOKForNow; // if true, it is a valid path for now; it will continue because stillToResolve is not empty for sure
			}
			else
			{
				// no more dependencies - so valid path
				ok = true;
			}
		} // else lib conflicts - invalid tree path
		return ok;
	}

	protected boolean handleReplace(DependencyMetadata extension, LibDependencyDeclaration[][] libsToBeRemovedBecauseOfUpdate, int[] pops,
		ExtensionNode parentNode)
	{
		boolean ok = false;
		// already installed extension, with different version; it is not visited before in this tree path so we might be able to update it
		boolean dependenciesOKForNow;

		// check for lib dependency conflicts with this version for current tree path
		dependenciesOKForNow = checkNoLibConflicts(extension.id, extension.version, extension.getLibDependencies(), visitedLibs);

		if (dependenciesOKForNow)
		{
			// find who used to depend on old version, as well as the old version
			List<DependencyMetadata> brokenDependenciesToOldVersion = new ArrayList<DependencyMetadata>();
			for (DependencyMetadata dmd : installedExtensions)
			{
				if (dmd.id.equals(extension.id))
				{
					libsToBeRemovedBecauseOfUpdate[0] = dmd.getLibDependencies();
				}
				else
				{
					boolean noBrokenDep = true;
					if (dmd.getExtensionDependencies() != null && dmd.getExtensionDependencies().length > 0)
					{
						for (int i = 0; noBrokenDep && (i < dmd.getExtensionDependencies().length); i++)
						{
							// see if it's a dependency broken by this update
							if (dmd.getExtensionDependencies()[i].id == extension.id &&
								!VersionStringUtils.belongsToInterval(extension.version, dmd.getExtensionDependencies()[i].minVersion,
									dmd.getExtensionDependencies()[i].maxVersion))
							{
								brokenDependenciesToOldVersion.add(dmd);
								noBrokenDep = false;
							}
						}
					}
				}
			}

			// find alternatives to broken dependencies and schedule a resolve on them
			for (int i = brokenDependenciesToOldVersion.size() - 1; dependenciesOKForNow && (i >= 0); i--)
			{
				DependencyMetadata[] possibleReplacements = extensionProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(
					brokenDependenciesToOldVersion.get(i).id, VersionStringUtils.UNBOUNDED, VersionStringUtils.UNBOUNDED));
				if (possibleReplacements != null)
				{
					// check which versions are compatible with the new updated dependency
					List<DependencyMetadata> compatibleAlternatives = new ArrayList<DependencyMetadata>();
					for (DependencyMetadata candidate : possibleReplacements)
					{
						boolean noDependencyConflict = true;
						if (candidate.getExtensionDependencies() != null && candidate.getExtensionDependencies().length > 0)
						{
							for (int j = 0; noDependencyConflict && (j < candidate.getExtensionDependencies().length); j++)
							{
								if (candidate.getExtensionDependencies()[j].id.equals(extension.id) &&
									!VersionStringUtils.belongsToInterval(extension.version, candidate.getExtensionDependencies()[j].minVersion,
										candidate.getExtensionDependencies()[j].maxVersion))
								{
									noDependencyConflict = false;
								}
							}
						}
						if (noDependencyConflict) compatibleAlternatives.add(candidate);
					}
					if (compatibleAlternatives.size() > 0)
					{
						// schedule for resolve
						stillToResolve.push(new StillToResolve(compatibleAlternatives.toArray(new DependencyMetadata[compatibleAlternatives.size()]),
							parentNode, true));
						pops[0]++;
					}
					else
					{
						messages.addWarning("An installed extension's replacement is not possible due to resulting broken dependencies. Extension: " +
							extension + "'. Broken extension: '" + brokenDependenciesToOldVersion.get(i) + "'.");
						dependenciesOKForNow = false; // other versions of a broken dependency were found, but none was compatible; invalid tree path
					}
				}
				else
				{
					messages.addWarning("An installed extension's replacement is not possible due to resulting broken dependencies. Extension: " + extension +
						"'. Broken extension: '" + brokenDependenciesToOldVersion.get(i) + "'.");
					dependenciesOKForNow = false; // no replacements for a broken dependency; invalid tree path
				}
			}
			ok = dependenciesOKForNow; // if true, it is a valid path for now; it will continue because stillToResolve is not empty for sure
		}

		if (ok)
		{
			// check also for dependencies of newly replaced extension, not only for broken dependencies to it
			ok = handleDependencies(extension, pops, parentNode);
		}

		return ok;
	}

	/**
	 * Finds multiple lib declarations between visited and installed extensions.
	 * @param visited extensions visited in the dependency tree-path. Might have conflicts in itself if ignoreLibConflicts == true.
	 * @param installed the installed lib dependencies. (no conflicts in this one)
	 * @param conflictsFound return length 1 array parameter; it's first element will be set to true if conflicts are found and false otherwise.
	 * @return an array of lib version lists (per lib id) in case multiple version declarations are found for that lib id. Also specified if that lib id has conflicts or not.
	 */
	protected LibChoice[] findLibConflicts(Map<String, List<TrackableLibDependencyDeclaration>> visited,
		Map<String, List<TrackableLibDependencyDeclaration>> installed, boolean[] conflictsFound)
	{
		// check the lib situation after this supposed install
		conflictsFound[0] = false;
		List<LibChoice> libChoices = null;
		Iterator<Entry<String, List<TrackableLibDependencyDeclaration>>> it = visited.entrySet().iterator(); // visited might have conflicts in itself if ignoreLibConflicts == true 
		while (it.hasNext())
		{
			Entry<String, List<TrackableLibDependencyDeclaration>> entry = it.next();
			List<TrackableLibDependencyDeclaration> visitedLibVersions = entry.getValue();
			List<TrackableLibDependencyDeclaration> installedLibVersions = installed.get(entry.getKey());
			if (installedLibVersions == null) installedLibVersions = new ArrayList<TrackableLibDependencyDeclaration>();

			if (visitedLibVersions != null && installedLibVersions != null) // visitedLibVersions should always be != null actually at this point
			{
				int combinedSize = visitedLibVersions.size() + installedLibVersions.size();
				List<String> availableLibVersions = new ArrayList<String>(combinedSize);
				for (LibDependencyDeclaration sameExistingLib : visitedLibVersions)
				{
					availableLibVersions.add(sameExistingLib.version);
				}
				for (LibDependencyDeclaration sameExistingLib : installedLibVersions)
				{
					availableLibVersions.add(sameExistingLib.version);
				}
				for (LibDependencyDeclaration sameExistingLib : visitedLibVersions)
				{
					filterOutIncompatibleLibVersions(sameExistingLib, availableLibVersions);
				}
				for (LibDependencyDeclaration sameExistingLib : installedLibVersions)
				{
					filterOutIncompatibleLibVersions(sameExistingLib, availableLibVersions);
				}

				// visitedLibVersions.size() should be > 0 already cause it's not null
				if (combinedSize > 1)
				{
					// multiple lib declarations for a lib id found remaining after install; active declaration should be decided
					if (libChoices == null) libChoices = new ArrayList<LibChoice>();
					ArrayList<TrackableLibDependencyDeclaration> oneLibConflicts = new ArrayList<TrackableLibDependencyDeclaration>(combinedSize);
					oneLibConflicts.addAll(visitedLibVersions);
					oneLibConflicts.addAll(installedLibVersions);

					libChoices.add(new LibChoice(availableLibVersions.size() == 0,
						oneLibConflicts.toArray(new TrackableLibDependencyDeclaration[oneLibConflicts.size()])));
					if (availableLibVersions.size() == 0)
					{
						conflictsFound[0] = true;
						if (!ignoreLibConflicts)
						{
							messages.addWarning("Library dependency incompatibility for lib id '" + entry.getKey() + "': " +
								libChoices.get(libChoices.size() - 1) + ".");
						}
					}
				}
			}
		}

		// also check for situations where (because of an extension upgrade/downgrade) previously installed
		// libs were uninstalled, but only one or more declarations with same id still remain installed;
		// active lib declaration should be updated in this case as well
		Iterator<String> uit = uninstalledLibIds.keySet().iterator();
		while (uit.hasNext())
		{
			String uninstalledLibId = uit.next();
			List<TrackableLibDependencyDeclaration> stillInstalledWithSameId = installed.get(uninstalledLibId);
			if (stillInstalledWithSameId != null && visited.get(uninstalledLibId) == null)
			{
				if (libChoices == null) libChoices = new ArrayList<LibChoice>();
				// do not mark this as install lib conflict (even if they are conflicting) because these libs were already previously installed
				libChoices.add(new LibChoice(false, stillInstalledWithSameId.toArray(new TrackableLibDependencyDeclaration[stillInstalledWithSameId.size()])));
			}
		}

		return libChoices == null ? null : libChoices.toArray(new LibChoice[libChoices.size()]);
	}

	protected boolean checkNoLibConflicts(String extensionId, String extensionVersion, LibDependencyDeclaration[] libDependencies,
		Map<String, List<TrackableLibDependencyDeclaration>> existingLibDependencies)
	{
		if (ignoreLibConflicts) return true;

		boolean noConflict = true;
		if (libDependencies != null && libDependencies.length > 0)
		{
			for (int i = 0; noConflict && (i < libDependencies.length); i++)
			{
				LibDependencyDeclaration lib = libDependencies[i];
				List<TrackableLibDependencyDeclaration> sameVisitedLibs = existingLibDependencies.get(lib.id);
				if (sameVisitedLibs != null)
				{
					List<String> availableLibVersions = new ArrayList<String>(sameVisitedLibs.size() + 1);
					availableLibVersions.add(lib.version);
					for (LibDependencyDeclaration sameExistingLib : sameVisitedLibs)
					{
						availableLibVersions.add(sameExistingLib.version);
					}
					filterOutIncompatibleLibVersions(lib, availableLibVersions);
					for (LibDependencyDeclaration sameExistingLib : sameVisitedLibs)
					{
						filterOutIncompatibleLibVersions(sameExistingLib, availableLibVersions);
					}

					if (availableLibVersions.size() == 0)
					{
						noConflict = false; // incompatible libs...

						// now create a pretty warning message
						List<TrackableLibDependencyDeclaration> libDepsToList = new ArrayList<TrackableLibDependencyDeclaration>(sameVisitedLibs);
						libDepsToList.add(new TrackableLibDependencyDeclaration(lib, extensionId, extensionVersion, false));
						LibChoice justForTheWarning = new LibChoice(true, libDepsToList.toArray(new TrackableLibDependencyDeclaration[libDepsToList.size()]));
						messages.addWarning("Library dependency incompatibility for lib id '" + lib.id + "': " + justForTheWarning + ".");
					}
				}
			}
		}
		return noConflict;
	}

	/**
	 * It will remove from the availableLibVersions list the versions that are not compatible with the given lib.
	 */
	protected void filterOutIncompatibleLibVersions(LibDependencyDeclaration lib, List<String> availableLibVersions)
	{
		Iterator<String> it = availableLibVersions.iterator();
		while (it.hasNext())
		{
			String ver = it.next();
			if (!VersionStringUtils.belongsToInterval(ver, lib.minVersion, lib.maxVersion))
			{
				it.remove();
			}
		}
	}

	protected void addUninstalledLibIds(LibDependencyDeclaration[] libDependencyDeclarations)
	{
		for (LibDependencyDeclaration ldd : libDependencyDeclarations)
		{
			Integer count = uninstalledLibIds.get(ldd.id);
			if (count == null) count = Integer.valueOf(1);
			else count = Integer.valueOf(count.intValue() + 1);

			uninstalledLibIds.put(ldd.id, count);
		}
	}

	protected void removeUninstalledLibIds(LibDependencyDeclaration[] libDependencyDeclarations)
	{
		for (LibDependencyDeclaration ldd : libDependencyDeclarations)
		{
			Integer count = uninstalledLibIds.get(ldd.id); // should never be null
			if (count.intValue() == 1) uninstalledLibIds.remove(ldd.id);
			else uninstalledLibIds.put(ldd.id, Integer.valueOf(count.intValue() - 1));
		}
	}

	protected void addToLibsMap(String extensionId, String extensionVersion, LibDependencyDeclaration[] libDependencies, boolean installed)
	{
		Map<String, List<TrackableLibDependencyDeclaration>> libsMap = (installed ? allInstalledLibs : visitedLibs);
		for (LibDependencyDeclaration lib : libDependencies)
		{
			List<TrackableLibDependencyDeclaration> x = libsMap.get(lib.id);
			if (x == null)
			{
				x = new ArrayList<TrackableLibDependencyDeclaration>(1);
				libsMap.put(lib.id, x);
			}
			x.add(new TrackableLibDependencyDeclaration(lib, extensionId, extensionVersion, installed));
		}
	}

	protected void removeFromLibsMap(String extensionId, LibDependencyDeclaration[] libDependencies,
		Map<String, List<TrackableLibDependencyDeclaration>> libsMap)
	{
		for (LibDependencyDeclaration lib : libDependencies)
		{
			List<TrackableLibDependencyDeclaration> x = libsMap.get(lib.id);
			if (x != null)
			{
				Iterator<TrackableLibDependencyDeclaration> it = x.iterator();
				while (it.hasNext())
				{
					if (it.next().declaringExtensionId.equals(extensionId)) it.remove();
				}
				if (x.size() == 0) libsMap.remove(lib.id);
			}
		}
	}

	protected static class StillToResolve
	{
		public final DependencyMetadata[] toResolve;
		public final ExtensionNode parentNode;
		public final boolean brokenDependency; // false for normal dependency, true for inverse broken dependency

		public StillToResolve(DependencyMetadata[] toResolve, ExtensionNode parentNode, boolean brokenDependency)
		{
			this.toResolve = toResolve;
			this.parentNode = parentNode;
			this.brokenDependency = brokenDependency;
		}

	}

}
