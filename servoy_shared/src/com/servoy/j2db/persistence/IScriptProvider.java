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
 * Interface to handle script object in a similar way
 * @author jblok
 */
@SuppressWarnings("nls")
public interface IScriptProvider extends IScriptElement
{
	public static final SerializableRuntimeProperty<String> FILENAME = new SerializableRuntimeProperty<String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<FILENAME>";
		}
	};

	public static final SerializableRuntimeProperty<String> TYPE = new SerializableRuntimeProperty<String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<TYPE>";
		}
	};

	public static final SerializableRuntimeProperty<Boolean> IS_DESTRUCTURING = new SerializableRuntimeProperty<Boolean>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<IS_DESTRUCTURING>";
		}
	};

	public static final SerializableRuntimeProperty<String> DESTRUCTURING = new SerializableRuntimeProperty<String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<DESTRUCTURING>";
		}
	};

	public static final SerializableRuntimeProperty<String[]> DESTRUCTURING_VARS = new SerializableRuntimeProperty<String[]>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<DESTRUCTURING_VARS>";
		}
	};

	public static final SerializableRuntimeProperty<String[]> DESTRUCTURING_INITIALIZERS = new SerializableRuntimeProperty<String[]>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<DESTRUCTURING_INITIALIZERS>";
		}
	};

	public static final SerializableRuntimeProperty<String> DESTRUCTURING_VALUE = new SerializableRuntimeProperty<String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String toString()
		{
			return "SerializableRuntimeProperty<DESTRUCTURING_VALUE>";
		}
	};

	public static final RuntimeProperty<MethodArgument[]> METHOD_ARGUMENTS = new RuntimeProperty<MethodArgument[]>()
	{
		@Override
		public String toString()
		{
			return "RuntimeProperty<METHOD_ARGUMENTS>";
		}
	};

	public static final RuntimeProperty<MethodArgument> METHOD_RETURN_TYPE = new RuntimeProperty<MethodArgument>()
	{
		@Override
		public String toString()
		{
			return "RuntimeProperty<METHOD_RETURN_TYPE>";
		}
	};

	public static final RuntimeProperty<String> COMMENT = new RuntimeProperty<String>()
	{
		@Override
		public String toString()
		{
			return "RuntimeProperty<COMMENT>";
		}
	};

	public String getDisplayName();

	public String getDataProviderID();

	public void setSource(String arg);

	public String getSource();

	public String getDeclaration();
}
