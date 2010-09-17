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
package com.servoy.j2db.ui;

/**
 * Interface that defines the 2 suffix constants that can be used for a IMAGE_MEDIA field, 
 * to store the filename and mimetype in 2 dataproviders besides the byte[] dataprovider
 * 
 * These will be used for setting the filename and content type when the file is being downloaded to the client (in the webclient)
 * 
 * @author jcompagner
 */
public interface IMediaFieldConstants
{
	/**
	 * Constant that is used to identify (for upload/download) the media filename when the media data provider id suffixed with it is a valid text column/form
	 * variable.
	 */
	public static final String FILENAME = "_filename";
	/**
	 * Constant that is used to identify (for upload/download) the media mime type when the media data provider id suffixed with it is a valid text column/form
	 * variable.
	 */
	public static final String MIMETYPE = "_mimetype";
}
