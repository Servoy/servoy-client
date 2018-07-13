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


import java.awt.Component;
import java.awt.Point;
import java.util.Comparator;

/**
 * @author jblok
 */
public class PositionComparator
{
	public static final Comparator< ? super IPersist> XY_PERSIST_COMPARATOR = new PositionPersistComparator(true);
	public static final Comparator< ? super Component> XY_COMPONENT_COMPARATOR = new PositionComponentComparator(true);
	public static final Comparator< ? super ISupportBounds> XY_BOUNDS_COMPARATOR = new SupportBoundsComparator(true);
	public static final Comparator< ? super IPersist> YX_PERSIST_COMPARATOR = new PositionPersistComparator(false);
	public static final Comparator< ? super Component> YX_COMPONENT_COMPARATOR = new PositionComponentComparator(false);
	public static final Comparator< ? super ISupportBounds> YX_BOUNDS_COMPARATOR = new SupportBoundsComparator(false);

	public static class PositionPersistComparator implements Comparator<IPersist>
	{
		private final boolean xy;

		private PositionPersistComparator(boolean xy)
		{
			this.xy = xy;
		}

		public int compare(IPersist o1, IPersist o2)
		{
			if (o1 instanceof ISupportBounds && o2 instanceof ISupportBounds)
			{
				return comparePoint(xy, CSSPosition.getLocation((ISupportBounds)o1), CSSPosition.getLocation((ISupportBounds)o2));
			}
			if (o1 instanceof ISupportBounds && !(o2 instanceof ISupportBounds))
			{
				return -1;
			}
			if (!(o1 instanceof ISupportBounds) && o2 instanceof ISupportBounds)
			{
				return 1;
			}
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	public static final <T extends ISupportBounds> SupportBoundsComparator<T> xyBoundsComparator()
	{
		return (SupportBoundsComparator<T>)XY_BOUNDS_COMPARATOR;
	}

	@SuppressWarnings("unchecked")
	public static final <T extends ISupportBounds> SupportBoundsComparator<T> yxBoundsComparator()
	{
		return (SupportBoundsComparator<T>)YX_BOUNDS_COMPARATOR;
	}

	public static class SupportBoundsComparator<T extends ISupportBounds> implements Comparator<T>
	{
		private final boolean xy;

		private SupportBoundsComparator(boolean xy)
		{
			this.xy = xy;
		}

		public int compare(ISupportBounds o1, ISupportBounds o2)
		{
			if (o1 == o2) return 0;
			if (o1 != null && o2 != null)
			{
				return comparePoint(xy, o1.getLocation(), o2.getLocation());
			}
			return o1 == null ? -1 : 1;
		}
	}

	public static class PositionComponentComparator implements Comparator<Component>
	{
		private final boolean xy;

		private PositionComponentComparator(boolean xy)
		{
			this.xy = xy;
		}

		public int compare(Component o1, Component o2)
		{
			if (o1 != null && o2 != null)
			{
				return comparePoint(xy, o1.getLocation(), o2.getLocation());
			}
			if (o1 == null)
			{
				return o2 == null ? 0 : 1;
			}
			return 1;
		}
	}

	public static int comparePoint(boolean xy, Point p1, Point p2)
	{
		if (p1 != null && p2 != null)
		{
			int diff = xy ? (p1.x - p2.x) : (p1.y - p2.y);
			if (diff == 0)
			{
				diff = xy ? (p1.y - p2.y) : (p1.x - p2.x);
			}
			return diff;
		}
		if (p1 == null)
		{
			return p2 == null ? 0 : -1;
		}
		return 1;
	}

}