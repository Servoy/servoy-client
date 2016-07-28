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

import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * @author lvostinar, rgansevles
 *
 */
@SuppressWarnings("nls")
public class FormTemplateGenerator
{
	private final Configuration cfg;

	public FormTemplateGenerator(IServoyDataConverterContext context, boolean useControllerProvider, boolean design)
	{
		cfg = new Configuration();

		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "templates"));
		cfg.setObjectWrapper(new FormTemplateObjectWrapper(context, useControllerProvider, design, context != null ? context.getApplication() : null));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	}

	public void generate(Form form, String realName, String templateName, Writer writer) throws IOException
	{
		Template template = cfg.getTemplate(templateName);
		try
		{
			template.process(new Object[] { form, realName }, writer);
		}
		catch (TemplateException e)
		{
			NGClientWebsocketSession.sendInternalError(e);
			Debug.error(e);
		}
	}

	public static boolean isWebcomponentBean(IPersist persist)
	{
		return persist instanceof WebComponent ||
			(persist instanceof Bean && ((Bean)persist).getBeanClassName() != null && ((Bean)persist).getBeanClassName().indexOf('-') > 0);
	}

	public static String getComponentTypeName(IFormElement persist)
	{
		String component_type = getPersistComponentTypeName(persist);
		if (WebComponentSpecProvider.getInstance().getWebComponentSpecification(component_type) == null)
		{
			Debug.error("Component spec for " + persist.getName() + " not found; please check your component spec file(s).");
			return FormElement.ERROR_BEAN;
		}
		return component_type;
	}

	public static WebObjectSpecification getWebObjectSpecification(IFormElement persist)
	{
		String component_type = getPersistComponentTypeName(persist);
		WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(component_type);
		if (spec == null)
		{
			Debug.error("Component spec for " + persist.getName() + " not found; please check your component spec file(s).");
			return WebComponentSpecProvider.getInstance().getWebComponentSpecification(FormElement.ERROR_BEAN);
		}
		return spec;
	}

	public static String getTagName(String componentType)
	{
		return componentType != null ? "data-" + componentType : "data-form";
	}

	private static String getPersistComponentTypeName(IFormElement persist)
	{
		if (persist instanceof IBasicWebComponent && isWebcomponentBean(persist))
		{
			String type = ((IBasicWebComponent)persist).getTypeName();
			return getComponentTypeName(type);
		}
		else
		{
			if (persist instanceof GraphicalComponent)
			{
				if (com.servoy.j2db.component.ComponentFactory.isButton((GraphicalComponent)persist))
				{
					return "servoydefault-button";
				}
				return "servoydefault-label";
			}
			if (persist instanceof Field)
			{
				switch (((Field)persist).getDisplayType())
				{
					case Field.COMBOBOX :
						return "servoydefault-combobox";
					case Field.TEXT_FIELD :
						if (((Field)persist).getValuelistID() > 0)
						{
							return "servoydefault-typeahead";
						}
						else
						{
							return "servoydefault-textfield";
						}
					case Field.RADIOS :
						return isSingleValueComponent(persist) ? "servoydefault-radio" : "servoydefault-radiogroup";
					case Field.CHECKS :
						return isSingleValueComponent(persist) ? "servoydefault-check" : "servoydefault-checkgroup";
					case Field.CALENDAR :
						return "servoydefault-calendar";
					case Field.TYPE_AHEAD :
						return "servoydefault-typeahead";
					case Field.TEXT_AREA :
						return "servoydefault-textarea";
					case Field.PASSWORD :
						return "servoydefault-password";
					case Field.SPINNER :
						return "servoydefault-spinner";
					case Field.LIST_BOX :
					case Field.MULTISELECT_LISTBOX :
						return "servoydefault-listbox";
					case Field.IMAGE_MEDIA :
						return "servoydefault-imagemedia";
					case Field.HTML_AREA :
						if (((Field)persist).getEditable())
						{
							return "servoydefault-htmlarea";
						}
						else
						{
							return "servoydefault-htmlview";
						}
				}
			}
			if (persist instanceof TabPanel)
			{
				int orient = ((TabPanel)persist).getTabOrientation();
				if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return "servoydefault-splitpane";
				return "servoydefault-tabpanel";
			}
			if (persist instanceof Portal)
			{
				return "servoycore-portal";
			}
			if (persist instanceof RectShape)
			{
				return "servoydefault-rectangle";
			}
		}
		return FormElement.ERROR_BEAN;
	}

	public static String getComponentTypeName(String beanClassName)
	{
		if (WebComponentSpecProvider.getInstance().getWebComponentSpecification(beanClassName) == null)
		{
			Debug.error("Component spec for " + beanClassName + " not found; please check your component spec file(s).");
			return FormElement.ERROR_BEAN;
		}
		return beanClassName;
	}

	/**
	 * @return false if the persist has no valuelist or at most one value in the valuelist, true otherwise
	 */
	public static boolean isSingleValueComponent(IFormElement persist)
	{
		Field field = (Field)persist;
		if (field.getValuelistID() > 0)
		{
			try
			{
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				FlattenedSolution fs = new FlattenedSolution((SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(
					((Solution)persist.getRootObject()).getName(), IRepository.SOLUTIONS), new AbstractActiveSolutionHandler(as)
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerRegistry.get().getLocalRepository();
						}
					});

				ValueList valuelist = fs.getValueList(field.getValuelistID());
				return isSingleValue(valuelist);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return true;
	}

	private static boolean isSingleValue(ValueList valuelist)
	{
		if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES &&
			valuelist.getAddEmptyValue() != IValueListConstants.EMPTY_VALUE_ALWAYS && valuelist.getCustomValues() != null &&
			!valuelist.getCustomValues().contains("\n") && !valuelist.getCustomValues().contains("\r"))
		{
			return true;
		}
		return false;
	}

}
