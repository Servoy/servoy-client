/*
 * Copyright (C) 2015 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

import ro.isdc.wro.config.factory.PropertyWroConfigurationFactory;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.http.WroFilter;
import ro.isdc.wro.manager.factory.BaseWroManagerFactory;
import ro.isdc.wro.manager.factory.WroManagerFactory;
import ro.isdc.wro.model.WroModel;
import ro.isdc.wro.model.factory.WroModelFactory;
import ro.isdc.wro.model.group.DefaultGroupExtractor;
import ro.isdc.wro.model.group.Group;
import ro.isdc.wro.model.group.GroupExtractor;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.util.ObjectFactory;

/**
 * @author gboros
 *
 */
@WebFilter(urlPatterns = { "/wro/*" })
@SuppressWarnings("nls")
public class NGWroFilter extends WroFilter
{
	public static final String WROFILTER = "wroFilter";
	private boolean isMinimize;

	@Override
	protected void doInit(final FilterConfig config) throws ServletException
	{
		// can be used in servoy.properties file with 'system.property.' prefix);
		boolean enableWebResourceOptimizer = Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.enableWebResourceOptimizer", "false"));
		setEnable(enableWebResourceOptimizer);
		if (enableWebResourceOptimizer) config.getServletContext().setAttribute(WROFILTER, this);
		isMinimize = Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.enableWebResourceOptimizerMinimize", "false"));
	}

	@Override
	protected ObjectFactory<WroConfiguration> newWroConfigurationFactory(final FilterConfig filterConfig)
	{
		Properties p = new Properties();
		p.setProperty("postProcessors", "jsMin,cssMin");
		return new PropertyWroConfigurationFactory(p);
	}

	@Override
	protected WroManagerFactory newWroManagerFactory()
	{
		return new ConfigurationFreeManagerFactory(this);
	}

	class ConfigurationFreeModelFactory implements WroModelFactory
	{
		private final NGWroFilter ngWroFilter;

		ConfigurationFreeModelFactory(NGWroFilter ngWroFilter)
		{
			this.ngWroFilter = ngWroFilter;
		}

		public WroModel create()
		{
			WroModel result = new WroModel();
			for (Group g : ngWroFilter.getContributionsGroups())
			{
				result.addGroup(g);
			}
			return result;
		}

		public void destroy()
		{
		}

	}

	class ConfigurationFreeGroupExtractor implements GroupExtractor
	{

		private final DefaultGroupExtractor defaultExtractor = new DefaultGroupExtractor();

		/**
		 * Everything that follows context path is considered a group name.
		 */
		public String getGroupName(HttpServletRequest request)
		{
			String contextPath = request.getContextPath();
			String uri = getUri(request);
			if (uri.startsWith(contextPath)) uri = uri.substring(contextPath.length());

			return uri;
		}

		private String getUri(HttpServletRequest request)
		{
			// exactly the same implementation as in the default group extractor
			String includeUriPath = (String)request.getAttribute(DefaultGroupExtractor.ATTR_INCLUDE_PATH);
			String uri = request.getRequestURI();
			uri = includeUriPath != null ? includeUriPath : uri;
			return uri;
		}

		public ResourceType getResourceType(HttpServletRequest request)
		{
			return defaultExtractor.getResourceType(request);
		}


		public boolean isMinimized(HttpServletRequest request)
		{
			return defaultExtractor.isMinimized(request);
		}

		public String encodeGroupUrl(String groupName, ResourceType resourceType, boolean minimize)
		{
			return groupName;
		}
	}

	class ConfigurationFreeManagerFactory extends BaseWroManagerFactory
	{
		private final NGWroFilter ngWroFilter;

		ConfigurationFreeManagerFactory(NGWroFilter ngWroFilter)
		{
			this.ngWroFilter = ngWroFilter;
		}

		@Override
		protected GroupExtractor newGroupExtractor()
		{
			return new ConfigurationFreeGroupExtractor();
		}

		@Override
		protected WroModelFactory newModelFactory()
		{
			return new ConfigurationFreeModelFactory(ngWroFilter);
		}
	}

	private final ArrayList<Group> contributionsGroups = new ArrayList<Group>();

	ArrayList<Group> getContributionsGroups()
	{
		return contributionsGroups;
	}

	public String createCSSGroup(String name, List<String> cssContributions)
	{
		return createGroup(name, cssContributions, ResourceType.CSS);
	}

	public String createJSGroup(String name, List<String> jsContributions)
	{
		return createGroup(name, jsContributions, ResourceType.JS);
	}

	private String createGroup(String name, List<String> contributions, ResourceType type)
	{
		Group g = new Group("/" + name);
		for (String s : contributions)
		{
			Resource r = new Resource();
			r.setType(type);
			r.setUri("/" + s);
			r.setMinimize(isMinimize && !s.toLowerCase().endsWith(".min.js"));
			g.addResource(r);
		}
		contributionsGroups.add(g);

		return name;
	}
}
