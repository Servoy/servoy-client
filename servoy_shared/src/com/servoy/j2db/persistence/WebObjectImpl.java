/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.j2db.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.utils.JSONUtils;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;


/**
 * Common implementation for spec/property description based persists that helps working with JSON content and nested persists.
 *
 * @author acostescu
 */
public class WebObjectImpl extends WebObjectBasicImpl
{

	private final Map<String, Object> persistMappedProperties = new HashMap<String, Object>(); // value can be IChildWebObject or IChildWebObject[] (ChildWebComponents or WebCustomTypes)

	// TODO should we have a map that contains all values from the JSON not only the above for IChildWebObject - or at least get/set/clear/has should handle all json values,
	//	not just persist mapped one (see commented out code in those methods)

	private boolean arePersistMappedPropertiesLoaded = false;
	private final boolean pdIsWebComponentSpecification;
	private PropertyDescription pdPleaseUseGetterToAccessThis;
	private Map<UUID, IPersist> persistMappedPropetiesByUUID = null; // cached just like in AbstractBase

	private boolean gettingTypeName;

	private boolean skipPersistMappedPropertiesUpdate = false;

	/**
	 * This constructor is to be used if getTypeName is the name of a WebComponent. (so it can be used to get the component spec)
	 */
	public WebObjectImpl(IBasicWebObject webObject)
	{
		super(webObject);
		pdIsWebComponentSpecification = true;
		WebObjectRegistry.registerWebObject(this);
	}

	public WebObjectImpl(IBasicWebObject webObject, Object specPD)
	{
		super(webObject);
		this.pdPleaseUseGetterToAccessThis = (PropertyDescription)specPD;
		this.pdIsWebComponentSpecification = false;
		WebObjectRegistry.registerWebObject(this);
	}

	public PropertyDescription getPropertyDescription()
	{
		// at the time WebComponent is created the resources project is not yet loaded nor is the typeName property set; so find it when it's needed in this case
		if (pdPleaseUseGetterToAccessThis == null && !gettingTypeName)
		{
			gettingTypeName = true;
			try
			{
				SpecProviderState specProviderState = WebComponentSpecProvider.getSpecProviderState();
				pdPleaseUseGetterToAccessThis = specProviderState == null ? null : specProviderState.getWebComponentSpecification(getTypeName());
			}
			finally
			{
				gettingTypeName = false;
			}
		}

		return pdPleaseUseGetterToAccessThis;
	}

	protected PropertyDescription getChildPropertyDescription(String propertyName)
	{
		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription != null) return propertyDescription.getProperty(propertyName);
		return null;
	}

	@Override
	public void updateJSONFromPersistMappedPropeties()
	{
		// writes persist mapped properties back to json
		if (arePersistMappedPropertiesLoaded && !skipPersistMappedPropertiesUpdate)
		{
			JSONObject old = (JSONObject)webObject.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			try
			{
				JSONObject entireModel = (old != null ? old : new ServoyJSONObject()); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties
				Iterator<String> it = entireModel.keys();

				// remove custom properties that were removed (be sure to keep any keys that do not map to custom properties or arrays of custom properties - for example ints, string arrays and so on)
				List<String> toRemove = new ArrayList<String>();
				while (it.hasNext())
				{
					String key = it.next();
					if (isPersistMappedProperty(key) && !getPersistMappedProperties().containsKey(key)) toRemove.add(key);
				}
				for (String key : toRemove)
				{
					entireModel.remove(key);
				}

				for (Map.Entry<String, Object> wo : getPersistMappedProperties().entrySet())
				{
					if (wo.getValue() == null) entireModel.put(wo.getKey(), JSONObject.NULL);
					else if (wo.getValue() instanceof IChildWebObject)
					{
						entireModel.put(wo.getKey(), ((IChildWebObject)wo.getValue()).getFullJsonInFrmFile());
					}
					else
					{
						ServoyJSONArray jsonArray = new ServoyJSONArray();
						int i = 0;
						for (IChildWebObject wo1 : (IChildWebObject[])wo.getValue())
						{
							if (wo1 != null)
							{
								// make sure index and jsonKey are correct; (for example we don't have
								// enough info when creating a new custom object in an array element in properties view)
								// and in that case we create a WebCustomType with incorrect index/jsonkey; see CustomObjectTypePropertyController.toggleValue
								wo1.setIndex(i);
								wo1.setJsonKey(wo.getKey());
							}
							i++;
							jsonArray.put(wo1 != null ? wo1.getFullJsonInFrmFile() : JSONObject.NULL);
						}
						entireModel.put(wo.getKey(), jsonArray);
					}

					PropertyDescription childPd = getChildPropertyDescription(wo.getKey());
					// if we are about to store a default value, don't (so if all the persists turned into a JSON equal to
					// the one defined in .spec file as default value, don't write it...)
					if (childPd.hasDefault() && JSONUtils.areEqual(childPd.getDefaultValue(), entireModel.opt(wo.getKey()), IChildWebObject.UUID_KEY))
					{
						entireModel.remove(wo.getKey());
					}
				}
				setJsonInternal(entireModel);
				((AbstractBase)webObject).flagChanged();
				updateParentJSON();
			}
			catch (JSONException ex)
			{
				Debug.error(ex);
			}
		}
	}

	public static boolean isPersistMappedProperty(PropertyDescription childPd)
	{
		return isCustomJSONObjectOrArrayOfCustomJSONObject(childPd) || isComponentOrArrayOfComponent(childPd);
	}

	protected boolean isPersistMappedProperty(String key)
	{
		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription == null) return false; // typeName is not yet set; so normally typed properties are not yet accessed

		PropertyDescription childPd = propertyDescription.getProperty(key);
		return isCustomJSONObjectOrArrayOfCustomJSONObject(childPd) || isComponentOrArrayOfComponent(childPd);
	}

	protected static boolean isArrayOfCustomJSONObject(IPropertyType< ? > propertyType)
	{
		boolean arrayReturnType = PropertyUtils.isCustomJSONArrayPropertyType(propertyType);
		if (arrayReturnType)
		{
			PropertyDescription elementPD = (propertyType instanceof ICustomType< ? >) ? ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition() : null;
			if (elementPD != null && PropertyUtils.isCustomJSONObjectProperty(elementPD.getType()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Return true if this property is handled as a custom property persist and false otherwise.
	 * @param childPd the subproperty pd.
	 * @return true if this property is handled as a custom property persist and false otherwise.
	 */
	protected static boolean isCustomJSONObjectOrArrayOfCustomJSONObject(PropertyDescription childPd)
	{
		if (childPd != null)
		{
			IPropertyType< ? > propertyType = childPd.getType();
			return PropertyUtils.isCustomJSONObjectProperty(propertyType) || isArrayOfCustomJSONObject(propertyType);
		}
		return false;
	}

	protected static boolean isComponent(IPropertyType< ? > propertyType)
	{
		return ChildWebComponent.COMPONENT_PROPERTY_TYPE_NAME.equals(propertyType.getName());
	}

	protected static boolean isArrayOfComponent(IPropertyType< ? > propertyType)
	{
		boolean arrayReturnType = PropertyUtils.isCustomJSONArrayPropertyType(propertyType);
		if (arrayReturnType)
		{
			PropertyDescription elementPD = (propertyType instanceof ICustomType< ? >) ? ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition() : null;
			if (elementPD != null && ChildWebComponent.COMPONENT_PROPERTY_TYPE_NAME.equals(elementPD.getType().getName()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Return true if this property is handled as a custom property persist and false otherwise.
	 * @param childPd the subproperty pd.
	 * @return true if this property is handled as a custom property persist and false otherwise.
	 */
	protected static boolean isComponentOrArrayOfComponent(PropertyDescription childPd)
	{
		if (childPd != null)
		{
			IPropertyType< ? > propertyType = childPd.getType();
			return isComponent(propertyType) || isArrayOfComponent(propertyType);
		}
		return false;
	}

	/**
	 * Returns false if it can't set is as a json property. Then caller should set it as another standard persist property.
	 */
	@Override
	public boolean setProperty(String propertyName, Object val)
	{
		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription != null)
		{
			PropertyDescription childPd = propertyDescription.getProperty(propertyName);
			if (childPd == null && propertyDescription instanceof WebObjectSpecification &&
				((WebObjectSpecification)propertyDescription).getHandler(propertyName) != null)
				childPd = ((WebObjectSpecification)propertyDescription).getHandler(propertyName).getAsPropertyDescription();
			if (childPd != null)
			{
				IPropertyType< ? > propertyType = childPd.getType();
				if ((val == null && isPersistMappedProperty(propertyName)) ||
					(val instanceof WebCustomType && PropertyUtils.isCustomJSONObjectProperty(propertyType)) ||
					(val instanceof IChildWebObject[] && isArrayOfCustomJSONObject(propertyType)) ||
					(val instanceof WebComponent && isComponent(propertyType)) || (val instanceof IChildWebObject[] && isArrayOfComponent(propertyType)))
				{
					if (getJson() == null) setJson(new ServoyJSONObject());
					getPersistMappedProperties().put(propertyName, val);
					persistMappedPropetiesByUUID = null;
					updateJSONFromPersistMappedPropeties();
					return true;
				}
				else
				{
					// it is a json property defined in spec, but it's not mapping to a persist
					setOrRemoveJsonSubproperty(propertyName, convertFromJavaType(getChildPropertyDescription(propertyName), val), false);
					return true;
				}
			}
		}
		return false; // typeName is not yet set (so normally typed properties are not yet accessed) or it's not a typed property
	}

	/**
	 * Returns false if it can't clear this as json property (it is something else). Then caller should just clear it as another standard persist property.
	 */
	@Override
	public boolean clearProperty(String propertyName)
	{
		Map<String, Object> persistMappedProperties = getPersistMappedProperties();
		if (persistMappedProperties.containsKey(propertyName))
		{
			persistMappedProperties.remove(propertyName); // remove the persist
			persistMappedPropetiesByUUID = null;
			updateJSONFromPersistMappedPropeties(); // remove it from json (as child persist is no longer there)

			JSONObject json = getJson(); // this actually gets flattened json (ends up in AbstractBase that does that)
			if (!ServoyJSONObject.isJavascriptNullOrUndefined(json) && json.has(propertyName))
				updatePersistMappedPropertyFromJSON(propertyName, json.get(propertyName)); // in case there is an inherited value for this key set back inherited value
			return true;
		}
		else
		{
			PropertyDescription propertyDescription = getPropertyDescription();
			if (propertyDescription != null)
			{
				// IMPORTANT if we decide that this method shouldn't affect all json properties and we remove the following code, we have to update code
				// in CustomJSONObjectTypePropertyController.CustomJSONObjectPropertySource.defaultResetProperty(Object) because underlyingPropertySource.defaultResetProperty(id);
				// depends on this in the end (the same for WebComponentPropertySource)
				PropertyDescription childPd = propertyDescription.getProperty(propertyName);
				if (childPd == null && propertyDescription instanceof WebObjectSpecification)
				{
					if (((WebObjectSpecification)propertyDescription).getHandler(propertyName) != null)
						childPd = ((WebObjectSpecification)propertyDescription).getHandler(propertyName).getAsPropertyDescription();
				}
				if (childPd != null)
				{
					// it is a json property defined in spec, but it's not mapping to a persist
					return setOrRemoveJsonSubproperty(propertyName, null, true);
				}
			}
		}
		return false;
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		boolean hasIt = false;
		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription != null)
		{
			PropertyDescription childPd = propertyDescription.getProperty(propertyName);
			if (childPd != null)
			{
				// it is a json property defined in spec, but it's not mapping to a persist
				JSONObject json = (JSONObject)webObject.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
				hasIt = (json != null && json.has(propertyName));
			}
		}
		return hasIt;
	}

	@Override
	public Object getProperty(String propertyName)
	{
		Map<String, Object> ctp = getPersistMappedProperties();
		if (ctp.containsKey(propertyName)) return ctp.get(propertyName);

		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription != null)
		{
			PropertyDescription childPd = propertyDescription.getProperty(propertyName);
			if (childPd == null && propertyDescription instanceof WebObjectSpecification &&
				((WebObjectSpecification)propertyDescription).getHandler(propertyName) != null)
				childPd = ((WebObjectSpecification)propertyDescription).getHandler(propertyName).getAsPropertyDescription();
			if (childPd != null)
			{
				// it is a json property defined in spec, but it's not mapping to a persist (web component/web custom type/array of those)
				JSONObject json = getJson();
				Object value = json != null ? json.opt(propertyName) : null;
				if (value == null && json != null && childPd.hasDefault() && isPersistMappedProperty(propertyName))
				{
					// if the value is null (for org.json this means undefined/unavailable) and this child has a default custom json property (array or object) then
					// we have to make that right now from the default so we get the persist mapped properties filled up for the properties view in designer
					Object defaultValue = childPd.getDefaultValue();
					if (defaultValue instanceof JSONObject || (defaultValue instanceof JSONArray &&
						PropertyUtils.isCustomJSONProperty(((ICustomType)childPd.getType()).getCustomJSONTypeDefinition().getType())))
					{
						defaultValue = ServoyJSONObject.deepCloneJSONArrayOrObj(defaultValue);

						// IMPORTANT: PersistInheritenceDelegateLabelProvider.getText(...) uses the fact that json is not modified for child components here (so json.put below is commented out),
						// which means that the json of the default child persist is not linked to this (parent) json in order to determine if a child persist is a default value or not
						// if you modify this update that code as well accordingly
						// json.put(propertyName, defaultValue); // we don't add the value to json as we don't want in the future to store the default values in .frm file
						updatePersistMappedPropertyFromJSON(propertyName, defaultValue);

						ctp = getPersistMappedProperties(); // should be the same instance anyway
						return ctp.get(propertyName);
					}
				} // we do not handle default values from .spec here for other types of values because those are done in designer code directly (they
					// don't need to generate persists so they are easier); see PersistPropertySource.getDefaultPersistValue(...) and WebComponentPropertyHandler.getValue(...)
					// they are also handled separately at runtime; maybe we should actually return defaults from spec here for non-persist mappped properties as well
					// in the future and remove the code from designer and runtime for them...

				value = convertToJavaType(childPd, value);
				return value;
			}
		}

		return null;
	}

	@Override
	public Object getPropertyDefaultValue(String propertyName)
	{
		PropertyDescription pd = getPropertyDescription();
		PropertyDescription propPD = pd != null ? pd.getProperty(propertyName) : null;
		return propPD != null && propPD.hasDefault() ? ServoyJSONObject.deepCloneJSONArrayOrObj(propPD.getDefaultValue()) : null;
	}

	public static Object convertToJavaType(PropertyDescription childPd, Object val)
	{
		Object value = val;
		IDesignValueConverter< ? > converter = null;
		if (value != null && childPd != null && (converter = getConverter(childPd)) != null)
		{
			value = converter.fromDesignValue(value, childPd);
		}
		return (value != JSONObject.NULL) ? value : null;
	}

	private static IDesignValueConverter< ? > getConverter(PropertyDescription pd)
	{
		return (pd.getType() instanceof IDesignValueConverter< ? >) ? (IDesignValueConverter< ? >)pd.getType() : null;
	}

	@Override
	public List<IChildWebObject> getAllPersistMappedProperties()
	{
		ArrayList<IChildWebObject> allPersistMappedProperties = new ArrayList<IChildWebObject>();
		for (Object wo : getPersistMappedProperties().values())
		{
			if (wo != null)
			{
				if (wo.getClass().isArray())
				{
					for (IChildWebObject t : (IChildWebObject[])wo)
					{
						if (t != null) allPersistMappedProperties.add(t);
					}
				}
				else
				{
					allPersistMappedProperties.add((IChildWebObject)wo);
				}
			}
		}

		return allPersistMappedProperties;
	}

	private Map<String, Object> getPersistMappedProperties()
	{
		if (!arePersistMappedPropertiesLoaded)
		{
			arePersistMappedPropertiesLoaded = true; // do this here rather then later to avoid stack overflows in case code below end up calling persist.getProperty() again

			if (getPropertyDescription() != null && getJson() != null)
			{
				JSONObject webObjectFlattenedJSON = webObject.getFlattenedJson();
				try
				{
					for (String jsonKey : ServoyJSONObject.getNames(webObjectFlattenedJSON))
					{
						Object object = webObjectFlattenedJSON.get(jsonKey);
						updatePersistMappedPropertyFromJSON(jsonKey, object);
					}
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
			}
			else arePersistMappedPropertiesLoaded = false; // maybe the solution is being activated as we speak and the property descriptions from resources project are not yet available...
		}

		return persistMappedProperties;
	}

	protected void updatePersistMappedPropertyFromJSON(String beanJSONKey, Object object)
	{
		if (object != null && getChildPropertyDescription(beanJSONKey) != null)
		{
			PropertyDescription childPd = getChildPropertyDescription(beanJSONKey);
			IPropertyType< ? > propertyType = childPd.getType();
			String simpleTypeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyType);
			if (isPersistMappedProperty(beanJSONKey))
			{
				if (ServoyJSONObject.isJavascriptUndefined(object))
				{
					persistMappedProperties.remove(beanJSONKey);
					persistMappedPropetiesByUUID = null;
				}
				else if (ServoyJSONObject.isJavascriptNull(object))
				{
					persistMappedProperties.put(beanJSONKey, null);
					persistMappedPropetiesByUUID = null;
				}
				else
				{
					boolean arrayReturnType = PropertyUtils.isCustomJSONArrayPropertyType(propertyType);
					if (!arrayReturnType)
					{
						IChildWebObject childWebObject;
						IChildWebObject existingWebObject = null;

						// find the UUID that is stored for that key already in the .frm file - if any
						UUID childChildWebObjectUUID = null;
						JSONObject childWebObjectJSON = WebObjectImpl.getFullJSONInFrmFile(webObject, beanJSONKey, -1, false);
						if (childWebObjectJSON != null && childWebObjectJSON.has(IChildWebObject.UUID_KEY))
						{
							try
							{
								childChildWebObjectUUID = UUID.fromString(childWebObjectJSON.getString(IChildWebObject.UUID_KEY));
							}
							catch (IllegalArgumentException ex)
							{
								Debug.error(ex);
							}
							if (childChildWebObjectUUID != null)
							{
								IChildWebObject currentPersist = null;
								if (persistMappedProperties.containsKey(beanJSONKey) && persistMappedProperties.get(beanJSONKey) instanceof IChildWebObject)
								{
									currentPersist = (IChildWebObject)persistMappedProperties.get(beanJSONKey);
									if (currentPersist != null && childChildWebObjectUUID.equals(currentPersist.getUUID()))
									{
										existingWebObject = currentPersist;
									}
								}
							}
						}

						if (existingWebObject == null)
						{
							// "object" is not null/undefined here - there are checks for that above; so create the needed persists
							if (isComponent(propertyType))
							{
								childWebObject = ChildWebComponent.createNewInstance(webObject, childPd, beanJSONKey, -1, false, childChildWebObjectUUID);
								persistMappedProperties.put(beanJSONKey, childWebObject);
								persistMappedPropetiesByUUID = null;
							}
							else if (PropertyUtils.isCustomJSONObjectProperty(propertyType))
							{
								childWebObject = WebCustomType.createNewInstance(webObject, childPd, beanJSONKey, -1, false, childChildWebObjectUUID);
								childWebObject.setTypeName(simpleTypeName);
								persistMappedProperties.put(beanJSONKey, childWebObject);
								persistMappedPropetiesByUUID = null;
							}
						}
					}
					else if (object instanceof JSONArray)
					{
						PropertyDescription elementPD = (propertyType instanceof ICustomType< ? >)
							? ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition() : null;
						if (elementPD != null)
						{
							ArrayList<IChildWebObject> persistMappedPropertyArray = new ArrayList<IChildWebObject>();

							if (PropertyUtils.isCustomJSONObjectProperty(elementPD.getType()) || isComponent(propertyType))
							{
								ArrayList<IChildWebObject> currentPersistMappedPropertyArray = new ArrayList<IChildWebObject>();
								if (persistMappedProperties.containsKey(beanJSONKey) && persistMappedProperties.get(beanJSONKey) instanceof IChildWebObject[])
								{
									currentPersistMappedPropertyArray.addAll(Arrays.asList((IChildWebObject[])persistMappedProperties.get(beanJSONKey)));
								}

								for (int i = 0; i < ((JSONArray)object).length(); i++)
								{
									IChildWebObject existingWebObject = null;
									UUID childChildWebObjectUUID = null;
									JSONObject childWebObjectJSON = WebObjectImpl.getFullJSONInFrmFile(webObject, beanJSONKey, i, false);
									if (childWebObjectJSON != null && childWebObjectJSON.has(IChildWebObject.UUID_KEY))
									{
										try
										{
											childChildWebObjectUUID = UUID.fromString(childWebObjectJSON.getString(IChildWebObject.UUID_KEY));
										}
										catch (IllegalArgumentException ex)
										{
											Debug.error(ex);
										}
										if (childChildWebObjectUUID != null)
										{
											for (IChildWebObject wo : currentPersistMappedPropertyArray)
											{
												if (wo != null && childChildWebObjectUUID.equals(wo.getUUID()))
												{
													persistMappedPropertyArray.add(wo);
													existingWebObject = wo;
													existingWebObject.setIndex(i); // just to be sure we didn't find it at some other index
													break;
												}
											}
										}
									}

									if (existingWebObject == null)
									{
										// we couldn't find an old array item with the same UUID; add a fresh value
										Object newJSNOValAtIdx = ((JSONArray)object).opt(i);
										if (ServoyJSONObject.isJavascriptNullOrUndefined(newJSNOValAtIdx))
										{
											persistMappedPropertyArray.add(null);
										}
										else
										{
											if (PropertyUtils.isCustomJSONObjectProperty(elementPD.getType()))
											{
												WebCustomType webCustomType = WebCustomType.createNewInstance(webObject, elementPD, beanJSONKey, i, false,
													childChildWebObjectUUID);
												webCustomType.setTypeName(simpleTypeName);
												persistMappedPropertyArray.add(webCustomType);
											}
											else if (isComponent(propertyType))
											{
												ChildWebComponent childComponent = ChildWebComponent.createNewInstance(webObject, elementPD, beanJSONKey, i,
													false, childChildWebObjectUUID);
												persistMappedPropertyArray.add(childComponent);
											}
										}
									}
								}
							}
							persistMappedProperties.put(beanJSONKey,
								persistMappedPropertyArray.toArray(new IChildWebObject[persistMappedPropertyArray.size()]));
							persistMappedPropetiesByUUID = null;
						}
					}
					else
					{
						Debug.error("Typed property value ('" + beanJSONKey + "') is not JSONArray although in spec it is defined as array... " + this + " - " +
							object);
					}
				}
			}
		}
		else
		{
			if (persistMappedProperties.remove(beanJSONKey) != null) persistMappedPropetiesByUUID = null;
		}
	}

	@Override
	public void reload()
	{
		arePersistMappedPropertiesLoaded = false;
		if (pdIsWebComponentSpecification) pdPleaseUseGetterToAccessThis = null; // if it's a web component, next time it's needed reload it's spec as well, don't use cached one
	}

	@Override
	public void setJson(JSONObject arg)
	{
		setJsonInternal(arg);

		if (arePersistMappedPropertiesLoaded)
		{
			if (arg != null)
			{
				try
				{
					skipPersistMappedPropertiesUpdate = true;
					HashSet<String> obsoletePersistMappedPropertyNames = new HashSet<String>(persistMappedProperties.keySet());
					for (String beanJSONKey : ServoyJSONObject.getNames(arg))
					{
						Object object = arg.get(beanJSONKey);
						updatePersistMappedPropertyFromJSON(beanJSONKey, object);
						obsoletePersistMappedPropertyNames.remove(beanJSONKey);
					}

					// delete all old properties that are no longer present in the new JSON
					for (String obsoleteKey : obsoletePersistMappedPropertyNames)
					{
						persistMappedProperties.remove(obsoleteKey);
						persistMappedPropetiesByUUID = null;
					}
				}
				finally
				{
					skipPersistMappedPropertiesUpdate = false;
				}
			}
			else
			{
				persistMappedProperties.clear();
			}
		}

		updateParentJSON();
	}

	@Override
	public void setJsonSubproperty(String key, Object value)
	{
		setOrRemoveJsonSubproperty(key, value, false);
	}

	private boolean setOrRemoveJsonSubproperty(String key, Object value, boolean remove)
	{
		try
		{
			boolean removed = false;

			JSONObject oldJson = (JSONObject)webObject.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());

			// we can no longer check for differences here as we now reuse JSON objects/arrays
			JSONObject jsonObject = (oldJson == null ? new ServoyJSONObject() : oldJson); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties

			ServoyJSONObject oldJsonClone = null;
			if (oldJson instanceof ServoyJSONObject)
			{
				oldJsonClone = ((ServoyJSONObject)oldJson).clone();
			}

			if (remove)
			{
				removed = (jsonObject.remove(key) != null);
			}
			else
			{
				jsonObject.put(key, ServoyJSONObject.nullToJsonNull(value)); // if value is "null" really allow storing null in there; otherwise jsonObject.put(key, null) will be equivalent to removing the value (useful for when in spec there is a non-null default value given and user chooses null)
			}

			if (oldJsonClone == null || !oldJsonClone.equals(jsonObject))
			{
				setJsonInternal(jsonObject);
				((AbstractBase)webObject).flagChanged();
			}

			if (arePersistMappedPropertiesLoaded && getPropertyDescription() != null)
			{
				updatePersistMappedPropertyFromJSON(key, value); // update this web object's child web objects if needed (if this key affects them)
			} // else not yet loaded - they will all load later so nothing to do here

			updateParentJSON();
			return removed;
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return false;
	}

	private void updateParentJSON()
	{
		// update JSON property of all parent web objects as all this web object hierarchy is actually described by top-most web object JSON property
		// and the JSON of each web object should never get out-of-sync with the child web objects it contains
		ISupportChilds parent = webObject.getParent();
		if (parent instanceof IBasicWebObject) ((IBasicWebObject)parent).updateJSON();
	}

	public static Object convertFromJavaType(PropertyDescription pd, Object value)
	{
		if (pd != null && getConverter(pd) != null)
		{
			return getConverter(pd).toDesignValue(value, pd);
		}
		return value;
	}

	@Override
	public boolean removeJsonSubproperty(String key)
	{
		return setOrRemoveJsonSubproperty(key, null, true);
	}

	@Override
	public void setJsonInternal(JSONObject arg)
	{
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	@Override
	public void internalAddChild(IPersist obj)
	{
		if (obj instanceof IChildWebObject)
		{
			IChildWebObject persistMappedPropertyValue = (IChildWebObject)obj;
			IPropertyType< ? > type = persistMappedPropertyValue.getPropertyDescription().getType();

			if (getPropertyDescription().isArrayReturnType(persistMappedPropertyValue.getJsonKey()))
			{
				if (type == ((CustomJSONArrayType< ? , ? >)getChildPropertyDescription(
					persistMappedPropertyValue.getJsonKey()).getType()).getCustomJSONTypeDefinition().getType())
				{
					Object children = getPersistMappedProperties().get(persistMappedPropertyValue.getJsonKey());
					if (children == null) children = new IChildWebObject[0];
					if (children instanceof IChildWebObject[])
					{
						List<IChildWebObject> t = new ArrayList<IChildWebObject>(Arrays.asList((IChildWebObject[])children));
						t.add(persistMappedPropertyValue.getIndex(), persistMappedPropertyValue);
						for (int i = persistMappedPropertyValue.getIndex() + 1; i < t.size(); i++)
						{
							IChildWebObject ct = t.get(i);
							ct.setIndex(i);
						}
						setProperty(persistMappedPropertyValue.getJsonKey(), t.toArray(new IChildWebObject[t.size()]));
					}
					else
					{
						Debug.error("Unexpected array property persist value: " + children);
					}
				}
				else
				{
					Debug.error("Element type (" +
						((CustomJSONArrayType< ? , ? >)getChildPropertyDescription(
							persistMappedPropertyValue.getJsonKey()).getType()).getCustomJSONTypeDefinition().getType() +
						") does not match persist-to-add type: " + type + " - " + webObject);
				}
			}
			else
			{
				if (type == getChildPropertyDescription(persistMappedPropertyValue.getJsonKey()).getType())
				{
					setProperty(persistMappedPropertyValue.getJsonKey(), persistMappedPropertyValue);
				}
				else
				{
					Debug.error("Property type (" + getChildPropertyDescription(persistMappedPropertyValue.getJsonKey()).getType() +
						") does not match persist-to-add type: " + type + " - " + webObject);
				}
			}
			persistMappedPropetiesByUUID = null;
		}
		else
		{
			Debug.error("Trying to add non - IChildWebObject to a WebObject: " + obj + ", " + webObject);
		}
	}

	@Override
	public void internalRemoveChild(IPersist obj)
	{
		if (obj instanceof IChildWebObject)
		{
			IChildWebObject customType = (IChildWebObject)obj;
			Object children = getPersistMappedProperties().get(customType.getJsonKey());
			if (children != null)
			{
				if (children instanceof IChildWebObject[])
				{
					List<IChildWebObject> t = new ArrayList<IChildWebObject>(Arrays.asList((IChildWebObject[])children));
					t.remove(customType);
					for (int i = customType.getIndex(); i < t.size(); i++)
					{
						IChildWebObject ct = t.get(i);
						ct.setIndex(i);
					}
					setProperty(customType.getJsonKey(), t.toArray(new IChildWebObject[t.size()]));
				}
				else
				{
					clearProperty(customType.getJsonKey());
				}
			}
			persistMappedPropetiesByUUID = null;
		}
		else
		{
			Debug.error("Trying to remove non - IChildWebObject from a WebObject: " + obj + ", " + webObject);
		}
	}

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		final Iterator<Object> it1 = getPersistMappedProperties().values().iterator();

		return new CustomTypesIterator(it1);
	}

	protected static class CustomTypesIterator implements Iterator<IPersist>
	{

		IChildWebObject nextSingleObj;

		IChildWebObject[] nextArrayObj;
		int nextArrayObjIdx = 0;

		private final Iterator<Object> it;

		public CustomTypesIterator(Iterator<Object> it)
		{
			this.it = it;
			prepareNext();
		}

		protected void prepareNext()
		{
			if (nextArrayObj != null)
			{
				// find next non-null in array
				while (nextArrayObjIdx < nextArrayObj.length && nextArrayObj[nextArrayObjIdx] == null)
				{
					nextArrayObjIdx++;
				}
			}

			if (!hasArrayItems())
			{
				nextArrayObj = null;
				nextArrayObjIdx = 0;
				nextSingleObj = null;

				if (it.hasNext())
				{
					Object o = it.next();
					while (!prepareNextItemFromElement(o) && it.hasNext())
					{
						o = it.next();
					}
				}
			}
		}

		protected boolean prepareNextItemFromElement(Object o)
		{
			if (o instanceof IChildWebObject)
			{
				nextSingleObj = (IChildWebObject)o;
				return true;
			}
			else if (o instanceof IChildWebObject[])
			{
				IChildWebObject[] arr = (IChildWebObject[])o;
				int i = 0;
				while (i < arr.length)
				{
					if (arr[i] != null)
					{
						nextArrayObj = arr;
						nextArrayObjIdx = i;
						return true;
					}
				}
				return false;
			}
			return false;
		}

		private boolean hasArrayItems()
		{
			return nextArrayObj != null && nextArrayObjIdx < nextArrayObj.length;
		}

		@Override
		public boolean hasNext()
		{
			return (nextSingleObj != null || hasArrayItems());
		}

		@Override
		public IPersist next()
		{
			if (hasNext())
			{
				IChildWebObject toReturn;
				if (nextSingleObj != null) toReturn = nextSingleObj;
				else toReturn = nextArrayObj[nextArrayObjIdx++];

				prepareNext();

				return toReturn;
			}
			else throw new NoSuchElementException();
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public IPersist getChild(UUID childUuid)
	{
		if (persistMappedPropetiesByUUID == null)
		{
			Iterator<IPersist> allobjects = getAllObjects();
			if (allobjects != null && allobjects.hasNext())
			{
				persistMappedPropetiesByUUID = new ConcurrentHashMap<UUID, IPersist>(persistMappedProperties.size(), 0.9f, 16);
				while (allobjects.hasNext())
				{
					IPersist persist = allobjects.next();
					if (persist != null)
					{
						persistMappedPropetiesByUUID.put(persist.getUUID(), persist);
					}
				}
			}
		}
		return persistMappedPropetiesByUUID == null ? null : persistMappedPropetiesByUUID.get(childUuid);
	}

	public static Pair<Integer, UUID> getNewIdAndUUID(IPersist persist)
	{
		UUID uuid = UUID.randomUUID();
		int id;
		try
		{
			id = ((IPersistFactory)((Solution)persist.getAncestor(IRepository.SOLUTIONS)).getRepository()).getNewElementID(uuid);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			id = 0;
		}
		return new Pair<>(Integer.valueOf(id), uuid);
	}

	private static JSONObject getFullJSONInFrmFile(IBasicWebObject parentWebObject, String jsonKey, int index, boolean isNew)
	{
		try
		{
			JSONObject entireModel = (JSONObject)parentWebObject.getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			if (entireModel == null) entireModel = new ServoyJSONObject();

			if (!isNew)
			{
				Object v;

				if (entireModel.has(jsonKey)) v = entireModel.opt(jsonKey);
				else
				{
					// see if there is a default value in spec for this property
					v = parentWebObject.getPropertyDefaultValueClone(jsonKey);
					if (v == null) v = new ServoyJSONObject();
				}

				JSONObject obj = null;
				if (v instanceof JSONArray)
				{
					obj = ((JSONArray)v).optJSONObject(index);
				}
				else
				{
					obj = (JSONObject)v;
				}
				return obj;
			}
			else
			{
				return new ServoyJSONObject();
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return null;
	}

	public static JSONObject getFullJSONInFrmFile(IChildWebObject webObject, boolean isNew)
	{
		return getFullJSONInFrmFile(webObject.getParent(), webObject.getJsonKey(), webObject.getIndex(), isNew);
	}

}
