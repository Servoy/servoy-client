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
import org.sablo.specification.BaseSpecProvider.ISpecReloadListener;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.AggregatedPropertyType;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.specification.property.types.VisiblePropertyType;
import org.sablo.websocket.TypedData;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormReference;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType;
import com.servoy.j2db.server.ngclient.property.types.IFindModeAwareType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.FormElementToJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignDefaultToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.utils.MiniMap;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
@SuppressWarnings("nls")
public final class FormElement implements IWebComponentInitializer, INGFormElement
{

	public static final String DROPPABLE = "droppable";
	public static final String ERROR_BEAN = "servoycore-errorbean";
	public static final String SVY_NAME_PREFIX = "svy_";

	private final Form form;
	private Map<String, Object> propertyValues;
	private Map<Class< ? >, Map<PropertyDescription, Object>> preprocessedPropertyInfo; // standard information that can be computed in template about some properties and will be needed at runtime; (infoType, (propertyName, infoObject))
	private final String componentType;

	private final PersistBasedFormElementImpl persistImpl;
	private final String uniqueIdWithinForm;

	private final boolean inDesigner;
	private boolean isVisible = true;

	private FlattenedSolution fs;

	public FormElement(IFormElement persist, FlattenedSolution fs, PropertyPath propertyPath, boolean inDesigner)
	{
		this.fs = fs;
		this.inDesigner = inDesigner;
		this.persistImpl = new PersistBasedFormElementImpl(persist, this);
		Form f = persistImpl.getForm();
		if (f instanceof FlattenedForm) this.form = f;
		else this.form = fs.getFlattenedForm(f);

		boolean willTurnIntoErrorBean = inDesigner && persist instanceof Bean && !DefaultNavigator.NAME_PROP_VALUE.equals(persist.getName()) &&
			WebComponentSpecProvider.getInstance().getWebComponentSpecification(((Bean)persist).getBeanClassName()) != null &&
			usesPersistTypedProperties(WebComponentSpecProvider.getInstance().getWebComponentSpecification(((Bean)persist).getBeanClassName()));

		this.componentType = willTurnIntoErrorBean ? FormElement.ERROR_BEAN : FormTemplateGenerator.getComponentTypeName(persist);
		IFormElement superPersist = persist;
		String uniqueId = String.valueOf(superPersist.getID());
		while (inDesigner && superPersist != null)
		{
			superPersist = (IFormElement)PersistHelper.getSuperPersist(superPersist);
			if (superPersist != null) uniqueId = String.valueOf(superPersist.getID());
		}
		this.uniqueIdWithinForm = uniqueId;

		propertyValues = new HashMap<String, Object>();
		propertyValues.put(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(), persist.getName());
		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		boolean addNameToPath = propertyPath.shouldAddElementNameAndClearFlag();
		if (addNameToPath) propertyPath.add(getName());
		Map<String, Object> map = persistImpl.getFormElementPropertyValues(fs, specProperties, propertyPath);

		adjustLocationRelativeToPart(fs, map);
		initTemplateProperties(specProperties, map, fs, propertyPath);

		if (willTurnIntoErrorBean)
		{
			map.put("error",
				"Please remove and insert this component again. Components which define custom types in their spec file will not work properly due to some changes in 8.0 beta2/beta3 versions (for solutions created with previous beta/alpha versions). See log file for details.");

			Debug.warn("Please remove and insert again the component with name '" + persist.getName() +
				(this.form != null ? "' on the form '" + this.form.getName() : "") + "'. Type: " + FormTemplateGenerator.getComponentTypeName(persist) +
				". Components which define custom types in their spec file will not work properly due to some changes in 8.0 beta2/beta3 versions (for solutions created with previous beta/alpha versions). Properties: " +
				map.get("beanXML")); // so Bean persist used for custom components is no longer working properly - now it uses WebComponent
		}

		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
		if (addNameToPath) propertyPath.backOneLevel();
	}

	public FormElement(String componentTypeString, JSONObject jsonObject, Form form, String uniqueIdWithinForm, FlattenedSolution fs, PropertyPath propertyPath,
		boolean inDesigner)
	{
		this.inDesigner = inDesigner;
		this.persistImpl = null;
		if (form instanceof FlattenedForm) this.form = form;
		else this.form = fs.getFlattenedForm(form);

		if (WebComponentSpecProvider.getInstance().getWebComponentSpecification(componentTypeString) == null)
		{
			this.componentType = FormElement.ERROR_BEAN;
		}
		else
		{
			this.componentType = componentTypeString;
		}
		this.uniqueIdWithinForm = uniqueIdWithinForm;

		propertyValues = new HashMap<String, Object>();
		propertyValues.put(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName(),
			jsonObject.optString(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName()));

		Map<String, PropertyDescription> specProperties = getWebComponentSpec().getProperties();
		boolean addNameToPath = propertyPath.shouldAddElementNameAndClearFlag();
		if (addNameToPath) propertyPath.add(getName());
		Map<String, Object> map = new HashMap<>();
		try
		{
			convertFromJSONToFormElementValues(fs, specProperties, map, getWebComponentSpec().getHandlers(), jsonObject, propertyPath);
		}
		catch (JSONException ex)
		{
			Debug.error("Error while parsing component design JSON", ex);
		}

		adjustLocationRelativeToPart(fs, map);
		initTemplateProperties(specProperties, map, fs, propertyPath);

		if (this.componentType == FormElement.ERROR_BEAN)
		{
			map.put("error", "Component (specification) with type: " + componentTypeString +
				" not found. Please make sure that this custom web component type is available.");
		}
		propertyValues = Collections.unmodifiableMap(new MiniMap<String, Object>(map, map.size()));
		if (addNameToPath) propertyPath.backOneLevel();
	}

	/**
	 * This is part of 'Conversion 1' (see {@link NGConversions})
	 */
	protected void convertFromJSONToFormElementValues(FlattenedSolution fs, Map<String, PropertyDescription> specProperties, Map<String, Object> jsonMap,
		Map<String, WebObjectFunctionDefinition> eventProperties, JSONObject propertyDesignJSONValues, PropertyPath propertyPath) throws JSONException
	{
		Iterator keys = propertyDesignJSONValues.keys();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			Object value = propertyDesignJSONValues.get(key);
			convertDesignToFormElementValueAndPut(fs, getPropertyOrEvent(key, specProperties, eventProperties), jsonMap, key, value, propertyPath);
		}
	}

	/**
	 * This is part of 'Conversion 1' (see {@link NGConversions})
	 *
	 * It should ONLY have primitives in properties for property descriptor types that provide such a conversion, as those will be fed to
	 * a converter that expects JSON. For types that do not provide a converter for 'Conversion 1' - it can be any type of object.
	 *
	 * If we figure out that we ever need conversions for non-primitive Java design types that a persist can create, we need another conversion on the type
	 * or hardcoded in persist converter 'Conversion 0' to convert it to a JSON first (from non-primitive Persist property value). Currently, non-primitive
	 * persist property values are always assumed to not need "Conversion 1"
	 */
	protected void convertFromPersistPrimitivesToFormElementValues(FlattenedSolution fs, Map<String, PropertyDescription> specProperties,
		Map<String, WebObjectFunctionDefinition> eventProperties, Map<String, Object> properties, PropertyPath propertyPath)
	{
		Iterator<String> keys = properties.keySet().iterator();
		while (keys.hasNext())
		{
			String key = keys.next();
			Object value = properties.get(key);
			if (!(value instanceof Number || value instanceof String || value instanceof Byte || value instanceof Character))
			{
				// non - primitive type; skip extra conversion as Persist already converted it or convertSpecialPersistProperties(...) already converted it
				continue;
			}
			convertDesignToFormElementValueAndPut(fs, getPropertyOrEvent(key, specProperties, eventProperties), properties, key, value, propertyPath);
		}
	}

	protected PropertyDescription getPropertyOrEvent(String key, Map<String, PropertyDescription> specProperties,
		Map<String, WebObjectFunctionDefinition> eventProperties)
	{
		PropertyDescription pd = specProperties.get(key);
		if (pd == null && eventProperties != null)
		{
			// or an event
			if (eventProperties.get(key) != null) pd = eventProperties.get(key).getAsPropertyDescription();
		}
		return pd;
	}

	/**
	 * Applies 'Conversion 1' (see {@link NGConversions}) to one property value - from design to FormElement value and then puts the value in the given formElementValues map.
	 */
	protected void convertDesignToFormElementValueAndPut(FlattenedSolution fs, PropertyDescription pd, Map<String, Object> formElementValues, String key,
		Object value, PropertyPath propertyPath)
	{
		// is it a property
		if (pd != null)
		{
			propertyPath.add(key);
			formElementValues.put(key, NGConversions.INSTANCE.convertDesignToFormElementValue(value, pd, fs, this, propertyPath));
			propertyPath.backOneLevel();
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

	private void initTemplateProperties(Map<String, PropertyDescription> specProperties, Map<String, Object> map, FlattenedSolution fs,
		PropertyPath propertyPath)
	{
		if (specProperties != null && map != null)
		{
			for (PropertyDescription pd : specProperties.values())
			{
				if (!map.containsKey(pd.getName()))
				{
					if (pd.hasDefault())
					{
						propertyPath.add(pd.getName());
						map.put(pd.getName(), NGConversions.INSTANCE.convertDesignToFormElementValue(pd.getDefaultValue(), pd, fs, this, propertyPath));
						propertyPath.backOneLevel();
					}
					else if (pd.getType() instanceof IDesignDefaultToFormElement< ? , ? , ? >)
					{
						propertyPath.add(pd.getName());
						map.put(pd.getName(), ((IDesignDefaultToFormElement< ? , ? , ? >)pd.getType()).toDefaultFormElementValue(pd, fs, this, propertyPath));
						propertyPath.backOneLevel();
					}
					else if (pd.getType().defaultValue(pd) != null)
					{
						// remember that we can use type specified default value when this gets transformed to JSON
						map.put(pd.getName(), NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER);
					}
				}

				Object formElementValue = map.get(pd.getName());

				// check find mode aware and data linked properties and remember/cache what they will need at runtime
				if (formElementValue != NGConversions.IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER)
				{
					// as array element property descriptions can describe multiple property values in the same bean - we won't cache those
					try
					{
						if (pd.getType() instanceof IDataLinkedType)
						{
							getOrCreatePreprocessedPropertyInfoMap(IDataLinkedType.class).put(pd,
								((IDataLinkedType)pd.getType()).getDataLinks(ServoyJSONObject.jsonNullToNull(formElementValue), pd, fs, this));
						}

						if (pd.getType() instanceof IFindModeAwareType)
						{
							getOrCreatePreprocessedPropertyInfoMap(IFindModeAwareType.class).put(pd,
								((IFindModeAwareType)pd.getType()).isFindModeAware(ServoyJSONObject.jsonNullToNull(formElementValue), pd, fs, this));
						}
					}
					catch (Exception e)
					{
						Debug.error(e);
					}
				}

				if (inDesigner && pd.getType() == VisiblePropertyType.INSTANCE)
				{
					Object isVisibleObj = map.get(pd.getName());
					if (isVisibleObj instanceof Boolean)
					{
						isVisible = isVisible && ((Boolean)isVisibleObj).booleanValue();
						map.put(pd.getName(), Boolean.TRUE);
					}
				}
			}
		}
	}

	/**
	 * Get some standard info that was pre-processed at template stage but is needed at runtime.
	 *
	 * @param propertyInfoType for example {@link IFindModeAwareType}.class or {@link IDataLinkedType}.class.
	 * @return the pre-processed information object (it's type and meaning depends on propertyInfoType).
	 */
	public Object getPreprocessedPropertyInfo(Class< ? > propertyInfoType, PropertyDescription key)
	{
		if (preprocessedPropertyInfo == null) return null;
		Map<PropertyDescription, Object> m = preprocessedPropertyInfo.get(propertyInfoType);
		if (m == null) return null;

		return m.get(key);
	}

	public Map<PropertyDescription, Object> getOrCreatePreprocessedPropertyInfoMap(Class< ? > propertyInfoType)
	{
		if (preprocessedPropertyInfo == null) preprocessedPropertyInfo = new HashMap<>();
		Map<PropertyDescription, Object> m = preprocessedPropertyInfo.get(propertyInfoType);
		if (m == null)
		{
			m = new HashMap<>();
			preprocessedPropertyInfo.put(propertyInfoType, m);
		}
		return m;
	}

	private void adjustLocationRelativeToPart(FlattenedSolution fs, Map<String, Object> map)
	{
		if (map != null && form != null)
		{
			Form flatForm = fs.getFlattenedForm(form);
			Point location = getDesignLocation();
			if (location != null)
			{
				// if it is design client, it has no parts
				boolean isInDesginer = getDesignId() != null;
				if (isInDesginer)
				{
					map.put(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), location);
					map.put("offsetY", 0);
				}
				else
				{
					Point newLocation = new Point(location);
					Part part = flatForm.getPartAt(location.y);
					if (part != null)
					{
						int top = flatForm.getPartStartYPos(part.getID());
						newLocation.y = newLocation.y - top;
						map.put(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), newLocation);
						map.put("offsetY", top);
						map.put("partHeight", part.getHeight());
					}
				}
			}
		}
		adjustForAbsoluteLayout();
	}

	protected void adjustForAbsoluteLayout()
	{
		if (form != null && !form.isResponsiveLayout())
		{
			WebObjectSpecification spec = getWebComponentSpec();
			if (spec.getProperty("location") == null)
				spec.putProperty("location", new PropertyDescription("location", TypesRegistry.getType(PointPropertyType.TYPE_NAME)));
			if (spec.getProperty("size") == null)
				spec.putProperty("size", new PropertyDescription("size", TypesRegistry.getType(DimensionPropertyType.TYPE_NAME)));
			if (spec.getProperty("anchors") == null)
				spec.putProperty("anchors", new PropertyDescription("anchors", TypesRegistry.getType(IntPropertyType.TYPE_NAME)));

			// TODO the following is a workaround that allows not clearing the form element cache when reloading any ng package (so only when reloaded spec was actually altered here before and it might need to be re-altered again)
			WebComponentSpecProvider webComponentSpecProvider = WebComponentSpecProvider.getInstance();
			if (webComponentSpecProvider != null) webComponentSpecProvider.addSpecReloadListener(spec.getName(), ClearFormElementCacheWhenSpecChanges.INSTANCE);
		}
	}

	public Map<String, Object> getRawPropertyValues()
	{
		return propertyValues;
	}

	public WebObjectSpecification getWebComponentSpec()
	{
		return getWebComponentSpec(true);
	}

	public WebObjectSpecification getWebComponentSpec(boolean throwException)
	{
		WebObjectSpecification spec = null;
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
			return WebComponentSpecProvider.getInstance().getWebComponentSpecification(FormElement.ERROR_BEAN);
			//if (throwException) throw new IllegalStateException(errorMessage);
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

	public String getRawName()
	{
		return (String)getPropertyValue(StaticContentSpecLoader.PROPERTY_NAME.getPropertyName());
	}

	public String getName(String rawValue)
	{
		String name = rawValue;
		if (name == null)
		{
			if (getPersistIfAvailable() != null && getPersistIfAvailable().getUUID() != null &&
				Utils.getAsBoolean(Settings.getInstance().getProperty("servoy.ngclient.testingMode", "false")))
			{
				name = SVY_NAME_PREFIX + getPersistIfAvailable().getUUID();
			}
			else
			{
				name = SVY_NAME_PREFIX + uniqueIdWithinForm;
			}
		}
		if (Character.isDigit(name.charAt(0)))
		{
			name = "_" + name;
		}
		return name.replace('-', '_');
	}

	public String getDesignId()
	{
		if (inDesigner && getPersistIfAvailable() != null && getPersistIfAvailable().getUUID() != null)
		{
			return getPersistIfAvailable().getUUID().toString();
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
	 * It is possible that it will return a {@link IFromDesignToFormElement#TYPE_DEFAULT_VALUE_MARKER} in case
	 * the type has a default value bu there was no design value or spec. defined default value for this property.
	 */
	public Object getRawPropertyValue(String propertyName)
	{
		return propertyValues.get(propertyName);
	}

	/**
	 * Same as {@link #getRawPropertyValue(String)} but changes {@link IFromDesignToFormElement#TYPE_DEFAULT_VALUE_MARKER} to null.
	 * It is probably that this method can be removed once more types are refactored to use converters instead of hard-coded lines here and there.
	 */
	public Object getPropertyValue(String propertyName)
	{
		Object value = propertyValues.get(propertyName);
		return value == IDesignToFormElement.TYPE_DEFAULT_VALUE_MARKER ? null : value;
	}

	@Override
	public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
	{
		if (getWebComponentSpec() != null)
		{
			return getWebComponentSpec().getProperties(type);
		}
		return null;
	}

	@Override
	public PropertyDescription getProperty(String name)
	{
		if (getWebComponentSpec() != null)
		{
			return getWebComponentSpec().getProperty(name);
		}
		return null;
	}

	/**
	 * Returns sablo webcomponent value representation of this property value.
	 *
	 * This uses 'Conversion 3' (see {@link NGConversions})
	 */
	public Object getPropertyValueConvertedForWebComponent(String propertyName, WebFormComponent component, DataAdapterList dal)
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
			if (propertyDescription != null) return NGConversions.INSTANCE.convertFormElementToSabloComponentValue(getRawPropertyValue(propertyName),
				propertyDescription, this, component, dal);
			else return getPropertyValue(propertyName); // just in case this method gets called for events for example (which are currently stored in the same map)
		}

		if (propertyDescription != null)
		{
			// we want a defaut value to be set anyway because it was sent into template
			return propertyDescription.getType().defaultValue(propertyDescription);
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
		if ("servoydefault-button".equals(componentType) || "servoydefault-label".equals(componentType))
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
		WebObjectSpecification componentSpec = getWebComponentSpec();
		Set<String> events = componentSpec.getHandlers().keySet();
		for (String eventName : events)
		{
			Object eventValue = getPropertyValue(eventName);
			if (eventValue != null && !(eventValue instanceof Integer && (((Integer)eventValue).intValue() == -1 || ((Integer)eventValue).intValue() == 0)))
			{
				handlers.add(eventName);
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSGAINEDMETHODID.getPropertyName()) &&
				(getForm().getOnElementFocusGainedMethodID() > 0))
			{
				handlers.add(eventName);
			}
			else if (Utils.equalObjects(eventName, StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName()) &&
				(getForm().getOnElementFocusLostMethodID() > 0))
			{
				handlers.add(eventName);
			}
		}
		return handlers;
	}

	public String getPropertiesString() throws JSONException
	{
		return propertiesAsTemplateJSON(null, new FormElementContext(this)).toString();
	}

	public JSONWriter propertiesAsTemplateJSON(JSONWriter writer, FormElementContext context) throws JSONException
	{
		TypedData<Map<String, Object>> propertiesTypedData = propertiesForTemplateJSON();

		JSONWriter propertyWriter = (writer != null ? writer : new JSONStringer());
		try
		{
			propertyWriter.object();
			JSONUtils.writeDataWithConversions(FormElementToJSON.INSTANCE, propertyWriter, propertiesTypedData.content, propertiesTypedData.contentType,
				context);
			if (inDesigner) propertyWriter.key("svyVisible").value(isVisible);
			return propertyWriter.endObject();
		}
		catch (JSONException | IllegalArgumentException e)
		{
			Debug.error("Problem detected when handling a component's (" + getTagname() + ") properties / events.", e);
			throw e;
		}
	}

	public TypedData<Map<String, Object>> propertiesForTemplateJSON()
	{
		Map<String, Object> properties = new HashMap<>();

		adjustForAbsoluteLayout();
		WebObjectSpecification componentSpec = getWebComponentSpec();
		Map<String, PropertyDescription> propDescription = componentSpec.getProperties();
		for (PropertyDescription pd : propDescription.values())
		{
			Object val = getRawPropertyValue(pd.getName());
			if (val != null)
			{
				properties.put(pd.getName(), val);
			}
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

		Object partHeight = getPropertyValue("partHeight");
		if (partHeight != null) properties.put("partHeight", partHeight);

		Object formview = getPropertyValue("formview");
		if (formview != null) properties.put("formview", formview);

		// get types for conversion
		PropertyDescription propertyTypes = AggregatedPropertyType.newAggregatedProperty();
		for (Entry<String, Object> p : properties.entrySet())
		{
			PropertyDescription t = getWebComponentSpec().getProperty(p.getKey());
			if (t != null) propertyTypes.putProperty(p.getKey(), t);
		}


		return new TypedData<>(properties, propertyTypes.hasChildProperties() ? propertyTypes : null);
	}

	Dimension getDesignSize()
	{
		if (persistImpl != null && persistImpl.getPersist() instanceof ISupportSize)
		{
			if (persistImpl.getPersist() instanceof FormReference)
			{
				FormReference fr = (FormReference)persistImpl.getPersist();
				Form containedForm = fs.getForm(fr.getContainsFormID());
				if (containedForm != null)
				{
					int w = Math.max((int)fr.getSize().getWidth(), (int)containedForm.getSize().getWidth());
					int h = Math.max((int)fr.getSize().getHeight(), (int)containedForm.getSize().getHeight());
					return new Dimension(w, h);
				}
			}
			return ((ISupportSize)persistImpl.getPersist()).getSize();
		}
		return null;
	}

	Point getDesignLocation()
	{
		if (persistImpl != null && persistImpl.getPersist() instanceof ISupportBounds)
		{
			return ((ISupportBounds)persistImpl.getPersist()).getLocation();
		}
		return null;
	}

	@Override
	public String toString()
	{
		return String.format(
			"<%1$s name=\"%2$s\" svy-model=\"model.%2$s\" svy-api=\"api.%2$s\" svy-handlers=\"handlers.%2$s\" svy-servoyApi=\"handlers.%2$s.svy_servoyApi\"></%1$s>",
			getTagname(), getName());
	}

	/**
	 * This should not be called normally; it's only called because legacy portal needs to remove some prefix from the dataprovider properties of children.
	 */
	public void updatePropertyValuesDontUse(Map<String, Object> elementProperties)
	{
		this.propertyValues = elementProperties;
	}

	/**
	 * @return a list of accepted type names when dropping from palette. Possible type names include the types defined in the specfile and the "component" type.
	 */
	public List<String>[] getSvyTypesAndPropertiesNames()
	{
		WebObjectSpecification spec = getWebComponentSpec(false);
		List<String> typeNamesA = new ArrayList<String>();
		List<String> propertyNamesA = new ArrayList<String>();
		Map<String, PropertyDescription> properties = spec.getProperties();
		for (PropertyDescription propertyDescription : properties.values())
		{
			Object configObject = propertyDescription.getConfig();
			String simpleTypeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType());
			if (simpleTypeName.equals("component") || (configObject instanceof JSONObject && Boolean.TRUE.equals(((JSONObject)configObject).opt(DROPPABLE)) &&
				PropertyUtils.isCustomJSONProperty(propertyDescription.getType())))
			{
				typeNamesA.add(simpleTypeName);
				propertyNamesA.add(propertyDescription.getName());
			}
		}
		return new List[] { typeNamesA, propertyNamesA };
	}

	public List<String> getForbiddenComponentNames()
	{
		ArrayList<String> result = new ArrayList<String>();
		// TODO:maybe we can add this kind of info to the spec ?
		// or maybe have a list of 'allowed' components ?
		if ("servoycore-portal".equals(getTypeName()))
		{
			result.add("servoycore-portal");
			result.add("servoydefault-tabpanel");
			result.add("servoydefault-splitpane");
		}
		return result;
	}

	public FlattenedSolution getFlattendSolution()
	{
		return fs;
	}

	/**
	 * Returns true if the give property description makes use of types that are turned into persists when used in persist properties.
	 * For example component property types, component array, custom object and custom object arrays return Persist instances when accessed through WebComponent.getProperty(...)
	 *
	 * @param pd the property description to look in.
	 * @return see description.
	 */
	public static boolean usesPersistTypedProperties(PropertyDescription pd)
	{
		if (pd == null) return false;

		for (PropertyDescription x : pd.getCustomJSONProperties().values())
		{
			if (PropertyUtils.isCustomJSONObjectProperty(x.getType()) || x.getType() instanceof ComponentPropertyType ||
				(PropertyUtils.isCustomJSONArrayPropertyType(x.getType()) &&
					usesPersistTypedProperties(((ICustomType< ? >)x.getType()).getCustomJSONTypeDefinition())))
				return true;
		}

		return false;
	}

	private static class ClearFormElementCacheWhenSpecChanges implements ISpecReloadListener
	{

		public static final ClearFormElementCacheWhenSpecChanges INSTANCE = new ClearFormElementCacheWhenSpecChanges();

		@Override
		public void webObjectSpecificationReloaded()
		{
			FormElementHelper.INSTANCE.reload();
		}

	}

}
