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

import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;

import com.servoy.j2db.persistence.I18NUtil;
import com.servoy.j2db.persistence.RepositoryException;

public interface ICustomMessageLoader
{
	public void loadMessages(String i18nDatasource, Properties properties, Locale language, int loadingType, String searchKey);

	// Does the same as the method above, but loads both default and locale for all messages that match the search key.
	// No loading type is specified, because DEFAULT_LOCALE and SPECIFIED_LOCALE will be used anyway.
	public void loadMessages(String i18nDatasource, Properties defaultProperties, Properties localeProperties, Locale language, String searchKey);

	TreeMap<String, I18NUtil.MessageEntry> readMessages(String serverName, String tableName) throws RepositoryException;

	void save(String serverName, String tableName, TreeMap<String, I18NUtil.MessageEntry> messages) throws RepositoryException;
}
