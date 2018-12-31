/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.base.query;


/**
 * Query table for mobile and regular clients.
 *
 * @author rgansevles
 *
 */
public class BaseQueryTable implements IBaseQueryElement
{
	private static long aliasCounter = 0l;

	protected String name;
	protected String dataSource;
	protected String alias;
	protected boolean needsQuoting;
	protected transient String catalogName;
	protected transient String schemaName;
	protected transient boolean generatedAlias;
	protected transient boolean isComplete;

	/**
	 * @param name table name as used in sql, may be quoted
	 */
	public BaseQueryTable(String name, String dataSource, String catalogName, String schemaName)
	{
		this(name, dataSource, catalogName, schemaName, true);
	}

	/**
	 * @param name table name as used in sql, may be quoted
	 */
	public BaseQueryTable(String name, String dataSource, String catalogName, String schemaName, boolean needsQuoting)
	{
		this(name, dataSource, catalogName, schemaName, null, needsQuoting);
	}

	public BaseQueryTable(String name, String dataSource, String catalogName, String schemaName, String alias)
	{
		this(name, dataSource, catalogName, schemaName, alias, true);
	}

	public BaseQueryTable(String name, String dataSource, String catalogName, String schemaName, String alias, boolean needsQuoting)
	{
		this.name = name;
		this.dataSource = dataSource;
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.generatedAlias = alias == null;
		this.alias = this.generatedAlias ? generateAlias(name) : alias;
		this.needsQuoting = needsQuoting;
		this.isComplete = true;
	}

	protected String generateAlias(String name)
	{
		// Skip anything but letters and digits
		StringBuilder aliasBuf = new StringBuilder();
		if (name != null)
		{
			char[] chars = name.toCharArray();
			for (char element : chars)
			{
				if (Character.isLetterOrDigit(element))
				{
					aliasBuf.append(element);
				}
			}
		}

		// generate next counter
		long n = getNextAliasCounter() & 0x7fffffffffffffffL;

		if (aliasBuf.length() == 0) // weird table name
		{
			return "T_" + n; //$NON-NLS-1$
		}

		aliasBuf.append(n);
		return aliasBuf.toString();
	}

	protected long getNextAliasCounter()
	{
		return ++aliasCounter;
	}

	/**
	 * QueryTable with all fields, only for internal use.
	 * @param name
	 * @param dataSource
	 * @param alias
	 * @param needsQuoting
	 * @param catalogName
	 * @param schemaName
	 * @param generatedAlias
	 * @param isComplete
	 */
	protected BaseQueryTable(String name, String dataSource, String alias, boolean needsQuoting, String catalogName, String schemaName, boolean generatedAlias,
		boolean isComplete)
	{
		this.name = name;
		this.dataSource = dataSource;
		this.alias = alias;
		this.needsQuoting = needsQuoting;
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.generatedAlias = generatedAlias;
		this.isComplete = isComplete;
	}

	/*
	 * Dummy call from legacy QueryTable1 deserialisation
	 */
	protected BaseQueryTable()
	{
		name = null;
		dataSource = null;
		needsQuoting = false;
		alias = null;
		generatedAlias = false;
		isComplete = false;
	}


	public String getName()
	{
		return name;
	}

	public String getDataSource()
	{
		return dataSource;
	}

	public String getCatalogName()
	{
		return this.catalogName;
	}

	public String getSchemaName()
	{
		return this.schemaName;
	}

	public String getAlias()
	{
		return alias;
	}

	/**
	 * Get the alias, when the alias was generated, keep this alias so after (de)serialization (when sent to the appserver) this alias is still the same.
	 */
	public String getAliasFrozen()
	{
		generatedAlias = false;
		return getAlias();
	}

	public boolean isAliasGenerated()
	{
		return generatedAlias;
	}

	public boolean needsQuoting()
	{
		return needsQuoting;
	}

	public boolean isComplete()
	{
		return isComplete;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((catalogName == null) ? 0 : catalogName.hashCode());
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((schemaName == null) ? 0 : schemaName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BaseQueryTable other = (BaseQueryTable)obj;
		if (alias == null)
		{
			if (other.alias != null) return false;
		}
		else if (!alias.equals(other.alias)) return false;
		if (catalogName == null)
		{
			if (other.catalogName != null) return false;
		}
		else if (!catalogName.equals(other.catalogName)) return false;
		if (dataSource == null)
		{
			if (other.dataSource != null) return false;
		}
		else if (!dataSource.equals(other.dataSource)) return false;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (schemaName == null)
		{
			if (other.schemaName != null) return false;
		}
		else if (!schemaName.equals(other.schemaName)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (catalogName != null)
		{
			sb.append(catalogName).append(':');
		}
		if (schemaName != null)
		{
			sb.append(schemaName).append(':');
		}
		sb.append(name);
		if (dataSource != null) sb.append('<').append(dataSource).append('>');
		if (alias != null)
		{
			sb.append('#').append(alias);
		}
		return sb.toString();
	}
}
