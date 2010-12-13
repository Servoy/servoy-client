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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Iterator;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.IFilter;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Wrapper element for group of IFormElements with the same groupID property. Note that a FormElementGroup is not an entity by itself and has no properties of
 * itself.
 * 
 * @author rgansevles
 * 
 */
public class FormElementGroup implements ISupportBounds, ISupportName
{
	private String groupID;
	private final Form form;
	private final FlattenedSolution flattenedSolution;

	public FormElementGroup(String groupID, FlattenedSolution flattenedSolution, Form form)
	{
		this.groupID = groupID;
		this.flattenedSolution = flattenedSolution;
		this.form = form;
	}

	public Iterator<IFormElement> getElements()
	{
		Form flattenedForm;
		try
		{
			flattenedForm = flattenedSolution.getFlattenedForm(form);
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
			return Collections.<IFormElement> emptyList().iterator();
		}
		return new FilteredIterator<IFormElement>(flattenedForm.getAllObjects(), new IFilter<IFormElement>()
		{
			public boolean match(Object o)
			{
				return (o instanceof IFormElement) && groupID.equals(((IFormElement)o).getGroupID());
			}
		});
	}

	public Form getParent()
	{
		return form;
	}

	public String getGroupID()
	{
		return groupID;
	}

	/**
	 * @param groupID
	 */
	public void setGroupID(String groupID)
	{
		this.groupID = groupID;
	}

	public String getName()
	{
		return getName(groupID);
	}

	/**
	 * the groupID can be either a name or a UUID (in which case the group is anonymous).
	 * 
	 * @param groupID
	 * @return name or null when anonymous
	 */
	public static String getName(String groupID)
	{
		if (groupID != null)
		{
			try
			{
				UUID.fromString(groupID);
			}
			catch (IllegalArgumentException e)
			{
				// not a uuid, so a real name
				return groupID;
			}
		}
		// group id is null or uuid: no name
		return null;
	}

	public Rectangle getBounds()
	{
		return Utils.getBounds(getElements());
	}

	public Point getLocation()
	{
		Rectangle bounds = getBounds();
		return new Point(bounds.x, bounds.y);
	}

	public Dimension getSize()
	{
		Rectangle bounds = getBounds();
		return new Dimension(bounds.width, bounds.height);
	}

	public void setLocation(Point p)
	{
		// ignore, location should only be set via FormElementGroupPropertySource
	}

	public void setSize(Dimension d)
	{
		// ignore, size should only be set via FormElementGroupPropertySource
	}


	public IFormElement getElement(int n)
	{
		Iterator<IFormElement> elements = getElements();
		for (int i = 0; elements.hasNext(); i++)
		{
			IFormElement next = elements.next();
			if (i == n)
			{
				return next;
			}
		}

		return null;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((groupID == null) ? 0 : groupID.hashCode());
		result = prime * result + ((form == null) ? 0 : form.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final FormElementGroup other = (FormElementGroup)obj;
		if (groupID == null)
		{
			if (other.groupID != null) return false;
		}
		else if (!groupID.equals(other.groupID)) return false;
		if (form == null)
		{
			if (other.form != null) return false;
		}
		else if (!form.equals(other.form)) return false;
		return true;
	}
}
