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

import java.util.ArrayList;
import java.util.List;


/**
 * @author jcompagner
 *
 */
public final class RelationList
{
	private final Relation relation;

	private final RelationList parent;

	private final short size;

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
		List<Relation> lst = new ArrayList<Relation>(5);
		lst.add(relation);
		RelationList parentWalker = parent;
		while (parentWalker != null)
		{
			lst.add(parentWalker.relation);
			parentWalker = parentWalker.parent;
		}
		int counter = 0;
		Relation[] relations = new Relation[lst.size()];
		for (int i = lst.size(); --i >= 0;)
		{
			relations[counter++] = lst.get(i);
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
		return relation.hashCode() + (parent == null ? 0 : parent.hashCode());
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
}
