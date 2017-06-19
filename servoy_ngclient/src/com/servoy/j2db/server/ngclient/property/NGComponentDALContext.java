/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.j2db.server.ngclient.property;

import java.beans.PropertyChangeListener;
import java.util.Collection;

import org.sablo.BaseWebObject;
import org.sablo.IWebObjectContext;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;

import com.servoy.j2db.server.ngclient.IDataAdapterList;

/**
 * {@link INGWebObjectContext} implementation for servoy WebFormComponents. Used by {@link FoundsetLinkedPropertyType} and properties that are wrapped
 * by that class - to be transformed into foundset linked properties.
 *
 * @author acostescu
 */
public class NGComponentDALContext implements INGWebObjectContext
{

	private final IDataAdapterList dataAdapterList;
	private final IWebObjectContext parentWebObjectContext;

	public NGComponentDALContext(IDataAdapterList dataAdapterList, IWebObjectContext parentWebObjectContext)
	{
		this.dataAdapterList = dataAdapterList;
		this.parentWebObjectContext = parentWebObjectContext;
	}

	@Override
	public IDataAdapterList getDataAdapterList()
	{
		return dataAdapterList;
	}

	@Override
	public String toString()
	{
		return "DAL web object context extension of " + parentWebObjectContext;
	}

	public static IDataAdapterList getDataAdapterList(IWebObjectContext webObjectContext)
	{
		IWebObjectContext currentContext = webObjectContext;
		BaseWebObject webObject = webObjectContext.getUnderlyingWebObject();
		while (currentContext != webObject && !(currentContext instanceof INGWebObjectContext))
		{
			currentContext = currentContext.getParentContext();
		}

		if (currentContext instanceof INGWebObjectContext) return ((INGWebObjectContext)currentContext).getDataAdapterList();
		else return null;
	}

	// THE rest of the methods below are just proxies to same methods in "parentWebObjectContext"

	@Override
	public Object getProperty(String name)
	{
		return parentWebObjectContext.getProperty(name);
	}

	@Override
	public boolean setProperty(String propertyName, Object value)
	{
		return parentWebObjectContext.setProperty(propertyName, value);
	}

	@Override
	public BaseWebObject getUnderlyingWebObject()
	{
		return parentWebObjectContext instanceof BaseWebObject ? (BaseWebObject)parentWebObjectContext : parentWebObjectContext.getUnderlyingWebObject();
	}

	@Override
	public PropertyDescription getPropertyDescription(String name)
	{
		return parentWebObjectContext.getPropertyDescription(name);
	}

	@Override
	public Object getRawPropertyValue(String name)
	{
		return parentWebObjectContext.getRawPropertyValue(name);
	}

	@Override
	public Collection<PropertyDescription> getProperties(IPropertyType< ? > type)
	{
		return parentWebObjectContext.getProperties(type);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		parentWebObjectContext.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		parentWebObjectContext.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public IWebObjectContext getParentContext()
	{
		return parentWebObjectContext;
	}

}
