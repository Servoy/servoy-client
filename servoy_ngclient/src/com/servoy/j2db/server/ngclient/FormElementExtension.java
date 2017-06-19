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

package com.servoy.j2db.server.ngclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * @author lvostinar
 *
 */
public class FormElementExtension implements INGFormElement
{
	private final INGFormElement parentFormElementContext;
	private final Map<String, ? > extensionValues;
	private final PropertyDescription extensionPropertyDescription;

	public FormElementExtension(INGFormElement formElement, Map<String, ? > extensionValues, PropertyDescription extensionPropertyDescription)
	{
		this.parentFormElementContext = formElement;
		this.extensionValues = extensionValues;
		this.extensionPropertyDescription = extensionPropertyDescription;
	}

	@Override
	public IPersist getPersistIfAvailable()
	{
		return parentFormElementContext.getPersistIfAvailable();
	}

	@Override
	public Object getPropertyValue(String propertyName)
	{
		if (extensionValues != null && extensionValues.containsKey(propertyName))
		{
			return extensionValues.get(propertyName);
		}
		return parentFormElementContext.getPropertyValue(propertyName);
	}

	@Override
	public String getDesignId()
	{
		return parentFormElementContext.getDesignId();
	}

	@Override
	public String getName()
	{
		return parentFormElementContext.getName();
	}

	@Override
	public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
	{
		Collection<PropertyDescription> properties = new ArrayList<PropertyDescription>();
		properties.addAll(parentFormElementContext.getProperties(type));
		if (extensionPropertyDescription != null)
		{
			properties.addAll(extensionPropertyDescription.getProperties(type));
		}
		return properties;
	}

	@Override
	public PropertyDescription getPropertyDescription(String name)
	{
		if (extensionPropertyDescription != null)
		{
			PropertyDescription pd = extensionPropertyDescription.getProperty(name);
			if (pd != null)
			{
				return pd;
			}
		}
		return parentFormElementContext.getPropertyDescription(name);
	}

	@Override
	public Form getForm()
	{
		return parentFormElementContext.getForm();
	}


}
