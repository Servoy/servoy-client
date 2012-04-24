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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.parser.FullLibDependencyDeclaration;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * When installed extensions contain multiple versions of the same lib, only one of them will be chosen to be used.<br>
 * This means that any other versions of that lib need to be removed, and maybe some jnlp files need to be modified as well to not point to removed libs any more.<br>
 * 
 * @author acostescu
 */
public class LibChoiceHandler
{

	protected final static String APP_SERVER_DIR = "application_server"; //$NON-NLS-1$
	protected final static String PLUGINS_DIR_NAME = "plugins"; //$NON-NLS-1$
	protected static String PLUGINS_DIR = APP_SERVER_DIR + File.separator + PLUGINS_DIR_NAME;

	protected File installDir;
	private final List<String> warnings = new ArrayList<String>();

	public class LibActivation
	{
		/** this is either a newly installed lib version or an already installed lib version that will be used */
		public final FullLibDependencyDeclaration toSelect;
		/** the .exp file that contains the dependency to select */
		public final File toSelectSourceExp;
		/** this is a list of either newly installed libs or already installed libs that will not be used (might have been before) */
		public final FullLibDependencyDeclaration[] toBeRemoved;

		public LibActivation(FullLibDependencyDeclaration toSelect, File toSelectSourceExp, FullLibDependencyDeclaration[] toBeRemoved)
		{
			this.toSelect = toSelect;
			this.toSelectSourceExp = toSelectSourceExp;
			this.toBeRemoved = toBeRemoved;
		}
	}

	public class RemovedLibReference
	{
		public final String libPath;
		public final String removedResourcesXML;
		/** path relative to the plugins dir of the modified jnlp file */
		public final String jnlpFileRelativePath;

		public RemovedLibReference(String libPath, String removedResourcesXML, String jnlpFileRelativePath)
		{
			this.libPath = libPath;
			this.removedResourcesXML = removedResourcesXML;
			this.jnlpFileRelativePath = jnlpFileRelativePath;
		}

	}

	public LibChoiceHandler(File installDir)
	{
		this.installDir = installDir;
	}

	/**
	 * Selects/activates lib versions.
	 * @param libActivation each item representing a lib id is a pair: version to select/activate - versions of the lib that are/will be installed but not used and may need to be removed.
	 */
	public void activateLibVersions(LibActivation[] libActivations)
	{
		List<RemovedLibReference> removedLibReferences = new ArrayList<RemovedLibReference>();
		readRemoveLibReferences(removedLibReferences);

		for (LibActivation libActivation : libActivations)
		{
			if (libActivation.toBeRemoved != null)
			{
				removeLibs(libActivation.toBeRemoved, removedLibReferences);
			}

			activateLib(libActivation, removedLibReferences);
		}

		writeRemoveLibReferences(removedLibReferences);
	}

	protected void removeLibs(FullLibDependencyDeclaration[] toBeRemoved, List<RemovedLibReference> removedLibReferences)
	{
		for (FullLibDependencyDeclaration libVersion : toBeRemoved)
		{
			File libFileToBeRemoved = getLibFile(libVersion);
			if (libFileToBeRemoved.exists())
			{
				// check to see if it's used in any plugin jnlp file; if it is, edit the JNLP and remember the change
				File pluginsDir = new File(installDir, PLUGINS_DIR);
				if (pluginsDir.exists() && pluginsDir.isDirectory())
				{
					Collection<File> jnlps = FileUtils.listFiles(pluginsDir, new String[] { "jnlp" }, true); //$NON-NLS-1$
					Iterator<File> it = jnlps.iterator();
					while (it.hasNext())
					{
						File jnlp = it.next();
						removeReferenceFromJNLP(jnlp, libFileToBeRemoved, removedLibReferences, libVersion.relativePath);
					}
				}
				else
				{
					Debug.warn("[RI] Cannot find plugins directory when choosing lib version."); //$NON-NLS-1$
				}

				// delete the lib file
				boolean deleted = libFileToBeRemoved.delete();
				if (!deleted)
				{
					warnings.add("Cannot deactivate lib version " + toBeRemoved + " - " + libFileToBeRemoved + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					Debug.warn(warnings.get(warnings.size() - 1));
				}
			}
		}
	}

	protected File getLibFile(FullLibDependencyDeclaration libVersion)
	{
		File f = new File(installDir, libVersion.relativePath);
		if (!ExtensionUtils.isInParentDir(installDir, f))
		{
			Debug.warn("[RI] Lirary path points outside of the install dir for " + libVersion + ": " + f); //$NON-NLS-1$ //$NON-NLS-2$
			f = null;
		}
		return f;
	}

	protected void removeReferenceFromJNLP(File jnlp, File libFileToBeRemoved, List<RemovedLibReference> removedLibReferences, String relativePath)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		if (dbf != null)
		{
			try
			{
				DocumentBuilder db = dbf.newDocumentBuilder();
				db.setErrorHandler(new XMLErrorHandler(jnlp.getName()));

				Document doc = db.parse(jnlp);
				Element root = doc.getDocumentElement();
				root.normalize();

				NodeList list = root.getElementsByTagName("jnlp"); //$NON-NLS-1$
				if (list != null && list.getLength() == 0 && list.item(0).getNodeType() == Node.ELEMENT_NODE)
				{
					list = ((Element)list.item(0)).getElementsByTagName("resources"); //$NON-NLS-1$
					if (list != null && list.getLength() == 0 && list.item(0).getNodeType() == Node.ELEMENT_NODE)
					{
						List<String> removedParts = new ArrayList<String>();
						StringBuffer removedXML = new StringBuffer();

						File appServerDir = new File(installDir, APP_SERVER_DIR);
						Element resourcesNode = (Element)list.item(0);
						findAndRemoveReferences(resourcesNode, "jar", appServerDir, libFileToBeRemoved, removedParts, removedXML); //$NON-NLS-1$
						findAndRemoveReferences(resourcesNode, "nativelib", appServerDir, libFileToBeRemoved, removedParts, removedXML); //$NON-NLS-1$
						findAndRemovePackages(resourcesNode, removedParts, removedXML);

						if (removedXML.length() > 0)
						{
							removedLibReferences.add(new RemovedLibReference(relativePath, removedXML.toString(), getRelativePluginPath(jnlp)));
						}
					}
				}
			}
			catch (ParserConfigurationException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlp.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (SAXException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlp.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (IOException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlp.getName() + "'.", e); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}

	protected String getRelativePluginPath(File jnlp)
	{
		File pluginsDir = new File(installDir, PLUGINS_DIR);
		String result = jnlp.getName();
		File current = jnlp.getParentFile();
		while (current != null && !current.equals(pluginsDir))
		{
			result = current.getName() + File.pathSeparatorChar + result;
			current = current.getParentFile();
		}
		return current == null ? jnlp.getAbsolutePath() : result;
	}

	protected File getRelativePluginFile(String relativePluginPath)
	{
		File f = new File(relativePluginPath);
		if (!f.isAbsolute())
		{
			File pluginsDir = new File(installDir, PLUGINS_DIR);
			f = new File(pluginsDir, relativePluginPath);
		}
		return f;
	}

	protected void findAndRemoveReferences(Element el, String tagName, File appServerDir, File libFileToBeRemoved, List<String> removedParts,
		StringBuffer removedXML)
	{
		NodeList list = el.getElementsByTagName(tagName);
		if (list != null && list.getLength() > 0)
		{
			List<Node> nodesToBeRemoved = new ArrayList<Node>();
			for (int i = list.getLength() - 1; i >= 0; i--)
			{
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element)node;
					String href = element.getAttribute("href"); //$NON-NLS-1$
					if (href != null)
					{
						if (href.startsWith("/")) href = href.substring(1); //$NON-NLS-1$
						if (new File(appServerDir, href).equals(libFileToBeRemoved))
						{
							// found one reference
							String part = element.getAttribute("part"); //$NON-NLS-1$
							if (part != null) removedParts.add(part);
							removedXML.append('\n');
							removedXML.append(element.getNodeValue());
							nodesToBeRemoved.add(element);
						}
					}
				}
			}
			for (Node n : nodesToBeRemoved)
			{
				el.removeChild(n);
			}
		}
	}

	protected void findAndRemovePackages(Element el, List<String> removedParts, StringBuffer removedXML)
	{
		NodeList list = el.getElementsByTagName("package"); //$NON-NLS-1$
		if (list != null && list.getLength() > 0)
		{
			List<Node> nodesToBeRemoved = new ArrayList<Node>();
			for (int i = list.getLength() - 1; i >= 0; i--)
			{
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					Element element = (Element)node;
					String part = element.getAttribute("part"); //$NON-NLS-1$
					if (removedParts.contains(part))
					{
						removedXML.append('\n');
						removedXML.append(element.getNodeValue());
						nodesToBeRemoved.add(element);
					}
				}
			}
			for (Node n : nodesToBeRemoved)
			{
				el.removeChild(n);
			}
		}
	}

	protected void activateLib(final LibActivation libActivation, List<RemovedLibReference> removedLibReferences)
	{
		// if it's not available (already installed but not used version), then make it available (take it from .exp, restore any jnlp references that were removed)
		final File libFile = getLibFile(libActivation.toSelect);
		if (libFile != null)
		{
			// restore lib file from .exp file
			if (!libFile.exists())
			{
				try
				{
					Pair<Boolean, Object> result = ExtensionUtils.runOnEntry(libActivation.toSelectSourceExp, libActivation.toSelect.relativePath,
						new EntryInputStreamRunner<Object>()
						{

							public Object runOnEntryInputStream(InputStream is)
							{
								BufferedOutputStream os = null;
								try
								{
									os = new BufferedOutputStream(new FileOutputStream(libFile));
									Utils.streamCopy(is, os);
								}
								catch (IOException e)
								{
									warnings.add("Cannot restore lib file for version " + libActivation.toSelect + " from .exp file: " + libActivation.toSelectSourceExp); //$NON-NLS-1$//$NON-NLS-2$
									Debug.error(e);
								}
								finally
								{
									Utils.closeOutputStream(os);
								}
								return null;
							}

						});
					if (Boolean.FALSE == result.getLeft())
					{
						warnings.add("Cannot find lib version " + libActivation.toSelect + " in .exp file: " + libActivation.toSelectSourceExp); //$NON-NLS-1$//$NON-NLS-2$
						Debug.error(warnings.get(warnings.size() - 1));
					}
				}
				catch (IOException e)
				{
					warnings.add("Unable to restore lib version " + libActivation.toSelect + " from .exp file: " + libActivation.toSelectSourceExp); //$NON-NLS-1$//$NON-NLS-2$
					Debug.error(e);
				}
			}

			if (libFile.exists())
			{
				// restore removed JNLP references if any
				Iterator<RemovedLibReference> it = removedLibReferences.iterator();
				while (it.hasNext())
				{
					RemovedLibReference removedLibReference = it.next();
					if (removedLibReference.libPath.equals(libActivation.toSelect.relativePath))
					{
						it.remove();
						File jnlpFile = getRelativePluginFile(removedLibReference.jnlpFileRelativePath);
						if (jnlpFile != null && jnlpFile.exists()) restoreReferencesInJNLP(jnlpFile, removedLibReference.removedResourcesXML);
					}
				}
			}
		}
	}

	protected void restoreReferencesInJNLP(File jnlpFile, String removedResourcesXML)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		if (dbf != null)
		{
			try
			{
				DocumentBuilder db = dbf.newDocumentBuilder();
				db.setErrorHandler(new XMLErrorHandler(jnlpFile.getName()));

				Document doc = db.parse(jnlpFile);
				Element root = doc.getDocumentElement();
				root.normalize();

				NodeList list = root.getElementsByTagName("jnlp"); //$NON-NLS-1$
				if (list != null && list.getLength() == 0 && list.item(0).getNodeType() == Node.ELEMENT_NODE)
				{
					list = ((Element)list.item(0)).getElementsByTagName("resources"); //$NON-NLS-1$
					if (list != null && list.getLength() == 0 && list.item(0).getNodeType() == Node.ELEMENT_NODE)
					{
						Node resourcesNode = list.item(0);
						resourcesNode.setNodeValue(resourcesNode.getNodeValue() + removedResourcesXML);
					}
				}
			}
			catch (ParserConfigurationException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlpFile.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (SAXException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlpFile.getName() + "'.", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (IOException e)
			{
				Debug.log("Cannot parse jnlp '" + jnlpFile.getName() + "'.", e); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}

	/**
	 * Persistently stores the jnlp removed lib references.
	 */
	protected void writeRemoveLibReferences(List<RemovedLibReference> removedLibReferences)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Reads the removed lib references from persistent storage.
	 */
	protected void readRemoveLibReferences(List<RemovedLibReference> removedLibReferences)
	{
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("nls")
	protected class XMLErrorHandler implements ErrorHandler
	{

		private final String fileName;

		public XMLErrorHandler(String fileName)
		{
			this.fileName = fileName;
		}

		public void warning(SAXParseException exception) throws SAXException
		{
			Debug.trace("Warning when parsing jnlp '" + fileName + "' for lib version activation.", exception);
		}

		public void error(SAXParseException exception) throws SAXException
		{
			Debug.log("Error when parsing jnlp '" + fileName + "' for lib version activation.", exception);
		}

		public void fatalError(SAXParseException exception) throws SAXException
		{
			Debug.log("Cannot parse jnlp '" + fileName + "' for lib version activation.", exception);
		}

	}

}