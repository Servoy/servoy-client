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
 *  <p>
 * NOTE: do not implement this interface, it can change with new Servoy versions if new functionality is needed.
 * Records instances should be get through the {@link IFoundSet}
 * </p>
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
	 * Check if this is a record that has been deleted from code but not in the database yet
	 *
	 * @return exists in datasource status
	 */
	public boolean isFlaggedForDeletion();

	/**
	 * Check to see if locked
	 *
	 * @return locked status
	 */
	public boolean isLocked();

	/**
	 * Add modification listener.<br/><br/>
	 *
	 * NOTE: the listener received {@link ModificationEvent#getValue()} will almost always be correct,
	 * with one very rare exception: if {@link #getValue(String)} is called for a calculation in a non-event thread
	 * while the event thread is modifying the same record/value, resulting in different values - the order of
	 * the events might be wrong (so the ModificationEvent#getValue() that is fired last might have already
	 * been changed and received by the listener as the value before). Smart client used to do that, but that
	 * is obsolete. You can always get the value directly from the record in the listener to make sure it's the
	 * correct up-to-date one.
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

	/**
	 * Returns last occurred exception on this record (or null).
	 *
	 * @return The occurred exception.
	 */
	public Exception getException();

	default String getDataSource()
	{
		IFoundSet foundset = getParentFoundSet();
		if (foundset == null)
		{
			return null;
		}
		return foundset.getDataSource();
	}
}
