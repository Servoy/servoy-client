/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient.property.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.Container;
import org.sablo.IChangeListener;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.ISmartPropertyValue;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.WebFormUI;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeFormElementValue;
import com.servoy.j2db.server.ngclient.property.ComponentTypeSabloValue;

/**
 * @author jcompagner
 * @since 8.4
 */
public class FormComponentSabloValue implements ISmartPropertyValue
{

	private final Form form;
	private final INGFormElement formElement;
	private final JSONObject formElementValue;
	private final PropertyDescription pd;
	private final DataAdapterList dal;
	private final WebFormComponent component;

	private FormComponentCache currentFormComponentCache;
	private ComponentTypeSabloValue[] components;

	public FormComponentSabloValue(INGFormElement formElement, JSONObject formElementValue, PropertyDescription pd, DataAdapterList dal,
		WebFormComponent component, Form form)
	{
		this.form = form;
		this.formElement = formElement;
		this.formElementValue = formElementValue;
		this.pd = pd;
		this.dal = dal;
		this.component = component;
	}

	public FormComponentCache getCache()
	{
		Container parent = component.findParent(WebFormUI.class);
		if (parent instanceof WebFormUI)
		{
			// cache it on the FormUI object, because FormElementHelper can only cache when it is not solution model, but then the cache is constantly changing..
			FormComponentCache fcc = ((WebFormUI)parent).getFormComponentCache(component);
			if (fcc != null) return fcc;
			fcc = FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, formElementValue, form, dal.getApplication().getFlattenedSolution());
			((WebFormUI)parent).cacheFormComponentCache(component, fcc);
			return fcc;
		}
		return FormElementHelper.INSTANCE.getFormComponentCache(formElement, pd, formElementValue, form, dal.getApplication().getFlattenedSolution());
	}

	private ComponentTypeSabloValue[] getComponents()
	{
		FormComponentCache formComponentCache = getCache();
		if (currentFormComponentCache != formComponentCache)
		{
			List<FormElement> elements = formComponentCache.getFormComponentElements();
			List<ComponentTypeSabloValue> componentsList = new ArrayList<ComponentTypeSabloValue>(elements.size());

			PropertyPath path = new PropertyPath();
			path.add(component.getName());
			path.add("containedForm");
			path.add("childElements");
			JSONObject tags = new JSONObject();
			tags.put(ComponentTypeSabloValue.TAG_ADD_TO_ELEMENTS_SCOPE, true);
			PropertyDescription compPd = new PropertyDescriptionBuilder().withName(pd.getName()).withType(ComponentPropertyType.INSTANCE).withConfig(
				pd.getConfig()).withTags(tags).build();
			int j = 0;
			for (FormElement element : elements)
			{
				path.add(j);
				ComponentTypeFormElementValue elementValue = ComponentPropertyType.INSTANCE.getFormElementValue(null, compPd, path, element,
					dal.getApplication().getFlattenedSolution());
				ComponentTypeSabloValue ctsv = ComponentPropertyType.INSTANCE.toSabloComponentValue(elementValue, compPd, element, component, dal);
				if (ctsv != null)
				{
					j++; // if it is null then it is probably a child component that was blocked by security (visibility == false); in that case just ignore it (similar to what portal does through .spec setting on comp. array to ignore null values at runtime)
					componentsList.add(ctsv);
				}
				path.backOneLevel();
				if (element.getWebComponentSpec() != null)
				{
					Collection<PropertyDescription> properties = element.getWebComponentSpec().getProperties(FormComponentPropertyType.INSTANCE);
					if (properties.size() > 0)
					{
						for (PropertyDescription pd : properties)
						{
							Object propertyValue = element.getPropertyValue(pd.getName());
							Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, dal.getApplication().getFlattenedSolution());
							if (frm == null) continue;
							FormComponentCache innerCache = FormElementHelper.INSTANCE.getFormComponentCache(element, pd, (JSONObject)propertyValue, frm,
								dal.getApplication().getFlattenedSolution());
							List<FormElement> innerElements = innerCache.getFormComponentElements();
							for (FormElement innerElement : innerElements)
							{
								path.add(j);
								elementValue = ComponentPropertyType.INSTANCE.getFormElementValue(null, compPd, path, innerElement,
									dal.getApplication().getFlattenedSolution());
								// use main property
								ctsv = ComponentPropertyType.INSTANCE.toSabloComponentValue(elementValue, compPd, innerElement, component, dal);
								if (ctsv != null)
								{
									j++; // if it is null then it is probably a child component that was blocked by security (visibility == false); in that case just ignore it (similar to what portal does through .spec setting on comp. array to ignore null values at runtime)
									componentsList.add(ctsv);
								}
								path.backOneLevel();
							}
						}
					}
				}
			}

			// re-attach
			if (currentFormComponentCache != null && changeMonitor != null && webObjectContext != null)
			{
				for (ComponentTypeSabloValue componentTypeSabloValue : componentsList)
				{
					componentTypeSabloValue.attachToBaseObject(changeMonitor, webObjectContext);
				}
			}

			currentFormComponentCache = formComponentCache;
			components = componentsList.toArray(new ComponentTypeSabloValue[0]);
		}
		return components;
	}

	private IChangeListener changeMonitor;
	private IWebObjectContext webObjectContext;

	@Override
	public void attachToBaseObject(IChangeListener changeMonitor, IWebObjectContext webObjectContext)
	{
		this.changeMonitor = changeMonitor;
		this.webObjectContext = webObjectContext;
		ComponentTypeSabloValue[] components = getComponents();
		for (ComponentTypeSabloValue componentTypeSabloValue : components)
		{
			componentTypeSabloValue.attachToBaseObject(changeMonitor, webObjectContext);
		}
	}

	@Override
	public void detach()
	{
		if (webObjectContext == null) return; // it is already detached

		if (components != null)
		{
			for (ComponentTypeSabloValue componentTypeSabloValue : components)
			{
				componentTypeSabloValue.detach();
			}
		}
		this.webObjectContext = null;
	}

	@SuppressWarnings("nls")
	public void fullToJSON(JSONWriter writer, FormComponentPropertyType formComponentPropertyType, IBrowserConverterContext dataConverterContext)
	{
		writer.object();
		writer.key("uuid");
		writer.value(getCache().getHtmlTemplateUUIDForAngular());
		writer.key("formHeight");
		writer.value(form.getSize().height);
		writer.key("formWidth");
		writer.value(form.getSize().width);
		writer.key("absoluteLayout");
		writer.value(!form.isResponsiveLayout());
		writer.key(IContentSpecConstants.PROPERTY_USE_CSS_POSITION);
		writer.value(form.getUseCssPosition());
		writer.key("childElements");
		writer.array();
		ComponentTypeSabloValue[] components = getComponents();
		for (ComponentTypeSabloValue component : components)
		{
			component.fullToJSON(writer, ComponentPropertyType.INSTANCE);
		}
		writer.endArray();
		writer.endObject();
	}

	public void browserUpdatesReceived(JSONArray array)
	{
		ComponentTypeSabloValue[] components = getComponents();
		for (int i = 0; i < array.length(); i++)
		{
			Object comp = array.get(i);
			components[i].browserUpdatesReceived(comp);
		}
	}

	public void changesToJSON(JSONWriter writer, FormComponentPropertyType formComponentPropertyType)
	{
		writer.object();
		writer.key("childElements");
		writer.array();
		ComponentTypeSabloValue[] components = getComponents();
		for (ComponentTypeSabloValue component : components)
		{
			component.changesToJSON(writer, ComponentPropertyType.INSTANCE);
		}
		writer.endArray();
		writer.endObject();
	}

}
