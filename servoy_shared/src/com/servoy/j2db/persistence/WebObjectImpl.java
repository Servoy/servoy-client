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
import java.util.NoSuchElementException;
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
public class WebObjectImpl extends WebObjectBasicImpl
{

	private final Map<String, Object> customTypeProperties = new HashMap<String, Object>();
	private boolean areCustomTypePropertiesLoaded = false;
	private PropertyDescription pdUseGetterInstead;
	private Map<UUID, IPersist> customTypePropsByUUID = null; // cached just like in AbstractBase

	/**
	 * This constructor is to be used if getTypeName is the name of a WebComponent. (so it can be used to get the component spec)
	 */
	public WebObjectImpl(IBasicWebObject webObject)
	{
		super(webObject);
	}

	@Override
	public PropertyDescription getPropertyDescription()
	{
		// at the time WebComponent is created the resources project is not yet loaded nor is the typeName property set; so find it when it's needed in this case
		if (pdUseGetterInstead == null) pdUseGetterInstead = WebComponentSpecProvider.getInstance() != null
			? WebComponentSpecProvider.getInstance().getWebComponentSpecification(getTypeName()) : null;

		return pdUseGetterInstead;
	}

	public WebObjectImpl(IBasicWebObject webObject, Object specPD)
	{
		super(webObject);
		this.pdUseGetterInstead = (PropertyDescription)specPD;
	}

	@Override
	public void updateCustomProperties()
	{
		if (areCustomTypePropertiesLoaded)
		{
			JSONObject old = getJson();
			try
			{
				JSONObject entireModel = (old != null ? old : new ServoyJSONObject()); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties
				Iterator<String> it = entireModel.keys();

				// remove custom properties that were removed (be sure to keep any keys that do not map to custom properties or arrays of custom properties - for example ints, string arrays and so on)
				while (it.hasNext())
				{
					String key = it.next();
					if (isCustomJSONOrArrayOfCustomJSON(key) && !getCustomTypeProperties().containsKey(key)) entireModel.remove(key);
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
	 * Return true if this property is handled as a custom property persist and false otherwise.
	 * @param key the subproperty name.
	 * @return true if this property is handled as a custom property persist and false otherwise.
	 */
	protected boolean isCustomJSONOrArrayOfCustomJSON(String key)
	{
		PropertyDescription childPd = getPropertyDescription().getProperty(key);
		IPropertyType< ? > propertyType = childPd.getType();

		if (PropertyUtils.isCustomJSONProperty(propertyType))
		{
			boolean arrayReturnType = PropertyUtils.isCustomJSONArrayPropertyType(propertyType);
			if (arrayReturnType)
			{
				PropertyDescription elementPD = (propertyType instanceof ICustomType< ? >) ? ((ICustomType< ? >)propertyType).getCustomJSONTypeDefinition()
					: null;
				if (elementPD != null && PropertyUtils.isCustomJSONObjectProperty(elementPD.getType()))
				{
					return true;
				}
			}
			else
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns false if it can't set is as a custom property. Then caller should set it as another standard persist property.
	 */
	@Override
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
	@Override
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

	@Override
	public Pair<Boolean, Object> getCustomProperty(String propertyName)
	{
		Map<String, Object> ctp = getCustomTypeProperties();
		if (ctp.containsKey(propertyName)) return new Pair<>(Boolean.TRUE, ctp.get(propertyName));

		return new Pair<>(Boolean.FALSE, null);
	}

	@Override
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
		if (!areCustomTypePropertiesLoaded)
		{
			areCustomTypePropertiesLoaded = true; // do this here rather then later to avoid stack overflows in case code below end up calling persist.getProperty() again

			if (getPropertyDescription() != null)
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
			}
			else areCustomTypePropertiesLoaded = false; // maybe the solution is being activated as we speak and the property descriptions from resources project are not yet available...
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

	@Override
	public void setJson(JSONObject arg)
	{
		clearCustomPropertyCache(); // let them completely reload later when needed
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);

		// update JSON property of all parent web objects as all this web object hierarchy is actually described by top-most web object JSON property
		// and the JSON of each web object should never get out-of-sync with the child web objects it contains
		ISupportChilds parent = webObject.getParent();
		if (parent instanceof IBasicWebObject) ((IBasicWebObject)parent).updateJSON();
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
			JSONObject oldJson = getJson();
			// we can no longer check for differences here as we now reuse JSON objects/arrays
			JSONObject jsonObject = (oldJson == null ? new ServoyJSONObject() : oldJson); // we have to keep the same instance if possible cause otherwise com.servoy.eclipse.designer.property.UndoablePropertySheetEntry would set child but restore completely from parent when modifying a child value in case of nested properties
			if (remove)
			{
				removed = (jsonObject.remove(key) != null);
			}
			else
			{
				jsonObject.put(key, value);
			}
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
			return removed;
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
		return false;
	}

	@Override
	public boolean removeJsonSubproperty(String key)
	{
		return setOrRemoveJsonSubproperty(key, null, true);
	}

	protected void clearCustomPropertyCache()
	{
		areCustomTypePropertiesLoaded = false;
		customTypeProperties.clear();
	}

	@Override
	public void setJsonInternal(JSONObject arg)
	{
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	@Override
	public void internalAddChild(IPersist obj)
	{
		if (obj instanceof WebCustomType)
		{
			WebCustomType customType = (WebCustomType)obj;
			IPropertyType< ? > type = ((PropertyDescription)customType.getPropertyDescription()).getType();

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

	@Override
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

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		final Iterator<Object> it1 = getCustomTypeProperties().values().iterator();

		return new CustomTypesIterator(it1);
	}

	protected static class CustomTypesIterator implements Iterator<IPersist>
	{


		WebCustomType nextSingleObj;

		WebCustomType[] nextArrayObj;
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
			if (o instanceof WebCustomType)
			{
				nextSingleObj = (WebCustomType)o;
				return true;
			}
			else if (o instanceof WebCustomType[])
			{
				WebCustomType[] arr = (WebCustomType[])o;
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
				WebCustomType toReturn;
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
