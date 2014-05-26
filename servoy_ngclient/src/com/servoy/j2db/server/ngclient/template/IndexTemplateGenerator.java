/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.server.ngclient.template;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.WebComponentSpecProvider;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 *  This class will be obsolete when lazy loading of scripts comes into play
 * @author obuligan
 *
 */
@SuppressWarnings("nls")
public class IndexTemplateGenerator
{
	private final Configuration cfg;

	public IndexTemplateGenerator(FlattenedSolution fs, String contextPath)
	{
		cfg = new Configuration();

		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "templates"));
		cfg.setObjectWrapper(new IndexTemplateObjectWrapper(fs, contextPath));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	}

	public void generate(FlattenedSolution fs, String templateName, Writer writer) throws IOException
	{
		Template template = cfg.getTemplate(templateName);
		try
		{
			template.process(fs, writer);
		}
		catch (TemplateException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static class IndexTemplateObjectWrapper extends DefaultObjectWrapper
	{
		private final FlattenedSolution fs;
		private final String contextPath;

		/**
		 * @param fs
		 * @param contextPath 
		 */
		public IndexTemplateObjectWrapper(FlattenedSolution fs, String contextPath)
		{
			this.fs = fs;
			this.contextPath = contextPath;
		}

		@Override
		public TemplateModel wrap(Object obj) throws TemplateModelException
		{
			Object wrapped;
			if (obj instanceof FlattenedSolution)
			{
				wrapped = new FormReferencesWrapper((FlattenedSolution)obj, contextPath);
			}
			else
			{
				wrapped = obj;
			}
			return super.wrap(wrapped);
		}

	}

	public static class FormReferencesWrapper
	{
		private final FlattenedSolution fs;
		private final String contextPath;

		public FormReferencesWrapper(FlattenedSolution fs, String contextPath)
		{
			this.fs = fs;
			this.contextPath = contextPath;
		}

		/** exposed in tpl file */
		public Collection<String> getFormScriptReferences()
		{
			List<String> forms = new ArrayList<>();
			if (Boolean.valueOf(System.getProperty("servoy.generateformscripts", "false")).booleanValue())
			{
				Iterator<Form> it = fs.getForms(false);
				while (it.hasNext())
				{
					Form form = it.next();
					Solution sol = (Solution)form.getAncestor(IRepository.SOLUTIONS);
					forms.add("solutions/" + sol.getName() + "/forms/" + form.getName() + ".js");
				}
			}
			return forms;
		}

		public Collection<String> getComponentReferences()
		{
			List<String> references = new ArrayList<>();
			WebComponentSpecification[] webComponentDescriptions = WebComponentSpecProvider.getInstance().getWebComponentSpecifications();
			for (WebComponentSpecification webComponentSpec : webComponentDescriptions)
			{
				references.add(webComponentSpec.getDefinition());
			}
			return references;
		}

		public Collection<String> getComponentCssReferences()
		{
			List<String> references = new ArrayList<>();
			WebComponentSpecification[] webComponentDescriptions = WebComponentSpecProvider.getInstance().getWebComponentSpecifications();
			for (WebComponentSpecification webComponentSpec : webComponentDescriptions)
			{
				String[] libraries = webComponentSpec.getLibraries();
				for (String lib : libraries)
				{
					if (lib.toLowerCase().endsWith(".css"))
					{
						references.add(lib);
					}
				}
			}
			return references;
		}

		public Collection<String> getComponentJsReferences()
		{
			List<String> references = new ArrayList<>();
			WebComponentSpecification[] webComponentDescriptions = WebComponentSpecProvider.getInstance().getWebComponentSpecifications();
			for (WebComponentSpecification webComponentSpec : webComponentDescriptions)
			{
				String[] libraries = webComponentSpec.getLibraries();
				for (String lib : libraries)
				{
					if (lib.toLowerCase().endsWith(".js"))
					{
						references.add(lib);
					}
				}
			}
			return references;
		}

		public String getContext()
		{
			return contextPath;
		}
	}
}
