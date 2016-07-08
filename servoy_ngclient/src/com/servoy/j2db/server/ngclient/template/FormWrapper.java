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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;

import org.json.JSONException;
import org.json.JSONStringer;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormReference;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.BodyPortal;
import com.servoy.j2db.server.ngclient.DefaultNavigator;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Utils;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateModelException;

/**
 * Wrapper around form for use in templates.
 *
 * @author rgansevles
 *
 */
public class FormWrapper
{
	private final Form form;
	private final boolean isTableView;
	private final boolean isListView;
	private final boolean useControllerProvider;
	private final String realName;
	private final IServoyDataConverterContext context;
	private final boolean design;
	private final FormTemplateObjectWrapper formTemplateObjectWrapper;

	public FormWrapper(Form form, String realName, boolean useControllerProvider, FormTemplateObjectWrapper formTemplateObjectWrapper,
		IServoyDataConverterContext context, boolean design)
	{
		this.form = form;
		this.realName = realName;
		this.useControllerProvider = useControllerProvider;
		this.formTemplateObjectWrapper = formTemplateObjectWrapper;
		this.context = context;
		this.design = design;
		isTableView = !design && (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
		isListView = !design && (form.getView() == IFormConstants.VIEW_TYPE_LIST || form.getView() == IFormConstants.VIEW_TYPE_LIST_LOCKED);
	}

	public boolean isDesign()
	{
		return design;
	}


	public String getFormCls()
	{
		return form.getStyleClass();
	}

	public String getName()
	{
		return realName == null ? form.getName() : realName;
	}

	public String getRegisterMethod()
	{
		if (useControllerProvider)
		{
			return "controllerProvider.register";
		}
		return "angular.module('servoyApp').controller";
	}

	private Part getBodyPart()
	{
		Part part = null;
		for (Part prt : Utils.iterate(form.getParts()))
		{
			if (prt.getPartType() == Part.BODY)
			{
				part = prt;
				break;
			}
		}
		return part;
	}

	public Collection<Part> getParts()
	{
		List<Part> parts = new ArrayList<>();
		Iterator<Part> it = form.getParts();
		while (it.hasNext())
		{
			Part part = it.next();
			if (!Part.rendersOnlyInPrint(part.getPartType()))
			{
				parts.add(part);
			}
		}
		return parts;
	}

	public Collection<IFormElement> getReferenceForms()
	{
		final List<IFormElement> referenceForms = new ArrayList<>();
		form.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof FormReference && isSecurityVisible(o) && !referenceForms.contains(o))
				{
					referenceForms.add((FormReference)o);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
		return referenceForms;
	}

	public Collection<IFormElement> getBaseComponents()
	{
		List<IFormElement> baseComponents = new ArrayList<>();

		Collection<BaseComponent> excludedComponents = null;

		if ((isListView && !design) || isTableView)
		{
			excludedComponents = getBodyComponents();
		}

		List<IFormElement> persists = form.getFlattenedObjects(Form.FORM_INDEX_COMPARATOR);
		for (IFormElement persist : persists)
		{
			if (isSecurityVisible(persist) && (excludedComponents == null || !excludedComponents.contains(persist))) baseComponents.add(persist);
		}
		baseComponents.addAll(getReferenceForms());
		if ((isListView && !design) || isTableView)
		{
			baseComponents.add(new BodyPortal(form));
		}
		if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			baseComponents.add(DefaultNavigator.INSTANCE);
		}
		return baseComponents;
	}

	public boolean isSecurityVisible(IPersist persist)
	{
		if (context.getApplication() == null) return true;
		int access = context.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID());
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}

	public Collection<BaseComponent> getBodyComponents()
	{
		Part part = getBodyPart();

		List<BaseComponent> baseComponents = new ArrayList<>();
		if (part == null) return baseComponents;

		int startPos = form.getPartStartYPos(part.getID());
		int endPos = part.getHeight();
		List<IFormElement> persists = form.getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		for (IFormElement persist : persists)
		{
			if (persist instanceof GraphicalComponent && isTableView && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			Point location = persist.getLocation();
			if (startPos <= location.y && endPos > location.y)
			{
				if (isSecurityVisible(persist)) baseComponents.add((BaseComponent)persist);
			}
		}
		return baseComponents;
	}

	// called by ftl template
	public String getPropertiesString() throws JSONException, IllegalArgumentException
	{
		Map<String, Object> properties = form.getPropertiesMap(); // a copy of form properties
		if (!properties.containsKey("size")) properties.put("size", form.getSize());
		properties.put("designSize", form.getSize());
		properties.put("addMinSize", !form.isResponsiveLayout() && (form.getView() == IForm.RECORD_VIEW || form.getView() == IForm.LOCKED_RECORD_VIEW) &&
			FormElementHelper.INSTANCE.hasExtraParts(form));
		properties.put("absoluteLayout", !form.isResponsiveLayout());
		if (design && !form.isResponsiveLayout())
		{
			properties.put(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName(),
				Integer.valueOf(ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER + ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER));
		}
		removeUnneededFormProperties(properties);
		if (properties.containsKey(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName()))
		{
			Border border = ComponentFactoryHelper.createBorder((String)properties.get(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName()), false);
			properties.put(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName(), BorderPropertyType.writeBorderToJson(border));
		}
		return JSONUtils.writeDataWithConversions(new JSONStringer().object(), properties, null, null).endObject().toString(); // null types as we don't have a spec file for forms
	}

	public String getContainerSizesString() throws JSONException, IllegalArgumentException, TemplateModelException
	{
		Map<String, Dimension> sizes = new HashMap<String, Dimension>();
		Collection<IFormElement> components = getBaseComponents();
		if (components != null)
		{
			for (IFormElement component : components)
			{
				if (component.getParent() instanceof FormReference && formTemplateObjectWrapper != null)
				{
					Object wrappedComponent = formTemplateObjectWrapper.wrap(component);
					Form designForm = context.getApplication().getFlattenedSolution().getForm(((FormReference)component.getParent()).getContainsFormID());
					if (designForm != null)
					{
						sizes.put(((FormElementContext)(((StringModel)wrappedComponent).getWrappedObject())).getName(), designForm.getSize());
					}
				}
				else if (component.getParent() instanceof Form && component.getParent() != form && formTemplateObjectWrapper != null)
				{
					Iterator<FormReference> it = form.getFormReferences();
					while (it.hasNext())
					{
						FormReference formReference = it.next();
						if (formReference.getContainsFormID() == component.getParent().getID())
						{
							sizes.put(((FormElementContext)(((StringModel)formTemplateObjectWrapper.wrap(component)).getWrappedObject())).getName(),
								((Form)component.getParent()).getSize());
							break;
						}
					}
				}

			}
		}
		return JSONUtils.writeDataWithConversions(new JSONStringer().object(), sizes, null, null).endObject().toString();
	}

	private static void removeUnneededFormProperties(Map<String, Object> properties)
	{
		properties.remove(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_SHOWINMENU.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ENCAPSULATION.getPropertyName());
	}
}
