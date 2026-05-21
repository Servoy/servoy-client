/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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


import java.awt.Dimension;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 */
public class WebComponent extends BaseComponent implements IWebComponent, ICommonWebComponent
{

	private static final long serialVersionUID = 1L;

	protected static Set<String> purePersistPropertyNames;

	static
	{
		try
		{
			purePersistPropertyNames = RepositoryHelper.getSettersViaIntrospection(WebComponent.class).keySet();
		}
		catch (IntrospectionException e)
		{
			purePersistPropertyNames = new HashSet<String>();
			Debug.error(e);
		}
	}

	protected transient boolean customTypesInitialized = false;

	protected WebComponent(ISupportChilds parent, UUID uuid)
	{
		super(IRepository.WEBCOMPONENTS, parent, uuid);
	}

	/**
	 * Serialize WebComponent through a proxy so inherited child caches are not serialized.
	 */
	private Object writeReplace()
	{
		return new SerializationProxy(this);
	}

	private static final class SerializationProxy implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private final ISupportChilds parent;
		private final UUID uuid;
		private final Map<String, Object> properties;

		private SerializationProxy(WebComponent component)
		{
			parent = component.getParent();
			uuid = component.getUUID();
			properties = component.getPropertiesMap();
		}

		private Object readResolve() throws ObjectStreamException
		{
			try
			{
				WebComponent component = new WebComponent(parent, uuid);
				component.copyPropertiesMap(properties, true);
				return component;
			}
			catch (RuntimeException e)
			{
				InvalidObjectException ex = new InvalidObjectException("Failed to deserialize WebComponent from proxy");
				ex.initCause(e);
				throw ex;
			}
		}
	}


	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (hasPersistProperty(propertyName)) super.setProperty(propertyName, val);
		else PersistHelper.setWebComponentProperty(this, propertyName, val);
	}

	@Override
	protected void setPropertyInternal(String propertyName, Object val)
	{
		super.setPropertyInternal(propertyName, val);
		PersistHelper.setWebComponentProperty(this, propertyName, val);
	}


	@Override
	public void clearProperty(String propertyName)
	{
		if (IContentSpecConstants.PROPERTY_TYPENAME.equals(propertyName)) return;
		super.clearProperty(propertyName);
		PersistHelper.clearWebComponentProperty(this, propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		if (!customTypesInitialized && !IContentSpecConstants.PROPERTY_JSON.equals(propertyName))
		{
			// is it ok to initialize custom types here?
			initCustomTypes();
		}
		if (hasPersistProperty(propertyName)) return super.getProperty(propertyName);
		return PersistHelper.getWebComponentProperty(this, propertyName);
	}

	@Override
	public List<IPersist> getAllObjectsAsList()
	{
		if (!customTypesInitialized)
		{
			// is it ok to initialize custom types here?
			initCustomTypes();
		}
		return super.getAllObjectsAsList();
	}

	@Override
	public Object getOwnPropertyOrDefault(String propertyName)
	{
		Object value = null;
		if (hasPersistProperty(propertyName)) value = getOwnProperty(propertyName);
		if (value == null)
		{
			JSONObject json = getJson();
			if (json != null)
			{
				Object object = json.opt(propertyName);
				if (object != null)
				{
					PropertyDescription propertyDescription = getPropertyDescription();
					if (propertyDescription != null)
					{
						PropertyDescription childPd = propertyDescription.getProperty(propertyName);
						if (childPd == null && propertyDescription instanceof WebObjectSpecification &&
							((WebObjectSpecification)propertyDescription).getHandler(propertyName) != null)
						{
							childPd = ((WebObjectSpecification)propertyDescription).getHandler(propertyName).getAsPropertyDescription();
						}
						if (childPd != null)
						{
							object = PersistHelper.convertToJavaType(childPd, object, this);
						}
					}
				}
				value = makeCopy(object);
			}
		}
		return value != null ? value : super.getOwnPropertyOrDefault(propertyName);
	}

	@Override
	public Object getPropertyDefaultValueClone(String propertyName)
	{
		PropertyDescription pd = getPropertyDescription();
		PropertyDescription propPD = pd != null ? pd.getProperty(propertyName) : null;
		return propPD != null && propPD.hasDefault() ? ServoyJSONObject.deepCloneJSONArrayOrObj(propPD.getDefaultValue()) : null;
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
				JSONObject json = getJson();
				hasIt = (json != null && json.has(propertyName));
			}
		}
		if (!hasIt) hasIt = super.hasProperty(propertyName);

		return hasIt;
	}

	protected boolean hasPersistProperty(String propertyName)
	{
		return purePersistPropertyNames.contains(propertyName);
	}

	public void setTypeName(String arg)
	{
		String oldTypeName = getTypeName();
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME, arg);
		if (!customTypesInitialized || !Utils.equalObjects(oldTypeName, arg))
		{
			initCustomTypes();
		}
	}

	public String getTypeName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME);
	}

	/**
	 * DO NOT USE this method! Use setProperty instead.
	 * @param arg
	 */
	public void setJson(JSONObject arg)
	{
		if (arg != null && !(arg instanceof ServoyJSONObject)) throw new RuntimeException("ServoyJSONObject is needed here in order to make it serializable");
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
		if (!customTypesInitialized)
		{
			initCustomTypes();
		}
		else
		{
			// just update the custom types based on the new json, without clearing them first
			PropertyDescription propertyDescription = getPropertyDescription();
			if (propertyDescription != null)
			{
				for (String propertyName : propertyDescription.getAllPropertiesNames())
				{
					PropertyDescription childPd = propertyDescription.getProperty(propertyName);
					if (PersistHelper.isPersistMappedProperty(childPd))
					{
						Object customTypesJSON = arg.opt(propertyName);
						if (customTypesJSON != null)
						{
							if (customTypesJSON instanceof JSONObject jsonObject)
							{
								UUID childUUID = Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false);
								WebCustomType existingCustomType = (WebCustomType)getChild(childUUID);
								if (existingCustomType != null)
								{
									existingCustomType.setJson(jsonObject);
								}
								else
								{
									WebCustomType.createNewInstance(this, (childPd.getType() instanceof ICustomType< ? >)
										? ((ICustomType< ? >)childPd.getType()).getCustomJSONTypeDefinition() : childPd, propertyName, -1,
										childUUID);
								}
							}
							else if (customTypesJSON instanceof JSONArray arr)
							{
								for (int i = 0; i < arr.length(); i++)
								{
									if (arr.opt(i) instanceof JSONObject jsonObject)
									{
										UUID childUUID = Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false);
										WebCustomType existingCustomType = (WebCustomType)getChild(childUUID);
										if (existingCustomType != null)
										{
											existingCustomType.setJson(jsonObject);
										}
										else
										{
											WebCustomType.createNewInstance(this, (childPd.getType() instanceof ICustomType< ? >)
												? ((ICustomType< ? >)childPd.getType()).getCustomJSONTypeDefinition() : childPd, propertyName, i,
												Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public JSONObject getJson()
	{
		JSONObject x = getTypedProperty(StaticContentSpecLoader.PROPERTY_JSON);
		try
		{
			return x == null ? x : new ServoyJSONObject(x, ServoyJSONObject.getNames(x), false, true);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return x;
		}
	}

	public JSONObject getFlattenedJson()
	{
		return PersistHelper.getFlattenedJSON(this);
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		super.internalRemoveChild(obj);
		PersistHelper.removeChildWebComponent(this, obj);
	}

	@Override
	public void addChild(IPersist obj, int index)
	{
		super.addChild(obj, index);
		if (obj instanceof WebCustomType customType)
		{
			JSONObject json = (JSONObject)getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
			if (json == null)
			{
				json = new ServoyJSONObject();
				setJson(json);
			}
			if (PersistHelper.isArrayOfCustomJSONObject(getPropertyDescription().getProperty(customType.getJsonKey()).getType()))
			{
				JSONArray customTypesArray = json.optJSONArray(customType.getJsonKey());
				if (customTypesArray == null)
				{
					customTypesArray = new ServoyJSONArray();
					json.put(customType.getJsonKey(), customTypesArray);
				}
				if (index >= 0)
				{
					if (index >= customTypesArray.length() || customTypesArray.opt(index) == null)
					{
						customTypesArray.put(index, customType.getFullJsonInFrmFile());
					}
					else
					{
						// move the element to the correct index
						for (int i = customTypesArray.length() - 1; i >= index; i--)
						{
							customTypesArray.put(i + 1, customTypesArray.opt(i));
						}
						customTypesArray.put(index, customType.getFullJsonInFrmFile());
					}
				}
				else
				{
					customTypesArray.put(customType.getFullJsonInFrmFile());
				}
			}
			else
			{
				json.put(customType.getJsonKey(), customType.getFullJsonInFrmFile());
			}
		}

	}

	@Override
	public IPersist cloneObj(ISupportChilds newParent, boolean deep, IValidateName validator, boolean changeName, boolean changeChildNames,
		boolean flattenOverrides) throws RepositoryException
	{
		IPersist clone = super.cloneObj(newParent, deep, validator, changeName, changeChildNames, flattenOverrides);
		if (deep && clone instanceof WebComponent wcClone)
		{
			if (this.getExtendsID() != null)
			{
				JSONObject flattenedJson = this.getFlattenedJson();
				if (flattenedJson != null)
				{
					wcClone.setProperty(IContentSpecConstants.PROPERTY_JSON, flattenedJson);
				}
			}
			List<WebCustomType> types = new ArrayList<WebCustomType>();
			clone.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof WebCustomType)
					{
						((WebCustomType)o).resetUUID();
						types.add((WebCustomType)o);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
			for (WebCustomType customType : types)
			{
				clone.getRootObject().getChangeHandler().fireIPersistChanged(customType);
			}
			// hack for cache, we put back the custom types
			this.acceptVisitor(new IPersistVisitor()
			{
				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof WebCustomType)
					{
						getRootObject().getChangeHandler().fireIPersistChanged(o);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}
		return clone;
	}

	/**
	 * returns the attribute value of the given attribute name.
	 * these attributes will be generated on the tag.
	 *
	 * @param name
	 * @return the value of the attribute
	 */
	public String getAttribute(String name)
	{
		Object value = getCustomProperty(new String[] { IContentSpecConstants.PROPERTY_ATTRIBUTES, name });
		if (value instanceof String) return (String)value;
		return null;
	}

	@Override
	public java.awt.Dimension getSize()
	{
		Dimension size = getTypedProperty(StaticContentSpecLoader.PROPERTY_SIZE);
		if (size == null)
		{
			Object defaultSize = getPropertyDefaultValueClone(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
			if (defaultSize instanceof JSONObject)
			{
				return new java.awt.Dimension(((JSONObject)defaultSize).optInt("width"), ((JSONObject)defaultSize).optInt("height")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return new java.awt.Dimension(140, 20);
		}
		return size;
	}

	@Override
	public java.awt.Point getLocation()
	{
		java.awt.Point point = getTypedProperty(StaticContentSpecLoader.PROPERTY_LOCATION);
		if (point == null)
		{
			Object defaultLocation = getPropertyDefaultValueClone(StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName());
			if (defaultLocation instanceof JSONObject)
			{
				return new Point(((JSONObject)defaultLocation).optInt("x"), ((JSONObject)defaultLocation).optInt("y")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			point = new Point(10, 10);
		}
		return point;
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}

	protected void initCustomTypes()
	{
		internalClearAllObjects().forEach(o -> {
			if (getRootObject().getChangeHandler() != null)
			{
				getRootObject().getChangeHandler().fireIPersistRemoved(o);
			}
		});
		PropertyDescription propertyDescription = getPropertyDescription();
		if (propertyDescription == null) return;
		JSONObject json = (JSONObject)getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		if (json == null) return;
		customTypesInitialized = true;
		if (json.length() > 0)
		{
			for (String propertyName : propertyDescription.getAllPropertiesNames())
			{
				PropertyDescription childPd = propertyDescription.getProperty(propertyName);
				if (PersistHelper.isPersistMappedProperty(childPd))
				{
					Object customTypesJSON = json.opt(propertyName);
					if (customTypesJSON != null)
					{
						if (customTypesJSON instanceof JSONObject jsonObject)
						{
							WebCustomType.createNewInstance(this, (childPd.getType() instanceof ICustomType< ? >)
								? ((ICustomType< ? >)childPd.getType()).getCustomJSONTypeDefinition() : childPd, propertyName, -1,
								Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false));
						}
						else if (customTypesJSON instanceof JSONArray arr)
						{
							for (int i = 0; i < arr.length(); i++)
							{
								if (arr.opt(i) instanceof JSONObject jsonObject)
								{
									WebCustomType.createNewInstance(this, (childPd.getType() instanceof ICustomType< ? >)
										? ((ICustomType< ? >)childPd.getType()).getCustomJSONTypeDefinition() : childPd, propertyName, i,
										Utils.getAsUUID(jsonObject.optString(IChildWebObject.UUID_KEY, null), false));
								}
							}
						}
					}
				}
			}
		}
		// handle the old implementation of custom types inheritance , based only on the index; set(guess) the correct extendsID
		IPersist superPersist = PersistHelper.getSuperPersist(this);
		if (superPersist != null)
		{
			List<IPersist> allObjectsAsList = getAllObjectsAsList();
			List<IPersist> superAllObjectsAsList = ((AbstractBase)superPersist).getAllObjectsAsList();
			if (allObjectsAsList.size() > 0 && allObjectsAsList.size() == superAllObjectsAsList.size())
			{
				boolean containsExtendsID = allObjectsAsList.stream()
					.anyMatch(persist -> persist instanceof ISupportExtendsID supportsExtendsIDPersist && supportsExtendsIDPersist.getExtendsID() != null);
				if (!containsExtendsID)
				{
					for (int i = 0; i < allObjectsAsList.size(); i++)
					{
						IPersist persist = allObjectsAsList.get(i);
						IPersist superPersistAtIndex = superAllObjectsAsList.get(i);
						if (persist instanceof ISupportExtendsID supportsExtendsIDPersist)
						{
							supportsExtendsIDPersist.setExtendsID(superPersistAtIndex.getUUID().toString());
						}
					}
				}
			}
		}
	}

	private boolean gettingTypeName;

	public PropertyDescription getPropertyDescription()
	{
		if (!gettingTypeName && WebComponentSpecProvider.isLoaded())
		{
			gettingTypeName = true;
			try
			{
				return WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(getTypeName());
			}
			finally
			{
				gettingTypeName = false;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		String name = getName();
		if (name == null || name.trim().length() == 0)
		{
			return getTypeName();
		}
		return name + " [" + getTypeName() + ']'; //$NON-NLS-1$
	}
}
