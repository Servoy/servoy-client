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
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

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
 *  @since 8.4
 *
 */
public class FormComponentSabloValue implements ISmartPropertyValue
{
	private final Form form;
	private final String elementStartName;
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
		this.elementStartName = FormElementHelper.getStartElementName(component.getFormElement(), pd);
	}

	public FormComponentCache getCache()
	{
		Container parent = component.getParent();
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
			components = new ComponentTypeSabloValue[elements.size()];

			PropertyPath path = new PropertyPath();
			path.add(component.getName());
			path.add("containedForm");
			path.add("childElements");
			JSONObject tags = new JSONObject();
			tags.put(ComponentTypeSabloValue.TAG_ADD_TO_ELEMENTS_SCOPE, true);
			PropertyDescription compPd = new PropertyDescriptionBuilder().withName(pd.getName()).withType(ComponentPropertyType.INSTANCE).withConfig(
				pd.getConfig()).withTags(tags).build();
			int j = 0;
			for (int i = 0; i < components.length; i++)
			{
				FormElement element = elements.get(i);
				path.add(j);
				ComponentTypeFormElementValue elementValue = ComponentPropertyType.INSTANCE.getFormElementValue(null, compPd, path, element,
					dal.getApplication().getFlattenedSolution());
				ComponentTypeSabloValue ctsv = ComponentPropertyType.INSTANCE.toSabloComponentValue(elementValue, compPd, element, component, dal);
				if (ctsv != null) components[j++] = ctsv; // if it is null then it is probably a child component that was blocked by security (visibility == false); in that case just ignore it (similar to what portal does through .spec setting on comp. array to ignore null values at runtime)
				path.backOneLevel();
			}

			// re-attach
			if (currentFormComponentCache != null && changeMonitor != null && webObjectContext != null)
			{
				for (ComponentTypeSabloValue componentTypeSabloValue : components)
				{
					componentTypeSabloValue.attachToBaseObject(changeMonitor, webObjectContext);
				}

			}
			currentFormComponentCache = formComponentCache;
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
		ComponentTypeSabloValue[] components = getComponents();
		for (ComponentTypeSabloValue componentTypeSabloValue : components)
		{
			componentTypeSabloValue.detach();
		}
	}

	@SuppressWarnings("nls")
	public void fullToJSON(JSONWriter writer, DataConversion clientConversion, FormComponentPropertyType formComponentPropertyType,
		IBrowserConverterContext dataConverterContext)
	{
		clientConversion.convert("formcomponent");
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
		writer.key("startName");
		writer.value(elementStartName);
		writer.key("childElements");
		writer.array();
		DataConversion componentConversionMarkers = new DataConversion();
		componentConversionMarkers.pushNode("childElements");
		ComponentTypeSabloValue[] comps = getComponents();
		for (int i = 0; i < comps.length; i++)
		{
			componentConversionMarkers.pushNode(String.valueOf(i));
			comps[i].fullToJSON(writer, componentConversionMarkers, ComponentPropertyType.INSTANCE);
			componentConversionMarkers.popNode();
		}
		componentConversionMarkers.popNode();
		writer.endArray();
		if (componentConversionMarkers.getConversions().size() > 0)
		{
			writer.key(JSONUtils.TYPES_KEY).object();
			JSONUtils.writeConversions(writer, componentConversionMarkers.getConversions());
			writer.endObject();
		}
		writer.endObject();
	}

	/**
	 * @param newJSONValue
	 */
	public void browserUpdatesReceived(JSONArray array)
	{
		ComponentTypeSabloValue[] components = getComponents();
		for (int i = 0; i < array.length(); i++)
		{
			Object comp = array.get(i);
			components[i].browserUpdatesReceived(comp);
		}
	}

	public void changesToJSON(JSONWriter writer, DataConversion clientConversion, FormComponentPropertyType formComponentPropertyType)
	{
		clientConversion.convert("formcomponent");
		writer.object();
		writer.key("childElements");
		writer.array();
		DataConversion componentConversionMarkers = new DataConversion();
		componentConversionMarkers.pushNode("childElements");
		ComponentTypeSabloValue[] components = getComponents();
		for (int i = 0; i < components.length; i++)
		{
			componentConversionMarkers.pushNode(String.valueOf(i));
			components[i].changesToJSON(writer, componentConversionMarkers, ComponentPropertyType.INSTANCE);
			componentConversionMarkers.popNode();
		}
		componentConversionMarkers.popNode();
		writer.endArray();
		if (componentConversionMarkers.getConversions().size() > 0)
		{
			writer.key(JSONUtils.TYPES_KEY).object();
			JSONUtils.writeConversions(writer, componentConversionMarkers.getConversions());
			writer.endObject();
		}
		writer.endObject();
	}
}
