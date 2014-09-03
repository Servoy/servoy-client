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
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.sablo.IWebComponentInitializer;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
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
import com.servoy.j2db.server.ngclient.design.DesignNGClient;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormatPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISupportsConversion1_FromDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
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
	private final String componentType;

	private final PersistBasedFormElementImpl persistImpl;
	private final String uniqueIdWithinForm;
	private IServoyDataConverterContext dataConverterContext;

	public FormElement(Form form)
	{
		this.form = form;
		persistImpl = new PersistBasedFormElementImpl(form, this);
		componentType = null;
		this.uniqueIdWithinForm = String.valueOf(form.getID());

		Map<String, Object> map = persistImpl.getFlattenedPropertiesMap();
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
	}

	public FormElement(IFormElement persist, final IServoyDataConverterContext context, PropertyPath propertyPath)
	{
		this.dataConverterContext = context;
		persistImpl = new PersistBasedFormElementImpl(persist, this);
		this.form = persistImpl.getForm();
		this.componentType = FormTemplateGenerator.getComponentTypeName(persist);
		this.uniqueIdWithinForm = String.valueOf(persist.getID());

		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		propertyPath.add(getName(persist.getName()));
		Map<String, Object> map = persistImpl.getFormElementPropertyValues(context, specProperties, propertyPath);

		initPropertiesWithDefaults(specProperties, map, context, propertyPath);
		adjustLocationRelativeToPart(context.getSolution(), map);
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
		propertyPath.backOneLevel();
	}

	public FormElement(String componentTypeString, JSONObject jsonObject, Form form, String uniqueIdWithinForm, IServoyDataConverterContext context,
		PropertyPath propertyPath)
	{
		this.dataConverterContext = context;
		persistImpl = null;
		this.form = form;
		this.componentType = componentTypeString;
		this.uniqueIdWithinForm = uniqueIdWithinForm;

		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		propertyPath.add(getName(jsonObject.optString(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName())));
		Map<String, Object> map = new HashMap<>();
		try
		{
			convertFromJSONToFormElementValues(context, specProperties, map, getWebComponentSpec().getHandlers(), jsonObject, propertyPath);
		}
		catch (JSONException ex)
		{
			Debug.error("Error while parsing component design JSON", ex);
		}

		initPropertiesWithDefaults(specProperties, map, context, propertyPath);
		adjustLocationRelativeToPart(context.getSolution(), map);
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
		propertyPath.backOneLevel();
	}

	public IServoyDataConverterContext getDataConverterContext()
	{
		return dataConverterContext;
	}

	/**
	 * This is part of 'Conversion 1' (see {@link NGConversions})
	 */
	protected void convertFromJSONToFormElementValues(IServoyDataConverterContext context, Map<String, PropertyDescription> specProperties,
		Map<String, Object> jsonMap, Map<String, PropertyDescription> eventProperties, JSONObject propertyDesignJSONValues, PropertyPath propertyPath)
		throws JSONException
	{
		Iterator keys = propertyDesignJSONValues.keys();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			Object value = propertyDesignJSONValues.get(key);
			convertDesignToFormElementValueAndPut(context, specProperties.get(key), jsonMap, eventProperties, key, value, propertyPath);
		}
	}

	/**
	 * This is part of 'Conversion 1' (see {@link NGConversions})
	 *
	 * It should ONLY have primitives in propertyDesignPrimitiveValues for property descriptor types that provide such a conversion, as those will be fed to
	 * a converter that expects JSON. For types that do not provide a converter for 'Conversion 1' - it can be any type of object.
	 *
	 * If we figure out that we ever need conversions for non-primitive Java design types that a persist can create, we need another conversion on the type
	 * or hardcoded in persist converter 'Conversion 0' to convert it to a JSON first (from non-primitive Persist property value). Currently, non-primitive
	 * persist property values are always assumed to not need "Conversion 1"
	 */
	protected void convertFromPersistPrimitivesToFormElementValues(IServoyDataConverterContext context, Map<String, PropertyDescription> specProperties,
		Map<String, Object> jsonMap, Map<String, PropertyDescription> eventProperties, Map<String, Object> propertyDesignPrimitiveValues,
		PropertyPath propertyPath)
	{
		Iterator<String> keys = propertyDesignPrimitiveValues.keySet().iterator();
		while (keys.hasNext())
		{
			String key = keys.next();
			Object value = propertyDesignPrimitiveValues.get(key);
			convertDesignToFormElementValueAndPut(context, specProperties.get(key), jsonMap, eventProperties, key, value, propertyPath);
		}
	}

	/**
	 * Applies 'Conversion 1' (see {@link NGConversions}) to one property value - from design to FormElement value and then puts the value in the given formElementValues map.
	 */
	protected void convertDesignToFormElementValueAndPut(IServoyDataConverterContext context, PropertyDescription pd, Map<String, Object> formElementValues,
		Map<String, PropertyDescription> eventProperties, String key, Object value, PropertyPath propertyPath)
	{
		// is it a property
		if (pd == null && eventProperties != null)
		{
			// or an event
			pd = eventProperties.get(key);
		}
		if (pd != null)
		{
			formElementValues.put(key, NGConversions.INSTANCE.applyConversion1(value, pd, context != null ? context.getSolution() : null, this, propertyPath));
		}
		else if (StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(key))
		{
			formElementValues.put(key, value);
		}
	}

	public IPersist getPersistIfAvailable()
	{
		if (persistImpl != null)
		{
			return persistImpl.getPersist();
		}
		return null;
	}

	private void initPropertiesWithDefaults(Map<String, PropertyDescription> specProperties, Map<String, Object> map, IServoyDataConverterContext context,
		PropertyPath propertyPath)
	{
		// do stuff here!
		if (specProperties != null && map != null)
		{
			for (PropertyDescription pd : specProperties.values())
			{
				if (!map.containsKey(pd.getName()))
				{
					if (pd.getDefaultValue() != null)
					{
						map.put(pd.getName(), NGConversions.INSTANCE.applyConversion1(pd.getDefaultValue(), pd, context != null ? context.getSolution() : null,
							this, propertyPath));
					}
					else if (pd.getType().defaultValue() != null)
					{
						// remember that we can use type specified default value when this gets transformed to JSON
						map.put(pd.getName(), NGConversions.ISupportsConversion1_FromDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER);
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
					map.put("offsetY", top);
				}

			}
		}
	}

	public Map<String, Object> getRawPropertyValues()
	{
		return propertyValues;
	}

	public WebComponentSpecification getWebComponentSpec()
	{
		return getWebComponentSpec(true);
	}

	public WebComponentSpecification getWebComponentSpec(boolean throwException)
	{
		WebComponentSpecification spec = null;
		try
		{
			spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentType);
		}
		catch (RuntimeException re)
		{
			Debug.error(re);
			if (throwException) throw re;
		}
		if (spec == null)
		{
			String errorMessage = "Component spec for " + componentType + " not found; please check your component spec file(s).";
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
		return getName((String)getPropertyValue(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName()));
	}

	public String getName(String rawValue)
	{
		String name = rawValue;
		if (name == null)
		{
			name = "svy_" + uniqueIdWithinForm;
		}
		return name.replace('-', '_');
	}

	public String getDesignId()
	{
		if (dataConverterContext != null && dataConverterContext.getApplication() instanceof DesignNGClient)
		{
			return persistImpl.getPersist().getUUID().toString();
		}
		return null;
	}

//
//	/**
//	 * Gets the JSON value to be set in form's ftl template. (initial value)<br><br>
//	 *
//	 * This uses 'Conversion 2' (see {@link NGConversions})
//	 */
//	public Object getPropertyValueConvertedForTemplate(String propertyName)
//	{
//		NGConversions.INSTANCE.applyConversion2(whatHere);
//		if (propertyDescription != null)
//		{
//			// if there is no spec defined default, nor any value for this property, use the sablo type default value,
//			// but that needs to go through Conversion 5.1 (see {@link NGConversions}) first
//			NGConversions.INSTANCE.applyConversion5_1();
//			return propertyDescription.getType().defaultValue();
//		}
//
//		return propertyValues.get(propertyName);
//	}

	/**
	 * Returns the actual value that this FormElement keeps for the requested property.<br>
	 * It is possible that it will return a {@link ISupportsConversion1_FromDesignToFormElement#TYPE_DEFAULT_VALUE_MARKER} in case
	 * the type has a default value bu there was no design value or spec. defined default value for this property.
	 */
	public Object getRawPropertyValue(String propertyName)
	{
		return propertyValues.get(propertyName);
	}

	/**
	 * Same as {@link #getRawPropertyValue(String)} but changes {@link ISupportsConversion1_FromDesignToFormElement#TYPE_DEFAULT_VALUE_MARKER} to null.
	 * It is probably that this method can be removed once more types are refactored to use converters instead of hard-coded lines here and there.
	 */
	public Object getPropertyValue(String propertyName)
	{
		Object value = propertyValues.get(propertyName);
		return value == ISupportsConversion1_FromDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER ? null : value;
	}

	/**
	 * Returns sablo webcomponent value representation of this property value.
	 *
	 * This uses 'Conversion 3' (see {@link NGConversions})
	 */
	public Object getPropertyValueConvertedForWebComponent(String propertyName, WebFormComponent component)
	{
//		// TODO remove this delegation when going with tree structure , this is needed for DataAdapterList which 'thinks' everything is flat
//		String[] split = name.split("\\.");
//		if (split.length > 1)
//		{
//			return ((Map)getProperty(split[0])).get(split[1]);
//		}// end toRemove

		PropertyDescription propertyDescription = getWebComponentSpec().getProperties().get(propertyName);
		if (propertyValues.containsKey(propertyName))
		{
			if (propertyDescription != null) return NGConversions.INSTANCE.applyConversion3(getRawPropertyValue(propertyName), propertyDescription, this,
				component);
			else return getPropertyValue(propertyName); // just in case this method gets called for events for example (which are currently stored in the same map)
		}

		if (StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName().equals(propertyName) && isGraphicalComponentWithNoAction())
		{
			// legacy behavior
			return Integer.valueOf(-1);
		}

		if (propertyDescription != null)
		{
			// we want a defaut value to be set anyway because it was sent into template
			return propertyDescription.getType().defaultValue();
		}
		return null;
	}

	public boolean isForm()
	{
		return persistImpl != null && persistImpl.isForm();
	}

	/**
	 * Refactored hack.
	 */
	boolean isGraphicalComponentWithNoAction()
	{
		if ("svy-button".equals(componentType) || "svy-label".equals(componentType))
		{
			Object onAction = getPropertyValue(StaticContentSpecLoader.PROPERTY_ONACTIONMETHODID.getPropertyName());
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
		return persistImpl != null && persistImpl.isLegacy();
	}

	public String getTagname()
	{
		return FormTemplateGenerator.getTagName(componentType);
	}

	public String getTypeName()
	{
		return componentType;
	}

	public IFormElement getLabel()
	{
		IFormElement label = null;
		String name = (String)getPropertyValue(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());

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
		WebComponentSpecification componentSpec = getWebComponentSpec();
		Set<String> events = componentSpec.getHandlers().keySet();
		for (String eventName : events)
		{
			Object eventValue = getPropertyValue(eventName);
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
		WebComponentSpecification componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> properties = componentSpec.getProperties(TypesRegistry.getType("valuelist"));

		for (PropertyDescription pd : properties.values())
		{
			if (getPropertyValue(pd.getName()) != null)
			{
				valuelistProperties.add(pd.getName());
			}
		}

		return valuelistProperties;
	}

	// called by ftl template
	public String getPropertiesString() throws JSONException
	{
		return propertiesAsTemplateJSON(null).toString();
	}

	@SuppressWarnings("nls")
	public JSONWriter propertiesAsTemplateJSON(JSONWriter writer) throws JSONException
	{
		Map<String, Object> properties = new HashMap<>();

		WebComponentSpecification componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> propDescription = componentSpec.getProperties();
		for (PropertyDescription pd : propDescription.values())
		{
			Object val = getRawPropertyValue(pd.getName());
			if (val == null) continue;

			if (val != ISupportsConversion1_FromDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER)
			{
				IPropertyType type = pd.getType();
				if (type instanceof DataproviderPropertyType || type instanceof FormatPropertyType || type instanceof ValueListPropertyType ||
					type instanceof MediaPropertyType)
				{
					// TODO move these to type conversions directly (those can return null)
					continue;
				}
				if (type instanceof TagStringPropertyType)
				{
					// tagstring if it has tags then it is data then it should be skipped.
					if (((String)val).contains("%%")) continue;
				}
			}
			properties.put(pd.getName(), val);
		}

		if (persistImpl == null || !persistImpl.isForm())
		{
			Dimension dim = getDesignSize();
			if (dim != null) properties.put(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), dim);
			Integer anchor = (Integer)getPropertyValue(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName());
			if (anchor != null)
			{
				properties.put(StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), anchor);
			}
		}

		Object offsetY = getPropertyValue("offsetY");
		if (offsetY != null) properties.put("offsetY", offsetY);

		// get types for conversion
		PropertyDescription propertyTypes = AggregatedPropertyType.newAggregatedProperty();
		for (Entry<String, Object> p : properties.entrySet())
		{
			PropertyDescription t = getWebComponentSpec().getProperty(p.getKey());
			if (t != null) propertyTypes.putProperty(p.getKey(), t);
		}
		if (!propertyTypes.hasChildProperties()) propertyTypes = null;

		JSONWriter propertyWriter = (writer != null ? writer : new JSONStringer());
		try
		{
			propertyWriter.object();
			JSONUtils.writeDataWithConversions(NGConversions.FormElementToJSON.INSTANCE, propertyWriter, properties, /* null */propertyTypes); // don't use property types here as they aren't yet converted...
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
		if (persistImpl != null && persistImpl.getPersist() instanceof ISupportSize) return ((ISupportSize)persistImpl.getPersist()).getSize();
		return null;
	}

	Point getDesignLocation()
	{
		if (persistImpl != null && persistImpl.getPersist() instanceof ISupportBounds) return ((ISupportBounds)persistImpl.getPersist()).getLocation();
		return null;
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

	/**
	 * This should not be called normally; it's only called because legacy portal needs to remove some prefix from the dataprovider properties of children.
	 */
	public void updatePropertyValuesDontUse(Map<String, Object> elementProperties)
	{
		this.propertyValues = elementProperties;
	}
}
