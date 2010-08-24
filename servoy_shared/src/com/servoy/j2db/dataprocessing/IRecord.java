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
package com.servoy.j2db.dataprocessing;


/**
 * The Record interface for business objects (table rows)
 * 
 * @author jblok
 * @since Servoy 5.0
 */
public interface IRecord extends IRecordDeprecated
{
	/**
	 * Start the edit of a record, must be called before any setValue.
	 * 
	 * @return true if successful start of edit, since the record can be locked
	 */
	public boolean startEditing();

	/**
	 * @param dataProviderID to set
	 * @param value new value
	 * @return the old value, or value if no change
	 */
	public Object setValue(String dataProviderID, Object value);

	/**
	 * Get a record value
	 * 
	 * @param dataProviderID
	 * @return the value
	 */
	public Object getValue(String dataProviderID);

	/**
	 * Returns true if the dataprovider can be resolved by this object.
	 * 
	 * @param dataprovider
	 * @return true if the dataprovider can be resolved by this object.
	 */
	boolean has(String dataprovider);

	/**
	 * Get the foundset this record belongs to
	 * 
	 * @return the foundset
	 */
	public IFoundSet getParentFoundSet();

	/**
	 * Get a related foundset for this record
	 * 
	 * @param relationName
	 * @return the foundset
	 */
	public IFoundSet getRelatedFoundSet(String relationName);

	/**
	 * The the record identifier (primary key)
	 * 
	 * @return the record PK
	 */
	public Object[] getPK();

	/**
	 * Check if this is a new not yet saved record
	 * 
	 * @return exists in datasource status
	 */
	public boolean existInDataSource();

	/**
	 * Check to see if locked
	 * 
	 * @return locked status
	 */
	public boolean isLocked();

	/**
	 * Add modification listener
	 * 
	 * @param l the listener
	 */
	public void addModificationListener(IModificationListener l);

	/**
	 * Remove modification listener
	 * 
	 * @param l the listener
	 */
	public void removeModificationListener(IModificationListener l);
}
