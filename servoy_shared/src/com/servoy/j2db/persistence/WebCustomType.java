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


import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 *
 * @author gboros
 */
public class WebCustomType extends AbstractBase implements IWebObject
{
	private transient final String jsonKey;
	private transient int index;
	protected transient final WebObjectImpl webObjectImpl;

	public WebCustomType(IBasicWebObject parentWebObject, PropertyDescription propertyDescription, String jsonKey, int index, boolean isNew)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, 0, UUID.randomUUID());
		webObjectImpl = new WebObjectImpl(this, propertyDescription);

		this.jsonKey = jsonKey;
		this.index = index;

		try
		{
			ServoyJSONObject entireModel = parentWebObject.getJson() != null ? parentWebObject.getJson() : new ServoyJSONObject();
			if (!isNew && entireModel.has(jsonKey))
			{
				Object v = entireModel.get(jsonKey);
				JSONObject obj = null;
				if (v instanceof JSONArray)
				{
					obj = ((JSONArray)v).getJSONObject(index);
				}
				else
				{
					obj = entireModel.getJSONObject(jsonKey);
				}
				webObjectImpl.setJsonInternal(obj instanceof ServoyJSONObject ? (ServoyJSONObject)obj : new ServoyJSONObject(obj.toString(), false));
			}
			else
			{
				webObjectImpl.setJsonInternal(new ServoyJSONObject());
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	@Override
	public PropertyDescription getPropertyDescription()
	{
		return webObjectImpl.getPropertyDescription();
	}

	@Override
	public void clearChanged()
	{
		super.clearChanged();
		for (WebCustomType x : getAllFirstLevelArrayOfOrCustomPropertiesFlattened())
		{
			if (x.isChanged()) x.clearChanged();
		}
	}

	@Override
	public void updateJSON()
	{
		webObjectImpl.updateCustomProperties();
		getParent().updateJSON();
	}

	@Override
	public void setJsonSubproperty(String key, Object value)
	{
		webObjectImpl.setJsonSubproperty(key, value);
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		if (!webObjectImpl.setCustomProperty(propertyName, val)) super.setProperty(propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		if (!webObjectImpl.clearCustomProperty(propertyName)) super.clearProperty(propertyName);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		if (webObjectImpl == null) return super.getProperty(propertyName);

		Pair<Boolean, Object> customResult = webObjectImpl.getCustomProperty(propertyName);
		if (customResult.getLeft().booleanValue()) return customResult.getRight();
		else return super.getProperty(propertyName);
	}

	public List<WebCustomType> getAllFirstLevelArrayOfOrCustomPropertiesFlattened()
	{
		return webObjectImpl.getAllCustomProperties();
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

	public void setJson(ServoyJSONObject arg)
	{
		webObjectImpl.setJson(arg);
	}

	public ServoyJSONObject getJson()
	{
		return webObjectImpl.getJson();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + webObjectImpl.toString(); //$NON-NLS-1$
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
		webObjectImpl.internalAddChild(obj);
	}

	@Override
	public Iterator<IPersist> getAllObjects()
	{
		return webObjectImpl.getAllObjects();
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

}
