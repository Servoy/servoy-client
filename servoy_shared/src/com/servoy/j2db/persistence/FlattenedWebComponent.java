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

import java.util.List;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author lvostinar
 *
 */
public class FlattenedWebComponent extends WebComponent implements IFlattenedPersistWrapper<WebComponent>
{

	private static final long serialVersionUID = 1L;

	private final WebComponent webComponent;

	public FlattenedWebComponent(WebComponent webComponent)
	{
		super(webComponent.getParent(), webComponent.getUUID());
		this.webComponent = webComponent;
		fill();
	}

	public WebComponent getWrappedPersist()
	{
		return webComponent;
	}

	private void fill()
	{
		internalClearAllObjects();
		List<IPersist> children = PersistHelper.getHierarchyChildren(webComponent);
		for (IPersist child : children)
		{
			internalAddChild(child);
		}
		this.customTypesInitialized = true;
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		webComponent.internalRemoveChild(obj);
		fill();
	}

	@Override
	public void addChild(IPersist obj)
	{
		webComponent.addChild(obj);
		fill();
	}

	@Override
	public <T> T getTypedProperty(TypedProperty<T> property)
	{
		return webComponent.getTypedProperty(property);
	}

	@Override
	<T> void setTypedProperty(TypedProperty<T> property, T value)
	{
		webComponent.setTypedProperty(property, value);
	}

	@Override
	public Object getProperty(String propertyName)
	{
		return webComponent.getProperty(propertyName);
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		webComponent.setProperty(propertyName, val);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		webComponent.clearProperty(propertyName);
	}

	@Override
	protected void initCustomTypes()
	{
		// do not initialize custom types, fill method does this
	}
}
