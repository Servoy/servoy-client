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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 *
 * @author gboros
 */
public class WebCustomType extends AbstractBase implements IChildWebObject, ISupportExtendsID
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
	private final PropertyDescription propertyDescription;

	public static WebCustomType createNewInstance(IBasicWebObject parentWebObject, PropertyDescription propertyDescription, String jsonKey,
		int index)
	{
		return createNewInstance(parentWebObject, propertyDescription, jsonKey, index, null);
	}

	public static WebCustomType createNewInstance(IBasicWebObject parentWebObject, PropertyDescription propertyDescription, String jsonKey,
		int index,
		UUID uuid)
	{
		return new WebCustomType(parentWebObject, propertyDescription, jsonKey, index,
			uuid != null ? uuid : UUID.randomUUID());
	}


	protected WebCustomType(IBasicWebObject parentWebObject, PropertyDescription propertyDescription, String jsonKey, int index, UUID uuid)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, uuid);

		this.jsonKey = jsonKey;
		this.propertyDescription = propertyDescription;
		JSONObject json = (JSONObject)((AbstractBase)parentWebObject).getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		if (json == null)
		{
			json = new ServoyJSONObject();
			parentWebObject.setJson(json);
		}
		((AbstractBase)parentWebObject).internalAddChild(this, index);
		((AbstractBase)parentWebObject).afterChildWasAdded(this);
		JSONObject fullJSONInFrmFile = null;
		if (PersistHelper.isArrayOfCustomJSONObject(((ICommonWebComponent)parentWebObject).getPropertyDescription().getProperty(jsonKey).getType()))
		{
			JSONArray customTypesArray = json.optJSONArray(jsonKey);
			if (customTypesArray == null)
			{
				fullJSONInFrmFile = new ServoyJSONObject();
				customTypesArray = new ServoyJSONArray();
				customTypesArray.put(fullJSONInFrmFile);
				json.put(jsonKey, customTypesArray);
			}
			else
			{
				for (int i = 0; i < customTypesArray.length(); i++)
				{
					Object item = customTypesArray.opt(i);
					if (item instanceof JSONObject candidate)
					{
						String candidateUuid = candidate.optString(UUID_KEY, null);
						if (getUUID().toString().equals(candidateUuid))
						{
							fullJSONInFrmFile = candidate;
							break;
						}
					}
				}
				if (fullJSONInFrmFile == null)
				{
					fullJSONInFrmFile = new ServoyJSONObject();
					if (index >= 0)
					{
						if (index >= customTypesArray.length() || customTypesArray.opt(index) == null)
						{
							customTypesArray.put(index, fullJSONInFrmFile);
						}
						else
						{
							// move the element to the correct index
							for (int i = customTypesArray.length() - 1; i >= index; i--)
							{
								customTypesArray.put(i + 1, customTypesArray.opt(i));
							}
							customTypesArray.put(index, fullJSONInFrmFile);
						}
					}
					else
					{
						customTypesArray.put(fullJSONInFrmFile);
					}
				}
			}
		}
		else
		{
			fullJSONInFrmFile = json.optJSONObject(jsonKey);
			if (fullJSONInFrmFile == null)
			{
				fullJSONInFrmFile = new ServoyJSONObject();
				json.put(jsonKey, fullJSONInFrmFile);
			}
		}

		fullJSONInFrmFile.put(UUID_KEY, getUUID().toString());
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, fullJSONInFrmFile);
		setTypeName(PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType()));
		this.initCustomTypes();
	}

	public PropertyDescription getPropertyDescription()
	{
		return propertyDescription;
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (purePersistPropertyNames.contains(propertyName)) super.setProperty(propertyName, val);
		PersistHelper.setWebComponentProperty(this, propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		super.clearProperty(propertyName);
		PersistHelper.clearWebComponentProperty(this, propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		Object value = null;
		if (purePersistPropertyNames.contains(propertyName)) value = super.getProperty(propertyName);
		if (value == null) value = PersistHelper.getWebComponentProperty(this, propertyName);
		return value;
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

	// TODO is this still really needed? we now work with the property description based on specs...
	public void setTypeName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME, arg);
	}

	public String getTypeName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME);
	}

	public void setJson(JSONObject arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
		this.initCustomTypes();
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
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + getTypeName() + '[' + getIndex() + ']'; //$NON-NLS-1$
	}

	@Override
	public IBasicWebObject getParent()
	{
		return (IBasicWebObject)super.getParent();
	}

	public int getIndex()
	{
		if (getParent() != null)
		{
			int index = 0;
			Iterator<IPersist> it = getParent().getAllObjects();
			while (it.hasNext())
			{
				IPersist child = it.next();
				if (child == this) return index;
				if (child instanceof WebCustomType && jsonKey.equals(((WebCustomType)child).getJsonKey())) index++;
			}
		}
		return -1;
	}

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

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		super.internalRemoveChild(obj);
		PersistHelper.removeChildWebComponent(this, obj);
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

		JSONObject fullJSONInFrmFile = getFullJsonInFrmFile();
		if (fullJSONInFrmFile == null)
		{
			fullJSONInFrmFile = new ServoyJSONObject();
			setJson(fullJSONInFrmFile);
		}
		fullJSONInFrmFile.put(UUID_KEY, getUUID().toString());
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
		JSONObject fullJSONInFrmFile = getFullJsonInFrmFile();
		if (fullJSONInFrmFile == null)
		{
			fullJSONInFrmFile = new ServoyJSONObject();
			setJson(fullJSONInFrmFile);
		}
		fullJSONInFrmFile.put(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName(), arg);
	}

	@Override
	public Map<String, Object> getFlattenedPropertiesMap()
	{
		return PersistHelper.getFlattenedPropertiesMap(this);
	}


	@Override
	public ISupportChilds getRealParent()
	{
		return PersistHelper.getRealParent(this);
	}

	private void initCustomTypes()
	{
		internalClearAllObjects();
		JSONObject json = (JSONObject)getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName());
		if (propertyDescription != null && json != null && json.length() > 0)
		{
			Collection<String> allPropertiesNames = propertyDescription.getType() instanceof CustomJSONPropertyType customJSONType
				? customJSONType.getCustomJSONTypeDefinition().getAllPropertiesNames() : propertyDescription.getAllPropertiesNames();
			for (String propertyName : allPropertiesNames)
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
	}
}
