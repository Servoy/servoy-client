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
package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;


/**
 * Runtime property interface for image support.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface HasRuntimeImage
{
	/**
	 * Gets/Sets the image displayed on a button or label; based on URL. 
	 * 
	 * @sample
	 * //dynamically sets an image displayed on a button or label
	 * %%prefix%%%%elementName%%.imageURL = "http://www.servoy.com/images/test.gif";
	 * 
	 * //sets an image from your own image library
	 * %%prefix%%%%elementName%%.imageURL = "media:///arrow.gif";
	 * 
	 * //loads an image (BLOB) from a field in a selected record into HTML
	 * %%prefix%%%%elementName%%.imageURL = 'media:///servoy_blobloader?datasource='+controller.getDataSource()+'&dataprovider=image_data&mimetype=image/jpeg&rowid1=2';	
	 */

	@JSGetter
	public String getImageURL();

	@JSSetter
	public void setImageURL(String text_url);


	/**
	 * Gets/Sets the image displayed on a button or label roll over; based on URL. 
	 * 
	 * @sample
	 * //dynamically sets a roll over image displayed on a button or label
	 * %%prefix%%%%elementName%%.rolloverImageURL = "http://www.servoy.com/images/test.gif";
	 * 
	 * //sets an image from your own image library
	 * %%prefix%%%%elementName%%.rolloverImageURL = "media:///arrow.gif";
	 * 
	 * //loads an image (BLOB) from a field in a selected record into HTML
	 * %%prefix%%%%elementName%%.rolloverImageURL = 'media:///servoy_blobloader?datasource='+controller.getDataSource()+'&dataprovider=image_data&mimetype=image/jpeg&rowid1=2';
	 * 	
	 */
	@JSGetter
	public String getRolloverImageURL();

	@JSSetter
	public void setRolloverImageURL(String image_url);

	/**
	 * Returns the image data in .jpg format from an icon; thumbnailing only works in record view. 
	 *
	 * @sample
	 * var jpgData = %%prefix%%%%elementName%%.getThumbnailJPGImage();
	 * plugins.file.writeFile("mypicture.jpg", jpgData);
	 *
	 * @return An array of bytes.
	 */
	@JSFunction
	public byte[] getThumbnailJPGImage();

	/**
	 * Returns the image data in .jpg format from an icon; thumbnailing only works in record view. 
	 *
	 * @sample
	 * var jpgData = %%prefix%%%%elementName%%.getThumbnailJPGImage(50, 50);
	 * plugins.file.writeFile("mypicture.jpg", jpgData);
	 *
	 * @param width The target width, use -1 for original image width.
	 * @param height The target height, use -1 for original image height.
	 * 
	 * @return An array of bytes.
	 */

	@JSFunction
	public byte[] getThumbnailJPGImage(int width, int height);


}