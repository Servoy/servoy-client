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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.servoy.j2db.serverconfigtemplates.EmptyTemplate;
import com.servoy.j2db.serverconfigtemplates.FilemakerTemplate;
import com.servoy.j2db.serverconfigtemplates.FireBirdTemplate;
import com.servoy.j2db.serverconfigtemplates.FoxProTemplate;
import com.servoy.j2db.serverconfigtemplates.InMemoryTemplate;
import com.servoy.j2db.serverconfigtemplates.InformixTemplate;
import com.servoy.j2db.serverconfigtemplates.MSSQLFreeTDSTemplate;
import com.servoy.j2db.serverconfigtemplates.MSSQLTemplate;
import com.servoy.j2db.serverconfigtemplates.MySQLTemplate;
import com.servoy.j2db.serverconfigtemplates.ODBCTemplate;
import com.servoy.j2db.serverconfigtemplates.OpenbaseTemplate;
import com.servoy.j2db.serverconfigtemplates.OracleTemplate;
import com.servoy.j2db.serverconfigtemplates.PostgresTemplate;
import com.servoy.j2db.serverconfigtemplates.ServerTemplateDefinition;
import com.servoy.j2db.serverconfigtemplates.SybaseASATemplate;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.SecuritySupport;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;


/**
 * Server configuration data.
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

	public static final Map<String, ServerTemplateDefinition> TEMPLATES = createTemplates();

	// supported connection validation types
	public static final int CONNECTION_EXCEPTION_VALIDATION = 0;
	public static final int CONNECTION_METADATA_VALIDATION = 1;
	public static final int CONNECTION_QUERY_VALIDATION = 2;
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
	private int maxActive;
	private int maxIdle;
	private final int maxPreparedStatementsIdle;
	private final int connectionValidationType;
	private final String validationQuery;
	private final String dataModelCloneFrom;
	private final boolean enabled;
	private final boolean skipSysTables;
	private int idleTimeout;
	private final String dialectClass;

	public ServerConfig(String serverName, String userName, String password, String serverUrl, Map<String, String> connectionProperties, String driver,
		String catalog, String schema, int maxActive, int maxIdle, int maxPreparedStatementsIdle, int connectionValidationType, String validationQuery,
		String dataModelCloneFrom, boolean enabled, boolean skipSysTables, int idleTimeout, String dialectClass)
	{
		this.serverName = Utils.toEnglishLocaleLowerCase(serverName);//safety for when stored in columnInfo
		this.userName = userName;
		String passwd = password;
		try
		{
			if (passwd.startsWith("sec:")) passwd = SecuritySupport.decrypt(Settings.getInstance(), passwd.substring(4));
		}
		catch (Exception e)
		{
			Debug.error("Could not decrypt password for server " + serverName, e);
		}
		this.password = passwd;
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
		this.idleTimeout = idleTimeout;
		this.dialectClass = dialectClass;

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

	public ServerConfig(String serverName, String userName, String password, String serverUrl, Map<String, String> connectionProperties, String driver,
		String catalog, String schema, boolean enabled, boolean skipSysTables, String dialectClass)
	{
		this(serverName, userName, password, serverUrl, connectionProperties, driver, catalog, schema, MAX_ACTIVE_DEFAULT, MAX_IDLE_DEFAULT,
			MAX_PREPSTATEMENT_IDLE_DEFAULT, VALIDATION_TYPE_DEFAULT, null, null, enabled, skipSysTables, -1, dialectClass);
	}

	public ServerConfig getNamedCopy(String newServerName)
	{
		if (serverName.equals(newServerName)) return this;
		return new ServerConfig(newServerName, userName, password, serverUrl, connectionProperties, driver, catalog, schema, maxActive, maxIdle,
			maxPreparedStatementsIdle, connectionValidationType, validationQuery, dataModelCloneFrom, enabled, skipSysTables, idleTimeout, dialectClass);
	}

	public ServerConfig getEnabledCopy(boolean newEnabled)
	{
		if (enabled == newEnabled) return this;
		return new ServerConfig(serverName, userName, password, serverUrl, connectionProperties, driver, catalog, schema, maxActive, maxIdle,
			maxPreparedStatementsIdle, connectionValidationType, validationQuery, dataModelCloneFrom, newEnabled, skipSysTables, idleTimeout, dialectClass);
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

	public String getDialectClass()
	{
		return dialectClass;
	}

	/**
	 * Should only be called from Server so that exiting connection data source gets updated as well
	 */
	public void setMaxActive(int maxActive)
	{
		this.maxActive = maxActive;
	}

	/**
	 * Should only be called from Server so that exiting connection data source gets updated as well
	 */
	public void setMaxIdle(int maxIdle)
	{
		this.maxIdle = maxIdle;
	}

	/**
	 * Should only be called from Server so that exiting connection data source gets updated as well
	 */
	public void setIdleTimeout(int idleTimeout)
	{
		this.idleTimeout = idleTimeout;
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
		map.put("ODBC Datasource", new ODBCTemplate());
		map.put("Openbase", new OpenbaseTemplate());
		map.put("Oracle", new OracleTemplate());
		map.put(POSTGRESQL_TEMPLATE_NAME, new PostgresTemplate());
		map.put("Sybase ASA", new SybaseASATemplate());

		return map;
	}

	/**
	 *
	 */
	public static void clearTemplates()
	{
		TEMPLATES.clear();
	}
}
