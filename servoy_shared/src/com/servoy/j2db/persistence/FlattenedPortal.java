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

import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;

/**
 * @author lvostinar
 *
 */
public class FlattenedPortal extends Portal
{
	private final Portal portal;

	public FlattenedPortal(Portal portal)
	{
		super(portal.getParent(), portal.getID(), portal.getUUID());
		this.portal = portal;
		fill();
	}

	private void fill()
	{
		internalClearAllObjects();
		List<Portal> portals = new ArrayList<Portal>();
		portals.add(portal);
		Form form = (Form)portal.getAncestor(IRepository.FORMS);
		while (form.getExtendsForm() != null)
		{
			if (form.getExtendsForm().getChild(uuid) instanceof Portal)
			{
				portals.add((Portal)form.getExtendsForm().getChild(uuid));
			}
			form = form.getExtendsForm();
		}
		for (Portal portal : portals)
		{
			for (IPersist child : portal.getAllObjectsAsList())
			{
				if (this.getChild(child.getUUID()) == null)
				{
					internalAddChild(child);
				}
			}
		}
	}

	@Override
	protected void internalRemoveChild(IPersist obj)
	{
		portal.internalRemoveChild(obj);
		fill();
	}

	@Override
	<T> T getTypedProperty(TypedProperty<T> property)
	{
		return portal.getTypedProperty(property);
	}

	@Override
	void setTypedProperty(TypedProperty< ? > property, Object value)
	{
		portal.setTypedProperty(property, value);
	}

	@Override
	void setTypedProperty(TypedProperty< ? > property, Object value, boolean validate)
	{
		portal.setTypedProperty(property, value, validate);
	}
}
