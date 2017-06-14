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
package com.servoy.j2db.dataprocessing;

import java.sql.SQLException;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.util.ServoyException;

/**
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class DataException extends ServoyException
{
	private String msg;
	private String sql;

	private String sqlState;
	private int vendorErrorCode;
	private Object[] parameters;

	// Only used in scripting
	public DataException()
	{
		super(0);
	}

	public DataException(int errorCode, SQLException ex, String sql)
	{
		super(errorCode);
		msg = getChainedMessage(ex);
		sqlState = ex.getSQLState();
		vendorErrorCode = ex.getErrorCode();
		this.sql = sql;
	}

	public DataException(int errorCode, Object value)
	{
		super(errorCode, new Object[] { value });
	}

	public DataException(int errorCode, Object value, Exception cause)
	{
		this(errorCode, value);
		initCause(cause);
	}

	private static String getChainedMessage(SQLException ex)
	{
		SQLException e = ex.getNextException();
		StringBuilder sb = new StringBuilder(String.valueOf(ex.getMessage()));
		while (e != null)
		{
			sb.append("\nNextException: SQL Error: ").append(e.getErrorCode()) //
				.append(", SQLState: ").append(e.getSQLState()) //
				.append(", Message: ").append(e.getMessage());
			e = e.getNextException();
		}
		return sb.toString();
	}


	// THIS METHOD IS REMOVED FROM InstanceJavaMethod WITH A HACK to keep compatibility with old ways :) - when ServoyException was not using js_...
	/**
	 * @sameas com.servoy.j2db.util.ServoyException#js_isServoyException()
	 *
	 * @deprecated Use "typeof" operator instead.
	 */
	@Deprecated
	@Override
	public boolean js_isServoyException()
	{
		return false;
	}

	/**
	 * This method will always return true; it makes the distinction between DataException and ServoyException.
	 *
	 * @deprecated Use "typeof" operator instead.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception.isDataException)
	 * {
	 * 	application.output("SQL: " + record.exception.getSQL());
	 * 	application.output("SQLState: " + record.exception.getSQLState());
	 * 	application.output("VendorErrorCode: " + record.exception.getVendorErrorCode());
	 * }
	 * @return true.

	 */
	@Deprecated
	public boolean js_isDataException()
	{
		return true;
	}


	@Override
	public String getMessage()
	{
		return super.getMessage() + '\n' + msg;
	}

	public String getSQL()
	{
		return sql;
	}

	public String getSQLState()
	{
		return this.sqlState;
	}

	public int getVendorErrorCode()
	{
		return this.vendorErrorCode;
	}

	/**
	 * Returns the SQL query that caused this DataException.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception instanceof DataException)
	 * {
	 * 	application.output("SQL: " + record.exception.getSQL());
	 * }
	 * @return the SQL query that caused this DataException.
	 */
	public String js_getSQL()
	{
		return getSQL();
	}

	/**
	 * Returns the value for this DataException.
	 * The value is the object thrown in table pre-insert, pre-update or pre-delete triggers.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception instanceof DataException)
	 * {
	 * 	application.output("VALUE: " + record.exception.getValue());
	 * }
	 * @return the value for this DataException.
	 */
	public Object js_getValue()
	{
		return tagValues == null || tagValues.length == 0 ? null : tagValues[0];
	}

	/**
	 * Returns the SQLState for this DataException.
	 * This is a "SQLstate" string, which follows either the XOPEN SQLstate conventions or the SQL 99 conventions.
	 * The values of the SQLState string are described in the appropriate spec.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception instanceof DataException)
	 * {
	 * 	application.output("SQLState: " + record.exception.getSQLState());
	 * }
	 * @return the SQLState for this DataException.
	 */
	public String js_getSQLState()
	{
		return getSQLState();
	}

	/**
	 * Returns the error code of the error thrown by the back-end database server.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception instanceof DataException)
	 * {
	 * 	application.output("VendorErrorCode: " + record.exception.getVendorErrorCode());
	 * }
	 * @return the error code of the error thrown by the back-end database server.
	 */
	public int js_getVendorErrorCode()
	{
		return getVendorErrorCode();
	}

	/**
	 * Returns the parameters of the SQL query that caused this DataException in an array.
	 *
	 * @sample
	 * var record = array[i];
	 * application.output(record.exception);
	 * if (record.exception instanceof DataException)
	 * {
	 * 	var param = record.exception.getParameters();
	 * 	for (j = 0; j < param.length; j++)
	 * 	{
	 * 		application.output("SQL Parameter [" + j + "]: " + param[j]);
	 * 	}
	 * }
	 * @return the parameters of the SQL query that caused this DataException in an array.
	 */
	public Object[] js_getParameters()
	{
		return getParameters();
	}

	public Object[] getParameters()
	{
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(Object[] parameters)
	{
		this.parameters = parameters;
	}
}