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
import java.util.HashMap;
import java.util.Map;

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

	private static ThreadLocal<Map<String, Boolean>> parentFormsCache = new ThreadLocal<>();

	public static class PositionPersistComparator implements Comparator<IPersist>
	{
		private final boolean xy;

		private PositionPersistComparator(boolean xy)
		{
			this.xy = xy;
		}

		public int compare(IPersist o1, IPersist o2)
		{
			// first, check if elements belong to different forms in a hierarchy
			int hierarchyCompare = compareByFormHierarchy(o1, o2);
			if (hierarchyCompare != 0)
			{
				return hierarchyCompare;
			}
			if (areBothFromSameParentForm(o1, o2))
			{
				// both elements are from the same parent form - use inverted sorting
				if (o1 instanceof ISupportBounds && o2 instanceof ISupportBounds)
				{
					return comparePoint(!xy, CSSPositionUtils.getLocation((ISupportBounds)o1), CSSPositionUtils.getLocation((ISupportBounds)o2));
				}
			}
			if (o1 instanceof ISupportBounds && o2 instanceof ISupportBounds)
			{
				return comparePoint(xy, CSSPositionUtils.getLocation((ISupportBounds)o1), CSSPositionUtils.getLocation((ISupportBounds)o2));
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
				return comparePoint(xy, CSSPositionUtils.getLocation(o1), CSSPositionUtils.getLocation(o2));
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

	/**
	 * Compares two persists based on their form hierarchy.
	 * Parent forms are sorted before child forms.
	 *
	 * @param o1 first persist
	 * @param o2 second persist
	 * @return positive if o1 should come after o2, negative if o1 should come before o2, 0 if same form
	 */
	static int compareByFormHierarchy(IPersist o1, IPersist o2)
	{
		Form form1 = getOriginalForm(o1);
		Form form2 = getOriginalForm(o2);

		// if from different forms in hierarchy, sort by hierarchy (parent forms first)
		if (form1 != null && form2 != null && !form1.equals(form2))
		{
			Map<String, Boolean> parentFormsMap = new HashMap<>();

			if (hasFormInHierarchy(form1, form2, parentFormsMap))
			{
				return 1;
			}
			if (hasFormInHierarchy(form2, form1, parentFormsMap))
			{
				return -1;
			}
		}

		return 0; // same form or no hierarchy relationship
	}

	/**
	 * Checks if form1 extends form2 (i.e., form2 is a parent/super-form of form1).
	 * Also populates the parentFormsMap with all parent forms found during traversal.
	 *
	 * @param form1 the form to check
	 * @param form2 the form to check against
	 * @param parentFormsMap a map to populate with parent forms found during traversal
	 * @return true if form1 extends form2, false otherwise
	 */
	static boolean hasFormInHierarchy(Form form1, Form form2, Map<String, Boolean> parentFormsMap)
	{
		Form superForm = form1.getExtendsForm();
		while (superForm != null)
		{
			// Track this as a parent form
			parentFormsMap.put(superForm.getUUID().toString(), Boolean.TRUE);

			if (superForm.getUUID().equals(form2.getUUID()))
			{
				parentFormsCache.set(parentFormsMap);
				return true;
			}
			superForm = superForm.getExtendsForm();
		}
		parentFormsCache.set(parentFormsMap);
		return false;
	}

	/**
	 * Unwraps a persist and gets its original form, handling FlattenedForm and FlattenedPersistWrapper cases.
	 *
	 * @param persist the persist to get the original form for
	 * @return the original form of the persist
	 */
	static Form getOriginalForm(IPersist persist)
	{
		// Unwrap flattened persist wrapper if needed
		IPersist unwrapped = persist instanceof IFlattenedPersistWrapper ? ((IFlattenedPersistWrapper< ? >)persist).getWrappedPersist() : persist;

		// Get the form ancestor
		Form form = (Form)unwrapped.getAncestor(IRepository.FORMS);

		// Unwrap FlattenedForm to get the actual form
		return (form instanceof FlattenedForm) ? ((FlattenedForm)form).getForm() : form;
	}

	/**
	 * Checks if both persists are from the same parent form.
	 * A parent form is a form that has children extending it.
	 *
	 * @param o1 first persist
	 * @param o2 second persist
	 * @return true if both persists are from the same parent form
	 */
	static boolean areBothFromSameParentForm(IPersist o1, IPersist o2)
	{
		Form form1 = getOriginalForm(o1);
		Form form2 = getOriginalForm(o2);

		if (form1 != null && form1.equals(form2))
		{
			Map<String, Boolean> parentFormsMap = parentFormsCache.get();
			if (parentFormsMap != null && parentFormsMap.containsKey(form1.getUUID().toString()))
			{
				return true;
			}
		}
		return false;
	}

}