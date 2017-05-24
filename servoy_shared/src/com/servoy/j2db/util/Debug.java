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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.servoy.base.util.ILogger;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.persistence.Solution;

public class Debug
{
	public static final ILogger LOGGER = new ILogger()
	{
		public void log(String message)
		{
			Debug.log(message);
		}

		public void error(String message, Throwable t)
		{
			Debug.error(message, t);
		}
	};

	private volatile static Logger log = null;

	private volatile static boolean trace = false;

	private volatile static boolean traceClient = false;

	static
	{
		try
		{
			// touch the LogFactory class in the init. (to avoid the webstart bug)
			Class< ? > cls = LoggerFactory.class;
			if (cls != null) cls.getName();
		}
		catch (Throwable t)
		{
		}
	}

	@SuppressWarnings("nls")
	public static void init()
	{
		try
		{
			System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
			System.setProperty("org.apache.commons.logging.simplelog.dateTimeFormat", "yyyy-MM-dd HH:mm");
			System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "warn");

			boolean st = false;
			try
			{
				st = Boolean.getBoolean("STACKTRACE"); //$NON-NLS-1$
			}
			catch (Exception ex)
			{
				//ignore
			}
			boolean STACKTRACE = st;
			if (STACKTRACE) System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");

			boolean t = false;
			try
			{
				if (STACKTRACE)
				{
					t = true;
				}
				else
				{
					t = Boolean.getBoolean("TRACE"); //$NON-NLS-1$
				}
			}
			catch (Exception ex)
			{
				//ignore
			}
			boolean TRACE = t;
			if (TRACE) System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "trace");

			log = LoggerFactory.getLogger(Debug.class);
			trace = log.isTraceEnabled();
		}
		catch (Throwable t)
		{
			System.err.println("Error initializing Debug log class");
			t.printStackTrace();
		}
	}

	public static void toggleTracing()
	{
		if (log == null)
		{
			init();
		}
		trace = !trace;
		traceClient = trace;
		if (traceClient)
		{
			log("tracing enabled"); //$NON-NLS-1$
		}
		else
		{
			log("tracing disabled"); //$NON-NLS-1$
		}
	}

	private static Boolean HASMDC = null;

	@SuppressWarnings("nls")
	private static boolean insertClientInfo(boolean insert)
	{
		if (HASMDC == null)
		{
			try
			{
				Class.forName("org.slf4j.MDC");
				HASMDC = Boolean.TRUE;
			}
			catch (Exception e)
			{
				HASMDC = Boolean.FALSE;
			}
		}
		if (!HASMDC.booleanValue()) return false;

		return insetClientInfoWithMDC(insert);
	}

	/**
	 * @param message
	 * @return
	 */
	@SuppressWarnings("nls")
	private static boolean insetClientInfoWithMDC(boolean insert)
	{
		IServiceProvider serviceProvider = J2DBGlobals.getServiceProvider();
		if (insert && serviceProvider != null)
		{
			Solution solution = serviceProvider.getSolution();
			String clientID = serviceProvider.getClientID();
			if (solution != null && clientID != null)
			{
				if (MDC.get("clientid") == null)
				{
					MDC.put("clientid", clientID);
					MDC.put("solution", solution.getName());
					return true;
				}
			}
			else
			{
				MDC.remove("clientid");
				MDC.remove("solution");
			}
		}
		else
		{
			MDC.remove("clientid");
			MDC.remove("solution");
		}
		return false;
	}

	public static void trace(String message, Throwable throwable)
	{
		if (log == null) return;
		if (traceClient)
		{
			// if trace is enable on client (trace boolean is runtime toggle)
			// then do log instead of trace, because the default
			// jre logging.properties only does INFO.
			log(message, throwable);
		}
		else
		{
			boolean wasInserted = insertClientInfo(true);
			log.trace(message, throwable);
			if (wasInserted) insertClientInfo(false);
		}
	}

	public static void trace(Object s)
	{
		if (log == null) return;
		if (traceClient)
		{
			// if trace is enable on client (trace boolean is runtime toggle)
			// then do log instead of trace, because the default
			// jre logging.properties only does INFO.
			log(s);
		}
		else
		{
			if (s instanceof Throwable)
			{
				boolean wasInserted = insertClientInfo(true);
				log.trace("Throwable", (Throwable)s);
				if (wasInserted) insertClientInfo(false);
			}
			else
			{
				boolean wasInserted = insertClientInfo(true);
				log.trace(String.valueOf(s));
				if (wasInserted) insertClientInfo(false);

			}
		}

//		if (TRACE)
//		{
//			System.out.println(s);
//			if(STACKTRACE && (s instanceof Throwable ))
//			{
//				((Throwable)s).printStackTrace();
//				logStackTraceToWindow((Throwable) s);
//			}
//			else if(s != null && !"".equals(s))
//			{
//				logToWindow(s, TRACE_LEVEL);
//			}
//			System.out.flush();
//
//			if (LOG && logFile != null && s != null && !"".equals(s))
//			{
//				logFile.print(sdf.format(new Date()));
//				logFile.print(" : ");
//				if(s instanceof Throwable )
//				{
//					((Throwable)s).printStackTrace(logFile);
//				}
//				else
//				{
//					logFile.println(s);
//				}
//				logFile.flush();
//			}
//		}
	}

	public static void error(String message, Throwable s)
	{
		initIfFirstTime();
		if (log == null) return;

		boolean wasInserted = insertClientInfo(true);
		Throwable t = s;
		if (!(t instanceof ServoyException) && t.getCause() != null)
		{
			t = t.getCause();
		}
		String scriptStackTrace = getScriptStackTrace(message, s);
		if (t instanceof ServoyException && ((ServoyException)t).getContext() != null)
		{
			log.error(message + scriptStackTrace + "\nContext: " + ((ServoyException)t).getContext(), s);
		}
		if (t instanceof JavaScriptException && ((JavaScriptException)t).getValue() instanceof Scriptable)
		{
			log.error(message + " " + Utils.getScriptableString((Scriptable)((JavaScriptException)t).getValue()) + "\n" + scriptStackTrace);
		}
		else log.error(message + (scriptStackTrace != null ? scriptStackTrace : ""), s);
		if (wasInserted) insertClientInfo(false);
	}

	/**
	 * @param message
	 * @param s
	 * @return
	 */
	private static String getScriptStackTrace(String message, Throwable s)
	{
		String scriptStackTrace = "";
		if (s instanceof RhinoException)
		{
			scriptStackTrace = ((RhinoException)s).getScriptStackTrace();
		}
		if ((scriptStackTrace == null || "".equals(scriptStackTrace)))
		{
			scriptStackTrace = getScriptStacktraceFromContext(message);
		}
		if (scriptStackTrace != null && !"".equals(scriptStackTrace))
		{
			scriptStackTrace += "\n" + scriptStackTrace;
		}
		return scriptStackTrace;
	}

	/**
	 * @param message
	 * @param scriptStackTrace
	 * @return
	 */
	public static String getScriptStacktraceFromContext(String message)
	{
		if (Context.getCurrentContext() != null)
		{
			try
			{
				EcmaError jsError = ScriptRuntime.constructError(message, message);
				return jsError.getScriptStackTrace();
			}
			catch (Exception e)
			{
				// just ignore
			}
		}
		return "";
	}

	public static void error(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		if (s instanceof Throwable)
		{
			error("Throwable", (Throwable)s);
		}
		else
		{
			boolean wasInserted = insertClientInfo(true);
			log.error(String.valueOf(s) + getScriptStacktraceFromContext(String.valueOf(s)));
			if (wasInserted) insertClientInfo(false);

		}
	}

	public static void log(String message, Throwable throwable)
	{
		if (log == null) return;
		if (throwable != null)
		{
			boolean wasInserted = insertClientInfo(true);
			log.warn(message + getScriptStackTrace(message, throwable), throwable);
			if (wasInserted) insertClientInfo(false);
		}
		else
		{
			boolean wasInserted = insertClientInfo(true);
			log.info(message + getScriptStacktraceFromContext(message));
			if (wasInserted) insertClientInfo(false);

		}
	}

	public static void log(Object s)
	{
		if (log == null) return;
		if (s instanceof Throwable)
		{
			boolean wasInserted = insertClientInfo(true);
			Throwable throwable = (Throwable)s;
			log.warn("Throwable" + getScriptStackTrace(throwable.getLocalizedMessage(), (Throwable)s), (Throwable)s);
			if (wasInserted) insertClientInfo(false);
		}
		else
		{
			boolean wasInserted = insertClientInfo(true);
			log.info(String.valueOf(s) + getScriptStacktraceFromContext(String.valueOf(s)));
			if (wasInserted) insertClientInfo(false);
		}
	}

	public static boolean tracing()
	{
		return trace;
	}

	public static void warn(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		boolean wasInserted = insertClientInfo(true);
		log.warn(String.valueOf(s) + getScriptStacktraceFromContext(String.valueOf(s)));
		if (wasInserted) insertClientInfo(false);
	}

	public static void fatal(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		boolean wasInserted = insertClientInfo(true);
		log.error(String.valueOf(s) + getScriptStacktraceFromContext(String.valueOf(s)));
		if (wasInserted) insertClientInfo(false);
	}

	public static void debug(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		boolean wasInserted = insertClientInfo(true);
		log.debug(String.valueOf(s) + getScriptStacktraceFromContext(String.valueOf(s)));
		if (wasInserted) insertClientInfo(false);
	}

	public static boolean isDebugEnabled()
	{
		return log.isDebugEnabled();
	}

	private static void initIfFirstTime()
	{
		if (log == null)
		{
			init();
		}
	}
}
