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
 *
 */
public final class RelationList
{
	private final Relation relation;

	private final RelationList parent;

	private final short size;

	private int hashcode = -1;

	/**
	 * 
	 */
	public RelationList(RelationList parent, Relation relation)
	{
		this.parent = parent;
		this.relation = relation;
		this.size = (short)(parent.size + 1);
	}

	/**
	 * @param relation2
	 */
	public RelationList(Relation relation)
	{
		this.relation = relation;
		this.parent = null;
		this.size = 1;
	}

	/**
	 * @return
	 */
	public Relation getRelation()
	{
		return relation;
	}

	/**
	 * @return
	 */
	public Relation[] getRelations()
	{
		Relation[] relations = new Relation[size];
		RelationList parentWalker = parent;
		relations[size - 1] = relation;
		for (int i = size - 2; parentWalker != null; i--)
		{
			relations[i] = parentWalker.relation;
			parentWalker = parentWalker.parent;
		}
		return relations;
	}

	/**
	 * @return
	 */
	public RelationList getParent()
	{
		return parent;
	}

	/**
	 * @param rel
	 * @return
	 */
	public boolean contains(Relation rel)
	{
		if (rel.equals(relation)) return true;
		RelationList parentWalker = parent;
		while (parentWalker != null)
		{
			if (rel.equals(parentWalker.relation)) return true;
			parentWalker = parentWalker.parent;
		}
		return false;
	}

	/**
	 * @return
	 */
	public int getSize()
	{
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		if (hashcode == -1)
		{
			if (parent != null)
			{
				hashcode = 31 * parent.hashCode() + relation.hashCode();
			}
			else
			{
				hashcode = 31 + relation.hashCode();
			}
		}
		return hashcode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj instanceof RelationList)
		{
			RelationList rl = (RelationList)obj;
			if (size != rl.size) return false;
			if (!relation.equals(rl.relation)) return false;
			if (parent == null)
			{
				return rl.parent == null;
			}
			return parent.equals(rl.parent);
		}
		return false;
	}

	/**
	 * @param i
	 * @return
	 */
	public Relation getRelation(int i)
	{
		int relationNumber = i + 1;
		if (size == relationNumber) return relation;
		RelationList parentWalker = parent;
		while (parentWalker != null)
		{
			if (parentWalker.size == relationNumber) return parentWalker.relation;
			parentWalker = parentWalker.parent;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		PrependingStringBuffer sb = new PrependingStringBuffer(size * 30);
		sb.prepend(relation.toString());
		RelationList parentWalker = parent;
		while (parentWalker != null)
		{
			sb.prepend('.');
			sb.prepend(parentWalker.relation.toString());
			parentWalker = parentWalker.parent;
		}

		return sb.toString();
	}


	public static class PrependingStringBuffer
	{
		private int size;
		private int position;

		private char[] buffer;

		/**
		 * Default constructor, the internal initial buffer size will be 16
		 */
		public PrependingStringBuffer()
		{
			this(16);
		}

		/**
		 * Constructs this PrependingStringBuffer with the given buffer size.
		 * 
		 * @param size
		 *            The initial size of the buffer.
		 */
		public PrependingStringBuffer(int size)
		{
			buffer = new char[size];
			position = size;
			this.size = 0;
		}

		/**
		 * Constructs and direct inserts the given string. The buffer size will be string.length+16
		 * 
		 * @param start
		 *            The string that is directly inserted.
		 */
		public PrependingStringBuffer(String start)
		{
			this(start.length() + 16);
			prepend(start);
		}

		/**
		 * Prepends one char to this PrependingStringBuffer
		 * 
		 * @param ch
		 *            The char that will be prepended
		 * @return this
		 */
		public PrependingStringBuffer prepend(char ch)
		{
			int len = 1;
			if (position < len)
			{
				expandCapacity(size + len);
			}
			position -= len;
			buffer[position] = ch;
			size += len;
			return this;
		}

		/**
		 * Prepends the string to this PrependingStringBuffer
		 * 
		 * @param str
		 *            The string that will be prepended
		 * @return this
		 */
		public PrependingStringBuffer prepend(String str)
		{
			int len = str.length();
			if (position < len)
			{
				expandCapacity(size + len);
			}
			str.getChars(0, len, buffer, position - len);
			position -= len;
			size += len;
			return this;
		}

		private void expandCapacity(int minimumCapacity)
		{
			int newCapacity = (buffer.length + 1) * 2;
			if (newCapacity < 0)
			{
				newCapacity = Integer.MAX_VALUE;
			}
			else if (minimumCapacity > newCapacity)
			{
				newCapacity = minimumCapacity;
			}

			char newValue[] = new char[newCapacity];
			System.arraycopy(buffer, position, newValue, newCapacity - size, size);
			buffer = newValue;
			position = newCapacity - size;
		}

		/**
		 * Returns the size of this PrependingStringBuffer
		 * 
		 * @return The size
		 */
		public int length()
		{
			return size;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			return new String(buffer, position, size);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == this)
			{
				return true;
			}
			else if (obj == null)
			{
				return false;
			}
			else
			{
				return toString().equals(obj.toString());
			}
		}

		@Override
		public int hashCode()
		{
			return toString().hashCode();
		}
	}
}
