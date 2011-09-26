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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;

public class Debug
{
	private volatile static Log log = null;

	private volatile static boolean trace = false;

	private volatile static boolean traceClient = false;

	static
	{
		try
		{
			// touch the LogFactory class in the init. (to avoid the webstart bug)
			Class< ? > cls = LogFactory.class;
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

			log = LogFactory.getLog(Debug.class);
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
	private static Object insertClientInfo(Object message)
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
		if (!HASMDC.booleanValue()) return message;

		return insetClientInfoWithMDC(message);
	}

	/**
	 * @param message
	 * @return
	 */
	@SuppressWarnings("nls")
	private static Object insetClientInfoWithMDC(Object message)
	{
		IServiceProvider serviceProvider = J2DBGlobals.getServiceProvider();
		if (serviceProvider != null && serviceProvider.getSolution() != null)
		{
			MDC.put("clientid", serviceProvider.getClientID());
			MDC.put("solution", serviceProvider.getSolution().getName());
		}
		else
		{
			MDC.remove("clientid");
			MDC.remove("solution");
		}
		return message;
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
			log.trace(insertClientInfo(message), throwable);
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
				log.trace(insertClientInfo("Throwable"), (Throwable)s);
			}
			else
			{
				log.trace(insertClientInfo(s));
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
		log.error(insertClientInfo(message), s);
	}

	public static void error(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		if (s instanceof Throwable)
		{
			log.error(insertClientInfo("Throwable"), (Throwable)s);
		}
		else
		{
			log.error(insertClientInfo(s));
		}
	}

	public static void log(String message, Throwable throwable)
	{
		if (log == null) return;
		if (throwable != null)
		{
			log.warn(insertClientInfo(message), throwable);
		}
		else
		{
			log.info(insertClientInfo(message));
		}
	}

	public static void log(Object s)
	{
		if (log == null) return;
		if (s instanceof Throwable)
		{
			log.warn(insertClientInfo("Throwable"), (Throwable)s);
		}
		else
		{
			log.info(insertClientInfo(s));
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
		log.warn(insertClientInfo(s));
	}

	public static void fatal(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		log.fatal(insertClientInfo(s));
	}

	public static void debug(Object s)
	{
		initIfFirstTime();
		if (log == null) return;
		log.debug(insertClientInfo(s));
	}

	private static void initIfFirstTime()
	{
		if (log == null)
		{
			init();
		}
	}
}
