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
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.IMessageProvider;
import com.servoy.extension.Message;
import com.servoy.extension.MessageKeeper;
import com.servoy.extension.parser.FullLibDependencyDeclaration;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * When installed extensions contain multiple versions of the same lib, only one of them will be chosen to be used.<br>
 * This means that any other versions of that lib need to be removed, and maybe some jnlp files need to be modified as well to not point to removed libs any more.<br>
 * 
 * @author acostescu
 */
public class LibActivationHandler implements IMessageProvider
{

	protected final static String APP_SERVER_DIR = "application_server"; //$NON-NLS-1$
	protected final static String PLUGINS_DIR_NAME = "plugins"; //$NON-NLS-1$
	protected static String PLUGINS_DIR = APP_SERVER_DIR + "/" + PLUGINS_DIR_NAME; //$NON-NLS-1$

	protected File installDir;
	protected MessageKeeper messages = new MessageKeeper();

	protected File pluginsDir;

	public static class LibActivation
	{
		/** this is either a newly installed lib version or an already installed lib version that will be used */
		public final FullLibDependencyDeclaration toSelect;
		/** the .exp file that contains the dependency to select */
		public final File toSelectSourceExp;
		/** this is a list of either newly installed libs or already installed libs that will not be used (might have been before); can be null in case of uninstall */
		public final FullLibDependencyDeclaration[] toBeDeactivated;
		/** this is a list of uninstalled instances of this lib; can be null */
		public final FullLibDependencyDeclaration[] uninstalled;

		public LibActivation(FullLibDependencyDeclaration toSelect, File toSelectSourceExp, FullLibDependencyDeclaration[] toBeDeactivated,
			FullLibDependencyDeclaration[] uninstalled)
		{
			this.toSelect = toSelect;
			this.toSelectSourceExp = toSelectSourceExp;
			this.toBeDeactivated = toBeDeactivated;
			this.uninstalled = uninstalled;
		}
	}

	public LibActivationHandler(File installDir)
	{
		this.installDir = installDir;
		this.pluginsDir = new File(installDir, PLUGINS_DIR);
	}

	/**
	 * Selects/activates lib versions.
	 * @param libActivation each item representing a lib id is a pair: version to select/activate - versions of the lib that are/will be installed but not used and may need to be removed.
	 */
	public void activateLibVersions(LibActivation[] libActivations)
	{
		for (LibActivation libActivation : libActivations)
		{
			deactivateLibs(libActivation.toBeDeactivated, libActivation.uninstalled, libActivation.toSelect);
			activateLib(libActivation);
		}
	}

	protected void deactivateLibs(FullLibDependencyDeclaration[] toBeDeactivated, FullLibDependencyDeclaration[] uninstalled,
		FullLibDependencyDeclaration toActivate)
	{
		if (toBeDeactivated != null)
		{
			for (FullLibDependencyDeclaration libVersion : toBeDeactivated)
			{
				deactivateLib(libVersion, false, toActivate);
			}
		}
		if (uninstalled != null)
		{
			for (FullLibDependencyDeclaration libVersion : uninstalled)
			{
				deactivateLib(libVersion, true, toActivate);
			}
		}
	}

	protected void deactivateLib(FullLibDependencyDeclaration libVersion, boolean uninstalled, FullLibDependencyDeclaration toActivate)
	{
		File libFileToBeRemoved = getLibFile(libVersion);
		if (libFileToBeRemoved != null && (libFileToBeRemoved.exists() || uninstalled))
		{
			// check to see if it's used in any plugin jnlp file; if it is, edit the JNLP and remember the change
			if (!toActivate.relativePath.equals(libVersion.relativePath))
			{
				if (pluginsDir.exists() && pluginsDir.isDirectory())
				{
					Collection<File> jnlps = FileUtils.listFiles(pluginsDir, new String[] { "jnlp" }, true); //$NON-NLS-1$
					Iterator<File> it = jnlps.iterator();
					while (it.hasNext())
					{
						File jnlp = it.next();
						replaceReferencesInJNLP(jnlp, libFileToBeRemoved, toActivate);
					}
				}
				else
				{
					Debug.warn("[RI] Cannot find plugins directory when choosing lib version."); //$NON-NLS-1$
				}
			}

			if (!uninstalled)
			{
				// delete the lib file
				boolean deleted = libFileToBeRemoved.delete();
				if (!deleted)
				{
					String tmp = "Cannot deactivate lib version " + libVersion + " - " + libFileToBeRemoved + "."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					messages.addError(tmp);
					Debug.warn(tmp);
				}
			}
		}
	}

	private String getJNLPHrefFromRelativePath(String libPathToActivate)
	{
		File appServerDir = new File(installDir, APP_SERVER_DIR);
		File f = new File(installDir, libPathToActivate);
		String result = f.getName();
		while (!appServerDir.equals(f.getParentFile()) && !installDir.equals(f.getParentFile()))
		{
			f = f.getParentFile();
			result = f.getName() + "/" + result; //$NON-NLS-1$
		}
		if (installDir.equals(f.getParentFile()))
		{
			result = "../" + result; //$NON-NLS-1$
		}
		result = "/" + result; //$NON-NLS-1$
		return result; // must be relative to app. server dir
	}

	protected File getLibFile(FullLibDependencyDeclaration libVersion)
	{
		File f = new File(installDir, libVersion.relativePath);
		if (!ExtensionUtils.isInParentDir(installDir, f))
		{
			String msg = "[RI] Lirary path points outside of the install dir for " + libVersion + ": " + f; //$NON-NLS-1$ //$NON-NLS-2$
			Debug.warn(msg);
			messages.addError(msg);
			f = null;
		}
		return f;
	}

	protected void replaceReferencesInJNLP(File jnlp, File libFileToBeRemoved, FullLibDependencyDeclaration toActivate)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new XMLErrorHandler(jnlp.getName()));

			Document doc = db.parse(jnlp);
			Element root = doc.getDocumentElement();
			root.normalize();

			NodeList list;
			if ("jnlp".equals(root.getNodeName())) //$NON-NLS-1$
			{
				list = root.getElementsByTagName("resources"); //$NON-NLS-1$
				if (list != null && list.getLength() == 1 && list.item(0).getNodeType() == Node.ELEMENT_NODE)
				{
					File appServerDir = new File(installDir, APP_SERVER_DIR);
					Element resourcesNode = (Element)list.item(0);
					boolean replaced1 = findAndReplaceReferences(resourcesNode, "jar", appServerDir, libFileToBeRemoved, toActivate); //$NON-NLS-1$
					boolean replaced2 = findAndReplaceReferences(resourcesNode, "nativelib", appServerDir, libFileToBeRemoved, toActivate); //$NON-NLS-1$

					if (replaced1 || replaced2)
					{
						// save back the jnlp file
						try
						{
							writeBackXML(doc, jnlp, doc.getXmlEncoding());
						}
						catch (Exception e)
						{
							messages.addError("Cannot write back jnlp file (when deactivating lib): " + jnlp); //$NON-NLS-1$ 
							Debug.error(e);
						}
					}
				}
			}
		}
		catch (ParserConfigurationException e)
		{
			String msg = "Cannot parse jnlp '" + jnlp.getName() + "'."; //$NON-NLS-1$ //$NON-NLS-2$
			Debug.log(msg, e);
		}
		catch (SAXException e)
		{
			String msg = "Cannot parse jnlp '" + jnlp.getName() + "'."; //$NON-NLS-1$ //$NON-NLS-2$
			Debug.log(msg, e);
		}
		catch (IOException e)
		{
			String msg = "Cannot parse jnlp '" + jnlp.getName() + "'."; //$NON-NLS-1$ //$NON-NLS-2$
			Debug.log(msg, e);
		}
		catch (FactoryConfigurationError e)
		{
			String msg = "Cannot parse jnlp '" + jnlp.getName() + "'. Please report this problem to Servoy."; //$NON-NLS-1$ //$NON-NLS-2$
			Debug.error(msg, e);
		}
	}

	protected String getRelativePluginPath(File jnlp)
	{
		String result = jnlp.getName();
		File current = jnlp.getParentFile();
		while (current != null && !current.equals(pluginsDir))
		{
			result = current.getName() + "/" + result; //$NON-NLS-1$
			current = current.getParentFile();
		}
		return current == null ? jnlp.getAbsolutePath() : result;
	}

	protected File getRelativePluginFile(String relativePluginPath)
	{
		File f = new File(relativePluginPath);
		if (!f.isAbsolute())
		{
			f = new File(pluginsDir, relativePluginPath);
		}
		return f;
	}

	protected boolean findAndReplaceReferences(Element el, String tagName, File appServerDir, File libFileToBeRemoved, FullLibDependencyDeclaration toActivate)
	{
		boolean replaced = false;
		NodeList list = el.getElementsByTagName(tagName);
		if (list != null && list.getLength() > 0)
		{
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
							element.setAttribute("href", getJNLPHrefFromRelativePath(toActivate.relativePath)); //$NON-NLS-1$
							String version = element.getAttribute("version"); //$NON-NLS-1$
							if (version != null && !version.equals("%%version%%")) //$NON-NLS-1$
							{
								element.setAttribute("version", toActivate.version); //$NON-NLS-1$
							}
							replaced = true;
						}
					}
				}
			}
		}
		return replaced;
	}

	protected void writeBackXML(Document doc, File f, String encoding) throws IOException, TransformerFactoryConfigurationError, TransformerException,
		DocumentException
	{
		BufferedOutputStream os = null;
		try
		{
			os = new BufferedOutputStream(new FileOutputStream(f));
			doc.normalize();
			StringWriter sw = new StringWriter(1024);
			DOMSource source = new DOMSource(doc);
			Transformer newTransformer = TransformerFactory.newInstance().newTransformer();
			newTransformer.setOutputProperty(OutputKeys.ENCODING, encoding);
			newTransformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
			newTransformer.transform(source, new StreamResult(sw));

			// normally the transformer code above should have been enough for producing pretty formatted XML (needed so that repeated remove/restore of tags
			// doesn't produce endless newlines or other bad looking XML); but it seems that with some versions of the JDK that doesn't do it's job so we use dom4j
			final OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding(encoding);
			final XMLWriter writer = new XMLWriter(os, format);
			writer.write(DocumentHelper.parseText(sw.toString()));
		}
		finally
		{
			Utils.closeOutputStream(os);
		}

	}

	protected void activateLib(final LibActivation libActivation)
	{
		// if it's not available (already installed but not used version), then make it available (take it from .exp, restore any jnlp references that were removed)
		final File libFile = getLibFile(libActivation.toSelect);
		if (libFile != null)
		{
			// restore lib file from .exp file, even if it exists, as more then one extension might point to the same lib file location
			// and you can't be sure it was not overwritten
			try
			{
				if (!ExtensionUtils.extractZipEntryToFile(libActivation.toSelectSourceExp, libActivation.toSelect.relativePath, libFile))
				{
					String tmp = "Cannot find lib version " + libActivation.toSelect + " in .exp file: " + libActivation.toSelectSourceExp; //$NON-NLS-1$//$NON-NLS-2$
					messages.addError(tmp);
					Debug.error(tmp);
				}
			}
			catch (IOException e)
			{
				messages.addError("Unable to restore lib version " + libActivation.toSelect + " from .exp file: " + libActivation.toSelectSourceExp); //$NON-NLS-1$//$NON-NLS-2$
				Debug.error(e);
			}
		}
	}

	public Message[] getMessages()
	{
		return messages.getMessages();
	}

	public void clearMessages()
	{
		messages.clearMessages();
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
			messages.addWarning("Warning when parsing '" + fileName + "' for lib version activation.");
			Debug.trace(exception);
		}

		public void error(SAXParseException exception) throws SAXException
		{
			messages.addWarning("Error when parsing '" + fileName + "' for lib version activation.");
			Debug.log(exception);
		}

		public void fatalError(SAXParseException exception) throws SAXException
		{
			messages.addWarning("Cannot parse '" + fileName + "' for lib version activation.");
			Debug.log(exception);
		}

	}

}