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

import java.util.List;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

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
import ro.isdc.wro.model.transformer.WildcardExpanderModelTransformer;
import ro.isdc.wro.util.ObjectFactory;
import ro.isdc.wro.util.Transformer;

import com.servoy.j2db.util.Utils;


/**
 * @author gboros
 *
 */
@WebFilter(urlPatterns = { "*.js", "*.css" })
public class NGWroFilter extends WroFilter
{
	@Override
	protected void doInit(final FilterConfig config) throws ServletException
	{
		// can be used in servoy.properties file with 'system.property.' prefix);
		setEnable(Utils.getAsBoolean(System.getProperty("servoy.enableWebResourceOptimizer", "true")));
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
		return new ConfigurationFreeManagerFactory();
	}

	public class ConfigurationFreeModelFactory implements WroModelFactory
	{
		public WroModel create()
		{
			WroModel result = new WroModel();
			result.addGroup(createAllResourcesGroup());
			return result;
		}

		private Group createAllResourcesGroup()
		{
			Resource allJS = new Resource();
			allJS.setType(ResourceType.JS);
			allJS.setUri("/**.js");

			Resource allCSS = new Resource();
			allCSS.setType(ResourceType.CSS);
			allCSS.setUri("/**.css");

			Group group = new Group("fake");
			group.addResource(allJS);
			group.addResource(allCSS);
			return group;
		}

		public void destroy()
		{
		}

	}

	public class ResourcesToGroupsModelTransformer implements Transformer<WroModel>
	{

		public WroModel transform(WroModel input) throws Exception
		{
			WroModel result = new WroModel();
			for (Group group : input.getGroups())
			{
				for (Resource resource : group.getResources())
				{
					Group resourceGroup = toGroup(resource);
					result.addGroup(resourceGroup);
				}
			}
			return result;
		}

		private Group toGroup(Resource resource)
		{
			Group resourceGroup = new Group(resource.getUri());
			resourceGroup.addResource(resource);
			return resourceGroup;
		}

	}

	public class ConfigurationFreeGroupExtractor implements GroupExtractor
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

	public class ConfigurationFreeManagerFactory extends BaseWroManagerFactory
	{

		@Override
		protected GroupExtractor newGroupExtractor()
		{
			return new ConfigurationFreeGroupExtractor();
		}

		@Override
		protected WroModelFactory newModelFactory()
		{
			return new ConfigurationFreeModelFactory();
		}

		@Override
		protected List<Transformer<WroModel>> newModelTransformers()
		{
			//The super class store the list of model transformers in a private
			//property. We need both modify that property and return the list of
			//correct transformers.
			List<Transformer<WroModel>> modelTransformers = super.newModelTransformers();
			if (modelTransformers != null) modelTransformers.clear();
			addModelTransformer(new WildcardExpanderModelTransformer());
			addModelTransformer(new ResourcesToGroupsModelTransformer());
			return super.newModelTransformers();
		}
	}
}
