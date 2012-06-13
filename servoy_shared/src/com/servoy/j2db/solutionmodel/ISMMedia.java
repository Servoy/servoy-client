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

package com.servoy.j2db.solutionmodel;

/**
 * Solution model media object.
 * 
 * @author rgansevles
 *
 * @since 6.1
 */
public interface ISMMedia extends ISMHasUUID
{
	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getMimeType()
	 * 
	 * @sampleas getBytes()
	 */
	public String getMimeType();

	public void setMimeType(String type);

	/**
	 * @clonedesc com.servoy.j2db.persistence.Media#getName()
	 * 
	 * @sampleas getBytes()
	 * 
	 * @return A String holding the name of this Media object.
	 */
	public String getName();

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
	public byte[] getBytes();

	public void setBytes(byte[] bytes);

}