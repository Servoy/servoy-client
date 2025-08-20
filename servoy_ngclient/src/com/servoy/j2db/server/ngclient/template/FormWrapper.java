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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.border.Border;

import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportScrollbars;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.scripting.info.UICONSTANTS;
import com.servoy.j2db.server.ngclient.BodyPortal;
import com.servoy.j2db.server.ngclient.DefaultNavigator;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.IServoyDataConverterContext;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

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
	private Collection<IFormElement> baseComponents;
	private final Map<String, String> formComponentTemplates = new HashMap<>();
	private final Map<String, Dimension> formComponentParentSizes = new HashMap<>();
	private final Map<String, Boolean> formComponentsLayout = new HashMap<>();
	private final Map<String, Boolean> formComponentCSSPositionElementNames = new HashMap<String, Boolean>();
	private final JSONObject runtimeData;

	public FormWrapper(Form form, String realName, boolean useControllerProvider, IServoyDataConverterContext context, boolean design, JSONObject runtimeData)
	{
		this.form = form;
		this.realName = realName;
		this.useControllerProvider = useControllerProvider;
		this.context = context;
		this.design = design;
		this.runtimeData = runtimeData;
		boolean hasBodyPart = context.getSolution().getFlattenedForm(form).hasPart(IPartConstants.BODY);
		isTableView = hasBodyPart && !design && (form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED);
		isListView = hasBodyPart && !design && (form.getView() == IFormConstants.VIEW_TYPE_LIST || form.getView() == IFormConstants.VIEW_TYPE_LIST_LOCKED);
	}

	public boolean isDesign()
	{
		return design;
	}

	public String hasRuntimeData()
	{
		return runtimeData != null ? "true" : "false";
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

	public Map<String, String> getTemplates()
	{
		if (this.baseComponents == null) getBaseComponents();
		return formComponentTemplates;
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

	public Collection<IFormElement> getBaseComponents()
	{
		if (this.baseComponents != null) return this.baseComponents;
		List<IFormElement> components = new ArrayList<>();

		Collection<BaseComponent> excludedComponents = null;

		if ((isListView && !design) || isTableView)
		{
			excludedComponents = getBodyComponents();
		}

		List<IFormElement> persists = form.getFlattenedObjects(Form.FORM_INDEX_COMPARATOR);
		for (IFormElement persist : persists)
		{
			if (isSecurityVisible(persist))
			{
				if ((excludedComponents == null || !excludedComponents.contains(persist))) components.add(persist);
				checkFormComponents(components, FormElementHelper.INSTANCE.getFormElement(persist, context.getSolution(), null, design), new HashSet<String>());
			}
		}
		if ((isListView && !design) || isTableView)
		{
			components.add(new BodyPortal(form));
		}
		if (form.getNavigatorID() == Form.NAVIGATOR_DEFAULT)
		{
			components.add(DefaultNavigator.INSTANCE);
		}

		this.baseComponents = components;
		return components;
	}

	private void checkFormComponents(List<IFormElement> components, FormElement formComponentFormElement, Set<String> recursiveCheck)
	{
		// if it's not a form component then .spec will not contain properties of type FormComponentPropertyType.INSTANCE and nothing will happen below
		WebObjectSpecification spec = formComponentFormElement.getWebComponentSpec();
		if (spec != null)
		{
			Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
			if (properties.size() > 0)
			{
				for (PropertyDescription pd : properties)
				{
					Object config = pd.getConfig();
					boolean isRepeating = config instanceof ComponentTypeConfig && ((ComponentTypeConfig)config).forFoundset != null;
					Object propertyValue = formComponentFormElement.getPropertyValue(pd.getName());
					Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, context.getSolution());
					if (frm == null) continue;
					if (!recursiveCheck.add(frm.getName()))
					{
						Debug.error("recursive reference found between (List)FormComponents: " + recursiveCheck); //$NON-NLS-1$
						continue;
					}
					FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentFormElement, pd, (JSONObject)propertyValue, frm,
						context.getSolution());
					Dimension frmSize = frm.getSize();
					for (FormElement element : cache.getFormComponentElements())
					{
						IFormElement persistOfElement = (IFormElement)element.getPersistIfAvailable();
						if ((!isRepeating || design)) components.add(persistOfElement);
						formComponentParentSizes.put(element.getName(), frmSize);
						if (!frm.isResponsiveLayout())
						{
							formComponentsLayout.put(element.getName(), Boolean.TRUE);
						}
						if (frm.getUseCssPosition())
						{
							String name = element.getDesignId() != null ? element.getDesignId() : element.getName();
							if (!formComponentCSSPositionElementNames.containsKey(name))
							{
								formComponentCSSPositionElementNames.put(name, Boolean.TRUE);
							}
						}
						checkFormComponents(components, element, recursiveCheck);
					}
					formComponentTemplates.put(cache.getHtmlTemplateUUIDForAngular(), cache.getTemplate());
					recursiveCheck.remove(frm.getName());
				}
			}
		}
	}

	public boolean isSecurityVisible(IPersist persist)
	{
		if (context.getApplication() == null) return true;
		int access = context.getApplication().getFlattenedSolution().getSecurityAccess(persist.getUUID(),
			form.getImplicitSecurityNoRights() ? IRepository.IMPLICIT_FORM_NO_ACCESS : IRepository.IMPLICIT_FORM_ACCESS);
		boolean b_visible = ((access & IRepository.VIEWABLE) != 0);
		return b_visible;
	}

	public Collection<BaseComponent> getBodyComponents()
	{
		Part part = getBodyPart();

		List<BaseComponent> baseComponents = new ArrayList<>();
		if (part == null) return baseComponents;

		int startPos = form.getPartStartYPos(part.getUUID().toString());
		int endPos = part.getHeight();
		List<IFormElement> persists = form.getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
		for (IFormElement persist : persists)
		{
			if (persist instanceof GraphicalComponent && isTableView && ((GraphicalComponent)persist).getLabelFor() != null) continue;
			Point location = CSSPositionUtils.getLocation(persist);
			if (startPos <= location.y && endPos > location.y)
			{
				if (isSecurityVisible(persist)) baseComponents.add((BaseComponent)persist);
			}
		}
		return baseComponents;
	}

	public String getRuntimePropertiesString()
	{
		return runtimeData != null ? runtimeData.toString() : "null";
	}

	// called by ftl template
	public String getPropertiesString() throws JSONException, IllegalArgumentException
	{
		getBaseComponents();
		Map<String, Object> properties = getProperties();
		return JSONUtils.writeDataAsFullToJSON(properties, null, null); // null types as we don't have a .spec file for forms
	}

	@SuppressWarnings("nls")
	public Map<String, Object> getProperties()
	{
		Map<String, Object> properties = form.getFlattenedPropertiesMap(); // a copy of form properties
		properties.put("size", form.getSize()); // form.getSize() computes the form height from form parts so do call it instead of relying on the height from size taken from raw form.getPropertiesMap() - where the height is not kept in sync in developer - we have to call getSize()
		properties.put("designSize", form.getSize());
		if (!form.isResponsiveLayout() && (form.getView() == IForm.RECORD_VIEW || form.getView() == IForm.LOCKED_RECORD_VIEW))
		{
			// backwards compartible with ng1
			properties.put("addMinSize", Boolean.TRUE);

			Boolean useMinWidth = form.getUseMinWidth();
			Boolean useMinHeight = form.getUseMinHeight();
			INGApplication application = context.getApplication();
			if (useMinWidth == null && application != null)
			{
				Object clientProperty = application.getClientProperty(UICONSTANTS.DEFAULT_FORM_USE_MIN_WIDTH);
				if (clientProperty instanceof Boolean cp) useMinWidth = cp;
			}
			if (useMinHeight == null && application != null)
			{
				Object clientProperty = application.getClientProperty(UICONSTANTS.DEFAULT_FORM_USE_MIN_HEIGHT);
				if (clientProperty instanceof Boolean cp) useMinHeight = cp;
			}

			properties.put("useMinWidth", useMinWidth != null ? useMinWidth : Boolean.TRUE);
			properties.put("useMinHeight", useMinHeight != null ? useMinHeight : Boolean.TRUE);
		}
		properties.put("hasExtraParts", Boolean.valueOf(FormElementHelper.INSTANCE.hasExtraParts(form)));
		HashMap<String, Boolean> absolute = new HashMap<>(formComponentsLayout);
		absolute.put("", Boolean.valueOf(!form.isResponsiveLayout()));
		for (FormElement fe : getAbsoluteLayoutElements())
		{
			absolute.put(fe.getName(), Boolean.TRUE);
		}
		properties.put("absoluteLayout", absolute);
		properties.put(IContentSpecConstants.PROPERTY_USE_CSS_POSITION, getCSSPositionElementNames());
		if (design && !form.isResponsiveLayout())
		{
			properties.put(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName(),
				Integer.valueOf(ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER + ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER));
		}
		if (form.getView() == IForm.LIST_VIEW || form.getView() == IFormConstants.VIEW_TYPE_LIST_LOCKED)
		{
			// handle horizontal scrollbar on form level for listview
			int horizontalScrollBars = ISupportScrollbars.HORIZONTAL_SCROLLBAR_AS_NEEDED;
			if ((form.getScrollbars() & ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS) != 0)
			{
				horizontalScrollBars = ISupportScrollbars.HORIZONTAL_SCROLLBAR_ALWAYS;
			}
			else if ((form.getScrollbars() & ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER) != 0)
			{
				horizontalScrollBars = ISupportScrollbars.HORIZONTAL_SCROLLBAR_NEVER;
			}
			int scrollbars = ISupportScrollbars.VERTICAL_SCROLLBAR_NEVER + horizontalScrollBars;
			properties.put(StaticContentSpecLoader.PROPERTY_SCROLLBARS.getPropertyName(), Integer.valueOf(scrollbars));
		}
		removeUnneededFormProperties(properties);
		if (properties.containsKey(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName()))
		{
			Border border = ComponentFactoryHelper.createBorder((String)properties.get(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName()), false);
			properties.put(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName(), BorderPropertyType.writeBorderToJson(border));
		}
		return properties;
	}

	private static void removeUnneededFormProperties(Map<String, Object> properties)
	{
		properties.remove(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_SHOWINMENU.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ENCAPSULATION.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_DEFAULTPAGEFORMAT.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_INITIALSORT.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_DEPRECATED.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_CUSTOMPROPERTIES.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_PAPERPRINTSCALE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_PAPERPRINTSCALE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_NAMEDFOUNDSET.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_SELECTIONMODE.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_NAVIGATORID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDELETERECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDELETEALLRECORDSCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDUPLICATERECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONFINDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONINVERTRECORDSCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONNEWRECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONNEXTRECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONOMITRECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONPREVIOUSRECORDCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONSEARCHCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONPRINTPREVIEWCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONSORTCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONSHOWALLRECORDSCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONSHOWOMITTEDRECORDSCMDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDRAGMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDRAGENDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDRAGOVERMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONDROPMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONELEMENTDATACHANGEMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSGAINEDMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONHIDEMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONBEFOREHIDEMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONLOADMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONUNLOADMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONRESIZEMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONSHOWMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONRENDERMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTARTMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONRECORDEDITSTOPMETHODID.getPropertyName());
		properties.remove(StaticContentSpecLoader.PROPERTY_ONRECORDSELECTIONMETHODID.getPropertyName());
	}

	public String getContainerSizesString() throws JSONException, IllegalArgumentException
	{
		Map<String, Dimension> sizes = new HashMap<String, Dimension>();
		sizes.putAll(formComponentParentSizes);
		for (FormElement fe : getAbsoluteLayoutElements())
		{
			sizes.put(fe.getName(), ((LayoutContainer)fe.getPersistIfAvailable().getParent()).getSize());
		}
		return JSONUtils.writeDataAsFullToJSON(sizes, null, null);
	}

	public List<FormElement> getAbsoluteLayoutElements()
	{
		List<FormElement> elements = new ArrayList<FormElement>();
		Collection<IFormElement> components = getBaseComponents();
		if (components != null)
		{
			for (IFormElement component : components)
			{
				if (CSSPositionUtils.isInAbsoluteLayoutMode(component))
				{
					elements.add(FormElementHelper.INSTANCE.getFormElement(component, context.getSolution(), null, design));
				}
			}
		}
		return elements;
	}

	private Map<String, Boolean> getCSSPositionElementNames()
	{
		Map<String, Boolean> names = new HashMap<String, Boolean>();
		names.putAll(formComponentCSSPositionElementNames);
		List<IFormElement> persists = form.getFlattenedObjects(null);
		for (IFormElement persist : persists)
		{
			if (form.getUseCssPosition().booleanValue() || CSSPositionUtils.isInAbsoluteLayoutMode(persist))
			{
				FormElement formElement = FormElementHelper.INSTANCE.getFormElement(persist, context.getSolution(), null, design);
				String name = formElement.getDesignId() != null ? formElement.getDesignId() : formElement.getName();
				if (!names.containsKey(name)) names.put(name, Boolean.TRUE);
			}
		}
		return names;
	}
}
