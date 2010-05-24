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
import java.util.Iterator;

import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.IFilter;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Wrapper element for group of IFormElements with the same groupID property. Note that a FormElementGroup is not an entity by itself and has no properties of
 * itself.
 * 
 * @author rob
 * 
 */
public class FormElementGroup implements ISupportBounds, ISupportUpdateableName
{
	private String groupID;
	private final ISupportChilds parent;

	public FormElementGroup(String groupID, ISupportChilds parent)
	{
		this.groupID = groupID;
		this.parent = parent;
	}

	public Iterator<IFormElement> getElements()
	{
		return new FilteredIterator<IFormElement>(parent.getAllObjects(), new IFilter<IFormElement>()
		{
			public boolean match(Object o)
			{
				return (o instanceof IFormElement) && groupID.equals(((IFormElement)o).getGroupID());
			}
		});
	}

	public ISupportChilds getParent()
	{
		return parent;
	}

	public String getGroupID()
	{
		return groupID;
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

	public void updateName(IValidateName validator, String name) throws RepositoryException
	{
		String newGroupId;
		if (name == null)
		{
			newGroupId = UUID.randomUUID().toString();
		}
		else
		{
			if (!name.equals(getName()))
			{
				validator.checkName(name, -1, new ValidatorSearchContext(parent, IRepository.ELEMENTS), false);
			}
			newGroupId = name;
		}

		Iterator<IFormElement> elements = getElements();
		while (elements.hasNext())
		{
			elements.next().setGroupID(newGroupId);
		}
		// must set grouID after looping over elements (uses current groupID)
		groupID = newGroupId;
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
		Point oldLocation = getLocation();
		int dx = p.x - oldLocation.x;
		int dy = p.y - oldLocation.y;
		if (dx == 0 && dy == 0) return;

		Iterator<IFormElement> elements = getElements();
		while (elements.hasNext())
		{
			IFormElement element = elements.next();
			Point oldElementLocation = element.getLocation();
			Point location = new Point(oldElementLocation.x + dx, oldElementLocation.y + dy);
			element.setLocation(location);
		}
	}

	public void setSize(Dimension d)
	{
		Rectangle oldBounds = getBounds();
		if (d.width == oldBounds.width && d.height == oldBounds.height || oldBounds.width == 0 || oldBounds.height == 0)
		{
			return;
		}

		float factorW = d.width / (float)oldBounds.width;
		float factorH = d.height / (float)oldBounds.height;

		Iterator<IFormElement> elements = getElements();
		while (elements.hasNext())
		{
			IFormElement element = elements.next();
			Dimension oldElementSize = element.getSize();
			Point oldElementLocation = element.getLocation();

			Dimension size = new Dimension((int)(oldElementSize.width * factorW), (int)(oldElementSize.height * factorH));

			int newX;
			if (oldElementLocation.x + oldElementSize.width == oldBounds.x + oldBounds.width)
			{
				// element was attached to the right side, keep it there
				newX = oldBounds.x + d.width - size.width;
			}
			else
			{
				// move relative to size factor
				newX = oldBounds.x + (int)((oldElementLocation.x - oldBounds.x) * factorW);
			}
			int newY;
			if (oldElementLocation.y + oldElementSize.height == oldBounds.y + oldBounds.height)
			{
				// element was attached to the bottom side, keep it there
				newY = oldBounds.y + d.height - size.height;
			}
			else
			{
				// move relative to size factor
				newY = oldBounds.y + (int)((oldElementLocation.y - oldBounds.y) * factorH);
			}
			Point location = new Point(newX, newY);

			element.setSize(size);
			element.setLocation(location);
		}
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
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
		if (parent == null)
		{
			if (other.parent != null) return false;
		}
		else if (!parent.equals(other.parent)) return false;
		return true;
	}


}
