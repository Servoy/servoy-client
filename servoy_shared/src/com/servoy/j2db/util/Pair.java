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


import java.io.Serializable;

public class Pair<L, R> implements Comparable<Pair<L, R>>, Serializable
{
	private L left;
	private R right;

	public Pair(L left, R right)
	{
		this.left = left;
		this.right = right;
	}

	/**
	 * Returns the left.
	 *
	 * @return Object
	 */
	public L getLeft()
	{
		return left;
	}

	/**
	 * Returns the right.
	 *
	 * @return Object
	 */
	public R getRight()
	{
		return right;
	}

	/**
	 * Sets the left.
	 *
	 * @param left The left to set
	 */
	public void setLeft(L left)
	{
		this.left = left;
	}

	/**
	 * Sets the right.
	 *
	 * @param right The right to set
	 */
	public void setRight(R right)
	{
		this.right = right;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other != null && other instanceof Pair)
		{
			Pair< ? , ? > otherPair = (Pair< ? , ? >)other;
			return (left == otherPair.left || (left != null && left.equals(otherPair.left))) &&
				(right == otherPair.right || (right != null && right.equals(otherPair.right)));

		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return 0x96ab4911 ^ (left == null ? 0 : left.hashCode()) ^ (right == null ? 0 : right.hashCode());
	}

	public int compareTo(Pair<L, R> pair)
	{
		int i = ((Comparable<L>)left).compareTo(pair.left);
		if (i != 0) return i;
		return ((Comparable<R>)right).compareTo(pair.right);
	}

	@Override
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		buffer.append(left);
		buffer.append(", "); //$NON-NLS-1$
		buffer.append(right);
		buffer.append(']');
		return buffer.toString();
	}

}
