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


/**
 * @author jcompagner
 */
public abstract class RuntimeProperty<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj != null && getClass().isInstance(obj);
	}

	/**
	 * @param metaData Array of metadata to search
	 * @return The entry value
	 */
	@SuppressWarnings("unchecked")
	T get(PropertyEntry[] propertyEntries)
	{
		if (propertyEntries != null)
		{
			for (PropertyEntry m : propertyEntries)
			{
				if (equals(m.key))
				{
					return (T)m.object;
				}
			}
		}
		return null;
	}

	/**
	 * @param metaData The array of metadata
	 * @param object The object to set, null to remove
	 * @return Any new metadata array (if it was reallocated)
	 */
	PropertyEntry[] set(final PropertyEntry[] metaData, final T object)
	{
		boolean set = false;
		PropertyEntry[] returnMetaData = metaData;
		if (returnMetaData != null)
		{
			for (int i = 0; i < returnMetaData.length; i++)
			{
				PropertyEntry m = returnMetaData[i];
				if (equals(m.key))
				{
					if (object != null)
					{
						// set new value
						m.object = object;
					}
					else
					{
						// remove value and shrink or null array
						if (returnMetaData.length > 1)
						{
							int l = returnMetaData.length - 1;
							PropertyEntry[] newMetaData = new PropertyEntry[l];
							System.arraycopy(returnMetaData, 0, newMetaData, 0, i);
							System.arraycopy(returnMetaData, i + 1, newMetaData, i, l - i);
							returnMetaData = newMetaData;
						}
						else
						{
							returnMetaData = null;
							break;
						}
					}
					set = true;
				}
			}
		}
		if (!set && object != null)
		{
			PropertyEntry m = new PropertyEntry(this, object);
			if (returnMetaData == null)
			{
				returnMetaData = new PropertyEntry[1];
				returnMetaData[0] = m;
			}
			else
			{
				final PropertyEntry[] newMetaData = new PropertyEntry[returnMetaData.length + 1];
				System.arraycopy(returnMetaData, 0, newMetaData, 0, returnMetaData.length);
				newMetaData[returnMetaData.length] = m;
				returnMetaData = newMetaData;
			}
		}
		return returnMetaData;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().toString();
	}
}
