/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.debug.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.debug.layout.ILayoutWrapper.MobileFormSection;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedForm;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.scripting.solutionmodel.JSButton;
import com.servoy.j2db.scripting.solutionmodel.JSField;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSLabel;
import com.servoy.j2db.scripting.solutionmodel.JSPortal;
import com.servoy.j2db.util.CompositeComparator;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Static methods for layouting elements in mobile form editor.
 * Can be called from DebugWebclient for layouting forms created using solution model.
 *
 * @author rgansevles
 *
 */
public class MobileFormLayout
{
	public static final int MOBILE_FORM_WIDTH = 350; // fixed width, future: make configurable

	private static final int MIN_FORM_HEIGHT = 250;

	public static void layoutForm(List< ? extends ILayoutWrapper> elements)
	{
		// children are based on model order as created in editPart.getModelChildren()
		int y = 0;
		int height = 0;

		for (ILayoutWrapper element : elements)
		{
			int x;
			if (element.getElementType() == MobileFormSection.ContentElement)
			{
				x = 10;
				y++;
			}
			else
			{
				x = 0;
			}
			int width = MOBILE_FORM_WIDTH - (2 * x);
			y += height;

			int childHeight = element.getPreferredHeight();
			height = childHeight <= 0 ? 55 : childHeight;

			if (y + height < MIN_FORM_HEIGHT && element.getElementType() == MobileFormSection.Footer)
			{
				y = MIN_FORM_HEIGHT - height;
			}

			element.setBounds(x, y, width, height);
		}
	}

	public static void layoutHeader(List< ? extends ILayoutWrapper> elements, int containerX, int containerY, int containerWidth)
	{
		int y = containerY + 8;
		int height = 28;

		for (ILayoutWrapper element : elements)
		{
			int width = 50;

			int x;
			if (element.getMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON) != null)
			{
				x = containerX + 20;
			}
			else if (element.getMobileProperty(IMobileProperties.HEADER_TEXT) != null)
			{
				width = containerWidth - 150;
				x = containerX + (containerWidth - width) / 2;
			}
			else if (element.getMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON) != null)
			{
				x = containerX + containerWidth - width - 20;
			}
			else continue;

			element.setBounds(x, y, width, height);
		}
	}

	public static void layoutFooter(List< ? extends ILayoutWrapper> elements, int containerX, int containerY, int containerWidth)
	{
		int x = containerX + 1;
		int y = containerY + 1;
		int width = 49;
		int height = 38;

		for (ILayoutWrapper element : elements)
		{
			element.setBounds(x, y, width, height);

			x += width + 1;
			if (x + width > containerWidth)
			{
				// next line
				x = containerX + 2;
				y += height + 2;
			}
		}
	}

	public static void layoutGroup(int containerX, int containerY, int containerWidth, int containerHeight, List< ? extends ILayoutWrapper> elements)
	{
		int x = containerX + 2;
		int y = containerY + 2;
		int width = containerWidth - 4;

		for (ILayoutWrapper element : elements)
		{
			int childPrefHeight = element.getPreferredHeight();
			int height = (childPrefHeight > 0 ? childPrefHeight : 38);

			element.setBounds(x, y, width, height);

			y += height + 2;
		}
	}

	public static int calculateGroupHeight(List<ILayoutWrapper> elements)
	{
		int height = 0;
		for (ILayoutWrapper element : elements)
		{
			int childPrefHeight = element.getPreferredHeight();
			height += (childPrefHeight > 0 ? childPrefHeight : 38) + 2;
		}

		return height;
	}

	public static List<ISupportBounds> getBodyElementsForRecordView(FlattenedSolution editingFlattenedSolution, Form flattenedForm)
	{
		List<ISupportBounds> elements = new ArrayList<ISupportBounds>();
		Set<String> groups = new HashSet<String>();
		for (IPersist persist : flattenedForm.getAllObjectsAsList())
		{
			if (persist instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement((ISupportExtendsID)persist))
			{
				// skip orphaned overrides
				continue;
			}

			if (persist instanceof ISupportFormElement && persist instanceof AbstractBase)
			{
				String groupID = ((ISupportFormElement)persist).getGroupID();
				if (groupID == null)
				{
					if (persist instanceof Portal && ((Portal)persist).isMobileInsetList())
					{
						// inset list
						elements.add(((Portal)persist));
					}

					// tabpanel: list elements or navtab
					else if (((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName) == null &&
						((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName) == null)
					{
						// regular item
						elements.add((ISupportBounds)(persist instanceof IFlattenedPersistWrapper
							? ((IFlattenedPersistWrapper< ? >)persist).getWrappedPersist() : persist));
					}
				}
				else if (groups.add(groupID))
				{
					elements.add(new FormElementGroup(groupID, editingFlattenedSolution, FlattenedForm.getWrappedForm(flattenedForm)));
				}
			}
		}

		// sort by y-position
		Collections.sort(elements, PositionComparator.YX_BOUNDS_COMPARATOR);

		return elements;
	}

	@SuppressWarnings("unchecked")
	public static List<ISupportFormElement> getGroupElements(FormElementGroup group)
	{
		List<ISupportFormElement> returnList = Utils.asList(group.getElements());
		Collections.sort(returnList, new CompositeComparator<ISupportFormElement>(new Comparator<ISupportFormElement>()
		{
			// sort so that label comes first
			public int compare(ISupportFormElement element1, ISupportFormElement element2)
			{
				if (element1.getTypeID() == IRepository.GRAPHICALCOMPONENTS)
				{
					return element2.getTypeID() == IRepository.GRAPHICALCOMPONENTS ? 0 : -1;
				}
				return element2.getTypeID() == IRepository.GRAPHICALCOMPONENTS ? 1 : 0;
			}
		}, PositionComparator.XY_PERSIST_COMPARATOR));
		return returnList;
	}

	public static ILayoutWrapper createLayoutWrapper(ISupportBounds element, JSForm jsform)
	{
		if (element instanceof Portal)
		{
			// inset list
			return new ComponentLayoutWrapper(new JSPortal(jsform, (Portal)element, jsform.getApplication(), false));
		}

		if (element instanceof Field)
		{
			return new ComponentLayoutWrapper(new JSField(jsform, (Field)element, jsform.getApplication(), false));
		}

		if (element instanceof GraphicalComponent)
		{
			return new ComponentLayoutWrapper(ComponentFactory.isButton((GraphicalComponent)element) ? new JSButton(jsform, (GraphicalComponent)element,
				jsform.getApplication(), false) : new JSLabel(jsform, (GraphicalComponent)element, jsform.getApplication(), false));
		}

		if (element instanceof FormElementGroup)
		{
			// group
			return new GroupLayoutWrapper((FormElementGroup)element, jsform);
		}

		return null;

	}

}
