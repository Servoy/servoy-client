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

import java.io.Serializable;
import java.lang.ref.SoftReference;

import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.scripting.ReferenceOnlyInJS;
import com.servoy.j2db.util.IntHashMap;

/**
 * Special wrapper values factory
 *
 * @author jblok
 */
public class ValueFactory
{
	private static IntHashMap<NullValue> nullValuesByType = new IntHashMap<NullValue>();

	public static class NullValue implements Serializable, Wrapper
	{
		private final int type;

		public NullValue(int sql_type)
		{
			type = sql_type;
		}

		/**
		 * @return int
		 */
		public int getType()
		{
			return type;
		}

		@Override
		public String toString()
		{
			return "NullValue with type: " + type; //$NON-NLS-1$
		}

		public Object unwrap()
		{
			return null;
		}
	}

	private static TableFlushValue tableFlushValue = new TableFlushValue();

	public static class TableFlushValue implements Serializable
	{
	}

	public final static class DbIdentValue implements Serializable, ReferenceOnlyInJS // visible/usable in JS as simple reference to create related records&stuff before save
	{
		private Object value;
		// to which this dbident belongs.
		private transient Row row;

		public Object getPkValue()
		{
			return this.value;
		}

		public void setPkValue(Object value)
		{
			this.value = value;
		}

		@Override
		public String toString()
		{
			if (value != null) return "DbIdentValue[" + value.toString() + ']'; //$NON-NLS-1$
			return "DbIdentValue" + hashCode(); //$NON-NLS-1$
		}

		public Row getRow()
		{
			return this.row;
		}

		public DbIdentValue setRow(Row row)
		{
			this.row = row;
			return this;
		}
	}

	private static BlobMarkerValue blobMarkerValue = new BlobMarkerValue();

	public static class BlobMarkerValue implements Serializable
	{
		private transient SoftReference<byte[]> cachedData;

		public BlobMarkerValue() //default constructor for serialize
		{
			//nop
		}

		public BlobMarkerValue(byte[] data_to_cache)
		{
			cachedData = new SoftReference<byte[]>(data_to_cache);
		}

		public byte[] getCachedData()
		{
			if (cachedData != null)
			{
				return cachedData.get();
			}
			return null;
		}


		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (!super.equals(obj))
			{
				if (obj instanceof BlobMarkerValue)
				{
					byte[] o = ((BlobMarkerValue)obj).getCachedData();
					if (o != null)
					{
						return o.equals(getCachedData());
					}
					else
					{
						return getCachedData() == null;
					}
				}
				return false;
			}
			return true;
		}

	}

	public static DbIdentValue createDbIdentValue()
	{
		return new DbIdentValue();
	}

	public static TableFlushValue createTableFlushValue()
	{
		return tableFlushValue;
	}

	public static NullValue createNullValue(int type)
	{
		NullValue nv = nullValuesByType.get(type);
		if (nv == null)
		{
			nv = new NullValue(type);
			nullValuesByType.put(type, nv);
		}
		return nv;
	}

	public static BlobMarkerValue createBlobMarkerValue()
	{
		return blobMarkerValue;
	}

	public static BlobMarkerValue createBlobMarkerValue(byte[] data_to_chache)
	{
		return new BlobMarkerValue(data_to_chache);
	}
}
