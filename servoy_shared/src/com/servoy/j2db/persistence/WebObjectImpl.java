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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
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
public class WebObjectImpl
{

	private final Map<String, Object> customTypeProperties = new HashMap<String, Object>();
	private boolean areCustomTypePropertiesLoaded = false;
	private final IWebObject webObject;
	private PropertyDescription pdUseGetterInstead;
	private Map<UUID, IPersist> customTypePropsByUUID = null; // cached just like in AbstractBase

	/**
	 * This constructor is to be used if getTypeName is the name of a WebComponent. (so it can be used to get the component spec)
	 */
	public WebObjectImpl(IWebObject webObject)
	{
		this.webObject = webObject;
	}

	public PropertyDescription getPropertyDescription()
	{
		// at the time WebComponent is created the resources project is not yet loaded nor is the typeName property set; so find it when it's needed in this case
		if (pdUseGetterInstead == null) pdUseGetterInstead = WebComponentSpecProvider.getInstance() != null
			? WebComponentSpecProvider.getInstance().getWebComponentSpecification(getTypeName()) : null;

		return pdUseGetterInstead;
	}

	public WebObjectImpl(IWebObject webObject, PropertyDescription specPD)
	{
		this.webObject = webObject;
		this.pdUseGetterInstead = specPD;
	}

	public void updateCustomProperties()
	{
		if (areCustomTypePropertiesLoaded)
		{
			JSONObject old = getJson();
			try
			{
				JSONObject entireModel = (old != null ? old : new ServoyJSONObject()); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties
				Iterator<String> it = entireModel.keys();
				while (it.hasNext())
				{
					String key = it.next();
					if (!getCustomTypeProperties().containsKey(key)) entireModel.remove(key);
				}
				for (Map.Entry<String, Object> wo : getCustomTypeProperties().entrySet())
				{
					if (wo.getValue() instanceof WebCustomType)
					{
						entireModel.put(wo.getKey(), ((WebCustomType)wo.getValue()).getJson());
					}
					else
					{
						ServoyJSONArray jsonArray = new ServoyJSONArray();
						for (WebCustomType wo1 : (WebCustomType[])wo.getValue())
						{
							jsonArray.put(wo1.getJson());
						}
						entireModel.put(wo.getKey(), jsonArray);
					}
				}
				setJsonInternal(entireModel);
				((AbstractBase)webObject).flagChanged();
			}
			catch (JSONException ex)
			{
				Debug.error(ex);
			}
		}
	}

	/**
	 * Returns false if it can't set is as a custom property. Then caller should set it as another standard persist property.
	 */
	public boolean setCustomProperty(String propertyName, Object val)
	{
		if (val instanceof WebCustomType || val instanceof WebCustomType[])
		{
			getCustomTypeProperties().put(propertyName, val);
			customTypePropsByUUID = null;
			updateCustomProperties();
			return true;
		}
		else return false;
	}

	/**
	 * Returns false if it can't clear this as a custom property (it is something else). Then caller should just clear it as another standard persist property.
	 */
	public boolean clearCustomProperty(String propertyName)
	{
		Map<String, Object> customProperties = getCustomTypeProperties();
		if (customProperties.containsKey(propertyName))
		{
			getCustomTypeProperties().remove(propertyName);
			customTypePropsByUUID = null;
			updateCustomProperties();
			return true;
		}
		else return false;
	}

	public Pair<Boolean, Object> getCustomProperty(String propertyName)
	{
		if (!"json".equals(propertyName) && !"typeName".equals(propertyName)) //$NON-NLS-1$//$NON-NLS-2$
		{
			Map<String, Object> ctp = getCustomTypeProperties();
			if (ctp.containsKey(propertyName)) return new Pair<>(Boolean.TRUE, ctp.get(propertyName));
		}
		return new Pair<>(Boolean.FALSE, null);
	}

	public List<WebCustomType> getAllCustomProperties()
	{
		ArrayList<WebCustomType> allCustomProperties = new ArrayList<WebCustomType>();
		for (Object wo : getCustomTypeProperties().values())
		{
			if (wo instanceof WebCustomType[])
			{
				allCustomProperties.addAll(Arrays.asList((WebCustomType[])wo));
			}
			else
			{
				allCustomProperties.add((WebCustomType)wo);
			}
		}

		return allCustomProperties;
	}

	private Map<String, Object> getCustomTypeProperties()
	{
		if (!areCustomTypePropertiesLoaded && getPropertyDescription() != null)
		{
			if (getJson() != null)
			{
				JSONObject beanJSON = getJson();
				try
				{

					for (String beanJSONKey : ServoyJSONObject.getNames(beanJSON))
					{
						Object object = beanJSON.get(beanJSONKey);
						updateCustomTypedProperty(beanJSONKey, object);
					}
				}
				catch (JSONException e)
				{
					Debug.error(e);
				}
			}
			areCustomTypePropertiesLoaded = true;
		}

		return customTypeProperties;
	}

	protected void updateCustomTypedProperty(String beanJSONKey, Object object)
	{
		if (object != null && getPropertyDescription().getProperty(beanJSONKey) != null)
		{
			PropertyDescription childPd = getPropertyDescription().getProperty(beanJSONKey);
			IPropertyType< ? > propertyType = childPd.getType();
			String simpleTypeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyType);
			if (PropertyUtils.isCustomJSONProperty(propertyType))
			{
				boolean arrayReturnType = PropertyUtils.isCustomJSONArrayPropertyType(propertyType);
				if (!arrayReturnType)
				{
					WebCustomType webCustomType = new WebCustomType(webObject, childPd, beanJSONKey, -1, false);
					webCustomType.setTypeName(simpleTypeName);
					customTypeProperties.put(beanJSONKey, webCustomType);
					customTypePropsByUUID = null;
				}
				else if (object instanceof JSONArray)
				{
					PropertyDescription elementPD = (propertyType instanceof ICustomType< ? >) ? ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition()
						: null;
					if (elementPD != null && PropertyUtils.isCustomJSONObjectProperty(elementPD.getType()))
					{
						ArrayList<WebCustomType> webCustomTypes = new ArrayList<WebCustomType>();
						for (int i = 0; i < ((JSONArray)object).length(); i++)
						{
							WebCustomType webCustomType = new WebCustomType(webObject, ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition(),
								beanJSONKey, i, false);
							webCustomType.setTypeName(simpleTypeName);
							webCustomTypes.add(webCustomType);
						}
						customTypeProperties.put(beanJSONKey, webCustomTypes.toArray(new WebCustomType[webCustomTypes.size()]));
						customTypePropsByUUID = null;
					}
				}
				else
				{
					Debug.error("Custom type value is not JSONArray although in spec it is defined as array... " + this + " - " + object);
				}
			}
		}
		else
		{
			if (customTypeProperties.remove(beanJSONKey) != null) customTypePropsByUUID = null;
		}
	}

	public void setTypeName(String arg)
	{
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME, arg);
	}

	public String getTypeName()
	{
		return ((AbstractBase)webObject).getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME);
	}

	public void setJson(JSONObject arg)
	{
		clearCustomPropertyCache(); // let them completely reload later when needed
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);

		// update JSON property of all parent web objects as all this web object hierarchy is actually described by top-most web object JSON property
		// and the JSON of each web object should never get out-of-sync with the child web objects it contains
		ISupportChilds parent = webObject.getParent();
		if (parent instanceof IBasicWebObject) ((IBasicWebObject)parent).updateJSON();
	}

	public void setJsonSubproperty(String key, Object value)
	{
		try
		{
			JSONObject oldJson = getJson();
			if (oldJson == null || !oldJson.has(key) || !oldJson.get(key).equals(value))
			{
				JSONObject jsonObject = (oldJson == null ? new ServoyJSONObject() : oldJson); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties
				jsonObject.put(key, value);
				setJsonInternal(jsonObject);
				((AbstractBase)webObject).flagChanged();

				if (areCustomTypePropertiesLoaded && getPropertyDescription() != null)
				{
					updateCustomTypedProperty(key, value); // update this web object's child web objects if needed (if this key affects them)
				} // else not yet loaded - they will all load later so nothing to do here

				// update JSON property of all parent web objects as all this web object hierarchy is actually described by top-most web object JSON property
				// and the JSON of each web object should never get out-of-sync with the child web objects it contains
				ISupportChilds parent = webObject.getParent();
				if (parent instanceof IBasicWebObject) ((IBasicWebObject)parent).updateJSON();
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	protected void clearCustomPropertyCache()
	{
		areCustomTypePropertiesLoaded = false;
		customTypeProperties.clear();
	}

	public void setJsonInternal(JSONObject arg)
	{
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	public JSONObject getJson()
	{
		return ((AbstractBase)webObject).getTypedProperty(StaticContentSpecLoader.PROPERTY_JSON);
	}

	// TODO should these two "name" methods be a part of something else?
	public void setName(String arg)
	{
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	public String getName()
	{
		return ((AbstractBase)webObject).getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
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

	public void internalAddChild(IPersist obj)
	{
		if (obj instanceof WebCustomType)
		{
			WebCustomType customType = (WebCustomType)obj;
			IPropertyType< ? > type = customType.getPropertyDescription().getType();

			if (getPropertyDescription().isArrayReturnType(customType.getJsonKey()))
			{
				if (type == ((CustomJSONArrayType< ? , ? >)getPropertyDescription().getProperty(customType.getJsonKey()).getType()).getCustomJSONTypeDefinition().getType())
				{
					Object children = getCustomTypeProperties().get(customType.getJsonKey());
					if (children == null) children = new WebCustomType[0];
					if (children instanceof WebCustomType[])
					{
						List<WebCustomType> t = new ArrayList<WebCustomType>(Arrays.asList((WebCustomType[])children));
						t.add(customType.getIndex(), customType);
						for (int i = customType.getIndex() + 1; i < t.size(); i++)
						{
							WebCustomType ct = t.get(i);
							ct.setIndex(i);
						}
						setCustomProperty(customType.getJsonKey(), t.toArray(new WebCustomType[t.size()]));
					}
					else
					{
						Debug.error("Unexpected array property persist value: " + children);
					}
				}
				else
				{
					Debug.error("Element type (" +
						((CustomJSONArrayType< ? , ? >)getPropertyDescription().getProperty(customType.getJsonKey()).getType()).getCustomJSONTypeDefinition().getType() +
						") does not match persist-to-add type: " + type + " - " + webObject);
				}
			}
			else
			{
				if (type == getPropertyDescription().getProperty(customType.getJsonKey()).getType())
				{
					setCustomProperty(customType.getJsonKey(), customType);
				}
				else
				{
					Debug.error("Property type (" + getPropertyDescription().getProperty(customType.getJsonKey()).getType() +
						") does not match persist-to-add type: " + type + " - " + webObject);
				}
			}
			customTypePropsByUUID = null;
		}
		else
		{
			Debug.error("Trying to add non - WebCustomType to a WebObject: " + obj + ", " + webObject);
		}
	}

	public void internalRemoveChild(IPersist obj)
	{
		if (obj instanceof WebCustomType)
		{
			WebCustomType customType = (WebCustomType)obj;
			Object children = getCustomTypeProperties().get(customType.getJsonKey());
			if (children != null)
			{
				if (children instanceof WebCustomType[])
				{
					List<WebCustomType> t = new ArrayList<WebCustomType>(Arrays.asList((WebCustomType[])children));
					t.remove(customType);
					for (int i = customType.getIndex(); i < t.size(); i++)
					{
						WebCustomType ct = t.get(i);
						ct.setIndex(i);
					}
					setCustomProperty(customType.getJsonKey(), t.toArray(new WebCustomType[t.size()]));
				}
				else
				{
					clearCustomProperty(customType.getJsonKey());
				}
			}
			customTypePropsByUUID = null;
		}
		else
		{
			Debug.error("Trying to remove non - WebCustomType from a WebObject: " + obj + ", " + webObject);
		}
	}

	// TODO should we offer alternate implementation for AbstractBase.getAllObjectsAsList() here as well? that one is currently final
	// and at first look I think we can leave it alone (and is not part of the IsupportChilds interface)

	public Iterator<IPersist> getAllObjects()
	{
		final Iterator<Object> it1 = getCustomTypeProperties().values().iterator();

		return new Iterator<IPersist>()
		{
			int idx = 0;
			WebCustomType[] ai;

			private boolean hasArrayItems()
			{
				return ai != null && idx < ai.length;
			}

			@Override
			public boolean hasNext()
			{
				return it1.hasNext() || hasArrayItems();
			}

			@Override
			public IPersist next()
			{
				IPersist item;
				if (hasArrayItems())
				{
					item = ai[idx++];
				}
				else
				{
					Object o = it1.next();
					while (o instanceof WebCustomType[])
					{
						ai = (WebCustomType[])o;
						if (ai.length == 0) o = it1.next();
						else
						{
							idx = 1;
							o = ai[0];
						}
					}
					item = (IPersist)o;
				}
				return item;
			}

			@Override
			public void remove()
			{
				throw new RuntimeException("Operation not supported.");
			}

		};
	}

	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return new TypeIterator<T>(getAllObjects(), tp);
	}

	public IPersist getChild(UUID childUuid)
	{
		if (customTypePropsByUUID == null)
		{
			Iterator<IPersist> allobjects = getAllObjects();
			if (allobjects != null && allobjects.hasNext())
			{
				customTypePropsByUUID = new ConcurrentHashMap<UUID, IPersist>(customTypeProperties.size(), 0.9f, 16);
				while (allobjects.hasNext())
				{
					IPersist persist = allobjects.next();
					if (persist != null)
					{
						customTypePropsByUUID.put(persist.getUUID(), persist);
					}
				}
			}
		}
		return customTypePropsByUUID == null ? null : customTypePropsByUUID.get(childUuid);
	}

}
