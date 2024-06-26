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

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.serverconfigtemplates.EmptyTemplate;
import com.servoy.j2db.serverconfigtemplates.FilemakerTemplate;
import com.servoy.j2db.serverconfigtemplates.FireBirdTemplate;
import com.servoy.j2db.serverconfigtemplates.FoxProTemplate;
import com.servoy.j2db.serverconfigtemplates.InMemoryTemplate;
import com.servoy.j2db.serverconfigtemplates.InformixTemplate;
import com.servoy.j2db.serverconfigtemplates.MSSQLFreeTDSTemplate;
import com.servoy.j2db.serverconfigtemplates.MSSQLTemplate;
import com.servoy.j2db.serverconfigtemplates.MariaDBTemplate;
import com.servoy.j2db.serverconfigtemplates.MySQLTemplate;
import com.servoy.j2db.serverconfigtemplates.ODBCTemplate;
import com.servoy.j2db.serverconfigtemplates.OpenbaseTemplate;
import com.servoy.j2db.serverconfigtemplates.OracleTemplate;
import com.servoy.j2db.serverconfigtemplates.PostgresTemplate;
import com.servoy.j2db.serverconfigtemplates.ProgressABLTemplate;
import com.servoy.j2db.serverconfigtemplates.ServerTemplateDefinition;
import com.servoy.j2db.serverconfigtemplates.SybaseASATemplate;
import com.servoy.j2db.util.Utils;


/**
 * Server configuration to define the runtime environment.
 *
 * This data differs from {@link ServerSettings} which contains the configuration of the behaviour of the solution.
 *
 * @author rgansevles
 *
 */
public class ServerConfig implements Serializable, Comparable<ServerConfig>
{
	public static final String EMPTY_TEMPLATE_NAME = "Empty"; //$NON-NLS-1$
	public static final String POSTGRESQL_TEMPLATE_NAME = "Postgresql"; //$NON-NLS-1$

	public static final int MAX_PREPSTATEMENT_IDLE_DEFAULT = 100;

	public static final int MAX_IDLE_DEFAULT = 10;

	public static final int MAX_ACTIVE_DEFAULT = 30;

	public static final boolean PREFIX_TABLES_DEFAULT = false;

	public static final boolean QUERY_PROCEDURES_DEFAULT = false;

	public static final Map<String, ServerTemplateDefinition> TEMPLATES = createTemplates();

	// supported connection validation types
	public static final int CONNECTION_EXCEPTION_VALIDATION = 0;
	public static final int CONNECTION_METADATA_VALIDATION = 1;
	public static final int CONNECTION_QUERY_VALIDATION = 2;
	public static final int CONNECTION_DRIVER_VALIDATION = 3;
	public static final int VALIDATION_TYPE_DEFAULT = CONNECTION_EXCEPTION_VALIDATION;

	public static final String NONE = "<none>"; //$NON-NLS-1$
	public static final String EMPTY = "<empty>"; //$NON-NLS-1$

	private final String serverName;
	private final String userName;
	private final String password;
	private final String serverUrl;
	private final Map<String, String> connectionProperties;
	private final String driver;
	private final String catalog;
	private final String schema;
	private final int maxActive;
	private final int maxIdle;
	private final int maxPreparedStatementsIdle;
	private final int connectionValidationType;
	private final String validationQuery;
	private final String dataModelCloneFrom;
	private final boolean enabled;
	private final boolean skipSysTables;
	private final boolean queryProcedures;
	private final boolean prefixTables;
	private final int idleTimeout;
	private final Integer selectINValueCountLimit;
	private final String dialectClass;
	private final List<String> quoteList;
	private final boolean clientOnlyConnections;
	private final String initializationString;

	public ServerConfig(String serverName, String userName, String password, String serverUrl, Map<String, String> connectionProperties, String driver,
		String catalog, String schema, int maxActive, int maxIdle, int maxPreparedStatementsIdle, int connectionValidationType, String validationQuery,
		String dataModelCloneFrom, boolean enabled, boolean skipSysTables, boolean prefixTables, boolean queryProcedures, int idleTimeout,
		Integer selectINValueCountLimit, String dialectClass, List<String> quoteList, boolean clientOnlyConnections, String initializationString)
	{
		this.clientOnlyConnections = clientOnlyConnections;
		this.serverName = Utils.toEnglishLocaleLowerCase(serverName); // safety for when stored in columnInfo
		this.userName = userName;
		this.password = password;
		this.serverUrl = serverUrl;
		this.connectionProperties = connectionProperties == null ? null : Collections.unmodifiableMap(connectionProperties);
		this.driver = driver;
		this.maxActive = maxActive;
		this.maxIdle = maxIdle;
		this.maxPreparedStatementsIdle = maxPreparedStatementsIdle;
		this.connectionValidationType = connectionValidationType;
		this.validationQuery = validationQuery;
		this.dataModelCloneFrom = Utils.toEnglishLocaleLowerCase(dataModelCloneFrom);
		this.enabled = enabled;
		this.skipSysTables = skipSysTables;
		this.queryProcedures = queryProcedures;
		this.prefixTables = prefixTables;
		this.idleTimeout = idleTimeout;
		this.selectINValueCountLimit = selectINValueCountLimit;
		this.dialectClass = dialectClass;
		this.quoteList = quoteList;
		this.initializationString = initializationString;

		if (driver == null || serverUrl == null)
		{
			throw new IllegalArgumentException("server URL or driver name not specified"); //$NON-NLS-1$
		}

		if (NONE.equals(catalog)) this.catalog = null;
		else if (EMPTY.equals(catalog)) this.catalog = ""; //$NON-NLS-1$
		else this.catalog = catalog;

		if (NONE.equals(schema)) this.schema = null;
		else if (EMPTY.equals(schema)) this.schema = ""; //$NON-NLS-1$
		else this.schema = schema;
	}

	public ServerConfig getNamedCopy(String newServerName)
	{
		if (serverName.equals(newServerName)) return this;
		return new Builder(this).setServerName(newServerName).build();
	}

	public ServerConfig getEnabledCopy(boolean newEnabled)
	{
		if (enabled == newEnabled) return this;
		return new Builder(this).setEnabled(newEnabled).build();
	}

	public String getServerName()
	{
		return serverName;
	}

	public String getUserName()
	{
		return userName;
	}

	public String getPassword()
	{
		return password;
	}

	public String getServerUrl()
	{
		return serverUrl;
	}

	public Map<String, String> getConnectionProperties()
	{
		return connectionProperties;
	}

	public String getDriver()
	{
		return driver;
	}

	public String getCatalog()
	{
		return catalog;
	}

	public String getSchema()
	{
		return schema;
	}

	/**
	 * @deprecated use {@link ServerSettings#getClientOnlyConnections()} with fallback on this call
	 */
	@Deprecated
	public boolean isClientOnlyConnections()
	{
		return clientOnlyConnections;
	}

	public int getMaxActive()
	{
		return maxActive;
	}

	public int getMaxIdle()
	{
		return maxIdle;
	}

	public int getIdleTimeout()
	{
		return idleTimeout;
	}

	public int getMaxPreparedStatementsIdle()
	{
		return maxPreparedStatementsIdle;
	}

	public int getConnectionValidationType()
	{
		return connectionValidationType;
	}

	public String getValidationQuery()
	{
		return validationQuery;
	}

	public String getDataModelCloneFrom()
	{
		return dataModelCloneFrom;
	}

	public boolean isEnabled()
	{
		return enabled;
	}


	public boolean getSkipSysTables()
	{
		return skipSysTables;
	}

	/**
	 * @deprecated use {@link ServerSettings#getQueryProcedures()} with fallback on this call
	 */
	@Deprecated
	public boolean getQueryProcedures()
	{
		return queryProcedures;
	}

	public boolean getPrefixTables()
	{
		return prefixTables;
	}

	public Integer getSelectINValueCountLimit()
	{
		return selectINValueCountLimit;
	}

	public String getDialectClass()
	{
		return dialectClass;
	}

	public List<String> getQuoteList()
	{
		return quoteList;
	}

	public String getInitializationString()
	{
		return initializationString;
	}

	public Builder newBuilder()
	{
		return new Builder(this);
	}

	// used for sorting in ServerManager (TreeMap)
	public int compareTo(ServerConfig sc)
	{
		if (sc == null) return -1;
		return serverName.compareTo(sc.getServerName());
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
		result = prime * result + ((connectionProperties == null) ? 0 : connectionProperties.hashCode());
		result = prime * result + connectionValidationType;
		result = prime * result + ((dataModelCloneFrom == null) ? 0 : dataModelCloneFrom.hashCode());
		result = prime * result + ((dialectClass == null) ? 0 : dialectClass.hashCode());
		result = prime * result + ((driver == null) ? 0 : driver.hashCode());
		result = prime * result + (enabled ? 1231 : 1237);
		result = prime * result + idleTimeout;
		result = prime * result + maxActive;
		result = prime * result + maxIdle;
		result = prime * result + maxPreparedStatementsIdle;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((serverUrl == null) ? 0 : serverUrl.hashCode());
		result = prime * result + (skipSysTables ? 1231 : 1237);
		result = prime * result + (prefixTables ? 1231 : 1237);
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		result = prime * result + ((validationQuery == null) ? 0 : validationQuery.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ServerConfig other = (ServerConfig)obj;
		if (catalog == null)
		{
			if (other.catalog != null) return false;
		}
		else if (!catalog.equals(other.catalog)) return false;
		if (connectionProperties == null)
		{
			if (other.connectionProperties != null) return false;
		}
		else if (!connectionProperties.equals(other.connectionProperties)) return false;
		if (connectionValidationType != other.connectionValidationType) return false;
		if (dataModelCloneFrom == null)
		{
			if (other.dataModelCloneFrom != null) return false;
		}
		else if (!dataModelCloneFrom.equals(other.dataModelCloneFrom)) return false;
		if (dialectClass == null)
		{
			if (other.dialectClass != null) return false;
		}
		else if (!dialectClass.equals(other.dialectClass)) return false;
		if (driver == null)
		{
			if (other.driver != null) return false;
		}
		else if (!driver.equals(other.driver)) return false;
		if (enabled != other.enabled) return false;
		if (idleTimeout != other.idleTimeout) return false;
		if (maxActive != other.maxActive) return false;
		if (maxIdle != other.maxIdle) return false;
		if (maxPreparedStatementsIdle != other.maxPreparedStatementsIdle) return false;
		if (password == null)
		{
			if (other.password != null) return false;
		}
		else if (!password.equals(other.password)) return false;
		if (schema == null)
		{
			if (other.schema != null) return false;
		}
		else if (!schema.equals(other.schema)) return false;
		if (serverName == null)
		{
			if (other.serverName != null) return false;
		}
		else if (!serverName.equals(other.serverName)) return false;
		if (serverUrl == null)
		{
			if (other.serverUrl != null) return false;
		}
		else if (!serverUrl.equals(other.serverUrl)) return false;
		if (skipSysTables != other.skipSysTables) return false;
		if (prefixTables != other.prefixTables) return false;
		if (userName == null)
		{
			if (other.userName != null) return false;
		}
		else if (!userName.equals(other.userName)) return false;
		if (validationQuery == null)
		{
			if (other.validationQuery != null) return false;
		}
		else if (!validationQuery.equals(other.validationQuery)) return false;
		return true;
	}

	public boolean isRepositoryServer()
	{
		return IServer.REPOSITORY_SERVER.equals(serverName);
	}

	//we need todo special things for the very buggy odbc bridge... :-(
	public boolean isODBCDriver()
	{
		return serverUrl.startsWith("jdbc:odbc:"); //$NON-NLS-1$
	}

	public boolean isOracleDriver()
	{
		return driver.toLowerCase().indexOf("oracle") != -1; //$NON-NLS-1$
	}

	public boolean isSybaseDriver()
	{
		return serverUrl.toLowerCase().startsWith("jdbc:sybase:"); //$NON-NLS-1$
	}

	public boolean isPostgresDriver()
	{
		return serverUrl.toLowerCase().startsWith("jdbc:postgresql:"); //$NON-NLS-1$
	}

	public boolean isHxttDBFDriver()
	{
		return driver.toLowerCase().indexOf("hxtt") != -1; //$NON-NLS-1$
	}

	public boolean isInMemDriver()
	{
		return driver.toLowerCase().indexOf("hsqldb") != -1; //$NON-NLS-1$
	}

	public static String getConnectionValidationTypeAsString(int connectionValidationType)
	{
		switch (connectionValidationType)
		{
			case CONNECTION_EXCEPTION_VALIDATION :
				return "exception validation"; //$NON-NLS-1$
			case CONNECTION_METADATA_VALIDATION :
				return "meta data validation"; //$NON-NLS-1$
			case CONNECTION_QUERY_VALIDATION :
				return "query validation"; //$NON-NLS-1$
			case CONNECTION_DRIVER_VALIDATION :
				return "driver based validation"; //$NON-NLS-1$
			default :
				return null;
		}
	}

	protected static Map<String, ServerTemplateDefinition> createTemplates()
	{
		Map<String, ServerTemplateDefinition> map = new LinkedHashMap<String, ServerTemplateDefinition>();

		map.put(EMPTY_TEMPLATE_NAME, new EmptyTemplate());

		// Prepared statement pool should be disabled, see http://www.hxtt.com/support_view_issue.jsp?product=dbf&id=1340742013
		map.put("FoxPro DBF", new FoxProTemplate());
		map.put("Filemaker", new FilemakerTemplate());
		map.put("FireBird", new FireBirdTemplate());
		map.put("In Memory", new InMemoryTemplate());
		map.put("Informix", new InformixTemplate());
		map.put("MS SQL", new MSSQLTemplate());
		map.put("MS SQL (freetds)", new MSSQLFreeTDSTemplate());
		map.put("MySQL", new MySQLTemplate());
		map.put("MariaDB", new MariaDBTemplate());
		map.put("ODBC Datasource", new ODBCTemplate());
		map.put("Openbase", new OpenbaseTemplate());
		map.put("Oracle", new OracleTemplate());
		map.put(POSTGRESQL_TEMPLATE_NAME, new PostgresTemplate());
		map.put("Progress ABL", new ProgressABLTemplate());
		map.put("Sybase ASA", new SybaseASATemplate());

		return map;
	}

	public static void clearTemplates()
	{
		TEMPLATES.clear();
	}

	@Override
	public String toString()
	{
		return new StringBuilder("ServerConfig [") //
			.append("serverName=").append(serverName) //
			.append(", userName=").append(userName) //
			.append(", serverUrl=").append(serverUrl) //
			.append(", connectionProperties=").append(connectionProperties) //
			.append(", driver=").append(driver) //
			.append(", catalog=").append(catalog) //
			.append(", schema=").append(schema) //
			.append(", maxActive=").append(maxActive) //
			.append(", maxIdle=").append(maxIdle) //
			.append(", maxPreparedStatementsIdle=").append(maxPreparedStatementsIdle) //
			.append(", connectionValidationType=").append(connectionValidationType) //
			.append(", validationQuery=").append(validationQuery) //
			.append(", dataModelCloneFrom=").append(dataModelCloneFrom) //
			.append(", enabled=").append(enabled) //
			.append(", skipSysTables=").append(skipSysTables) //
			.append(", idleTimeout=").append(idleTimeout) //
			.append(", selectINValueCountLimit=").append(selectINValueCountLimit) //
			.append(", dialectClass=").append(dialectClass) //
			.append("]").toString();
	}

	public static final class Builder
	{
		private String serverName;
		private String userName;
		private String password;
		private String serverUrl;
		private Map<String, String> connectionProperties;
		private String driver;
		private String catalog;
		private String schema;
		private int maxActive = MAX_ACTIVE_DEFAULT;
		private int maxIdle = MAX_IDLE_DEFAULT;
		private int maxPreparedStatementsIdle = MAX_PREPSTATEMENT_IDLE_DEFAULT;
		private int connectionValidationType = VALIDATION_TYPE_DEFAULT;
		private String validationQuery;
		private String dataModelCloneFrom;
		private boolean enabled = true;
		private boolean skipSysTables;
		private boolean queryProcedures = QUERY_PROCEDURES_DEFAULT;
		private boolean prefixTables = PREFIX_TABLES_DEFAULT;
		private int idleTimeout = -1;
		private Integer selectINValueCountLimit;
		private String dialectClass;
		private List<String> quoteList = emptyList();
		private boolean clientOnlyConnections;
		private String initializationString;

		public Builder()
		{
		}

		public Builder(ServerConfig serverConfig)
		{
			this.serverName = serverConfig.serverName;
			this.userName = serverConfig.userName;
			this.password = serverConfig.password;
			this.serverUrl = serverConfig.serverUrl;
			this.connectionProperties = serverConfig.connectionProperties;
			this.driver = serverConfig.driver;
			this.catalog = serverConfig.catalog;
			this.schema = serverConfig.schema;
			this.maxActive = serverConfig.maxActive;
			this.maxIdle = serverConfig.maxIdle;
			this.maxPreparedStatementsIdle = serverConfig.maxPreparedStatementsIdle;
			this.connectionValidationType = serverConfig.connectionValidationType;
			this.validationQuery = serverConfig.validationQuery;
			this.dataModelCloneFrom = serverConfig.dataModelCloneFrom;
			this.enabled = serverConfig.enabled;
			this.skipSysTables = serverConfig.skipSysTables;
			this.queryProcedures = serverConfig.queryProcedures;
			this.prefixTables = serverConfig.prefixTables;
			this.idleTimeout = serverConfig.idleTimeout;
			this.selectINValueCountLimit = serverConfig.selectINValueCountLimit;
			this.dialectClass = serverConfig.dialectClass;
			this.quoteList = serverConfig.quoteList;
			this.clientOnlyConnections = serverConfig.clientOnlyConnections;
			this.initializationString = serverConfig.initializationString;
		}

		public Builder setServerName(String serverName)
		{
			this.serverName = serverName;
			return this;
		}

		public Builder setUserName(String userName)
		{
			this.userName = userName;
			return this;
		}

		public Builder setPassword(String password)
		{
			this.password = password;
			return this;
		}

		public Builder setServerUrl(String serverUrl)
		{
			this.serverUrl = serverUrl;
			return this;
		}

		public Builder setConnectionProperties(Map<String, String> connectionProperties)
		{
			this.connectionProperties = connectionProperties;
			return this;
		}

		public Builder setDriver(String driver)
		{
			this.driver = driver;
			return this;
		}

		public Builder setCatalog(String catalog)
		{
			this.catalog = catalog;
			return this;
		}

		public Builder setSchema(String schema)
		{
			this.schema = schema;
			return this;
		}

		public Builder setMaxActive(int maxActive)
		{
			this.maxActive = maxActive;
			return this;
		}

		public Builder setMaxIdle(int maxIdle)
		{
			this.maxIdle = maxIdle;
			return this;
		}

		public Builder setMaxPreparedStatementsIdle(int maxPreparedStatementsIdle)
		{
			this.maxPreparedStatementsIdle = maxPreparedStatementsIdle;
			return this;
		}

		public Builder setConnectionValidationType(int connectionValidationType)
		{
			this.connectionValidationType = connectionValidationType;
			return this;
		}

		public Builder setValidationQuery(String validationQuery)
		{
			this.validationQuery = validationQuery;
			return this;
		}

		public Builder setDataModelCloneFrom(String dataModelCloneFrom)
		{
			this.dataModelCloneFrom = dataModelCloneFrom;
			return this;
		}

		public Builder setEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public Builder setSkipSysTables(boolean skipSysTables)
		{
			this.skipSysTables = skipSysTables;
			return this;
		}

		public Builder setQueryProcedures(boolean queryProcedures)
		{
			this.queryProcedures = queryProcedures;
			return this;
		}

		public Builder setPrefixTables(boolean prefixTables)
		{
			this.prefixTables = prefixTables;
			return this;
		}

		public Builder setIdleTimeout(int idleTimeout)
		{
			this.idleTimeout = idleTimeout;
			return this;
		}

		public Builder setSelectINValueCountLimit(Integer selectINValueCountLimit)
		{
			this.selectINValueCountLimit = selectINValueCountLimit;
			return this;
		}

		public Builder setDialectClass(String dialectClass)
		{
			this.dialectClass = dialectClass;
			return this;
		}

		public Builder setQuoteList(List<String> quoteList)
		{
			this.quoteList = quoteList;
			return this;
		}

		public Builder setClientOnlyConnections(boolean clientOnlyConnections)
		{
			this.clientOnlyConnections = clientOnlyConnections;
			return this;
		}

		public Builder setInitializationString(String initializationString)
		{
			this.initializationString = initializationString;
			return this;
		}

		public ServerConfig build()
		{
			return new ServerConfig(
				serverName,
				userName,
				password,
				serverUrl,
				connectionProperties,
				driver,
				catalog,
				schema,
				maxActive,
				maxIdle,
				maxPreparedStatementsIdle,
				connectionValidationType,
				validationQuery,
				dataModelCloneFrom,
				enabled,
				skipSysTables,
				queryProcedures,
				prefixTables,
				idleTimeout,
				selectINValueCountLimit,
				dialectClass,
				quoteList,
				clientOnlyConnections,
				initializationString);
		}
	}
}
