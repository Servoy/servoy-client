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
package com.servoy.j2db.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.StringComparator;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "i18n", scriptingName = "i18n")
public class JSI18N
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSI18N.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return null;
			}
		});
	}

	private volatile IApplication application;

	public JSI18N(IApplication application)
	{
		this.application = application;
	}

	/**
	 * Set/Overwrite the locale for this client.
	 * All forms not yet loaded will change (execute this in solution startup or first form).
	 * 
	 * The language must be a lowercase 2 letter code defined by ISO-649.
	 * see http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt
	 * The country must be an upper case 2 letter code defined by IS-3166
	 * see http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html 
	 *
	 * NOTE: For more information on i18n, see the chapter on Internationalization (i18n) in the Servoy Developer User's Guide, beginning with the Introduction to i18n
	 * 
	 * @sample
	 * //Warning: already created form elements with i18n text lookup will not change,
	 * //so call this method in the solution startup method or in methods from first form
	 * 
	 * i18n.setLocale('en','US');
	 *
	 * @param language The lowercase 2 letter code
	 *
	 * @param country The upper case 2 letter code.
	 */
	public void js_setLocale(String language, String country)
	{
		application.setLocale(new Locale(language, country));
	}

	/**
	 * Gets the current language; based on the current locale settings in the Servoy Client Locale preferences. 
	 * 
	 * NOTE: For more information on i18n, see the chapter on Internationalization (i18n) in the Servoy Developer User's Guide, beginning with the Introduction to i18n
	 * @sample
	 * var currLang = i18n.getCurrentLanguage();
	 * 
	 * @return a String representing the current language.
	 */
	public String js_getCurrentLanguage()
	{
		return application.getLocale().getLanguage();
	}

	/**
	 * Gets the current country; based on the current locale settings in the Servoy Client Locale preferences. 
	 * 
	 * NOTE: For more information on i18n, see the chapter on Internationalization (i18n) in the Servoy Developer User's Guide, beginning with the Introduction to i18n
	 * 
	 * @sample
	 * var currCountry = i18n.getCurrentCountry();
	 * 
	 * @return a String representing the current country.
	 */
	public String js_getCurrentCountry()
	{
		return application.getLocale().getCountry();
	}

	/**
	 * Gets the current time zone of the client; based on the current locale settings in the Servoy Client Locale preferences. For Servoy Web Clients the time zone is given by the browser (if it is possible to obtain it).
	 *
	 * @sample
	 * var currTimeZone = i18n.getCurrentTimeZone();
	 * 
	 * @return a String representing the current time zone.
	 */
	public String js_getCurrentTimeZone()
	{
		return application.getTimeZone().getID();
	}

	/**
	 * Gets the current default date format; based on the current locale settings in the Servoy Client Locale preferences.
	 *  
	 * @sample
	 * var defaultDateFormat = i18n.getDefaultDateFormat();
	 * 
	 * @return a String representing the default date format.
	 */
	public String js_getDefaultDateFormat()
	{
		return TagResolver.getFormatString(Date.class, application.getSettings());
	}

	/**
	 * Gets the current default number format; based on the current locale settings in the Servoy Client Locale preferences.
	 * 
	 * @sample
	 * var defaultNumberFormat = i18n.getDefaultNumberFormat();
	 * 
	 * @return a String representing the default number format.
	 */
	public String js_getDefaultNumberFormat()
	{
		return TagResolver.getFormatString(Number.class, application.getSettings());
	}

	/**
	 * Returns a dataset with rows that contains a language key (en) and the displayname (English) column.
	 *
	 * See http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt for a list that could be returned.
	 * 
	 * @sample
	 * var set = i18n.getLanguages();
	 * for(var i=1;i<=set.getMaxRowIndex();i++)
	 * {
	 * 	application.output(set.getValue(i, 1) + " " + set.getValue(i, 2));
	 * }
	 * 
	 * @return a JSDataSet with all the languages.
	 * 
	 * @link http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt
	 */
	public JSDataSet js_getLanguages()
	{
		Locale[] locales = Locale.getAvailableLocales();
		TreeMap<String, String> languages = new TreeMap<String, String>(StringComparator.INSTANCE);
		for (Locale element : locales)
		{
			String name = element.getDisplayLanguage(element);

			languages.put(element.getLanguage(), name);
		}
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		Iterator<Entry<String, String>> it = languages.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<String, String> entry = it.next();
			list.add(new Object[] { entry.getKey(), entry.getValue() });
		}
		BufferedDataSet set = new BufferedDataSet(new String[] { "language_key", "language_name" }, list); //$NON-NLS-1$ //$NON-NLS-2$
		return new JSDataSet(application, set);
	}

	/**
	 * Returns an array of known timezones.
	 *
	 * @sample 
	 * var timeZones = i18n.getTimeZones();
	 * 
	 * @return an Array with all the timezones.
	 */
	public String[] js_getTimeZones()
	{
		return TimeZone.getAvailableIDs();
	}


	/**
	 * Returns the offset (in milliseconds) of this time zone from UTC for the current date or at the specified date.
	 *
	 * @sample
	 * var timeZoneOffset = i18n.getTimeZoneOffset('America/Los_Angeles');
	 *
	 * @param timezone The time zone to get the offset for.
	 *
	 * @param date optional The date in the time zone (default current date). Needed in case daylight saving time/GMT offset changes are used in the time zone.
	 * 
	 * @return an int representing the time zone's offset from UTC.
	 */
	public int js_getTimeZoneOffset(Object[] vargs)
	{
		if (vargs == null || vargs.length == 0 || vargs[0] == null) return 0;

		String timeZoneId = vargs[0].toString();
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
		if (timeZone != null)
		{
			if (vargs.length > 1 && vargs[1] instanceof Date)
			{
				return timeZone.getOffset(((Date)vargs[1]).getTime());
			}
			else
			{
				return timeZone.getOffset(System.currentTimeMillis());
			}
		}
		return 0;
	}

	/**
	 * Gets the real message (for the clients locale) for a specified message key. 
	 *
	 * @sample
	 * // returns 'Welcome my_name in my solution'
	 * // if the key 'mykey.username.text' is 'Welcome {0} in my solution'
	 * i18n.getI18NMessage('mykey.username.text',new Array('my_name'))
	 *
	 * @param i18nKey The message key
	 *
	 * @return a String that is the message for the message key.
	 */
	public String js_getI18NMessage(String i18nKey)
	{
		return application.getI18NMessage(i18nKey);
	}

	/**
	 * Gets the real message (for the clients locale) for a specified message key. 
	 * You can use parameter substitution by using {n}, where n is a index number of the value thats in the arguments array.
	 *
	 * @sample
	 * // returns 'Welcome my_name in my solution'
	 * // if the key 'mykey.username.text' is 'Welcome {0} in my solution'
	 * i18n.getI18NMessage('mykey.username.text',new Array('my_name'))
	 *
	 * @param i18nKey The message key
	 *
	 * @param dynamicValues Arguments array when using parameter substitution.
	 * 
	 * @return a String that is the message for the message key.
	 */
	public String js_getI18NMessage(String i18nKey, Object[] dynamicValues)
	{
		return application.getI18NMessage(i18nKey, dynamicValues);
	}

	/**
	 * Sets the value of i18n key for client scope,if value null the setting is removed. 
	 * All forms not yet loaded will change (execute this in solution startup or first form)
	 *
	 * @sample
	 * //sets the value of i18n key for client scope; if value null the setting is removed
	 * //Warning: already created form elements with i18n text lookup will not change,
	 * //so call this method in the solution startup method or in methods from first form
	 * 
	 * i18n.setI18NMessage('mykey.username.text','my_name')
	 *
	 * @param i18nKey The message key
	 *
	 * @param value They value for the message key.
	 */
	public void js_setI18NMessage(String i18nKey, String value)
	{
		application.setI18NMessage(i18nKey, value);
	}

	/**
	 * Call this if you want to add a filter for a column (created by you) in the i18n table.
	 * So that you can have multiple default values and multiple values per locale for one key.
	 * You can use filter fall back by setting the filter value as a string with filters values
	 * separated by "_" (underscore)
	 *
	 * @sample
	 * // Puts i18n in filter mode - this allows you to have multiple default/per locale
	 * // values for one key and to use one of them based on the filter parameters.
	 * // Let's say you added a new column "message_variant" to your i18n table.
	 * // Now you can have keys that will translate to a language differently depending on the used variant
	 * // For example you have 2 rows in you table for key X, language EN, different values and different "message_variant" (let's say 1 and 2)
	 * // If you want the solution to use the first variant, you will have to call:
	 * i18n.setI18NMessagesFilter('message_variant', '1')
	 * 
	 * // Using filter fall back : if there is no key for 'message_variant' with value '1', it will
	 * // search for value '2'
	 * i18n.setI18NMessagesFilter('message_variant', '1_2')
	 * 
	 * // ATTENTION: if you use setI18NMessagesFilter(...) it is not recommended to use the i18n Dialog (especially before the filter is applied through JS).
	 *
	 * @param columnName The column name that is the filter column in the i18n table.
	 *
	 * @param value The filter value.  
	 */
	public void js_setI18NMessagesFilter(String columnName, String value)
	{
		application.setI18NMessagesFilter(columnName, value);
	}

	/**
	 * Returns a dataset with rows that contains 3 columns: 'key' (i18n key), 'reference' (reference text for that key) and 'locale ([CURRENT_LOCALE])' (where [CURRENT_LOCALE] is the current language) - with the system messages of servoy. 
	 * This means all servoy messages, with all available translations.
	 *
	 * @sample
	 * var set = i18n.getSystemMessages();
	 * for(var i=1;i<=set.getMaxRowIndex();i++)
	 * {
	 * 	application.output(set.getValue(i, 1) + " " + set.getValue(i, 2)+ " " + set.getValue(i, 3));
	 * }
	 * 
	 * @return a JSDataSet with all the system messages.
	 */
	public JSDataSet js_getSystemMessages()
	{
		Locale locale = application.getLocale();

		TreeMap<Object, Object> defaultJarMessages = new TreeMap<Object, Object>();
		try
		{
			Properties properties = new Properties();
			properties.load(Messages.class.getClassLoader().getResourceAsStream("com/servoy/j2db/messages.properties")); //$NON-NLS-1$
			defaultJarMessages.putAll(properties);
		}
		catch (Exception e)
		{
			//Debug.error(e);
		}
		Properties currentLocaleJarMessages = new Properties();
		try
		{
			currentLocaleJarMessages.load(Messages.class.getClassLoader().getResourceAsStream(
				"com/servoy/j2db/messages_" + locale.getLanguage() + ".properties")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e)
		{
			//Debug.error(e);
		}

		try
		{
			currentLocaleJarMessages.load(Messages.class.getClassLoader().getResourceAsStream("com/servoy/j2db/messages_" + locale + ".properties")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e)
		{
			//Debug.error(e);
		}
		Object[][] rowData = new Object[defaultJarMessages.size()][3];

		int k = 0;
		Iterator<Entry<Object, Object>> it = defaultJarMessages.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Object, Object> entry = it.next();
			Object key = entry.getKey();
			rowData[k][0] = key;
			rowData[k][1] = entry.getValue();
			rowData[k][2] = currentLocaleJarMessages.get(key);
			k++;
		}

		String[] cols = new String[] { "key", "reference", "locale (" + locale.getDisplayLanguage(locale) + ")" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
		BufferedDataSet set = new BufferedDataSet(cols, Arrays.asList(rowData));
		return new JSDataSet(application, set);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "i18n"; //$NON-NLS-1$
	}

	public void destroy()
	{
		application = null;
	}
}
