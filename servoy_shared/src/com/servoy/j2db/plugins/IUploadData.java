/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.j2db.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Container object holding the data of the uploaded files.
 *
 * NOTE: don't implement directly, this interface can change.
 * 
 * @author jcompagner
 * @since 5.1
 * @see IMediaUploadCallback
 */
public interface IUploadData
{
	/**
	 * If this data maps to a file it will return the real file system file, null otherwise.
	 *   
	 * @return File object
	 */
	File getFile();

	/**
	 * The contents of this uploaded data.
	 * @return
	 */
	byte[] getBytes();

	/**
	 * The name of the file on the users machine.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * The content type of the data.
	 * @return
	 */
	String getContentType();

	/**
	 * An inputStream on the data
	 * @return
	 */
	InputStream getInputStream() throws IOException;
}
