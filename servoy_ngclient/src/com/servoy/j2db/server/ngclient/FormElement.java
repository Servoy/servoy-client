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

package com.servoy.j2db.server.ngclient;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.IWebComponentInitializer;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpec;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.property.IComplexPropertyValue;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.ConversionLocation;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.MiniMap;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public final class FormElement implements IWebComponentInitializer
{
	private final Form form;
	private Map<String, Object> propertyValues;
	private final String componentTypeString;

	private final PersistBasedFormElementImpl legacyImpl;
	private final String uniqueIdWithinForm;
	private IDataConverterContext dataConverterContext;

	public FormElement(Form form)
	{
		this.form = form;
		legacyImpl = new PersistBasedFormElementImpl(form, this);
		componentTypeString = null;
		this.uniqueIdWithinForm = String.valueOf(form.getID());

		Map<String, Object> map = legacyImpl.getFlattenedPropertiesMap();
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
	}

	public FormElement(IFormElement persist, final IDataConverterContext context)
	{
		this.dataConverterContext = context;
		legacyImpl = new PersistBasedFormElementImpl(persist, this);
		this.form = legacyImpl.getForm();
		this.componentTypeString = FormTemplateGenerator.getComponentTypeName(persist);
		this.uniqueIdWithinForm = String.valueOf(persist.getID());

		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		Map<String, Object> map = legacyImpl.getConvertedProperties(context, specProperties);

		propertyValues = map; // temporary - can be needed when initProperties initialises complex property type values 
		initProperties(specProperties, map, context);
		adjustLocationRelativeToPart(context.getSolution(), map);
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
	}

	public FormElement(String componentTypeString, JSONObject jsonObject, Form form, String uniqueIdWithinForm, IDataConverterContext context)
	{
		this.dataConverterContext = context;
		legacyImpl = null;
		this.form = form;
		this.componentTypeString = componentTypeString;
		this.uniqueIdWithinForm = uniqueIdWithinForm;

		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		Map<String, Object> map = new HashMap<>();
		try
		{
			getConvertedJSONDefinitionProperties(context, specProperties, map, getWebComponentSpec().getHandlers(), jsonObject);
		}
		catch (JSONException ex)
		{
			Debug.error("Error while parsing component design JSON", ex);
		}

		propertyValues = map; // temporary - can be needed when initProperties initialises complex property type values 
		initProperties(specProperties, map, context);
		adjustLocationRelativeToPart(context.getSolution(), map);
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
	}

	public IDataConverterContext getDataConverterContext()
	{
		return dataConverterContext;
	}

	void getConvertedJSONDefinitionProperties(IDataConverterContext context, Map<String, PropertyDescription> specProperties, Map<String, Object> jsonMap,
		Map<String, PropertyDescription> eventProperties, JSONObject jsonProperties) throws JSONException
	{
		Iterator keys = jsonProperties.keys();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			// is it a property
			PropertyDescription pd = specProperties.get(key);
			if (pd == null)
			{
				// or an event
				pd = eventProperties.get(key);
			}
			if (pd != null)
			{
				// TODO where the handle PropertyType.form properties? (see tabpanel below)
				// toJavaObject should accept application because it is needed for format
				jsonMap.put(key, NGClientForJsonConverter.toJavaObject(jsonProperties.get(key), pd, context, ConversionLocation.DESIGN, null));
				//jsonMap.put(key, JSONUtils.toJavaObject(jsonProperties.get(key), pd.getType(), fs));
			}
		}
	}

	public IPersist getLegacyPersistIfAvailable()
	{
		return legacyImpl.getPersist();
	}

	private void initProperties(Map<String, PropertyDescription> specProperties, Map<String, Object> map, IDataConverterContext context)
	{
		if (specProperties != null && map != null)
		{
			for (PropertyDescription pd : specProperties.values())
			{
				if (pd.getDefaultValue() != null && !map.containsKey(pd.getName()))
				{
					try
					{
						map.put(pd.getName(), NGClientForJsonConverter.toJavaObject(pd.getDefaultValue(), pd, context, ConversionLocation.DESIGN, null));
					}
					catch (JSONException e)
					{
						Debug.error("Error while parsing/loading default value for property: " + pd.getName() + ". Value: " + pd.getDefaultValue(), e);
					}
				}
				else
				{
					Object value = map.get(pd.getName());
					if (value instanceof IComplexPropertyValue)
					{
						((IComplexPropertyValue)value).initialize(this, pd.getName(), pd.getDefaultValue());
					}
				}
			}
		}
	}

	private void adjustLocationRelativeToPart(FlattenedSolution fs, Map<String, Object> map)
	{
		if (map != null && form != null)
		{
			Form flatForm = fs.getFlattenedForm(form);
			Point location = getDesignLocation();
			if (location != null)
			{
				Point newLocation = new Point(location);
				Part part = flatForm.getPartAt(location.y);
				if (part != null)
				{
					int top = flatForm.getPartStartYPos(part.getID());
					newLocation.y = newLocation.y - top;
					map.put(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), newLocation);
				}

			}
		}
	}

	public Map<String, Object> getProperties()
	{
		return propertyValues;
	}

	public WebComponentSpec getWebComponentSpec()
	{
		return getWebComponentSpec(true);
	}

	public WebComponentSpec getWebComponentSpec(boolean throwException)
	{
		WebComponentSpec spec = null;
		try
		{
			spec = WebComponentSpecProvider.getInstance().getWebComponentDescription(componentTypeString);
		}
		catch (RuntimeException re)
		{
			Debug.error(re);
			if (throwException) throw re;
		}
		if (spec == null)
		{
			String errorMessage = "Component spec for " + componentTypeString + " not found; please check your component spec file(s).";
			Debug.error(errorMessage);
			if (throwException) throw new IllegalStateException(errorMessage);
		}
		return spec;
	}

	/**
	 * Never returns null. Will try to return a name that is unique in containing form and consistent between different runs - if a name
	 * was not explicitly set on the component.
	 */
	public String getName()
	{
		String name = (String)getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
		if (name == null)
		{
			name = "svy_" + uniqueIdWithinForm;
		}
		return name;
	}

	public Object getProperty(String name)
	{
		return propertyValues.get(name);
	}

	public Object getPropertyWithDefault(String name)
	{
		// TODO remove this delegation when going with tree structure , this is needed for DataAdapterList which 'thinks' everything is flat
		String[] split = name.split("\\.");
		if (split.length > 1)
		{
			return ((Map)getProperty(split[0])).get(split[1]);
		}// end toRemove

		if (propertyValues.containsKey(name))
		{
			return getProperty(name);
		}

		if (StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName().equals(name) && isGraphicalComponentWithNoAction())
		{
			// legacy behavior
			return Integer.valueOf(-1);
		}

		PropertyDescription propertyDescription = getWebComponentSpec().getProperties().get(name);

		if (propertyDescription != null)
		{
			switch (propertyDescription.getType().getDefaultEnumValue())
			{
				case bool :
					return Boolean.FALSE;
				case valuelist :
				case bytenumber :
				case doublenumber :
				case floatnumber :
				case intnumber :
				case longnumber :
				case shortnumber :
					return Integer.valueOf(0);
				case dimension :
					return new Dimension(0, 0);
				case point :
					return new Point(0, 0);
			}
		}
		return null;
	}

	public boolean isForm()
	{
		return legacyImpl != null && legacyImpl.isForm();
	}

	/**
	 * Refactored hack.
	 */
	boolean isGraphicalComponentWithNoAction()
	{
		if ("svy-button".equals(componentTypeString) || "svy-label".equals(componentTypeString))
		{
			Object onAction = getProperty(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName());
			if (onAction == null || (onAction instanceof Integer && (((Integer)onAction).intValue() <= 0))) return true;
		}
		return false;
	}

	public Form getForm()
	{
		return form;
	}

	public boolean isLegacy()
	{
		return legacyImpl != null && legacyImpl.isLegacy();
	}

	public String getTagname()
	{
		return componentTypeString != null ? "data-" + componentTypeString : "data-form";
	}

	public String getTypeName()
	{
		return componentTypeString;
	}

	public IFormElement getLabel()
	{
		IFormElement label = null;
		String name = (String)getProperty(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());

		if (name != null && form != null)
		{
			Iterator<IPersist> formElementsIte = form.getAllObjects();
			IPersist p;
			while (formElementsIte.hasNext())
			{
				p = formElementsIte.next();
				if (p instanceof GraphicalComponent && name.equals(((GraphicalComponent)p).getLabelFor()))
				{
					label = (GraphicalComponent)p;
					break;
				}
			}
		}
		return label;
	}

	public Collection<String> getHandlers()
	{
		List<String> handlers = new ArrayList<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Set<String> events = componentSpec.getHandlers().keySet();
		for (String eventName : events)
		{
			Object eventValue = getProperty(eventName);
			if (eventValue != null && !(eventValue instanceof Integer && (((Integer)eventValue).intValue() == -1 || ((Integer)eventValue).intValue() == 0)))
			{
				handlers.add(eventName);
			}
		}
		return handlers;
	}

	public Collection<String> getValuelistProperties()
	{
		List<String> valuelistProperties = new ArrayList<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> properties = componentSpec.getProperties(IPropertyType.Default.valuelist.getType());

		for (PropertyDescription pd : properties.values())
		{
			if (getProperty(pd.getName()) != null)
			{
				valuelistProperties.add(pd.getName());
			}
		}

		return valuelistProperties;
	}

	// called by ftl template
	public String getPropertiesString() throws JSONException
	{
		return propertiesAsJSON(null).toString();
	}

	@SuppressWarnings("nls")
	public JSONWriter propertiesAsJSON(JSONWriter writer) throws JSONException
	{
		Map<String, Object> properties = new HashMap<>();
		WebComponentSpec componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> propDescription = componentSpec.getProperties();
		for (PropertyDescription pd : propDescription.values())
		{
			Object val = getProperty(pd.getName());
			if (val == null) continue;
			switch (pd.getType().getDefaultEnumValue())
			{
			// dataprovider,formats,valuelist are always only pushed through the components
				case dataprovider :
				case format :
				case valuelist :
					continue;
				case tagstring :
					// tagstring if it has tags then it is data then it should be skipped.
					if (((String)val).contains("%%")) continue;
				default :
					properties.put(pd.getName(), val);
					break;

			}
		}

		if (legacyImpl == null || !legacyImpl.isForm())
		{
			Map<String, Object> propertiesMap = new HashMap<>(propertyValues);
			Dimension dim = getDesignSize();
			properties.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), dim);
			Integer anchor = (Integer)propertiesMap.get(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName());
			if (anchor != null)
			{
				properties.put(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), anchor);
			}
		}

		JSONWriter propertyWriter = (writer != null ? writer : new JSONStringer());
		try
		{
			propertyWriter.object();
			JSONUtils.addObjectPropertiesToWriter(propertyWriter, properties, NGClientForJsonConverter.INSTANCE);
			return propertyWriter.endObject();
		}
		catch (JSONException | IllegalArgumentException e)
		{
			Debug.error("Problem detected when handling a component's (" + getTagname() + ") properties / events.", e);
			throw e;
		}
	}

	Dimension getDesignSize()
	{
		Dimension dim = null;
		if (propertyValues != null)
		{
			dim = (Dimension)getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
		}
		if (dim == null && legacyImpl != null && legacyImpl.getPersist() instanceof ISupportSize) dim = ((ISupportSize)legacyImpl.getPersist()).getSize();
		if (dim == null) dim = new Dimension(80, 80); // just like a Bean persist would do
		return dim;
	}

	Point getDesignLocation()
	{
		Point location = null;
		if (propertyValues != null)
		{
			location = (Point)getProperty(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
		}
		if (location == null && legacyImpl != null && legacyImpl.getPersist() instanceof ISupportBounds) location = ((ISupportBounds)legacyImpl.getPersist()).getLocation();
		return location;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format(
			"<%1$s name=\"%2$s\" svy-model=\"model.%2$s\" svy-api=\"api.%2$s\" svy-handlers=\"handlers.%2$s\" svy-apply=\"handlers.%2$s.svy_apply\" svy-servoyApi=\"handlers.%2$s.svy_servoyApi\"></%1$s>",
			getTagname(), getName());
	}
}
