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


import java.beans.IntrospectionException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author gboros
 */
public class WebCustomType extends AbstractBase implements IChildWebObject, ISupportsIndexedChildren, ISupportExtendsID
{

//	private static final long serialVersionUID = 1L; // this shouldn't get serialized anyway for now; parent WebComponent just serializes it's json

	protected static Set<String> purePersistPropertyNames;

	static
	{
		try
		{
			purePersistPropertyNames = RepositoryHelper.getSettersViaIntrospection(WebCustomType.class).keySet();
		}
		catch (IntrospectionException e)
		{
			purePersistPropertyNames = new HashSet<String>();
			Debug.error(e);
		}
	}

	private transient String jsonKey;
	private transient int index;
	protected transient final WebObjectImpl webObjectImpl;

	public static WebCustomType createNewInstance(IBasicWebObject parentWebObject, Object propertyDescription, String jsonKey, int index, boolean isNew)
	{
		return createNewInstance(parentWebObject, propertyDescription, jsonKey, index, isNew, null);
	}

	public static WebCustomType createNewInstance(IBasicWebObject parentWebObject, Object propertyDescription, String jsonKey, int index, boolean isNew,
		UUID uuid)
	{
		return new WebCustomType(parentWebObject, propertyDescription, jsonKey, index, isNew,
			uuid != null ? uuid : UUID.randomUUID());
	}


	public WebCustomType(IBasicWebObject parentWebObject, Object propertyDescription, String jsonKey, int index, boolean isNew, UUID uuid)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, uuid);
		webObjectImpl = new WebObjectImpl(this, propertyDescription);

		this.jsonKey = jsonKey;
		this.index = index;

		JSONObject fullJSONInFrmFile = WebObjectImpl.getFullJSONInFrmFile(this, isNew);
		if (fullJSONInFrmFile == null) fullJSONInFrmFile = new ServoyJSONObject();
		fullJSONInFrmFile.put(UUID_KEY, getUUID().toString());
		webObjectImpl.setJsonInternal(fullJSONInFrmFile);
	}

	public PropertyDescription getPropertyDescription()
	{
		return webObjectImpl.getPropertyDescription();
	}

	@Override
	public void clearChanged()
	{
		super.clearChanged();
		for (IChildWebObject x : getAllPersistMappedProperties())
		{
			if (x.isChanged()) x.clearChanged();
		}
	}

	@Override
	public void updateJSON()
	{
		webObjectImpl.updateJSONFromPersistMappedProperties();
		getParent().updateJSON();
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (webObjectImpl.setProperty(propertyName, val))
		{
			// see if it's not a direct persist property as well such as size, location, anchors... if it is set it here as well anyway so that they are in sync with spec properties
			if (purePersistPropertyNames.contains(propertyName)) super.setProperty(propertyName, val);
		}
		else super.setProperty(propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		if (!webObjectImpl.clearProperty(propertyName)) super.clearProperty(propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		Object value = null;
		if (webObjectImpl == null || purePersistPropertyNames.contains(propertyName))
		{
			if (StaticContentSpecLoader.PROPERTY_JSON.getPropertyName().equals(propertyName))
			{
				Object mergedParentProperty = ((AbstractBase)parent).getProperty(propertyName);
				if (mergedParentProperty instanceof JSONObject)
				{
					Object mergedProperty = ((JSONObject)mergedParentProperty).opt(jsonKey);

					if (mergedProperty == null && parent instanceof IBasicWebObject)
					{
						// maybe it's a default value WebCustomType (that means it's JSON will probably not be present in parent);
						// so if it has a default value, just return that so that (even non-persist) sub-properties of the default value are returned correctly
						mergedProperty = ((IBasicWebObject)parent).getPropertyDefaultValueClone(jsonKey);
					}

					if (mergedProperty instanceof JSONArray)
					{
						mergedProperty = ((JSONArray)mergedProperty).opt(getIndex());
					}

					if (mergedProperty instanceof JSONObject)
					{
						value = mergedProperty;
					}
				}
			}
			else
			{
				value = super.getProperty(propertyName);
			}
		}
		if (value == null) value = webObjectImpl.getProperty(propertyName);
		return value;
	}

	@Override
	public Object getPropertyDefaultValueClone(String propertyName)
	{
		return webObjectImpl != null ? webObjectImpl.getPropertyDefaultValue(propertyName) : null;
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		boolean hasIt = false;
		if (webObjectImpl == null) hasIt = super.hasProperty(propertyName);
		else
		{
			hasIt = webObjectImpl.hasProperty(propertyName);
			if (!hasIt) hasIt = super.hasProperty(propertyName);
		}
		return hasIt;
	}

	/**
	 * Returns all direct child typed properties or array of such typed properties.
	 */
	protected List<IChildWebObject> getAllPersistMappedProperties()
	{
		return webObjectImpl.getAllPersistMappedProperties();
	}

	// TODO is this still really needed? we now work with the property description based on specs...
	public void setTypeName(String arg)
	{
		webObjectImpl.setTypeName(arg);
	}

	public String getTypeName()
	{
		return webObjectImpl.getTypeName();
	}

	/**
	 * DO NOT USE this method in general! Use setProperty instead.
	 * @deprecated it is not really deprecated just marked as such so that it's usage is avoided in general (with the exception of who really should use it)
	 */
	@Deprecated
	public void setJson(JSONObject arg)
	{
		webObjectImpl.setJson(arg);
	}

	/**
	 * DO NOT USE this method in general! Use getProperty instead.
	 * @deprecated it is not really deprecated just marked as such so that it's usage is avoided in general (with the exception of who really should use it)
	 */
	@Deprecated
	public JSONObject getJson()
	{
		return webObjectImpl.getJson();
	}

	/**
	 * DO NOT USE this method in general! Use getProperty instead.
	 * @deprecated it is not really deprecated just marked as such so that it's usage is avoided in general (with the exception of who really should use it)
	 */
	@Deprecated
	@Override
	public JSONObject getFlattenedJson()
	{
		return webObjectImpl.getJson();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + webObjectImpl.toString() + '[' + index + ']'; //$NON-NLS-1$
	}

	@Override
	public IBasicWebObject getParent()
	{
		return (IBasicWebObject)super.getParent();
	}

	public IBasicWebComponent getParentComponent()
	{
		return getParent().getParentComponent();
	}

	public int getIndex()
	{
		return index;
	}

//	public String getUUIDString()
//	{
//		String addIndex = "";
//		if (index >= 0) addIndex = "." + index;
//		return parent.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
//	}

//	public String getUUIDString()
//	{
//		String addIndex = "";
//		if (index >= 0) addIndex = "[" + index + "]";
//		return parentWebObject.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
//	}
//

//	@Override
//	public boolean equals(Object obj)
//	{
//		if (obj instanceof WebCustomType)
//		{
//			return ((WebCustomType)obj).getUUIDString().equals(this.getUUIDString());
//		}
//		return super.equals(obj);
//	}
//
	@Override
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	@Override
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	public String getJsonKey()
	{
		return jsonKey;
	}

	@Override
	public void setJsonKey(String newJsonKey)
	{
		jsonKey = newJsonKey;
	}

	/**
	 * @param i the new index
	 */
	public void setIndex(int i)
	{
		index = i;
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		webObjectImpl.internalRemoveChild(obj);
	}

	@Override
	public void internalAddChild(IPersist obj)
	{
		webObjectImpl.setChild(obj);
	}

	@Override
	public void setChild(IChildWebObject child)
	{
		webObjectImpl.setChild(child);
		afterChildWasAdded(child);
	}

	@Override
	public void insertChild(IChildWebObject child)
	{
		webObjectImpl.insertChild(child);
		afterChildWasAdded(child);
	}

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		return webObjectImpl.getAllObjects();
	}

	@Override
	public List<IPersist> getAllObjectsAsList()
	{
		return Utils.asList(getAllObjects());
	}

	@Override
	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return webObjectImpl.getObjects(tp);
	}

	@Override
	public IPersist getChild(UUID childUuid)
	{
		return webObjectImpl.getChild(childUuid);
	}

	@Override
	public JSONObject getFullJsonInFrmFile()
	{
		return (JSONObject)getPropertiesMap().get(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
	}

	@Override
	public void resetUUID(UUID uuidParam)
	{
		super.resetUUID(uuidParam);

		JSONObject fullJSONInFrmFile = WebObjectImpl.getFullJSONInFrmFile(this, false);
		if (fullJSONInFrmFile == null) fullJSONInFrmFile = new ServoyJSONObject();
		fullJSONInFrmFile.put(UUID_KEY, getUUID().toString());
		webObjectImpl.setJsonInternal(fullJSONInFrmFile);
	}

	@Override
	public void applyPropertiesBuffer()
	{
		super.applyPropertiesBuffer();
		if (parent instanceof IBasicWebObject) ((IBasicWebObject)parent).updateJSON();
	}

	@Override
	public String getExtendsID()
	{
		// this used to save the id in the full JSON in the frm file, make sure it doesn't fail
		Object value = getFullJsonInFrmFile().opt(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName());
		return value != null ? value.toString() : null;
	}

	@Override
	public void setExtendsID(String arg)
	{
		webObjectImpl.setJsonInternal(getFullJsonInFrmFile().put(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName(), arg));
		webObjectImpl.reload(true);
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		Map<String, Object> flattenedMap = new HashMap<>();
		IPersist superPersist = PersistHelper.getSuperPersist(this);
		if (superPersist instanceof WebCustomType)
		{
			JSONObject parentJson = ((WebCustomType)superPersist).getFlattenedJson();
			if (parentJson != null)
			{
				addJsonToMap(parentJson, flattenedMap);
			}
		}
		JSONObject childJson = getJson();
		if (childJson != null)
		{
			// merge child JSON properties into the flattened map (can override parent properties)
			addJsonToMap(childJson, flattenedMap);
		}

		return flattenedMap;
	}

	private void addJsonToMap(JSONObject json, Map<String, Object> map)
	{
		Iterator<String> keys = json.keys();
		while (keys.hasNext())
		{
			String key = keys.next();
			try
			{
				map.put(key, json.get(key));
			}
			catch (JSONException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public ISupportChilds getRealParent()
	{
		return PersistHelper.getRealParent(this);
	}
}
