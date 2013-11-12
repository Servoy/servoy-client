package com.servoy.j2db.ui.runtime;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

/**
 *  extracted from {@link com.servoy.j2db.ui.runtime.HasRuntimeImage} imageUrl  accesor methods for their own interface (this interface) 
 * 
 * @author obuligan
 */
public interface HasRuntimeImageURL
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
}
