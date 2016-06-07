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


import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.base.scripting.api.IJSFoundSet;
import com.servoy.base.scripting.api.IJSRecord;
import com.servoy.base.scripting.api.IJSUtils;
import com.servoy.base.util.ITagResolver;
import com.servoy.j2db.BasicFormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.dataprocessing.IRecordInternal;
import com.servoy.j2db.dataprocessing.JSDatabaseManager;
import com.servoy.j2db.dataprocessing.TagResolver;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Text;
import com.servoy.j2db.util.Utils;

/**
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Utils", scriptingName = "utils")
public class JSUtils implements IJSUtils
{
	private volatile IApplication application;

	public JSUtils(IApplication application)
	{
		this.application = application;
	}

	/**
	 * @deprecated As of release 2.0, replaced by {@link #hasRecords(JSFoundSet)}.
	 */
	@Deprecated
	public boolean js_hasChildRecords(Object foundset)
	{
		if (foundset instanceof IFoundSetInternal && foundset instanceof IJSFoundSet)
		{
			return hasRecords((IJSFoundSet)foundset);
		}
		return false;
	}

	/**
	 * Returns true if the (related)foundset exists and has records.
	 * Another use is, to pass a record and qualified relations string to test multiple relations/foundset at once
	 *
	 * @sample
	 * //test the orders_to_orderitems foundset
	 * if (%%elementName%%.hasRecords(orders_to_orderitems))
	 * {
	 * 	//do work on relatedFoundSet
	 * }
	 * //test the orders_to_orderitems.orderitems_to_products foundset to be reached from the current record
	 * //if (%%elementName%%.hasRecords(foundset.getSelectedRecord(),'orders_to_orderitems.orderitems_to_products'))
	 * //{
	 * //	//do work on deeper relatedFoundSet
	 * //}
	 *
	 * @param foundset the foundset to be tested

	 * @return true if exists
	 */
	@JSFunction
	public boolean hasRecords(IJSFoundSet foundset)//needed for calcs
	{
		if (foundset != null)
		{
			return foundset.getSize() > 0;
		}
		return false;
	}

	/**
	 * @clonedesc hasRecords(IJSFoundSet)
	 *
	 * @sampleas hasRecords(IJSFoundSet)
	 *
	 * @param record A JSRecord to test.
	 * @param relationString The relation name.
	 *
	 * @return true if the foundset/relation has records.
	 */
	@JSFunction
	public boolean hasRecords(IJSRecord record, String relationString)
	{
		return JSDatabaseManager.hasRecords((IRecordInternal)record, relationString);
	}

	/**
	 * Returns a string containing the character for the unicode number.
	 *
	 * @sample
	 * //returns a big dot
	 * var dot = utils.getUnicodeCharacter(9679);
	 *
	 * @param unicodeCharacterNumber the number indicating the unicode character
	 *
	 * @return a string containing the unicode character
	 */
	public String js_getUnicodeCharacter(int unicodeCharacterNumber)//no cast from int to char possible in javascript
	{
		return Character.toString((char)unicodeCharacterNumber);
	}

	/**
	 * Returns the text with %%tags%% replaced, based on provided record or foundset or form.
	 *
	 * @sample
	 * //Next line places a string in variable x, whereby the tag(%%TAG%%) is filled with the value of the database column 'company_name' of the selected record.
	 * var x = utils.stringReplaceTags("The companyName of the selected record is %%company_name%% ", foundset)
	 * //var otherExample = utils.stringReplaceTags("The amount of the related order line %%amount%% ", order_to_orderdetails);
	 * //var recordExample = utils.stringReplaceTags("The amount of the related order line %%amount%% ", order_to_orderdetails.getRecord(i);
	 * //Next line places a string in variable y, whereby the tag(%%TAG%%) is filled with the value of the form variable 'x' of the form named 'main'.
	 * //var y = utils.stringReplaceTags("The value of form variable is %%x%% ", forms.main);
	 * //The next sample shows the use of a javascript object
	 * //var obj = new Object();//create a javascript object
	 * //obj['x'] = 'test';//assign an named value
	 * //var y = utils.stringReplaceTags("The value of object variable is %%x%% ", obj);//use the named value in a tag
	 * @param text the text tags to work with
	 * @param scriptable the javascript object or foundset,record,form to be used to fill in the tags
	 * @return the text with replaced tags
	 */
	public String js_stringReplaceTags(String text, Object scriptable)
	{
		if (text != null)
		{
			ITagResolver tagResolver = null;
			Properties settings = null;
			if (scriptable instanceof FoundSet)
			{
				IRecordInternal record = ((FoundSet)scriptable).getRecord(((FoundSet)scriptable).getSelectedIndex());
				if (record != null)
				{
					settings = record.getParentFoundSet().getFoundSetManager().getApplication().getSettings();
					tagResolver = TagResolver.createResolver(record);
				}
			}
			else if (scriptable instanceof IRecordInternal)
			{
				IRecordInternal record = (IRecordInternal)scriptable;
				settings = record.getParentFoundSet().getFoundSetManager().getApplication().getSettings();
				tagResolver = TagResolver.createResolver(record);
			}
			else if (scriptable instanceof BasicFormController)
			{
				final BasicFormController fc = (BasicFormController)scriptable;
				IFoundSetInternal fs = fc.getFoundSet();
				final ITagResolver defaultTagResolver = (fs != null) ? TagResolver.createResolver(fs.getRecord(fs.getSelectedIndex())) : null;
				settings = fc.getApplication().getSettings();
				tagResolver = new ITagResolver()
				{
					public String getStringValue(String name)
					{
						Object value = fc.getFormScope().get(name);
						if (value == null || value == Scriptable.NOT_FOUND)
						{
							value = defaultTagResolver != null ? defaultTagResolver.getStringValue(name) : null;
						}
						return value != null ? value.toString() : ""; //$NON-NLS-1$
					}
				};
			}
			else if (scriptable instanceof Scriptable)
			{
				Scriptable scriptObject = (Scriptable)scriptable;
				settings = Settings.getInstance();
				tagResolver = TagResolver.createResolver(scriptObject, application.getLocale());
			}

			if (tagResolver != null && settings != null)
			{
				return Text.processTags(TagResolver.formatObject(text, application.getLocale(), settings), tagResolver);
			}
			return ""; //$NON-NLS-1$
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns true when Monday is the first day of the week for your current locale setting.
	 *
	 * @sample
	 * if(utils.isMondayFirstDayOfWeek())
	 * {
	 * 	//a date calculation
	 * }
	 * @return true if Monday is first day of the week in current locale
	 */
	public boolean js_isMondayFirstDayOfWeek()
	{
		return Calendar.getInstance().getFirstDayOfWeek() == Calendar.MONDAY;
	}

	/**
	 * Parse a string to a date object.
	 *
	 * @sample
	 * var parsedDate = utils.parseDate(datestring,'EEE, d MMM yyyy HH:mm:ss');
	 *
	 * @param date the date as text
	 * @param format the format to parse the to date
	 * @return the date as date object
	 */
	public Date js_parseDate(String date, String format)
	{
		if (format != null && date != null)
		{
			try
			{
				return new SimpleDateFormat(format, application.getLocale()).parse(date);
			}
			catch (ParseException ex)
			{
				Debug.error("Date parsing error: " + date + ", format: " + format, ex); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		return null;
	}

	/**
	 * @deprecated replaced by parseDate(String, String) or dateFormat(Date, String)
	 */
	@Deprecated
	public Object js_dateFormat(Object date, String format)
	{
		if (date instanceof String)
		{
			return js_parseDate((String)date, format);
		}
		if (date instanceof Date)
		{
			return dateFormat((Date)date, format);
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Format a date object to a text representation.
	 *
	 * @sample
	 * var formattedDateString = utils.dateFormat(dateobject,'EEE, d MMM yyyy HH:mm:ss');
	 *
	 * @param date the date
	 * @param format the format to output
	 * @return the date as text
	 */
	@JSFunction
	public String dateFormat(Date date, String format)
	{
		if (format != null && date != null)
		{
			return new SimpleDateFormat(format, application.getLocale()).format(date);
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns a datestamp from the timestamp (sets hours,minutes,seconds and milliseconds to 0).
	 *
	 * @sample
	 * var date = utils.timestampToDate(application.getTimeStamp());
	 *
	 * @param date object to be stripped from its time elements
	 * @return the stripped date object
	 */
	public Date js_timestampToDate(Date date)
	{
		if (date != null)
		{
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			Utils.applyMinTime(calendar);
			return calendar.getTime();
		}
		return null;
	}

	/**
	 * Returns the number of words, starting from the left.
	 *
	 * @sample
	 * //returns 'this is a'
	 * var retval = utils.stringLeftWords('this is a test',3);
	 *
	 * @param text to process
	 * @param numberof_words to return
	 * @return the string with number of words form the left
	 */
	public String js_stringLeftWords(String text, Number numberof_words)
	{
		if (text != null && numberof_words != null)
		{
			try
			{
				int words = numberof_words.intValue();
				StringBuilder sb = new StringBuilder();
				StringTokenizer st = new StringTokenizer(text, " "); //$NON-NLS-1$
				int i = 0;
				while (st.hasMoreTokens() && i < words)
				{
					String value = st.nextToken();
					sb.append(value);
					if (st.hasMoreTokens() && i < (words - 1))
					{
						sb.append(" "); //$NON-NLS-1$
					}
					i++;
				}
				return sb.toString();
			}
			catch (Exception ex)
			{
				return text.toString();
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns a substring from the original string.
	 *
	 * @sample
	 * //returns 'is a'
	 * var retval = utils.stringMiddleWords('this is a test',2,2);
	 *
	 * @param text to process
	 * @param i_start start word index
	 * @param numberof_words the word count to return
	 * @return the string with number of words form the left and
	 */
	public String js_stringMiddleWords(String text, Number i_start, Number numberof_words)
	{
		if (text != null && i_start != null && numberof_words != null)
		{
			try
			{
				int start = i_start.intValue();
				int words = numberof_words.intValue();
				StringBuilder sb = new StringBuilder();
				StringTokenizer st = new StringTokenizer(text.toString(), " "); //$NON-NLS-1$
				start = start - 1;
				int i = 0;
				while (st.hasMoreTokens())
				{
					String value = st.nextToken();
					if (i >= (start) && i < (start + words))
					{
						sb.append(value);
					}
					if (st.hasMoreTokens() && i >= (start) && i < (start + words) - 1)
					{
						sb.append(" "); //$NON-NLS-1$
					}
					i++;
				}
				return sb.toString();
			}
			catch (Exception ex)
			{
				return text.toString();
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns the number of words, starting from the right.
	 *
	 * @sample
	 * //returns 'is a test'
	 * var retval = utils.stringRightWords('this is a test',3);
	 *
	 * @param text to process
	 * @param numberof_words to return
	 * @return the string with number of words form the right
	 */
	public String js_stringRightWords(String text, Number numberof_words)
	{
		if (text != null && numberof_words != null)
		{
			try
			{
				int words = numberof_words.intValue();
				StringBuilder sb = new StringBuilder();
				StringTokenizer st = new StringTokenizer(text.toString(), " "); //$NON-NLS-1$
				int i = 0;
				int countwords = st.countTokens();
				while (st.hasMoreTokens())
				{
					String value = st.nextToken();
					if (countwords - words <= i)
					{
						sb.append(value);
						if (st.hasMoreTokens())
						{
							sb.append(" "); //$NON-NLS-1$
						}
					}
					i++;
				}
				return sb.toString();
			}
			catch (Exception ex)
			{
				return text.toString();
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns the number of times searchString appears in textString.
	 *
	 * @sample
	 * //returns 2 as count
	 * var count = utils.stringPatternCount('this is a test','is');
	 *
	 * @param text the text to process
	 * @param toSearchFor the string to search for
	 * @return the occurrenceCount that the search string is found in the text
	 */
	public int js_stringPatternCount(String text, String toSearchFor)
	{
		if (text != null && toSearchFor != null && toSearchFor.length() > 0)
		{
			try
			{
				int length = toSearchFor.length();
				int i = 0;
				int index = text.indexOf(toSearchFor);
				while (index != -1)
				{
					i++;
					index = text.indexOf(toSearchFor, index + length);
				}
				return i;
			}
			catch (Exception ex)
			{
			}
		}
		return -1;
	}

	/**
	 * @deprecated use stringPatternCount(String text, String searchString)
	 */
	@Deprecated
	public int js_stringPatternCount(String text, Object searchString)
	{
		if (searchString instanceof String)
		{
			return js_stringPatternCount(text, (String)searchString);
		}
		if (searchString instanceof Double)
		{
			return js_stringPatternCount(text, ScriptRuntime.numberToString(((Double)searchString).doubleValue(), 10));
		}
		return -1;
	}

	/**
	 * Returns the position of the string to search for, from a certain start position and occurrence.
	 *
	 * @sample
	 * //returns 4 as position
	 * var pos = utils.stringPosition('This is a test','s',1,1)
	 *
	 * @param textString the text to process
	 * @param toSearchFor the string to search
	 * @param i_start the start index to search from
	 * @param i_occurrence the occurrence
	 *
	 * @return the position
	 */
	public int js_stringPosition(String textString, String toSearchFor, Number i_start, Number i_occurrence)
	{
		if (textString != null && toSearchFor != null && i_start != null && i_occurrence != null)
		{
			try
			{
				int start = i_start.intValue() - 1;
				if (start < 0) start = 0;

				int occurrence = i_occurrence.intValue();
				if (occurrence == 0) occurrence = 1;

				String text = textString.toString();

				int length = toSearchFor.length();
				if (text.length() <= start) return -1;

				if (occurrence < 0)
				{
					occurrence = occurrence * -1;
					int i = 0;
					int index = text.lastIndexOf(toSearchFor, start);
					while (index != -1)
					{
						i++;
						if (occurrence == i)
						{
							return index + 1;
						}
						index = text.lastIndexOf(toSearchFor, index);
					}
				}
				else
				{
					int i = 0;
					int index = text.indexOf(toSearchFor, start);
					while (index != -1)
					{
						i++;
						if (occurrence == i)
						{
							return index + 1;
						}
						index = text.indexOf(toSearchFor, index + length);
					}
				}
			}
			catch (Exception ex)
			{
				Debug.error("Error in utils.stringPosition", ex); //$NON-NLS-1$
			}
		}
		return -1;
	}

	/**
	 * @deprecated use stringPosition(String, String, Number, Number)
	 */
	@Deprecated
	public int js_stringPosition(String textString, Object searchString, Number i_start, Number i_occurrence)
	{
		if (searchString instanceof String)
		{
			return js_stringPosition(textString, (String)searchString, i_start, i_occurrence);
		}
		if (searchString instanceof Double)
		{
			return js_stringPosition(textString, ScriptRuntime.numberToString(((Double)searchString).doubleValue(), 10), i_start, i_occurrence);
		}
		return -1;
	}

	/**
	 * Replaces a portion of a string with replacement text from a specified index.
	 *
	 * @sample
	 * //returns 'this was a test'
	 * var retval = utils.stringIndexReplace('this is a test',6,2,'was');
	 *
	 * @param text the text to process
	 * @param i_start the start index to work from
	 * @param i_size the size of the text to replace
	 * @param replacement_text the replacement text
	 * @return the changed text string
	 */
	@SuppressWarnings("nls")
	public String js_stringIndexReplace(String text, Number i_start, Number i_size, String replacement_text)
	{
		if (text != null && i_start != null && i_size != null)
		{
			try
			{
				int start = i_start.intValue();
				int size = i_size.intValue();
				start = start - 1;
				String left = text.substring(0, start);
				String right = text.substring(start + size, text.length());
				return left + String.valueOf(replacement_text) + right;
			}
			catch (Exception ex)
			{
			}
		}
		return text == null ? "" : text;
	}

	/**
	 * @deprecated use stringIndexReplace(String, Number, Number, String)
	 */
	@Deprecated
	public String js_stringIndexReplace(String text, Number i_start, Number i_size, Object replacement_text)
	{
		if (replacement_text instanceof String)
		{
			return js_stringIndexReplace(text, i_start, i_size, (String)replacement_text);
		}
		if (replacement_text instanceof Double)
		{
			return js_stringIndexReplace(text, i_start, i_size, ScriptRuntime.numberToString(((Double)replacement_text).doubleValue(), 10));
		}
		return text == null ? "" : text; //$NON-NLS-1$
	}

	/**
	 * Replaces a portion of a string with replacement text.
	 *
	 * @sample
	 * //returns 'these are cow 1 and cow 2.'
	 * var retval = utils.stringReplace('these are test 1 and test 2.','test','cow');
	 *
	 * @param text the text to process
	 * @param search_text the string to search
	 * @param replacement_text the replacement text
	 *
	 * @return the changed text string
	 */
	@SuppressWarnings("nls")
	public String js_stringReplace(String text, String search_text, String replacement_text)
	{
		if (text != null && search_text != null)
		{
			return Utils.stringReplace(text.toString(), search_text.toString(), String.valueOf(replacement_text));
		}
		return text == null ? "" : text;
	}

	/**
	 * @deprecated use stringReplace(String, String, String)
	 */
	@Deprecated
	public String js_stringReplace(String text, String search_text, Object replacement_text)
	{
		if (replacement_text instanceof String)
		{
			return js_stringReplace(text, search_text, (String)replacement_text);
		}
		if (replacement_text instanceof Double)
		{
			return js_stringReplace(text, search_text, ScriptRuntime.numberToString(((Double)replacement_text).doubleValue(), 10));
		}
		return text == null ? "" : text; //$NON-NLS-1$
	}

	/**
	 * Returns a string with the requested number of characters, starting from the left.
	 *
	 * @sample
	 * //returns 'this i'
	 * var retval = utils.stringLeft('this is a test',6);
	 *
	 * @param text the text to process
	 * @param i_size the size of the text to return
	 * @return the result text string
	 */
	public String js_stringLeft(String text, Number i_size)
	{
		if (text != null && i_size != null)
		{
			try
			{
				return text.substring(0, i_size.intValue());
			}
			catch (Exception ex)
			{
				return text;
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns a substring from the original string.
	 *
	 * @sample
	 * //returns 'his'
	 * var retval = utils.stringMiddle('this is a test',2,3);
	 *
	 * @param text the text to process
	 * @param i_start the start index to work from
	 * @param i_size the size of the text to return
	 * @return the result text string
	 */
	public String js_stringMiddle(String text, Number i_start, Number i_size)
	{
		if (text != null && i_start != null && i_size != null)
		{
			int start = i_start.intValue();
			int length = i_size.intValue();
			try
			{
				start = start - 1;//Filemaker starts counting at 1>>Java at 0
				return text.substring((start), (start + length));
			}
			catch (Exception ex)
			{
				return text.substring((start), text.length());
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Returns a string with the requested number of characters, starting from the right.
	 *
	 * @sample
	 * //returns 'a test'
	 * var retval = utils.stringLeft('this is a test',6);
	 *
	 * @param text the text to process
	 * @param i_size the size of the text to return
	 * @return the result text string
	 */
	public String js_stringRight(String text, Number i_size)
	{
		if (text != null && i_size != null)
		{
			try
			{
				int pos = i_size.intValue();
				return text.substring(text.length() - pos, text.length());
			}
			catch (Exception ex)
			{
				return text;
			}
		}
		else
		{
			return ""; //$NON-NLS-1$
		}
	}

	/**
	 * Filters characters out of from a string and leaves digits, returns the number. Uses locale decimal separator.
	 *
	 * @sample
	 * //returns 65567
	 * var retval = utils.stringToNumber('fg65gf567');
	 *
	 * @param textString the text to process
	 * @return the resulting number
	 */
	public double js_stringToNumber(String textString)
	{
		return js_stringToNumber(textString, null);
	}

	/**
	 * Filters characters out of from a string and leaves digits, returns the number. Decimal separator is specified as parameter.
	 *
	 * @sample
	 * //returns 65.567
	 * var retval = utils.stringToNumber('fg65gf.567','.');
	 *
	 * @param textString the text to process
	 * @param decimalSeparator decimal separator
	 *
	 * @return the resulting number
	 */
	public double js_stringToNumber(String textString, String decimalSeparator)
	{
		if (textString != null)
		{
			if (decimalSeparator == null)
			{
				DecimalFormatSymbols dfs = RoundHalfUpDecimalFormat.getDecimalFormatSymbols(application.getLocale());
				decimalSeparator = String.valueOf(dfs.getDecimalSeparator());
			}
			int flag = 0;
			StringBuilder sb = new StringBuilder();
			char[] array = textString.toCharArray();
			for (char element : array)
			{
				Character c = new Character(element);
				String cc = c.toString();
				if (Character.isDigit(element))
				{
					sb.append(element);
				}
				else if (cc != null && cc.equals(decimalSeparator) && flag == 0)
				{
					sb.append(element);
					flag = 1;
				}
			}
			String textt = sb.toString();
			try
			{
				return Utils.getAsDouble(textt);
			}
			catch (Exception ex)
			{
			}
		}
		return 0;
	}

	/**
	 * @deprecated use stringToNumber(String)
	 */
	@Deprecated
	public double js_stringToNumber(Object textString)
	{
		if (textString instanceof Number) return ((Number)textString).doubleValue();
		if (textString != null)
		{
			return js_stringToNumber(textString.toString());
		}

		return 0;
	}

	/**
	 * Returns the PBKDF2 hash for specified text. This method is preferred above the old MD5 hash for enhanced security.
	 * It uses a default of 9999 iterations. The string that is returned can only be used in the utils.validatePBKDF2Hash(password,thisReturnValue)
	 * to check if this hash is a result of that password.
	 * This will always be false: utils.stringPBKDF2Hash("test") == utils.stringPBKDF2Hash("test"). Because for the same string in multiply calls it will not generate the same hash.
	 * So you can only check it like this: utils.validatePBKDF2Hash("test",utils.stringPBKDF2Hash("test"))
	 *
	 * NOTE: PBKDF2 is the key hash function for the PKCS (Public-Key Cryptography) standard, for more info see: http://en.wikipedia.org/wiki/PBKDF2
	 * @sample var hashed_password = utils.stringPBKDF2Hash(user_password)
	 *
	 * @param textString the text to process
	 * @return the resulting hashString
	 */
	public String js_stringPBKDF2Hash(String textString)
	{
		if (textString != null && !"".equals(textString))
		{
			return Utils.calculatePBKDF2(textString.toString(), 9999);
		}
		return null;
	}

	/**
	 * Returns the PBKDF2 hash for specified text. This method is preferred above the old MD5 hash for enhanced security.
	 *
	 * NOTE: PBKDF2 is the key hash function for the PKCS (Public-Key Cryptography) standard, for more info see: http://en.wikipedia.org/wiki/PBKDF2
	 * @sample var hashed_password = utils.stringPBKDF2Hash(user_password,5000)
	 *
	 * @param textString the text to process
	 * @param iterations how many hash iterations should be done, minimum should be 1000 or higher.
	 * @return the resulting hashString
	 */
	public String js_stringPBKDF2Hash(String textString, int iterations)
	{
		if (textString != null && !"".equals(textString)) //$NON-NLS-1$
		{
			return Utils.calculatePBKDF2(textString.toString(), iterations);
		}
		return null;
	}

	/**
	 * Validates the given password against the given hash. The hash should be generated by one of the stringPBKDF2Hash(password [,iteration]) functions. If hash is null or empty string the method will return false.
	 *
	 * NOTE: PBKDF2 is the key hash function for the PKCS (Public-Key Cryptography) standard, for more info see: http://en.wikipedia.org/wiki/PBKDF2
	 * @sample
	 * if (utils.validatePBKDF2Hash(user_password, hashFromDb)) {
	 *    // logged in
	 * }
	 *
	 * @param password the password to test against
	 * @param hash the hash the password needs to validate to.
	 * @return true if his hash is valid for that password
	 */
	public boolean js_validatePBKDF2Hash(String password, String hash)
	{
		if (password != null && !"".equals(password) && hash != null)
		{
			return Utils.validatePBKDF2Hash(password, hash);
		}
		return false;
	}

	/**
	 * Returns the md5 hash (encoded as base64) for specified text.
	 *
	 * NOTE: MD5 (Message-Digest Algorythm 5) is a hash function with a 128-bit hash value, for more info see: http://en.wikipedia.org/wiki/MD5
	 * @sample var hashed_password = utils.stringMD5HashBase64(user_password)
	 *
	 * @param textString the text to process
	 * @return the resulting hashString
	 */
	public String js_stringMD5HashBase64(String textString)
	{
		if (textString != null)
		{
			return Utils.calculateMD5HashBase64(textString.toString());
		}
		return null;
	}

	/**
	 * Returns the md5 hash (encoded as base16) for specified text.
	 *
	 * NOTE: MD5 (Message-Digest Algorythm 5) is a hash function with a 128-bit hash value, for more info see: http://en.wikipedia.org/wiki/MD5
	 * @sample var hashed_password = utils.stringMD5HashBase16(user_password)
	 *
	 * @param textString the text to process
	 * @return the resulting hashString
	 */
	public String js_stringMD5HashBase16(String textString)
	{
		if (textString != null)
		{
			return Utils.calculateMD5HashBase16(textString.toString());
		}
		return null;
	}

	/**
	 * Returns the string without leading or trailing spaces.
	 *
	 * @sample
	 * //returns 'text'
	 * var retval = utils.stringTrim('   text   ');
	 *
	 * @param textString the text to process
	 * @return the resulting trimmed string
	 */
	public String js_stringTrim(String textString)
	{
		if (textString != null)
		{
			return textString.trim();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @deprecated use stringTrim(String)
	 */
	@Deprecated
	public String js_stringTrim(Object textString)
	{
		if (textString != null)
		{
			if (textString instanceof Double)
			{
				return js_stringTrim(ScriptRuntime.numberToString(((Double)textString).doubleValue(), 10));
			}

			return js_stringTrim(textString.toString());
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Format a number to have a defined fraction.
	 *
	 * @sample
	 * var textalNumber = utils.numberFormat(16.749, 2); //returns 16.75
	 *
	 * @param number the number to format
	 * @param digits nr of digits
	 * @return the resulting number in text
	 */
	@JSFunction
	public String numberFormat(Number number, Number digits)
	{
		if (number != null)
		{
			if (digits == null)
			{
				return number.toString();
			}
			return Utils.formatNumber(application.getLocale(), number.doubleValue(), digits.intValue());
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Format a number to specification.
	 *
	 * @sample
	 * var textalNumber2 = utils.numberFormat(100006.749, '#,###.00'); //returns 100,006.75
	 *
	 * @param number the number to format
	 * @param format the format
	 * @return the resulting number in text
	 */
	@JSFunction
	public String numberFormat(Number number, String format)
	{
		if (number != null)
		{
			if (format == null)
			{
				return number.toString();
			}

			return new RoundHalfUpDecimalFormat(format, application.getLocale()).format(number.doubleValue());
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @param number the number to format
	 * @param digitsOrFormat nr of digits or the format
	 *
	 * @deprecated use numberFormat(Number, String) or numberFormat(Number, Number)
	 */
	@Deprecated
	public String js_numberFormat(Object number, Object digitsOrFormat)
	{
		if (number != null)
		{
			if (digitsOrFormat instanceof Number)
			{
				return numberFormat(Double.valueOf(Utils.getAsDouble(number)), (Number)digitsOrFormat);
			}
			if (digitsOrFormat instanceof String)
			{
				return numberFormat(Double.valueOf(Utils.getAsDouble(number)), (String)digitsOrFormat);
			}
			return number.toString();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Formats a string according to format specifiers and arguments.
	 *
	 * @sample
	 * // the  format specifier has the syntax: %[argument_index$][flags][width][.precision]conversion
	 * // argument index is 1$, 2$ ...
	 * // flags is a set of characters that modify the output format
	 * // typical values: '+'(The result will always include a sign), ','(The result will include locale-specific grouping separators)
	 * // width is a non-negative decimal integer indicating the minimum number of characters to be written to the output
	 * // precision is a non-negative decimal integer usually used to restrict the number of characters
	 * // conversion is a character indicating how the argument should be formatted
	 * // typical conversion values: b(boolean), s(string), c(character), d(decimal integer), f(floating number), t(prefix for date and time)
	 * // Date/Time Conversions (used after 't' prefix):
	 * 		// 'H' 	Hour of the day for the 24-hour clock, formatted as two digits with a leading zero as necessary i.e. 00 - 23.
	 * 		// 'I' 	Hour for the 12-hour clock, formatted as two digits with a leading zero as necessary, i.e. 01 - 12.
	 * 		// 'k' 	Hour of the day for the 24-hour clock, i.e. 0 - 23.
	 * 		// 'l' 	Hour for the 12-hour clock, i.e. 1 - 12.
	 * 		// 'M' 	Minute within the hour formatted as two digits with a leading zero as necessary, i.e. 00 - 59.
	 * 		// 'S' 	Seconds within the minute, formatted as two digits with a leading zero as necessary, i.e. 00 - 60 ("60" is a special value required to support leap seconds).
	 * 		// 'L' 	Millisecond within the second formatted as three digits with leading zeros as necessary, i.e. 000 - 999.
	 * 		// 'p' 	Locale-specific morning or afternoon marker in lower case, e.g."am" or "pm". Use of the conversion prefix 'T' forces this output to upper case.
	 * 		// 'z' 	RFC 822 style numeric time zone offset from GMT, e.g. -0800.
	 * 		// 'Z' 	A string representing the abbreviation for the time zone.
	 * 		// 'B' 	Locale-specific full month name, e.g. "January", "February".
	 * 		// 'b' 	Locale-specific abbreviated month name, e.g. "Jan", "Feb".
	 * 		// 'h' 	Same as 'b'.
	 * 		// 'A' 	Locale-specific full name of the day of the week, e.g. "Sunday", "Monday"
	 * 		// 'a' 	Locale-specific short name of the day of the week, e.g. "Sun", "Mon"
	 * 		// 'C' 	Four-digit year divided by 100, formatted as two digits with leading zero as necessary, i.e. 00 - 99
	 * 		// 'Y' 	Year, formatted as at least four digits with leading zeros as necessary, e.g. 0092 equals 92 CE for the Gregorian calendar.
	 * 		// 'y' 	Last two digits of the year, formatted with leading zeros as necessary, i.e. 00 - 99.
	 * 		// 'j' 	Day of year, formatted as three digits with leading zeros as necessary, e.g. 001 - 366 for the Gregorian calendar.
	 * 		// 'm' 	Month, formatted as two digits with leading zeros as necessary, i.e. 01 - 13.
	 * 		// 'd' 	Day of month, formatted as two digits with leading zeros as necessary, i.e. 01 - 31
	 * 		// 'e' 	Day of month, formatted as two digits, i.e. 1 - 31.
	 *
	 * 		// common compositions for date/time conversion
	 * 		// 'R' 	Time formatted for the 24-hour clock as "%tH:%tM"
	 * 		// 'T' 	Time formatted for the 24-hour clock as "%tH:%tM:%tS".
	 * 		// 'r' 	Time formatted for the 12-hour clock as "%tI:%tM:%tS %Tp". The location of the morning or afternoon marker ('%Tp') may be locale-dependent.
	 * 		// 'D' 	Date formatted as "%tm/%td/%ty".
	 * 		// 'F' 	ISO 8601 complete date formatted as "%tY-%tm-%td".
	 * 		// 'c' 	Date and time formatted as "%ta %tb %td %tT %tZ %tY", e.g. "Sun Jul 20 16:17:00 EDT 1969".
	 *
	 * utils.stringFormat('%s Birthday: %2$tm %2$te,%2$tY',new Array('My',new Date(2009,0,1))) // returns My Birthday: 01 1,2009
	 * utils.stringFormat('The time is: %1$tH:%1$tM:%1$tS',new Array(new Date(2009,0,1,12,0,0))) // returns The time is: 12:00:00
	 * utils.stringFormat('My %s: %2$.0f, my float: %2$.2f',new Array('integer',10)) // returns My integer: 10, my float: 10.00
	 * utils.stringFormat('Today is: %1$tc',new Array(new Date())) // returns current date/time as:  Today is: Fri Feb 20 14:15:54 EET 2009
	 * utils.stringFormat('Today is: %tF',new Array(new Date())) // returns current date as: Today is: 2009-02-20
	 *
	 * @param text_to_format the text to format
	 * @param parameters the array with parameters
	 * @return the formatted text
	 */
	public String js_stringFormat(String text_to_format, Object[] parameters)
	{
		if (text_to_format == null) return null;
		if (parameters != null)
		{
			for (int i = 0; i < parameters.length; i++)
			{
				if (parameters[i] instanceof Integer)
				{
					// possible problem
					int j = 0;
					int position = text_to_format.indexOf("%"); //$NON-NLS-1$
					while (j != i && position >= 0)
					{
						j++;
						position = text_to_format.indexOf("%", position + 1); //$NON-NLS-1$
					}
					if (i == j && position >= 0)
					{
						List<Character> conversionsList = Arrays.asList(CONVERSIONS);

						for (int k = position + 1; k < text_to_format.length(); k++)
						{
							if (text_to_format.charAt(k) == 'f')
							{
								parameters[i] = new Float(((Integer)parameters[i]).intValue());
								break;
							}
							if (conversionsList.contains(Character.valueOf(text_to_format.charAt(k))))
							{
								break;
							}
						}
					}
				}
			}
			return new Formatter().format(text_to_format, parameters).toString();
		}
		else return text_to_format;
	}

	private final Character[] CONVERSIONS = new Character[] { Character.valueOf('b'), Character.valueOf('B'), Character.valueOf('h'), Character.valueOf('H'), //
		Character.valueOf('s'), Character.valueOf('S'), Character.valueOf('c'), Character.valueOf('C'), Character.valueOf('d'), Character.valueOf('o'), //
		Character.valueOf('x'), Character.valueOf('X'), Character.valueOf('e'), Character.valueOf('E'), Character.valueOf('g'), Character.valueOf('G'), //
		Character.valueOf('a'), Character.valueOf('A'), Character.valueOf('t'), Character.valueOf('T'), Character.valueOf('n') };

	/**
	 * Returns the number of words in the text string.
	 *
	 * @sample
	 * //returns '4' as result
	 * var retval = utils.stringWordCount('this is a test');
	 *
	 * @param text the text to process
	 * @return the word count
	 */
	public int js_stringWordCount(String text)
	{
		if (text != null)
		{
			try
			{
				StringTokenizer st = new StringTokenizer(text, " \n\r\t"); //$NON-NLS-1$
				return st.countTokens();
			}
			catch (Exception ex)
			{
				return -1;
			}
		}
		else
		{
			return -1;
		}
	}

	/**
	 * Returns the escaped markup text (HTML/XML).
	 *
	 * @sample var escapedText = utils.stringEscapeMarkup('<html><body>escape me</body></html>')
	 *
	 * @param textString the text to process
	 * @return the escaped text
	 */
	public String js_stringEscapeMarkup(String textString)
	{
		return js_stringEscapeMarkup(textString, Boolean.FALSE, Boolean.FALSE);
	}

	/**
	 * @clonedesc js_stringEscapeMarkup(String)
	 * @sampleas js_stringEscapeMarkup(String)
	 *
	 * @param textString the text to process
	 * @param escapeSpaces boolean indicating to escape spaces
	 * @return the escaped text
	 */
	public String js_stringEscapeMarkup(String textString, Boolean escapeSpaces)
	{
		return js_stringEscapeMarkup(textString, escapeSpaces, Boolean.FALSE);
	}

	/**
	 * @clonedesc js_stringEscapeMarkup(String)
	 * @sampleas js_stringEscapeMarkup(String)
	 *
	 * @param textString the text to process
	 * @param escapeSpaces boolean indicating to escape spaces
	 * @param convertToHtmlUnicodeEscapes boolean indicating to use unicode escapes
	 * @return the escaped text
	 */
	public String js_stringEscapeMarkup(String textString, Boolean escapeSpaces, Boolean convertToHtmlUnicodeEscapes)
	{
		boolean _escapeSpaces = Utils.getAsBoolean(escapeSpaces);
		boolean _convertToHtmlUnicodeEscapes = Utils.getAsBoolean(convertToHtmlUnicodeEscapes);
		CharSequence retval = HtmlUtils.escapeMarkup(textString, _escapeSpaces, _convertToHtmlUnicodeEscapes);
		return (retval != null ? retval.toString() : null);
	}

	/**
	 * Returns all words starting with capital chars.
	 *
	 * @sample
	 * //returns 'This Is A Test'
	 * var retval = utils.stringInitCap('This is A test');
	 *
	 * @param text the text to process
	 *
	 * @return the changed text
	 */
	public String js_stringInitCap(String text)
	{
		return Utils.stringInitCap(text);
	}

	@Override
	public String toString()
	{
		return "JavaScript Utils"; //$NON-NLS-1$
	}

	public void destroy()
	{
		this.application = null;
	}
}
