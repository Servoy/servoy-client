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
package com.servoy.j2db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.j2db.dataprocessing.DataproviderTableFilterdefinition;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.dataprocessing.ISQLActionTypes;
import com.servoy.j2db.dataprocessing.ISQLStatement;
import com.servoy.j2db.dataprocessing.SQLStatement;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.dataprocessing.TableFilterdefinition;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.CompareCondition;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QueryColumnValue;
import com.servoy.j2db.query.QueryDelete;
import com.servoy.j2db.query.QuerySelect;
import com.servoy.j2db.query.QueryTable;
import com.servoy.j2db.query.SetCondition;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;


/**
 * @author jcompagner
 */
public class Messages
{
	public static final String JRE_DEFAULT_KEY_VALUE = "%%java_default%%";
	public static final String[] JRE_DEFAULT_KEYS = { "OptionPane.yesButtonText", "OptionPane.noButtonText", "OptionPane.cancelButtonText", "OptionPane.okButtonText", "OptionPane.yesButtonMnemonic", "OptionPane.noButtonMnemonic", "OptionPane.cancelButtonMnemonic", "OptionPane.okButtonMnemonic", "OptionPane.titleText", "OptionPane.inputDialogTitle", "OptionPane.messageDialogTitle", "FileChooser.fileDescriptionText", "FileChooser.directoryDescriptionText", "FileChooser.newFolderErrorText", "FileChooser.newFolderErrorSeparator", "FileChooser.acceptAllFileFilterText", "FileChooser.cancelButtonText", "FileChooser.cancelButtonMnemonic", "FileChooser.saveButtonText", "FileChooser.saveButtonMnemonic", "FileChooser.openButtonText", "FileChooser.openButtonMnemonic", "FileChooser.saveDialogTitleText", "FileChooser.openDialogTitleText", "FileChooser.updateButtonText", "FileChooser.updateButtonMnemonic", "FileChooser.helpButtonText", "FileChooser.helpButtonMnemonic", "FileChooser.directoryOpenButtonText", "FileChooser.directoryOpenButtonMnemonic", "FileChooser.fileSizeKiloBytes", "FileChooser.fileSizeMegaBytes", "FileChooser.fileSizeGigaBytes", "FileChooser.win32.newFolder", "FileChooser.win32.newFolder.subsequent", "FileChooser.other.newFolder", "FileChooser.other.newFolder.subsequent", "FileChooser.cancelButtonToolTipText", "FileChooser.saveButtonToolTipText", "FileChooser.openButtonToolTipText", "FileChooser.updateButtonToolTipText", "FileChooser.helpButtonToolTipText", "FileChooser.directoryOpenButtonToolTipText", "ColorChooser.previewText", "ColorChooser.okText", "ColorChooser.cancelText", "ColorChooser.resetText", "ColorChooser.resetMnemonic", "ColorChooser.sampleText", "ColorChooser.swatchesNameText", "ColorChooser.swatchesMnemonic", "ColorChooser.swatchesDisplayedMnemonicIndex", "ColorChooser.swatchesRecentText", "ColorChooser.hsbNameText", "ColorChooser.hsbMnemonic", "ColorChooser.hsbDisplayedMnemonicIndex", "ColorChooser.hsbHueText", "ColorChooser.hsbSaturationText", "ColorChooser.hsbBrightnessText", "ColorChooser.hsbRedText", "ColorChooser.hsbGreenText", "ColorChooser.hsbBlueText", "ColorChooser.rgbNameText", "ColorChooser.rgbMnemonic", "ColorChooser.rgbDisplayedMnemonicIndex", "ColorChooser.rgbRedText", "ColorChooser.rgbRedMnemonic", "ColorChooser.rgbGreenText", "ColorChooser.rgbGreenMnemonic", "ColorChooser.rgbBlueText", "ColorChooser.rgbBlueMnemonic", };

	public static final int ALL_LOCALES = 0;
	public static final int DEFAULT_LOCALE = 1;
	public static final int SPECIFIED_LANGUAGE = 2;
	public static final int SPECIFIED_LOCALE = 3;

	private static final String FILE_NAME = "servoy_messages"; //$NON-NLS-1$
	private static final String CLIENT_LOCAL_FILE_NAME = "servoy_messages"; //$NON-NLS-1$

	public static final String BUNDLE_NAME = "com.servoy.j2db.messages";//$NON-NLS-1$

	private static ResourceBundle localeJarMessages = ResourceBundle.getBundle(BUNDLE_NAME);
	private static Properties localeServerMessages = null;
	private static Properties localeSolutionMessages = null;

	public static volatile boolean invalidConnection;
	public static volatile boolean noConnection;
	public static volatile long changedTime = System.currentTimeMillis();

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT USE.
	 *
	 * @exclude
	 */
	public static ICustomMessageLoader customMessageLoader;

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public static void loadInternal(IMessagesCallback callback, IFoundSetManagerInternal fm)
	{
		localeJarMessages = ResourceBundle.getBundle(BUNDLE_NAME);
		localeServerMessages = loadMessagesFromServer(null, callback, fm);
		if (callback.getSolution() != null)
		{
			invalidConnection = false;
			localeSolutionMessages = loadMessagesFromServer(callback.getSolution(), callback, fm);
		}
		else
		{
			localeSolutionMessages = null;
		}

		callback.messagesLoaded();
	}

	public static boolean isI18NTable(String serverName, String tableName, IApplication application)
	{
		Solution solution = application.getSolution();
		IFoundSetManagerInternal fm = application.getFoundSetManager();

		if (solution != null && fm != null)
		{
			String i18nDatasource = solution.getI18nDataSource();
			String[] names = getServerTableNames(i18nDatasource, application.getSettings());

			return names[0] != null && names[0].equals(serverName) && names[1] != null && ((tableName == null) || names[1].equals(tableName));
		}
		return false;
	}

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	private static Properties loadMessagesFromServer(Solution solution, IMessagesCallback callback, IFoundSetManagerInternal fm)
	{
		Properties properties = new Properties();
		// only called by the smart client, where the default is set
		Locale language = Locale.getDefault();
		boolean isTableFilterInUse = false;
		if (solution != null && fm != null)
		{
			String i18nDatasource = solution.getI18nDataSource();
			String[] names = getServerTableNames(i18nDatasource, callback.getSettings());
			String serverName = names[0];
			String tableName = names[1];
			if (serverName != null && tableName != null)
			{
				isTableFilterInUse = fm.hasTableFilter(serverName, tableName);
			}
		}

		if (callback.isRunningRemote() && !isTableFilterInUse)
		{
			File parent = new File(System.getProperty("user.home"), J2DBGlobals.CLIENT_LOCAL_DIR); //$NON-NLS-1$
			parent.mkdirs();
			long date = -1;
			File file = null;
			URL base = callback.getServerURL();
			String filterFilePart = "";
			String filterUrlPart = "";
			String[] columnValueFilter;
			if (callback.getI18NColumnNameFilter() != null && (columnValueFilter = callback.getI18NColumnValueFilter()) != null)
			{
				StringBuffer sbColumnValueFilter = new StringBuffer();
				for (String s : columnValueFilter)
				{
					sbColumnValueFilter.append("&columnvalue=" + s);
				}
				String columnValueFilterParam = sbColumnValueFilter.toString();
				filterFilePart = "_" + callback.getI18NColumnNameFilter() + "_" + columnValueFilterParam;
				filterUrlPart = "&columnname=" + callback.getI18NColumnNameFilter() + columnValueFilterParam;
			}
			if (solution != null)
			{
				file = new File(parent, CLIENT_LOCAL_FILE_NAME + "_" + base.getHost() + "_" + base.getPort() + "_" + solution.getName() + "_" + language + //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
					filterFilePart + ".properties"); //$NON-NLS-1$
			}
			else
			{
				file = new File(parent, CLIENT_LOCAL_FILE_NAME + "_" + base.getHost() + "_" + base.getPort() + "_" + language + filterFilePart + ".properties"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			if (file.exists())
			{
				date = file.lastModified();
				Debug.trace("MessageFile " + file + " exists date: " + date); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				Debug.trace("MessageFile " + file + " does NOT exists"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			try
			{
				URL configfile = null;
				if (solution != null)
				{
					configfile = new URL(base,
						FILE_NAME + "?lastmodified=" + date + "&language=" + language + "&solution=" + solution.getName() + filterUrlPart); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				else
				{
					configfile = new URL(base, FILE_NAME + "?lastmodified=" + date + "&language=" + language + filterUrlPart); //$NON-NLS-1$ //$NON-NLS-2$
				}
				Debug.trace("Trying to load i18n from: " + configfile); //$NON-NLS-1$
				InputStream is = (InputStream)configfile.getContent();
				properties.load(is);
				is.close();
				Debug.trace("Properties loaded: " + properties); //$NON-NLS-1$
				if (properties.size() > 0)
				{
					FileOutputStream fos = new FileOutputStream(file);
					properties.store(fos, ""); //$NON-NLS-1$
					fos.close();
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			// if still 0 size then load from disk...
			if (properties.size() == 0 && file.exists())
			{
				try
				{
					FileInputStream fis = new FileInputStream(file);
					properties.load(fis);
					fis.close();
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				Debug.trace("Messages loaded from disk, size: " + properties.size()); //$NON-NLS-1$
			}
		}
		else
		{
			loadMessagesFromDatabaseInternal(DataSourceUtils.getI18NDataSource(solution, callback.getSettings()), callback.getClientID(),
				callback.getSettings(), callback.getDataServer(), callback.getRepository(), properties, language, ALL_LOCALES, null, null,
				callback.getI18NColumnNameFilter(), callback.getI18NColumnValueFilter(), fm);
		}
		return properties;
	}

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public static void loadMessagesFromDatabaseInternal(String i18nDatasource, String clientId, Properties settings, IDataServer dataServer,
		IRepository repository, Properties properties, Locale language, IFoundSetManagerInternal fm)
	{
		if (Messages.customMessageLoader != null)
		{
			Messages.customMessageLoader.loadMessages(i18nDatasource, properties, language, ALL_LOCALES, null);
		}
		else if (dataServer != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, dataServer, repository, properties, null, language, ALL_LOCALES,
				null, null, null, null, fm);
		}
		else if (ApplicationServerRegistry.get() != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, ApplicationServerRegistry.get().getDataServer(),
				ApplicationServerRegistry.get().getLocalRepository(), properties, null, language, ALL_LOCALES, null, null, null, null, fm);
		}
	}

	private static String[] getServerTableNames(String i18nDatasource, Properties settings)
	{
		String[] names = new String[2];
		if (invalidConnection) return names;

		names[0] = settings.getProperty("defaultMessagesServer"); //$NON-NLS-1$
		names[1] = settings.getProperty("defaultMessagesTable"); //$NON-NLS-1$

		if (i18nDatasource != null)
		{
			String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(i18nDatasource);
			String i18nServerName = dbServernameTablename != null ? dbServernameTablename[0] : null;
			String i18nTableName = dbServernameTablename != null ? dbServernameTablename[1] : null;

			if (i18nServerName != null && i18nServerName.equals(names[0]))
			{
				if (i18nTableName != null && !i18nTableName.equals(names[1]))
				{
					names[1] = i18nTableName;
				}
			}
			else if (i18nServerName != null && i18nTableName != null)
			{
				names[0] = i18nServerName;
				names[1] = i18nTableName;
			}
		}
		Debug.trace("Loading messages from server: " + names[0] + " table: " + names[1]); //$NON-NLS-1$ //$NON-NLS-2$
		return names;
	}

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public static void loadMessagesFromDatabaseInternal(String i18nDatasource, String clientId, Properties settings, IDataServer dataServer,
		IRepository repository, Properties properties, Locale language, int loadingType, String searchKey, String searchText, String columnNameFilter,
		String[] columnValueFilter, IFoundSetManagerInternal fm)
	{
		if (Messages.customMessageLoader != null)
		{
			Messages.customMessageLoader.loadMessages(i18nDatasource, properties, language, loadingType, searchKey);
		}
		else if (dataServer != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, dataServer, repository, properties, null, language, ALL_LOCALES,
				searchKey, searchText, columnNameFilter, columnValueFilter, fm);
		}
		else if (ApplicationServerRegistry.get() != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, ApplicationServerRegistry.get().getDataServer(),
				ApplicationServerRegistry.get().getLocalRepository(), properties, null, language, ALL_LOCALES, searchKey, searchText, columnNameFilter,
				columnValueFilter, fm);
		}
	}

	public static void loadMessagesFromDatabase(String i18nDatasource, String clientId, Properties settings, IDataServer dataServer, IRepository repository,
		Properties defaultProperties, Properties localeProperties, Locale language, String searchKey, String searchText, String columnNameFilter,
		String[] columnValueFilter, IFoundSetManagerInternal fm)
	{
		if (Messages.customMessageLoader != null)
		{
			Messages.customMessageLoader.loadMessages(i18nDatasource, defaultProperties, localeProperties, language, searchKey);
		}
		else if (dataServer != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, dataServer, repository, defaultProperties, localeProperties,
				language, ALL_LOCALES, searchKey, searchText, columnNameFilter, columnValueFilter, fm);
		}
		else if (ApplicationServerRegistry.get() != null)
		{
			loadMessagesFromDatabaseRepositoryInternal(i18nDatasource, clientId, settings, ApplicationServerRegistry.get().getDataServer(),
				ApplicationServerRegistry.get().getLocalRepository(), defaultProperties, localeProperties, language, ALL_LOCALES, searchKey, searchText,
				columnNameFilter, columnValueFilter, fm);
		}
	}

	/**
	 * CURRENTLY FOR INTERNAL USE ONLY, DO NOT CALL.
	 *
	 * @exclude
	 */
	public static void loadMessagesFromDatabaseRepositoryInternal(String i18nDatasource, String clientId, Properties settings, IDataServer dataServer,
		IRepository repository, Properties properties, Properties localeProperties, Locale language, int loadingType, String searchKey, String searchText,
		String columnNameFilter, String[] columnValueFilter, IFoundSetManagerInternal fm)
	{
		noConnection = false;
		String[] names = getServerTableNames(i18nDatasource, settings);
		String serverName = names[0];
		String tableName = names[1];

		if (serverName == null || tableName == null || "".equals(serverName) || "".equals(tableName))
		{
			noConnection = true;
			return;
		}

		try
		{
			Debug.trace("Loading messages from DB: Server: " + serverName + " Table: " + tableName + " Language: " + language); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			IServer server = repository.getServer(serverName);
			if (server == null)
			{
				noConnection = true;
				return;
			}
			Table table = (Table)server.getTable(tableName);
			if (table == null)
			{
				noConnection = true;
				return;
			}
			Column filterColumn = null;
			if (columnNameFilter != null)
			{
				// check if column exists
				filterColumn = table.getColumn(columnNameFilter);
				if (filterColumn == null)
				{
					return;
				}
			}

			String[] iColumnValueFilter = columnValueFilter;
			boolean isColumnValueFilterChanged = false;
			if (filterColumn == null && fm != null)
			{
				QueryTable messagesTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
				QuerySelect sql = new QuerySelect(messagesTable);
				ArrayList<TableFilter> tableFilters = fm.getTableFilterParams(serverName, sql);
				if (tableFilters != null)
				{
					for (TableFilter tableFilter : tableFilters)
					{
						TableFilterdefinition tableFilterdefinition = tableFilter.getTableFilterdefinition();
						if (tableFilterdefinition instanceof DataproviderTableFilterdefinition)
						{
							DataproviderTableFilterdefinition dpTtableFilterdefinition = (DataproviderTableFilterdefinition)tableFilterdefinition;
							Object value = dpTtableFilterdefinition.getValue();
							if (value instanceof Object[])
							{
								filterColumn = table.getColumn(dpTtableFilterdefinition.getDataprovider());
								iColumnValueFilter = new String[((Object[])value).length];
								for (int i = 0; i < ((Object[])value).length; i++)
								{
									iColumnValueFilter[i] = ((Object[])value)[i] != null ? ((Object[])value)[i].toString() : null;
								}
								isColumnValueFilterChanged = true;
								break;
							}
						}
					}
				}
			}

			if (!isColumnValueFilterChanged)
			{
				loadMessagesFromDatabaseRepositorySinglefilter(server, table, clientId, dataServer, properties, localeProperties, language, loadingType,
					searchKey, searchText, filterColumn, null, fm);
			}
			if (iColumnValueFilter != null)
			{
				for (int i = iColumnValueFilter.length - 1; i >= 0; i--)
				{
					loadMessagesFromDatabaseRepositorySinglefilter(server, table, clientId, dataServer, properties, localeProperties, language, loadingType,
						searchKey, searchText, filterColumn, iColumnValueFilter[i], fm);
				}
			}
		}
		catch (Exception e)
		{
			Debug.error("Couldn't get the default messages"); //$NON-NLS-1$
			Debug.error(e);
			invalidConnection = true;
		}
	}

	private static void loadMessagesFromDatabaseRepositorySinglefilter(IServer server, Table table, String clientId, IDataServer dataServer,
		Properties properties, Properties localeProperties, Locale language, int loadingType, String searchKey, String searchText, Column filterColumn,
		String singleColumnValueFilter, IFoundSetManagerInternal fm) throws RemoteException, ServoyException
	{

		Debug.trace("Loading messages from DB: Server: " + server.getName() + " Table: " + table.getName() + " Language: " + language); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

		if (loadingType == ALL_LOCALES || loadingType == DEFAULT_LOCALE)
		{
			QueryTable messagesTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
			QuerySelect sql = new QuerySelect(messagesTable);
			QueryColumn msgKey = new QueryColumn(messagesTable, -1, "message_key", Types.VARCHAR, 150, 0, 0); //$NON-NLS-1$
			QueryColumn msgVal = new QueryColumn(messagesTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0); //$NON-NLS-1$
			QueryColumn msgLang = new QueryColumn(messagesTable, -1, "message_language", Types.VARCHAR, 5, 0, 0); //$NON-NLS-1$
			sql.addColumn(msgKey);
			sql.addColumn(msgVal);

			String condMessages = "MESSAGES"; //$NON-NLS-1$
			sql.addCondition(condMessages,
				new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR | IBaseSQLCondition.ORNULL_MODIFIER, msgLang, new QueryColumnValue("", null)));

			if (filterColumn != null)
			{
				QueryColumn columnFilter = new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(), filterColumn.getType(),
					filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags());
				CompareCondition cc = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, columnFilter,
					new QueryColumnValue(singleColumnValueFilter, null));
				sql.addCondition(condMessages, cc);
			}

			if (searchKey != null || searchText != null)
			{
				QueryTable subselectTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
				QuerySelect subselect = new QuerySelect(subselectTable);
				QueryColumn msgKeySub = new QueryColumn(subselectTable, -1, "message_key", Types.VARCHAR, 150, 0, 0); //$NON-NLS-1$
				QueryColumn msgValueSub = new QueryColumn(subselectTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0); //$NON-NLS-1$
				QueryColumn msgLangSub = new QueryColumn(subselectTable, -1, "message_language", Types.VARCHAR, 5, 0, 0); //$NON-NLS-1$
				subselect.addColumn(msgKeySub);

				String condSearch = "SEARCH"; //$NON-NLS-1$
				if (searchKey != null)
				{
					subselect.addCondition(condSearch,
						new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, msgKeySub, new QueryColumnValue('%' + searchKey + '%', null)));
				}
				if (searchText != null)
				{
					subselect.addConditionOr(condSearch,
						new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, msgValueSub, new QueryColumnValue('%' + searchText + '%', null)));
				}
				String condLang = "LANGUAGE"; //$NON-NLS-1$
				subselect.addCondition(condLang,
					new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgLangSub, new QueryColumnValue(localeToString(language), null)));
				subselect.addConditionOr(condLang,
					new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR | IBaseSQLCondition.ORNULL_MODIFIER, msgLangSub, new QueryColumnValue("", null))); //$NON-NLS-1$

				sql.addCondition(condMessages, new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new QueryColumn[] { msgKey }, subselect, true));
			}
			if (Debug.tracing()) Debug.trace("Loading messages from DB: SQL: " + sql); //$NON-NLS-1$

			IDataSet set = dataServer.performQuery(clientId, server.getName(), null, sql, fm != null ? fm.getTableFilterParams(server.getName(), sql) : null,
				false, 0, Integer.MAX_VALUE, IDataServer.MESSAGES_QUERY);
			for (int i = 0; i < set.getRowCount(); i++)
			{
				Object[] row = set.getRow(i);
				if (row[0] != null && row[1] != null)
				{
					properties.setProperty((String)row[0], (String)row[1]);
				}
			}
		}

		if (loadingType == ALL_LOCALES || loadingType == SPECIFIED_LANGUAGE)
		{
			fillLocaleMessages(clientId, dataServer, table, server.getName(), filterColumn, singleColumnValueFilter, searchKey, searchText, language,
				localeProperties != null ? localeProperties : properties, SPECIFIED_LANGUAGE, fm);
		}

		if (loadingType == ALL_LOCALES || loadingType == SPECIFIED_LOCALE)
		{
			fillLocaleMessages(clientId, dataServer, table, server.getName(), filterColumn, singleColumnValueFilter, searchKey, searchText, language,
				localeProperties != null ? localeProperties : properties, SPECIFIED_LOCALE, fm);
		}
	}

	private static void fillLocaleMessages(String clientId, IDataServer dataServer, Table table, String serverName, Column filterColumn,
		Object columnValueFilter, String searchKey, String searchText, Locale language, Properties properties, int loadingType, IFoundSetManagerInternal fm)
		throws ServoyException, RemoteException
	{
		QueryTable messagesTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
		QuerySelect sql = new QuerySelect(messagesTable);
		QueryColumn msgKey = new QueryColumn(messagesTable, -1, "message_key", Types.VARCHAR, 150, 0, 0); //$NON-NLS-1$
		QueryColumn msgVal = new QueryColumn(messagesTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0); //$NON-NLS-1$
		QueryColumn msgLang = new QueryColumn(messagesTable, -1, "message_language", Types.VARCHAR, 5, 0, 0); //$NON-NLS-1$
		sql.addColumn(msgKey);
		sql.addColumn(msgVal);

		String condMessages = "MESSAGES"; //$NON-NLS-1$
		String langValue = (loadingType == SPECIFIED_LOCALE) ? localeToString(language) : language.getLanguage();

		sql.addCondition(condMessages, new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgLang, new QueryColumnValue(langValue, null))); // default

		if (filterColumn != null)
		{
			QueryColumn columnFilter = new QueryColumn(messagesTable, filterColumn.getID(), filterColumn.getSQLName(), filterColumn.getType(),
				filterColumn.getLength(), filterColumn.getScale(), filterColumn.getFlags());
			CompareCondition cc = new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, columnFilter, new QueryColumnValue(columnValueFilter, null));
			sql.addCondition(condMessages, cc);
		}

		if (searchKey != null || searchText != null)
		{
			QueryTable subselectTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
			QuerySelect subselect = new QuerySelect(subselectTable);
			QueryColumn msgKeySub = new QueryColumn(subselectTable, -1, "message_key", Types.VARCHAR, 150, 0, 0); //$NON-NLS-1$
			QueryColumn msgValueSub = new QueryColumn(subselectTable, -1, "message_value", Types.VARCHAR, 2000, 0, 0); //$NON-NLS-1$
			QueryColumn msgLangSub = new QueryColumn(subselectTable, -1, "message_language", Types.VARCHAR, 5, 0, 0); //$NON-NLS-1$
			subselect.addColumn(msgKeySub);

			String condSearch = "SEARCH"; //$NON-NLS-1$
			if (searchKey != null)
			{
				subselect.addCondition(condSearch,
					new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, msgKeySub, new QueryColumnValue('%' + searchKey + '%', null)));
			}
			if (searchText != null)
			{
				subselect.addConditionOr(condSearch,
					new CompareCondition(IBaseSQLCondition.LIKE_OPERATOR, msgValueSub, new QueryColumnValue('%' + searchText + '%', null)));
			}

			sql.addCondition(condMessages, new SetCondition(IBaseSQLCondition.EQUALS_OPERATOR, new QueryColumn[] { msgKey }, subselect, true));
		}

		if (Debug.tracing()) Debug.trace("Loading messages from DB: SQL: " + sql); //$NON-NLS-1$
		IDataSet set = dataServer.performQuery(clientId, serverName, null, sql, fm != null ? fm.getTableFilterParams(serverName, sql) : null, false, 0,
			Integer.MAX_VALUE, IDataServer.MESSAGES_QUERY);
		for (int i = 0; i < set.getRowCount(); i++)
		{
			Object[] row = set.getRow(i);
			if (row[1] != null && !"".equals(row[1])) //$NON-NLS-1$
			{
				properties.setProperty((String)row[0], (String)row[1]);
			}
		}
	}


	public static void setI18nScriptingMessage(String key, String value)
	{
		if (key != null)
		{
			if (localeSolutionMessages == null) localeSolutionMessages = new Properties();
			if (value == null) localeSolutionMessages.remove(key);
			else localeSolutionMessages.setProperty(key, value);
		}
	}

	public static String getStringIfPrefix(String key)
	{
		if (key != null && key.startsWith("i18n:")) //$NON-NLS-1$
		{
			return getString(key.substring(5));
		}
		return key;
	}

	public static String getString(String key)
	{
		if (key == null) return ""; //$NON-NLS-1$

		if (key.startsWith("i18n:")) //$NON-NLS-1$
		{
			key = key.substring(5);
		}

		try
		{
			String value = null;
			if (localeSolutionMessages != null)
			{
				value = localeSolutionMessages.getProperty(key);
			}
			if (value == null && localeServerMessages != null)
			{
				value = localeServerMessages.getProperty(key);
			}
			if (value == null)
			{
				value = localeJarMessages.getString(key);
			}

			// special handling  for mnemonics, should always return a one char string!
			if ((value == null || value.length() == 0) && key.endsWith(".mnemonic"))
			{
				value = "\0"; //$NON-NLS-1$
			}
			return value;
		}
		catch (MissingResourceException e)
		{
			if (Arrays.asList(Messages.JRE_DEFAULT_KEYS).indexOf(key) != -1) return Messages.JRE_DEFAULT_KEY_VALUE;
			else return '!' + key + '!';
		}
	}

	public static String getString(String key, Object[] args)
	{
		if (key == null) return ""; //$NON-NLS-1$

		if (key.startsWith("i18n:")) //$NON-NLS-1$
		{
			key = key.substring(5);
		}

		String message = null;
		try
		{
			message = getString(key);
			message = Utils.stringReplace(message, "'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
			if (args != null && args.length != 0)
			{
				MessageFormat mf = new MessageFormat(message);
				mf.setLocale(localeJarMessages.getLocale());
				return mf.format(args);
			}
			else
			{
				return message;
			}
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
		catch (IllegalArgumentException e)
		{
			return '!' + key + "!,txt:" + message + ", error:" + e.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static boolean deleteKey(String key, String i18nDatasource, String clientId, Properties settings, IDataServer dataServer, IRepository repository,
		IFoundSetManagerInternal fm)
	{
		String[] names = getServerTableNames(i18nDatasource, settings);
		String serverName = names[0];
		String tableName = names[1];
		if (serverName != null && tableName != null)
		{
			try
			{
				IServer server = repository.getServer(serverName);
				if (server == null)
				{
					return false;
				}
				Table table = (Table)server.getTable(tableName);
				if (table == null)
				{
					return false;
				}

				QueryTable messagesTable = new QueryTable(table.getSQLName(), table.getDataSource(), table.getCatalog(), table.getSchema());
				QueryColumn msgKey = new QueryColumn(messagesTable, -1, "message_key", Types.VARCHAR, 150, 0, 0); //$NON-NLS-1$
				QueryDelete delete = new QueryDelete(messagesTable);
				delete.addCondition(new CompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, msgKey, key));

				ISQLStatement sqlStatement = new SQLStatement(ISQLActionTypes.DELETE_ACTION, serverName, tableName, null, null, delete,
					fm != null ? fm.getTableFilterParams(serverName, delete) : null);

				dataServer.performUpdates(clientId, new ISQLStatement[] { sqlStatement });
			}
			catch (Exception e)
			{
				return false;
			}
		}
		return true;
	}

	public static String localeToString(Locale locale)
	{
		if (locale.getVariant().length() > 0)
		{
			StringBuffer localeString = new StringBuffer(locale.getLanguage());
			String country = locale.getCountry();
			if (country.length() > 0) localeString.append("_").append(country);
			return localeString.toString();
		}
		else return locale.toString();
	}
}
