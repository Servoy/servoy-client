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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 *
 * @author gboros
 */
public class WebCustomType extends AbstractBase implements IWebObject
{
	private final String jsonKey;
	private final String typeName;
	private int index;

	/**
	 * @param newBean
	 * @param b
	 * @param parent
	 * @param element_id
	 * @param uuid
	 */
	public WebCustomType(IWebComponent parentElement, String jsonKey, String typeName, int index, boolean isNew)
	{
		//we just tell the GhostBean that it has a parent, we do not tell the parent that it contains a GhostBean
		super(IRepository.WEBCUSTOMTYPES, parentElement, 0, UUID.randomUUID());
		this.jsonKey = jsonKey;
		this.typeName = typeName;
		this.index = index;

		try
		{
			JSONObject entireModel = parentElement.getJson() != null ? parentElement.getJson() : new ServoyJSONObject();
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
				setJson(obj instanceof ServoyJSONObject ? (ServoyJSONObject)obj : new ServoyJSONObject(obj.toString(), false));
			}
			else
			{
				setJson(new ServoyJSONObject());
			}
		}
		catch (JSONException e)
		{
			Debug.error(e);
		}
	}

	public int getIndex()
	{
		return index;
	}

	public void setTypeName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME, arg);
	}

	public String getTypeName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_TYPENAME);
	}

	public void setJson(ServoyJSONObject arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	public ServoyJSONObject getJson()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_JSON);
	}

	public IWebComponent getParentComponent()
	{
		return (IWebComponent)super.getParent();
	}

	/**
	 * @return
	 */
	public String getUUIDString()
	{
		String addIndex = "";
		if (index >= 0) addIndex = "." + index;
		return parent.getUUID() + "_" + jsonKey + addIndex + "_" + typeName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractBase#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof WebCustomType)
		{
			return ((WebCustomType)obj).getUUIDString().equals(this.getUUIDString());
		}
		return super.equals(obj);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IWebObject#setName(java.lang.String)
	 */
	@Override
	public void setName(String arg)
	{
		setTypedProperty(StaticContentSpecLoader.PROPERTY_NAME, arg);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IWebObject#getName()
	 */
	@Override
	public String getName()
	{
		return getTypedProperty(StaticContentSpecLoader.PROPERTY_NAME);
	}

	/**
	 * @return the jsonKey
	 */
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
}
