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
package com.servoy.j2db.scripting.solutionmodel;

import java.nio.charset.Charset;

import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.solutionmodel.ISMMedia;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.UUID;

/**
 * <p><code>JSMedia</code> is a media wrapper designed for managing media content within solutions. It enables manipulation of media properties such as content bytes, MIME types, and metadata through a set of defined properties and methods.</p>
 *
 * <p>The <code>bytes</code> property represents the content of the media as a byte array. This allows direct modification of the media content while retaining its original name. For example, users can read image files, replace their content, and verify changes programmatically. The <code>mimeType</code> property specifies the type of media, such as 'image/jpg' or 'image/png'. It can be updated alongside the content to reflect new media formats without altering the media’s name.</p>
 *
 * <p>JSMedia provides several methods for interacting with media objects:</p>
 * <ul>
 *   <li><b><code>getAsString()</code></b>: Converts the byte content into a UTF-8 encoded string, returning <code>null</code> if the conversion fails or if the byte content is unavailable.</li>
 *   <li><b><code>getName()</code></b>: Retrieves the name of the media object, ensuring that changes to content or MIME type do not affect the original name.</li>
 *   <li><b><code>getUUID()</code></b>: Returns a unique identifier (UUID) for the media, allowing for precise identification.</li>
 *   <li><b><code>setAsString(string)</code></b>: Updates the media’s byte content using a UTF-8 encoded string, enabling quick modifications with string inputs.</li>
 * </ul>
 *
 * <p>For more comprehensive information, refer to the <a href="../../../servoy-developer/solution-explorer/all-solutions/active-solution/media/README.md">media</a> section of this documentation.</p>
 *
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSMedia implements IJavaScriptType, ISMMedia
{
	private Media media;
	private boolean isCopy;
	private final FlattenedSolution fs;

	public JSMedia(Media media, FlattenedSolution fs, boolean isCopy)
	{
		this.media = media;
		this.fs = fs;
		this.isCopy = isCopy;
	}

	private final void checkModification()
	{
		if (!isCopy)
		{
			// then get the replace the item with the item of the copied relation.
			media = fs.createPersistCopy(media);
			isCopy = true;
		}
	}

	/**
	 * @return the media
	 */
	public Media getMedia()
	{
		return media;
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getMimeType()
	 *
	 * @sampleas getBytes()
	 *
	 * @return The MIME type of this Media object.
	 */
	@JSGetter
	public String getMimeType()
	{
		return media.getMimeType();
	}

	@JSSetter
	public void setMimeType(String type)
	{
		checkModification();
		media.setMimeType(type);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getName()
	 *
	 * @sampleas getBytes()
	 *
	 * @return A String holding the name of this Media object.
	 */
	@JSFunction
	public String getName()
	{
		return media.getName();
	}

	/**
	 * A byte array holding the content of the Media object.
	 *
	 * @sample
	 * var ballBytes = plugins.file.readFile('d:/ball.jpg');
	 * var mapBytes = plugins.file.readFile('d:/map.png');
	 * var ballImage = solutionModel.newMedia('ball.jpg', ballBytes);
	 * application.output('original image name: ' + ballImage.getName());
	 * ballImage.bytes = mapBytes;
	 * ballImage.mimeType = 'image/png';
	 * application.output('image name after change: ' + ballImage.getName()); // The name remains unchanged. Only the content (bytes) are changed.
	 * application.output('image mime type: ' + ballImage.mimeType);
	 * application.output('image size: ' + ballImage.bytes.length);
	 *
	 * @return A byte array holding the content of the Media object.
	 */
	@JSGetter
	public byte[] getBytes()
	{
		return media.getMediaData();
	}

	@JSSetter
	public void setBytes(byte[] bytes)
	{
		checkModification();
		media.setPermMediaData(bytes);

		if (bytes != null)
		{
			media.setMimeType(MimeTypes.getContentType(bytes));
		}
	}

	/**
	 * Returns this media's bytes a a String converting it with the UTF-8 Charset.
	 * Returns null if it couldn't convert it or the bytes where null.
	 *
	 * @return This media's bytes as a string converted with the UTF-8 charset, or null if conversion is not possible or bytes are null.
	 */
	@JSFunction
	public String getAsString()
	{
		if (media.getMediaData() != null) return new String(media.getMediaData(), Charset.forName("UTF-8")); //$NON-NLS-1$
		return null;
	}

	/**
	 * Sets the bytes of this media to the give String that is converted to bytes using the UTF-8 Charset.
	 *
	 * @param string
	 */
	@JSFunction
	public void setAsString(String string)
	{
		setBytes(string.getBytes(Charset.forName("UTF-8"))); //$NON-NLS-1$
	}

	/**
	 * Returns the UUID of this media
	 *
	 * @sample
	 * var ballImg = plugins.file.readFile('d:/ball.jpg');
	 * application.output(ballImg.getUUID().toString());
	 *
	 * @return The UUID of this Media object.
	 */
	@JSFunction
	public UUID getUUID()
	{
		return media.getUUID();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("nls")
	@Override
	public String toString()
	{
		return "JSMedia[name: " + media.getName() + ']';
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((media == null) ? 0 : media.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JSMedia other = (JSMedia)obj;
		if (media == null)
		{
			if (other.media != null) return false;
		}
		else if (!media.getUUID().equals(other.media.getUUID())) return false;
		return true;
	}
}
