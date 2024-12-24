/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.j2db.scripting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IFile;
import com.servoy.j2db.plugins.IUploadData;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * The <code>JSUpload</code> class provides robust tools for managing uploaded files in Servoy applications.
 * It supports accessing file contents as bytes, strings, or input streams and provides metadata retrieval for handling uploads.
 * Developers can determine if files are stored in memory or on disk and write them to specified locations using the <code>write</code> method,
 * which manages temporary files.
 *
 * Metadata associated with uploads, such as form field names and their values, can be accessed using the <code>getFields</code> and
 * <code>getFieldValue</code> methods. The class also allows retrieval of file-specific details, including size, name, and content type,
 * while ensuring compatibility with browsers that may include full file paths.
 *
 * The <code>deleteFile</code> method explicitly removes temporary files to free resources, complementing automatic cleanup processes.
 * By combining file content management with metadata handling, <code>JSUpload</code> offers a streamlined solution for file upload operations
 * in Servoy applications.
 *
 * @author jcompagner
 * @since 2019.09
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, scriptingName = "JSUpload")
public class JSUpload implements IUploadData, IJavaScriptType, IFile
{
	private final Object item;
	private final Map<String, String> formFields;

	public JSUpload(Object item, Map<String, String> formFields)
	{
		// inlining casting is needed because of smart client, that doesn't have a FileItem
		this.item = item;
		this.formFields = formFields;
	}

	/**
	 * If this returns false, then a tmp file is created for it. This means that you can also convert this to a JSFile and call rename() on it.
	 * But the method write(file) will always work by writing the contents of this upload file to a different file.
	 *
	 * @return true if this upload is fully in memory (not saved to a temp file)
	 */
	@JSFunction
	public boolean isInMemory()
	{
		return ((FileItem)item).isInMemory();
	}

	/**
	 * @return the size of the upload
	 */
	@JSFunction
	public long getSize()
	{
		return ((FileItem)item).getSize();
	}

	/**
	 * Returns the contents of the file as as string in UTF-8 encoding.
	 * @return the String contents
	 */
	@JSFunction
	public String getString()
	{
		try
		{
			return ((FileItem)item).getString("UTF-8"); //$NON-NLS-1$
		}
		catch (UnsupportedEncodingException e)
		{
		}
		return ((FileItem)item).getString();
	}

	/**
	 * Writes the contents of this upload right to a file. Use the file plugin to create a JSFile object that can be given to this function.
	 * If this file was not fully in memory (isInMemory == false) then this will just stream the tmp file to the give file.
	 * If it was a temp file then it will try to move the file to the given location (so temp file is moved and because of that already deleted/cleaned up).
	 *
	 * @param file the file object where to write to can be a JSFile or path string
	 * @return if write could be done
	 */
	@JSFunction
	public boolean write(Object file)
	{
		Object f = file;
		if (f instanceof IFile) f = ((IFile)f).getFile();
		if (f instanceof String) f = new File((String)f);
		if (f instanceof File)
		{
			try
			{
				((FileItem)item).write((File)f);
			}
			catch (Exception e)
			{
				Debug.error(e);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Delets this uploaded file so it will be cleaned up if it was streamed in a temp file.
	 * The system tries to clean this up for you, but that can take a while and depends on Garbage Collection.
	 * So it is better to be explicit and delete this file.
	 * if you use JSUpload.write(file) then the file is very likely moved instead of copied so the temp file is also removed.
	 *
	 * @since 2021.12
	 */
	@JSFunction
	public void deleteFile()
	{
		((FileItem)item).delete();
	}

	/**
	 * This returns the field names of the form fields that where give as metadata to this upload file.
	 *
	 * @return String[] Array of names of the field names
	 */
	@JSFunction
	public String[] getFields()
	{
		return formFields != null ? formFields.keySet().toArray(new String[0]) : new String[0];
	}

	/**
	 * Returns the value for a give form field that was give as metadata to this uploaded file
	 *
	 * @param name The form fields name
	 * @return the value that was given or null
	 */
	@JSFunction
	public String getFieldValue(String name)
	{
		return formFields != null ? formFields.get(name) : null;
	}

	/**
	 * @return the java file object if it is backed by a file, use file plugin.convertToJSFile(upload) to convert this to a JSFile to work with it.
	 */
	public File getFile()
	{
		if (item instanceof DiskFileItem)
		{
			return ((DiskFileItem)item).getStoreLocation();
		}
		else if (item instanceof IFile)
		{
			return ((IFile)item).getFile();
		}
		return null;
	}

	/**
	 * This returns the bytes of the uploaded file, try to using streaming or file operation on it (so the bytes don't have to be full loaded in to memory)
	 *
	 * @return the bytes of the upload file,
	 */
	@JSFunction
	public byte[] getBytes()
	{
		return ((FileItem)item).get();
	}


	/**
	 * @return the name of the upload file.
	 */
	@JSFunction
	public String getName()
	{
		String name = ((FileItem)item).getName();

		// when uploading from localhost some browsers will specify the entire path, we strip it
		// down to just the file name
		name = Utils.lastPathComponent(name, '/');
		name = Utils.lastPathComponent(name, '\\');

		name = name.replace('\\', '/');
		String[] tokenized = name.split("/"); //$NON-NLS-1$
		return tokenized[tokenized.length - 1];
	}

	/**
	 * @return the content type of this upload
	 */
	@JSFunction
	public String getContentType()
	{
		return ((FileItem)item).getContentType();
	}

	/**
	 * @return the java input stream object.
	 */
	public InputStream getInputStream()
	{
		try
		{
			return ((FileItem)item).getInputStream();
		}
		catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * @return {long} The timestamp of the last modification, in milliseconds since the epoch.
	 */
	public long lastModified()
	{
		return System.currentTimeMillis();
	}

	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSUpload:" + getName() + ", size:" + getSize() + ", inmem: " + isInMemory() + ", file: " + getFile();
	}
}
