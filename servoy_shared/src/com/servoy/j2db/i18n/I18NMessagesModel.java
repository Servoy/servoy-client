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
package com.servoy.j2db.i18n;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.servoy.base.util.I18NProvider;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.IFoundSetManagerInternal;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.StringComparator;
import com.servoy.j2db.util.Utils;

public class I18NMessagesModel
{
	private final String i18nDatasource;
	private final Solution solution;
	private final String clientId;
	private final Properties settings;
	private final IDataServer dataServer;
	private final IRepository repository;

	protected Map<String, I18NMessagesModelEntry> defaultMap;
	protected Locale language;


	public I18NMessagesModel(String i18nDatasource, String clientId, Properties settings, IDataServer dataServer, IRepository repository)
	{
		this.i18nDatasource = i18nDatasource;
		this.solution = null;
		this.clientId = clientId;
		this.settings = settings;
		this.dataServer = dataServer;
		this.repository = repository;
	}

	public I18NMessagesModel(Solution solution, String clientId, Properties settings, IDataServer dataServer, IRepository repository)
	{
		this.i18nDatasource = null;
		this.solution = solution;
		this.clientId = clientId;
		this.settings = settings;
		this.dataServer = dataServer;
		this.repository = repository;
	}

	public void setLanguage(Locale language)
	{
		this.language = language;

		defaultMap = new TreeMap<String, I18NMessagesModelEntry>();
		Properties defaultJarMessages = new Properties();
		try
		{
			defaultJarMessages.load(Messages.class.getClassLoader().getResourceAsStream(Messages.BUNDLE_NAME.replace('.', '/') + ".properties")); //$NON-NLS-1$
			for (String jre_key : Messages.JRE_DEFAULT_KEYS)
				defaultJarMessages.put(jre_key, Messages.JRE_DEFAULT_KEY_VALUE);
		}
		catch (Exception e)
		{
			// Debug.error(e);
		}
		Properties currentLocaleJarMessages = new Properties();
		try
		{
			currentLocaleJarMessages.load(
				Messages.class.getClassLoader().getResourceAsStream(Messages.BUNDLE_NAME.replace('.', '/') + "_" + language.getLanguage() + ".properties")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e)
		{
			// Debug.error(e);
		}
		try
		{
			currentLocaleJarMessages.load(
				Messages.class.getClassLoader().getResourceAsStream(Messages.BUNDLE_NAME.replace('.', '/') + "_" + language + ".properties")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e)
		{
			// Debug.error(e);
		}
		addKeys(defaultMap, defaultJarMessages, currentLocaleJarMessages);
	}

	static void addKeys(Map<String, I18NMessagesModelEntry> map, Properties defaultMessages, Properties localeMessages)
	{
		Iterator<Map.Entry<Object, Object>> it = defaultMessages.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Object, Object> entry = it.next();
			Object[] array = new Object[2];
			array[0] = entry.getValue();
			array[1] = localeMessages.get(entry.getKey());
			map.put((String)entry.getKey(),
				new I18NMessagesModelEntry((String)entry.getKey(), (String)entry.getValue(), localeMessages.getProperty((String)entry.getKey())));
		}

		it = localeMessages.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Object, Object> entry = it.next();
			if (!defaultMessages.containsKey(entry.getKey()))
			{
				Object[] array = new Object[2];
				array[0] = entry.getValue();
				array[1] = entry.getValue();
				map.put((String)entry.getKey(), new I18NMessagesModelEntry((String)entry.getKey(), (String)entry.getValue(), (String)entry.getValue()));
			}
		}
	}

	public Collection<I18NMessagesModelEntry> getMessages(String searchKey, String filterColumn, String[] filterValue, IFoundSetManagerInternal fm,
		boolean mobileKeys, I18NMessagesModelEntry selectedEntry)
	{
		TreeMap<String, I18NMessagesModelEntry> tm = new TreeMap<String, I18NMessagesModelEntry>(StringComparator.INSTANCE);
		if (defaultMap != null)
		{
			tm.putAll(defaultMap);
			if (mobileKeys)
			{
				Iterator<Map.Entry<String, I18NMessagesModelEntry>> it = tm.entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<String, I18NMessagesModelEntry> entry = it.next();
					if (!entry.getKey().toLowerCase().startsWith(I18NProvider.MOBILE_KEY_PREFIX))
					{
						it.remove();
					}
				}
			}
			if (searchKey != null)
			{
				String searchKeyLowerCase = searchKey.toLowerCase();
				Iterator<Map.Entry<String, I18NMessagesModelEntry>> it = tm.entrySet().iterator();
				while (it.hasNext())
				{
					Map.Entry<String, I18NMessagesModelEntry> entry = it.next();
					if ((entry.getKey()).toLowerCase().indexOf(searchKeyLowerCase) == -1)
					{
						if (entry.getValue().defaultvalue == null || (entry.getValue().defaultvalue).toLowerCase().indexOf(searchKeyLowerCase) == -1)
						{
							if (entry.getValue().localeValue == null || (entry.getValue().localeValue).toLowerCase().indexOf(searchKeyLowerCase) == -1)
							{
								it.remove();
							}
						}
					}
				}
			}
		}

		Properties messageDefault = new Properties();
		Properties messageLocale = new Properties();
		Messages.loadMessagesFromDatabase(null, clientId, settings, dataServer, repository, messageDefault, messageLocale, language, searchKey, searchKey, null,
			null, fm);

		addKeys(tm, messageDefault, messageLocale);

		messageDefault.clear();
		messageLocale.clear();
		Messages.loadMessagesFromDatabase(i18nDatasource != null ? i18nDatasource : solution != null ? solution.getI18nDataSource() : null, clientId, settings,
			dataServer, repository, messageDefault, messageLocale, language, searchKey, searchKey, filterColumn, filterValue, fm);

		addKeys(tm, messageDefault, messageLocale);
		if (selectedEntry != null && !tm.containsKey(selectedEntry.key))
		{
			tm.put(selectedEntry.key, selectedEntry);
		}
		return tm.values();
	}

	public Map<String, I18NMessagesModelEntry> getDefaultMap()
	{
		return new HashMap<String, I18NMessagesModelEntry>(defaultMap);
	}

	public static class I18NMessagesModelEntry
	{
		public String key;
		public String defaultvalue;
		public String localeValue;

		public I18NMessagesModelEntry(String key, String defaultvalue, String localeValue)
		{
			this.key = key;
			this.defaultvalue = defaultvalue;
			this.localeValue = localeValue;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof I18NMessagesModelEntry)) return false;
			if (Utils.equalObjects(this.key, ((I18NMessagesModelEntry)obj).key) &&
				Utils.equalObjects(this.defaultvalue, ((I18NMessagesModelEntry)obj).defaultvalue) &&
				Utils.equalObjects(this.localeValue, ((I18NMessagesModelEntry)obj).localeValue)) return true;
			return false;
		}
	}
}
