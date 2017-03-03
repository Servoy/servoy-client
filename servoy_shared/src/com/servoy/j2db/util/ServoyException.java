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
package com.servoy.j2db.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.annotations.JSFunction;

import com.servoy.j2db.Messages;
import com.servoy.j2db.dataprocessing.DataException;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;

/**
 * IMPORTANT: The names are exposed to javascripting do not refactor names!
 * @author jblok
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "ServoyException", scriptingName = "ServoyException")
public class ServoyException extends Exception implements IReturnedTypesProvider, IConstantsObject
{
	private static final long serialVersionUID = 3598145362930457281L;

	// --------------------------------------------
	//db set 1xx
	// --------------------------------------------
	/**
	 * Exception code for UNKNOWN_DATABASE_EXCEPTION.
	 *
	 * This code is used when an unrecognized database exception has occurred.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int UNKNOWN_DATABASE_EXCEPTION = 100;
	/**
	 * Exception code for DATA_INTEGRITY_VIOLATION.
	 *
	 * This code is used when a database exception is recognized as an integrity exception (like constraint violation).
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int DATA_INTEGRITY_VIOLATION = 101;
	/**
	 * Exception code for BAD_SQL_SYNTAX.
	 *
	 * This code is used when a database exception is recognized as an sql syntax error.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int BAD_SQL_SYNTAX = 102;
	/**
	 * Exception code for PERMISSION_DENIED.
	 *
	 * This code is used when a database exception is recognized as a authorization error.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int PERMISSION_DENIED = 103;
	/**
	 * Exception code for DEADLOCK.
	 *
	 * This code is used when a deadlock is detected by the database.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int DEADLOCK = 104;
	/**
	 * Exception code for DATA_ACCESS_RESOURCE_FAILURE.
	 *
	 * This code is used when a database exception received an error accessing storage devices.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int DATA_ACCESS_RESOURCE_FAILURE = 105;
	/**
	 * Exception code for ACQUIRE_LOCK_FAILURE.
	 *
	 * This code is used when a database failed to lock a row or table.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int ACQUIRE_LOCK_FAILURE = 106;
	/**
	 * Exception code for INVALID_RESULTSET_ACCESS.
	 *
	 * This code is used when a data is requested that is not selected in the sql.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int INVALID_RESULTSET_ACCESS = 107;
	/**
	 * Exception code for UNEXPECTED_UPDATE_COUNT.
	 *
	 * This code is used when a data could not be deleted or updated when expected (for example
	 * when a record was deleted outside Servoy and a Servoy client wants to update the record).
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int UNEXPECTED_UPDATE_COUNT = 108;


	// --------------------------------------------
	//application error code should be in 300 range
	// --------------------------------------------
	/**
	 * Exception code for NO_LICENSE.
	 *
	 * This code is used when a client could not be registered with the server because of license limitations.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_LICENSE = 307;
	/**
	 * Exception code for RECORD_LOCKED.
	 *
	 * This code is used when a record could not be updated or deleted because it is locked by another client.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int RECORD_LOCKED = 308;
	/**
	 * Exception code for INVALID_INPUT_FORMAT.
	 *
	 * This code is not used.
	 *
	 * @deprecated This code is not used
	 */
	@Deprecated
	public static final int INVALID_INPUT_FORMAT = 309;
	/**
	 * Exception code for INVALID_INPUT.
	 *
	 * This code is used when the user enters data that could not be validated.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int INVALID_INPUT = 310;
	/**
	 * Exception code for EXECUTE_PROGRAM_FAILED.
	 *
	 * This code is used when an external program was not executed correctly.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int EXECUTE_PROGRAM_FAILED = 311;
	/**
	 * Exception code for INCORRECT_LOGIN.
	 *
	 * This code is used when the user enters invalid credentials.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int INCORRECT_LOGIN = 312;
	/**
	 * Exception code for NO_MODIFY_ACCESS.
	 *
	 * This code is used when a user wants to update data and this is disallowed by security settings.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_MODIFY_ACCESS = 319;
	/**
	 * Exception code for NO_ACCESS.
	 *
	 * This code is used when a user wants to view data and this is disallowed by security settings.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_ACCESS = 320;
	/**
	 * Exception code for NO_DELETE_ACCESS.
	 *
	 * This code is used when a user wants to delete data and this is disallowed by security settings.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_DELETE_ACCESS = 322;
	/**
	 * Exception code for NO_CREATE_ACCESS.
	 *
	 * This code is used when a user wants to create new records and this is disallowed by security settings.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_CREATE_ACCESS = 323;
	/**
	 * Exception code for NO_RELATED_CREATE_ACCESS.
	 *
	 * This code is used when a user wants to create new related records and this is disallowed by security settings.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_RELATED_CREATE_ACCESS = 324;
//	public static final int VALIDATOR_NOT_FOUND = 327;
//	public static final int CONVERTER_NOT_FOUND = 328;
	/**
	 * Exception code for SAVE_FAILED.
	 *
	 * This code is used when a javascript exception occurred during saving data to the database.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int SAVE_FAILED = 330;
	/**
	 * Exception code for NO_PARENT_DELETE_WITH_RELATED_RECORDS.
	 *
	 * This code is used when a record could not be deleted because a non-empty relation exists for the record that does not allow parent delete when having related records.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int NO_PARENT_DELETE_WITH_RELATED_RECORDS = 331;
	/**
	 * Exception code for DELETE_NOT_GRANTED.
	 *
	 * This code is used when a record deletion was rejected by a pre-delete Servoy trigger.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int DELETE_NOT_GRANTED = 332;
	/**
	 * Exception code for MAINTENANCE_MODE.
	 *
	 * This code is used when a client could not be registered with the server because the server is in maintenance mode.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int MAINTENANCE_MODE = 333;
	/**
	 * Exception code for ABSTRACT_FORM.
	 *
	 * This code is used when a form, that cannot be created, is shown (for example, a form without parts).
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int ABSTRACT_FORM = 334;
	/**
	 * Exception code for RECORD_VALIDATION_FAILED.
	 *
	 * This code is used when a record update/insert was rejected by a pre-update/insert Servoy trigger.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int RECORD_VALIDATION_FAILED = 335;
	/**
	 * Exception code for CLIENT_NOT_AUTHORIZED.
	 *
	 * This code is used when an client performs an action that requires the user to be logged in and the user has not logged in yet.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 */
	public static final int CLIENT_NOT_AUTHORIZED = 336;

	/**
	 * Error codes not available from java-script.
	 */
	public static class InternalCodes
	{
		public static final int UNKNOWN_EXCEPTION = 0;
		public static final int INTERNAL_ERROR = 1;

		// --------------------------------------------
		//repository set 2xx
		// --------------------------------------------
		public static final int ERROR_NO_REPOSITORY_IN_DB = 204;
		public static final int ERROR_OLD_REPOSITORY_IN_DB = 205;
		public static final int ERROR_TOO_NEW_REPOSITORY_IN_DB = 206;
		public static final int SERVER_NOT_FOUND = 213;
		public static final int TABLE_NOT_FOUND = 214;
		public static final int COLUMN_NOT_FOUND = 225;
		public static final int PRIMARY_KEY_NOT_FOUND = 221;
		public static final int ERROR_IN_TRANSACTION = 202;
		public static final int NO_TRANSACTION_ACTIVE = 215;
		public static final int INVALID_RMI_SERVER_CONNECTION = 216;
		public static final int CUSTOM_REPOSITORY_ERROR = 217;
		public static final int CHECKSUM_FAILURE = 218;
		public static final int ELEMENT_CHANGED_TYPE = 219; // an element (fixed uuid) changed object type between revisions
		public static final int INVALID_PROPERTY_VALUE = 220;
		public static final int INVALID_EXPORT = 226;
		public static final int CONNECTION_POOL_EXHAUSTED = 227;

		// --------------------------------------------
		//unknown set 4xx
		// --------------------------------------------
		public static final int CONNECTION_LOST = 401;
		public static final int OPERATION_CANCELLED = 403;
		public static final int JS_SCRIPT_ERROR = 410; //only use for js errors which halts the script
		public static final int CLIENT_NOT_REGISTERED = 420;
	}

	private final int errorCode;
	protected final Object[] tagValues;

	private String scriptStackTrace;

	protected String context;

	public ServoyException()
	{
		this(0, null);
	} // for scripting purposes

	public ServoyException(int errorCode)
	{
		this(errorCode, null);
	}

	public ServoyException(int errorCode, Object[] values)
	{
		super();
		this.errorCode = errorCode;
		tagValues = values;
		fillScriptStack();
	}

	@Override
	public synchronized Throwable initCause(Throwable cause)
	{
		Throwable retValue = super.initCause(cause);
		if (cause instanceof RhinoException)
		{
			// if the cause is a RhinoException then reset the scriptStackTrace and recalculate it
			// so that this cause is used to get a full script stack.
			scriptStackTrace = null;
			fillScriptStack();
		}
		return retValue;
	}

	/**
	 * Returns the errorCode.
	 *
	 * @return int
	 */
	public int getErrorCode()
	{
		return errorCode;
	}

	/**
	 * Always true; it makes the distinction between ServoyException and DataException.
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 * @return true.
	 * @deprecated Use "typeof" operator instead.
	 */
	@Deprecated
	public boolean js_isServoyException()
	{
		return true;
	}

	@Override
	public String getMessage()
	{
		switch (errorCode)
		{
			case InternalCodes.CONNECTION_LOST :
				return Messages.getString("servoy.applicationException.connectionLost"); //$NON-NLS-1$

			case RECORD_LOCKED :
				return Messages.getString("servoy.foundSet.recordLocked"); //$NON-NLS-1$

			case InternalCodes.JS_SCRIPT_ERROR :
				return Messages.getString("servoy.applicationException.javascriptError"); //$NON-NLS-1$

			case NO_LICENSE :
				return Messages.getString("servoy.applicationException.noLicense"); //$NON-NLS-1$

			case EXECUTE_PROGRAM_FAILED :
				return Messages.getString("servoy.applicationException.execureProgramFailed"); //$NON-NLS-1$

			case INCORRECT_LOGIN :
				return Messages.getString("servoy.applicationException.incorrectLogin"); //$NON-NLS-1$

			case InternalCodes.SERVER_NOT_FOUND :
				return Messages.getString("servoy.exception.serverNotFound", tagValues); //$NON-NLS-1$

			case InternalCodes.TABLE_NOT_FOUND :
				return Messages.getString("servoy.sqlengine.error.tableMissing", tagValues); //$NON-NLS-1$

			case InternalCodes.COLUMN_NOT_FOUND :
				return Messages.getString("servoy.sqlengine.error.columnMissing", tagValues); //$NON-NLS-1$

			case InternalCodes.PRIMARY_KEY_NOT_FOUND :
				return Messages.getString("servoy.exception.primaryKeyNeeded", tagValues); //$NON-NLS-1$

			case InternalCodes.NO_TRANSACTION_ACTIVE :
				return Messages.getString("servoy.sqlengine.error.noTransactionActive", tagValues); //$NON-NLS-1$

			case InternalCodes.INVALID_RMI_SERVER_CONNECTION :
				return Messages.getString("servoy.exception.invalidServerConnection"); //$NON-NLS-1$

			case InternalCodes.ERROR_NO_REPOSITORY_IN_DB :
				return "No repository found in the database."; //$NON-NLS-1$

			case InternalCodes.ERROR_OLD_REPOSITORY_IN_DB :
				return "Old repository found in the database. Repository version: " + tagValues[0] + ", software version: " + tagValues[1] + //$NON-NLS-1$ //$NON-NLS-2$
					". Upgrade the repository first."; //$NON-NLS-1$

			case InternalCodes.ERROR_TOO_NEW_REPOSITORY_IN_DB :
				return "Repository found in database too new for this software version. Repository version: " + tagValues[0] + ", software version: " + //$NON-NLS-1$ //$NON-NLS-2$
					tagValues[1] + ". Upgrade Servoy first."; //$NON-NLS-1$

			case InternalCodes.CHECKSUM_FAILURE :
				return "Checksum failure"; //$NON-NLS-1$

			case InternalCodes.INVALID_EXPORT :
				return "Invalid export"; //$NON-NLS-1$

			case InternalCodes.CONNECTION_POOL_EXHAUSTED :
				return "Connection pool for server " + tagValues[0] + " exhausted"; //$NON-NLS-1$ //$NON-NLS-2$

			case InternalCodes.ERROR_IN_TRANSACTION :
				return "Error in transaction"; //$NON-NLS-1$

			case InternalCodes.CUSTOM_REPOSITORY_ERROR :
				return tagValues[0] != null ? tagValues[0].toString() : ""; //$NON-NLS-1$

			case NO_MODIFY_ACCESS :
				return Messages.getString("servoy.foundSet.error.noModifyAccess"); //$NON-NLS-1$

			case NO_ACCESS :
				return Messages.getString("servoy.foundSet.error.noAccess"); //$NON-NLS-1$

			case NO_DELETE_ACCESS :
				return Messages.getString("servoy.foundSet.error.noDeleteAccess"); //$NON-NLS-1$

			case NO_CREATE_ACCESS :
				return Messages.getString("servoy.foundSet.error.noCreateAccess"); //$NON-NLS-1$

			case NO_RELATED_CREATE_ACCESS :
				return Messages.getString("servoy.foundset.error.createRelatedRecordsNotAllowed", tagValues); //$NON-NLS-1$

			case RECORD_VALIDATION_FAILED :
				return Messages.getString("servoy.foundset.error.recordValidationFailed", tagValues); //$NON-NLS-1$

			case SAVE_FAILED :
				return Messages.getString("servoy.formPanel.error.saveFormData"); //$NON-NLS-1$

			case NO_PARENT_DELETE_WITH_RELATED_RECORDS :
				return Messages.getString("servoy.foundset.error.noParentDeleteWithRelatedrecords", tagValues); //$NON-NLS-1$

			case DELETE_NOT_GRANTED :
				return Messages.getString("servoy.foundset.error.deleteNotGranted"); //$NON-NLS-1$

			case MAINTENANCE_MODE :
				return Messages.getString("servoy.applicationException.maintenanceMode"); //$NON-NLS-1$

			case ABSTRACT_FORM :
				return Messages.getString("servoy.formPanel.error.cannotShowForm", tagValues); //$NON-NLS-1$

			case InternalCodes.OPERATION_CANCELLED :
				return "Operation cancelled"; //$NON-NLS-1$

			case INVALID_INPUT_FORMAT :
				return Messages.getString("servoy.applicationException.invalidInputFormat", tagValues); //$NON-NLS-1$

			case INVALID_INPUT :
				return Messages.getString("servoy.applicationException.invalidInput") + (getCause() != null ? ", " + getCause().getMessage() : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			case CLIENT_NOT_AUTHORIZED :
				return Messages.getString("servoy.client.notAuthorized"); //$NON-NLS-1$

			case InternalCodes.CLIENT_NOT_REGISTERED :
				return Messages.getString("servoy.sqlengine.error.notRegistered"); //$NON-NLS-1$

			case UNEXPECTED_UPDATE_COUNT :
				return "Update/insert failed, unexpected nr of records affected: expected " + tagValues[0] + ", actual " + tagValues[1]; //$NON-NLS-1$ //$NON-NLS-2$

			default :
			{
				if (errorCode == 0 && getCause() != null)
				{
					return super.getMessage();
				}
				else
				{
					return Messages.getString("servoy.applicationException.errorCode", new Object[] { new Integer(errorCode) }); //$NON-NLS-1$
				}
			}
		}
	}

	public int findErrorCode()
	{
		if (errorCode > 0)
		{
			return errorCode;
		}
		if (getCause() instanceof ServoyException)
		{
			return ((ServoyException)getCause()).findErrorCode();
		}
		return 0;
	}

	public boolean hasErrorCode(int code)
	{
		if (errorCode == code)
		{
			return true;
		}
		if (getCause() instanceof ServoyException)
		{
			return ((ServoyException)getCause()).hasErrorCode(code);
		}
		return false;
	}

	/**
	 * Returns the error code for this ServoyException. Can be one of the constants declared in ServoyException.
	 *
	 * @sample
	 * //this sample script should be attached to onError method handler in the solution settings
	 * application.output('Exception Object: '+ex)
	 * application.output('MSG: '+ex.getMessage())
	 * if (ex instanceof ServoyException)
	 * {
	 * 	/** @type {ServoyException} *&#47;
	 * 	var servoyException = ex;
	 * 	application.output("is a ServoyException")
	 * 	application.output("Errorcode: "+servoyException.getErrorCode())
	 * 	var trace = "";
	 * 	if (ex.getScriptStackTrace) trace = servoyException.getScriptStackTrace();
	 * 	else if (servoyException.getStackTrace)  trace = servoyException.getStackTrace();
	 * 	if (servoyException.getErrorCode() == ServoyException.SAVE_FAILED)
	 * 	{
	 * 		plugins.dialogs.showErrorDialog( 'Error',  'It seems you did not fill in a required field', 'OK');
	 * 		//Get the failed records after a save
	 * 		var array = databaseManager.getFailedRecords()
	 * 		for( var i = 0 ; i < array.length ; i++ )
	 * 		{
	 * 			var record = array[i];
	 * 			application.output(record.exception);
	 * 			if (record.exception instanceof DataException)
	 * 			{
	 * 				/** @type {DataException} *&#47;
	 * 				var dataException = record.exception;
	 * 				application.output('SQL: '+dataException.getSQL())
	 * 				application.output('SQLState: '+dataException.getSQLState())
	 * 				application.output('VendorErrorCode: '+dataException.getVendorErrorCode())
	 * 			}
	 * 		}
	 * 		return false
	 * 	}
	 * }
	 * //if returns false or no return, error is not reported to client; if returns true error is reported
	 * //by default error report means logging the error, in smart client an error dialog will also show up
	 * return true
	 *
	 * @return the error code for this ServoyException. Can be one of the constants declared in ServoyException.
	 */
	public int js_getErrorCode()
	{
		return errorCode;
	}

	/**
	 * Returns the string message for this ServoyException.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 * @return the string message for this ServoyException.
	 */
	public String js_getMessage()
	{
		return getMessage();
	}

	/**
	 * Returns the stack trace for this ServoyException.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 * @return the string stack trace for this ServoyException.
	 */
	public String js_getStackTrace()
	{
		Writer result = new StringWriter();
		this.printStackTrace(new PrintWriter(result));
		return result.toString();
	}

	public String js_getContext()
	{
		return context;
	}

	/**
	 * Returns the script stack trace for this ServoyException if this could be created.
	 *
	 * @sampleas js_getErrorCode()
	 * @see #js_getErrorCode()
	 * @return the string stack trace for this ServoyException.
	 */
	@JSFunction
	public String getScriptStackTrace()
	{
		return scriptStackTrace;
	}

	/**
	 * fills the script stack if not already generated if there is a current script context
	 */
	public void fillScriptStack()
	{
		if (scriptStackTrace == null && Context.getCurrentContext() != null)
		{
			try
			{
				// if the cause is a rhino exception (ecma error or javascript exception) then use that scriptstack trace.
				if (getCause() instanceof RhinoException)
				{
					scriptStackTrace = ((RhinoException)getCause()).getScriptStackTrace();
				}
				else
				{
					EcmaError jsError = ScriptRuntime.constructError(getMessage(), getMessage());
					scriptStackTrace = jsError.getScriptStackTrace();
				}
			}
			catch (Exception e)
			{
				// just ignore
			}
		}
	}

	/**
	 * @see com.servoy.j2db.scripting.IScriptObject#getAllReturnedTypes()
	 */
	public Class< ? >[] getAllReturnedTypes()
	{
		return new Class[] { DataException.class };
	}

	/**
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString()
	{
		if (errorCode == 0)
		{
			return "ServoyException"; //$NON-NLS-1$
		}
		return super.toString();
	}

	public ServoyException setContext(String context)
	{
		this.context = context;
		return this;
	}

	public String getContext()
	{
		return this.context;
	}
}
