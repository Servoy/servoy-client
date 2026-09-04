package com.servoy.mcp.startup;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;

import com.servoy.mcp.McpServlet;

public class ServiceProvider implements IServicesProvider {
	
	/**
	 * @param context The context that is being configured
	 */
	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(McpServlet.class);
			return set;
		}
		return null;
	}
}
