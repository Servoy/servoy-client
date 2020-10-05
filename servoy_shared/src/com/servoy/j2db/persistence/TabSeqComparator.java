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
import java.util.Comparator;
import java.util.List;

/**
 * @author jcompagner,jblok
 */
public class TabSeqComparator implements Comparator<ISupportTabSeq>
{
	public static final Comparator<ISupportTabSeq> INSTANCE = new TabSeqComparator();

	private TabSeqComparator()
	{
	}

	public int compare(ISupportTabSeq o1, ISupportTabSeq o2)
	{
		int seq1 = o1.getTabSeq();
		int seq2 = o2.getTabSeq();

		return compareTabSeq(seq1, o1, seq2, o2);
	}

	public static int compareTabSeq(int seq1, Object o1, int seq2, Object o2)
	{
		int yxCompare = 0;
		if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT && o1 instanceof IPersist && o2 instanceof IPersist)
		{
			IPersist form = ((IPersist)o1).getAncestor(IRepository.FORMS);
			if (form instanceof Form && ((Form)form).isResponsiveLayout())
			{
				if (((IPersist)o1).getParent().equals(((IPersist)o2).getParent()))
				{
					//delegate to Yx
					yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare((IPersist)o1, (IPersist)o2);
				}
				else
				{
					/*
					 * We must search all the parents of the objects o1 and o2.If the objects have a same parent by searching all the ancestors we must compare
					 * the objects before encountering the same parent.
					 */
					List<IPersist> parentsOfo1 = new ArrayList<IPersist>();
					IPersist parent = ((IPersist)o1).getParent();
					while (!(parent instanceof Form))
					{
						parentsOfo1.add(parent);
						parent = parent.getParent();
					}
					//also add the form to the list of parents
					parentsOfo1.add(parent);

					IPersist childo2 = (IPersist)o2;
					IPersist parento2 = ((IPersist)o2).getParent();
					while (!(parento2 instanceof Form))
					{
						if (parentsOfo1.contains(parento2))
						{
							int index = parentsOfo1.indexOf(parento2);
							//delegate to Yx
							if (index > 0)
							{
								yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(index - 1), childo2);
							}
						}
						childo2 = parento2;
						parento2 = parento2.getParent();
					}
					//also check to see if the common parent is the actual form
					if (parentsOfo1.contains(parento2))
					{
						int index = parentsOfo1.indexOf(parento2);
						if (index > 0)
						{
							yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare(parentsOfo1.get(index - 1), childo2);
						}
					}
				}
			}
			else if (form instanceof Form && !((Form)form).isResponsiveLayout())
			{
				yxCompare = PositionComparator.YX_PERSIST_COMPARATOR.compare((IPersist)o1, (IPersist)o2);
			}
			// if they are at the same position, and are different persist, just use UUID to decide the sequence
			return yxCompare == 0 ? ((IPersist)o1).getUUID().compareTo(((IPersist)o2).getUUID()) : yxCompare;
		}
		else if (seq1 == ISupportTabSeq.DEFAULT)
		{
			return 1;
		}
		else if (seq2 == ISupportTabSeq.DEFAULT)
		{
			return -1;
		}
		return seq1 - seq2;
	}

}