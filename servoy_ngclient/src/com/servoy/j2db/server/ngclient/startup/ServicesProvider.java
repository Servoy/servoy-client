package com.servoy.j2db.server.ngclient.startup;

import java.util.HashSet;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.server.ngclient.ComponentsModuleGenerator;
import com.servoy.j2db.server.ngclient.MediaResourcesServlet;
import com.servoy.j2db.server.ngclient.NGClientEntryFilter;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;


public class ServicesProvider implements IServicesProvider
{
	@Override
	public void registerServices()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tomcat.starter.IServicesProvider#getAnnotatedClasses(java.lang.String)
	 */
	@Override
	public Set<Class< ? >> getAnnotatedClasses(String context)
	{
		// ng client stuff are only reported for the root context in developer.
		if ("".equals(context))
		{
			HashSet<Class< ? >> set = new HashSet<Class< ? >>();
			set.add(MediaResourcesServlet.class);
			set.add(ComponentsModuleGenerator.class);
			set.add(NGClientEntryFilter.class);
			set.add(WebsocketEndpoint.class);
			set.add(ResourceProvider.class);
			return set;
		}
		return null;
	}
}
