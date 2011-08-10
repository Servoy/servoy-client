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
 * @author jcompagner
 * 
 */
public interface IScriptLabelMethods extends IScriptDataProviderMethods
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

	public String js_getImageURL();

	public void js_setImageURL(String text_url);


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

	public String js_getRolloverImageURL();

	public void js_setRolloverImageURL(String image_url);

	/**
	 * Returns the image data in .jpg format from an icon; thumbnailing only works in record view. 
	 * 
	 * Syntax
	 * elements.elementName.getThumbnailJPGImage([width],[height])
	 *
	 * @sample
	 * var jpgData = %%prefix%%%%elementName%%.getThumbnailJPGImage(50,50);
	 * application.writeFile("mypicture.jpg", jpgData);
	 *
	 * @param width optional The target width, if not specified original image width will be used.
	 *
	 * @param height optional The target height, if not specified original image width will be used.
	 * 
	 * @return An array of bytes.
	 */
	public byte[] js_getThumbnailJPGImage(Object[] args);

	/**
	 * Gets or sets the specified character(s) - typically an underlined letter- used with/without the modifier key(s) for the label, button or image. 
	 * 
	 * Modifiers key values: 
	 * 1 SHIFT 
	 * 2 CTRL 
	 * 4 Meta/CMD (Macintosh)
	 * 8 ALT(Windows, Unix); OPTION (Macintosh) 
	 * 
	 * NOTE: A mnemonic is usually a single key used with/without the CTRL, CMD, SHIFT, ALT, or OPTION key(s) to activate a menu item or command - depending, in part on whether the menmonic applies in a command line or graphic interface. For one description, you can refer to this web page: http://msdn.microsoft.com/en-us/library/bb158536.aspx or perform a search in a web browser search engine using the criteria "mnemonic".
	 * NOTE2: Mnemonic is only supported in Smart Client.
	 * 
	 * @sample
	 * //gets the mnemonic of the element
	 * var my_mnemoic = %%prefix%%%%elementName%%.mnemonic;
	 * 
	 * //sets the mnemonic of the element
	 * %%prefix%%%%elementName%%.mnemonic = 'f';
	 */
	public String js_getMnemonic();

	public void js_setMnemonic(String mnemonic);

	/**
	 * Gets or sets the display formatting of a label/button that has dataProviderID; does not affect the actual value stored in the database column.
	 *
	 * @sample
	 * //sets the display formatting of the label/button
	 * %%prefix%%%%elementName%%.format = '###';
	 * 
	 * //gets the display formatting of the label/button
	 * var format = %%prefix%%%%elementName%%.format;
	 */
	public String js_getFormat();

	public void js_setFormat(String format);


}