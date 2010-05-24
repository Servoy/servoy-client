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

public class Sequence
{
	protected long seq;
	protected int seq_cache_size;

	public Sequence(long start_seq, int seq_cache_size)
	{
		seq = start_seq;
		this.seq_cache_size = seq_cache_size;

	}

	/**
	 * Warning all calls to this method needs to be synchronized by them selfs!!!
	 */
	public long getNextVal()
	{
		if (seq_cache_size > 0)
		{
			long retval = seq;
			seq++;
			seq_cache_size--;
			return retval;
		}
		else
		{
			return -1;
		}
	}
}