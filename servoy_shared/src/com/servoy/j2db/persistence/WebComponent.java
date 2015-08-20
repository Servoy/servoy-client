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

import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * @author gboros
 */
public class WebComponent extends BaseComponent implements IWebComponent
{

	protected transient WebObjectImpl webObjectImpl;

	protected WebComponent(ISupportChilds parent, int element_id, UUID uuid)
	{
		super(IRepository.WEBCOMPONENTS, parent, element_id, uuid);
		webObjectImpl = new WebObjectImpl(this);
	}

	private void writeObject(java.io.ObjectOutputStream stream) throws java.io.IOException
	{
		stream.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException
	{
		stream.defaultReadObject();
		webObjectImpl = new WebObjectImpl(this);
	}

	@Override
	public PropertyDescription getPropertyDescription()
	{
		return webObjectImpl.getPropertyDescription();
	}

	@Override
	public IWebComponent getParentComponent()
	{
		return this;
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
		if (!webObjectImpl.clearCustomProperty(propertyName) && !webObjectImpl.removeJsonSubproperty(propertyName)) super.clearProperty(propertyName);
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

	public void setTypeName(String arg)
	{
		webObjectImpl.setTypeName(arg);
	}

	public String getTypeName()
	{
		return webObjectImpl.getTypeName();
	}

	public void setJson(JSONObject arg)
	{
		if (!(arg instanceof ServoyJSONObject)) throw new RuntimeException("ServoyJSONObject is needed here in order to make it serializable");
		webObjectImpl.setJson(arg);
	}

	public JSONObject getJson()
	{
		JSONObject x = webObjectImpl.getJson();
		try
		{
			return ((x instanceof ServoyJSONObject) || x == null) ? x : new ServoyJSONObject(x, ServoyJSONObject.getNames(x), true, true);
		}
		catch (JSONException e)
		{
			Debug.error(e);
			return x;
		}
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

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " -> " + webObjectImpl.toString(); //$NON-NLS-1$
	}

}
