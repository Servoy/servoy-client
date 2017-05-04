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

package com.servoy.base.dataprocessing;

import java.sql.Types;
import java.util.Date;

import com.servoy.base.persistence.BaseColumn;
import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.persistence.constants.IColumnTypeConstants;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.base.query.IBaseQuerySelectValue;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.query.IQueryFactory;
import com.servoy.base.util.ILogger;


/**
 * Create 'queries' applicable both in mobile and regular clients.
 *
 * @author rgansevles
 *
 */
public class BaseSQLGenerator
{
	private static final int NULLCHECK_NONE = 0;
	private static final int NULLCHECK_NULL = 1;
	private static final int NULLCHECK_NULL_EMPTY = 2;

	public static IBaseSQLCondition parseFindExpression(IQueryFactory queryFactory, Object raw, IBaseQuerySelectValue qCol, BaseQueryTable columnTable,
		int dataProviderType, String formatString, IBaseColumn c, boolean addNullPkNotNullCondition, IValueConverter valueConverter,
		ITypeConverter typeConverter, BaseColumn firstForeignPKColumn, ILogger logger)
	{
		IBaseSQLCondition or = null;

		//filter on the || (=or)
		String[] rawElements = raw instanceof String[] ? (String[])raw : raw.toString().split("\\|\\|"); //$NON-NLS-1$
		for (String element : rawElements)
		{
			String data = element;
			if (!(c instanceof BaseColumn) || ((BaseColumn)c).getType() != Types.CHAR)
			{
				// if char, it fills up with spaces, so don't trim
				data = data.trim();
			}
			if (data.length() == 0) //filter out the zero length strings
			{
				continue;
			}

			try
			{
				// find the format (only applicable for date columns)
				if (dataProviderType == IColumnTypeConstants.DATETIME)
				{
					int pipe_index = data.indexOf('|');
					if (pipe_index != -1)//the format is speced from within javascript '1-1-2003...30-1-2003|dd-MM-yyyy'
					{
						formatString = data.substring(pipe_index + 1);
						data = data.substring(0, pipe_index);
					}
				}

				// find the operators and the modifiers
				boolean isNot = false;
				boolean hash = false;
				int nullCheck = NULLCHECK_NONE;
				int operator = IBaseSQLCondition.EQUALS_OPERATOR;
				String data2 = null; // for between

				boolean parsing = true;
				while (parsing && data.length() > 0)
				{
					char first = data.charAt(0);
					switch (first)
					{
						case '!' : // ! negation
							if (data.startsWith("!!")) //$NON-NLS-1$
							{
								parsing = false;
							}
							else
							{
								isNot = true;
							}
							data = data.substring(1);

							break;

						case '#' : // # case insensitive (Text) or day search (Date)
							if (data.startsWith("##")) //$NON-NLS-1$
							{
								parsing = false;
							}
							else
							{
								hash = true;
							}
							data = data.substring(1);
							break;

						case '^' : // ^ or ^= nullchecks
							if (data.startsWith("^^")) //$NON-NLS-1$
							{
								data = data.substring(1);
							}
							else
							{
								if (data.startsWith("^=")) //$NON-NLS-1$
								{
									nullCheck = NULLCHECK_NULL_EMPTY;
								}
								else
								{
									nullCheck = NULLCHECK_NULL;
								}
							}
							parsing = false;
							break;

						default :

							// unary operators
							if (data.startsWith("<=") || data.startsWith("=<")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								operator = IBaseSQLCondition.LTE_OPERATOR;
								data = data.substring(2);
							}
							else if (data.startsWith(">=") || data.startsWith("=>")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								operator = IBaseSQLCondition.GTE_OPERATOR;
								data = data.substring(2);
							}
							else if (data.startsWith("==")) //$NON-NLS-1$
							{
								operator = IBaseSQLCondition.EQUALS_OPERATOR;
								data = data.substring(2);
							}
							else if (data.startsWith("<")) //$NON-NLS-1$
							{
								operator = IBaseSQLCondition.LT_OPERATOR;
								data = data.substring(1);
							}
							else if (data.startsWith(">")) //$NON-NLS-1$
							{
								operator = IBaseSQLCondition.GT_OPERATOR;
								data = data.substring(1);
							}
							else if (data.startsWith("=")) //$NON-NLS-1$
							{
								operator = IBaseSQLCondition.EQUALS_OPERATOR;
								data = data.substring(1);
							}
							else
							{
								// between ?
								int index = data.indexOf("..."); //$NON-NLS-1$
								if (index != -1)
								{
									data2 = data.substring(index + 3);
									data = data.substring(0, index);
									operator = IBaseSQLCondition.BETWEEN_OPERATOR;
								}

								// regular data
								parsing = false;
							}
					}
				}

				IBaseSQLCondition condition = null;

				if (nullCheck != NULLCHECK_NONE)
				{
					// nullchecks
					IBaseSQLCondition compareEmpty = null;
					if (nullCheck == NULLCHECK_NULL_EMPTY)
					{
						switch (dataProviderType)
						{
							case IColumnTypeConstants.INTEGER :
							case IColumnTypeConstants.NUMBER :
								compareEmpty = queryFactory.createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, qCol, Integer.valueOf(0));
								break;

							case IColumnTypeConstants.TEXT :
								compareEmpty = queryFactory.createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, qCol, ""); //$NON-NLS-1$
								break;
						}
					}

					condition = queryFactory.or(compareEmpty, queryFactory.createCompareCondition(IBaseSQLCondition.EQUALS_OPERATOR, qCol, null));
				}

				else if (data.length() > 0)
				{
					// get the operators
					Object value = null;
					Object value2 = null; // for between
					int modifier = 0;

					switch (dataProviderType)
					{
						case IColumnTypeConstants.INTEGER :
						case IColumnTypeConstants.NUMBER :
							Object initialObj = (raw instanceof String || raw instanceof String[]) ? data : raw;
							Object objRightType = typeConverter.getAsRightType(dataProviderType, c.getFlags(), initialObj, formatString, c.getLength(), false);
							// Now get asRightType with RAW and not with the string.
							// Because if it is already a Number then it shouldn't be converted to String and then back
							if (initialObj != null && objRightType == null)
							{
								logger.log("Cannot convert (" + initialObj.getClass() + ") " + initialObj + " to a number/int."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								value = null;
							}
							else
							{
								value = objRightType;
							}

							// parse data2 (between)
							if (data2 != null)
							{
								value2 = typeConverter.getAsRightType(dataProviderType, c.getFlags(), data2, formatString, c.getLength(), false);
								if (value2 == null)
								{
									logger.log("Cannot convert (" + data2.getClass() + ") " + data2 + " to a number/int."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
							}
							break;

						case IColumnTypeConstants.DATETIME :
							// special date parsing
							boolean dateSearch = hash;
							Date date;
							Date tmp = null;
							if (data.equalsIgnoreCase("now")) //$NON-NLS-1$
							{
								date = (Date)typeConverter.getAsRightType(dataProviderType, c.getFlags(), tmp = new Date(), c.getLength(), false);
							}
							else if (data.startsWith("//") || data.equalsIgnoreCase("today")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								date = (Date)typeConverter.getAsRightType(dataProviderType, c.getFlags(), tmp = new Date(), c.getLength(), false);
								dateSearch = true;
							}
							else
							{
								// Now get asRightType with RAW and not with the string.
								// Because if it is already a Date then it shouldn't be converted to String and then back
								Object initialObj1 = ((raw instanceof String || raw instanceof String[]) ? data : raw);
								Object tst = typeConverter.getAsRightType(dataProviderType, c.getFlags(), initialObj1, formatString, c.getLength(), false);
								if (tst == null && initialObj1 != null)
								{
									// Format failed.. Reporting that to the user
									logger.log("Cannot parse " + initialObj1 + " using format " + formatString + '.'); //$NON-NLS-1$ //$NON-NLS-2$
									date = null;
								}
								else
								{
									date = (Date)tst;
								}
							}

							if (dateSearch && date != null)
							{
								if (operator == IBaseSQLCondition.EQUALS_OPERATOR)
								{
									value = getStartOfDay(date, c, typeConverter);
									value2 = getEndOfDay(date, c, typeConverter);
									operator = IBaseSQLCondition.BETWEEN_OPERATOR;
								}
								else if (operator == IBaseSQLCondition.BETWEEN_OPERATOR || operator == IBaseSQLCondition.LT_OPERATOR ||
									operator == IBaseSQLCondition.GTE_OPERATOR)
								{
									value = getStartOfDay(date, c, typeConverter);
								}
								else
								{
									value = getEndOfDay(date, c, typeConverter);
								}
							}
							else
							{
								value = date;
							}

							// parse data2 (between)
							if (data2 != null)
							{
								dateSearch = hash;
								if (data2.equalsIgnoreCase("now")) //$NON-NLS-1$
								{
									date = (Date)typeConverter.getAsRightType(dataProviderType, c.getFlags(), (tmp != null ? tmp : new Date()), c.getLength(),
										false);
								}
								else if (data2.startsWith("//") || data2.equalsIgnoreCase("today")) //$NON-NLS-1$ //$NON-NLS-2$
								{
									date = (Date)typeConverter.getAsRightType(dataProviderType, c.getFlags(), (tmp != null ? tmp : new Date()), c.getLength(),
										false);
									dateSearch = true;
								}
								else
								{
									Object dt = typeConverter.getAsRightType(dataProviderType, c.getFlags(), data2, formatString, c.getLength(), false);
									if (dt instanceof Date)
									{
										date = (Date)dt;
									}
									else
									{
										logger.log("Cannot parse '" + data2 + "' using format " + formatString + '.'); //$NON-NLS-1$ //$NON-NLS-2$
										date = null;
									}
								}

								if (dateSearch && date != null)
								{
									value2 = getEndOfDay(date, c, typeConverter);
								}
								else
								{
									value2 = date;
								}
							}
							break;


						case IColumnTypeConstants.TEXT :
							if (hash)
							{
								modifier |= IBaseSQLCondition.CASEINSENTITIVE_MODIFIER;
							}

							if (operator == IBaseSQLCondition.EQUALS_OPERATOR)
							{
								//count the amount of percents based upon the amount we decide what to do
								char[] chars = data.toCharArray();
								StringBuilder dataBuf = new StringBuilder();
								boolean escapeNext = false;
								for (char d : chars)
								{
									if (!escapeNext && d == '\\')
									{
										escapeNext = true;
									}
									else
									{
										if (!escapeNext && d == '%')
										{
											// found a like operator, use backslash as escape in like,
											// use unmodified value, db will use escape backslash from like expression
											operator = IBaseSQLCondition.LIKE_OPERATOR;
											if (data.indexOf('\\') >= 0)
											{
												value2 = "\\"; // escape char, put escape in sql when seen in string  //$NON-NLS-1$
											}
											break;
										}
										dataBuf.append(d);
										escapeNext = false;
									}
								}
								if (operator == IBaseSQLCondition.EQUALS_OPERATOR)
								{
									data = dataBuf.toString();
								}
								// else escape in db will handle escape. use original data
							}
							else
							{
								value2 = data2;
							}
							value = data;
							break;

						default :
							operator = IBaseSQLCondition.LIKE_OPERATOR;
							value = typeConverter.getAsRightType(dataProviderType, c.getFlags(), data, formatString, c.getLength() + 2, false);//+2 for %...%
					}

					// create the condition
					if (value != null)
					{
						Object operand;
						// for like, value2 may be the escape character
						if (value2 != null && operator == IBaseSQLCondition.BETWEEN_OPERATOR)
						{
							operand = new Object[] { typeConverter.getAsRightType(c.getDataProviderType(), c.getFlags(),
								valueConverter == null ? value : valueConverter.convertFromObject(value), null, c.getLength(),
								false), typeConverter.getAsRightType(c.getDataProviderType(), c.getFlags(),
									valueConverter == null ? value2 : valueConverter.convertFromObject(value2), null, c.getLength(), false) };
						}
						else if (operator == IBaseSQLCondition.LIKE_OPERATOR)
						{
							operand = value2 == null ? value : new Object[] { value, value2 };
						}
						else
						{
							operand = typeConverter.getAsRightType(c.getDataProviderType(), c.getFlags(),
								valueConverter == null ? value : valueConverter.convertFromObject(value), null, c.getLength(), false);
						}
						condition = queryFactory.createCompareCondition(operator | modifier, qCol, operand);
					}
				}

				if (condition != null)
				{
					if (isNot)
					{
						condition = condition.negate();
					}
					else
					{
						// When a search on a related null-value is performed, we have to add a not-null check to the related pk to make sure
						// the left outer join does not cause a match with the null value.
						// Skip this if the search is on the related pk column, the user explicitly wants to find records that have no related record (left outer join)
						if (addNullPkNotNullCondition && nullCheck != NULLCHECK_NONE && (c.getFlags() & IBaseColumn.IDENT_COLUMNS) == 0)
						{
							// in case of composite pk, checking only the first pk column is enough
							condition = queryFactory.and(condition,
								queryFactory.createCompareCondition(IBaseSQLCondition.NOT_OPERATOR,
									queryFactory.createQueryColumn(columnTable, firstForeignPKColumn.getID(), firstForeignPKColumn.getSQLName(),
										firstForeignPKColumn.getType(), firstForeignPKColumn.getLength(), firstForeignPKColumn.getScale(),
										firstForeignPKColumn.getFlags()),
									null));
						}
					}

					or = queryFactory.or(or, condition);
				}
			}
			catch (Exception ex)
			{
				logger.error("Error in parsing find expression '" + element + "'", ex);
			}
		}

		return or;
	}

	private static Object getEndOfDay(Date date, IBaseColumn c, ITypeConverter typeConverter)
	{
		Date d = new Date(((date.getTime() / 1000) * 1000) + 999);
		d.setHours(23);
		d.setMinutes(59);
		d.setSeconds(59);
		return typeConverter.getAsRightType(IColumnTypeConstants.DATETIME, c.getFlags(), d, c.getLength(), false);
	}

	private static Object getStartOfDay(Date date, IBaseColumn c, ITypeConverter typeConverter)
	{
		Date d = new Date((date.getTime() / 1000) * 1000);
		d.setHours(0);
		d.setMinutes(0);
		d.setSeconds(0);
		return typeConverter.getAsRightType(IColumnTypeConstants.DATETIME, c.getFlags(), d, c.getLength(), false);
	}
}
