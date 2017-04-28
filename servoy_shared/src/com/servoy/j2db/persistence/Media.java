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
package com.servoy.j2db.persistence;


import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * Media is binary data tagged with a mime type
 *
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, typeCode = IRepository.MEDIA)
public class Media extends AbstractBase implements ISupportName, ISupportEncapsulation, ICloneable, ISupportDeprecated
{
	public static final long serialVersionUID = 468097341226347599L;

	private transient byte[] media_data;
	// this shouldn't be transient, because of the solution serialize, then the last modified should be kept.
	private long lastModifiedTime = -1;
	byte[] perm_media_data; //only used in runtime/application server

	Media(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.MEDIA, parent, element_id, uuid);
	}

	/**
	 * @return the lastModifiedTime
	 */
	public long getLastModifiedTime()
	{
		return lastModifiedTime;
	}

	public int getBlobId()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_BLOBID).intValue();
	}

	public void setBlobId(int blob_id)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_BLOBID, blob_id);
		media_data = null;
	}

/*
 * public void setMediaData(byte[] media_data) throws RepositoryException { // Save the blob in the repository.
 * setBlobId(getRootObject().getChangeHandler().getLocalRepository().saveBlob(media_data)); // Get a reference to a the cached version in case this blob already
 * existed; otherwise this does nothing... // Note that this call will never go to the database since the blob data is already in memory. this.media_data =
 * getRootObject().getChangeHandler().getLocalRepository().getMediaBlob(blob_id); }
 */

	public byte[] getMediaData()
	{
		if (perm_media_data != null && media_data == null)
		{
			media_data = perm_media_data;
		}

		if (media_data == null && getBlobId() > 0)
		{
			try
			{
				if (getRootObject().getRepository() == null)
				{
					Debug.warn("Could not load media data (no repository");
				}
				else
				{
					// Lazy loading of media data.
					media_data = getRootObject().getRepository().getMediaBlob(getBlobId());
					lastModifiedTime = System.currentTimeMillis();
				}
			}
			catch (Exception ex)
			{
				media_data = new byte[0];
				Debug.error(ex);
			}
		}
		return media_data;
	}

	/**
	 * Should only be called by runtime builder and application server.
	 */
	public void makeBlobPermanent()
	{
		perm_media_data = media_data;
	}

	public void setName(String name)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, name);
	}

	/**
	 * The name of the Media object.
	 */
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * The MIME type of the Media object.
	 *
	 * Some examples are: 'image/jpg', 'image/png', etc.
	 */
	public String getMimeType()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_MIMETYPE);
	}

	public void setMimeType(String mime_type)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_MIMETYPE, mime_type);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * @param fileContent
	 */
	public void setPermMediaData(byte[] fileContent)
	{
		perm_media_data = fileContent;
		media_data = null;
		lastModifiedTime = System.currentTimeMillis();
	}

	@Override
	public void setEncapsulation(int arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION, arg);
	}

	/**
	 * The encapsulation mode of this Media. The following can be used:
	 *
	 * - Public (available in both scripting and designer from any module)
	 * - Module Scope - available in both scripting and designer but only in the same module.
	 *
	 * @return the encapsulation mode/level of the persist.
	 */
	@Override
	public int getEncapsulation()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_ENCAPSULATION).intValue();
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#getDeprecated()
	 */
	@Override
	public String getDeprecated()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED);
	}

	/*
	 * @see com.servoy.j2db.persistence.ISupportDeprecated#setDeprecated(String)
	 */
	@Override
	public void setDeprecated(String deprecatedInfo)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_DEPRECATED, deprecatedInfo);
	}
}
