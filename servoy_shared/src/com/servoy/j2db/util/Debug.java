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

public class Debug
{
	private volatile static Log log = null;

	private volatile static boolean trace = false;

	private volatile static boolean traceClient = false;

	public static void init()
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
			log.trace(message, throwable);
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
				log.trace("Throwable", (Throwable)s);
			}
			else
			{
				log.trace(s);
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
		log.error(message, s);
	}

	public static void error(Object s)
	{
		initIfFirstTime();
		if (s instanceof Throwable)
		{
			log.error("Throwable", (Throwable)s);
		}
		else
		{
			log.error(s);
		}
	}

	public static void log(String message, Throwable throwable)
	{
		if (log == null) return;
		if (throwable != null)
		{
			log.warn(message, throwable);
		}
		else
		{
			log.info(message);
		}
	}

	public static void log(Object s)
	{
		if (log == null) return;
		if (s instanceof Throwable)
		{
			log.warn("Throwable", (Throwable)s);
		}
		else
		{
			log.info(s);
		}
	}

	public static boolean tracing()
	{
		return trace;
	}

	public static void warn(Object s)
	{
		initIfFirstTime();
		log.warn(s);
	}

	public static void fatal(Object s)
	{
		initIfFirstTime();
		log.fatal(s);
	}

	public static void debug(Object s)
	{
		initIfFirstTime();
		log.debug(s);
	}

	private static void initIfFirstTime()
	{
		if (log == null)
		{
			init();
		}
	}
}
