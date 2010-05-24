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


import java.util.Comparator;

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

		if (seq1 == ISupportTabSeq.DEFAULT && seq2 == ISupportTabSeq.DEFAULT && o1 instanceof IPersist && o2 instanceof IPersist)
		{
			//delegate to Yx
			return PositionComparator.YX_PERSIST_COMPARATOR.compare((IPersist)o1, (IPersist)o2);
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