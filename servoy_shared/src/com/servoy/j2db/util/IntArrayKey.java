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
package com.servoy.j2db.util;


import java.util.Arrays;

/**
 * The <code>IntArrayKey</code> class is a wrapper around an <code>int[]</code> which can be used to index a Hashtable or HashMap. This class implements the
 * <code>hashCode</code>, and <code>equals</code> methods as specified by the <code>Hashtable</code> class. The <code>equals</code> methods implements
 * true equality (of the contents), not reference equality. Furthermore, it implements the <code>Cloneable</code> interface which allows the key to be copied
 * (with deep copy semantics, i.e. the contents are cloned as well).
 * 
 * @see java.util.Hashtable
 * @see java.util.HashMap
 * @see Object#equals(Object)
 * @see Object#hashCode()
 * @see Object#clone()
 * @see Cloneable
 * 
 */
public class IntArrayKey implements Cloneable
{

	/**
	 * A reference to the <code>int[]</code> which represents the contents of this class.
	 */
	protected int intArray[];

	/**
	 * Temporary array which is used to calculate the hash of <code>intArray</code>.
	 */
	protected int hash[];

	/**
	 * Boolean to indicate whether the hash was previously calculated and is currently cached.
	 */
	protected boolean isHashCached;

	/**
	 * Constructs a new <code>IntArrayKey</code> with a <code>null</code> array as contents.
	 */
	public IntArrayKey()
	{
		this(null);
	}

	/**
	 * Constructs a new <code>IntArrayKey</code> with the specified array as contents.
	 * 
	 * @param intArray the array which will represent the contents of this key (may be <code>null</code>)
	 */
	public IntArrayKey(int intArray[])
	{
		hash = new int[3];
		setIntArray(intArray);
	}

	/**
	 * Returns the array which represents the contents of this key.
	 * 
	 * @return the array which represents the contents of this key
	 */
	public synchronized int[] getIntArray()
	{
		return intArray;
	}

	/**
	 * Sets the contents of this key to the specified array. The cached value of the hash is thrown away.
	 * 
	 * @param intArray the array to which the contents of this key will be set
	 */
	public synchronized void setIntArray(int intArray[])
	{
		this.intArray = intArray;
		isHashCached = false;
	}

	/**
	 * Returns whether or not this instance and the specified other object are equal. This is the case iff the other object is an instance of the
	 * <code>IntArrayKey</code> class and the contents arrays are equal.
	 * 
	 * @param other the other object to compare this instance with
	 * 
	 * @return whether or not this instance is equal to the specified instance
	 */
	@Override
	public synchronized boolean equals(Object other)
	{
		if (other instanceof IntArrayKey)
		{
			return Arrays.equals(intArray, ((IntArrayKey)other).intArray);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Calculate the hashcode for the contents of this instance.
	 * 
	 * @return a hashcode for the contents of this instance.
	 */
	@Override
	public synchronized int hashCode()
	{

		// If the hash code is cached return it immediately.
		if (isHashCached)
		{
			return hash[2];
		}

		// If the array is null return the constant value 0x31337157.
		if (intArray == null)
		{
			return 0x31337157;
		}

		// Otherwise calculate a hash.

		hash[0] = hash[1] = 0x9e3779b9;
		hash[2] = 0x31337157 + intArray.length;

		int i = 0, j = 0;
		while (i < intArray.length)
		{
			if (j == 3)
			{
				j = 0;
				mix();
			}
			hash[j++] += intArray[i++];
		}
		mix();
		isHashCached = true;

		return hash[2];
	}

	/**
	 * Mixes the values in the <code>hash</code> array to increase the dependencies of the hashcode on the different bits of the contents array.
	 */
	protected synchronized void mix()
	{
		int i = hash[0], j = hash[1], k = hash[2];
		i -= j;
		i -= k;
		i ^= k >> 13;
		j -= k;
		j -= i;
		j ^= i << 8;
		k -= i;
		k -= j;
		k ^= j >> 13;
		i -= j;
		i -= k;
		i ^= k >> 12;
		j -= k;
		j -= i;
		j ^= i << 16;
		k -= i;
		k -= j;
		k ^= j >> 5;
		i -= j;
		i -= k;
		i ^= k >> 3;
		j -= k;
		j -= i;
		j ^= i << 10;
		k -= i;
		k -= j;
		k ^= j >> 15;
		hash[0] = i;
		hash[1] = j;
		hash[2] = k;
	}

}
