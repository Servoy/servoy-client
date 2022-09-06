/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

import static com.servoy.j2db.persistence.SortingNullprecedence.databaseDefault;

import java.io.Serializable;


/**
 * Server settings that define behaviour of the solution.
 *
 * This data differs from {@link ServerConfig} which contains the configuration of the runtime environment.
 *
 * @author rgansevles
 *
 */
public class ServerSettings implements Serializable
{
	public static final ServerSettings DEFAULT = new ServerSettings(false, databaseDefault, null, null);

	private final boolean sortIgnorecase;
	private final SortingNullprecedence sortingNullprecedence;
	private final Boolean queryProcedures; // when null the value has not been explicitly set, use value from ServerConfig
	private final Boolean clientOnlyConnections; // when null the value has not been explicitly set, use value from ServerConfig

	public ServerSettings(boolean sortIgnorecase, SortingNullprecedence sortingNullprecedence, Boolean queryProcedures, Boolean clientOnlyConnections)
	{
		this.sortIgnorecase = sortIgnorecase;
		this.sortingNullprecedence = sortingNullprecedence;
		this.queryProcedures = queryProcedures;
		this.clientOnlyConnections = clientOnlyConnections;
	}

	/**
	 * @return the sortIgnorecase
	 */
	public boolean isSortIgnorecase()
	{
		return sortIgnorecase;
	}

	/**
	 * @return the sortingNullprecedence
	 */
	public SortingNullprecedence getSortingNullprecedence()
	{
		return sortingNullprecedence;
	}

	/**
	 * @return the queryProcedures
	 */
	public Boolean getQueryProcedures()
	{
		return queryProcedures;
	}

	/**
	 * @return the clientOnlyConnections
	 */
	public Boolean getClientOnlyConnections()
	{
		return clientOnlyConnections;
	}

	public ServerSettings withQueryProcedures(boolean queryProceduresSet)
	{
		if (Boolean.valueOf(queryProceduresSet).equals(this.queryProcedures))
		{
			return this;
		}
		return new ServerSettings(this.sortIgnorecase, this.sortingNullprecedence, Boolean.valueOf(queryProceduresSet), this.clientOnlyConnections);
	}

	public ServerSettings withClientOnlyConnections(boolean clientOnlyConnectionsSet)
	{
		if (Boolean.valueOf(clientOnlyConnectionsSet).equals(this.clientOnlyConnections))
		{
			return this;
		}
		return new ServerSettings(this.sortIgnorecase, this.sortingNullprecedence, this.queryProcedures, Boolean.valueOf(clientOnlyConnectionsSet));
	}

	/**
	 * copy legacy value from server config.
	 */
	public ServerSettings withDefaults(ServerConfig serverConfig)
	{
		ServerSettings updated = this;
		if (this.queryProcedures == null)
		{
			updated = updated.withQueryProcedures(serverConfig.getQueryProcedures());
		}
		if (this.clientOnlyConnections == null)
		{
			updated = updated.withClientOnlyConnections(serverConfig.isClientOnlyConnections());
		}

		return updated;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clientOnlyConnections == null) ? 0 : clientOnlyConnections.hashCode());
		result = prime * result + ((queryProcedures == null) ? 0 : queryProcedures.hashCode());
		result = prime * result + (sortIgnorecase ? 1231 : 1237);
		result = prime * result + ((sortingNullprecedence == null) ? 0 : sortingNullprecedence.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ServerSettings other = (ServerSettings)obj;
		if (clientOnlyConnections == null)
		{
			if (other.clientOnlyConnections != null) return false;
		}
		else if (!clientOnlyConnections.equals(other.clientOnlyConnections)) return false;
		if (queryProcedures == null)
		{
			if (other.queryProcedures != null) return false;
		}
		else if (!queryProcedures.equals(other.queryProcedures)) return false;
		if (sortIgnorecase != other.sortIgnorecase) return false;
		if (sortingNullprecedence != other.sortingNullprecedence) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return new StringBuilder("ServerSettings [")
			.append("sortIgnorecase=").append(sortIgnorecase)
			.append(", sortingNullprecedence=").append(sortingNullprecedence)
			.append(", queryProcedures=").append(queryProcedures)
			.append(", clientOnlyConnections=").append(clientOnlyConnections)
			.append("]").toString();
	}
}
