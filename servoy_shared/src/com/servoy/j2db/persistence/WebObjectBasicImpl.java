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
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;


/**
 * @author gboros
 */
public class WebObjectBasicImpl
{
	protected final IBasicWebObject webObject;

	public WebObjectBasicImpl(IBasicWebObject webObject)
	{
		this.webObject = webObject;
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
		((AbstractBase)webObject).setTypedProperty(StaticContentSpecLoader.PROPERTY_JSON, arg);
	}

	public JSONObject getJson()
	{
		return ((AbstractBase)webObject).getTypedProperty(StaticContentSpecLoader.PROPERTY_JSON);
	}

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

	/**
	 * Returns false if it can't clear this as a custom property (it is something else). Then caller should just clear it as another standard persist property.
	 */
	public boolean clearCustomProperty(String propertyName)
	{
		return false;
	}

	public List<WebCustomType> getAllCustomProperties()
	{
		return new ArrayList<WebCustomType>(0);
	}

	public Iterator<IPersist> getAllObjects()
	{
		return new ArrayList<IPersist>().iterator();
	}

	public IPersist getChild(UUID childUuid)
	{
		return null;
	}

	public boolean hasCustomProperty(String propertyName)
	{
		return false;
	}

	public Pair<Boolean, Object> getCustomProperty(String propertyName)
	{
		return new Pair<>(Boolean.FALSE, null);
	}

	public <T extends IPersist> Iterator<T> getObjects(int tp)
	{
		return new TypeIterator<T>(getAllObjects(), tp);
	}

	public PropertyDescription getPropertyDescription()
	{
		return null;
	}

	public void internalAddChild(IPersist obj)
	{
	}

	public void internalRemoveChild(IPersist obj)
	{
	}

	public boolean removeJsonSubproperty(String key)
	{
		return false;
	}

	public boolean setCustomProperty(String propertyName, Object val)
	{
		return false;
	}

	public void setJsonInternal(JSONObject arg)
	{
	}

	public void setJsonSubproperty(String key, Object value)
	{
	}

	public void updateCustomProperties()
	{
	}

}
