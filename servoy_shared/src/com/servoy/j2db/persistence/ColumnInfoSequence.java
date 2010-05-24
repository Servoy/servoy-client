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

public class ColumnInfoSequence extends Sequence
{
	private final long creationTime;
	private final int sequenceStepSize;
	private final int total_seq_size;

	public ColumnInfoSequence(long start_seq, int sequenceStepSize, int seq_cache_size, long creationTime)
	{
		super(start_seq, seq_cache_size);
		total_seq_size = seq_cache_size;
		this.sequenceStepSize = sequenceStepSize;
		this.creationTime = creationTime;
	}

	/**
	 * Warning all calls to this method needs to be synchronized by them selfs!!!
	 */
	@Override
	public long getNextVal()
	{
		if (seq_cache_size > 0)
		{
			long retval = seq;
			seq = seq + sequenceStepSize;
			seq_cache_size--;
			return retval;
		}
		else
		{
			return -1;
		}
	}

	public long getLastReturnedVal()
	{
		return seq - sequenceStepSize;
	}

	public long getCreationTime()
	{
		return creationTime;
	}

	public int getTotalSeqSize()
	{
		return total_seq_size;
	}

	public int getSequenceStepSize()
	{
		return sequenceStepSize;
	}

}