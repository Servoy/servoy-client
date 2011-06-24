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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.IApplicationServerMessagesLoader;
import com.servoy.j2db.util.Debug;

public class MessagesResourceBundle extends ResourceBundle implements Externalizable
{
	private static final long serialVersionUID = 1L;

	private static IApplicationServerMessagesLoader applicationServerLoader;

	private transient Locale locale;
	private transient String i18nColumnName;
	private transient String[] i18nColunmValue;
	private transient int solutionId;

	private transient Properties messages;
	private transient ResourceBundle jarBundle;
	private transient IApplication application;

	public MessagesResourceBundle()
	{
		// used by serialization.
	}

	/**Set application server loader for loading loading messages on the server that are specific for the client.
	 * 
	 * @param asl
	 */
	public static void setApplicationServerLoader(IApplicationServerMessagesLoader asl)
	{
		applicationServerLoader = asl;
	}

	public MessagesResourceBundle(IApplication application, Locale locale, String i18nColumnName, String[] i18nColunmValue, int solutionId)
	{
		this.application = application;
		this.locale = locale;
		this.i18nColumnName = i18nColumnName;
		this.i18nColunmValue = i18nColunmValue;
		this.solutionId = solutionId;
		this.jarBundle = ResourceBundle.getBundle(Messages.BUNDLE_NAME, locale);
	}

	@Override
	public Enumeration getKeys()
	{
		return new Enumeration()
		{
			private Enumeration solutionKeys = getMessages().keys();
			private final Enumeration jarKeys = jarBundle.getKeys();

			public Object nextElement()
			{
				if (solutionKeys != null) return solutionKeys.nextElement();
				else return jarKeys.nextElement();
			}

			public boolean hasMoreElements()
			{
				if (solutionKeys != null && solutionKeys.hasMoreElements())
				{
					return true;
				}
				solutionKeys = null;
				return jarKeys.hasMoreElements();
			}
		};
	}

	private Properties getMessages()
	{
		if (messages == null)
		{
			messages = new Properties();
			if (application != null)
			{
				Solution sol;
				try
				{
					sol = (Solution)application.getRepository().getActiveRootObject(solutionId);
					Messages.loadMessagesFromDatabaseInternal(null, application.getClientID(), application.getSettings(), application.getDataServer(),
						application.getRepository(), messages, locale, Messages.ALL_LOCALES, null, null, i18nColumnName, i18nColunmValue);
					Messages.loadMessagesFromDatabaseInternal(sol != null ? sol.getI18nDataSource() : null, application.getClientID(),
						application.getSettings(), application.getDataServer(), application.getRepository(), messages, locale, Messages.ALL_LOCALES, null,
						null, i18nColumnName, i18nColunmValue);
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
			}
			else if (applicationServerLoader != null)
			{
				// we are on the server load from the server.
				applicationServerLoader.loadMessages(messages, locale, solutionId, i18nColumnName, i18nColunmValue);
			}
		}
		return messages;
	}

	@Override
	protected Object handleGetObject(String key)
	{
		String value = null;
		Properties messages = getMessages();
		try
		{
			value = messages.getProperty(key);
			if (value == null)
			{
				value = jarBundle.getString(key);
			}
			// special handling for mnemonics, should always return a one char
			// string!
			if ((value == null || value.length() == 0) && key.endsWith(".mnemonic")) //$NON-NLS-1$
			{
				value = "\0"; //$NON-NLS-1$
			}
			return value;
		}
		catch (MissingResourceException e)
		{
			return '!' + key + '!';
		}
	}

	@Override
	public Locale getLocale()
	{
		return locale;
	}

	public void writeExternal(ObjectOutput s) throws IOException
	{
		if (locale == null)
		{
			locale = Locale.getDefault();
		}
		s.writeObject(locale);
		s.writeObject(i18nColumnName);
		s.writeObject(i18nColunmValue);
		s.writeInt(solutionId);
	}

	public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException
	{
		locale = (Locale)s.readObject();
		i18nColumnName = (String)s.readObject();
		i18nColunmValue = (String[])s.readObject();
		solutionId = s.readInt();
		jarBundle = ResourceBundle.getBundle(Messages.BUNDLE_NAME, locale);
	}
}
