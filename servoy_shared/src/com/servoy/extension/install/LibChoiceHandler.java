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

package com.servoy.extension.install;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.servoy.extension.IExtensionProvider;
import com.servoy.extension.IFileBasedExtensionProvider;
import com.servoy.extension.IMessageProvider;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.dependency.ILibVersionChooser;
import com.servoy.extension.dependency.LibChoice;
import com.servoy.extension.dependency.TrackableLibDependencyDeclaration;
import com.servoy.extension.install.LibActivationHandler.LibActivation;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.FullDependencyMetadata;
import com.servoy.extension.parser.FullLibDependencyDeclaration;
import com.servoy.extension.parser.IEXPParserPool;


/**
 * Takes in a list of lib choices, a lib version chooser and a lib activation handler, and activates for each choice the lib version chosen by the chooser.
 * @author acostescu
 */
public class LibChoiceHandler implements IMessageProvider
{

	protected IEXPParserPool parserPool;
	protected IExtensionProvider extensionProvider; // can be null in case of uninstall
	protected IFileBasedExtensionProvider installedExtensionsProvider;

	protected MessageKeeper messages = new MessageKeeper();
	protected List<LibActivation> activations = new ArrayList<LibActivation>();

	public LibChoiceHandler(IFileBasedExtensionProvider installedExtensionsProvider, IExtensionProvider extensionProvider, IEXPParserPool parserPool)
	{
		this.installedExtensionsProvider = installedExtensionsProvider;
		this.extensionProvider = extensionProvider;
		this.parserPool = parserPool;
	}

	/**
	 * Chooses the lib versions that will get activated for choices.
	 * This MUST be called BEFORE the actuall install/uninstall/replace process executes, so while any uninstalled extensions are still available (installed),
	 * because access is needed to them for getting the uninstalledLibDependencies file paths.
	 * @param libChoices the lib choices to be made.
	 * @param libVersionChooser the chooser to apply to each choice.
	 */
	public void prepareChoices(LibChoice[] libChoices, ILibVersionChooser libVersionChooser)
	{
		activations.clear();

		for (LibChoice choice : libChoices)
		{
			TrackableLibDependencyDeclaration chosenOne = libVersionChooser.chooseLibDeclaration(choice);
			List<FullLibDependencyDeclaration> shovedLibs = new ArrayList<FullLibDependencyDeclaration>();
			List<FullLibDependencyDeclaration> uninstalledLibs = null;
			for (TrackableLibDependencyDeclaration ldd : choice.libDependencies)
			{
				if (chosenOne != ldd) shovedLibs.add(getFullDependencyDeclaration(ldd));
			}
			if (choice.uninstalledLibDependencies != null)
			{
				uninstalledLibs = new ArrayList<FullLibDependencyDeclaration>();
				for (TrackableLibDependencyDeclaration ldd : choice.uninstalledLibDependencies)
				{
					uninstalledLibs.add(getFullDependencyDeclaration(ldd));
				}
			}
			activations.add(new LibActivation(getFullDependencyDeclaration(chosenOne), getEXPFile(chosenOne),
				shovedLibs.toArray(new FullLibDependencyDeclaration[shovedLibs.size()]), uninstalledLibs != null
					? uninstalledLibs.toArray(new FullLibDependencyDeclaration[uninstalledLibs.size()]) : null));
		}
	}

	/**
	 * Gives the prepared choices to the libActivationHandler for actual activation/deactivation of lib versions.
	 * This MUST be called AFTER the actuall install/uninstall/replace process executes, to make sure any herein activated lib version is
	 * not then deleted by that process (more then one extension/version could install the lib in the same path).
	 * @param libActivationHandler the handles the activations.
	 */
	public void handlePreparedChoices(LibActivationHandler libActivationHandler)
	{
		libActivationHandler.activateLibVersions(activations.toArray(new LibActivation[activations.size()]));
	}

	protected File getEXPFile(TrackableLibDependencyDeclaration tldd)
	{
		File f = null;
		if (tldd.declaringExtensionInstalled)
		{
			f = installedExtensionsProvider.getEXPFile(tldd.declaringExtensionId, tldd.declaringExtensionVersion, null);
		}
		else if (extensionProvider != null)
		{
			f = extensionProvider.getEXPFile(tldd.declaringExtensionId, tldd.declaringExtensionVersion, null);
		}
		return f;
	}

	protected FullLibDependencyDeclaration getFullDependencyDeclaration(TrackableLibDependencyDeclaration tldd)
	{
		FullLibDependencyDeclaration result = null;

		EXPParser parser = parserPool != null ? parserPool.getOrCreateParser(getEXPFile(tldd)) : new EXPParser(getEXPFile(tldd));
		FullDependencyMetadata dmd = parser.parseDependencyInfo();
		messages.addAll(parser.getMessages());
		parser.clearMessages();

		if (dmd != null)
		{
			for (FullLibDependencyDeclaration fldd : dmd.getLibDependencies())
			{
				if (fldd.id.equals(tldd.id) && fldd.version.equals(tldd.version))
				{
					result = fldd;
					break;
				}
			}
		}

		if (result == null)
		{
			// should never happen
			messages.addError("Cannot find full lib dependency declaration. Internal error."); //$NON-NLS-1$
		}

		return result;
	}

	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clearMessages();
	}

}