package com.servoy.j2db.scripting;

import java.util.concurrent.ConcurrentHashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptStackElement;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.server.shared.IPerformanceDataProvider;
import com.servoy.j2db.server.shared.PerformanceData;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IntHashMap;

public class ServoyContextFactory extends ContextFactory
{
	private static final ConcurrentHashMap<Thread, Context> activeContexts = new ConcurrentHashMap<>();
	private static boolean initialized;

	private final IntHashMap<Boolean> features = new IntHashMap<Boolean>(2);

	private static final ContextFactory.Listener contextListener = new ContextFactory.Listener()
	{
		public void contextCreated(Context cx)
		{
			activeContexts.put(Thread.currentThread(), cx);

			IServiceProvider sp = J2DBGlobals.getServiceProvider();
			if (sp instanceof IApplication && sp.isSolutionLoaded())
			{
				IApplication application = (IApplication)sp;
				cx.setApplicationClassLoader(application.getPluginManager().getClassLoader(), false);
				cx.setWrapFactory(new ServoyWrapFactory(application));

				String version = application.getSettings().getProperty("servoy.javascript.version", Integer.toString(Context.VERSION_ES6)); //$NON-NLS-1$

				if (version != null && version.length() > 0)
				{
					try
					{
						cx.setLanguageVersion(Integer.parseInt(version));
					}
					catch (Exception e)
					{
						Debug.error("Error parsing value of 'servoy.javascript.version' property to an integer value: " + version); //$NON-NLS-1$
					}
				}
				PerformanceData performanceData;
				if (cx.getDebugger() == null &&
					(performanceData = application instanceof IPerformanceDataProvider
						? ((IPerformanceDataProvider)application).getPerformanceData() : null) != null)
				{
					cx.setDebugger(new ProfilingDebugger(performanceData, application), null);
				}
			}
		}

		public void contextReleased(Context cx)
		{
			activeContexts.remove(Thread.currentThread());
			cx.setApplicationClassLoader(null, false);
		}
	};

	public static synchronized void init()
	{
		if (!initialized)
		{
			initialized = true;
			ContextFactory.initGlobal(new ServoyContextFactory());
			ContextFactory.getGlobal().addListener(contextListener);
		}
	}

	public static ScriptStackElement[] getScriptStackForThread(Thread thread)
	{
		Context cx = activeContexts.get(thread);
		if (cx != null)
		{
			try
			{
				return cx.getScriptStack();
			}
			catch (Exception e)
			{
				Debug.trace(e);
			}
		}
		return null;
	}

	@Override
	protected boolean hasFeature(Context context, int featureIndex)
	{
		Boolean value = features.get(featureIndex);
		if (value != null) return value.booleanValue();
		if (featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR) return true;
		return super.hasFeature(context, featureIndex);
	}

	public void setFeature(int featureIndex, boolean value)
	{
		features.put(featureIndex, Boolean.valueOf(value));
	}
}
