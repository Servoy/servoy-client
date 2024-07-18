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


import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.servoy.base.persistence.BaseColumn;
import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.BaseQueryTable;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.BroadcastFilter;
import com.servoy.j2db.dataprocessing.IDataServer;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.dataprocessing.ValueFactory.DbIdentValue;
import com.servoy.j2db.dataprocessing.ValueFactory.NullValue;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.query.IQuerySelectValue;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.query.QuerySort;
import com.servoy.j2db.util.AliasKeyMap.ISupportAlias;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.TimezoneUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.keyword.Ident;

/**
 * A database column, this information is not stored inside the repository but recreated each time<br>
 * Only the ColumnInfo is stored in the database
 *
 * @author jblok
 */
public class Column extends BaseColumn implements Serializable, IColumn, ISupportHTMLToolTipText, ISupportAlias<String>
{
	public static final long serialVersionUID = -2730015162348120893L;
	public static final int MAX_SQL_OBJECT_NAME_LENGTH = RepositoryHelper.MAX_SQL_NAME_LENGTH; // max length of table names, column names, etc; 30 seen by oracle, 31 seen by firebird
																								//UPDATE: increase this to 50, oracle supports it now
																								/*
																								 * _____________________________________________________________
																								 * Declaration of attributes
																								 */
	public static final int[] allDefinedTypes = new int[] { TEXT, INTEGER, NUMBER, DATETIME, MEDIA };

	private final ITable table;
	private String plainSQLName;
	private ColumnType columnType; // as returned by current database, columnInfo holds column type as configured by developer
	private boolean existInDB;
	private boolean dbPK = false; // please only use this if column exists in database (use existInDB to find out)
	private String databaseDefaultValue = null;
	private boolean allowNull = true;
	private ColumnInfo columnInfo;

/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	public Column(ITable db, String theSQLName, int type, int length, int scale, boolean existInDB)
	{
		table = db;
		this.plainSQLName = theSQLName;
		this.existInDB = existInDB;
		updateColumnType(type, length, scale);
	}

	public String toHTML()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<b>"); //$NON-NLS-1$
		sb.append(getSQLName());
		sb.append("</b> "); //$NON-NLS-1$
		sb.append(getDisplayTypeString(mapToDefaultType(getType())));
		if (getLength() > 0)
		{
			sb.append(" length: "); //$NON-NLS-1$
			sb.append(getLength());
		}
		if (!getAllowNull())
		{
			sb.append("<br>"); //$NON-NLS-1$
			sb.append("<pre><font color=\"red\">NOT NULL</font></pre>"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	public String getTextualPropertyInfo()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			return ci.getTextualPropertyInfo(false);
		}
		return null;
	}

	public static String getDisplayTypeString(int atype)
	{
		switch (mapToDefaultType(atype))
		{
			case DATETIME :
				return "DATETIME"; //$NON-NLS-1$

			case TEXT :
				return "TEXT"; //$NON-NLS-1$

			case NUMBER :
				return "NUMBER"; //$NON-NLS-1$

			case INTEGER :
				return "INTEGER"; //$NON-NLS-1$

			case MEDIA :
				return "MEDIA"; //$NON-NLS-1$

			default :
				return "UNKNOWN TYPE#" + atype; //$NON-NLS-1$
		}
	}

	public static int mapToDefaultType(BaseColumnType type)
	{
		// see SQLEngine createBufferedDataSet
		if (type.getScale() == 0 && (type.getSqlType() == Types.NUMERIC || type.getSqlType() == Types.DECIMAL))
		{
			return INTEGER;
		}
		return mapToDefaultType(type.getSqlType());
	}

	public static int mapToDefaultType(int atype)
	{
		switch (atype)
		{
			case Types.DATE :
			case Types.TIME :
			case Types.TIMESTAMP :
			case -155 : // MS SQlServer datetimeoffset
			case 11 ://date?? fix for 'odbc-bridge' and 'inet driver'
				return DATETIME;

			case Types.CHAR :
			case Types.NCHAR :
			case Types.VARCHAR :
			case Types.LONGVARCHAR :
			case Types.LONGNVARCHAR :
			case Types.CLOB :
			case Types.NCLOB :
			case Types.ROWID : //nchar fix for 'odbc-bridge' and 'inet driver'
			case Types.NVARCHAR : //nvarchar fix for 'odbc-bridge' and 'inet driver'
			case -10 ://ntext fix for 'odbc-bridge' and 'inet driver'
			case -11 ://UID text fix M$ driver -sql server
			case Types.JAVA_OBJECT : //postgres uuid
				return TEXT;

			case Types.FLOAT :
			case Types.DOUBLE :
			case Types.DECIMAL :
			case Types.REAL :
			case Types.NUMERIC :
				return NUMBER;

			case Types.TINYINT :
			case Types.SMALLINT :
			case Types.INTEGER :
			case Types.BIGINT :
			case Types.BIT :
			case Types.BOOLEAN :
				return INTEGER;

			case Types.VARBINARY :
			case Types.BINARY :
			case Types.LONGVARBINARY :
			case Types.BLOB :
			case Types.SQLXML :
			case Types.NULL :
				return MEDIA;

			case Types.OTHER :
			default :
				return atype;
		}
	}

	public static Object getAsRightType(int type, int flags, Object obj, String format, int length, TimeZone timeZone, boolean throwOnFail, boolean truncate)
	{
		return getAsRightType(new BaseColumnType(type, length, 1/* 0 would make numeric integer, do we want that? */), flags, obj, format, timeZone,
			throwOnFail, truncate);
	}

	public static Object getAsRightType(BaseColumnType type, int flags, Object obj, String format, TimeZone timeZone, boolean throwOnFail, boolean truncate)
	{
		if (obj == null) return null;
		if (obj instanceof DbIdentValue || obj instanceof NullValue) return obj;

		if (format == null) return getAsRightType(type, flags, obj, throwOnFail, truncate);//can't do anything else

		try
		{
			if (obj instanceof String)
			{
				String str = ((String)obj).trim();

				ParsePosition pos = new ParsePosition(0);
				switch (mapToDefaultType(type))
				{
					case DATETIME :
						SimpleDateFormat dformatter = new SimpleDateFormat(format);
						if (timeZone != null)
						{
							dformatter.setTimeZone(timeZone);
						}
						Date date = dformatter.parse(str, pos);
						return getAsRightType(type, flags, date, throwOnFail, false);

					case NUMBER :
						DecimalFormat nformatter = new DecimalFormat(format);
					{
						String pos_prefix = nformatter.getPositivePrefix();
						if (pos_prefix == null) pos_prefix = ""; //$NON-NLS-1$
						String neg_prefix = nformatter.getNegativePrefix();
						if (neg_prefix == null) neg_prefix = "-"; //$NON-NLS-1$
						if (!str.startsWith(pos_prefix) && !str.startsWith(neg_prefix))
						{
							nformatter.setPositivePrefix(""); //$NON-NLS-1$
							nformatter.setNegativePrefix("-"); //$NON-NLS-1$
						}
					}
					{
						String pos_suffix = nformatter.getPositiveSuffix();
						if (pos_suffix == null) pos_suffix = ""; //$NON-NLS-1$
						String neg_suffix = nformatter.getNegativeSuffix();
						if (neg_suffix == null) neg_suffix = ""; //$NON-NLS-1$
						if (!str.endsWith(pos_suffix) && !str.endsWith(neg_suffix))
						{
							nformatter.setPositiveSuffix(""); //$NON-NLS-1$
							nformatter.setNegativeSuffix(""); //$NON-NLS-1$
						}
					}
						return getAsRightType(type, flags, nformatter.parse(str, pos), throwOnFail, false);

					case INTEGER :
						DecimalFormat iformatter = new DecimalFormat(format);
					{
						String pos_prefix = iformatter.getPositivePrefix();
						if (pos_prefix == null) pos_prefix = ""; //$NON-NLS-1$
						String neg_prefix = iformatter.getNegativePrefix();
						if (neg_prefix == null) neg_prefix = "-"; //$NON-NLS-1$
						if (!str.startsWith(pos_prefix) && !str.startsWith(neg_prefix))
						{
							iformatter.setPositivePrefix(""); //$NON-NLS-1$
							iformatter.setNegativePrefix("-"); //$NON-NLS-1$
						}
					}
					{
						String pos_suffix = iformatter.getPositiveSuffix();
						if (pos_suffix == null) pos_suffix = ""; //$NON-NLS-1$
						String neg_suffix = iformatter.getNegativeSuffix();
						if (neg_suffix == null) neg_suffix = ""; //$NON-NLS-1$
						if (!str.endsWith(pos_suffix) && !str.endsWith(neg_suffix))
						{
							iformatter.setPositiveSuffix(""); //$NON-NLS-1$
							iformatter.setNegativeSuffix(""); //$NON-NLS-1$
						}
					}
						return getAsRightType(type, flags, iformatter.parse(str, pos), throwOnFail, false);

					case TEXT :
						if (type.getLength() > 0 && str.length() >= type.getLength())
						{
							obj = str.substring(0, type.getLength());
						}
						return obj;

					case MEDIA :
						if (obj instanceof byte[])
						{
							return obj;
						}
						if (throwOnFail)
						{
							throw new RuntimeException(Messages.getString("servoy.conversion.error.media", new Object[] { obj })); //$NON-NLS-1$
						}
						return null;

					default :
						return obj.toString();
				}
			}
			else
			{
				switch (mapToDefaultType(type))
				{
					case DATETIME :
						if (obj instanceof Date)
						{
							return getAsRightType(type, flags, obj, throwOnFail, false);
						}
						if (obj instanceof Number)
						{
							return getAsRightType(type, flags, new Date(((Number)obj).longValue()), throwOnFail, false);
						}
						return getAsRightType(type, flags, obj.toString(), format, timeZone, throwOnFail, false);

					case NUMBER :
						if (obj instanceof Number)
						{
							return obj;
						}
						return getAsRightType(type, flags, obj.toString(), format, timeZone, throwOnFail, false);

					case INTEGER :
						if (obj instanceof Number)
						{
							if (obj instanceof Integer || obj instanceof Long)
							{
								return obj;
							}
							return new Long(((Number)obj).longValue());
						}
						return getAsRightType(type, flags, obj.toString(), format, timeZone, throwOnFail, false);

					case TEXT :
						String str = obj.toString();
						if (truncate && type.getLength() > 0 && str.length() >= type.getLength())
						{
							str = str.substring(0, type.getLength());
						}
						return str;

					case MEDIA :
						if (obj instanceof byte[])
						{
							return obj;
						}
						if (throwOnFail)
						{
							throw new RuntimeException(Messages.getString("servoy.conversion.error.media", new Object[] { obj })); //$NON-NLS-1$
						}
						return null;

					default :
						return obj.toString();
				}
			}
		}
		catch (RuntimeException e)
		{
			if (throwOnFail) throw e;
			Debug.log(e);
		}
		return null;
	}

	public static Object getAsRightType(int type, int flags, Object obj, int length, boolean throwOnFail, boolean truncate)
	{
		return getAsRightType(new BaseColumnType(type, length, 1/* 0 would make numeric integer, do we want that? */), flags, obj, throwOnFail, truncate);
	}

	public static Object getAsRightType(BaseColumnType type, int flags, Object obj, boolean throwOnFail, boolean truncate)
	{
		if (obj == null) return null;
		if (obj instanceof DbIdentValue || obj instanceof NullValue) return obj;

		if (obj instanceof Object[])
		{
			return stream((Object[])obj).map(el -> getAsRightType(type, flags, el, throwOnFail, truncate)).toArray();
		}

		if ((flags & UUID_COLUMN) != 0 || obj instanceof UUID)
		{
			UUID uuid = Utils.getAsUUID(obj, throwOnFail);
			if (uuid == null)
			{
				return null;
			}
			switch (mapToDefaultType(type.getSqlType()))
			{
				case TEXT :
					return uuid.toString();
				case MEDIA :
					return uuid.toBytes();
			}
		}

		try
		{
			switch (type.getSqlType())
			{
				case Types.NULL : //Type.NULL == 0 means untyped, just return the object as it is
					return obj;

				case Types.DATE :
					if (obj instanceof java.util.Date)
					{
						return new java.sql.Date(((java.util.Date)obj).getTime());
					}
					if (obj instanceof Number)
					{
						return new java.sql.Date(((Number)obj).longValue());
					}
					if (obj instanceof String)
					{
						return new java.sql.Date(getAsTime((String)obj));
					}
					if (throwOnFail)
					{
						throw new RuntimeException(Messages.getString("servoy.conversion.error.date", new Object[] { obj })); //$NON-NLS-1$
					}
					return null;

				case Types.TIME :
					if (obj instanceof java.util.Date)
					{
						return new java.sql.Time(((java.util.Date)obj).getTime());
					}
					if (throwOnFail)
					{
						throw new RuntimeException(Messages.getString("servoy.conversion.error.date", new Object[] { obj })); //$NON-NLS-1$
					}
					return null;
			}

			switch (mapToDefaultType(type))
			{
				case NUMBER :
					if (obj instanceof Double || obj instanceof BigDecimal) return obj;
					if ("".equals(obj)) return null;
					Double retValue = new Double(Utils.getAsDouble(obj, throwOnFail));
					if (obj instanceof Long)
					{
						// long could hold a bigger integer number then a double can hold in its precision/mantissa
						if (((Long)obj).longValue() != retValue.longValue())
						{
							return new BigDecimal(((Long)obj).longValue());
						}
					}
					return retValue;

				case INTEGER :
					if (obj instanceof Integer) return obj;
					if ("".equals(obj)) return null;

					long asLong = Utils.getAsLong(obj, throwOnFail);
					if (asLong >= Integer.MIN_VALUE && asLong <= Integer.MAX_VALUE)
					{
						return new Integer((int)asLong);
					}
					return new Long(asLong);

				case TEXT :
					String str = obj.toString();
					if (truncate && type.getLength() > 0 && str.length() > type.getLength())
					{
						if (Debug.tracing())
						{
							Debug.trace("String trimmed to length: " + type.getLength() + ", " + str); //$NON-NLS-1$ //$NON-NLS-2$
						}
						str = str.substring(0, type.getLength());
					}
					return str;

				case MEDIA :
					if (obj instanceof byte[])
					{
						return obj;
					}
					if (throwOnFail)
					{
						throw new RuntimeException(Messages.getString("servoy.conversion.error.media", new Object[] { obj })); //$NON-NLS-1$
					}
					return null;

				case DATETIME :
					if (obj instanceof org.mozilla.javascript.NativeDate)
					{
						return new Timestamp(((java.util.Date)((org.mozilla.javascript.NativeDate)obj).unwrap()).getTime());
					}
					if (obj instanceof java.util.Date)
					{
						return new Timestamp(((java.util.Date)obj).getTime());
					}
					if (obj instanceof Number)
					{
						return new Timestamp(((Number)obj).longValue());
					}
					if (obj instanceof String)
					{
						return new Timestamp(getAsTime((String)obj));
					}
					if (throwOnFail)
					{
						throw new RuntimeException(Messages.getString("servoy.conversion.error.date", new Object[] { obj })); //$NON-NLS-1$
					}
					return null;

				default :
					return obj.toString();
			}
		}
		catch (RuntimeException e)
		{
			if (throwOnFail) throw e;
			Debug.log(e);
		}
		return null;
	}

	public static Object[] getArrayAsRightType(BaseColumnType type, int flags, Object[] array, boolean throwOnFail, boolean truncate)
	{
		return stream(array).map(obj -> getAsRightType(type, flags, obj, throwOnFail, truncate)).toArray();
	}

	public Object getAsRightType(Object obj, String format)
	{
		return getAsRightType(columnType, getFlags(), obj, format, null, false, true);
	}

	public Object getAsRightType(Object obj)
	{
		return getAsRightType(columnType, getFlags(), obj, false, true);
	}

	public Object getAsRightType(Object obj, boolean throwOnFail, boolean truncate)
	{
		return getAsRightType(columnType, getFlags(), obj, throwOnFail, truncate);
	}

	private static final DateTimeFormatter CUSTOM_FORMATTER = new DateTimeFormatterBuilder() //
		.appendPattern("yyyy") //
		.appendLiteral('-') //
		.appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('-') //
		.appendValue(ChronoField.DAY_OF_MONTH) //
		.appendPattern("[ HH:mm]") // optional sections are surrounded by []
		.appendPattern("['T'HH:mm]") // optional sections are surrounded by []
		.appendPattern("[:s]") // optional sections are surrounded by []
		.appendPattern("[:SSS]") // optional sections are surrounded by []
		.appendPattern("[.SSS]") // optional sections are surrounded by []
		.appendPattern("[Z]") // optional time zone section for +0800
		.optionalStart() //
		.appendOffsetId() // optional time zone section for just "Z" or +08:00
		.optionalEnd() //
		.optionalStart() //
		.appendOffset("+HH", "+00") // support for just hours in timezone
		.optionalEnd() //
		.parseDefaulting(ChronoField.HOUR_OF_DAY, ChronoField.HOUR_OF_DAY.range().getMinimum()) //
		.parseDefaulting(ChronoField.MINUTE_OF_HOUR, ChronoField.MINUTE_OF_HOUR.range().getMinimum()) //
		.parseDefaulting(ChronoField.SECOND_OF_MINUTE, ChronoField.SECOND_OF_MINUTE.range().getMinimum()) //
		.parseDefaulting(ChronoField.OFFSET_SECONDS, -1).toFormatter();

	public static long getAsTime(String date)
	{
		OffsetDateTime parse = OffsetDateTime.parse(date, CUSTOM_FORMATTER);
		if (parse.getOffset().getTotalSeconds() == -1)
		{
			return parse.toLocalDateTime().toInstant(ZoneId.systemDefault().getRules().getOffset(parse.toLocalDateTime())).toEpochMilli();
		}
		else
		{
			return parse.toInstant().toEpochMilli();
		}
	}

	public boolean isAggregate()
	{
		return false;
	}

	public Object getModificationValue(IServiceProvider application) throws RemoteException
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			int autoentertype = ci.getAutoEnterType();
			switch (autoentertype)
			{
				case ColumnInfo.SYSTEM_VALUE_AUTO_ENTER :
					int systemValueType = ci.getAutoEnterSubType();
					switch (systemValueType)
					{
						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_SERVER_DATETIME :
							return new Timestamp(application.getClientHost().getServerTime(application.getClientID()).getTime());

						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_DATETIME :
							return new Timestamp(TimezoneUtils.getClientDate(application).getTime());

						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERNAME :
							return application.getUserName();

						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERUID :
							String user_uid = application.getUserUID();
							if (user_uid == null) user_uid = ""; //$NON-NLS-1$
							switch (getDataProviderType())
							{
								case NUMBER :
									return new Double(Utils.getAsDouble(user_uid));
								case INTEGER :
									return new Integer(Utils.getAsInteger(user_uid));
								case TEXT :
								default :
									return user_uid;
							}

						default :
							return null;
					}
				default :
					return null;
			}
		}
		return null;
	}

	//NOTE also called for duplicate
	public Object getNewRecordValue(IServiceProvider application) throws Exception
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			int autoEnterType = ci.getAutoEnterType();
			int autoEnterSubType = ci.getAutoEnterSubType();
			switch (autoEnterType)
			{
				case ColumnInfo.SYSTEM_VALUE_AUTO_ENTER :
					switch (autoEnterSubType)
					{
						case ColumnInfo.SYSTEM_VALUE_CREATION_SERVER_DATETIME :
							return new Timestamp(application.getClientHost().getServerTime(application.getClientID()).getTime());

						case ColumnInfo.SYSTEM_VALUE_CREATION_DATETIME :
//						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_DATETIME://makes it possible to search for non modified records
							return new Timestamp(TimezoneUtils.getClientDate(application).getTime());

						case ColumnInfo.SYSTEM_VALUE_CREATION_USERNAME :
//						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_NAME://makes it possible to search for non modified records
							return application.getUserName();

						case ColumnInfo.SYSTEM_VALUE_CREATION_USERUID :
//						case ColumnInfo.SYSTEM_VALUE_MODIFICATION_USERID://makes it possible to search for non modified records
						{
							String user_uid = application.getUserUID();
							if (user_uid == null) user_uid = ""; //$NON-NLS-1$
							switch (getDataProviderType())
							{
								case NUMBER :
									return new Double(Utils.getAsDouble(user_uid));
								case INTEGER :
									return new Integer(Utils.getAsInteger(user_uid));
								case TEXT :
								default :
									return user_uid;
							}
						}

						default :
							return null;
					}

				case ColumnInfo.SEQUENCE_AUTO_ENTER :
					if (autoEnterSubType != ColumnInfo.NO_SEQUENCE_SELECTED)
					{
						IDataServer ds = application.getDataServer();
						if (ds != null)
						{
							return ds.getNextSequence(getTable().getServerName(), getTable().getName(), getName(), ci.getID(), getTable().getServerName());
						}
						return Integer.valueOf(0);
					}

					//$FALL-THROUGH$
				case ColumnInfo.CUSTOM_VALUE_AUTO_ENTER :
					String val = ci.getDefaultValue();
					switch (getDataProviderType())
					{
						case NUMBER :
							return new Double(Utils.getAsDouble(val));
						case INTEGER :
							return new Integer(Utils.getAsInteger(val));
						case TEXT :
							return val;
					}
					return val;

//				case ColumnInfo.CALCULATION_VALUE_AUTO_ENTER:
//					return "<todo impl>";

//				case ColumnInfo.LOOKUP_VALUE_AUTO_ENTER:
//					return "<todo impl>";

				case ColumnInfo.NO_AUTO_ENTER :
//					if (!getAllowNull())//not wanted by Servoy developers, they will fillin/use the autoenter they say
//					{
//					switch(mapToDefaultType(type))
//					{
//					case TEXT:
//					return ""; //$NON-NLS-1$
//					case NUMBER:
//					return new Double(0);
//					case INTEGER:
//					return new Integer(0);
//					case DATETIME:
//					return new Date();
//					}
//					}
				default :
					// If we have a simple table filter (one that can also be used as broadcast filter), take the (first) value from it
					// Note that security.setTenantValue() generates such a table filter.
					return application.getFoundSetManager().getTableFilters(getTable().getServerName(), getTable().getName()).stream()
						.map(TableFilter::createBroadcastFilter)
						.filter(Objects::nonNull)
						.filter(bcFilter -> getName().equals(bcFilter.getColumnName()))
						.map(BroadcastFilter::getFilterValue)
						.filter(values -> values != null && values.length > 0)
						.map(values -> values[0])
						.findFirst().orElse(null);
			}
		}
		return null;
	}

	/**
	 * When the sorting is case insensitive, check whether is should be applied to the sql.
	 */
	public static boolean mustApplyCaseinsensitiveModifier(QuerySort querySort, IQuerySelectValue column)
	{
		return querySort.isIgnoreCase() && mapToDefaultType(column.getColumnType()) == TEXT && (column.getFlags() & UUID_COLUMN) == 0;
	}

	/**
	 * Get the column type as defined by the db.
	 */
	public ColumnType getColumnType()
	{
		return columnType;
	}

	@Override
	public int getType()
	{
		return columnType.getSqlType();
	}

	public String getTypeAsString()
	{
		return getDisplayTypeString(getType());
	}

/*
 * _____________________________________________________________ The methods below override methods from superclass <classname>
 */
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Column)
		{
			Column other = (Column)o;
			if ((other.table.equals(table)) && (other.plainSQLName.equalsIgnoreCase(plainSQLName)))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return ((table.hashCode() / 2) + (plainSQLName.hashCode() / 2));
	}

/*
 * _____________________________________________________________ The methods below belong to interface <interfacename>
 */

	@Override
	public String getSQLName()//can be camelcasing
	{
		return plainSQLName;
	}

	public void setSQLName(String name)
	{
		plainSQLName = name;
		hasBadName = null; // clear notify, so checks are run again
		normalizedName = null; // should be recalculated
	}


	private transient String normalizedName = null;//temp var
	private transient String dataProviderID = null;//temp var

	public String getDataProviderID()//get the id
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null && ci.getDataProviderID() != null)
		{
			return ci.getDataProviderID();
		}
		if (dataProviderID != null)
		{
			return dataProviderID;
		}
		return getName();
	}

	public String getAlias()
	{
		return getDataProviderID();
	}

	public void setDataProviderID(String dataProviderID)
	{
		String oldDataProviderID = getDataProviderID();
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			ci.setDataProviderID(getName().equals(dataProviderID) ? null : dataProviderID);
			this.dataProviderID = null;
			ci.flagChanged();
		}
		else
		{
			this.dataProviderID = dataProviderID;
		}
		table.columnDataProviderIDChanged(oldDataProviderID);
	}

	public int getDataProviderType()
	{
		return mapToDefaultType(getType());
	}


	public ColumnWrapper getColumnWrapper()
	{
		return new ColumnWrapper(this);
	}

	public boolean isEditable()
	{
		return getRowIdentType() == NORMAL_COLUMN;
	}

/*
 * _____________________________________________________________ The methods below belong to this class
 */
	@Override
	public int getID()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci == null)
		{
			return -1;
		}
		else
		{
			return ci.getID();
		}
	}

	public ITable getTable()
	{
		return table;
	}

	// Used to update database type and length after table creation to make sure
	// they hibernate dialect's type choice matches out type.
	public void updateColumnType(int type, int length, int scale)
	{
		this.columnType = checkColumnType(ColumnType.getInstance(type, length, scale));
		table.fireIColumnChanged(this);
	}

	public static ColumnType checkColumnType(ColumnType columnType)
	{
		if (columnType == null)
		{
			return null;
		}
		int defType = Column.mapToDefaultType(columnType.getSqlType());
		return ColumnType.getInstance(columnType.getSqlType(), (defType == IColumnTypes.INTEGER || defType == IColumnTypes.DATETIME) ? 0 /* length irrelevant */
			: columnType.getLength(), defType == IColumnTypes.NUMBER ? columnType.getScale() : 0);
	}

	public String getName()
	{
		if (normalizedName == null)
		{
			normalizedName = Ident.generateNormalizedNonKeywordName(plainSQLName);
		}
		return normalizedName;
	}

	public String getTitle()
	{
		ColumnInfo ci = getColumnInfo();
		String title = (ci != null && ci.getTitleText() != null && ci.getTitleText().trim().length() != 0 ? ci.getTitleText() : null);
		if (title != null && title.startsWith("i18n")) //$NON-NLS-1$
		{
			title = J2DBGlobals.getServiceProvider().getI18NMessage(title);
		}
		return (title == null ? getName() : title);
	}

	public void updateName(IValidateName validator, String name) throws RepositoryException
	{
		if (!existInDB)
		{
			String newName = name.substring(0, Math.min(name.length(), MAX_SQL_OBJECT_NAME_LENGTH));//limit name for column info table
			String oldSQLName = plainSQLName;
			try
			{
				table.columnNameChange(validator, oldSQLName, newName);
			}
			catch (RepositoryException e)
			{
				setSQLName(oldSQLName);
				throw e;
			}
		}
	}

	public void updateDataProviderID(IValidateName validator, String dpid) throws RepositoryException
	{
		String ndpid = Ident.generateNormalizedName(dpid);
		Column other = table.getColumn(ndpid);
		if (other != null && other != this)
		{
			throw new RepositoryException("A column on table " + table.getName() + " with name/dataProviderID " + ndpid + " already exists"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		validator.checkName(ndpid, -1, new ValidatorSearchContext(this, IRepository.COLUMNS), false);
		setDataProviderID(ndpid);
		table.fireIColumnChanged(this);
	}

	@Override
	public int getScale()
	{
		return columnType.getScale();
	}

	public int getLength()
	{
		return columnType.getLength();
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public void setExistInDB(boolean b)
	{
		existInDB = b;
	}

	public boolean getExistInDB()
	{
		return existInDB;
	}

	//return if this this column a primary key
	public int getRowIdentType()
	{
		return getFlags() & IDENT_COLUMNS;
	}

	/**
	 * set this column to be a primary key type must be NORMAL_COLUMN,PK_COLUMN and USER_ROW_ID_COLUMN.
	 */
	public void setRowIdentType(int type)
	{
		setFlags(type | (getFlags() & NON_IDENT_COLUMNS));
	}

	public void setFlags(int f)
	{
		if (f < 0)
		{
			// -1 value is only for internal class use; all external setFlags calls should have f >= 0
			Debug.error("Set flags called with " + f + ". This is not a valid value for column flags."); //$NON-NLS-1$//$NON-NLS-2$
			return;
		}

		// dbPK dictates the value of the PK_COLUMN flag and can disable USER_ROWID_COLUMN
		int colIdentFlags;
		if (existInDB)
		{
			if ((f & IDENT_COLUMNS) == USER_ROWID_COLUMN && !dbPK)
			{
				colIdentFlags = USER_ROWID_COLUMN; // only set user row ident if it is not already pk
			}
			else
			{
				colIdentFlags = dbPK ? PK_COLUMN : NORMAL_COLUMN;
			}
		}
		else
		{
			colIdentFlags = f & IDENT_COLUMNS;
		}
		// use computed identity flags combined with other flags from columnInfo
		int newFlags = (f & NON_IDENT_COLUMNS) | colIdentFlags;

		updateTableIdentColumns(newFlags);

		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			if (ci.getFlags() != newFlags)
			{
				ci.setFlags(newFlags);
				ci.flagChanged();
			}
			this.flags = -1; // clear local
		}
		else
		{
//			dbPK = ((newFlags & PK_COLUMN) != 0);
			this.flags = newFlags;
		}
	}

	protected void updateTableIdentColumns(int newFlags)
	{
		if ((newFlags & IDENT_COLUMNS) != NORMAL_COLUMN)
		{
			table.addRowIdentColumn(this);
			if (!existInDB) allowNull = false;
		}
		else
		{
			table.removeRowIdentColumn(this);
		}
	}

	public int getFlags()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci == null)
		{
			if (flags != -1)
			{
				return flags;
			}
			return dbPK ? IBaseColumn.PK_COLUMN : 0;
		}
		return ci.getFlags();
	}

	public void setDatabasePK(boolean pk)
	{
		dbPK = pk;
		if (columnInfo == null && flags == -1)
		{
			updateTableIdentColumns(dbPK ? PK_COLUMN : NORMAL_COLUMN);
		}
		else
		{
			setFlags(getFlags()); // update flags/table ident columns to reflect new dbPK status
		}
	}

	public boolean isDatabasePK()
	{
		if (!existInDB)
		{
			return (getFlags() & PK_COLUMN) != 0;
		}
		else
		{
			return dbPK;
		}
	}

	/**
	 * Set or clear a flag.
	 *
	 * @param flag
	 * @param set
	 */
	public void setFlag(int flag, boolean set)
	{
		setFlags(set ? (getFlags() | flag) : (getFlags() & ~flag));
	}

	public void setDatabaseDefaultValue(String value)
	{
		databaseDefaultValue = value;
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			// Don't flag the column info as changed, since the default value is dynamic (i.e., not saved).
			ci.setDatabaseDefaultValue(value);
		}
	}

	public String getDatabaseDefaultValue()
	{
		ColumnInfo ci = getColumnInfo();
		return ci != null ? ci.getDatabaseDefaultValue() : databaseDefaultValue;
	}

	/**
	 * @return column type as configured by developer, fall back to db type when configured is not available
	 */
	public ColumnType getConfiguredColumnType()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null && ci.getConfiguredColumnType() != null)
		{
			return ci.getConfiguredColumnType();
		}
		// default to db-defined column type
		return columnType;
	}

	public void setColumnInfo(ColumnInfo ci)
	{
		if (ci == null) throw new NullPointerException("Column info cannot be set null"); //$NON-NLS-1$

		String oldDataProviderID = getDataProviderID();

		if (!ci.isStoredPersistently() && getColumnType().getScale() > 0 && ci.getCompatibleColumnTypes() == null) // if this is a default in-memory column info, it's type should be compatible with the actual column type
		{
			// if table definition has a scale, add it to the compatible list, because the default type won't store scale.
			ci.addCompatibleColumnType(getColumnType());
		}

		columnInfo = ci;
		if (sequenceType != ColumnInfo.NO_SEQUENCE_SELECTED) //delegate
		{
			setSequenceType(sequenceType);
			if (databaseSequenceName != null)
			{
				setDatabaseSequenceName(databaseSequenceName);
			}
		}
		if (dataProviderID != null)
		{
			setDataProviderID(dataProviderID);
		}
		if (flags != -1)
		{
			setFlags(flags | ci.getFlags()); // use the flags (meant for the use-case when you want to create a column marked as UUID - before actually creating it in DB, see commit for revision 4340)
		}
		else
		{
			// re-apply column-info flags to column to adjust them if necessary as setFlags() allows
			setFlags(ci.getFlags());
		}

		// The database default value only gets set once, via the column itself and never via the column info.
		// Thus the version in the column is always the correct one and should override anything in column info.
		setDatabaseDefaultValue(databaseDefaultValue);

		table.columnDataProviderIDChanged(oldDataProviderID);
	}

	public void removeColumnInfo()//only called when column is deleted...
	{
		columnInfo = null;
	}

	/**
	 * Is only present after Server.createTableInDB if creating new
	 */
	public ColumnInfo getColumnInfo()
	{
		return columnInfo;
	}

	/**
	 * Returns the allowNull.
	 *
	 * @return boolean
	 */
	public boolean getAllowNull()
	{
		return allowNull;
	}

	/**
	 * Sets the allowNull.
	 *
	 * @param allowNull The allowNull to set
	 */
	public void setAllowNull(boolean allowNull)
	{
		this.allowNull = allowNull;
	}

	/**
	 * Returns the isUUID.
	 *
	 * @return boolean
	 */
	public boolean isUUID()
	{
		return hasFlag(UUID_COLUMN);
	}

	/**
	 * Sets the isUUID.
	 *
	 * @param isUUID The isUUID to set
	 */
	public void setUUIDFlag(boolean isUUID)
	{
		setFlag(UUID_COLUMN, isUUID);
	}

	/**
	 * @return the isNativetype
	 */
	public boolean isNativetype()
	{
		return hasFlag(NATIVE_COLUMN);
	}

	/**
	 * @param isNativetype the isNativetype to set
	 */
	public void setNativetypeFlag(boolean isNativetype)
	{
		setFlag(NATIVE_COLUMN, isNativetype);
	}

	private transient String note;//used to show temp tooltip text when hovering over

	public String getNote()
	{
		ColumnInfo ci = getColumnInfo();
		if (note == null && ci != null)
		{
			// plain text
			return ci.getTextualPropertyInfo(false);
		}
		return note;
	}

	/**
	 * @param string
	 */
	public void setNote(String string)
	{
		note = string;
	}

	public int getSequenceType()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null && ci.getAutoEnterType() == ColumnInfo.SEQUENCE_AUTO_ENTER)
		{
			return ci.getAutoEnterSubType();
		}
		return sequenceType;
	}

	/*
	 * For temp_xxx tables columninfo is not set but identity column may be used
	 */
	public boolean isDBIdentity()
	{
		return getSequenceType() == ColumnInfo.DATABASE_IDENTITY;
	}

	private transient int sequenceType = ColumnInfo.NO_SEQUENCE_SELECTED;
	private transient String databaseSequenceName;

	private transient int flags = -1;
	private transient String nativeTypename;


	public void setSequenceType(int i)
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			ci.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
			ci.setAutoEnterSubType(i);
			ci.flagChanged();
			sequenceType = ColumnInfo.NO_SEQUENCE_SELECTED; //clear local
		}
		else
		{
			sequenceType = i;
		}
	}

	public void setDatabaseSequenceName(String databaseSequenceName)
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null)
		{
			ci.setDatabaseSequenceName(databaseSequenceName);
			ci.flagChanged();
			this.databaseSequenceName = null; // clear local
		}
		else
		{
			this.databaseSequenceName = databaseSequenceName;
		}
	}


	public void setNativeTypename(String nativeTypename)
	{
		this.nativeTypename = nativeTypename;
	}

	public String getNativeTypename()
	{
		return nativeTypename;
	}

	public QueryColumn queryColumn(BaseQueryTable queryTable)
	{
		return new QueryColumn(queryTable, getID(), getSQLName(), getColumnType(), getNativeTypename(), getFlags(), isDBIdentity());
	}

	/**
	 * @param flags
	 */
	public static String getFlagsString(int flags)
	{
		StringBuilder sb = new StringBuilder();
		if ((flags & USER_ROWID_COLUMN) != 0) sb.append(" row_ident"); //$NON-NLS-1$
		if ((flags & PK_COLUMN) != 0) sb.append(" pk"); //$NON-NLS-1$
		if ((flags & UUID_COLUMN) != 0) sb.append(" uuid"); //$NON-NLS-1$
		if ((flags & EXCLUDED_COLUMN) != 0) sb.append(" excluded"); //$NON-NLS-1$
		if ((flags & TENANT_COLUMN) != 0) sb.append(" tenant"); //$NON-NLS-1$
		if ((flags & NATIVE_COLUMN) != 0) sb.append(" native"); //$NON-NLS-1$
		return sb.toString().trim();
	}

	/**
	 * It can happen when developers load existing table/columns from the db, that they contain reserved word for other dbs
	 */
	private transient Boolean hasBadName = null;
	public static final String _SV_ROWID = "_sv_rowid";

	public boolean hasBadNaming(IValidateName validator, boolean isMobile)
	{
		if (hasBadName == null)
		{
			hasBadName = FALSE;
			try
			{
				validator.checkName(getName(), getID(), new ValidatorSearchContext(getTable(), IRepository.COLUMNS), true);
			}
			catch (NamevalidationException e)
			{
				hasBadName = TRUE;
				note = e.getMessages().stream().collect(Collectors.joining());
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
		return hasBadName.booleanValue();
	}

	/**
	 * Determines the length of an object, mainly string and byte[]
	 *
	 * @param value the object
	 * @param type of the object
	 * @return 0 if irrelevant, Integer.MAX_VALUE if it does not know
	 */
	public static int getObjectSize(Object value, int type)
	{
		if (value == null) return 0;//length irrelevant for null values

		switch (mapToDefaultType(type))
		{
			case NUMBER :
			case INTEGER :
			case DATETIME :
				return 0; //irrelevant, db makes it fit

			case TEXT :
				if (value instanceof String)
				{
					return ((String)value).length();
				}
				if (value instanceof UUID)
				{
					return 16;
				}
				break;

			case MEDIA :
				if (value instanceof byte[])
				{
					return ((byte[])value).length;
				}
				if (value instanceof UUID)
				{
					return 16;
				}
				break;
		}
		return Integer.MAX_VALUE;
	}

	public void flagColumnInfoChanged()
	{
		ColumnInfo ci = getColumnInfo();
		if (ci != null) ci.flagChanged();
		if (table != null) table.fireIColumnChanged(this);
	}

	/**
	 * Check if db column type is compatible with external column type (like from import)
	 */
	public static boolean isColumnInfoCompatible(ColumnType dbColumnType, ColumnType externalColumnType, boolean checkLength)
	{
		if (dbColumnType == null && externalColumnType == null)
		{
			return true;
		}
		if (dbColumnType == null)
		{
			return false;
		}
		if (dbColumnType.equals(externalColumnType))
		{
			return true;
		}

		int dbtype = mapToDefaultType(dbColumnType.getSqlType());
		int exttype = mapToDefaultType(externalColumnType.getSqlType());

		if (dbtype == exttype)
		{
			if (checkLength && dbtype == IColumnTypes.TEXT && dbColumnType.getLength() != 0 && externalColumnType.getLength() != 0 &&
				dbColumnType.getLength() != externalColumnType.getLength())
			{
				// different length
				return false;
			}
			return true;
		}

		// different type
		if (dbtype == IColumnTypes.NUMBER && exttype == IColumnTypes.INTEGER)
		{
			return true;
		}

		return false;
	}
}
