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
	 * @return
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
